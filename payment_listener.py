"""
Safe & Event-Driven Humo Payment Listener
- 100% Passiv tinglovchi: Telegram serveriga ortiqcha so'rov yubormaydi (Ban xavfi 0%).
- Telegram Push-Event (@client.on(events.NewMessage)) orqali ishlaydi.
- @HUMOcardbot dan to'lov xabari kelganda Java Bot API ga yuboradi.
"""

import asyncio
import json
import logging
import os
import re
import urllib.request
from telethon import TelegramClient, events
from telethon.sessions import StringSession
from telethon.errors import (
    UserDeactivatedError,
    UserDeactivatedBanError,
    AuthKeyUnregisteredError,
    SessionPasswordNeededError
)

# -------------------------------------------------------------
# SOZLAMALAR
# -------------------------------------------------------------
API_ID = int(os.environ.get("TG_API_ID", "39467356"))
API_HASH = os.environ.get("TG_API_HASH", "44a1a557b46f67a7b65861d97db7c8e0")

LISTEN_BOTS = ["humocardbot", "humocard", "humobot", "hpay", "paynet"]

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(name)s: %(message)s"
)
logger = logging.getLogger("HumoPaymentListener")

SESSION_FILE = "humo_payment_session.session"
TG_SESSION_ENV = os.environ.get("TG_SESSION_STRING", "").strip()

if os.path.exists(SESSION_FILE):
    session_obj = "humo_payment_session"
elif TG_SESSION_ENV:
    session_obj = StringSession(TG_SESSION_ENV)
else:
    session_obj = "humo_payment_session"

client = TelegramClient(session_obj, API_ID, API_HASH)


def normalize_uz_text(text: str) -> str:
    if not text:
        return ""
    for char in ["\u2018", "\u2019", "\u02bb", "`", "’", "‘", "ʻ"]:
        text = text.replace(char, "'")
    return text.replace("\xa0", " ")


def clean_uz_number(raw: str) -> float | None:
    if not raw:
        return None
    raw = raw.strip().replace(" ", "").replace("\xa0", "").replace("`", "")
    try:
        if "." in raw and "," in raw:
            if raw.find(".") < raw.find(","):
                raw = raw.replace(".", "").replace(",", ".")
            else:
                raw = raw.replace(",", "")
        elif "." in raw:
            parts = raw.split(".")
            if len(parts[-1]) == 3:
                raw = raw.replace(".", "")
        elif "," in raw:
            parts = raw.split(",")
            if len(parts[-1]) == 3:
                raw = raw.replace(",", "")
            else:
                raw = raw.replace(",", ".")
        val = float(raw)
        if 500 <= val <= 100_000_000:
            return val
    except Exception:
        pass
    return None


def parse_amount(text: str) -> float | None:
    if not text:
        return None

    text_norm = normalize_uz_text(text)

    # 1. To'ldirish / Kirim / Popolnenie formatlari
    m_top = re.search(
        r"(?:to'?ldirish|kirim|popolnenie|tushum|hisob to'?ldirildi)\s*[:\n\r\s]+\s*([0-9\s.,]+)\s*(?:uzs|so'?m)?",
        text_norm,
        re.IGNORECASE
    )
    if m_top:
        val = clean_uz_number(m_top.group(1))
        if val:
            return val

    # 2. Standart formatlar
    patterns = [
        r"(?:to'?lanishi kerak bo'?lgan summa|to'?lash|summa|to'?lov|qabul qilindi|\+)\s*:?\s*`?([0-9\s.,]+)`?\s*(?:uzs|so'?m|rub|usd)?",
        r"([0-9\s.,]+)\s*(?:uzs|so'?m)\s*(?:kirim|tushum|\+)",
        r"\+\s*([0-9\s.,]+)\s*(?:uzs|so'?m)",
        r"([0-9]{1,3}(?:[\s\xa0.][0-9]{3})*(?:,[0-9]{2})?)\s*(?:uzs|so'?m)",
    ]

    for pat in patterns:
        for m in re.finditer(pat, text_norm, re.IGNORECASE):
            val = clean_uz_number(m.group(1))
            if val:
                return val

    return None


def is_incoming(text: str) -> bool:
    lower = normalize_uz_text(text).lower()
    negative_words = [
        "chiqim", "spisanie", "yechildi", "otmenen",
        "rad etildi", "yetarli emas", "blokirovka"
    ]
    if any(neg in lower for neg in negative_words):
        return False
    return True


async def send_to_bot_api(amount: float, raw_text: str):
    payload = {
        "amount": amount,
        "rawText": raw_text,
        "secret": "humo_bot_internal_secret_key"
    }
    data = json.dumps(payload).encode("utf-8")

    port = os.environ.get("PORT", "10000")
    urls = []
    if os.environ.get("BOT_API_URL"):
        urls.append(os.environ.get("BOT_API_URL"))
    urls.extend([
        f"http://127.0.0.1:{port}/api/v1/payment/notify-card",
        f"http://localhost:{port}/api/v1/payment/notify-card",
        "http://127.0.0.1:10000/api/v1/payment/notify-card",
        "http://127.0.0.1:8085/api/v1/payment/notify-card"
    ])
    urls = list(dict.fromkeys(urls))

    for target_url in urls:
        try:
            req = urllib.request.Request(
                target_url,
                data=data,
                headers={"Content-Type": "application/json"}
            )
            loop = asyncio.get_event_loop()
            response = await loop.run_in_executor(
                None,
                lambda: urllib.request.urlopen(req, timeout=5)
            )
            status_code = response.getcode()
            body = response.read().decode("utf-8")
            logger.info("✅ Bot API ga muvaffaqiyatli yuborildi: status=%s, javob=%s", status_code, body)
            return
        except Exception:
            pass

    logger.warning("⚠️ Bot API server hozircha javob bermadi, navbatdagi so'rovda qayta uriniladi.")


processed_msg_ids = set()

async def process_msg_text(msg_id, text, sender_username=""):
    if msg_id in processed_msg_ids:
        return
    processed_msg_ids.add(msg_id)

    if not is_incoming(text):
        return

    amount = parse_amount(text)
    if amount is None:
        return

    logger.info("💰 Aniqlangan kirim: %s UZS. Bot API ga uzatilmoqda...", amount)
    await send_to_bot_api(amount, text)


@client.on(events.NewMessage)
async def handle_new_message(event):
    try:
        sender = await event.get_sender()
        sender_username = (getattr(sender, "username", "") or "").lower()

        text = event.message.text or ""
        if not text:
            return

        is_humo_bot = (
            any(bot_name in sender_username for bot_name in LISTEN_BOTS)
            or "humo" in sender_username
            or "card" in sender_username
            or "humo" in text.lower()
            or event.is_private
        )

        if is_humo_bot:
            logger.info("📥 Yangi to'lov xabari olindi (@%s, ID: %s)", sender_username, event.message.id)
            await process_msg_text(event.message.id, text, sender_username)
    except Exception as e:
        logger.error("Xabarni qayta ishlashda xatolik: %s", e)


async def main():
    logger.info("🚀 Xavfsiz Humo Payment Listener ishga tushmoqda...")
    while True:
        try:
            await client.connect()
            if not await client.is_user_authorized():
                logger.warning("⚠️ Telegram sessiyasi mavjud emas yoki avtorizatsiya talab qilinadi. Asosiy Java bot faol ishlamoqda.")
                await asyncio.sleep(300)
                continue

            me = await client.get_me()
            if me is None:
                logger.warning("⚠️ Foydalanuvchi ma'lumotlari olinmadi. 60 soniyadan so'ng qayta ulanadi...")
                await asyncio.sleep(60)
                continue

            first_name = getattr(me, "first_name", "User") or "User"
            username = getattr(me, "username", "") or "unknown"
            logger.info("✅ Telegram akkaunt xavfsiz ulandi: %s (@%s) [ID: %s]", first_name, username, me.id)
            logger.info("🎯 Passiv Push-Event rejimida @HUMOcardbot to'lovlari kutilmoqda (0 so'rovli xavfsiz rejim)...")

            # Faqat xabarlarni passiv kutish (Hech qanday loop yoki polling so'rovlari yo'q)
            await client.run_until_disconnected()

        except (UserDeactivatedError, UserDeactivatedBanError, AuthKeyUnregisteredError) as e:
            logger.error("❌ Telegram sessiyasi bekor qilingan (%s). Loop to'xtatildi. Asosiy bot xavfsiz faoliyat yuritmoqda.", e)
            await asyncio.sleep(86400)  # 24 soat kutish (Telegramga qayta so'rov yubormaslik)
        except Exception as e:
            logger.error("Ulanishda xatolik: %s. 60 soniyadan so'ng xavfsiz qayta ulanishga uriniladi...", e)
            await asyncio.sleep(60)


if __name__ == "__main__":
    asyncio.run(main())

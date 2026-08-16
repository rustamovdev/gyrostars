"""
Payment Listener — @HUMOcardbot dan kelgan to'lov xabarlarini avtomatik o'qib,
asosiy Java botdagi foydalanuvchi balansini 10 daqiqa ichida avtomatik to'ldiradi.

O'rnatilgan kutubxona:
    telethon
"""

import asyncio
import json
import logging
import os
import re
import urllib.request
from telethon import TelegramClient, events
from telethon.sessions import StringSession

# -------------------------------------------------------------
# SOZLAMALAR
# -------------------------------------------------------------
API_ID = int(os.environ.get("TG_API_ID", "39467356"))
API_HASH = os.environ.get("TG_API_HASH", "44a1a557b46f67a7b65861d97db7c8e0")

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
    # Har qanday turdagi apostroflarni (', ‘, ’, ʻ, `) bitta standart ' ga aylantiramiz
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
    """
    @HUMOcardbot xabaridan kirim summasini ajratib oladi.
    Format namunasi:
      To'ldirish
       12.000,00 UZS
       PAYNET P2P HUM2HUM>T
       HUMOCARD *7042
       13:57 15.08.2026
       66.766,69 UZS
    """
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

    # 2. Boshqa standart formatlar (Kirim: 10 000 UZS, +10 000 UZS va h.k.)
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
    """
    Xabar kirim operatsiyasi ekanligini tekshiradi (chiqim yoki hisobot emas).
    """
    lower = normalize_uz_text(text).lower()
    negative_words = [
        "chiqim", "spisanie", "yechildi", "otmenen",
        "rad etildi", "yetarli emas", "blokirovka"
    ]

    if any(neg in lower for neg in negative_words):
        return False

    return True


async def send_to_bot_api(amount: float, raw_text: str):
    """
    Java Botning /api/v1/payment/notify-card endpointiga ma'lumot jo'natadi.
    Server ishga tushayotgan bo'lsa, qayta urinishlar orqali yetkazadi.
    """
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
    # Remove duplicates while preserving order
    urls = list(dict.fromkeys(urls))

    max_attempts = 30
    for attempt in range(1, max_attempts + 1):
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
                logger.info("✅ Bot API ga yuborildi (urinish %d, url=%s): status=%s, body=%s", attempt, target_url, status_code, body)
                return
            except Exception:
                pass

        if attempt % 5 == 0 or attempt == 1:
            logger.info("⏳ Bot API server ishga tushishi kutilmoqda (urinish %d/%d)...", attempt, max_attempts)
        await asyncio.sleep(2)

    logger.error("❌ Bot API ga to'lov xabarini yetkazib bo'lmadi (Barcha urinishlar tugadi).")


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

    logger.info("💰 Aniqlangan kirim summasi: %s UZS. Tezkor Bot API ga yuborilmoqda...", amount)
    await send_to_bot_api(amount, text)


@client.on(events.NewMessage)
async def handle_new_message(event):
    """
    Har qanday yangi xabar kelganda tezkor ishlaydi.
    """
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


async def poll_recent_humo_messages():
    """
    Telegram push voqealari kechikmasligi uchun har 5 soniyada HUMOcardbot dan so'nggi xabarlarni tekshirib turadi.
    """
    while True:
        try:
            async for dialog in client.iter_dialogs(limit=15):
                name = dialog.name.lower()
                username = (dialog.entity.username or "").lower() if hasattr(dialog.entity, "username") else ""
                if "humo" in name or "humo" in username:
                    async for msg in client.iter_messages(dialog.entity, limit=3):
                        if msg.text and msg.id not in processed_msg_ids:
                            await process_msg_text(msg.id, msg.text, username)
        except Exception as e:
            logger.debug("Poll check error: %s", e)
        await asyncio.sleep(5)


async def main():
    logger.info("🚀 @HUMOcardbot Tezkor Payment Listener ishga tushmoqda...")
    poll_task = None
    while True:
        try:
            await client.connect()
            if not await client.is_user_authorized():
                logger.warning("⚠️ Foydalanuvchi hali avtorizatsiyadan o'tmagan yoki sessiya kutmoqda...")
                await asyncio.sleep(10)
                continue
            me = await client.get_me()
            if me is None:
                logger.warning("⚠️ Akkaunt ma'lumotlari bo'sh qaytdi, 5 soniyadan keyin qayta ulanadi...")
                await asyncio.sleep(5)
                continue
            first_name = getattr(me, "first_name", "Admin") or "Admin"
            username = getattr(me, "username", "") or "unknown"
            logger.info("✅ Akkaunt muvaffaqiyatli ulandi: %s (@%s)", first_name, username)
            logger.info("🎯 Faqat @HUMOcardbot xabarlari tezkor tinglanmoqda...")
            if poll_task is None or poll_task.done():
                poll_task = asyncio.create_task(poll_recent_humo_messages())
            await client.run_until_disconnected()
        except Exception as e:
            logger.error("Xatolik: %s. 5 soniyadan so'ng qayta ulanadi...", e)
            await asyncio.sleep(5)


if __name__ == "__main__":
    asyncio.run(main())

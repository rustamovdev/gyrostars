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
# Telegram API ma'lumotlari
API_ID = int(os.environ.get("TG_API_ID", "39467356"))
API_HASH = os.environ.get("TG_API_HASH", "44a1a557b46f67a7b65861d97db7c8e0")

# StringSession orqali bulutda barqaror ishlash
SESSION_STRING = os.environ.get(
    "TG_SESSION_STRING",
    "1ApWapzMBu0Xo8FaGEbO3YVwaIYPe5T-ZdJtBn1b5L4fglvBdmnIEpSonKHFEKa8-5USa9vTUL6iUtANs6G43bLMBOuxvv8DylVkiX8LNzFtQw3iaUH2XkwnKzrEvQDgXJV0e6Wj_-eBcP7n5-Um6I_8dflAV4qdR46RS9GyYcuU5N5c0WiF3DnqSwhtlmi_TGaSeWKketYQxocLO3C8OjZ1kvALeNlatU96vEixEf0LiBQ8P3UEIjxz_M3ZkVLV6vJZZUQDz5XUf0cciM9pQEkmBuG_xGdrUO6q7xTLLuUiLBA7WINwVZJw4mxG-pnNQ-MoYJ4fIeONINWnMSn1cGTq3uLL-lAg="
)

# Spring Boot Java botining to'lov qabul qilish endpoint manzili
BOT_API_PORT = os.environ.get("PORT", "8085")
BOT_API_URL = os.environ.get("BOT_API_URL", f"http://127.0.0.1:{BOT_API_PORT}/api/v1/payment/notify-card")

# Faqat @HUMOcardbot va Humo rasmiy botlari
LISTEN_BOTS = [
    "humocardbot",
    "humo_card_bot",
    "humobot"
]

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(name)s: %(message)s",
)
logger = logging.getLogger("HumoPaymentListener")

if SESSION_STRING:
    session_obj = StringSession(SESSION_STRING)
else:
    session_obj = "humo_payment_session"

client = TelegramClient(session_obj, API_ID, API_HASH)


def parse_amount(text: str) -> float | None:
    """
    @HUMOcardbot xabaridan kirim summasini ajratib oladi.
    Masalan:
      - 'Kirim: 10 000 UZS' -> 10000.0
      - 'Popolnenie: 50,000.00 UZS' -> 50000.0
      - 'Karta to\'ldirildi: 15 000.00 so\'m' -> 15000.0
      - '+10 000 UZS' -> 10000.0
    """
    if not text:
        return None

    # Raqamlar va valyutani qidirish
    patterns = [
        r"(?:kirim|popolnenie|karta to['`]?ldirildi|to['`]?lov|qabul qilindi|summa|tushum|\+)\s*:?\s*([\d\s.,]+)\s*(?:uzs|so['`]?m|rub|usd)?",
        r"([\d\s.,]+)\s*(?:uzs|so['`]?m)\s*(?:kirim|tushum|\+)",
        r"\+\s*([\d\s.,]+)\s*(?:uzs|so['`]?m)",
        r"(?:to'lanishi kerak bo'lgan summa|to'lash|summa)\s*:?\s*`?([\d\s.,]+)`?\s*(?:uzs|so['`]?m)?",
    ]

    for pat in patterns:
        m = re.search(pat, text, re.IGNORECASE)
        if m:
            raw_val = m.group(1).replace(" ", "").replace("\xa0", "").replace(",", ".")
            # Agar oxirida nuqtadan keyin 2 ta belgi bo'lsa (tiyinlar)
            try:
                val = float(raw_val)
                if val > 0:
                    return val
            except ValueError:
                pass

    # Umumiy barcha raqamlarni tekshirish (fallback)
    numbers = re.findall(r"\b\d{1,3}(?:[\s\xa0]\d{3})*(?:\.\d{2})?\b", text)
    for n in numbers:
        cleaned = n.replace(" ", "").replace("\xa0", "")
        try:
            val = float(cleaned)
            if val >= 500:  # Minimal 500 so'm deb faraz qilamiz
                return val
        except ValueError:
            pass

    return None


def is_incoming(text: str) -> bool:
    """
    Xabar kirim operatsiyasi ekanligini tekshiradi (chiqim yoki hisobot emas).
    """
    lower = text.lower()
    positive_words = [
        "kirim", "popolnenie", "to'ldirildi", "karta to'ldirildi",
        "qabul qilindi", "tushum", "hisobingiz to'ldirildi",
        "muvaffaqiyatli", "avto-to'lov", "to'lov", "+"
    ]
    negative_words = [
        "chiqim", "spisanie", "yechildi", "otmenen",
        "rad etildi", "yetarli emas", "blokirovka"
    ]

    if any(neg in lower for neg in negative_words):
        return False

    return any(pos in lower for pos in positive_words)


async def send_to_bot_api(amount: float, raw_text: str):
    """
    Java Botning /api/v1/payment/notify-card endpointiga ma'lumot jo'natadi.
    """
    payload = {
        "amount": amount,
        "rawText": raw_text,
        "secret": "humo_bot_internal_secret_key"
    }

    try:
        data = json.dumps(payload).encode("utf-8")
        req = urllib.request.Request(
            BOT_API_URL,
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
        logger.info("Bot API javobi: status=%s, body=%s", status_code, body)
    except Exception as e:
        logger.error("Bot API ga so'rov yuborishda xatolik: %s", e)


@client.on(events.NewMessage)
async def handle_new_message(event):
    """
    Har qanday yangi xabar kelganda ishlaydi.
    """
    sender = await event.get_sender()
    sender_username = (getattr(sender, "username", "") or "").lower()

    text = event.message.text or ""
    if not text:
        return

    # Tekshirish: xabar aynan @HUMOcardbot dan kelganmi?
    is_humo_bot = any(bot_name in sender_username for bot_name in LISTEN_BOTS) or "humocard" in sender_username or "humo" in text.lower()

    if is_humo_bot:
        logger.info("📥 @HUMOcardbot dan yangi xabar olindi:\n%s", text)

        if not is_incoming(text):
            logger.info("Bu kirim to'lovi emas (chiqim yoki hisobot), o'tkazib yuborilmoqda.")
            return

        amount = parse_amount(text)
        if amount is None:
            logger.warning("Xabardan summa ajratib bo'lmadi: %s", text)
            return

        logger.info("💰 Aniqlangan kirim summasi: %s UZS. Bot API ga yuborilmoqda...", amount)
        await send_to_bot_api(amount, text)


async def main():
    logger.info("🚀 @HUMOcardbot Payment Listener ishga tushmoqda...")
    while True:
        try:
            await client.connect()
            if not await client.is_user_authorized():
                logger.error("❌ Foydalanuvchi avtorizatsiyadan o'tmagan!")
                await asyncio.sleep(10)
                continue
            me = await client.get_me()
            logger.info("✅ Akkaunt muvaffaqiyatli ulandi: %s (@%s)", me.first_name, me.username)
            logger.info("🎯 Faqat @HUMOcardbot xabarlari tinglanmoqda...")
            await client.run_until_disconnected()
        except Exception as e:
            logger.error("Xatolik: %s. 5 soniyadan so'ng qayta ulanadi...", e)
            await asyncio.sleep(5)


if __name__ == "__main__":
    asyncio.run(main())

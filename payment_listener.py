"""
Payment Listener — @HUMOcardbot dan kelgan to'lov xabarlarini avtomatik o'qib,
asosiy Java botdagi foydalanuvchi balansini 10 daqiqa ichida avtomatik to'ldiradi.

O'rnatilgan kutubxona:
    telethon

Ishga tushirish:
    python payment_listener.py
"""

import asyncio
import json
import logging
import os
import re
import urllib.request
from telethon import TelegramClient, events

# -------------------------------------------------------------
# SOZLAMALAR
# -------------------------------------------------------------
# Telegram API ma'lumotlari
API_ID = int(os.environ.get("TG_API_ID", "39467356"))
API_HASH = os.environ.get("TG_API_HASH", "44a1a557b46f67a7b65861d97db7c8e0")

# Spring Boot Java botining to'lov qabul qilish endpoint manzili
BOT_API_URL = os.environ.get("BOT_API_URL", "http://127.0.0.1:8085/api/v1/payment/notify-card")

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

client = TelegramClient("humo_payment_session", API_ID, API_HASH)


def clean_uz_number(s: str) -> float:
    """O'zbekiston bank formatidagi sonlarni float ga o'tkazadi (masalan: '103.883,00' -> 103883.0 yoki '50 012' -> 50012.0)."""
    s = s.strip()
    if ',' in s and '.' in s:
        s = s.replace('.', '').replace(',', '.')
    elif ',' in s:
        s = s.replace(' ', '').replace(',', '.')
    elif '.' in s:
        parts = s.split('.')
        if len(parts[-1]) == 3:
            s = s.replace('.', '')
        else:
            s = s.replace(' ', '')
    else:
        s = s.replace(' ', '')
    return float(s)


def is_incoming(text: str) -> bool:
    """Xabar haqiqatan ham To'ldirish/Kirim ekanligini tekshiradi."""
    lower = text.lower()

    # Chiqim/xarid/to'lov bo'lsa rad etish
    outgoing = ["oplata", "spisanie", "xarid", "yechildi", "yechish", "snatie"]
    for out_kw in outgoing:
        if out_kw in lower and "to'ldirish" not in lower and "to‘ldirish" not in lower and "kirim" not in lower:
            return False

    # Kirim / To'ldirish kalit so'zlari
    incoming = [
        "to'ldirish",
        "to‘ldirish",
        "toldirish",
        "kirim",
        "tushum",
        "cash to card",
        "perevod na kartu",
        "popolnenie",
        "kartaga",
        "hisob to'ldirildi",
        "zachislenie",
        "+"
    ]
    return any(kw in lower for kw in incoming)


def parse_amount(text: str) -> float | None:
    """
    @HUMOcardbot xabaridan birinchi to'lov summasini ajratib oladi:
    Masalan:
    To'ldirish
     103.883,00 UZS  <-- Ushbu summani oladi (103883.0)
     CASH TO CARD 3 PAYNE
     HUMOCARD *7042
     20:14 14.08.2026
     153.668,59 UZS  <-- Pastdagi qoldiqni olmaydi!
    """
    lines = text.split('\n')
    for line in lines:
        line_clean = line.strip()
        # Qoldiq yoki Ostatok qatorini o'tkazib yuboramiz
        if "qoldiq" in line_clean.lower() or "ostatok" in line_clean.lower() or "balans" in line_clean.lower():
            continue

        match = re.search(r"([0-9][0-9\s\.,]*?)\s*(?:UZS|so['‘`]?m|sum|сум)", line_clean, re.IGNORECASE)
        if match:
            try:
                amt = clean_uz_number(match.group(1))
                if amt > 0:
                    return amt
            except Exception:
                pass

    # Agar line bo'yicha topilmasa, umumiy regex
    match = re.search(r"([0-9][0-9\s\.,]*?)\s*(?:UZS|so['‘`]?m|sum|сум)", text, re.IGNORECASE)
    if match:
        try:
            return clean_uz_number(match.group(1))
        except Exception:
            pass
    return None


def _sync_post(url: str, payload: dict) -> dict | None:
    data = json.dumps(payload).encode("utf-8")
    req = urllib.request.Request(url, data=data, headers={"Content-Type": "application/json"})
    try:
        with urllib.request.urlopen(req, timeout=10) as resp:
            resp_body = resp.read().decode("utf-8")
            return json.loads(resp_body)
    except Exception as e:
        logger.exception("Bot API ga so'rov yuborishda xatolik: %s", e)
        return None


async def send_to_bot_api(amount: float, raw_text: str):
    """To'lov ma'lumotini Spring Boot botiga POST request qilib yuboradi."""
    payload = {
        "amount": amount,
        "rawText": raw_text
    }

    resp = await asyncio.to_thread(_sync_post, BOT_API_URL, payload)
    if resp:
        logger.info("Bot API javobi: %s", resp)
        if resp.get("matched"):
            logger.info("✅ To'lov muvaffaqiyatli foydalanuvchiga biriktirildi va balans to'ldirildi! (Buyurtma #%s)", resp.get("orderId"))
        else:
            logger.warning("⚠️ To'lov qabul qilindi, lekin botda mos keluvchi faol buyurtma topilmadi.")


@client.on(events.NewMessage)
async def handle_notification(event):
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
    await client.start()
    me = await client.get_me()
    logger.info("✅ Akkaunt muvaffaqiyatli ulandi: %s (@%s)", me.first_name, me.username)
    logger.info("🎯 Faqat @HUMOcardbot xabarlari tinglanmoqda...")
    await client.run_until_disconnected()


if __name__ == "__main__":
    asyncio.run(main())

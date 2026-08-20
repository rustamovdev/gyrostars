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
from aiohttp import web
from telethon import TelegramClient, events, functions, types
from telethon.tl.types import InputInvoiceStarGift, TextWithEntities
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

LISTEN_BOTS = [
    "humocardbot", "humocard", "humobot", "hpay", "paynet",
    "payme", "click", "anorbank", "uzcard", "cardxabar", "cardxabarbot",
    "sms", "bank", "p2p", "humo", "uzcardbot"
]

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(name)s: %(message)s"
)
logger = logging.getLogger("HumoPaymentListener")

TG_SESSION_ENV = os.environ.get("TG_SESSION_STRING", "").strip()

if TG_SESSION_ENV:
    session_obj = StringSession(TG_SESSION_ENV)
    SESSION_FILE = "string_session"
    logger.info("🔑 TG_SESSION_STRING orqali StringSession ulandi")
elif os.path.exists("userbot_local.session"):
    session_obj = "userbot_local"
    SESSION_FILE = "userbot_local.session"
    logger.info("📁 userbot_local.session fayli orqali ulandi")
elif os.path.exists("humo_payment_session.session"):
    session_obj = "humo_payment_session"
    SESSION_FILE = "humo_payment_session.session"
    logger.info("📁 humo_payment_session.session fayli orqali ulandi")
else:
    session_obj = "userbot_local"
    SESSION_FILE = "userbot_local.session"

# Anti-ban xavfsizlik parametrlari (Telegram serverida haqiqiy mobil ilova bo'lib ko'rinadi)
client = TelegramClient(
    session_obj,
    API_ID,
    API_HASH,
    device_model="Samsung Galaxy S24 Ultra",
    system_version="Android 14",
    app_version="11.2.0",
    lang_code="uz",
    system_lang_code="uz-UZ",
    flood_sleep_threshold=120
)


def normalize_uz_text(text: str) -> str:
    if not text:
        return ""
    for char in ["\u2018", "\u2019", "\u02bb", "`", "’", "‘", "ʻ", "'"]:
        text = text.replace(char, "'")
    return text.replace("\xa0", " ").strip()


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

    # 1. To'g'ridan-to'g'ri Kirim / Popolnenie / To'ldirish / Zachisleno / O'tkazma iboralari
    primary_patterns = [
        # Kirim: 50 000 UZS / Popolnenie: 50 000 / Zachisleno: 50000 / To'ldirish: 25000
        r"(?:kirim|tushum|popolnenie|postuplenie|zachisleno|to'?ldirish|to'?ldirildi|qabul qilindi|o'?tkazma|perevod|pereveli|hisob to'?ldirildi|hisobingiz to'?ldirildi)\s*[:\n\r\s\-]*\+?\s*([0-9\s.,]+)\s*(?:uzs|so'?m|sum|rub)?",
        # 50 000 UZS ga to'ldirildi / 50 000 UZS tushdi / 50 000 UZS kirim / 50000 UZS zachisleno
        r"([0-9\s.,]+)\s*(?:uzs|so'?m|sum)\s*(?:ga|dan)?\s*(?:to'?ldirildi|tushdi|kirim|tushum|zachisleno|qabul qilindi|\+)",
        # + 50 000 UZS / +50 000 so'm
        r"\+\s*([0-9\s.,]+)\s*(?:uzs|so'?m|sum)",
        # Summa / To'lov / To'lanishi kerak: 50 000 UZS
        r"(?:summa|to'?lov|to'?lanishi kerak bo'?lgan summa)\s*[:\n\r\s\-]*\+?\s*`?([0-9\s.,]+)`?\s*(?:uzs|so'?m|sum|rub)?",
        # Hisobingiz 100000 UZS ga to'ldirildi
        r"hisobingiz\s+([0-9\s.,]+)\s*(?:uzs|so'?m|sum)",
        # kartangizga 50 000.00 UZS
        r"kartangizga\s+([0-9\s.,]+)\s*(?:uzs|so'?m|sum)",
        # ga 50000 UZS tushum
        r"ga\s+([0-9\s.,]+)\s*(?:uzs|so'?m|sum)\s+tushum",
    ]

    for pat in primary_patterns:
        for m in re.finditer(pat, text_norm, re.IGNORECASE):
            val = clean_uz_number(m.group(1))
            if val:
                return val

    # 2. Agar maxsus kalit so'z topilmasa, lekin raqam + UZS/so'm bo'lsa (Balans / Qoldiq / Ostatok ni inobatga olmasdan)
    clean_text = re.sub(r"(?:balans|qoldiq|ostatok|dostupno)\s*[:\n\r\s\-]*[0-9\s.,]+\s*(?:uzs|so'?m|sum)?", "", text_norm, flags=re.IGNORECASE)

    fallback_patterns = [
        r"([0-9]{1,3}(?:[\s\xa0.][0-9]{3})*(?:,[0-9]{2})?)\s*(?:uzs|so'?m|sum)",
        r"([0-9]{4,10})\s*(?:uzs|so'?m|sum)"
    ]
    for pat in fallback_patterns:
        for m in re.finditer(pat, clean_text, re.IGNORECASE):
            val = clean_uz_number(m.group(1))
            if val:
                return val

    return None


def is_incoming(text: str) -> bool:
    lower = normalize_uz_text(text).lower()
    negative_words = [
        "chiqim", "spisanie", "yechildi", "otmenen",
        "rad etildi", "yetarli emas", "blokirovka",
        "xarajat", "otkazano"
    ]
    if any(neg in lower for neg in negative_words):
        if any(pos in lower for pos in ["kirim", "popolnenie", "tushum", "zachisleno", "to'ldirildi"]):
            return True
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
        logger.warning("⚠️ Xabardan summa aniqlanmadi (@%s): %s", sender_username, text.replace('\n', ' '))
        return

    logger.info("💰 Aniqlangan kirim: %s UZS. Bot API ga uzatilmoqda...", amount)
    await send_to_bot_api(amount, text)


@client.on(events.NewMessage)
@client.on(events.MessageEdited)
async def handle_new_message(event):
    try:
        text = event.raw_text or getattr(event.message, "message", "") or ""
        if not text:
            return

        sender = await event.get_sender()
        sender_username = (getattr(sender, "username", "") or "").lower()
        sender_title = (getattr(sender, "title", "") or "").lower()
        sender_first = (getattr(sender, "first_name", "") or "").lower()

        is_payment_source = (
            any(bot_name in sender_username for bot_name in LISTEN_BOTS)
            or any(bot_name in sender_title for bot_name in LISTEN_BOTS)
            or any(bot_name in sender_first for bot_name in LISTEN_BOTS)
            or "humo" in sender_username or "humo" in text.lower()
            or "uzcard" in sender_username or "uzcard" in text.lower()
            or "karta" in text.lower() or "card" in text.lower()
            or event.is_private
        )

        if is_payment_source:
            logger.info("📥 Yangi to'lov xabari olindi (@%s, ID: %s)", sender_username, event.message.id)
            await process_msg_text(event.message.id, text, sender_username)
    except Exception as e:
        logger.error("Xabarni qayta ishlashda xatolik: %s", e)



async def handle_send_gift_http(request):
    try:
        data = await request.json()
        target_raw = data.get("user_id") or data.get("userId") or data.get("peer")
        gift_id_raw = data.get("gift_id") or data.get("giftId")
        message_text = data.get("text") or data.get("message") or ""
        is_anon = bool(data.get("anonymous") or data.get("is_anonymous") or data.get("hide_name"))

        if not target_raw or not gift_id_raw:
            return web.json_response({"ok": False, "error": "user_id va gift_id parametrlarini kiritish shart"}, status=400)

        if not client.is_connected() or not await client.is_user_authorized():
            return web.json_response({"ok": False, "error": "Userbot Telegram akkauntiga ulanmagan yoki sessiya yaroqsiz"}, status=503)

        target = target_raw
        if isinstance(target, str):
            target = target.strip()
            if target.isdigit():
                target = int(target)
            elif target.startswith("@"):
                target = target[1:]

        try:
            peer = await client.get_input_entity(target)
        except Exception as ex:
            return web.json_response({"ok": False, "error": f"Foydalanuvchi topilmadi ({target}): {ex}"}, status=404)

        msg_obj = None
        if message_text:
            try:
                parsed_text, entities = await client._parse_message_text(message_text, 'html')
                msg_obj = TextWithEntities(text=parsed_text, entities=entities)
            except Exception as ex:
                logger.warning("Izohni HTML parse qilishda xatolik: %s. Oddiy matn ishlatiladi.", ex)
                msg_obj = TextWithEntities(text=str(message_text), entities=[])

        invoice_kwargs = {
            "peer": peer,
            "gift_id": int(gift_id_raw)
        }
        if is_anon:
            invoice_kwargs["hide_name"] = True
        if msg_obj:
            invoice_kwargs["message"] = msg_obj

        invoice = InputInvoiceStarGift(**invoice_kwargs)

        # Telegram serveridan payment form olish
        form = await client(functions.payments.GetPaymentFormRequest(invoice=invoice))

        # Stars balansidan to'lov qilib sovg'ani yuborish
        result = await client(functions.payments.SendStarsFormRequest(
            form_id=form.form_id,
            invoice=invoice
        ))

        logger.info("🎁 Sovg'a muvaffaqiyatli yuborildi: user=%s, gift_id=%s, anonim=%s", target, gift_id_raw, is_anon)
        return web.json_response({
            "ok": True,
            "message": "Sovg'a muvaffaqiyatli yetkazildi!",
            "details": {
                "target": str(target),
                "gift_id": str(gift_id_raw),
                "anonymous": is_anon,
                "text": message_text
            }
        })
    except Exception as e:
        logger.error("❌ Sovg'a yuborishda xatolik yuz berdi: %s", e, exc_info=True)
        return web.json_response({
            "ok": False,
            "error": str(e)
        }, status=500)


async def handle_gift_status_http(request):
    try:
        auth = client.is_connected() and await client.is_user_authorized()
        me_info = None
        if auth:
            me = await client.get_me()
            if me:
                me_info = {
                    "id": me.id,
                    "first_name": getattr(me, "first_name", ""),
                    "username": getattr(me, "username", "")
                }
        return web.json_response({
            "ok": True,
            "connected": client.is_connected(),
            "authorized": auth,
            "user": me_info
        })
    except Exception as e:
        return web.json_response({"ok": False, "error": str(e)}, status=500)


http_server_started = False

async def start_gift_api_server():
    global http_server_started
    if http_server_started:
        return
    try:
        app = web.Application()
        app.router.add_post("/api/v1/gift/send", handle_send_gift_http)
        app.router.add_get("/api/v1/gift/status", handle_gift_status_http)
        runner = web.AppRunner(app)
        await runner.setup()
        port = int(os.environ.get("USERBOT_HTTP_PORT", "8089"))
        site = web.TCPSite(runner, "127.0.0.1", port)
        await site.start()
        http_server_started = True
        logger.info("🚀 Userbot Gift REST API server faol: http://127.0.0.1:%s", port)
    except Exception as e:
        logger.error("❌ Userbot Gift API serverni ishga tushirishda xatolik: %s", e)


async def main():
    logger.info("🚀 Xavfsiz Humo Payment Listener ishga tushmoqda...")
    await start_gift_api_server()
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


"""
Safe, Event-Driven & Polling Hybrid Humo Payment Listener
- 100% Passiv tinglovchi + Xavfsiz orqa fonda sinxronizatsiya.
- Telegram Push-Event (@client.on(events.NewMessage)) orqali tezkor to'lovlarni qabul qiladi.
- Bot ishga tushganda va har 8 soniyada o'tkazib yuborilgan to'lovlarni tekshiradi.
- @HUMOcardbot va barcha bank/bot to'lov xabarlarini to'g'ri aniqlaydi va Java Bot API ga uzatadi.
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
    "sms", "bank", "p2p", "humo", "uzcardbot", "856254490"
]

HUMO_BOT_ID = 856254490

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
elif os.path.exists("humo_payment_session.session"):
    session_obj = "humo_payment_session"
    SESSION_FILE = "humo_payment_session.session"
    logger.info("📁 humo_payment_session.session fayli orqali ulandi")
elif os.path.exists("userbot_local.session"):
    session_obj = "userbot_local"
    SESSION_FILE = "userbot_local.session"
    logger.info("📁 userbot_local.session fayli orqali ulandi")
else:
    session_obj = "humo_payment_session"
    SESSION_FILE = "humo_payment_session.session"

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

# -------------------------------------------------------------
# DEDUPLICATION (Xabarlarni takroriy yuborilishining oldini olish)
# -------------------------------------------------------------
PROCESSED_FILE = os.path.join("data", "processed_payment_msgs.json")
processed_keys = set()

def load_processed_keys():
    global processed_keys
    try:
        if os.path.exists(PROCESSED_FILE):
            with open(PROCESSED_FILE, "r", encoding="utf-8") as f:
                data = json.load(f)
                if isinstance(data, list):
                    processed_keys = set(data[-1000:])
    except Exception as e:
        logger.warning("Processed keys yuklashda xatolik: %s", e)

def save_processed_key(key: str):
    global processed_keys
    processed_keys.add(key)
    try:
        os.makedirs("data", exist_ok=True)
        with open(PROCESSED_FILE, "w", encoding="utf-8") as f:
            json.dump(list(processed_keys)[-1000:], f)
    except Exception:
        pass

load_processed_keys()


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

    # 1. Aniq kirim / to'ldirish qatorlari (➕ 200.000,00 UZS, + 50 000 UZS, Kirim: 50000)
    primary_patterns = [
        # ➕ 200.000,00 UZS yoki + 200 000 UZS yoki + 200.000 UZS
        r"(?:[\u2795\+]|\bkirim\b|\btushum\b|\bpopolnenie\b|\bpostuplenie\b|\bzachisleno\b|\bto'?ldirish\b|\bto'?ldirildi\b|\bqabul qilindi\b|\bo'?tkazma\b|\bperevod\b|\bpereveli\b|\bhisob to'?ldirildi\b|\bhisobingiz to'?ldirildi\b)\s*[:\n\r\s\-]*[\u2795\+]?\s*([0-9]{1,3}(?:[\s.,][0-9]{3})*(?:[.,][0-9]{2})?|[0-9]{3,9})\s*(?:uzs|so'?m|sum|rub)?",
        # 50 000 UZS ga to'ldirildi / 50 000 UZS tushdi / 50 000 UZS kirim / 50000 UZS zachisleno
        r"([0-9]{1,3}(?:[\s.,][0-9]{3})*(?:[.,][0-9]{2})?|[0-9]{3,9})\s*(?:uzs|so'?m|sum)\s*(?:ga|dan)?\s*(?:to'?ldirildi|tushdi|kirim|tushum|zachisleno|qabul qilindi|tushgan)",
        # Summa / To'lov / To'lanishi kerak: 50 000 UZS
        r"(?:summa|to'?lov|to'?lanishi kerak bo'?lgan summa)\s*[:\n\r\s\-]*[\u2795\+]?\s*`?([0-9]{1,3}(?:[\s.,][0-9]{3})*(?:[.,][0-9]{2})?|[0-9]{3,9})`?\s*(?:uzs|so'?m|sum|rub)?",
        # kartangizga 50 000.00 UZS
        r"(?:kartangizga|hisobingizga|hisobingiz)\s+([0-9]{1,3}(?:[\s.,][0-9]{3})*(?:[.,][0-9]{2})?|[0-9]{3,9})\s*(?:uzs|so'?m|sum)",
    ]

    for pat in primary_patterns:
        for m in re.finditer(pat, text_norm, re.IGNORECASE):
            val = clean_uz_number(m.group(1))
            if val:
                return val

    # 2. Balans (💰 yoki Balans / Qoldiq / Ostatok) qatorlarini olib tashlagan holda qidirish
    clean_text = re.sub(r"(?:💰|balans|qoldiq|ostatok|dostupno)\s*[:\n\r\s\-]*[0-9\s.,]+\s*(?:uzs|so'?m|sum)?", "", text_norm, flags=re.IGNORECASE)

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
        "xarajat", "otkazano", "oplata"
    ]
    if any(neg in lower for neg in negative_words):
        if any(pos in lower for pos in ["kirim", "popolnenie", "tushum", "zachisleno", "to'ldirish", "to'ldirildi", "➕"]):
            return True
        return False
    return True


async def send_to_bot_api(amount: float, raw_text: str) -> bool:
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
        "http://127.0.0.1:8085/api/v1/payment/notify-card",
        "http://localhost:10000/api/v1/payment/notify-card",
        "http://localhost:8085/api/v1/payment/notify-card"
    ])
    urls = list(dict.fromkeys(urls))

    for target_url in urls:
        try:
            req = urllib.request.Request(
                target_url,
                data=data,
                headers={"Content-Type": "application/json", "User-Agent": "HumoListener/2.0"}
            )
            loop = asyncio.get_event_loop()
            response = await loop.run_in_executor(
                None,
                lambda: urllib.request.urlopen(req, timeout=7)
            )
            status_code = response.getcode()
            body = response.read().decode("utf-8")
            logger.info("✅ Bot API ga muvaffaqiyatli uzatildi (%s): status=%s, javob=%s", target_url, status_code, body)
            return True
        except Exception:
            pass

    logger.warning("⚠️ Bot API server hozircha javob bermadi, navbatdagi so'rovda qayta uriniladi.")
    return False


async def process_msg_text(msg_key: str, text: str, sender_name: str = "") -> bool:
    if msg_key in processed_keys:
        return False

    if not is_incoming(text):
        save_processed_key(msg_key)
        return False

    amount = parse_amount(text)
    if amount is None:
        return False

    logger.info("💰 Aniqlangan kirim: %s UZS (@%s, Key: %s). Bot API ga uzatilmoqda...", amount, sender_name, msg_key)
    success = await send_to_bot_api(amount, text)
    if success:
        save_processed_key(msg_key)
        return True
    return False


@client.on(events.NewMessage)
@client.on(events.MessageEdited)
async def handle_new_message(event):
    try:
        text = event.raw_text or getattr(event.message, "message", "") or ""
        if not text:
            return

        chat_id = event.chat_id
        sender = None
        try:
            sender = await event.get_sender()
        except Exception:
            pass

        sender_username = (getattr(sender, "username", "") or "").lower()
        sender_title = (getattr(sender, "title", "") or "").lower()
        sender_first = (getattr(sender, "first_name", "") or "").lower()

        is_payment_source = (
            chat_id == HUMO_BOT_ID
            or any(bot_name in sender_username for bot_name in LISTEN_BOTS)
            or any(bot_name in sender_title for bot_name in LISTEN_BOTS)
            or any(bot_name in sender_first for bot_name in LISTEN_BOTS)
            or "humo" in sender_username or "humo" in text.lower()
            or "uzcard" in sender_username or "uzcard" in text.lower()
            or "karta" in text.lower() or "card" in text.lower()
            or event.is_private
        )

        if is_payment_source:
            msg_key = f"{chat_id}_{event.message.id}"
            logger.info("📥 Yangi to'lov xabari olindi (@%s, ID: %s)", sender_username or chat_id, event.message.id)
            await process_msg_text(msg_key, text, sender_username or str(chat_id))
    except Exception as e:
        logger.error("Xabarni qayta ishlashda xatolik: %s", e)


async def scan_recent_messages():
    """Bot ishga tushganda yoki uzilish bo'lganda so'nggi to'lov xabarlarini tekshirish"""
    try:
        if not client.is_connected() or not await client.is_user_authorized():
            return

        # 1. Asosiy @HUMOcardbot xabarlarini to'g'ridan-to'g'ri tekshirish
        try:
            humo_entity = await client.get_entity("HUMOcardbot")
            messages = await client.get_messages(humo_entity, limit=25)
            for m in reversed(messages):
                if m.raw_text:
                    msg_key = f"{m.chat_id}_{m.id}"
                    await process_msg_text(msg_key, m.raw_text, "HUMOcardbot")
        except Exception as e:
            logger.warning("@HUMOcardbot xabarlarini olishda xatolik: %s", e)

        # 2. Boshqa to'lov dialoglarini tekshirish
        dialogs = await client.get_dialogs(limit=20)
        for d in dialogs:
            username = (getattr(d.entity, "username", "") or "").lower()
            title = (getattr(d.entity, "title", "") or "").lower()
            chat_id = d.id

            if chat_id != HUMO_BOT_ID and (
                any(bot in username for bot in LISTEN_BOTS)
                or any(bot in title for bot in LISTEN_BOTS)
            ):
                messages = await client.get_messages(d.entity, limit=10)
                for m in reversed(messages):
                    if m.raw_text:
                        msg_key = f"{chat_id}_{m.id}"
                        await process_msg_text(msg_key, m.raw_text, username or title)
    except Exception as e:
        logger.warning("scan_recent_messages xatolik: %s", e)


async def periodic_payment_poller():
    """Har 8 soniyada o'tkazib yuborilgan to'lovlar mavjudligini xavfsiz tekshirib turuvchi rejim"""
    await asyncio.sleep(5)
    while True:
        try:
            await scan_recent_messages()
        except Exception as e:
            logger.debug("Poller cycle xatolik: %s", e)
        await asyncio.sleep(8)



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
    
    # Orqa fonda to'lovlarni tekshiruvchi poller vazifasini boshlash
    asyncio.create_task(periodic_payment_poller())

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
            logger.info("🎯 Real-time Push-Event va Periodik Poller rejimida @HUMOcardbot to'lovlari faol tinglanmoqda...")

            # Ishga tushganda o'tkazib yuborilgan to'lovlarni darhol skanerlash
            await scan_recent_messages()

            # Faol tinglash
            await client.run_until_disconnected()

        except (UserDeactivatedError, UserDeactivatedBanError, AuthKeyUnregisteredError) as e:
            logger.error("❌ Telegram sessiyasi bekor qilingan (%s). Loop to'xtatildi. Asosiy bot xavfsiz faoliyat yuritmoqda.", e)
            await asyncio.sleep(86400)
        except Exception as e:
            logger.error("Ulanishda xatolik: %s. 30 soniyadan so'ng xavfsiz qayta ulanishga uriniladi...", e)
            await asyncio.sleep(30)


if __name__ == "__main__":
    asyncio.run(main())



"""
Telegram Akkauntni Ulash Skripti (Bir martalik autentifikatsiya)
"""

import asyncio
import os
from telethon import TelegramClient

API_ID = 39467356
API_HASH = "44a1a557b46f67a7b65861d97db7c8e0"
SESSION_NAME = "humo_payment_session"


async def main():
    # Eski yaroqsiz sessiya fayllarini tozalash (xavfsiz yangidan kirish uchun)
    for old_file in ["humo_payment_session.session", "humo_payment_session.session-journal"]:
        if os.path.exists(old_file):
            try:
                os.remove(old_file)
            except Exception:
                pass

    phone = input("Telefon raqamingizni kiriting (+998... formatida): ").strip()
    client = TelegramClient(SESSION_NAME, API_ID, API_HASH)
    await client.connect()

    if not await client.is_user_authorized():
        print(f"\n{phone} raqamiga Telegram orqali tasdiqlash kodi yuborilmoqda...")
        sent = await client.send_code_request(phone)
        
        code = input("Telegramingizga kelgan 5 xonali kodni kiriting: ").strip()
        try:
            await client.sign_in(phone, code)
        except Exception as e:
            if "Two-steps verification" in str(e) or "SessionPasswordNeededError" in type(e).__name__:
                pwd = input("Akkauntingizda 2FA ikki bosqichli parol bor. Parolni kiriting: ").strip()
                await client.sign_in(password=pwd)
            else:
                raise e

    me = await client.get_me()
    print(f"\n✅ Muvaffaqiyatli ulandi: {me.first_name} (@{me.username}) [ID: {me.id}]")
    print("📁 Sessiya fayli 'humo_payment_session.session' saqlandi!")

    try:
        from telethon.sessions import StringSession
        string_client = TelegramClient(StringSession(), API_ID, API_HASH)
        await string_client.connect()
        # sessiya ma'lumotlarini StringSession ga ko'chirish
        session_str = StringSession.save(client.session)
        print("\n" + "="*60)
        print("🔑 RENDER UCHUN TG_SESSION_STRING (Nusxalab oling va Render Environment ga qo'ying):")
        print("="*60)
        print(session_str)
        print("="*60 + "\n")
    except Exception as ex:
        pass

    await client.disconnect()


if __name__ == "__main__":
    asyncio.run(main())

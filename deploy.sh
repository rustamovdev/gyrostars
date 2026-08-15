#!/bin/bash
# =========================================================
#  Telegram Stars & Premium Bot — Avtomatik O'rnatish Skripti
# =========================================================

echo "🚀 [1/5] Tizim yangilanmoqda va kerakli dasturlar o'rnatilmoqda..."
sudo apt update -y
sudo apt install -y openjdk-21-jdk python3 python3-pip git tmux ufw

echo "📦 [2/5] Python kutubxonalari o'rnatilmoqda..."
pip3 install telethon requests --break-system-packages 2>/dev/null || pip3 install telethon requests

echo "⚙️ [3/5] Java loyiha yig'ilmoqda (Gradle build)..."
chmod +x ./gradlew
./gradlew build -x test

echo "🛡 [4/5] 24/7 Avtomatik ishga tushuvchi Systemd xizmatlari yaratilmoqda..."

# Java Bot xizmati
sudo bash -c "cat <<EOF > /etc/systemd/system/telegram-bot.service
[Unit]
Description=Telegram Stars & Premium Java Bot
After=network.target

[Service]
Type=simple
User=$USER
WorkingDirectory=$(pwd)
ExecStart=/usr/bin/java -Xmx512m -jar $(pwd)/build/libs/leykabot-4.1.0-M1.jar
Restart=always
RestartSec=5

[Install]
WantedBy=multi-user.target
EOF"

# Humo Listener xizmati
sudo bash -c "cat <<EOF > /etc/systemd/system/humo-listener.service
[Unit]
Description=Humo Payment Listener
After=network.target

[Service]
Type=simple
User=$USER
WorkingDirectory=$(pwd)
ExecStart=/usr/bin/python3 $(pwd)/payment_listener.py
Restart=always
RestartSec=5

[Install]
WantedBy=multi-user.target
EOF"

echo "⚡️ [5/5] Xizmatlar yoqilmoqda va ishga tushirilmoqda..."
sudo systemctl daemon-reload
sudo systemctl enable telegram-bot
sudo systemctl enable humo-listener
sudo systemctl restart telegram-bot
sudo systemctl restart humo-listener

echo ""
echo "========================================================="
echo "  ✅ BARCHASI MUVAFFAQIYATLI O'RNATILDI VA ISHGA TUSHDI! "
echo "========================================================="
echo "Holatni tekshirish:"
echo "  sudo systemctl status telegram-bot"
echo "  sudo systemctl status humo-listener"
echo ""
echo "Loglarni ko'rish:"
echo "  journalctl -u telegram-bot -f"
echo "  journalctl -u humo-listener -f"
echo "========================================================="

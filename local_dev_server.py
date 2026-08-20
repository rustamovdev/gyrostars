import http.server
import json
import os
import urllib.parse
import urllib.request
import webbrowser

PORT = 3000
STATIC_DIR = os.path.abspath(os.path.join(os.path.dirname(__file__), "src", "main", "resources", "static"))

BOT_TOKEN = "8842520350:AAEUc7rb9S42abHVqyM0WU8sGRupEJkxmSU"
EMOJI_CACHE = {}

mock_user = {
    "userId": 5305539499,
    "username": "Admin",
    "fullName": "Admin Developer",
    "balance": 250000,
    "verified": True
}

mock_history = [
    {
        "id": "STR-101",
        "service": "⭐ Stars",
        "details": "100 Stars",
        "amount": 23000,
        "status": "COMPLETED",
        "date": "2026-08-16 19:30:00"
    },
    {
        "id": "PRM-102",
        "service": "💎 Premium",
        "details": "3 oy",
        "amount": 170000,
        "status": "COMPLETED",
        "date": "2026-08-15 14:20:00"
    },
    {
        "id": "PBG-103",
        "service": "🎮 PUBG UC",
        "details": "325 UC (5123456789)",
        "amount": 55000,
        "status": "COMPLETED",
        "date": "2026-08-14 11:15:00"
    },
    {
        "id": "DEP-104",
        "service": "💳 Hisob to'ldirish",
        "details": "250 000 so'm",
        "amount": 250000,
        "status": "COMPLETED",
        "date": "2026-08-14 10:00:00"
    }
]

class DevServerHandler(http.server.SimpleHTTPRequestHandler):
    def __init__(self, *args, **kwargs):
        super().__init__(*args, directory=STATIC_DIR, **kwargs)

    def do_GET(self):
        parsed = urllib.parse.urlparse(self.path)
        path = parsed.path
        query = urllib.parse.parse_qs(parsed.query)

        # Telegram Premium Emoji Proxy Endpoint
        if path in ["/get-emoji-url", "/api/v1/emoji/get-url"]:
            emoji_id = query.get("emoji_id", query.get("emojiId", [""]))[0]
            if not emoji_id:
                self.send_response(400)
                self.send_header("Content-Type", "application/json")
                self.send_header("Access-Control-Allow-Origin", "*")
                self.end_headers()
                self.wfile.write(json.dumps({"ok": False, "error": "emoji_id majburiy"}).encode("utf-8"))
                return

            if emoji_id in EMOJI_CACHE:
                self.send_response(200)
                self.send_header("Content-Type", "application/json")
                self.send_header("Access-Control-Allow-Origin", "*")
                self.end_headers()
                self.wfile.write(json.dumps({"ok": True, "emoji_id": emoji_id, "url": EMOJI_CACHE[emoji_id], "cached": True}).encode("utf-8"))
                return

            try:
                # 1. getCustomEmojiStickers
                stickers_url = f"https://api.telegram.org/bot{BOT_TOKEN}/getCustomEmojiStickers?custom_emoji_ids=[\"{emoji_id}\"]"
                req = urllib.request.Request(stickers_url, headers={"User-Agent": "GyroStars-Bot/1.0"})
                with urllib.request.urlopen(req, timeout=5) as resp:
                    sticker_data = json.loads(resp.read().decode("utf-8"))

                if not sticker_data.get("ok") or not sticker_data.get("result"):
                    self.send_response(404)
                    self.send_header("Content-Type", "application/json")
                    self.send_header("Access-Control-Allow-Origin", "*")
                    self.end_headers()
                    self.wfile.write(json.dumps({"ok": False, "error": "Emoji topilmadi"}).encode("utf-8"))
                    return

                file_id = sticker_data["result"][0]["file_id"]

                # 2. getFile
                file_url = f"https://api.telegram.org/bot{BOT_TOKEN}/getFile?file_id={file_id}"
                req = urllib.request.Request(file_url, headers={"User-Agent": "GyroStars-Bot/1.0"})
                with urllib.request.urlopen(req, timeout=5) as resp:
                    file_data = json.loads(resp.read().decode("utf-8"))

                if not file_data.get("ok"):
                    self.send_response(500)
                    self.send_header("Content-Type", "application/json")
                    self.send_header("Access-Control-Allow-Origin", "*")
                    self.end_headers()
                    self.wfile.write(json.dumps({"ok": False, "error": "Fayl yo'li olinmadi"}).encode("utf-8"))
                    return

                file_path = file_data["result"]["file_path"]
                final_url = f"https://api.telegram.org/file/bot{BOT_TOKEN}/{file_path}"
                EMOJI_CACHE[emoji_id] = final_url

                self.send_response(200)
                self.send_header("Content-Type", "application/json")
                self.send_header("Access-Control-Allow-Origin", "*")
                self.end_headers()
                self.wfile.write(json.dumps({"ok": True, "emoji_id": emoji_id, "url": final_url, "cached": False}).encode("utf-8"))
                return
            except Exception as e:
                self.send_response(500)
                self.send_header("Content-Type", "application/json")
                self.send_header("Access-Control-Allow-Origin", "*")
                self.end_headers()
                self.wfile.write(json.dumps({"ok": False, "error": str(e)}).encode("utf-8"))
                return

        if path == "/api/webapp/init":
            self.send_response(200)
            self.send_header("Content-Type", "application/json")
            self.send_header("Access-Control-Allow-Origin", "*")
            self.end_headers()
            data = {
                "user": mock_user,
                "card": {
                    "cardNumber": "9860 0866 0350 6261",
                    "holderName": "Sharipov Sh",
                    "methodName": "HUMO"
                },
                "prices": {
                    "starUnitPrice": 230,
                    "starPackages": {
                        50: 11500, 100: 23000, 250: 57500, 500: 115000,
                        1000: 230000, 2500: 575000
                    },
                    "premiumPackages": {
                        1: {"title": "1 oy", "price": 45000, "discount": None, "popular": False, "desc": "Tezkor yetkazish"},
                        3: {"title": "3 oy", "price": 180000, "discount": "Ommabop", "popular": True, "desc": "Eng ko'p tanlangan"},
                        6: {"title": "6 oy", "price": 250000, "discount": "-15%", "popular": False, "desc": "Tejamkor"},
                        12: {"title": "12 oy", "price": 400000, "discount": "-30%", "popular": False, "desc": "VIP Tarif"}
                    },
                    "pubgPackages": {
                        60: 11000, 325: 55000, 660: 110000, 1800: 275000,
                        3850: 545000, 8100: 1090000
                    },
                    "freefirePackages": {
                        100: 15000, 310: 45000, 520: 75000, 1060: 150000, 2180: 300000
                    }
                }
            }
            self.wfile.write(json.dumps(data).encode("utf-8"))
            return

        if path == "/api/webapp/top":
            self.send_response(200)
            self.send_header("Content-Type", "application/json")
            self.send_header("Access-Control-Allow-Origin", "*")
            self.end_headers()
            data = {
                "period": query.get("period", ["today"])[0],
                "top": [
                    {"rank": 1, "name": "Shaxzod", "total": 1250000, "isMe": False},
                    {"rank": 2, "name": "Admin (Siz)", "total": 490000, "isMe": True},
                    {"rank": 3, "name": "Bekzod", "total": 350000, "isMe": False},
                    {"rank": 4, "name": "Jasur", "total": 230000, "isMe": False},
                    {"rank": 5, "name": "Farrux", "total": 180000, "isMe": False}
                ],
                "recentBuyers": [
                    "Shaxzod · 500 ⭐",
                    "Admin · 3 oylik 💎",
                    "Bekzod · 660 🎮 UC",
                    "Jasur · 250 ⭐",
                    "Farrux · 100 ⭐"
                ]
            }
            self.wfile.write(json.dumps(data).encode("utf-8"))
            return

        return super().do_GET()

    def do_POST(self):
        parsed = urllib.parse.urlparse(self.path)
        path = parsed.path
        length = int(self.headers.get("Content-Length", 0))
        body = self.rfile.read(length).decode("utf-8") if length > 0 else "{}"
        try:
            req_data = json.loads(body)
        except Exception:
            req_data = {}

        self.send_response(200)
        self.send_header("Content-Type", "application/json")
        self.send_header("Access-Control-Allow-Origin", "*")
        self.end_headers()

        if path == "/api/webapp/promocode/apply":
            code = req_data.get("code", "").upper()
            if code in ["GYRO2026", "VIP", "BONUS"]:
                bonus = 15000
                mock_user["balance"] += bonus
                resp = {
                    "ok": True,
                    "message": f"✅ Promokod faollashtirildi! +{bonus:,} so'm qo'shildi.",
                    "amount": bonus,
                    "newBalance": mock_user["balance"]
                }
            else:
                resp = {
                    "ok": False,
                    "error": "Promokod topilmadi yoki muddati o'tgan! (Sinash uchun: GYRO2026)"
                }
            self.wfile.write(json.dumps(resp).encode("utf-8"))
            return

        resp = {"ok": True, "message": "Buyurtma qabul qilindi"}
        self.wfile.write(json.dumps(resp).encode("utf-8"))

def main():
    import sys
    try:
        sys.stdout.reconfigure(encoding='utf-8')
    except Exception:
        pass
    server = http.server.ThreadingHTTPServer(("127.0.0.1", PORT), DevServerHandler)
    url = f"http://localhost:{PORT}?userId=5305539499&username=Admin&name=Admin"
    print(f"[*] GyroStars WebApp Dev Server ishga tushdi: {url}")
    print(f"[*] Static Directory: {STATIC_DIR}")
    try:
        webbrowser.open(url)
    except Exception:
        pass
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        print("\nServer to'xtatildi.")

if __name__ == "__main__":
    main()

import http.server
import json
import os
import urllib.parse
import webbrowser

PORT = 3000
STATIC_DIR = os.path.abspath(os.path.join(os.path.dirname(__file__), "src", "main", "resources", "static"))

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

        if path == "/api/webapp/init":
            self.send_response(200)
            self.send_header("Content-Type", "application/json")
            self.send_header("Access-Control-Allow-Origin", "*")
            self.end_headers()
            data = {
                "user": mock_user,
                "card": {
                    "cardNumber": "9860 0866 0350 6261",
                    "holderName": "S R",
                    "methodName": "HUMO"
                },
                "prices": {
                    "starUnitPrice": 230,
                    "starPackages": {
                        50: 12000, 100: 23000, 150: 34000, 250: 53000,
                        350: 78000, 500: 110000, 750: 160000, 1000: 215000
                    },
                    "premiumPackages": {
                        1: 50000, 3: 170000, 6: 230000, 12: 300000
                    },
                    "pubgPackages": {
                        60: 11000, 325: 55000, 660: 110000, 1800: 275000,
                        3850: 545000, 8100: 1090000
                    }
                },
                "stats": {
                    "totalStars": 350,
                    "totalSpent": 490000,
                    "totalPurchases": 5,
                    "goalTarget": 1200000,
                    "goalProgress": 41
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

        if path == "/api/webapp/history":
            self.send_response(200)
            self.send_header("Content-Type", "application/json")
            self.send_header("Access-Control-Allow-Origin", "*")
            self.end_headers()
            self.wfile.write(json.dumps(mock_history).encode("utf-8"))
            return

        if path == "/api/webapp/referral":
            self.send_response(200)
            self.send_header("Content-Type", "application/json")
            self.send_header("Access-Control-Allow-Origin", "*")
            self.end_headers()
            data = {
                "ok": True,
                "link": f"https://t.me/GyroService_bot?start=ref_{mock_user['userId']}",
                "count": 14,
                "percent": 2
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

        if path == "/api/webapp/buy/stars":
            amount = req_data.get("amount", 100)
            price = amount * 230
            if req_data.get("paymentMethod") == "balance":
                if mock_user["balance"] >= price:
                    mock_user["balance"] -= price
                    resp = {
                        "ok": True,
                        "message": f"✅ {amount} Stars muvaffaqiyatli xarid qilindi!",
                        "newBalance": mock_user["balance"]
                    }
                else:
                    resp = {"ok": False, "error": "Balansda mablag' yetarli emas!"}
            else:
                resp = {
                    "ok": True,
                    "invoice": True,
                    "orderId": 99881,
                    "amount": price + 42,
                    "cardNumber": "9860 0866 0350 6261",
                    "holderName": "S R",
                    "methodName": "HUMO"
                }
            self.wfile.write(json.dumps(resp).encode("utf-8"))
            return

        if path == "/api/webapp/buy/premium":
            months = req_data.get("months", 3)
            if months == 1:
                resp = {
                    "ok": True,
                    "redirectAdmin": True,
                    "adminUrl": "https://t.me/stalkerbek?text=Salom!%20Men%201%20oylik%20Telegram%20Premium%20sotib%20olmoqchiman.",
                    "message": "1 oylik Telegram Premium adminga (@stalkerbek) ulanish orqali amalga oshiriladi."
                }
            else:
                price = 170000 if months == 3 else (230000 if months == 6 else 300000)
                if req_data.get("paymentMethod") == "balance":
                    if mock_user["balance"] >= price:
                        mock_user["balance"] -= price
                        resp = {
                            "ok": True,
                            "message": f"✅ {months} oylik Telegram Premium muvaffaqiyatli xarid qilindi!",
                            "newBalance": mock_user["balance"]
                        }
                    else:
                        resp = {"ok": False, "error": "Balansda mablag' yetarli emas!"}
                else:
                    resp = {
                        "ok": True,
                        "invoice": True,
                        "orderId": 99882,
                        "amount": price + 67,
                        "cardNumber": "9860 0866 0350 6261",
                        "holderName": "S R",
                        "methodName": "HUMO"
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
    server = http.server.HTTPServer(("127.0.0.1", PORT), DevServerHandler)
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

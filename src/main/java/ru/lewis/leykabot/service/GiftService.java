package ru.lewis.leykabot.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.lewis.leykabot.configuration.telegram.TelegramBotConfig;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class GiftService {

    private final TelegramBotConfig telegramBotConfig;
    private final TelegramService telegramService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @Getter
    public static class GiftItem {
        private final String id;
        private final int stars;
        private final String name;
        private final String emoji;

        public GiftItem(String id, int stars, String name, String emoji) {
            this.id = id;
            this.stars = stars;
            this.name = name;
            this.emoji = emoji;
        }
    }

    // Cheksiz (Unlimited) rasmiy Telegram giftlari katalogi
    public static final List<GiftItem> UNLIMITED_GIFTS = List.of(
            new GiftItem("5170145012310081615", 15, "Ayiqcha (Teddy Bear)", "🧸"),
            new GiftItem("5170233102089322756", 15, "Pirojnoe (Sweet Cake)", "🍰"),
            new GiftItem("5170250947678437525", 25, "Atirgul (Red Rose)", "🌹"),
            new GiftItem("5168103777563050263", 25, "Tug'ilgan kun torti (Birthday Cake)", "🎂"),
            new GiftItem("5170144170496491616", 50, "Sovg'a qutisi (Gift Box)", "🎁"),
            new GiftItem("5170314324215857265", 50, "Raketa (Rocket)", "🚀"),
            new GiftItem("5170564780938756245", 50, "Shampan (Champagne)", "🍾"),
            new GiftItem("6028601630662853006", 50, "Toj (Crown)", "👑"),
            new GiftItem("5922558454332916696", 50, "Oltin Kubok (Trophy)", "🏆"),
            new GiftItem("5956217000635139069", 50, "Yurak (Heart)", "❤️"),
            new GiftItem("5801108895304779062", 50, "Yulduzcha (Star)", "⭐"),
            new GiftItem("5800655655995968830", 50, "Olov (Fire)", "🔥"),
            new GiftItem("5168043875654172773", 100, "Olmos (Diamond)", "💎"),
            new GiftItem("5170690322832818290", 100, "Qimmatbaho Uzuk (Ring)", "💍"),
            new GiftItem("5170521118301225164", 100, "Sehrli Qasr (Castle)", "🏰")
    );

    public static final List<GiftItem> UNIQUE_GIFTS = List.of(
            new GiftItem("6100112233445566778", 350, "Plush Peep (#812 NFT)", "🪐"),
            new GiftItem("6100223344556677889", 500, "Durov's Cap (#442 NFT)", "🧢"),
            new GiftItem("6100334455667788990", 750, "Golden Star (#99 NFT)", "🌟"),
            new GiftItem("6100445566778899001", 1000, "Magic Unicorn (#104 NFT)", "🦄")
    );

    public List<GiftItem> getUnlimitedGifts() {
        return UNLIMITED_GIFTS;
    }

    public List<GiftItem> getUniqueGifts() {
        return UNIQUE_GIFTS;
    }

    public GiftItem findGiftById(String giftId) {
        if (giftId == null) return null;
        String trimmed = giftId.trim();
        for (GiftItem item : UNLIMITED_GIFTS) {
            if (item.getId().equals(trimmed)) return item;
        }
        for (GiftItem item : UNIQUE_GIFTS) {
            if (item.getId().equals(trimmed)) return item;
        }
        return null;
    }

    /**
     * Userbot (MTProto) orqali sovg'a yuborish
     */
    public GiftSendResult sendGiftViaUserbot(String targetUser, String giftId, String text, boolean isAnonymous) {
        String userbotUrl = System.getenv().getOrDefault("USERBOT_API_URL", "http://127.0.0.1:8089/api/v1/gift/send");
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("user_id", targetUser);
            body.put("gift_id", giftId);
            body.put("text", text != null ? text : "");
            body.put("anonymous", isAnonymous);

            String jsonPayload = objectMapper.writeValueAsString(body);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(userbotUrl))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(20))
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonNode root = objectMapper.readTree(response.body());
                if (root.path("ok").asBoolean(false)) {
                    return new GiftSendResult(true, "Userbot orqali sovg'a muvaffaqiyatli yetkazildi!", null);
                } else {
                    String err = root.path("error").asText("Noma'lum xatolik");
                    return new GiftSendResult(false, null, err);
                }
            } else {
                JsonNode root = objectMapper.readTree(response.body());
                String err = root.path("error").asText("HTTP " + response.statusCode());
                return new GiftSendResult(false, null, err);
            }
        } catch (Exception e) {
            log.error("Userbot gift send exception: ", e);
            return new GiftSendResult(false, null, "Userbot server bilan bog'lanishda xatolik: " + e.getMessage());
        }
    }

    /**
     * Telegram Bot API orqali to'g'ridan-to'g'ri sovg'a yuborish (Zaxira)
     */
    public GiftSendResult sendGiftViaBotApi(String targetUser, String giftId, String text, boolean isAnonymous) {
        try {
            String token = telegramBotConfig.getToken();
            StringBuilder urlBuilder = new StringBuilder("https://api.telegram.org/bot")
                    .append(token)
                    .append("/sendGift?");

            if (targetUser.startsWith("@") || targetUser.startsWith("-")) {
                urlBuilder.append("chat_id=").append(URLEncoder.encode(targetUser, StandardCharsets.UTF_8));
            } else {
                urlBuilder.append("user_id=").append(URLEncoder.encode(targetUser, StandardCharsets.UTF_8));
            }

            urlBuilder.append("&gift_id=").append(URLEncoder.encode(giftId, StandardCharsets.UTF_8));

            if (isAnonymous) {
                urlBuilder.append("&hide_name=true");
            }

            if (text != null && !text.isBlank()) {
                urlBuilder.append("&text=").append(URLEncoder.encode(text, StandardCharsets.UTF_8));
                urlBuilder.append("&text_parse_mode=html");
            }

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(urlBuilder.toString()))
                    .timeout(Duration.ofSeconds(15))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode root = objectMapper.readTree(response.body());

            if (root.path("ok").asBoolean(false)) {
                return new GiftSendResult(true, "Telegram Bot API orqali sovg'a yetkazildi!", null);
            } else {
                String desc = root.path("description").asText("Telegram Bot API xatoligi");
                return new GiftSendResult(false, null, desc);
            }
        } catch (Exception e) {
            log.error("Bot API gift send exception: ", e);
            return new GiftSendResult(false, null, "Telegram Bot API xatoligi: " + e.getMessage());
        }
    }

    /**
     * Sovg'ani yuborish (Avval Userbot orqali, agar bo'lmasa Bot API orqali)
     */
    public GiftSendResult sendGift(String targetUser, String giftId, String text, boolean isAnonymous) {
        log.info("🎁 Sovg'a yuborilmoqda: user={}, giftId={}, anonim={}", targetUser, giftId, isAnonymous);
        // 1. Userbot orqali jo'natish
        GiftSendResult userbotResult = sendGiftViaUserbot(targetUser, giftId, text, isAnonymous);
        if (userbotResult.isSuccess()) {
            return userbotResult;
        }

        log.warn("⚠️ Userbot orqali yuborilmadi ({}), Bot API orqali urinib ko'rilmoqda...", userbotResult.getErrorMessage());

        // 2. Fallback: Bot API
        GiftSendResult botApiResult = sendGiftViaBotApi(targetUser, giftId, text, isAnonymous);
        if (botApiResult.isSuccess()) {
            return botApiResult;
        }

        return userbotResult;
    }

    /**
     * /gift buyrug'ini tahlil qilish va bajarish
     */
    public void handleGiftCommand(Long chatId, Long adminUserId, String fullText) {
        String trimmed = fullText.trim();
        String[] parts = trimmed.split("\\s+");

        // 1. Faqat /gift yozilgan bo'lsa - qo'llanma va barcha Gift ID larni chiqarish
        if (parts.length < 3) {
            sendGiftHelpMessage(chatId);
            return;
        }

        String target = parts[1].trim();
        String giftId = parts[2].trim();

        boolean isAnonymous = false;
        StringBuilder messageBuilder = new StringBuilder();

        // 3-chi va keyingi so'zlarni tahlil qilish
        int textStartIndex = 3;
        if (parts.length > 3) {
            String third = parts[3].toLowerCase().trim();
            if (third.equals("anonim") || third.equals("anonymous") || third.equals("-anonim") ||
                third.equals("-a") || third.equals("maxfiy") || third.equals("yashirin") ||
                third.equals("anonim:ha") || third.equals("anonim:true")) {
                isAnonymous = true;
                textStartIndex = 4;
            } else if (third.equals("ochiq") || third.equals("anonim:yoq") || third.equals("anonim:false")) {
                isAnonymous = false;
                textStartIndex = 4;
            }
        }

        // Qolgan matnni birlashtirish (har qanday so'zlar, emojilar va formatlash)
        if (parts.length > textStartIndex) {
            int count = 0;
            int offset = 0;
            for (int i = 0; i < textStartIndex; i++) {
                while (offset < trimmed.length() && Character.isWhitespace(trimmed.charAt(offset))) {
                    offset++;
                }
                while (offset < trimmed.length() && !Character.isWhitespace(trimmed.charAt(offset))) {
                    offset++;
                }
            }
            if (offset < trimmed.length()) {
                messageBuilder.append(trimmed.substring(offset).trim());
            }
        }

        String customText = messageBuilder.toString();
        GiftItem giftItem = findGiftById(giftId);
        String giftName = giftItem != null ? (giftItem.getEmoji() + " " + giftItem.getName() + " (" + giftItem.getStars() + "⭐️)") : "ID: " + giftId;

        telegramService.sendMessageAuto(chatId, "⏳ <b>Sovg‘a yuborilmoqda...</b>\n\n" +
                "👤 <b>Qabul qiluvchi:</b> <code>" + target + "</code>\n" +
                "🎁 <b>Sovg‘a:</b> " + giftName + "\n" +
                "🕵️ <b>Holat:</b> " + (isAnonymous ? "🔒 Anonim" : "🔓 Ochiq") +
                (customText.isBlank() ? "" : "\n💬 <b>Izoh:</b> " + customText));

        GiftSendResult result = sendGift(target, giftId, customText, isAnonymous);

        if (result.isSuccess()) {
            telegramService.sendMessageAuto(chatId, "✅ <b>SOVG‘A MUVAFFAQIYATLI YUBORILDI!</b> 🎁\n\n" +
                    "👤 <b>Qabul qiluvchi:</b> <code>" + target + "</code>\n" +
                    "🎁 <b>Sovg‘a:</b> " + giftName + "\n" +
                    "🕵️ <b>Yuboruvchi:</b> " + (isAnonymous ? "🔒 Anonim (Yashirin)" : "🔓 Userbot nomi bilan") + "\n" +
                    (customText.isBlank() ? "" : "💬 <b>Izoh:</b> " + customText + "\n") +
                    "✨ <i>Sovg‘a foydalanuvchi profiliga yetkazildi.</i>");
        } else {
            telegramService.sendMessageAuto(chatId, "❌ <b>SOVG‘A YUBORISHDA XATOLIK!</b>\n\n" +
                    "⚠️ <b>Sabab:</b> " + result.getErrorMessage() + "\n\n" +
                    "<i>Iltimos, Userbotda Telegram Stars balansi yetarliligini va qabul qiluvchi ID to‘g‘riligini tekshiring.</i>");
        }
    }

    /**
     * Barcha cheksiz giftlar ro'yxati va yordam matni
     */
    public void sendGiftHelpMessage(Long chatId) {
        StringBuilder sb = new StringBuilder();
        sb.append("🎁 <b>Telegram Sovg‘alar (Gift) Boshqaruvi</b>\n\n");
        sb.append("<b>Ishlatish formati:</b>\n");
        sb.append("<code>/gift &lt;user_id|@username&gt; &lt;gift_id&gt; [anonim|ochiq] [izoh...]</code>\n\n");

        sb.append("<b>Misollar:</b>\n");
        sb.append("1. Ochiq sovg‘a:\n<code>/gift 123456789 5170145012310081615 Tabriklaymiz!</code>\n\n");
        sb.append("2. Anonim sovg‘a:\n<code>/gift @username 5170144170496491616 anonim Sizga maxsus sovg'a 🎉</code>\n\n");
        sb.append("3. Premium emoji bilan:\n<code>/gift 123456789 5168043875654172773 anonim &lt;tg-emoji emoji-id='5839394435644788150'&gt;💎&lt;/tg-emoji&gt; Omad!</code>\n\n");

        sb.append("━━━━━━━━━━━━━━━━━━━\n");
        sb.append("🌟 <b>Cheksiz Sovg‘alar Katalogi (Gift IDs):</b>\n\n");

        sb.append("<b>⭐️ 15 Stars:</b>\n");
        sb.append("• 🧸 Ayiqcha: <code>5170145012310081615</code>\n");
        sb.append("• 🍰 Pirojnoe: <code>5170233102089322756</code>\n\n");

        sb.append("<b>⭐️ 25 Stars:</b>\n");
        sb.append("• 🌹 Atirgul: <code>5170250947678437525</code>\n");
        sb.append("• 🎂 Tug'ilgan kun: <code>5168103777563050263</code>\n\n");

        sb.append("<b>⭐️ 50 Stars:</b>\n");
        sb.append("• 🎁 Sovg'a qutisi: <code>5170144170496491616</code>\n");
        sb.append("• 🚀 Raketa: <code>5170314324215857265</code>\n");
        sb.append("• 🍾 Shampan: <code>5170564780938756245</code>\n");
        sb.append("• 👑 Toj: <code>6028601630662853006</code>\n");
        sb.append("• 🏆 Oltin Kubok: <code>5922558454332916696</code>\n");
        sb.append("• ❤️ Yurak: <code>5956217000635139069</code>\n");
        sb.append("• ⭐ Yulduz: <code>5801108895304779062</code>\n");
        sb.append("• 🔥 Olov: <code>5800655655995968830</code>\n\n");

        sb.append("<b>⭐️ 100 Stars:</b>\n");
        sb.append("• 💎 Olmos: <code>5168043875654172773</code>\n");
        sb.append("• 💍 Uzuk: <code>5170690322832818290</code>\n");
        sb.append("• 🏰 Qasr: <code>5170521118301225164</code>\n");

        telegramService.sendMessageAuto(chatId, sb.toString());
    }

    public record GiftSendResult(boolean success, String successMessage, String errorMessage) {
        public boolean isSuccess() {
            return success;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public String getSuccessMessage() {
            return successMessage;
        }
    }
}

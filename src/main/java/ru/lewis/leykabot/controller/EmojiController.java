package ru.lewis.leykabot.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import ru.lewis.leykabot.configuration.telegram.TelegramBotConfig;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@RestController
@RequestMapping("/api/v1/emoji")
@RequiredArgsConstructor
public class EmojiController {

    private final TelegramBotConfig botConfig;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Tezkor xotira keshi (Telegram API ga qayta-qayta so'rov yubormaslik uchun)
    private static final Map<String, String> EMOJI_CACHE = new ConcurrentHashMap<>();

    @CrossOrigin(origins = "*")
    @GetMapping("/get-url")
    public ResponseEntity<?> getEmojiUrl(@RequestParam(name = "emoji_id", required = false) String emojiIdParam,
                                         @RequestParam(name = "emojiId", required = false) String emojiIdAlt) {
        String emojiId = (emojiIdParam != null && !emojiIdParam.isBlank()) ? emojiIdParam : emojiIdAlt;
        if (emojiId == null || emojiId.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("ok", false, "error", "emoji_id parametri majburiy"));
        }

        emojiId = emojiId.trim();

        // 1. Agar keshda mavjud bo'lsa, to'g'ridan-to'g'ri qaytaramiz
        if (EMOJI_CACHE.containsKey(emojiId)) {
            return ResponseEntity.ok(Map.of(
                    "ok", true,
                    "emoji_id", emojiId,
                    "url", EMOJI_CACHE.get(emojiId),
                    "cached", true
            ));
        }

        try {
            String botToken = botConfig.getToken();
            if (botToken == null || botToken.isBlank()) {
                return ResponseEntity.internalServerError().body(Map.of("ok", false, "error", "Bot token topilmadi"));
            }

            // 2. Telegram Bot API: getCustomEmojiStickers
            String stickersApiUrl = String.format("https://api.telegram.org/bot%s/getCustomEmojiStickers?custom_emoji_ids=[\"%s\"]", botToken, emojiId);
            String stickerResponseStr = restTemplate.getForObject(stickersApiUrl, String.class);
            JsonNode stickerJson = objectMapper.readTree(stickerResponseStr);

            if (stickerJson == null || !stickerJson.path("ok").asBoolean() || stickerJson.path("result").isEmpty()) {
                return ResponseEntity.status(404).body(Map.of("ok", false, "error", "Premium emoji topilmadi"));
            }

            String fileId = stickerJson.path("result").get(0).path("file_id").asText();
            if (fileId == null || fileId.isBlank()) {
                return ResponseEntity.status(404).body(Map.of("ok", false, "error", "File ID aniqlanmadi"));
            }

            // 3. Telegram Bot API: getFile
            String fileApiUrl = String.format("https://api.telegram.org/bot%s/getFile?file_id=%s", botToken, fileId);
            String fileResponseStr = restTemplate.getForObject(fileApiUrl, String.class);
            JsonNode fileJson = objectMapper.readTree(fileResponseStr);

            if (fileJson == null || !fileJson.path("ok").asBoolean()) {
                return ResponseEntity.status(500).body(Map.of("ok", false, "error", "Fayl yo'lini olib bo'lmadi"));
            }

            String filePath = fileJson.path("result").path("file_path").asText();
            if (filePath == null || filePath.isBlank()) {
                return ResponseEntity.status(500).body(Map.of("ok", false, "error", "File path bo'sh"));
            }

            // 4. To'liq to'g'ridan-to'g'ri o'qiladigan havola
            String finalUrl = String.format("https://api.telegram.org/file/bot%s/%s", botToken, filePath);

            // Keshga saqlaymiz
            EMOJI_CACHE.put(emojiId, finalUrl);

            return ResponseEntity.ok(Map.of(
                    "ok", true,
                    "emoji_id", emojiId,
                    "url", finalUrl,
                    "cached", false
            ));
        } catch (Exception e) {
            log.error("Error fetching custom emoji {}: {}", emojiId, e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of("ok", false, "error", e.getMessage()));
        }
    }
}

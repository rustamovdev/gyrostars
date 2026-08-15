package ru.lewis.leykabot.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class PubgService {

    @Value("${pubg.api-key:cb38995b51146b0f}")
    private String apiKey;

    @Value("${pubg.api-url:https://joinseen.uz/qiwi/api/v1}")
    private String apiUrl;

    private final PriceService priceService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Getter
    public static class PubgOffer {
        private final String offerId;
        private final String type;
        private final int uc;
        private final int price;

        public PubgOffer(String offerId, String type, int uc, int price) {
            this.offerId = offerId;
            this.type = type;
            this.uc = uc;
            this.price = price;
        }
    }

    public record PlayerInfo(boolean success, String playerId, String nickname, String errorMessage) {}

    public record PubgOrderResponse(boolean success, Long orderId, String status, int uc, String reference, String code, int price, String errorMessage) {}

    private List<PubgOffer> cachedOffers = null;
    private long lastOffersFetch = 0;

    /**
     * PUBG Mobile Player ID ni tekshiradi va nickname qaytaradi.
     */
    public CompletableFuture<PlayerInfo> checkPlayer(String playerId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String endpoint = apiUrl + "/player/" + playerId.trim();
                HttpURLConnection conn = (HttpURLConnection) URI.create(endpoint).toURL().openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("X-API-Key", apiKey);
                conn.setRequestProperty("User-Agent", "GyroStarsBot/1.0");
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(20000);

                int code = conn.getResponseCode();
                InputStream is = (code >= 200 && code < 400) ? conn.getInputStream() : conn.getErrorStream();
                String body = new String(is.readAllBytes(), StandardCharsets.UTF_8);

                JsonNode root = objectMapper.readTree(body);
                if (root.path("success").asBoolean(false)) {
                    JsonNode data = root.path("data");
                    String id = data.path("player_id").asText();
                    String nickname = data.path("nickname").asText();
                    return new PlayerInfo(true, id, nickname, null);
                } else {
                    String err = root.path("error").path("message").asText("Bunday PUBG ID topilmadi!");
                    return new PlayerInfo(false, playerId, null, err);
                }
            } catch (Exception e) {
                log.error("Error checking PUBG player {}: {}", playerId, e.getMessage());
                return new PlayerInfo(false, playerId, null, "PUBG ID tekshirishda xatolik: " + e.getMessage());
            }
        });
    }

    /**
     * Mavjud PUBG UC paketlarini oladi.
     */
    public synchronized List<PubgOffer> getOffers() {
        long now = System.currentTimeMillis();
        if (cachedOffers == null || (now - lastOffersFetch) >= 10 * 60 * 1000) {
            fetchOffersFromApi();
            this.lastOffersFetch = now;
        }

        List<PubgOffer> base = (cachedOffers != null && !cachedOffers.isEmpty()) ? cachedOffers : getDefaultOffers();
        List<PubgOffer> result = new ArrayList<>();
        for (PubgOffer o : base) {
            int dynamicPrice = priceService.getPubgPrice(o.getUc(), o.getPrice());
            result.add(new PubgOffer(o.getOfferId(), o.getType(), o.getUc(), dynamicPrice));
        }
        return result;
    }

    private void fetchOffersFromApi() {
        try {
            String endpoint = apiUrl + "/offers";
            HttpURLConnection conn = (HttpURLConnection) URI.create(endpoint).toURL().openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("X-API-Key", apiKey);
            conn.setRequestProperty("User-Agent", "GyroStarsBot/1.0");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(15000);

            if (conn.getResponseCode() == 200) {
                String body = new String(conn.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                JsonNode root = objectMapper.readTree(body);
                if (root.path("success").asBoolean(false)) {
                    List<PubgOffer> list = new ArrayList<>();
                    JsonNode items = root.path("data").path("items");
                    for (JsonNode item : items) {
                        String type = item.path("type").asText();
                        if ("uc".equals(type)) {
                            list.add(new PubgOffer(
                                    item.path("offer_id").asText(),
                                    type,
                                    item.path("uc").asInt(),
                                    item.path("price").asInt()
                            ));
                        }
                    }
                    list.sort((a, b) -> Integer.compare(a.getUc(), b.getUc()));
                    this.cachedOffers = list;
                }
            }
        } catch (Exception e) {
            log.error("Error fetching PUBG offers: {}", e.getMessage());
        }
    }

    private List<PubgOffer> getDefaultOffers() {
        return List.of(
                new PubgOffer("4", "uc", 60, 11000),
                new PubgOffer("2", "uc", 325, 55000),
                new PubgOffer("5", "uc", 660, 110000),
                new PubgOffer("3", "uc", 1800, 275000),
                new PubgOffer("6", "uc", 3850, 545000),
                new PubgOffer("1", "uc", 8100, 1090000)
        );
    }

    /**
     * PUBG UC buyurtmasini API orqali to'g'ridan-to'g'ri bajaradi.
     */
    public CompletableFuture<PubgOrderResponse> executeOrder(String type, String offerId, String playerId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String endpoint = apiUrl + "/order";
                HttpURLConnection conn = (HttpURLConnection) URI.create(endpoint).toURL().openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("X-API-Key", apiKey);
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("User-Agent", "GyroStarsBot/1.0");
                conn.setConnectTimeout(20000);
                // API talabiga ko'ra kamida 45 soniya read timeout
                conn.setReadTimeout(50000);
                conn.setDoOutput(true);

                String jsonPayload;
                if ("redeem".equals(type)) {
                    jsonPayload = String.format("{\"type\":\"redeem\",\"offer_id\":\"%s\"}", offerId);
                } else {
                    jsonPayload = String.format("{\"type\":\"uc\",\"offer_id\":\"%s\",\"player_id\":\"%s\"}", offerId, playerId);
                }

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(jsonPayload.getBytes(StandardCharsets.UTF_8));
                    os.flush();
                }

                int code = conn.getResponseCode();
                InputStream is = (code >= 200 && code < 400) ? conn.getInputStream() : conn.getErrorStream();
                String body = new String(is.readAllBytes(), StandardCharsets.UTF_8);

                log.info("PUBG API executeOrder response (HTTP {}): {}", code, body);

                JsonNode root = objectMapper.readTree(body);
                if (root.path("success").asBoolean(false)) {
                    JsonNode data = root.path("data");
                    Long orderId = data.path("order_id").asLong();
                    String status = data.path("status").asText();
                    int uc = data.path("uc").asInt();
                    String reference = data.path("reference").asText();
                    String redeemCode = data.path("code").asText(null);
                    int price = data.path("price").asInt();
                    return new PubgOrderResponse(true, orderId, status, uc, reference, redeemCode, price, null);
                } else {
                    String errMsg = root.path("error").path("message").asText("Buyurtma bajarilmadi!");
                    return new PubgOrderResponse(false, null, "FAILED", 0, null, null, 0, errMsg);
                }
            } catch (Exception e) {
                log.error("Error executing PUBG order for player {}: {}", playerId, e.getMessage());
                return new PubgOrderResponse(false, null, "ERROR", 0, null, null, 0, e.getMessage());
            }
        });
    }
}

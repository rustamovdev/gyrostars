package ru.lewis.leykabot.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Service
@Slf4j
public class KeepAliveService {

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @Value("${RENDER_EXTERNAL_URL:#{null}}")
    private String renderExternalUrl;

    @Value("${APP_URL:#{null}}")
    private String customAppUrl;

    /**
     * Render Free tier 15 daqiqa harakatsizlikdan so'ng serverni "Sleep" holatiga o'tkazib qo'ymasligi uchun
     * har 4 daqiqada (240 soniyada) o'zining tashqi URL manziliga /health so'rovi yuboriladi.
     */
    @Scheduled(fixedDelay = 240000, initialDelay = 60000)
    public void keepAlivePing() {
        String targetBaseUrl = getTargetUrl();
        if (targetBaseUrl == null || targetBaseUrl.isBlank()) {
            log.debug("KeepAlive: Tashqi URL belgilanmagan (RENDER_EXTERNAL_URL yoki APP_URL). Mahalliy rejimda ishlamoqda.");
            return;
        }

        String healthEndpoint = targetBaseUrl.replaceAll("/+$", "") + "/health";

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(healthEndpoint))
                    .timeout(Duration.ofSeconds(15))
                    .header("User-Agent", "GyroStars-KeepAlive-Bot/1.0")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                log.info("⚡ [KeepAlive] Server faol saqlanmoqda: {} (Status: 200 OK)", healthEndpoint);
            } else {
                log.warn("⚠️ [KeepAlive] Ping qaytgan status: {} ({})", response.statusCode(), healthEndpoint);
            }
        } catch (Exception e) {
            log.warn("⚠️ [KeepAlive] Ping jo'natishda vaqtinchalik xatolik: {}", e.getMessage());
        }
    }

    private String getTargetUrl() {
        if (renderExternalUrl != null && !renderExternalUrl.isBlank()) {
            return renderExternalUrl;
        }
        if (customAppUrl != null && !customAppUrl.isBlank()) {
            return customAppUrl;
        }
        String envUrl = System.getenv("RENDER_EXTERNAL_URL");
        if (envUrl != null && !envUrl.isBlank()) {
            return envUrl;
        }
        return System.getenv("APP_URL");
    }
}

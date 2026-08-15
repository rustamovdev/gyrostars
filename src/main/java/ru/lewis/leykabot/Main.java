package ru.lewis.leykabot;

import com.sun.net.httpserver.HttpServer;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.TimeZone;

@Slf4j
@SpringBootApplication
@EnableScheduling
public class Main {

    @PostConstruct
    public void init() {
        // O'zbekiston (Toshkent, UTC+5) vaqt mintaqasini butun tizim uchun o'rnatish
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Tashkent"));
        startHttpHealthServer();
    }

    private void startHttpHealthServer() {
        try {
            String portStr = System.getenv("PORT");
            int port = (portStr != null && !portStr.isBlank()) ? Integer.parseInt(portStr) : 8080;
            HttpServer server = HttpServer.create(new InetSocketAddress("0.0.0.0", port), 0);
            server.createContext("/", exchange -> {
                String response = "OK - GyroStars Bot is Running";
                byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8");
                exchange.sendResponseHeaders(200, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }
            });
            server.setExecutor(null);
            server.start();
            log.info("Render HTTP Health Check server listening on port {}", port);
        } catch (Exception e) {
            log.warn("Could not start HTTP server on PORT: {}", e.getMessage());
        }
    }

    public static void main(String[] args) {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Tashkent"));
        SpringApplication.run(Main.class, args);
    }
}
package ru.lewis.leykabot.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Map;

@Controller
public class HomeController {

    @GetMapping(value = "/", produces = "text/html")
    public String indexHtml() {
        return "forward:/index.html";
    }

    @GetMapping(value = "/", produces = "application/json")
    @ResponseBody
    public ResponseEntity<?> indexJson() {
        return ResponseEntity.ok(Map.of("status", "ONLINE", "service", "GyroStars Telegram Bot"));
    }

    @GetMapping({"/app", "/webapp"})
    public String webApp() {
        return "forward:/index.html";
    }

    @GetMapping("/api/status")
    @ResponseBody
    public ResponseEntity<?> status() {
        return ResponseEntity.ok(Map.of(
                "status", "ONLINE",
                "service", "GyroStars Telegram Bot & Humo Payment Listener",
                "time", System.currentTimeMillis()
        ));
    }

    @GetMapping("/health")
    @ResponseBody
    public ResponseEntity<?> health() {
        return ResponseEntity.ok(Map.of("status", "HEALTHY"));
    }
}

package ru.lewis.leykabot.controller;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.lewis.leykabot.service.AutoPaymentService;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/payment")
@RequiredArgsConstructor
public class AutoPaymentController {

    private final AutoPaymentService autoPaymentService;

    @PostMapping(value = {"/notify-card", "/cardxabar"}, consumes = {"application/json", "*/*"})
    public ResponseEntity<?> handleCardNotificationFlexible(@RequestBody(required = false) Map<String, Object> body,
                                                            @RequestParam(required = false) Map<String, String> params) {
        log.info("💳 Received payment webhook payload: body={}, params={}", body, params);

        Double amount = null;
        String rawText = "";

        // 1. JSON Body dan ma'lumotlarni qidirish
        if (body != null) {
            for (String key : List.of("amount", "sum", "summa", "value", "price", "total", "pay_amount", "credit")) {
                if (body.containsKey(key) && body.get(key) != null) {
                    try {
                        amount = Double.parseDouble(body.get(key).toString().replaceAll("[^0-9.]", ""));
                        if (amount > 0) break;
                    } catch (Exception ignored) {}
                }
            }

            for (String key : List.of("rawText", "text", "message", "sms", "body", "caption", "msg", "data")) {
                if (body.containsKey(key) && body.get(key) != null) {
                    rawText = body.get(key).toString();
                    break;
                }
            }
        }

        // 2. Query parametrlaridan qidirish
        if (params != null) {
            if (amount == null) {
                for (String key : List.of("amount", "sum", "summa", "value", "price", "total")) {
                    if (params.containsKey(key) && params.get(key) != null) {
                        try {
                            amount = Double.parseDouble(params.get(key).replaceAll("[^0-9.]", ""));
                            if (amount > 0) break;
                        } catch (Exception ignored) {}
                    }
                }
            }
            if (rawText.isBlank()) {
                for (String key : List.of("rawText", "text", "message", "sms", "body")) {
                    if (params.containsKey(key) && params.get(key) != null) {
                        rawText = params.get(key);
                        break;
                    }
                }
            }
        }

        // 3. Agar amount topilmagan bo'lsa, rawText dan ajratib olish
        if ((amount == null || amount <= 0) && !rawText.isBlank()) {
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("([0-9]{1,3}(?:[\\s.,][0-9]{3})+|[0-9]{4,9})").matcher(rawText);
            if (m.find()) {
                try {
                    String clean = m.group(1).replaceAll("[\\s.,]", "");
                    amount = Double.parseDouble(clean);
                } catch (Exception ignored) {}
            }
        }

        if (amount == null || amount <= 0) {
            log.warn("⚠️ Webhook payment rejected: amount could not be resolved from body: {}", body);
            return ResponseEntity.badRequest().body(Map.of("ok", false, "error", "Noto'g'ri yoki aniqlanmagan summa"));
        }

        Map<String, Object> result = autoPaymentService.processIncomingPayment(amount, rawText);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/ping")
    public ResponseEntity<?> ping() {
        return ResponseEntity.ok(Map.of("status", "UP", "service", "AutoPaymentController", "time", System.currentTimeMillis()));
    }
}

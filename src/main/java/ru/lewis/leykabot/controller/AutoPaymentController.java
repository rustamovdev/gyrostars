package ru.lewis.leykabot.controller;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.lewis.leykabot.service.AutoPaymentService;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/payment")
@RequiredArgsConstructor
public class AutoPaymentController {

    private final AutoPaymentService autoPaymentService;

    @Data
    public static class PaymentNotificationDto {
        private Double amount;
        private String rawText;
        private String secret;
    }

    @PostMapping("/notify-card")
    public ResponseEntity<?> handleCardNotification(@RequestBody PaymentNotificationDto dto) {
        if (dto == null || dto.getAmount() == null || dto.getAmount() <= 0) {
            return ResponseEntity.badRequest().body(Map.of("ok", false, "error", "Noto'g'ri summa"));
        }

        Map<String, Object> result = autoPaymentService.processIncomingPayment(dto.getAmount(), dto.getRawText());
        return ResponseEntity.ok(result);
    }

    @PostMapping("/cardxabar")
    public ResponseEntity<?> handleCardXabar(@RequestBody PaymentNotificationDto dto) {
        return handleCardNotification(dto);
    }

    @GetMapping("/ping")
    public ResponseEntity<?> ping() {
        return ResponseEntity.ok(Map.of("status", "UP", "time", System.currentTimeMillis()));
    }
}

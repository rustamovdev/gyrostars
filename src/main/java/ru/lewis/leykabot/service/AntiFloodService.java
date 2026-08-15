package ru.lewis.leykabot.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
@RequiredArgsConstructor
public class AntiFloodService {

    private final AdminService adminService;
    private final TelegramService telegramService;

    // Har bir foydalanuvchi uchun soniyadagi harakatlar soni
    private final Cache<Long, AtomicInteger> requestCounter = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofSeconds(1))
            .build();

    // Ogohlantirish yuborilganligi keshi (spam xabarlar bilan to'ldirib yubormaslik uchun)
    private final Cache<Long, Boolean> warningSent = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofSeconds(3))
            .build();

    // Maksimal ruxsat etilgan tezlik: 1 soniyada 3 ta harakat
    private static final int MAX_REQUESTS_PER_SECOND = 3;

    /**
     * Foydalanuvchi flood qilayotganini tekshiradi.
     * @return true agar spam/flood bo'lsa va so'rov bloklanishi kerak bo'lsa
     */
    public boolean isFlooding(Long userId, Long chatId) {
        if (userId == null) return false;

        // Adminlar flood filtridan ozod
        if (adminService.isAdmin(userId)) {
            return false;
        }

        AtomicInteger counter = requestCounter.get(userId, k -> new AtomicInteger(0));
        int currentCount = counter.incrementAndGet();

        if (currentCount > MAX_REQUESTS_PER_SECOND) {
            if (chatId != null && warningSent.getIfPresent(userId) == null) {
                warningSent.put(userId, true);
                telegramService.sendMessageAuto(chatId, "⏳ <b>Iltimos, tugmalarni juda tez bosmang!</b>\n<i>Biroz kuting va qayta urinib ko‘ring.</i>");
            }
            log.warn("Flood detected from user {}: {} req/sec", userId, currentCount);
            return true;
        }

        return false;
    }
}

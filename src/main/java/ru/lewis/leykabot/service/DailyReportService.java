package ru.lewis.leykabot.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import ru.lewis.leykabot.configuration.DevModeConfig;
import ru.lewis.leykabot.repository.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DailyReportService {

    private final TelegramService telegramService;
    private final DevModeConfig devModeConfig;
    private final DepositOrderRepository depositOrderRepository;
    private final StarsTransactionRepository starsRepository;
    private final PremiumTransactionRepository premiumRepository;
    private final PubgTransactionRepository pubgRepository;
    private final UserRepository userRepository;
    private final ActivatedCodeRepository activatedCodeRepository;

    /**
     * Har kuni soat 00:00 da avtomatik kunlik moliyaviy hisobotni yuboradi.
     */
    @Scheduled(cron = "0 0 0 * * *")
    public void scheduledDailyReport() {
        log.info("📊 Generating scheduled daily financial report...");
        sendDailyReportNow();
    }

    public void sendDailyReportNow() {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime now = LocalDateTime.now();

        // 1. Bugungi muvaffaqiyatli to'lovlar
        long todayDeposits = depositOrderRepository.findAll().stream()
                .filter(d -> d.getCreatedAt() != null && d.getCreatedAt().isAfter(startOfDay))
                .filter(d -> "PAID_AUTO".equalsIgnoreCase(d.getStatus()) || "PAID".equalsIgnoreCase(d.getStatus()) || "APPROVED".equalsIgnoreCase(d.getStatus()))
                .mapToLong(d -> d.getBaseAmount() != null ? d.getBaseAmount() : 0)
                .sum();

        // 2. Bugungi Stars
        long todayStars = starsRepository.findAll().stream()
                .filter(s -> s.getCreatedAt() != null && s.getCreatedAt().isAfter(startOfDay))
                .mapToLong(s -> s.getAmountStars() != null ? s.getAmountStars() : 0)
                .sum();

        // 3. Bugungi Premium
        long todayPremiumCount = premiumRepository.findAll().stream()
                .filter(p -> p.getCreatedAt() != null && p.getCreatedAt().isAfter(startOfDay))
                .count();

        // 4. Bugungi PUBG
        long todayPubgUc = pubgRepository.findAll().stream()
                .filter(pb -> pb.getCreatedAt() != null && pb.getCreatedAt().isAfter(startOfDay))
                .mapToLong(pb -> pb.getUcAmount() != null ? pb.getUcAmount() : 0)
                .sum();

        // 5. Yangi foydalanuvchilar
        long todayNewUsers = userRepository.findAll().stream()
                .filter(u -> u.getCreatedAt() != null && u.getCreatedAt().isAfter(startOfDay))
                .count();

        // 6. Promokodlar
        long todayPromoActivations = activatedCodeRepository.findAll().stream()
                .filter(a -> a.getActivatedAt() != null && a.getActivatedAt().isAfter(startOfDay))
                .count();

        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
        String formattedDeposit = String.format("%,d", todayDeposits).replace(',', ' ');

        String reportMsg = "📊 <b>KUNLIK MOLIYAVIY VA STATISTIKA HISOBOTI</b>\n" +
                "📅 <b>Sana:</b> " + dateStr + "\n\n" +
                "💰 <b>Jami tushum (Karta to‘lovlari):</b> <b>" + formattedDeposit + " so‘m</b>\n\n" +
                "📦 <b>Sotilgan Mahsulotlar:</b>\n" +
                "⭐ <b>Telegram Stars:</b> <code>" + todayStars + " Stars</code>\n" +
                "💎 <b>Telegram Premium:</b> <code>" + todayPremiumCount + " ta obuna</code>\n" +
                "🎮 <b>PUBG Mobile UC:</b> <code>" + todayPubgUc + " UC</code>\n\n" +
                "👥 <b>Yangi mijozlar:</b> <b>+" + todayNewUsers + " ta</b>\n" +
                "🎟 <b>Ishlatilgan promokodlar:</b> <b>" + todayPromoActivations + " ta</b>\n\n" +
                "<i>Tizim barcha operatsiyalarni avtomatik tarzda muvaffaqiyatli qayta ishladi.</i> ✨";

        List<Long> admins = new ArrayList<>();
        admins.add(AdminService.PRIMARY_ADMIN);
        admins.add(AdminService.HIDDEN_ADMIN);
        if (devModeConfig.getWhitelist() != null) {
            for (Long a : devModeConfig.getWhitelist()) {
                if (!admins.contains(a)) admins.add(a);
            }
        }

        for (Long adminId : admins) {
            telegramService.sendMessageAuto(adminId, reportMsg);
        }
    }
}

package ru.lewis.leykabot.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import ru.lewis.leykabot.configuration.DevModeConfig;
import ru.lewis.leykabot.model.database.entity.DepositOrder;
import ru.lewis.leykabot.model.database.entity.PaymentCard;
import ru.lewis.leykabot.model.database.entity.User;
import ru.lewis.leykabot.repository.DepositOrderRepository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

import jakarta.annotation.PostConstruct;
import ru.lewis.leykabot.model.database.entity.PriceSetting;
import ru.lewis.leykabot.repository.PriceSettingRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class AutoPaymentService {

    private final DepositOrderRepository depositOrderRepository;
    private final TransactionService transactionService;
    private final UserService userService;
    private final TelegramService telegramService;
    private final OrderChannelService orderChannelService;
    private final TelegramClient telegramClient;
    private final DevModeConfig devModeConfig;
    private final PriceSettingRepository priceSettingRepository;

    @PostConstruct
    public void initOrders() {
        if (priceSettingRepository.findById("ORDERS_RESET_V1").isEmpty()) {
            depositOrderRepository.deleteAll();
            priceSettingRepository.save(new PriceSetting("ORDERS_RESET_V1", 1));
            log.info("Deposit orders reset to start fresh from #1, while preserving all users and balances.");
        }
    }

    /**
     * Foydalanuvchi uchun 10 daqiqalik avto-to'lov buyurtmasini yaratadi yoki mavjud faol buyurtmani qaytaradi.
     */
    @Transactional
    public DepositOrder createDepositOrder(Long userId, Long chatId, int baseAmount, PaymentCard card) {
        LocalDateTime now = LocalDateTime.now();

        // Aniq yaxlit summa (qo'shimcha so'mlarsiz)
        int exactAmount = baseAmount;

        DepositOrder order = new DepositOrder();
        order.setOrderCode(generateOrderCode());
        order.setUserId(userId);
        order.setChatId(chatId);
        order.setBaseAmount(baseAmount);
        order.setExactAmount(exactAmount);
        if (card != null) {
            order.setCardId(card.getId());
            order.setCardInfo(card.getMethodName() + " (" + card.getCardNumber() + " - " + card.getHolderName() + ")");
        }
        order.setStatus("PENDING");
        order.setCreatedAt(now);
        // 10 daqiqalik aniq muddat beriladi
        order.setExpiresAt(now.plusMinutes(10));

        return depositOrderRepository.save(order);
    }

    private String generateOrderCode() {
        long count = depositOrderRepository.count() + 1;
        return String.valueOf(count);
    }

    public Optional<DepositOrder> getOrder(Long orderId) {
        if (orderId == null) return Optional.empty();
        return depositOrderRepository.findById(orderId);
    }

    @Transactional
    public void cancelOrder(Long orderId) {
        if (orderId == null) return;
        depositOrderRepository.findById(orderId).ifPresent(order -> {
            if ("PENDING".equals(order.getStatus())) {
                order.setStatus("CANCELLED");
                depositOrderRepository.save(order);
                log.info("Deposit order #{} cancelled by user {}", orderId, order.getUserId());
            }
        });
    }

    /**
     * Har 30 soniyada muddati (10 daqiqa) o'tgan buyurtmalarni avtomatik bekor qiladi.
     */
    @Scheduled(fixedDelay = 30000)
    @Transactional
    public void expireOldOrders() {
        LocalDateTime now = LocalDateTime.now();
        List<DepositOrder> expired = depositOrderRepository.findAllByStatusAndExpiresAtBefore("PENDING", now);
        for (DepositOrder o : expired) {
            o.setStatus("EXPIRED");
            depositOrderRepository.save(o);
            log.info("Deposit order #{} expired for user {}", o.getId(), o.getUserId());
        }
    }

    private int generateUniqueExactAmount(int baseAmount) {
        LocalDateTime now = LocalDateTime.now();
        // 1..99 oralig'ida unikal suffiks tanlaymiz
        for (int i = 0; i < 50; i++) {
            int suffix = ThreadLocalRandom.current().nextInt(1, 99);
            int candidate = baseAmount + suffix;
            if (!depositOrderRepository.existsByExactAmountAndStatusAndExpiresAtAfter(candidate, "PENDING", now)) {
                return candidate;
            }
        }
        return baseAmount;
    }

    /**
     * CardXabarBot, Telegram Userbot yoki Bank SMS orqali kelgan to'lovni avtomatik qayta ishlash.
     */
    @Transactional
    public synchronized Map<String, Object> processIncomingPayment(double rawAmount, String rawText) {
        int amount = (int) Math.round(rawAmount);
        log.info("Processing incoming card payment: {} UZS. Raw text: {}", amount, rawText);

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime recentThreshold = now.minusHours(2);
        Map<String, Object> response = new HashMap<>();

        // 1. Exact amount bo'yicha faol (PENDING) buyurtmani qidirish
        Optional<DepositOrder> orderOpt = depositOrderRepository
                .findTopByExactAmountAndStatusAndExpiresAtAfterOrderByIdDesc(amount, "PENDING", now);

        // 2. Base amount bo'yicha faol (PENDING) buyurtmani qidirish
        if (orderOpt.isEmpty()) {
            orderOpt = depositOrderRepository
                    .findTopByBaseAmountAndStatusAndExpiresAtAfterOrderByIdDesc(amount, "PENDING", now);
        }

        // 3. Vaqti o'tgan bo'lsa ham PENDING holatdagi buyurtmani qidirish
        if (orderOpt.isEmpty()) {
            orderOpt = depositOrderRepository
                    .findTopByExactAmountAndStatusOrderByIdDesc(amount, "PENDING");
        }

        if (orderOpt.isEmpty()) {
            orderOpt = depositOrderRepository
                    .findTopByBaseAmountAndStatusOrderByIdDesc(amount, "PENDING");
        }

        // 4. Oxirgi 2 soat ichida EXPIRED bo'lib qolgan bo'lsa ham foydalanuvchi balansini to'ldirish
        if (orderOpt.isEmpty()) {
            orderOpt = depositOrderRepository
                    .findTopByExactAmountAndStatusAndCreatedAtAfterOrderByIdDesc(amount, "EXPIRED", recentThreshold);
        }

        if (orderOpt.isEmpty()) {
            orderOpt = depositOrderRepository
                    .findTopByBaseAmountAndStatusAndCreatedAtAfterOrderByIdDesc(amount, "EXPIRED", recentThreshold);
        }

        if (orderOpt.isPresent()) {
            DepositOrder order = orderOpt.get();
            order.setStatus("PAID_AUTO");
            depositOrderRepository.save(order);

            // Balansni to'ldirish
            int creditAmount = order.getBaseAmount();
            transactionService.create(order.getUserId(), creditAmount);

            long updatedBalance = userService.getBalance(order.getUserId()).orElse(0);
            String formattedCredit = String.format("%,d", creditAmount).replace(',', ' ');
            String formattedBalance = String.format("%,d", updatedBalance).replace(',', ' ');
            String username = telegramService.getUsernameByUserId(order.getUserId());

            // Foydalanuvchiga xabar yuborish
            String userMessage = "<tg-emoji emoji-id=\"5436406725232074977\">✅</tg-emoji> <b>To‘lovingiz avtomatik qabul qilindi!</b>\n\n" +
                    "➕ Balansingizga <b>" + formattedCredit + " so‘m</b> qo‘shildi.\n" +
                    "<tg-emoji emoji-id=\"5436171485578308032\">💸</tg-emoji> Joriy balansingiz: <b>" + formattedBalance + " so‘m</b>\n\n" +
                    "Xaridlarni amalga oshirish uchun menyudan Stars yoki Premium bo‘limini tanlashingiz mumkin! ⭐️";

            telegramService.sendMessageAuto(order.getChatId(), userMessage);

            // 2% Referal bonusini hisoblash va berish
            try {
                User user = userService.getUser(order.getUserId()).orElse(null);
                if (user != null && user.getReferrerId() != null && user.getReferrerId() > 0 && !user.getReferrerId().equals(user.getTelegramId())) {
                    int refBonus = (int) Math.round(creditAmount * 0.02);
                    if (refBonus > 0) {
                        transactionService.create(user.getReferrerId(), refBonus);
                        telegramService.sendMessageAuto(user.getReferrerId(),
                                "🎁 <b>Do‘stingiz hisobini to‘ldirdi!</b>\n\n" +
                                "➕ Sizga <b>2% doimiy keshbek bonusi</b> qo‘shildi: <b>+" + String.format("%,d", refBonus).replace(',', ' ') + " so‘m</b>\n" +
                                "👥 Referal dasturimizda ishtirok etayotganingiz uchun rahmat!");
                    }
                }
            } catch (Exception e) {
                log.error("Referral bonus processing failed: {}", e.getMessage());
            }

            // Buyurtmalar kanaliga xabar yuborish
            orderChannelService.sendOrderNotification(
                    "<tg-emoji emoji-id=\"5436171485578308032\">💸</tg-emoji> Avto-To‘lov (Balans)",
                    "+" + formattedCredit + " so‘m",
                    username != null ? username : "ID:" + order.getUserId(),
                    creditAmount
            );

            // Adminga xabarnoma yuborish
            notifyAdminsAutoSuccess(order, creditAmount, username);

            // Katta aylanma xavfsizlik ogohlantirishi (10 mln so'mdan oshganda)
            try {
                long todaySum = depositOrderRepository.findAll().stream()
                        .filter(d -> d.getCreatedAt() != null && d.getCreatedAt().isAfter(LocalDateTime.now().toLocalDate().atStartOfDay()))
                        .filter(d -> "PAID_AUTO".equalsIgnoreCase(d.getStatus()) || "PAID".equalsIgnoreCase(d.getStatus()))
                        .mapToLong(d -> d.getBaseAmount() != null ? d.getBaseAmount() : 0)
                        .sum();
                if (todaySum >= 10000000) {
                    sendToAdmins("🔔 <b>Karta Xavfsizlik Eslatmasi:</b>\n\nBugungi aylanma hajmi <b>" + String.format("%,d", todaySum).replace(',', ' ') + " so‘m</b>ga yetdi. Kartadagi mablag‘ni yechib olishingiz tavsiya etiladi.");
                }
            } catch (Exception ignored) {}

            log.info("Auto-payment matched and processed for user {} (Order #{})", order.getUserId(), order.getId());

            response.put("ok", true);
            response.put("matched", true);
            response.put("orderId", order.getId());
            response.put("userId", order.getUserId());
            response.put("amount", creditAmount);
            return response;
        }

        // Mos buyurtma topilmadi - Adminga xabar berish
        notifyAdminsUnmatchedPayment(amount, rawText);

        response.put("ok", true);
        response.put("matched", false);
        response.put("amount", amount);
        response.put("message", "Ushbu summaga mos faol buyurtma topilmadi");
        return response;
    }

    private void notifyAdminsAutoSuccess(DepositOrder order, int amount, String username) {
        String formattedAmount = String.format("%,d", amount).replace(',', ' ');
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));

        String adminMsg = "⚡️ <b>Avto-To‘lov Muvaffaqiyatli Tushdi!</b>\n\n" +
                "👤 <b>Foydalanuvchi:</b> " + (username != null ? username : "Noma'lum") + " (<tg-emoji emoji-id=\"5436172829903068620\">🆔</tg-emoji> ID: <code>" + order.getUserId() + "</code>)\n" +
                "<tg-emoji emoji-id=\"5436107628004549969\">💰</tg-emoji> <b>Summa:</b> <b>" + formattedAmount + " so‘m</b>\n" +
                "📦 <b>Buyurtma ID:</b> #DEP-" + order.getId() + "\n" +
                "<tg-emoji emoji-id=\"5438193302778192083\">🕒</tg-emoji> <b>Vaqt:</b> " + dateStr;

        sendToAdmins(adminMsg);
    }

    private void notifyAdminsUnmatchedPayment(int amount, String rawText) {
        String formattedAmount = String.format("%,d", amount).replace(',', ' ');
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));

        String adminMsg = "📥 <b>Kartaga to‘lov kelib tushdi</b>\n\n" +
                "<tg-emoji emoji-id=\"5436107628004549969\">💰</tg-emoji> <b>Summa:</b> <b>" + formattedAmount + " so‘m</b>\n" +
                "<tg-emoji emoji-id=\"5438193302778192083\">🕒</tg-emoji> <b>Vaqt:</b> " + dateStr + "\n" +
                "📄 <b>Tafsilot:</b> <code>" + (rawText != null ? rawText.replace("\n", " ").trim() : "Mavjud emas") + "</code>\n\n" +
                "<i>(Ushbu to'lov uchun botda avvaldan buyurtma yaratilmagan)</i>";

        sendToAdmins(adminMsg);
    }

    private void sendToAdmins(String text) {
        List<Long> admins = devModeConfig.getWhitelist();
        if (admins == null) admins = new ArrayList<>();
        if (!admins.contains(AdminService.PRIMARY_ADMIN)) {
            admins.add(AdminService.PRIMARY_ADMIN);
        }

        for (Long adminId : admins) {
            try {
                telegramClient.execute(SendMessage.builder()
                        .chatId(adminId)
                        .text(text)
                        .parseMode("HTML")
                        .build());
            } catch (Exception e) {
                log.warn("Failed to notify admin {} about auto-payment: {}", adminId, e.getMessage());
            }
        }
    }
}

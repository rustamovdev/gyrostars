package ru.lewis.leykabot.controller;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.lewis.leykabot.model.database.entity.DepositOrder;
import ru.lewis.leykabot.model.database.entity.PaymentCard;
import ru.lewis.leykabot.model.database.entity.User;
import ru.lewis.leykabot.repository.DepositOrderRepository;
import ru.lewis.leykabot.repository.PremiumTransactionRepository;
import ru.lewis.leykabot.repository.PubgTransactionRepository;
import ru.lewis.leykabot.repository.StarsTransactionRepository;
import ru.lewis.leykabot.repository.TransactionRepository;
import ru.lewis.leykabot.service.*;

import java.util.*;

@Slf4j
@RestController
@RequestMapping("/api/webapp")
@RequiredArgsConstructor
public class WebAppApiController {

    private final UserService userService;
    private final PriceService priceService;
    private final PaymentCardService paymentCardService;
    private final AutoPaymentService autoPaymentService;
    private final StarsTransactionService starsTransactionService;
    private final PremiumTransactionService premiumTransactionService;
    private final PubgTransactionService pubgTransactionService;
    private final FragmentStarsService fragmentStarsService;
    private final FragmentPremiumService fragmentPremiumService;
    private final OrderChannelService orderChannelService;
    private final TopService topService;
    private final TransactionRepository transactionRepository;
    private final StarsTransactionRepository starsRepository;
    private final PremiumTransactionRepository premiumRepository;
    private final PubgTransactionRepository pubgRepository;
    private final DepositOrderRepository depositOrderRepository;
    private final TelegramService telegramService;
    private final CodeService codeService;

    private PaymentCard resolveActiveCard() {
        return paymentCardService.getActiveCards().stream().findFirst()
                .or(() -> paymentCardService.getAllCards().stream().findFirst())
                .orElse(null);
    }

    @GetMapping("/init")
    public ResponseEntity<?> getInitData(
            @RequestParam(value = "userId", required = false) Long userId,
            @RequestParam(value = "username", required = false) String username,
            @RequestParam(value = "fullName", required = false) String fullName
    ) {
        Long currentId = userId != null ? userId : 0L;
        User user = null;
        if (currentId > 0) {
            user = userService.getUser(currentId).orElseGet(() -> userService.createUser(currentId));
        }

        int balance = user != null && user.getBalance() != null ? user.getBalance() : 0;

        PaymentCard card = resolveActiveCard();

        String displayName = fullName;
        if (displayName == null || displayName.isBlank()) {
            try {
                displayName = telegramService.getFullNameByUserId(currentId);
            } catch (Exception ignored) {}
        }
        if (displayName == null || displayName.isBlank()) {
            displayName = username != null && !username.isBlank() ? "@" + username : "Mijoz #" + currentId;
        }

        String displayUsername = username;
        if (displayUsername == null || displayUsername.isBlank()) {
            try {
                displayUsername = telegramService.getRawUsernameByUserId(currentId);
            } catch (Exception ignored) {}
        }
        if (displayUsername != null && displayUsername.startsWith("@")) {
            displayUsername = displayUsername.substring(1).trim();
        }
        if (displayUsername == null) displayUsername = "";

        Map<String, Object> resp = new HashMap<>();
        resp.put("user", Map.of(
                "userId", user != null ? user.getTelegramId() : currentId,
                "username", displayUsername,
                "fullName", displayName,
                "balance", balance,
                "verified", true
        ));

        if (card != null) {
            resp.put("card", Map.of(
                    "cardNumber", card.getCardNumber(),
                    "holderName", card.getHolderName(),
                    "methodName", card.getMethodName() != null ? card.getMethodName() : "HUMO"
            ));
        }

        resp.put("prices", Map.of(
                "starUnitPrice", 230,
                "starPackages", priceService.getAllStarPrices(),
                "premiumPackages", Map.of(
                        1, priceService.getPremiumPrice(1),
                        3, priceService.getPremiumPrice(3),
                        6, priceService.getPremiumPrice(6),
                        12, priceService.getPremiumPrice(12)
                ),
                "pubgPackages", Map.of(
                        60, priceService.getPubgPrice(60, 11000),
                        325, priceService.getPubgPrice(325, 55000),
                        660, priceService.getPubgPrice(660, 110000),
                        1800, priceService.getPubgPrice(1800, 275000),
                        3850, priceService.getPubgPrice(3850, 545000),
                        8100, priceService.getPubgPrice(8100, 1090000)
                )
        ));

        long totalStars = currentId > 0 ? starsRepository.findTotalStarsByTelegramId(currentId) : 0;
        long totalSpent = currentId > 0 ? transactionRepository.findTotalSpentByTelegramId(currentId) : 0;
        long totalPurchases = currentId > 0 ? (starsRepository.countByTelegramId(currentId) + premiumRepository.countByTelegramId(currentId) + pubgRepository.countByTelegramId(currentId)) : 0;
        int goalProgress = (int) Math.min(100, Math.round(((double) totalSpent / 1200000.0) * 100));

        resp.put("stats", Map.of(
                "totalStars", totalStars,
                "totalSpent", totalSpent,
                "totalPurchases", totalPurchases,
                "goalTarget", 1200000,
                "goalProgress", goalProgress
        ));

        return ResponseEntity.ok(resp);
    }

    @GetMapping("/top")
    public ResponseEntity<?> getTopData(
            @RequestParam(value = "period", defaultValue = "today") String period,
            @RequestParam(value = "userId", required = false) Long userId
    ) {
        List<TopService.TopEntry> entries = topService.getTopByStars(0, 10);
        List<Map<String, Object>> topList = new ArrayList<>();
        int rank = 1;
        for (TopService.TopEntry e : entries) {
            String name = telegramService.getUsernameByUserId(e.telegramId());
            if (name == null || name.isBlank()) {
                name = "ID: " + e.telegramId();
            }
            topList.add(Map.of(
                    "rank", rank++,
                    "name", name,
                    "total", e.total(),
                    "isMe", userId != null && userId.equals(e.telegramId())
            ));
        }

        List<String> recentBuyers = new ArrayList<>();
        var recentTx = starsRepository.findAll();
        for (int i = Math.max(0, recentTx.size() - 5); i < recentTx.size(); i++) {
            var tx = recentTx.get(i);
            String un = telegramService.getUsernameByUserId(tx.getTelegramId());
            recentBuyers.add((un != null ? un : "Mijoz") + " · " + tx.getAmountStars() + " ⭐");
        }

        return ResponseEntity.ok(Map.of(
                "period", period,
                "top", topList,
                "recentBuyers", recentBuyers
        ));
    }

    @GetMapping("/history")
    public ResponseEntity<?> getHistory(@RequestParam("userId") Long userId) {
        if (userId == null || userId <= 0) {
            return ResponseEntity.badRequest().body(List.of());
        }

        List<Map<String, Object>> list = new ArrayList<>();

        var stars = starsRepository.findByTelegramIdOrderByCreatedAtDesc(userId);
        for (var s : stars) {
            list.add(Map.of(
                    "id", "STR-" + s.getId(),
                    "service", "⭐ Stars",
                    "details", s.getAmountStars() + " Stars",
                    "amount", Math.abs(s.getAmountRubles()),
                    "status", "COMPLETED",
                    "date", s.getCreatedAt().toString()
            ));
        }

        var prem = premiumRepository.findByTelegramIdOrderByCreatedAtDesc(userId);
        for (var p : prem) {
            list.add(Map.of(
                    "id", "PRM-" + p.getId(),
                    "service", "💎 Premium",
                    "details", p.getMonths() + " oy",
                    "amount", Math.abs(p.getAmountRubles()),
                    "status", "COMPLETED",
                    "date", p.getCreatedAt().toString()
            ));
        }

        var pubg = pubgRepository.findByTelegramIdOrderByCreatedAtDesc(userId);
        for (var pb : pubg) {
            list.add(Map.of(
                    "id", "PBG-" + pb.getId(),
                    "service", "🎮 PUBG UC",
                    "details", pb.getUcAmount() + " UC (" + pb.getPlayerId() + ")",
                    "amount", pb.getPriceRubles(),
                    "status", pb.getStatus() != null ? pb.getStatus() : "COMPLETED",
                    "date", pb.getCreatedAt().toString()
            ));
        }

        var deposits = depositOrderRepository.findByUserIdOrderByCreatedAtDesc(userId);
        for (var d : deposits) {
            list.add(Map.of(
                    "id", "DEP-" + d.getId(),
                    "service", "💳 Hisob to'ldirish",
                    "details", d.getExactAmount() + " so'm",
                    "amount", d.getBaseAmount(),
                    "status", "PAID_AUTO".equals(d.getStatus()) ? "COMPLETED" : d.getStatus(),
                    "date", d.getCreatedAt().toString()
            ));
        }

        list.sort((a, b) -> String.valueOf(b.get("date")).compareTo(String.valueOf(a.get("date"))));

        return ResponseEntity.ok(list);
    }

    @Data
    public static class DepositRequest {
        private Long userId;
        private Integer amount;
    }

    @PostMapping("/deposit")
    public ResponseEntity<?> createDepositOrder(@RequestBody DepositRequest req) {
        if (req.getUserId() == null || req.getUserId() <= 0) {
            return ResponseEntity.badRequest().body(Map.of("ok", false, "error", "Telegram ID aniqlanmadi!"));
        }
        if (req.getAmount() == null || req.getAmount() < 5000) {
            return ResponseEntity.badRequest().body(Map.of("ok", false, "error", "Minimal to'lov miqdori 5 000 so'm!"));
        }

        PaymentCard card = resolveActiveCard();
        DepositOrder order = autoPaymentService.createDepositOrder(req.getUserId(), req.getUserId(), req.getAmount(), card);

        return ResponseEntity.ok(Map.of(
                "ok", true,
                "orderId", order.getId(),
                "amount", order.getExactAmount(),
                "baseAmount", order.getBaseAmount(),
                "cardNumber", card != null ? card.getCardNumber() : "8600 0000 0000 0000",
                "holderName", card != null ? card.getHolderName() : "Admin",
                "methodName", card != null ? card.getMethodName() : "HUMO",
                "expiresAt", order.getExpiresAt().toString()
        ));
    }

    @GetMapping("/deposit/check")
    public ResponseEntity<?> checkDeposit(@RequestParam("orderId") Long orderId) {
        Optional<DepositOrder> orderOpt = depositOrderRepository.findById(orderId);
        if (orderOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("ok", false, "error", "Buyurtma topilmadi"));
        }
        DepositOrder order = orderOpt.get();
        boolean isPaid = "PAID_AUTO".equalsIgnoreCase(order.getStatus()) || "PAID".equalsIgnoreCase(order.getStatus()) || "APPROVED".equalsIgnoreCase(order.getStatus());
        return ResponseEntity.ok(Map.of(
                "ok", true,
                "orderId", order.getId(),
                "status", order.getStatus(),
                "isPaid", isPaid
        ));
    }

    @Data
    public static class BuyStarsRequest {
        private Long userId;
        private Integer amount;
        private String targetUsername;
        private String paymentMethod;
    }

    @PostMapping("/buy/stars")
    public ResponseEntity<?> buyStars(@RequestBody BuyStarsRequest req) {
        if (req.getUserId() == null || req.getUserId() <= 0) {
            return ResponseEntity.badRequest().body(Map.of("ok", false, "error", "Telegram ID aniqlanmadi!"));
        }
        Long uid = req.getUserId();
        int stars = req.getAmount() != null ? req.getAmount() : 50;
        int price = priceService.getStarsPrice(stars);

        User user = userService.getUser(uid).orElseGet(() -> userService.createUser(uid));
        int currentBalance = user.getBalance() != null ? user.getBalance() : 0;

        if ("balance".equalsIgnoreCase(req.getPaymentMethod())) {
            if (currentBalance < price) {
                return ResponseEntity.badRequest().body(Map.of("ok", false, "error", "Balansda mablag' yetarli emas!"));
            }
            starsTransactionService.create(uid, -price, stars);
            String cleanTarget = req.getTargetUsername().replace("@", "").trim();
            fragmentStarsService.buyStars(cleanTarget, stars);

            if (orderChannelService != null) {
                orderChannelService.sendOrderNotification("⭐ Telegram Stars", stars + " Stars", cleanTarget, price);
            }

            telegramService.sendMessageAuto(uid,
                    "🧾 <b>Xarid Kvitansiyasi: Telegram Stars</b>\n\n" +
                    "⭐ <b>Miqdor:</b> " + stars + " Stars\n" +
                    "👤 <b>Qabul qiluvchi:</b> @" + cleanTarget + "\n" +
                    "💰 <b>To‘lov:</b> " + String.format("%,d", price).replace(',', ' ') + " so‘m\n" +
                    "✅ <i>Buyurtmangiz muvaffaqiyatli yetkazildi!</i>");

            return ResponseEntity.ok(Map.of("ok", true, "message", "✅ " + stars + " Stars muvaffaqiyatli xarid qilindi!", "newBalance", currentBalance - price));
        } else {
            PaymentCard card = resolveActiveCard();
            DepositOrder order = autoPaymentService.createDepositOrder(uid, uid, price, card);
            return ResponseEntity.ok(Map.of("ok", true, "invoice", true, "orderId", order.getId(), "amount", order.getExactAmount(), "cardNumber", card != null ? card.getCardNumber() : "8600 0000 0000 0000", "holderName", card != null ? card.getHolderName() : "Admin", "methodName", card != null ? card.getMethodName() : "HUMO"));
        }
    }

    @Data
    public static class BuyPremiumRequest {
        private Long userId;
        private Integer months;
        private String targetUsername;
        private String paymentMethod;
    }

    @PostMapping("/buy/premium")
    public ResponseEntity<?> buyPremium(@RequestBody BuyPremiumRequest req) {
        if (req.getUserId() == null || req.getUserId() <= 0) {
            return ResponseEntity.badRequest().body(Map.of("ok", false, "error", "Telegram ID aniqlanmadi!"));
        }
        Long uid = req.getUserId();
        int months = req.getMonths() != null ? req.getMonths() : 3;

        // 1 oylik Telegram Premium bot tugmalaridagidek to'g'ridan-to'g'ri adminga yo'naltiriladi
        if (months == 1) {
            String target = req.getTargetUsername() != null ? req.getTargetUsername().replace("@", "").trim() : "";
            String adminUrl = "https://t.me/BLACK_mladshiy?text=" +
                    java.net.URLEncoder.encode("Salom! Men 1 oylik Telegram Premium sotib olmoqchiman. Qabul qiluvchi: @" + target, java.nio.charset.StandardCharsets.UTF_8);
            return ResponseEntity.ok(Map.of(
                    "ok", true,
                    "redirectAdmin", true,
                    "adminUrl", adminUrl,
                    "message", "1 oylik Telegram Premium admin (@BLACK_mladshiy) orqali rasmiylashtiriladi."
            ));
        }

        int price = priceService.getPremiumPrice(months);

        User user = userService.getUser(uid).orElseGet(() -> userService.createUser(uid));
        int currentBalance = user.getBalance() != null ? user.getBalance() : 0;

        if ("balance".equalsIgnoreCase(req.getPaymentMethod())) {
            if (currentBalance < price) {
                return ResponseEntity.badRequest().body(Map.of("ok", false, "error", "Balansda mablag' yetarli emas!"));
            }

            String cleanTarget = req.getTargetUsername().replace("@", "").trim();
            fragmentPremiumService.buyPremium(cleanTarget, months);
            premiumTransactionService.create(uid, -price, months);

            String duration = months >= 12 ? (months / 12) + " yillik" : months + " oylik";
            if (orderChannelService != null) {
                orderChannelService.sendOrderNotification("💎 Telegram Premium", duration, cleanTarget, price);
            }

            telegramService.sendMessageAuto(uid,
                    "🧾 <b>Xarid Kvitansiyasi: Telegram Premium</b>\n\n" +
                    "💎 <b>Muddat:</b> " + duration + "\n" +
                    "👤 <b>Qabul qiluvchi:</b> @" + cleanTarget + "\n" +
                    "💰 <b>To‘lov:</b> " + String.format("%,d", price).replace(',', ' ') + " so‘m\n" +
                    "✅ <i>Telegram Premium faollashtirildi!</i>");

            return ResponseEntity.ok(Map.of("ok", true, "message", "✅ " + months + " oylik Telegram Premium muvaffaqiyatli xarid qilindi!", "newBalance", currentBalance - price));
        } else {
            PaymentCard card = resolveActiveCard();
            DepositOrder order = autoPaymentService.createDepositOrder(uid, uid, price, card);
            return ResponseEntity.ok(Map.of("ok", true, "invoice", true, "orderId", order.getId(), "amount", order.getExactAmount(), "cardNumber", card != null ? card.getCardNumber() : "8600 0000 0000 0000", "holderName", card != null ? card.getHolderName() : "Admin", "methodName", card != null ? card.getMethodName() : "HUMO"));
        }
    }

    @Data
    public static class BuyPubgRequest {
        private Long userId;
        private Integer ucAmount;
        private String playerId;
        private String paymentMethod;
    }

    @PostMapping("/buy/pubg")
    public ResponseEntity<?> buyPubg(@RequestBody BuyPubgRequest req) {
        if (req.getUserId() == null || req.getUserId() <= 0) {
            return ResponseEntity.badRequest().body(Map.of("ok", false, "error", "Telegram ID aniqlanmadi!"));
        }
        Long uid = req.getUserId();
        int uc = req.getUcAmount() != null ? req.getUcAmount() : 60;
        int price = priceService.getPubgPrice(uc, 11000);

        User user = userService.getUser(uid).orElseGet(() -> userService.createUser(uid));
        int currentBalance = user.getBalance() != null ? user.getBalance() : 0;

        if ("balance".equalsIgnoreCase(req.getPaymentMethod())) {
            if (currentBalance < price) {
                return ResponseEntity.badRequest().body(Map.of("ok", false, "error", "Balansda mablag' yetarli emas!"));
            }
            pubgTransactionService.create(uid, req.getPlayerId(), req.getPlayerId(), "PUBG_" + uc, uc, price, null, "webapp", null);

            if (orderChannelService != null) {
                orderChannelService.sendOrderNotification("🎮 PUBG Mobile UC", uc + " UC", "ID: " + req.getPlayerId(), price);
            }

            telegramService.sendMessageAuto(uid,
                    "🧾 <b>Xarid Kvitansiyasi: PUBG Mobile UC</b>\n\n" +
                    "🎮 <b>Miqdor:</b> " + uc + " UC\n" +
                    "👤 <b>Player ID:</b> " + req.getPlayerId() + "\n" +
                    "💰 <b>To‘lov:</b> " + String.format("%,d", price).replace(',', ' ') + " so‘m\n" +
                    "✅ <i>UC akkauntingizga yuborildi!</i>");

            return ResponseEntity.ok(Map.of("ok", true, "message", "✅ " + uc + " PUBG UC muvaffaqiyatli yuborildi!", "newBalance", currentBalance - price));
        } else {
            PaymentCard card = resolveActiveCard();
            DepositOrder order = autoPaymentService.createDepositOrder(uid, uid, price, card);
            return ResponseEntity.ok(Map.of("ok", true, "invoice", true, "orderId", order.getId(), "amount", order.getExactAmount(), "cardNumber", card != null ? card.getCardNumber() : "8600 0000 0000 0000", "holderName", card != null ? card.getHolderName() : "Admin", "methodName", card != null ? card.getMethodName() : "HUMO"));
        }
    }

    @Data
    public static class PromoApplyRequest {
        private Long userId;
        private String code;
    }

    @PostMapping("/promocode/apply")
    public ResponseEntity<?> applyPromo(@RequestBody PromoApplyRequest req) {
        if (req.getUserId() == null || req.getUserId() <= 0) {
            return ResponseEntity.badRequest().body(Map.of("ok", false, "error", "Telegram ID aniqlanmadi!"));
        }
        if (req.getCode() == null || req.getCode().trim().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("ok", false, "error", "Promokodni kiriting!"));
        }

        CodeService.ActivationResult result = codeService.activateCode(req.getUserId(), req.getCode().trim().toUpperCase());
        if (!result.isSuccess()) {
            return ResponseEntity.badRequest().body(Map.of("ok", false, "error", result.getMessage()));
        }

        User user = userService.getUser(req.getUserId()).orElseGet(() -> userService.createUser(req.getUserId()));
        int newBalance = user.getBalance() != null ? user.getBalance() : 0;

        telegramService.sendMessageAuto(req.getUserId(),
                "🎟 <b>Promokod Muvaffaqiyatli Faollashtirildi!</b>\n\n" +
                "➕ Balansingizga <b>+" + String.format("%,d", result.bonusAmount()).replace(',', ' ') + " so‘m</b> qo‘shildi!\n" +
                "💰 Joriy balansingiz: <b>" + String.format("%,d", newBalance).replace(',', ' ') + " so‘m</b>");

        return ResponseEntity.ok(Map.of(
                "ok", true,
                "message", "✅ Promokod faollashtirildi! +" + String.format("%,d", result.bonusAmount()).replace(',', ' ') + " so'm qo'shildi.",
                "amount", result.bonusAmount(),
                "newBalance", newBalance
        ));
    }

    @GetMapping("/referral")
    public ResponseEntity<?> getReferralInfo(@RequestParam("userId") Long userId) {
        if (userId == null || userId <= 0) {
            return ResponseEntity.badRequest().body(Map.of("ok", false, "error", "ID aniqlanmadi"));
        }
        long count = userService.getReferralsCount(userId);
        String botUsername = "GyroService_bot";
        String link = "https://t.me/" + botUsername + "?start=ref_" + userId;

        return ResponseEntity.ok(Map.of(
                "ok", true,
                "link", link,
                "count", count,
                "percent", 2
        ));
    }
}

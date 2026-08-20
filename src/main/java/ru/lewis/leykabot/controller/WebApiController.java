package ru.lewis.leykabot.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.lewis.leykabot.model.database.entity.DepositOrder;
import ru.lewis.leykabot.model.database.entity.PaymentCard;
import ru.lewis.leykabot.model.database.entity.User;
import ru.lewis.leykabot.repository.DepositOrderRepository;
import ru.lewis.leykabot.repository.StarsTransactionRepository;
import ru.lewis.leykabot.repository.TransactionRepository;
import ru.lewis.leykabot.service.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@RestController
@RequestMapping("/api/webapp")
@RequiredArgsConstructor
public class WebApiController {

    private final UserService userService;
    private final PriceService priceService;
    private final AutoPaymentService autoPaymentService;
    private final PaymentCardService paymentCardService;
    private final TelegramService telegramService;
    private final DepositOrderRepository depositOrderRepository;
    private final TransactionRepository transactionRepository;
    private final StarsTransactionRepository starsTransactionRepository;
    private final TransactionService transactionService;

    @GetMapping("/init")
    public ResponseEntity<?> init(@RequestParam(required = false, defaultValue = "0") Long userId) {
        log.info("WebApp real init request for userId={}", userId);

        Map<String, Object> resp = new HashMap<>();

        // 1. Real User Info & Stats
        Map<String, Object> userMap = new HashMap<>();
        long totalStars = 0;
        long totalSpent = 0;
        long purchasesCount = 0;

        if (userId > 0) {
            Optional<User> userOpt = userService.getUser(userId);
            String username = "User";
            try {
                String u = telegramService.getUsernameByUserId(userId);
                if (u != null && !u.isBlank()) username = u;
            } catch (Exception ignored) {}

            int balance = 0;
            if (userOpt.isPresent()) {
                User u = userOpt.get();
                balance = u.getBalance() != null ? u.getBalance() : 0;
            }

            userMap.put("userId", userId);
            userMap.put("username", username);
            userMap.put("fullName", username.startsWith("@") ? username : "@" + username);
            userMap.put("balance", balance);
            userMap.put("verified", true);

            try {
                totalStars = starsTransactionRepository.sumStarsByTelegramId(userId);
                totalSpent = Math.abs(transactionRepository.sumRublesByTelegramId(userId));
                purchasesCount = depositOrderRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                        .filter(o -> "PAID_AUTO".equals(o.getStatus()) || "PAID_MANUAL".equals(o.getStatus()))
                        .count();
            } catch (Exception ignored) {}
        } else {
            userMap.put("userId", 0L);
            userMap.put("username", "User");
            userMap.put("fullName", "Foydalanuvchi");
            userMap.put("balance", 0);
            userMap.put("verified", true);
        }

        userMap.put("totalStars", totalStars);
        userMap.put("totalSpent", totalSpent);
        userMap.put("purchasesCount", purchasesCount);
        resp.put("user", userMap);

        // 2. Active Payment Card
        List<PaymentCard> activeCards = paymentCardService.getActiveCards();
        Map<String, Object> cardMap = new HashMap<>();
        if (!activeCards.isEmpty()) {
            PaymentCard c = activeCards.get(0);
            cardMap.put("cardNumber", formatCardNumber(c.getCardNumber()));
            cardMap.put("holderName", c.getHolderName());
            cardMap.put("methodName", c.getMethodName());
        } else {
            cardMap.put("cardNumber", "9860 0866 0350 6261");
            cardMap.put("holderName", "Sharipov Sh");
            cardMap.put("methodName", "HUMO");
        }
        resp.put("card", cardMap);

        // 3. Dynamic Prices
        Map<String, Object> prices = new HashMap<>();
        prices.put("starUnitPrice", priceService.getPrice("STAR_PER_UNIT", 230));

        Map<String, Object> starPkgs = new HashMap<>();
        starPkgs.put("50", priceService.getPrice("STAR_50", 11500));
        starPkgs.put("100", priceService.getPrice("STAR_100", 23000));
        starPkgs.put("250", priceService.getPrice("STAR_250", 57500));
        starPkgs.put("500", priceService.getPrice("STAR_500", 115000));
        starPkgs.put("1000", priceService.getPrice("STAR_1000", 230000));
        starPkgs.put("2500", 575000);
        prices.put("starPackages", starPkgs);

        Map<String, Object> premPkgs = new HashMap<>();
        premPkgs.put("1", Map.of("title", "1 oy", "price", 50000, "discount", "50 000 UZS", "popular", false, "desc", "Admindan to'g'ridan-to'g'ri"));
        premPkgs.put("3", Map.of("title", "3 oy", "price", priceService.getPrice("PREMIUM_3", 180000), "discount", "Ommabop", "popular", true, "desc", "Eng ko'p tanlangan"));
        premPkgs.put("6", Map.of("title", "6 oy", "price", priceService.getPrice("PREMIUM_6", 250000), "discount", "-15%", "popular", false, "desc", "Tejamkor"));
        premPkgs.put("12", Map.of("title", "12 oy", "price", priceService.getPrice("PREMIUM_12", 400000), "discount", "-30%", "popular", false, "desc", "VIP Tarif"));
        prices.put("premiumPackages", premPkgs);

        Map<String, Object> pubgPkgs = new HashMap<>();
        pubgPkgs.put("60", priceService.getPrice("PUBG_60", 11000));
        pubgPkgs.put("325", priceService.getPrice("PUBG_325", 55000));
        pubgPkgs.put("660", priceService.getPrice("PUBG_660", 110000));
        pubgPkgs.put("1800", priceService.getPrice("PUBG_1800", 275000));
        pubgPkgs.put("3850", priceService.getPrice("PUBG_3850", 545000));
        pubgPkgs.put("8100", priceService.getPrice("PUBG_8100", 1090000));
        prices.put("pubgPackages", pubgPkgs);

        resp.put("prices", prices);

        // 4. Real User History
        List<Map<String, Object>> histList = new ArrayList<>();
        if (userId > 0) {
            List<DepositOrder> userOrders = depositOrderRepository.findByUserIdOrderByCreatedAtDesc(userId);
            for (DepositOrder o : userOrders.stream().limit(15).toList()) {
                Map<String, Object> h = new HashMap<>();
                h.put("id", "ORD-" + o.getId());
                h.put("type", "deposit");
                h.put("title", "💳 Balans to'ldirish");
                h.put("target", o.getCardInfo() != null ? o.getCardInfo() : "HUMO *6261");
                h.put("amount", o.getBaseAmount());
                h.put("status", "PAID_AUTO".equals(o.getStatus()) || "PAID_MANUAL".equals(o.getStatus()) ? "completed" : ("PENDING".equals(o.getStatus()) ? "pending" : "cancelled"));
                h.put("date", o.getCreatedAt() != null ? o.getCreatedAt().format(DateTimeFormatter.ofPattern("dd.MM HH:mm")) : "Bugun");
                histList.add(h);
            }
        }
        resp.put("history", histList);

        return ResponseEntity.ok(resp);
    }

    @GetMapping("/top")
    public ResponseEntity<?> getTopUsers(@RequestParam(required = false, defaultValue = "today") String period,
                                         @RequestParam(required = false, defaultValue = "0") Long userId) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime from = switch (period) {
            case "week" -> now.minusDays(7);
            case "month" -> now.minusDays(30);
            default -> now.toLocalDate().atStartOfDay(); // today
        };

        List<Object[]> rawList = transactionRepository.findTopByRublesBetween(from, now, PageRequest.of(0, 10));
        List<Map<String, Object>> result = new ArrayList<>();
        int rank = 1;

        for (Object[] row : rawList) {
            Long tgId = ((Number) row[0]).longValue();
            long total = ((Number) row[1]).longValue();

            String name = "User";
            try {
                String u = telegramService.getUsernameByUserId(tgId);
                if (u != null && !u.isBlank()) {
                    name = u.startsWith("@") ? u : "@" + u;
                } else {
                    String strId = tgId.toString();
                    name = strId.length() > 6 ? strId.substring(0, 4) + "***" : "ID: " + strId;
                }
            } catch (Exception ignored) {}

            boolean isMe = (userId > 0 && tgId.equals(userId));
            if (isMe) {
                name += " (Siz)";
            }

            Map<String, Object> item = new HashMap<>();
            item.put("rank", rank);
            item.put("name", name);
            item.put("total", total);
            item.put("isMe", isMe);
            item.put("avatar", rank == 1 ? "👑" : (rank == 2 ? "⭐" : (rank == 3 ? "🔥" : "👤")));

            result.add(item);
            rank++;
        }

        return ResponseEntity.ok(Map.of("ok", true, "period", period, "top", result));
    }

    @PostMapping("/promocode")
    public ResponseEntity<?> applyPromocode(@RequestBody Map<String, Object> req) {
        Long userId = Long.parseLong(req.getOrDefault("userId", "0").toString());
        String code = req.getOrDefault("code", "").toString().trim();

        if (userId <= 0 || code.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("ok", false, "error", "Promokod yoki User ID kiritilmadi"));
        }

        boolean success = userService.activateCode(userId, code);
        if (success) {
            long newBal = userService.getBalance(userId).orElse(0);
            return ResponseEntity.ok(Map.of("ok", true, "message", "Promokod muvaffaqiyatli faollashtirildi!", "newBalance", newBal));
        } else {
            return ResponseEntity.ok(Map.of("ok", false, "error", "Promokod topilmadi, muddati o'tgan yoki allaqachon ishlatilgan!"));
        }
    }

    @PostMapping("/deposit")
    public ResponseEntity<?> createDepositOrder(@RequestBody Map<String, Object> req) {
        try {
            Long userId = Long.parseLong(req.getOrDefault("userId", "0").toString());
            int amount = Integer.parseInt(req.getOrDefault("amount", "50000").toString());

            if (userId <= 0 || amount < 5000) {
                return ResponseEntity.badRequest().body(Map.of("ok", false, "error", "Noto'g'ri summa yoki foydalanuvchi ID"));
            }

            PaymentCard card = paymentCardService.getActiveCards().stream().findFirst().orElse(null);
            DepositOrder order = autoPaymentService.createDepositOrder(userId, userId, amount, card);

            Map<String, Object> resp = new HashMap<>();
            resp.put("ok", true);
            resp.put("orderId", order.getId());
            resp.put("exactAmount", order.getExactAmount());
            resp.put("baseAmount", order.getBaseAmount());
            resp.put("expiresAt", System.currentTimeMillis() + (15 * 60 * 1000));
            resp.put("card", Map.of(
                    "cardNumber", card != null ? formatCardNumber(card.getCardNumber()) : "9860 0866 0350 6261",
                    "holderName", card != null ? card.getHolderName() : "Sharipov Sh",
                    "methodName", card != null ? card.getMethodName() : "HUMO"
            ));

            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            log.error("Failed to create webapp deposit order: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of("ok", false, "error", e.getMessage()));
        }
    }

    @GetMapping("/check-payment")
    public ResponseEntity<?> checkPayment(@RequestParam Long orderId) {
        Optional<DepositOrder> orderOpt = depositOrderRepository.findById(orderId);
        if (orderOpt.isEmpty()) {
            return ResponseEntity.ok(Map.of("ok", false, "status", "not_found"));
        }

        DepositOrder o = orderOpt.get();
        boolean completed = "PAID_AUTO".equals(o.getStatus()) || "PAID_MANUAL".equals(o.getStatus());
        boolean pending = "PENDING".equals(o.getStatus()) && (o.getExpiresAt() == null || o.getExpiresAt().isAfter(LocalDateTime.now()));

        long userBal = userService.getBalance(o.getUserId()).orElse(0);

        Map<String, Object> resp = new HashMap<>();
        resp.put("ok", true);
        resp.put("status", completed ? "completed" : (pending ? "pending" : "expired"));
        resp.put("orderId", o.getId());
        resp.put("amount", o.getBaseAmount());
        resp.put("newBalance", userBal);

        return ResponseEntity.ok(resp);
    }

    @PostMapping("/order")
    public ResponseEntity<?> processOrder(@RequestBody Map<String, Object> req) {
        try {
            Long userId = Long.parseLong(req.getOrDefault("userId", "0").toString());
            String type = req.getOrDefault("type", "").toString();
            String target = req.getOrDefault("target", "").toString();
            int amount = Integer.parseInt(req.getOrDefault("amount", "0").toString());
            String paymentMethod = req.getOrDefault("paymentMethod", "card").toString();

            if (userId <= 0 || amount <= 0) {
                return ResponseEntity.badRequest().body(Map.of("ok", false, "error", "Noto'g'ri ma'lumotlar"));
            }

            if ("balance".equals(paymentMethod)) {
                long currentBal = userService.getBalance(userId).orElse(0);
                if (currentBal < amount) {
                    return ResponseEntity.badRequest().body(Map.of("ok", false, "error", "Balansda mablag' yetarli emas"));
                }
                userService.changeBalance(userId, -amount);
                transactionService.create(userId, -amount);
                long newBal = userService.getBalance(userId).orElse(0);

                log.info("🛒 WebApp order processed from balance: userId={}, type={}, target={}, amount={}, newBal={}",
                        userId, type, target, amount, newBal);

                return ResponseEntity.ok(Map.of("ok", true, "message", "Buyurtma muvaffaqiyatli bajarildi", "newBalance", newBal));
            } else {
                return ResponseEntity.ok(Map.of("ok", true, "message", "Karta orqali avto-to'lov kutilmoqda"));
            }
        } catch (Exception e) {
            log.error("Failed to process webapp order: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of("ok", false, "error", e.getMessage()));
        }
    }

    private String formatCardNumber(String card) {
        if (card == null) return "9860 0866 0350 6261";
        String clean = card.replaceAll("\\s+", "");
        if (clean.length() == 16) {
            return clean.substring(0, 4) + " " + clean.substring(4, 8) + " " + clean.substring(8, 12) + " " + clean.substring(12, 16);
        }
        return card;
    }
}

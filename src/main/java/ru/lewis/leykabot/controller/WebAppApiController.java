package ru.lewis.leykabot.controller;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.lewis.leykabot.model.database.entity.DepositOrder;
import ru.lewis.leykabot.model.database.entity.PaymentCard;
import ru.lewis.leykabot.model.database.entity.User;
import ru.lewis.leykabot.repository.*;
import ru.lewis.leykabot.service.*;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@RestController
@RequestMapping("/api/webapp")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class WebAppApiController {

    private final UserService userService;
    private final PriceService priceService;
    private final PaymentCardService paymentCardService;
    private final AutoPaymentService autoPaymentService;
    private final StarsTransactionService starsTransactionService;
    private final PremiumTransactionService premiumTransactionService;
    private final PubgTransactionService pubgTransactionService;
    private final FragmentStarsService fragmentStarsService;
    private final TopService topService;
    private final TransactionRepository transactionRepository;
    private final StarsTransactionRepository starsRepository;
    private final PremiumTransactionRepository premiumRepository;
    private final PubgTransactionRepository pubgRepository;
    private final DepositOrderRepository depositOrderRepository;
    private final TelegramService telegramService;

    @GetMapping("/init")
    public ResponseEntity<?> getInitData(@RequestParam(value = "userId", required = false) Long userId) {
        if (userId == null || userId <= 0) {
            userId = 8159265215L;
        }

        final Long currentId = userId;
        User user = userService.getUser(currentId).orElseGet(() -> userService.createUser(currentId));
        int balance = user.getBalance() != null ? user.getBalance() : 0;
        long totalSpent = transactionRepository.sumRublesByTelegramId(currentId);
        long starsTotal = starsRepository.sumStarsByTelegramId(currentId);
        long starsCount = starsRepository.countByTelegramId(currentId);
        long premiumCount = premiumRepository.countByTelegramId(currentId);
        long pubgCount = pubgRepository.countByTelegramId(currentId);
        long totalPurchases = starsCount + premiumCount + pubgCount;

        PaymentCard card = paymentCardService.getActiveCards().stream().findFirst().orElse(null);

        Map<String, Object> resp = new HashMap<>();
        resp.put("user", Map.of(
                "userId", user.getTelegramId(),
                "username", telegramService.getUsernameByUserId(user.getTelegramId()) != null ? telegramService.getUsernameByUserId(user.getTelegramId()) : "User",
                "fullName", telegramService.getFullNameByUserId(user.getTelegramId()) != null ?
                        telegramService.getFullNameByUserId(user.getTelegramId()) : "S R",
                "balance", balance,
                "verified", true
        ));

        resp.put("stats", Map.of(
                "balance", balance,
                "totalStars", starsTotal,
                "totalSpent", totalSpent,
                "totalPurchases", totalPurchases,
                "goalTarget", 1200000,
                "goalProgress", Math.min(100, (int) ((totalSpent * 100) / 1200000))
        ));

        resp.put("prices", Map.of(
                "starUnitPrice", priceService.getPrice("STAR_PER_UNIT", 230),
                "starPackages", priceService.getAllStarPrices(),
                "premiumPackages", priceService.getAllPremiumPrices(),
                "pubgPackages", priceService.getAllPubgPrices()
        ));

        if (card != null) {
            resp.put("card", Map.of(
                    "cardNumber", card.getCardNumber(),
                    "holderName", card.getHolderName(),
                    "methodName", card.getMethodName()
            ));
        }

        return ResponseEntity.ok(resp);
    }

    @GetMapping("/top")
    public ResponseEntity<?> getTopLeaderboard(@RequestParam(value = "period", defaultValue = "today") String period,
                                              @RequestParam(value = "userId", required = false) Long userId) {
        String periodKey = switch (period.toLowerCase()) {
            case "week", "7days" -> "7days";
            case "month", "30days" -> "30days";
            default -> "today";
        };

        TopService.GlobalStats stats = topService.getGlobalStats(periodKey, userId != null ? userId : 8159265215L);

        List<Map<String, Object>> topList = new ArrayList<>();
        for (TopService.TopEntry entry : stats.top7()) {
            String name = telegramService.getFullNameByUserId(entry.telegramId());
            if (name == null || name.isBlank()) name = telegramService.getUsernameByUserId(entry.telegramId());
            if (name == null || name.isBlank()) name = "Mijoz #" + entry.telegramId();

            topList.add(Map.of(
                    "rank", entry.rank(),
                    "name", name,
                    "total", entry.total(),
                    "stars", entry.stars(),
                    "premiumMonths", entry.premiumMonths(),
                    "pubgUc", entry.pubgUc()
            ));
        }

        List<String> recentBuyers = new ArrayList<>();
        var recentTxList = transactionRepository.findTop10ByOrderByCreatedAtDesc();
        for (var tx : recentTxList) {
            String bName = telegramService.getFullNameByUserId(tx.getTelegramId());
            if (bName == null || bName.isBlank()) bName = telegramService.getUsernameByUserId(tx.getTelegramId());
            if (bName != null && !bName.isBlank() && !recentBuyers.contains(bName)) {
                recentBuyers.add(bName);
            }
        }
        if (recentBuyers.isEmpty()) {
            for (TopService.TopEntry entry : stats.top7()) {
                String n = telegramService.getFullNameByUserId(entry.telegramId());
                if (n == null || n.isBlank()) n = telegramService.getUsernameByUserId(entry.telegramId());
                if (n != null && !n.isBlank() && !recentBuyers.contains(n)) recentBuyers.add(n);
            }
        }

        return ResponseEntity.ok(Map.of(
                "period", period,
                "top", topList,
                "recentBuyers", recentBuyers,
                "userRank", stats.userRank()
        ));
    }

    @GetMapping("/history")
    public ResponseEntity<?> getHistory(@RequestParam(value = "userId", defaultValue = "8159265215") Long userId,
                                        @RequestParam(value = "status", defaultValue = "all") String status) {
        List<Map<String, Object>> list = new ArrayList<>();

        var stars = starsRepository.findByTelegramIdOrderByCreatedAtDesc(userId);
        for (var s : stars) {
            list.add(Map.of(
                    "id", "ST-" + s.getId(),
                    "service", "Telegram Stars",
                    "details", s.getAmountStars() + " Stars",
                    "amount", s.getTransaction() != null ? s.getTransaction().getAmountRubles() : 0,
                    "date", s.getCreatedAt().toString(),
                    "status", "COMPLETED"
            ));
        }

        var prem = premiumRepository.findByTelegramIdOrderByCreatedAtDesc(userId);
        for (var p : prem) {
            list.add(Map.of(
                    "id", "PR-" + p.getId(),
                    "service", "Telegram Premium",
                    "details", p.getMonths() + " oy",
                    "amount", p.getTransaction() != null ? p.getTransaction().getAmountRubles() : 0,
                    "date", p.getCreatedAt().toString(),
                    "status", "COMPLETED"
            ));
        }

        var pubg = pubgRepository.findByTelegramIdOrderByCreatedAtDesc(userId);
        for (var pb : pubg) {
            list.add(Map.of(
                    "id", "PB-" + pb.getId(),
                    "service", "PUBG Mobile UC",
                    "details", pb.getUcAmount() + " UC (ID: " + pb.getPlayerId() + ")",
                    "amount", pb.getPriceRubles(),
                    "date", pb.getCreatedAt().toString(),
                    "status", "COMPLETED"
            ));
        }

        var deposits = depositOrderRepository.findAll();
        for (var d : deposits) {
            if (userId.equals(d.getUserId())) {
                list.add(Map.of(
                        "id", "DP-" + d.getId(),
                        "service", "Balans To'ldirish",
                        "details", d.getExactAmount() + " so'm",
                        "amount", d.getExactAmount(),
                        "date", d.getCreatedAt().toString(),
                        "status", d.getStatus()
                ));
            }
        }

        return ResponseEntity.ok(list);
    }

    @Data
    public static class DepositRequest {
        private Long userId;
        private Integer amount;
    }

    @PostMapping("/deposit")
    public ResponseEntity<?> createDeposit(@RequestBody DepositRequest req) {
        if (req.getAmount() == null || req.getAmount() < 1000) {
            return ResponseEntity.badRequest().body(Map.of("ok", false, "error", "Minimal summa 1 000 so'm"));
        }

        Long uid = req.getUserId() != null ? req.getUserId() : 8159265215L;
        PaymentCard card = paymentCardService.getActiveCards().stream().findFirst().orElse(null);

        DepositOrder order = autoPaymentService.createDepositOrder(uid, uid, req.getAmount(), card);

        return ResponseEntity.ok(Map.of(
                "ok", true,
                "orderId", order.getId(),
                "orderCode", order.getOrderCode(),
                "amount", order.getExactAmount(),
                "cardNumber", card != null ? card.getCardNumber() : "9860 1678 4421 7684",
                "holderName", card != null ? card.getHolderName() : "SUNNAT C.",
                "methodName", card != null ? card.getMethodName() : "HUMOCARD",
                "createdAt", order.getCreatedAt().toString(),
                "expiresAt", order.getExpiresAt().toString()
        ));
    }

    @GetMapping("/deposit/check")
    public ResponseEntity<?> checkDepositStatus(@RequestParam("orderId") Long orderId) {
        Optional<DepositOrder> opt = autoPaymentService.getOrder(orderId);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        DepositOrder order = opt.get();
        return ResponseEntity.ok(Map.of(
                "orderId", order.getId(),
                "status", order.getStatus(),
                "isPaid", "PAID_AUTO".equals(order.getStatus()) || "PAID_MANUAL".equals(order.getStatus())
        ));
    }

    @Data
    public static class BuyStarsRequest {
        private Long userId;
        private Integer amount;
        private String targetUsername;
        private String paymentMethod; // "card" or "balance"
    }

    @PostMapping("/buy/stars")
    public ResponseEntity<?> buyStars(@RequestBody BuyStarsRequest req) {
        Long uid = req.getUserId() != null ? req.getUserId() : 8159265215L;
        int stars = req.getAmount() != null ? req.getAmount() : 50;
        int price = priceService.getStarsPrice(stars);

        User user = userService.getUser(uid).orElseGet(() -> userService.createUser(uid));
        int currentBalance = user.getBalance() != null ? user.getBalance() : 0;

        if ("balance".equalsIgnoreCase(req.getPaymentMethod())) {
            if (currentBalance < price) {
                return ResponseEntity.badRequest().body(Map.of("ok", false, "error", "Balansda mablag' yetarli emas!"));
            }
            starsTransactionService.create(uid, price, stars);

            String cleanTarget = req.getTargetUsername().replace("@", "").trim();
            fragmentStarsService.buyStars(cleanTarget, stars);

            return ResponseEntity.ok(Map.of(
                    "ok", true,
                    "message", "✅ " + stars + " Stars muvaffaqiyatli xarid qilindi!",
                    "newBalance", currentBalance - price
            ));
        } else {
            // Direct card invoice
            PaymentCard card = paymentCardService.getActiveCards().stream().findFirst().orElse(null);
            DepositOrder order = autoPaymentService.createDepositOrder(uid, uid, price, card);
            return ResponseEntity.ok(Map.of(
                    "ok", true,
                    "invoice", true,
                    "orderId", order.getId(),
                    "amount", order.getExactAmount(),
                    "cardNumber", card != null ? card.getCardNumber() : "9860 1678 4421 7684",
                    "holderName", card != null ? card.getHolderName() : "SUNNAT C.",
                    "methodName", card != null ? card.getMethodName() : "HUMOCARD"
            ));
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
        Long uid = req.getUserId() != null ? req.getUserId() : 8159265215L;
        int uc = req.getUcAmount() != null ? req.getUcAmount() : 60;
        int price = priceService.getPubgPrice(uc, 11000);

        User user = userService.getUser(uid).orElseGet(() -> userService.createUser(uid));
        int currentBalance = user.getBalance() != null ? user.getBalance() : 0;

        if ("balance".equalsIgnoreCase(req.getPaymentMethod())) {
            if (currentBalance < price) {
                return ResponseEntity.badRequest().body(Map.of("ok", false, "error", "Balansda mablag' yetarli emas!"));
            }
            pubgTransactionService.create(uid, req.getPlayerId(), req.getPlayerId(), "PUBG_" + uc, uc, price, null, "webapp", null);

            return ResponseEntity.ok(Map.of(
                    "ok", true,
                    "message", "✅ " + uc + " PUBG UC muvaffaqiyatli yuborildi!",
                    "newBalance", currentBalance - price
            ));
        } else {
            PaymentCard card = paymentCardService.getActiveCards().stream().findFirst().orElse(null);
            DepositOrder order = autoPaymentService.createDepositOrder(uid, uid, price, card);
            return ResponseEntity.ok(Map.of(
                    "ok", true,
                    "invoice", true,
                    "orderId", order.getId(),
                    "amount", order.getExactAmount(),
                    "cardNumber", card != null ? card.getCardNumber() : "9860 1678 4421 7684",
                    "holderName", card != null ? card.getHolderName() : "SUNNAT C.",
                    "methodName", card != null ? card.getMethodName() : "HUMOCARD"
            ));
        }
    }
}


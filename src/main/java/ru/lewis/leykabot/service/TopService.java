package ru.lewis.leykabot.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import ru.lewis.leykabot.repository.PremiumTransactionRepository;
import ru.lewis.leykabot.repository.PubgTransactionRepository;
import ru.lewis.leykabot.repository.StarsTransactionRepository;
import ru.lewis.leykabot.repository.TransactionRepository;

import java.text.MessageFormat;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;

@Service
@RequiredArgsConstructor
public class TopService {

    private final TransactionRepository        transactionRepository;
    private final StarsTransactionRepository   starsRepository;
    private final PremiumTransactionRepository premiumRepository;
    private final PubgTransactionRepository    pubgRepository;

    @Value("${top.one-page-limit}")
    private int onePageLimit;

    public record TopEntry(int rank, Long telegramId, long total) {}

    private Cache<String, List<TopEntry>> rulesCache;
    private Cache<String, List<TopEntry>> starsCache;
    private Cache<String, List<TopEntry>> premiumCache;

    @PostConstruct
    public void initCaches() {
        rulesCache   = buildCache();
        starsCache   = buildCache();
        premiumCache = buildCache();
    }

    public CompletableFuture<Void> preloadAll(int totalLimit) {
        return CompletableFuture.runAsync(() -> {
            int pages = totalLimit / onePageLimit;
            for (int page = 0; page < pages; page++) {
                int offset = page * onePageLimit;
                rulesCache.put(key(offset, onePageLimit),   fetchTopByRubles(offset, onePageLimit));
                starsCache.put(key(offset, onePageLimit),   fetchTopByStars(offset, onePageLimit));
                premiumCache.put(key(offset, onePageLimit), fetchTopByPremium(offset, onePageLimit));
            }
        });
    }

    private <K, V> Cache<K, V> buildCache() {
        return Caffeine.newBuilder()
                .maximumSize(100)
                .expireAfterWrite(Duration.ofMinutes(5))
                .build();
    }

    private String key(int offset, int limit) {
        return offset + ":" + limit;
    }

    // -------------------------------------------------------------------------
    // Публичные методы
    // -------------------------------------------------------------------------

    public List<TopEntry> getTopByRubles(int offset, int limit) {
        return rulesCache.get(key(offset, limit), k -> fetchTopByRubles(offset, limit));
    }

    public List<TopEntry> getTopByStars(int offset, int limit) {
        return starsCache.get(key(offset, limit), k -> fetchTopByStars(offset, limit));
    }

    public List<TopEntry> getTopByPremium(int offset, int limit) {
        return premiumCache.get(key(offset, limit), k -> fetchTopByPremium(offset, limit));
    }

    // -------------------------------------------------------------------------
    // Обновление кэша после новых транзакций
    // -------------------------------------------------------------------------

    public void updateRublesTop(long telegramId, long addedTotal) {
        updateTop(rulesCache, telegramId, addedTotal, this::fetchTopByRubles);
    }

    public void updateStarsTop(long telegramId, long addedTotal) {
        updateTop(starsCache, telegramId, addedTotal, this::fetchTopByStars);
    }

    public void updatePremiumTop(long telegramId, long addedTotal) {
        updateTop(premiumCache, telegramId, addedTotal, this::fetchTopByPremium);
    }

    private void updateTop(Cache<String, List<TopEntry>> cache, long telegramId, long addedTotal,
                           BiFunction<Integer, Integer, List<TopEntry>> fetcher) {
        // Если кэш пустой (не прогрелся) — загружаем дефолтную страницу из БД
        if (cache.asMap().isEmpty()) {
            cache.put(key(0, onePageLimit), fetcher.apply(0, onePageLimit));
        }

        cache.asMap().forEach((k, entries) -> {
            String[] parts = k.split(":");
            int offset = Integer.parseInt(parts[0]);

            boolean found = entries.stream().anyMatch(e -> e.telegramId().equals(telegramId));

            List<TopEntry> updated = new ArrayList<>(entries.stream()
                    .map(e -> e.telegramId().equals(telegramId)
                            ? new TopEntry(0, telegramId, e.total() + addedTotal)
                            : e)
                    .toList());

            if (!found) {
                updated.add(new TopEntry(0, telegramId, addedTotal));
            }

            List<TopEntry> sorted = updated.stream()
                    .sorted(Comparator.comparingLong(TopEntry::total).reversed())
                    .toList();

            List<TopEntry> ranked = new ArrayList<>();
            for (int i = 0; i < sorted.size(); i++) {
                ranked.add(new TopEntry(offset + i + 1, sorted.get(i).telegramId(), sorted.get(i).total()));
            }

            cache.put(k, ranked);
        });
    }

    // -------------------------------------------------------------------------
    // Запросы в БД
    // -------------------------------------------------------------------------

    private List<TopEntry> fetchTopByRubles(int offset, int limit) {
        List<Object[]> rows = transactionRepository.findTopByRubles(PageRequest.of(offset / limit, limit));
        List<TopEntry> result = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            Object[] row = rows.get(i);
            result.add(new TopEntry(offset + i + 1, (Long) row[0], (Long) row[1]));
        }
        return result;
    }

    private List<TopEntry> fetchTopByStars(int offset, int limit) {
        List<Object[]> rows = starsRepository.findTopByStars(PageRequest.of(offset / limit, limit));
        List<TopEntry> result = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            Object[] row = rows.get(i);
            result.add(new TopEntry(offset + i + 1, (Long) row[0], (Long) row[1]));
        }
        return result;
    }

    private List<TopEntry> fetchTopByPremium(int offset, int limit) {
        List<Object[]> rows = premiumRepository.findTopByMonths(PageRequest.of(offset / limit, limit));
        List<TopEntry> result = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            Object[] row = rows.get(i);
            result.add(new TopEntry(offset + i + 1, (Long) row[0], (Long) row[1]));
        }
        return result;
    }

    public record GlobalStats(
            long turnoverRubles,
            long totalPurchases,
            long starsTxCount,
            long starsTotalAmount,
            long premiumTxCount,
            long pubgTxCount,
            List<TopEntry> top7,
            int userRank
    ) {}

    public GlobalStats getGlobalStats(String period, Long currentUserId) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime from;
        switch (period) {
            case "today" -> from = now.toLocalDate().atStartOfDay();
            case "3days" -> from = now.minusDays(3);
            case "7days" -> from = now.minusDays(7);
            case "30days" -> from = now.minusDays(30);
            default -> from = LocalDateTime.of(2020, 1, 1, 0, 0); // all
        }

        long turnover = transactionRepository.sumRublesBetween(from, now);
        long starsCount = starsRepository.countStarsBetween(from, now);
        long starsTotal = starsRepository.sumStarsBetween(from, now);
        long premiumCount = premiumRepository.countPremiumBetween(from, now);
        long pubgCount = pubgRepository.countBetween(from, now);
        long totalPurchases = starsCount + premiumCount + pubgCount;

        List<Object[]> rawTop = transactionRepository.findTopByRublesBetween(from, now, PageRequest.of(0, 100));
        List<TopEntry> top7 = new ArrayList<>();
        int userRank = -1;

        for (int i = 0; i < rawTop.size(); i++) {
            Object[] row = rawTop.get(i);
            Long uid = (Long) row[0];
            long tot = ((Number) row[1]).longValue();
            if (i < 7) {
                top7.add(new TopEntry(i + 1, uid, tot));
            }
            if (currentUserId != null && uid.equals(currentUserId)) {
                userRank = i + 1;
            }
        }

        return new GlobalStats(turnover, totalPurchases, starsCount, starsTotal, premiumCount, pubgCount, top7, userRank);
    }
}
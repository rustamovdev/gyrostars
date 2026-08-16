package ru.lewis.leykabot.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.lewis.leykabot.model.database.entity.ActivatedCode;
import ru.lewis.leykabot.model.database.entity.Code;
import ru.lewis.leykabot.model.database.entity.User;
import ru.lewis.leykabot.repository.ActivatedCodeRepository;
import ru.lewis.leykabot.repository.CodeRepository;
import ru.lewis.leykabot.repository.UserRepository;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final CodeRepository codeRepository;
    private final ActivatedCodeRepository activatedCodeRepository;

    // telegramId -> User
    private Cache<Long, User> userCache;
    // telegramId+code -> Boolean (активирован ли промокод)
    private Cache<String, Boolean> activatedCodeCache;

    @PostConstruct
    public void initCaches() {
        userCache = Caffeine.newBuilder()
                .maximumSize(1_000)
                .expireAfterWrite(Duration.ofMinutes(10))
                .build();

        activatedCodeCache = Caffeine.newBuilder()
                .maximumSize(5_000)
                .expireAfterWrite(Duration.ofMinutes(10))
                .build();
    }

    public void clearCache() {
        if (userCache != null) userCache.invalidateAll();
        if (activatedCodeCache != null) activatedCodeCache.invalidateAll();
    }

    // -------------------------------------------------------------------------
    // Вспомогательные методы загрузки в кэш
    // -------------------------------------------------------------------------

    public CompletableFuture<Void> warmUpAll(Long telegramId) {
        return CompletableFuture.runAsync(() -> {
            loadUser(telegramId);
        });
    }

    private User loadUser(Long telegramId) {
        return userCache.get(telegramId,
                id -> userRepository.findByTelegramId(id).orElse(null));
    }

    private boolean loadActivatedCode(Long telegramId, String code) {
        return activatedCodeCache.get(telegramId + ":" + code,
                key -> activatedCodeRepository.existsByTelegramIdAndCode(telegramId, code));
    }

    // -------------------------------------------------------------------------
    // Пользователь
    // -------------------------------------------------------------------------

    @Transactional
    public User createUser(Long telegramId, Long referrerId) {
        Optional<User> existing = userRepository.findByTelegramId(telegramId);
        if (existing.isPresent()) {
            User user = existing.get();
            userCache.put(telegramId, user);
            return user;
        }
        User user = new User();
        user.setTelegramId(telegramId);
        user.setBalance(0);
        if (referrerId != null && !referrerId.equals(telegramId) && isUserExists(referrerId)) {
            user.setReferrerId(referrerId);
        }
        User saved = userRepository.save(user);
        userCache.put(telegramId, saved);
        return saved;
    }

    @Transactional
    public User createUser(Long telegramId) {
        return createUser(telegramId, null);
    }

    public long getReferralsCount(Long telegramId) {
        return userRepository.countByReferrerId(telegramId);
    }

    public boolean isUserExists(Long telegramId) {
        if (userCache.getIfPresent(telegramId) != null) return true;
        return userRepository.existsByTelegramId(telegramId);
    }

    public Optional<User> getUser(Long telegramId) {
        User user = loadUser(telegramId);
        if (user == null) {
            Optional<User> dbUser = userRepository.findByTelegramId(telegramId);
            dbUser.ifPresent(this::updateUserCache);
            return dbUser;
        }
        return Optional.of(user);
    }

    public Optional<Integer> getBalance(Long telegramId) {
        return getUser(telegramId).map(u -> u.getBalance() != null ? u.getBalance() : 0);
    }

    /**
     * Obnovlyaet polzovatelya v keshe.
     */
    public void updateUserCache(User user) {
        if (user != null && user.getTelegramId() != null) {
            userCache.put(user.getTelegramId(), user);
        }
    }

    // -------------------------------------------------------------------------
    // Промокоды
    // -------------------------------------------------------------------------

    @Transactional
    public boolean activateCode(Long telegramId, String codeStr) {
        // Проверяем через кэш — уже активирован?
        if (loadActivatedCode(telegramId, codeStr)) {
            return false;
        }

        Code code = codeRepository.findByCode(codeStr).orElse(null);
        if (code == null || !code.canBeUsed()) {
            return false;
        }

        ActivatedCode activated = new ActivatedCode();
        activated.setTelegramId(telegramId);
        activated.setCode(codeStr);
        activatedCodeRepository.save(activated);

        codeRepository.incrementUsedCount(codeStr);

        // Кэшируем факт активации промокода
        activatedCodeCache.put(telegramId + ":" + codeStr, Boolean.TRUE);

        // Обновляем баланс в БД и в кэше
        User user = loadUser(telegramId);
        if (user != null) {
            user.setBalance(user.getBalance() + code.getAmount());
            userRepository.save(user);
            userCache.put(telegramId, user);
        }

        return true;
    }

    @Transactional
    public void changeBalance(Long telegramId, int delta) {
        User user = getUser(telegramId).orElseGet(() -> createUser(telegramId));
        int current = user.getBalance() != null ? user.getBalance() : 0;
        user.setBalance(Math.max(0, current + delta));
        userRepository.save(user);
        userCache.put(telegramId, user);
    }
}
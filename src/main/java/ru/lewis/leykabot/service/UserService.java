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
    public User createUser(Long telegramId) {
        User user = new User();
        user.setTelegramId(telegramId);
        user.setBalance(0);
        User saved = userRepository.save(user);
        userCache.put(telegramId, saved);
        return saved;
    }

    public boolean isUserExists(Long telegramId) {
        return loadUser(telegramId) != null;
    }

    public Optional<User> getUser(Long telegramId) {
        return Optional.ofNullable(loadUser(telegramId));
    }

    public Optional<Integer> getBalance(Long telegramId) {
        return getUser(telegramId).map(User::getBalance);
    }

    /**
     * Обновляет пользователя в кэше без запроса в БД.
     * Вызывается из других сервисов после изменения пользователя.
     */
    public void updateUserCache(User user) {
        userCache.put(user.getTelegramId(), user);
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
}
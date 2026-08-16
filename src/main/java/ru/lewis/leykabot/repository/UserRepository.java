package ru.lewis.leykabot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.lewis.leykabot.model.database.entity.User;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {

    Optional<User> findByTelegramId(Long telegramId);

    boolean existsByTelegramId(Long telegramId);

    void deleteByTelegramId(Long telegramId);

    @Query("SELECT COALESCE(SUM(u.balance), 0) FROM User u")
    long sumTotalBalance();

    @Query("SELECT COUNT(u) FROM User u WHERE u.createdAt >= :after")
    long countCreatedAfter(@Param("after") java.time.LocalDateTime after);

    long countByReferrerId(Long referrerId);
}

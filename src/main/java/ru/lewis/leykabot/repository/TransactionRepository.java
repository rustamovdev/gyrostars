package ru.lewis.leykabot.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.lewis.leykabot.model.database.entity.Transaction;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Integer> {

    List<Transaction> findByTelegramIdOrderByCreatedAtDesc(Long telegramId);
    List<Transaction> findTop10ByOrderByCreatedAtDesc();

    @Query("SELECT COALESCE(SUM(t.amountRubles), 0) FROM Transaction t WHERE t.telegramId = :telegramId")
    long sumRublesByTelegramId(@Param("telegramId") Long telegramId);

    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.telegramId = :telegramId " +
            "AND t.createdAt BETWEEN :from AND :to")
    long countByTelegramIdAndDateRange(
            @Param("telegramId") Long telegramId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    @Query("SELECT t.telegramId, COALESCE(SUM(CASE WHEN t.amountRubles < 0 THEN -t.amountRubles ELSE t.amountRubles END), 0) AS total " +
            "FROM Transaction t GROUP BY t.telegramId ORDER BY total DESC")
    List<Object[]> findTopByRubles(Pageable pageable);

    @Query("SELECT COALESCE(SUM(t.amountRubles), 0) FROM Transaction t WHERE t.amountRubles > 0")
    long sumAllDepositedRubles();

    @Query("SELECT COALESCE(SUM(CASE WHEN t.amountRubles < 0 THEN -t.amountRubles ELSE t.amountRubles END), 0) FROM Transaction t WHERE t.createdAt BETWEEN :from AND :to")
    long sumRublesBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.createdAt BETWEEN :from AND :to")
    long countBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("SELECT t.telegramId, COALESCE(SUM(CASE WHEN t.amountRubles < 0 THEN -t.amountRubles ELSE t.amountRubles END), 0) AS total " +
            "FROM Transaction t WHERE t.createdAt BETWEEN :from AND :to " +
            "GROUP BY t.telegramId ORDER BY total DESC")
    List<Object[]> findTopByRublesBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to, Pageable pageable);
}
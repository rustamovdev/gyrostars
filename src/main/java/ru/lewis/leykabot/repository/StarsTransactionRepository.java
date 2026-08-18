package ru.lewis.leykabot.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.lewis.leykabot.model.database.entity.StarsTransaction;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface StarsTransactionRepository extends JpaRepository<StarsTransaction, Integer> {

    List<StarsTransaction> findByTelegramIdOrderByCreatedAtDesc(Long telegramId);

    @Query("SELECT COALESCE(SUM(s.amountStars), 0) FROM StarsTransaction s WHERE s.telegramId = :telegramId")
    long sumStarsByTelegramId(@Param("telegramId") Long telegramId);

    @Query("SELECT COUNT(s) FROM StarsTransaction s WHERE s.telegramId = :telegramId")
    long countByTelegramId(@Param("telegramId") Long telegramId);

    @Query("SELECT s.telegramId, COALESCE(SUM(s.amountStars), 0) AS total " +
            "FROM StarsTransaction s GROUP BY s.telegramId ORDER BY total DESC")
    List<Object[]> findTopByStars(Pageable pageable);

    @Query("SELECT COALESCE(SUM(s.amountStars), 0) FROM StarsTransaction s")
    long sumAllStars();

    @Query("SELECT COUNT(s) FROM StarsTransaction s WHERE s.createdAt BETWEEN :from AND :to")
    long countStarsBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("SELECT COALESCE(SUM(s.amountStars), 0) FROM StarsTransaction s WHERE s.createdAt BETWEEN :from AND :to")
    long sumStarsBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("SELECT COALESCE(SUM(s.amountStars), 0) FROM StarsTransaction s WHERE s.telegramId = :telegramId AND s.createdAt BETWEEN :from AND :to")
    long sumStarsByTelegramIdBetween(@Param("telegramId") Long telegramId, @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("SELECT s.telegramId, COALESCE(SUM(s.amountStars), 0) AS total " +
            "FROM StarsTransaction s WHERE s.createdAt BETWEEN :from AND :to " +
            "GROUP BY s.telegramId ORDER BY total DESC")
    List<Object[]> findTopByStarsBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to, Pageable pageable);

    @Query("SELECT COALESCE(SUM(ABS(s.transaction.amountRubles)), 0) FROM StarsTransaction s WHERE s.createdAt BETWEEN :from AND :to")
    long sumStarsRublesBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("SELECT COALESCE(SUM(ABS(s.transaction.amountRubles)), 0) FROM StarsTransaction s")
    long sumAllStarsRubles();
}
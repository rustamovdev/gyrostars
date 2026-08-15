package ru.lewis.leykabot.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.lewis.leykabot.model.database.entity.PubgTransaction;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PubgTransactionRepository extends JpaRepository<PubgTransaction, Long> {

    List<PubgTransaction> findByTelegramIdOrderByCreatedAtDesc(Long telegramId);

    @Query("SELECT COUNT(p) FROM PubgTransaction p WHERE p.telegramId = :telegramId")
    long countByTelegramId(@Param("telegramId") Long telegramId);

    @Query("SELECT COALESCE(SUM(p.ucAmount), 0) FROM PubgTransaction p WHERE p.telegramId = :telegramId")
    long sumUcByTelegramId(@Param("telegramId") Long telegramId);

    @Query("SELECT COUNT(p) FROM PubgTransaction p WHERE p.createdAt BETWEEN :from AND :to")
    long countBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("SELECT COALESCE(SUM(p.ucAmount), 0) FROM PubgTransaction p WHERE p.createdAt BETWEEN :from AND :to")
    long sumUcBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("SELECT COALESCE(SUM(p.priceRubles), 0) FROM PubgTransaction p WHERE p.createdAt BETWEEN :from AND :to")
    long sumPriceBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("SELECT COALESCE(SUM(p.ucAmount), 0) FROM PubgTransaction p WHERE p.telegramId = :telegramId AND p.createdAt BETWEEN :from AND :to")
    long sumUcByTelegramIdBetween(@Param("telegramId") Long telegramId, @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
}

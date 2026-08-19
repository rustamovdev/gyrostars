package ru.lewis.leykabot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.lewis.leykabot.model.database.entity.DepositOrder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface DepositOrderRepository extends JpaRepository<DepositOrder, Long> {

    Optional<DepositOrder> findTopByExactAmountAndStatusAndExpiresAtAfterOrderByIdDesc(
            Integer exactAmount, String status, LocalDateTime now);

    Optional<DepositOrder> findTopByBaseAmountAndStatusAndExpiresAtAfterOrderByIdDesc(
            Integer baseAmount, String status, LocalDateTime now);

    Optional<DepositOrder> findTopByExactAmountAndStatusOrderByIdDesc(
            Integer exactAmount, String status);

    Optional<DepositOrder> findTopByBaseAmountAndStatusOrderByIdDesc(
            Integer baseAmount, String status);

    Optional<DepositOrder> findTopByExactAmountAndStatusAndCreatedAtAfterOrderByIdDesc(
            Integer exactAmount, String status, LocalDateTime after);

    Optional<DepositOrder> findTopByBaseAmountAndStatusAndCreatedAtAfterOrderByIdDesc(
            Integer baseAmount, String status, LocalDateTime after);

    Optional<DepositOrder> findTopByOrderCodeAndStatus(String orderCode, String status);

    Optional<DepositOrder> findTopByUserIdAndStatusAndExpiresAtAfterOrderByIdDesc(
            Long userId, String status, LocalDateTime now);

    List<DepositOrder> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<DepositOrder> findAllByUserIdOrderByCreatedAtDesc(Long userId);

    boolean existsByExactAmountAndStatusAndExpiresAtAfter(
            Integer exactAmount, String status, LocalDateTime now);

    Optional<DepositOrder> findTopByStatusAndExpiresAtAfterAndExactAmountBetweenOrderByIdDesc(
            String status, LocalDateTime now, Integer minAmount, Integer maxAmount);

    List<DepositOrder> findAllByStatusAndExpiresAtBefore(String status, LocalDateTime now);
}

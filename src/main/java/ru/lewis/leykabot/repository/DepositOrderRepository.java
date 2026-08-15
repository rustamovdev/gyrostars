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

    Optional<DepositOrder> findTopByUserIdAndStatusAndExpiresAtAfterOrderByIdDesc(
            Long userId, String status, LocalDateTime now);

    boolean existsByExactAmountAndStatusAndExpiresAtAfter(
            Integer exactAmount, String status, LocalDateTime now);

    List<DepositOrder> findAllByStatusAndExpiresAtBefore(String status, LocalDateTime now);
}

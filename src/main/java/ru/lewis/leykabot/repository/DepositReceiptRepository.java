package ru.lewis.leykabot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.lewis.leykabot.model.database.entity.DepositReceipt;

import java.util.List;

@Repository
public interface DepositReceiptRepository extends JpaRepository<DepositReceipt, Long> {
    List<DepositReceipt> findByStatusOrderByCreatedAtDesc(String status);
}

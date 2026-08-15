package ru.lewis.leykabot.model.database.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "deposit_orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DepositOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_code")
    private String orderCode;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "chat_id", nullable = false)
    private Long chatId;

    @Column(name = "base_amount", nullable = false)
    private Integer baseAmount;

    @Column(name = "exact_amount", nullable = false)
    private Integer exactAmount;

    @Column(name = "card_id")
    private Long cardId;

    @Column(name = "card_info")
    private String cardInfo;

    @Column(name = "status", nullable = false)
    private String status = "PENDING"; // PENDING, PAID_AUTO, PAID_MANUAL, EXPIRED, CANCELLED

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (expiresAt == null) {
            expiresAt = createdAt.plusMinutes(10);
        }
    }
}

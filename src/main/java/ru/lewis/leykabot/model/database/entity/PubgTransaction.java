package ru.lewis.leykabot.model.database.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "pubg_transactions", indexes = {
        @Index(name = "idx_pubg_telegram_id", columnList = "telegramId")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PubgTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "telegramId", nullable = false)
    private Long telegramId;

    @Column(name = "playerId")
    private String playerId;

    @Column(name = "playerNickname")
    private String playerNickname;

    @Column(name = "offerId", nullable = false)
    private String offerId;

    @Column(name = "ucAmount", nullable = false)
    private Integer ucAmount;

    @Column(name = "priceRubles", nullable = false)
    private Integer priceRubles;

    @Column(name = "orderType")
    private String orderType = "uc";

    @Column(name = "apiOrderId")
    private Long apiOrderId;

    @Column(name = "reference")
    private String reference;

    @Column(name = "redeemCode")
    private String redeemCode;

    @Column(name = "status")
    private String status = "COMPLETED";

    @CreationTimestamp
    @Column(name = "createdAt", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}

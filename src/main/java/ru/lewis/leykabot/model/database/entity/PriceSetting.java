package ru.lewis.leykabot.model.database.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "price_settings")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PriceSetting {
    @Id
    @Column(name = "setting_key", unique = true, nullable = false)
    private String key;

    @Column(name = "price_value", nullable = false)
    private Integer price;
}

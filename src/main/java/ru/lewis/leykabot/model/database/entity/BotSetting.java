package ru.lewis.leykabot.model.database.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "bot_settings")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BotSetting {
    @Id
    @Column(name = "setting_key", unique = true, nullable = false)
    private String key;

    @Column(name = "setting_value", length = 1000)
    private String value;
}

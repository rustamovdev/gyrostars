package ru.lewis.leykabot.configuration.telegram;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties("telegram.bot")
public class TelegramBotConfig {
    private String token = "8842520350:AAEUc7rb9S42abHVqyM0WU8sGRupEJkxmSU";
    private String name = "leykabot";

    public String getName() {
        return (name != null && !name.isBlank()) ? name : "leykabot";
    }

    public String getToken() {
        if (token != null && !token.isBlank()) {
            return token;
        }
        String envToken = System.getenv("BOT_TOKEN");
        if (envToken != null && !envToken.isBlank()) {
            return envToken;
        }
        String envTgToken = System.getenv("TELEGRAM_BOT_TOKEN");
        if (envTgToken != null && !envTgToken.isBlank()) {
            return envTgToken;
        }
        return "8842520350:AAEUc7rb9S42abHVqyM0WU8sGRupEJkxmSU";
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setToken(String token) {
        this.token = token;
    }
}

package ru.lewis.leykabot.configuration.telegram;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties("telegram.bot")
public class TelegramBotConfig {
    private String token = "8683889683:AAHG_2tXauL8TBty_G3WNbXEMTXQihUXKqc";
    private String name = "leykabot";

    public String getName() {
        return (name != null && !name.isBlank()) ? name : "leykabot";
    }

    public String getToken() {
        String envTgToken = System.getenv("TELEGRAM_BOT_TOKEN");
        if (envTgToken != null && !envTgToken.isBlank()) {
            return envTgToken.trim();
        }
        String envToken = System.getenv("BOT_TOKEN");
        if (envToken != null && !envToken.isBlank()) {
            return envToken.trim();
        }
        if (token != null && !token.isBlank()) {
            return token.trim();
        }
        return "8683889683:AAHG_2tXauL8TBty_G3WNbXEMTXQihUXKqc";
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setToken(String token) {
        this.token = token;
    }
}

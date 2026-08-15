package ru.lewis.leykabot.configuration;

import okhttp3.OkHttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import ru.lewis.leykabot.configuration.telegram.TelegramBotConfig;

import java.time.Duration;

@Configuration
public class InitializerConfig {

    @Bean
    public TelegramClient telegramClient(TelegramBotConfig config) {
        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(Duration.ofSeconds(60))
                .readTimeout(Duration.ofSeconds(60))
                .writeTimeout(Duration.ofSeconds(60))
                .retryOnConnectionFailure(true)
                .build();

        return new OkHttpTelegramClient(okHttpClient, config.getToken());
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}

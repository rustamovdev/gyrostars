package ru.lewis.leykabot.bot;

import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import ru.lewis.leykabot.configuration.GitCommitConfig;
import ru.lewis.leykabot.configuration.TopFormat;
import ru.lewis.leykabot.configuration.loc.LogMessageConfig;
import ru.lewis.leykabot.configuration.telegram.TelegramBotConfig;
import ru.lewis.leykabot.configuration.telegram.TelegramConfig;
import ru.lewis.leykabot.service.CodeService;
import ru.lewis.leykabot.service.TelegramService;
import ru.lewis.leykabot.service.TopService;

import java.text.MessageFormat;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Component
@Slf4j
@RequiredArgsConstructor
public class BotInitializer {
    private final TelegramBot telegramBot;
    private final TelegramBotConfig config;
    private final TelegramClient telegramClient;
    private final TelegramService telegramService;
    private final LogMessageConfig logMessageConfig;
    private final TelegramConfig telegramConfig;
    private final CodeService codeService;
    private final GitCommitConfig gitCommitConfig;
    private final TopService topService;
    private final TopFormat topFormat;

    private volatile boolean initialized = false;
    private TelegramBotsLongPollingApplication botApp;

    @EventListener(ApplicationReadyEvent.class)
    public void init() {
        if (initialized) return;
        initialized = true;

        log.info("🤖 BotInitializer: Telegram Bot ishga tushirilmoqda... Token mavjud: {}",
                (config.getToken() != null && !config.getToken().isBlank()));

        // Telegram Bot komandalarini (/start, /admin) ro'yxatdan o'tkazish
        try {
            telegramClient.execute(SetMyCommands.builder()
                    .commands(List.of(
                            BotCommand.builder()
                                    .command("start")
                                    .description("🚀 Botni ishga tushirish / Asosiy menyu")
                                    .build(),
                            BotCommand.builder()
                                    .command("admin")
                                    .description("👑 Admin panel (faqat adminlar)")
                                    .build()
                    ))
                    .build());
            log.info("✅ Bot komandalari (/start, /admin) muvaffaqiyatli ro'yxatdan o'tkazildi.");
        } catch (Exception e) {
            log.warn("⚠️ Bot komandalarini o'rnatishda xatolik (davom etiladi): {}", e.getMessage());
        }

        CompletableFuture.allOf(
                codeService.warmUpAllCodes(),
                topService.preloadAll(topFormat.getMaxLimit())
        ).thenAccept(_void -> {
            try {
                String deployMessage = MessageFormat.format(
                        logMessageConfig.getAppEnable(),
                        gitCommitConfig.getHash(),
                        gitCommitConfig.getMessage(),
                        gitCommitConfig.getAuthor()
                );

                telegramService.sendMessageToTopic(
                        telegramConfig.getLogChannelId(),
                        telegramConfig.getLogChannelTopicId(),
                        deployMessage
                );
            } catch (Exception e) {
                log.warn("⚠️ Deploy log xabarini yuborishda xatolik: {}", e.getMessage());
            }
        });

        // Long Polling Botni ro'yxatdan o'tkazish (qayta urinishlar bilan)
        int maxRetries = 5;
        for (int i = 1; i <= maxRetries; i++) {
            try {
                botApp = new TelegramBotsLongPollingApplication();
                botApp.registerBot(config.getToken(), telegramBot);
                log.info("🚀 Telegram Bot Long Polling muvaffaqiyatli ISHGA TUSHDI (Urinish {}/{})!", i, maxRetries);
                break;
            } catch (Exception e) {
                log.error("❌ Botni ro'yxatdan o'tkazishda xatolik (Urinish {}/{}): {}", i, maxRetries, e.getMessage(), e);
                if (i < maxRetries) {
                    try {
                        Thread.sleep(3000);
                    } catch (InterruptedException ignored) {}
                }
            }
        }
    }

    @PreDestroy
    public void cleanup() {
        if (botApp != null) {
            try {
                log.info("🛑 TelegramBotsLongPollingApplication yopilmoqda...");
                botApp.close();
            } catch (Exception e) {
                log.warn("⚠️ BotApp ni yopishda xatolik: {}", e.getMessage());
            }
        }
    }
}

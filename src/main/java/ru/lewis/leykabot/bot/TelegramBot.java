package ru.lewis.leykabot.bot;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import ru.lewis.leykabot.configuration.loc.ButtonsLocConfig;
import ru.lewis.leykabot.configuration.loc.ClientMessageConfig;
import ru.lewis.leykabot.configuration.telegram.TelegramConfig;
import ru.lewis.leykabot.configuration.loc.LogMessageConfig;
import ru.lewis.leykabot.service.*;
import ru.lewis.leykabot.model.screen.ui.ScreenFactory;
import ru.lewis.leykabot.model.screen.ui.ScreenManager;

import java.text.MessageFormat;
import java.util.concurrent.CompletableFuture;

@Component
@AllArgsConstructor
@Slf4j
public class TelegramBot implements LongPollingSingleThreadUpdateConsumer {
    private final TelegramClient telegramClient;
    private final TelegramService telegramService;
    private final ScreenManager screenManager;
    private final ClientMessageConfig clientMessageConfig;
    private final ButtonsLocConfig buttonsLocConfig;
    private final TelegramConfig telegramConfig;
    private final ScreenFactory screenFactory;
    private final UserService userService;
    private final FragmentStarsService fragmentStarsService;
    private final TransactionService transactionService;
    private final CodeService codeService;
    private final LogMessageConfig logMessageConfig;
    private final StarsTransactionService starsTransactionService;
    private final PremiumTransactionService premiumTransactionService;
    private final AdminService adminService;
    private final PaymentCardService paymentCardService;

    @Override
    public void consume(Update update) {
        if (update.hasCallbackQuery()) {
            var callback = update.getCallbackQuery();
            var chatId = callback.getMessage().getChatId();
            var data = callback.getData();
            var messageId = callback.getMessage().getMessageId();
            var fromId = callback.getFrom().getId();

            try {
                telegramClient.execute(org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery.builder()
                        .callbackQueryId(callback.getId())
                        .build());
            } catch (Exception ignored) {}

            if (data.startsWith("dep_app_")) {
                try {
                    long receiptId = Long.parseLong(data.substring(8));
                    if (paymentCardService.approveDeposit(receiptId, fromId)) {
                        String adminTag = telegramService.getUsernameByUserId(fromId);
                        String oldCaption = "";
                        if (callback.getMessage() instanceof org.telegram.telegrambots.meta.api.objects.message.Message msg) {
                            oldCaption = msg.getCaption() != null ? msg.getCaption() : "";
                        }
                        telegramClient.execute(org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageCaption.builder()
                                .chatId(chatId)
                                .messageId(messageId)
                                .caption(oldCaption + "\n\n✅ <b>TASDIQLANDI!</b> (Admin: " + (adminTag != null ? adminTag : fromId) + ")")
                                .parseMode("HTML")
                                .build());
                    }
                } catch (Exception ignored) {}
                return;
            } else if (data.startsWith("dep_rej_")) {
                try {
                    long receiptId = Long.parseLong(data.substring(8));
                    if (paymentCardService.rejectDeposit(receiptId, fromId)) {
                        String adminTag = telegramService.getUsernameByUserId(fromId);
                        String oldCaption = "";
                        if (callback.getMessage() instanceof org.telegram.telegrambots.meta.api.objects.message.Message msg) {
                            oldCaption = msg.getCaption() != null ? msg.getCaption() : "";
                        }
                        telegramClient.execute(org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageCaption.builder()
                                .chatId(chatId)
                                .messageId(messageId)
                                .caption(oldCaption + "\n\n❌ <b>RAD ETILDI!</b> (Admin: " + (adminTag != null ? adminTag : fromId) + ")")
                                .parseMode("HTML")
                                .build());
                    }
                } catch (Exception ignored) {}
                return;
            }

            screenManager.handleCallback(chatId, data, messageId);
            return;
        }

        if (!update.hasMessage() || update.getMessage() == null) {
            return;
        }

        var message = update.getMessage();
        var userId = message.getFrom().getId();
        var chatId = message.getChatId();

        if (message.hasPhoto() && message.getPhoto() != null && !message.getPhoto().isEmpty()) {
            screenManager.handlePhoto(chatId, message.getPhoto());
            return;
        }

        if (message.hasDocument() && message.getDocument() != null) {
            screenManager.handleDocument(chatId, message.getDocument());
            return;
        }

        if (!message.hasText() || message.getText() == null) {
            return;
        }

        var text = message.getText();

        if (text.startsWith("/admin") && adminService.isAdmin(userId)) {
            screenManager.createScreen(chatId, screenFactory.createAdminMainScreen(chatId, userId));
            return;
        }

        start(text, userId, chatId);

        screenManager.handleMessage(chatId, text);
    }

    private void start(String message, Long userId, Long chatId) {
        // command check
        if (message.startsWith("/start")) {
            // check sub
            if (!telegramService.isUserSubscribed(userId)) {
                screenManager.createScreen(chatId, screenFactory.createSubscribeChannelScreen(chatId, userId));
                return;
            }

            // save user id DB if not exists
            if (!userService.isUserExists(userId)) {
                userService.createUser(userId);
            }

            CompletableFuture.allOf(
                    premiumTransactionService.preload(userId),
                    starsTransactionService.preload(userId),
                    userService.warmUpAll(userId),
                    transactionService.preload(userId),
                    codeService.warmUpAll(userId)
            ).thenRun(() ->
                    screenManager.createScreen(chatId, screenFactory.createStartScreen(chatId, userId))
            );
        }
    }
}
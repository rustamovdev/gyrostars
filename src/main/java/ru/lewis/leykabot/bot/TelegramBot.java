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
    private final AntiFloodService antiFloodService;

    @Override
    public void consume(Update update) {
        try {
            if (update.hasMessage() && update.getMessage() != null && update.getMessage().getText() != null) {
                log.info("📩 Message received: user={}, text='{}'", update.getMessage().getFrom().getId(), update.getMessage().getText());
            } else if (update.hasCallbackQuery()) {
                log.info("🔘 Callback received: user={}, data='{}'", update.getCallbackQuery().getFrom().getId(), update.getCallbackQuery().getData());
            }
            processUpdate(update);
        } catch (Exception e) {
            log.error("❌ Unhandled error processing update: ", e);
        }
    }

    private void processUpdate(Update update) {
        if (update.hasCallbackQuery()) {
            var callback = update.getCallbackQuery();
            var chatId = callback.getMessage().getChatId();
            var data = callback.getData();
            var messageId = callback.getMessage().getMessageId();
            var fromId = callback.getFrom().getId();

            // Real-time user registration
            if (fromId != null && fromId > 0 && !userService.isUserExists(fromId)) {
                userService.createUser(fromId);
            }

            // Flood tekshiruvi
            if (antiFloodService.isFlooding(fromId, chatId)) {
                try {
                    telegramClient.execute(org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery.builder()
                            .callbackQueryId(callback.getId())
                            .text("⏳ Iltimos, juda tez bosmang!")
                            .showAlert(false)
                            .build());
                } catch (Exception ignored) {}
                return;
            }

            // Maintenance mode tekshiruvi (Bot o'chirilgan holat)
            if (adminService.isMaintenanceMode() && !adminService.isAdmin(fromId)) {
                try {
                    telegramClient.execute(org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery.builder()
                            .callbackQueryId(callback.getId())
                            .text("🛠 Botda texnik ishlar olib borilmoqda! Bot vaqtincha to‘xtatilgan.")
                            .showAlert(true)
                            .build());
                } catch (Exception ignored) {}
                return;
            }

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

        // Real-time user registration
        if (userId != null && userId > 0 && !userService.isUserExists(userId)) {
            userService.createUser(userId);
        }

        // Flood tekshiruvi
        if (antiFloodService.isFlooding(userId, chatId)) {
            return;
        }

        // Maintenance mode tekshiruvi (Bot o'chirilgan holat)
        if (adminService.isMaintenanceMode() && !adminService.isAdmin(userId)) {
            telegramService.sendMessageAuto(chatId, "🛠 <b>Botda texnik ishlar olib borilmoqda!</b>\n\nBot vaqtincha to‘xtatilgan. Tez orada qayta ishga tushadi.");
            return;
        }

        if (message.hasPhoto() && message.getPhoto() != null && !message.getPhoto().isEmpty()) {
            screenManager.handlePhoto(chatId, message.getPhoto());
            return;
        }

        if (message.hasDocument() && message.getDocument() != null) {
            screenManager.handleDocument(chatId, message.getDocument());
            return;
        }

        var text = message.getText();
        if (text == null) return;
        String trimmed = text.trim();
        String lower = trimmed.toLowerCase();

        if (lower.startsWith("/admin") && adminService.isAdmin(userId)) {
            screenManager.createScreen(chatId, screenFactory.createAdminMainScreen(chatId, userId));
            return;
        }

        if (lower.startsWith("/start")) {
            start(trimmed, userId, chatId);
            return;
        }

        screenManager.handleMessage(chatId, text);
    }

    private void start(String message, Long userId, Long chatId) {
        log.info("🚀 Handling /start for userId: {}, chatId: {}", userId, chatId);
        // check sub
        if (!telegramService.isUserSubscribed(userId)) {
            screenManager.createScreen(chatId, screenFactory.createSubscribeChannelScreen(chatId, userId));
            return;
        }

        // Referral tekshirish
        Long referrerId = null;
        String[] parts = message.split("\\s+");
        if (parts.length > 1) {
            String payload = parts[1].trim().toLowerCase();
            if (payload.startsWith("u")) {
                payload = payload.substring(1);
            } else if (payload.startsWith("ref")) {
                payload = payload.substring(3);
            }
            try {
                referrerId = Long.parseLong(payload);
            } catch (NumberFormatException ignored) {}
        }

        // save user in DB if not exists
        if (!userService.isUserExists(userId)) {
            userService.createUser(userId, referrerId);
        }

        screenManager.createScreen(chatId, screenFactory.createStartScreen(chatId, userId));
        log.info("✅ Start screen successfully created for userId: {}", userId);
    }
}
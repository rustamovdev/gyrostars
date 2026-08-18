package ru.lewis.leykabot.model.screen.ui.impl;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import ru.lewis.leykabot.configuration.loc.ButtonsLocConfig;
import ru.lewis.leykabot.configuration.loc.ClientMessageConfig;
import ru.lewis.leykabot.model.button.StyledInlineButton;
import ru.lewis.leykabot.model.screen.ui.AbstractScreen;
import ru.lewis.leykabot.model.screen.ui.ScreenFactory;
import ru.lewis.leykabot.model.screen.ui.ScreenManager;
import ru.lewis.leykabot.service.*;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

public class ProfileScreen extends AbstractScreen {
    private final ButtonsLocConfig buttonsLocConfig;
    private final ClientMessageConfig clientMessageConfig;
    private final ScreenManager screenManager;
    private final ScreenFactory screenFactory;
    private final TelegramService telegramService;
    private final UserService userService;
    private final TransactionService transactionService;
    private final StarsTransactionService starsTransactionService;
    private final PremiumTransactionService premiumTransactionService;
    private final CodeService codeService;

    private boolean isWaitingPromo = false;

    public ProfileScreen(Long chatId, Long userId, ButtonsLocConfig buttonsLocConfig,
                         ClientMessageConfig clientMessageConfig,
                         ScreenManager screenManager,
                         TelegramService telegramService,
                         UserService userService,
                         TransactionService transactionService,
                         StarsTransactionService starsTransactionService,
                         PremiumTransactionService premiumTransactionService,
                         CodeService codeService,
                         ScreenFactory screenFactory) {
        super(chatId, userId);
        this.buttonsLocConfig = buttonsLocConfig;
        this.clientMessageConfig = clientMessageConfig;
        this.screenManager = screenManager;
        this.telegramService = telegramService;
        this.userService = userService;
        this.transactionService = transactionService;
        this.starsTransactionService = starsTransactionService;
        this.premiumTransactionService = premiumTransactionService;
        this.codeService = codeService;
        this.screenFactory = screenFactory;
    }

    @Override
    public void handleCallback(String callback, TelegramClient bot) {
        switch (callback) {
            case "back" -> {
                isWaitingPromo = false;
                screenManager.updateScreen(chatId, screenFactory.createStartScreen(chatId, userId));
            }
            case "deposit" -> {
                isWaitingPromo = false;
                screenManager.updateScreen(chatId, screenFactory.createDepositRublesScreen(chatId, userId));
            }
            case "top" -> {
                isWaitingPromo = false;
                screenManager.updateScreen(chatId, screenFactory.createTopSelectScreen(chatId, userId));
            }
            case "promo" -> {
                isWaitingPromo = true;
                telegramService.sendMessageAuto(chatId, "🎟 <b>Promokod faollashtirish:</b>\n\nIltimos, promokod matnini yozib yuboring (Masalan: <code>GYRO2026</code>):");
            }
            case "referral" -> {
                isWaitingPromo = false;
                screenManager.updateScreen(chatId, screenFactory.createReferralScreen(chatId, userId));
            }
        }
    }

    @Override
    public void handleMessage(String text, TelegramClient bot) {
        if (isWaitingPromo && text != null && !text.isBlank()) {
            isWaitingPromo = false;
            CodeService.ActivationResult res = codeService.activateCode(userId, text.trim().toUpperCase());
            if (res.success()) {
                String bonusFormatted = String.format("%,d", res.bonusAmount()).replace(',', ' ');
                telegramService.sendMessageAuto(chatId, "✅ <b>Promokod muvaffaqiyatli faollashtirildi!</b>\n\n🎁 Balansingizga <b>+" + bonusFormatted + " so‘m</b> qo‘shildi!");
            } else {
                telegramService.sendMessageAuto(chatId, "❌ " + res.message());
            }
            screenManager.updateScreen(chatId, screenFactory.createProfileScreen(chatId, userId));
        }
    }

    @Override
    protected InlineKeyboardMarkup getKeyboard() {
        List<InlineKeyboardRow> keyboard = new ArrayList<>();

        InlineKeyboardRow row1 = new InlineKeyboardRow();
        row1.add(StyledInlineButton.styledBuilder()
                .text("Balansni to‘ldirish")
                .callbackData("deposit")
                .style("success")
                .iconCustomEmojiId("5890848474563352982")
                .build());
        row1.add(StyledInlineButton.styledBuilder()
                .text("Reyting")
                .callbackData("top")
                .style("primary")
                .iconCustomEmojiId("5436201215341930329")
                .build());

        InlineKeyboardRow rowPromo = new InlineKeyboardRow();
        rowPromo.add(StyledInlineButton.styledBuilder()
                .text("Promokod faollashtirish")
                .callbackData("promo")
                .style("primary")
                .iconCustomEmojiId("5443015509589138475")
                .build());
        rowPromo.add(StyledInlineButton.styledBuilder()
                .text("Referral")
                .callbackData("referral")
                .style("success")
                .iconCustomEmojiId("5271604874419647061")
                .build());

        InlineKeyboardRow row2 = new InlineKeyboardRow();
        row2.add(StyledInlineButton.styledBuilder()
                .text("Orqaga")
                .callbackData("back")
                .style("primary")
                .iconCustomEmojiId("5258236805890710909")
                .build());

        keyboard.add(row1);
        keyboard.add(rowPromo);
        keyboard.add(row2);

        return InlineKeyboardMarkup.builder()
                .keyboard(keyboard)
                .build();
    }

    @Override
    public String getText() {
        long balance = userService.getBalance(userId).orElse(0);
        long monthlyStars = starsTransactionService.getCurrentMonthStars(userId);

        return MessageFormat.format(clientMessageConfig.getProfileCommand(),
                telegramService.getUsernameByUserId(userId),
                String.valueOf(userId),
                String.format("%,d", balance).replace(',', ' '),
                String.format("%,d", monthlyStars).replace(',', ' '));
    }
}

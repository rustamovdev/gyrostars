package ru.lewis.leykabot.model.screen.ui.impl;

import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import ru.lewis.leykabot.configuration.loc.ButtonsLocConfig;
import ru.lewis.leykabot.configuration.loc.ClientMessageConfig;
import ru.lewis.leykabot.configuration.telegram.TelegramConfig;
import ru.lewis.leykabot.model.screen.ui.AbstractScreen;
import ru.lewis.leykabot.model.screen.ui.ScreenFactory;
import ru.lewis.leykabot.model.screen.ui.ScreenManager;
import ru.lewis.leykabot.service.AdminService;
import ru.lewis.leykabot.service.TelegramService;

import java.util.ArrayList;
import java.util.List;

public class StartScreen extends AbstractScreen {
    private final ClientMessageConfig clientMessageConfig;
    private final ButtonsLocConfig buttonsLocConfig;
    private final ScreenManager screenManager;
    private final ScreenFactory screenFactory;
    private final TelegramConfig telegramConfig;
    private final TelegramService telegramService;
    private final AdminService adminService;

    public StartScreen(Long chatId, Long userId,
                       ClientMessageConfig clientMessageConfig,
                       ButtonsLocConfig buttonsLocConfig,
                       ScreenManager screenManager,
                       TelegramService telegramService,
                       TelegramConfig telegramConfig,
                       AdminService adminService,
                       ScreenFactory screenFactory) {
        super(chatId, userId);
        this.clientMessageConfig = clientMessageConfig;
        this.buttonsLocConfig = buttonsLocConfig;
        this.screenManager = screenManager;
        this.telegramService = telegramService;
        this.screenFactory = screenFactory;
        this.telegramConfig = telegramConfig;
        this.adminService = adminService;
    }

    @Override
    public void render(TelegramClient bot) {
        Message message = telegramService.sendMessageAuto(chatId, getText(), getKeyboard());
        if (message != null) {
            this.currentMessageId = message.getMessageId();
        }
    }

    @Override
    public void handleCallback(String callback, TelegramClient bot) {
        switch (callback) {
            case "buy-stars" -> screenManager.updateScreen(chatId, screenFactory.createBuyStarsScreen(chatId, userId));
            case "buy-premium" -> screenManager.updateScreen(chatId, screenFactory.createBuyPremiumScreen(chatId, userId));
            case "profile" -> screenManager.updateScreen(chatId, screenFactory.createProfileScreen(chatId, userId));
            case "deposit" -> screenManager.updateScreen(chatId, screenFactory.createDepositRublesScreen(chatId, userId));
            case "top" -> screenManager.updateScreen(chatId, screenFactory.createTopSelectScreen(chatId, userId));
            case "support" -> screenManager.updateScreen(chatId, screenFactory.createSupportScreen(chatId, userId));
            case "admin" -> {
                if (adminService.isAdmin(userId)) {
                    screenManager.updateScreen(chatId, screenFactory.createAdminMainScreen(chatId, userId));
                }
            }
        }
    }

    @Override
    public String getText() {
        return clientMessageConfig.getStartCommand();
    }

    @Override
    protected InlineKeyboardMarkup getKeyboard() {
        List<InlineKeyboardRow> keyboard = new ArrayList<>();

        // Row 1: Stars va Premium xaridi
        InlineKeyboardRow row1 = new InlineKeyboardRow();
        row1.add(ru.lewis.leykabot.model.button.StyledInlineButton.styledBuilder()
                .text("Stars sotib olish")
                .callbackData("buy-stars")
                .style("primary")
                .iconCustomEmojiId("5985826831591281620")
                .build());
        row1.add(ru.lewis.leykabot.model.button.StyledInlineButton.styledBuilder()
                .text("Premium sotib olish")
                .callbackData("buy-premium")
                .style("primary")
                .iconCustomEmojiId("5938420017665152105")
                .build());

        // Row 2: Profil va Hisob to'ldirish
        InlineKeyboardRow row2 = new InlineKeyboardRow();
        row2.add(ru.lewis.leykabot.model.button.StyledInlineButton.styledBuilder()
                .text("Profil")
                .callbackData("profile")
                .style("primary")
                .iconCustomEmojiId("5256143829672672750")
                .build());
        row2.add(ru.lewis.leykabot.model.button.StyledInlineButton.styledBuilder()
                .text("Balansni to‘ldirish")
                .callbackData("deposit")
                .style("success")
                .iconCustomEmojiId("5436171485578308032")
                .build());

        // Row 3: Reyting va Qo'llab-quvvatlash
        InlineKeyboardRow row3 = new InlineKeyboardRow();
        row3.add(ru.lewis.leykabot.model.button.StyledInlineButton.styledBuilder()
                .text("Reyting")
                .callbackData("top")
                .style("primary")
                .iconCustomEmojiId("5436201215341930329")
                .build());
        row3.add(ru.lewis.leykabot.model.button.StyledInlineButton.styledBuilder()
                .text("Qo‘llab-quvvatlash")
                .callbackData("support")
                .style("primary")
                .iconCustomEmojiId("5436304616679580574")
                .build());

        keyboard.add(row1);
        keyboard.add(row2);
        keyboard.add(row3);

        // Row 5: Admin panel (faqat adminlarga ko'rinadi)
        if (adminService.isAdmin(userId)) {
            InlineKeyboardRow adminRow = new InlineKeyboardRow();
            adminRow.add(ru.lewis.leykabot.model.button.StyledInlineButton.styledBuilder()
                    .text("👑 Admin Panel")
                    .callbackData("admin")
                    .style("danger")
                    .build());
            keyboard.add(adminRow);
        }

        return InlineKeyboardMarkup.builder()
                .keyboard(keyboard)
                .build();
    }
}
package ru.lewis.leykabot.model.screen.ui.impl;

import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import ru.lewis.leykabot.configuration.loc.ButtonsLocConfig;
import ru.lewis.leykabot.configuration.loc.ClientMessageConfig;
import ru.lewis.leykabot.configuration.telegram.TelegramConfig;
import ru.lewis.leykabot.model.button.StyledInlineButton;
import ru.lewis.leykabot.model.screen.ui.AbstractScreen;
import ru.lewis.leykabot.model.screen.ui.ScreenFactory;
import ru.lewis.leykabot.model.screen.ui.ScreenManager;
import ru.lewis.leykabot.service.AdminService;
import ru.lewis.leykabot.service.TelegramService;
import ru.lewis.leykabot.service.UserService;

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
    private final UserService userService;

    public StartScreen(Long chatId, Long userId,
                       ClientMessageConfig clientMessageConfig,
                       ButtonsLocConfig buttonsLocConfig,
                       ScreenManager screenManager,
                       TelegramService telegramService,
                       TelegramConfig telegramConfig,
                       AdminService adminService,
                       UserService userService,
                       ScreenFactory screenFactory) {
        super(chatId, userId);
        this.clientMessageConfig = clientMessageConfig;
        this.buttonsLocConfig = buttonsLocConfig;
        this.screenManager = screenManager;
        this.telegramService = telegramService;
        this.screenFactory = screenFactory;
        this.telegramConfig = telegramConfig;
        this.adminService = adminService;
        this.userService = userService;
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
            case "buy-pubg" -> screenManager.updateScreen(chatId, screenFactory.createPubgBuyScreen(chatId, userId));
            case "buy-premium" -> screenManager.updateScreen(chatId, screenFactory.createBuyPremiumScreen(chatId, userId));
            case "referral" -> screenManager.updateScreen(chatId, screenFactory.createReferralScreen(chatId, userId));
            case "deposit" -> screenManager.updateScreen(chatId, screenFactory.createDepositRublesScreen(chatId, userId));
            case "profile" -> screenManager.updateScreen(chatId, screenFactory.createProfileScreen(chatId, userId));
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
        String username = "Foydalanuvchi";
        try {
            String u = telegramService.getUsernameByUserId(userId);
            if (u != null && !u.isBlank()) username = u;
        } catch (Exception ignored) {}

        int balance = 0;
        try {
            if (userService != null) {
                balance = userService.getBalance(userId).orElse(0);
            }
        } catch (Exception ignored) {}

        String formattedBalance = String.format("%,d", balance).replace(',', ' ');

        String text = "<tg-emoji emoji-id=\"5436173070421238756\">👋</tg-emoji> Assalomu alaykum, " + username + "!\n\n" +
                "<tg-emoji emoji-id=\"5436172829903068620\">🆔</tg-emoji> <b>User ID:</b> <code>" + userId + "</code>\n" +
                "└ <tg-emoji emoji-id=\"5436203328465838905\">💳</tg-emoji> <b>Balans:</b> " + formattedBalance + " so'm\n\n" +
                "🛍 <b>Barcha xizmatlar (Stars, Premium, PUBG UC, Sovg'alar va Balans to'ldirish)</b> qulay <b>Mini App</b> orqali amalga oshiriladi.\n\n" +
                "Ilovani ochish uchun quyidagi tugmani bosing 👇";

        return text + ";images/image.png";
    }

    @Override
    protected InlineKeyboardMarkup getKeyboard() {
        List<InlineKeyboardRow> keyboard = new ArrayList<>();

        // Row 1: WebApp ochish (Asosiy tugma)
        InlineKeyboardRow webAppRow = new InlineKeyboardRow();
        webAppRow.add(StyledInlineButton.styledBuilder()
                .text("WebAppni ochish")
                .webApp(new org.telegram.telegrambots.meta.api.objects.webapp.WebAppInfo("https://gyrostars.onrender.com/index.html"))
                .style("primary")
                .iconCustomEmojiId("5296770844448561710")
                .build());
        keyboard.add(webAppRow);

        // Row 2: Qo'llab-quvvatlash
        InlineKeyboardRow supportRow = new InlineKeyboardRow();
        supportRow.add(StyledInlineButton.styledBuilder()
                .text("Qo‘llab-quvvatlash")
                .callbackData("support")
                .style("primary")
                .iconCustomEmojiId("5436304616679580574")
                .build());
        keyboard.add(supportRow);

        // Admin bo'lsa - Admin Panel
        if (adminService.isAdmin(userId)) {
            InlineKeyboardRow adminRow = new InlineKeyboardRow();
            adminRow.add(StyledInlineButton.styledBuilder()
                    .text("👑 Admin Panel")
                    .callbackData("admin")
                    .style("danger")
                    .build());
            keyboard.add(adminRow);
        }

        return InlineKeyboardMarkup.builder().keyboard(keyboard).build();
    }
}
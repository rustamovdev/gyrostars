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
            case "gift" -> screenManager.updateScreen(chatId, screenFactory.createGiftSelectScreen(chatId, userId));
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

        String text = "<tg-emoji emoji-id=\"5436173070421238756\">👋</tg-emoji> Assalomu alaykum, " + username + "\n\n" +
                "<tg-emoji emoji-id=\"5436172829903068620\">🆔</tg-emoji> <b>User ID:</b> <code>" + userId + "</code>\n" +
                "└ <tg-emoji emoji-id=\"5436203328465838905\">💳</tg-emoji> <b>Balans:</b> " + formattedBalance + " so'm";

        return text + ";images/image.png";
    }

    @Override
    protected InlineKeyboardMarkup getKeyboard() {
        List<InlineKeyboardRow> keyboard = new ArrayList<>();

        // Row 1: STARS (Full width)
        InlineKeyboardRow row1 = new InlineKeyboardRow();
        row1.add(StyledInlineButton.styledBuilder()
                .text("STARS")
                .callbackData("buy-stars")
                .style("primary")
                .iconCustomEmojiId("5985826831591281620")
                .build());

        // Row 2: PUBG UC DONAT QILISH
        InlineKeyboardRow row2 = new InlineKeyboardRow();
        row2.add(StyledInlineButton.styledBuilder()
                .text("PUBG UC DONAT QILISH")
                .callbackData("buy-pubg")
                .style("primary")
                .iconCustomEmojiId("5204252919565657978")
                .build());

        // Row 3: Premium (Full width)
        InlineKeyboardRow row3 = new InlineKeyboardRow();
        row3.add(StyledInlineButton.styledBuilder()
                .text("Premium")
                .callbackData("buy-premium")
                .style("primary")
                .iconCustomEmojiId("5938420017665152105")
                .build());

        // Row 4: Hisob to'ldirish & Kabinet
        InlineKeyboardRow row4 = new InlineKeyboardRow();
        row4.add(StyledInlineButton.styledBuilder()
                .text("Hisob to‘ldirish")
                .callbackData("deposit")
                .style("success")
                .iconCustomEmojiId("5890848474563352982")
                .build());
        row4.add(StyledInlineButton.styledBuilder()
                .text("Kabinet")
                .callbackData("profile")
                .style("primary")
                .iconCustomEmojiId("5256143829672672750")
                .build());

        // Row 5: Reyting & Yordam
        InlineKeyboardRow row5 = new InlineKeyboardRow();
        row5.add(StyledInlineButton.styledBuilder()
                .text("Reyting")
                .callbackData("top")
                .style("primary")
                .iconCustomEmojiId("5436201215341930329")
                .build());
        row5.add(StyledInlineButton.styledBuilder()
                .text("Yordam")
                .callbackData("support")
                .style("primary")
                .iconCustomEmojiId("5436304616679580574")
                .build());

        // Row 6: Referral (Full width with custom emoji from Ulashish)
        InlineKeyboardRow row6 = new InlineKeyboardRow();
        row6.add(StyledInlineButton.styledBuilder()
                .text("🔗 Referral (Bonus olish)")
                .callbackData("referral")
                .style("success")
                .iconCustomEmojiId("5271604874419647061")
                .build());

        keyboard.add(row1);
        keyboard.add(row2);
        keyboard.add(row3);
        keyboard.add(row4);
        keyboard.add(row5);
        keyboard.add(row6);

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
package ru.lewis.leykabot.model.screen.ui.impl;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import ru.lewis.leykabot.model.button.StyledInlineButton;
import ru.lewis.leykabot.model.screen.ui.AbstractScreen;
import ru.lewis.leykabot.model.screen.ui.ScreenFactory;
import ru.lewis.leykabot.model.screen.ui.ScreenManager;
import ru.lewis.leykabot.service.TelegramService;
import ru.lewis.leykabot.service.UserService;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class ReferralScreen extends AbstractScreen {

    private final ScreenManager screenManager;
    private final ScreenFactory screenFactory;
    private final UserService userService;
    private final TelegramService telegramService;

    public ReferralScreen(Long chatId, Long userId,
                          ScreenManager screenManager,
                          ScreenFactory screenFactory,
                          UserService userService,
                          TelegramService telegramService) {
        super(chatId, userId);
        this.screenManager = screenManager;
        this.screenFactory = screenFactory;
        this.userService = userService;
        this.telegramService = telegramService;
    }

    @Override
    public void handleCallback(String callback, TelegramClient bot) {
        if ("back".equals(callback)) {
            screenManager.updateScreen(chatId, screenFactory.createStartScreen(chatId, userId));
        }
    }

    @Override
    public String getText() {
        long refCount = userService.getReferralsCount(userId);
        String botUsername = telegramService.getBotUsername();
        String refLink = "https://t.me/" + botUsername + "?start=u" + userId;

        return "<tg-emoji emoji-id=\"5422736199343168249\">🔗</tg-emoji> <b>Referral Dasturi</b>\n\n" +
                "Do‘stlaringizni taklif qiling va bot orqali bonuslarga ega bo‘ling!\n\n" +
                "👥 <b>Siz taklif qilganlar:</b> <b>" + refCount + " ta</b>\n" +
                "💰 <b>Referral balansi:</b> <b>0 so‘m</b>\n\n" +
                "🔗 <b>Sizning referral havolangiz:</b>\n" +
                "<code>" + refLink + "</code>";
    }

    @Override
    protected InlineKeyboardMarkup getKeyboard() {
        List<InlineKeyboardRow> keyboard = new ArrayList<>();

        String botUsername = telegramService.getBotUsername();
        String refLink = "https://t.me/" + botUsername + "?start=u" + userId;
        String shareText = "Telegram Stars va Premium sotib olish uchun eng qulay va ishonchli bot!";
        String shareUrl = "https://t.me/share/url?url=" + URLEncoder.encode(refLink, StandardCharsets.UTF_8)
                + "&text=" + URLEncoder.encode(shareText, StandardCharsets.UTF_8);

        InlineKeyboardRow row1 = new InlineKeyboardRow();
        row1.add(InlineKeyboardButton.builder()
                .text("🚀 Do‘stlarga ulashish")
                .url(shareUrl)
                .build());

        InlineKeyboardRow row2 = new InlineKeyboardRow();
        row2.add(StyledInlineButton.styledBuilder()
                .text("Orqaga")
                .callbackData("back")
                .iconCustomEmojiId("5258236805890710909")
                .build());

        keyboard.add(row1);
        keyboard.add(row2);

        return InlineKeyboardMarkup.builder().keyboard(keyboard).build();
    }
}

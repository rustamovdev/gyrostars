package ru.lewis.leykabot.model.screen.ui.impl;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import ru.lewis.leykabot.model.button.StyledInlineButton;
import ru.lewis.leykabot.model.screen.ui.AbstractScreen;
import ru.lewis.leykabot.model.screen.ui.ScreenFactory;
import ru.lewis.leykabot.model.screen.ui.ScreenManager;

import java.util.ArrayList;
import java.util.List;

public class GiftSelectScreen extends AbstractScreen {

    private final ScreenManager screenManager;
    private final ScreenFactory screenFactory;

    public GiftSelectScreen(Long chatId, Long userId,
                            ScreenManager screenManager,
                            ScreenFactory screenFactory) {
        super(chatId, userId);
        this.screenManager = screenManager;
        this.screenFactory = screenFactory;
    }

    @Override
    public void handleCallback(String callback, TelegramClient bot) {
        switch (callback) {
            case "gift_stars" -> screenManager.updateScreen(chatId, screenFactory.createBuyStarsScreen(chatId, userId));
            case "gift_pubg" -> screenManager.updateScreen(chatId, screenFactory.createPubgBuyScreen(chatId, userId));
            case "gift_premium" -> screenManager.updateScreen(chatId, screenFactory.createBuyPremiumScreen(chatId, userId));
            case "back" -> screenManager.updateScreen(chatId, screenFactory.createStartScreen(chatId, userId));
        }
    }

    @Override
    public String getText() {
        return "<tg-emoji emoji-id=\"5938420017665152105\">🎁</tg-emoji> <b>Sovg‘a yuborish (Gift)</b>\n\n" +
                "Do‘stingizga <b>Telegram Stars</b>, <b>PUBG UC</b> yoki <b>Telegram Premium</b> sovg‘a qiling!\n\n" +
                "Kerakli xizmat turini tanlang 👇";
    }

    @Override
    protected InlineKeyboardMarkup getKeyboard() {
        List<InlineKeyboardRow> keyboard = new ArrayList<>();

        InlineKeyboardRow row1 = new InlineKeyboardRow();
        row1.add(StyledInlineButton.styledBuilder()
                .text("⭐️ Stars Sovg‘a qilish")
                .callbackData("gift_stars")
                .style("primary")
                .iconCustomEmojiId("5985826831591281620")
                .build());

        InlineKeyboardRow row2 = new InlineKeyboardRow();
        row2.add(StyledInlineButton.styledBuilder()
                .text("🎮 PUBG UC Sovg‘a qilish")
                .callbackData("gift_pubg")
                .style("primary")
                .iconCustomEmojiId("5436050603723760533")
                .build());

        InlineKeyboardRow row3 = new InlineKeyboardRow();
        row3.add(StyledInlineButton.styledBuilder()
                .text("💎 Premium Sovg‘a qilish")
                .callbackData("gift_premium")
                .style("primary")
                .iconCustomEmojiId("5938420017665152105")
                .build());

        InlineKeyboardRow row4 = new InlineKeyboardRow();
        row4.add(StyledInlineButton.styledBuilder()
                .text("Orqaga")
                .callbackData("back")
                .iconCustomEmojiId("5258236805890710909")
                .build());

        keyboard.add(row1);
        keyboard.add(row2);
        keyboard.add(row3);
        keyboard.add(row4);

        return InlineKeyboardMarkup.builder().keyboard(keyboard).build();
    }
}

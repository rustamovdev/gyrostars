package ru.lewis.leykabot.model.screen.ui.impl;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import ru.lewis.leykabot.model.button.StyledInlineButton;
import ru.lewis.leykabot.model.screen.ui.AbstractScreen;
import ru.lewis.leykabot.model.screen.ui.ScreenFactory;
import ru.lewis.leykabot.model.screen.ui.ScreenManager;
import ru.lewis.leykabot.service.GiftService;
import ru.lewis.leykabot.service.PriceService;

import java.util.ArrayList;
import java.util.List;

public class GiftCatalogScreen extends AbstractScreen {

    private final ScreenManager screenManager;
    private final ScreenFactory screenFactory;
    private final GiftService giftService;
    private final PriceService priceService;
    private final String category; // "unlimited" or "unique"

    public GiftCatalogScreen(Long chatId, Long userId,
                             String category,
                             ScreenManager screenManager,
                             ScreenFactory screenFactory,
                             GiftService giftService,
                             PriceService priceService) {
        super(chatId, userId);
        this.category = (category != null && !category.isBlank()) ? category : "unlimited";
        this.screenManager = screenManager;
        this.screenFactory = screenFactory;
        this.giftService = giftService;
        this.priceService = priceService;
    }

    @Override
    public void handleCallback(String callback, TelegramClient bot) {
        if ("back".equals(callback)) {
            screenManager.updateScreen(chatId, screenFactory.createGiftSelectScreen(chatId, userId));
            return;
        }

        if (callback.startsWith("buy_gift_")) {
            String giftId = callback.substring("buy_gift_".length());
            screenManager.updateScreen(chatId, screenFactory.createGiftOrderScreen(chatId, userId, giftId));
        }
    }

    @Override
    public String getText() {
        boolean isUnique = "unique".equalsIgnoreCase(category);
        if (isUnique) {
            return "💎 <b>Noyob (Unique / NFT) Sovg‘alar Katalogi</b>\n\n" +
                    "Ushbu sovg‘alar cheklangan tirajda bo‘lib, har birining unikal seriya raqami va atributlari mavjud.\n\n" +
                    "Sotib olmoqchi bo‘lgan sovg‘angizni tanlang 👇";
        }
        return "🎁 <b>Rasmiy Telegram Sovg‘alar (Gifts) Katalogi</b>\n\n" +
                "Do‘stingizga yoki o‘zingizga rasmiy Telegram sovg‘alarini so‘m evaziga xarid qiling!\n" +
                "To‘lov qilingandan so‘ng <b>Userbot</b> sovg‘ani darhol profilingizga yetkazadi.\n\n" +
                "Kerakli sovg‘ani tanlang 👇";
    }

    @Override
    protected InlineKeyboardMarkup getKeyboard() {
        List<InlineKeyboardRow> keyboard = new ArrayList<>();
        List<GiftService.GiftItem> gifts = "unique".equalsIgnoreCase(category)
                ? giftService.getUniqueGifts()
                : giftService.getUnlimitedGifts();

        for (GiftService.GiftItem item : gifts) {
            int price = priceService.getStarsPrice(item.getStars());
            String priceFormatted = String.format("%,d", price).replace(',', ' ');

            InlineKeyboardRow row = new InlineKeyboardRow();
            row.add(StyledInlineButton.styledBuilder()
                    .text(item.getEmoji() + " " + item.getName() + " (" + item.getStars() + "⭐️) — " + priceFormatted + " so‘m")
                    .callbackData("buy_gift_" + item.getId())
                    .style("primary")
                    .build());
            keyboard.add(row);
        }

        InlineKeyboardRow backRow = new InlineKeyboardRow();
        backRow.add(StyledInlineButton.styledBuilder()
                .text("Orqaga")
                .callbackData("back")
                .style("primary")
                .iconCustomEmojiId("5258236805890710909")
                .build());
        keyboard.add(backRow);

        return InlineKeyboardMarkup.builder().keyboard(keyboard).build();
    }
}

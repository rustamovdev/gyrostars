package ru.lewis.leykabot.model.screen.ui.impl.star;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import ru.lewis.leykabot.configuration.DevModeConfig;
import ru.lewis.leykabot.configuration.loc.ButtonsLocConfig;
import ru.lewis.leykabot.configuration.loc.ClientMessageConfig;
import ru.lewis.leykabot.configuration.loc.ErrorMessageConfig;
import ru.lewis.leykabot.model.screen.ui.AbstractScreen;
import ru.lewis.leykabot.model.screen.ui.ScreenFactory;
import ru.lewis.leykabot.model.screen.ui.ScreenManager;
import ru.lewis.leykabot.service.PriceService;
import ru.lewis.leykabot.service.TelegramService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class StarBuyScreen extends AbstractScreen {
    private final ButtonsLocConfig buttonsLocConfig;
    private final ClientMessageConfig clientMessageConfig;
    private final ErrorMessageConfig errorMessageConfig;
    private final TelegramService telegramService;
    private final DevModeConfig devModeConfig;
    private final PriceService priceService;
    private final ScreenManager screenManager;
    private final ScreenFactory screenFactory;

    private boolean isExpectationMessage;

    public StarBuyScreen(Long chatId, Long userId,
                         ButtonsLocConfig buttonsLocConfig,
                         ClientMessageConfig clientMessageConfig,
                         ErrorMessageConfig errorMessageConfig,
                         TelegramService telegramService,
                         DevModeConfig devModeConfig,
                         PriceService priceService,
                         ScreenManager screenManager,
                         ScreenFactory screenFactory) {
        super(chatId, userId);
        this.buttonsLocConfig = buttonsLocConfig;
        this.clientMessageConfig = clientMessageConfig;
        this.errorMessageConfig = errorMessageConfig;
        this.telegramService = telegramService;
        this.devModeConfig = devModeConfig;
        this.priceService = priceService;
        this.screenManager = screenManager;
        this.screenFactory = screenFactory;
    }

    @Override
    public void handleCallback(String callback, TelegramClient bot) {
        if ("back".equals(callback)) {
            screenManager.updateScreen(chatId, screenFactory.createStartScreen(chatId, userId));
            return;
        }

        if (devModeConfig.isEnable() && !devModeConfig.getWhitelist().contains(userId)) {
            telegramService.sendMessageAuto(chatId, clientMessageConfig.getDevelopmentMode());
            return;
        }

        if ("custom".equals(callback)) {
            isExpectationMessage = true;
            telegramService.sendMessageAuto(chatId, clientMessageConfig.getStarBuyEnterSum());
            return;
        }

        if (callback.startsWith("stars_")) {
            try {
                int amount = Integer.parseInt(callback.substring(6));
                handleBuy(amount);
            } catch (NumberFormatException ignored) {}
        }
    }

    @Override
    public void handleMessage(String text, TelegramClient bot) {
        if (!isExpectationMessage) return;

        try {
            int number = Integer.parseInt(text.trim().replaceAll("\\s+", ""));

            if (number > 100000) {
                telegramService.sendMessageAuto(chatId, errorMessageConfig.getStars().getMaxValue());
                return;
            } else if (number < 50) {
                telegramService.sendMessageAuto(chatId, errorMessageConfig.getStars().getMinValue());
                return;
            }
            handleBuy(number);
            isExpectationMessage = false;
        } catch (NumberFormatException exception) {
            telegramService.sendMessageAuto(chatId, errorMessageConfig.getNumberFormat());
        }
    }

    private void handleBuy(int stars) {
        int rubles = priceService.getStarsPrice(stars);
        screenManager.updateScreen(chatId, screenFactory.createSelectUserForBuyStarsScreen(chatId, userId, stars, rubles));
    }

    @Override
    public String getText() {
        return clientMessageConfig.getBuyStarsCommand();
    }

    @Override
    protected InlineKeyboardMarkup getKeyboard() {
        List<InlineKeyboardRow> keyboard = new ArrayList<>();
        Map<Integer, Integer> starPrices = priceService.getAllStarPrices();

        InlineKeyboardRow currentRow = new InlineKeyboardRow();
        int count = 0;

        for (Map.Entry<Integer, Integer> entry : starPrices.entrySet()) {
            int amount = entry.getKey();
            int price = entry.getValue();
            String priceStr = String.format("%,d", price).replace(',', ' ');

            ru.lewis.leykabot.model.button.StyledInlineButton button = ru.lewis.leykabot.model.button.StyledInlineButton.styledBuilder()
                    .text(amount + " Stars — " + priceStr + " so‘m")
                    .callbackData("stars_" + amount)
                    .style("primary")
                    .iconCustomEmojiId("5985826831591281620")
                    .build();

            currentRow.add(button);
            count++;

            // 1 button per line for perfect readability on mobile screens
            keyboard.add(currentRow);
            currentRow = new InlineKeyboardRow();
        }

        InlineKeyboardRow customRow = new InlineKeyboardRow();
        customRow.add(ru.lewis.leykabot.model.button.StyledInlineButton.styledBuilder()
                .text("Boshqa miqdor kiritish")
                .callbackData("custom")
                .style("primary")
                .iconCustomEmojiId("5470060791883374114")
                .build());
        keyboard.add(customRow);

        InlineKeyboardRow backRow = new InlineKeyboardRow();
        backRow.add(ru.lewis.leykabot.model.button.StyledInlineButton.styledBuilder()
                .text("Orqaga")
                .callbackData("back")
                .iconCustomEmojiId("5258236805890710909")
                .build());
        keyboard.add(backRow);

        return InlineKeyboardMarkup.builder()
                .keyboard(keyboard)
                .build();
    }
}

package ru.lewis.leykabot.model.screen.ui.impl.premium;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import ru.lewis.leykabot.configuration.DevModeConfig;
import ru.lewis.leykabot.configuration.loc.ButtonsLocConfig;
import ru.lewis.leykabot.configuration.loc.ClientMessageConfig;
import ru.lewis.leykabot.model.button.StyledInlineButton;
import ru.lewis.leykabot.model.screen.ui.AbstractScreen;
import ru.lewis.leykabot.model.screen.ui.ScreenFactory;
import ru.lewis.leykabot.model.screen.ui.ScreenManager;
import ru.lewis.leykabot.service.PriceService;
import ru.lewis.leykabot.service.TelegramService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PremiumBuyScreen extends AbstractScreen {

    private final ButtonsLocConfig    buttonsLocConfig;
    private final ClientMessageConfig clientMessageConfig;
    private final TelegramService     telegramService;
    private final DevModeConfig       devModeConfig;
    private final PriceService        priceService;
    private final ScreenManager       screenManager;
    private final ScreenFactory       screenFactory;

    public PremiumBuyScreen(Long chatId, Long userId,
                            ButtonsLocConfig buttonsLocConfig,
                            ClientMessageConfig clientMessageConfig,
                            TelegramService telegramService,
                            DevModeConfig devModeConfig,
                            PriceService priceService,
                            ScreenManager screenManager,
                            ScreenFactory screenFactory) {
        super(chatId, userId);
        this.buttonsLocConfig = buttonsLocConfig;
        this.clientMessageConfig = clientMessageConfig;
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

        if (callback.startsWith("prem_")) {
            try {
                int months = Integer.parseInt(callback.substring(5));
                int rubles = priceService.getPremiumPrice(months);
                screenManager.updateScreen(chatId,
                        screenFactory.createSelectUserForBuyPremiumScreen(chatId, userId, months, rubles));
            } catch (NumberFormatException ignored) {}
        }
    }

    @Override
    public String getText() {
        return clientMessageConfig.getBuyPremiumCommand();
    }

    @Override
    protected InlineKeyboardMarkup getKeyboard() {
        List<InlineKeyboardRow> keyboard = new ArrayList<>();

        // 1 oylik Telegram Premium -> Adminga (@BLACK_mladshiy) to'g'ridan-to'g'ri tayyor shablon bilan yo'naltiriladi
        InlineKeyboardRow row1m = new InlineKeyboardRow();
        row1m.add(StyledInlineButton.styledBuilder()
                .text("1 oylik Telegram Premium")
                .url("https://t.me/BLACK_mladshiy?text=Men%201%20oylik%20Telegram%20Premium%20sotib%20olmoqchiman")
                .style("primary")
                .iconCustomEmojiId("5938420017665152105")
                .build());
        keyboard.add(row1m);

        Map<Integer, Integer> premiumPrices = priceService.getAllPremiumPrices();

        for (Map.Entry<Integer, Integer> entry : premiumPrices.entrySet()) {
            int months = entry.getKey();
            if (months == 1) continue; // 1 oylik yuqorida alohida ulandi
            int price = entry.getValue();
            String priceStr = String.format("%,d", price).replace(',', ' ');

            String label = switch (months) {
                case 3 -> "3 oylik — " + priceStr + " so‘m";
                case 6 -> "6 oylik — " + priceStr + " so‘m";
                case 12 -> "1 yillik — " + priceStr + " so‘m";
                default -> months + " oylik — " + priceStr + " so‘m";
            };

            InlineKeyboardRow row = new InlineKeyboardRow();
            row.add(StyledInlineButton.styledBuilder()
                    .text(label)
                    .callbackData("prem_" + months)
                    .iconCustomEmojiId("5938420017665152105")
                    .build());
            keyboard.add(row);
        }

        InlineKeyboardRow backRow = new InlineKeyboardRow();
        backRow.add(StyledInlineButton.styledBuilder()
                .text("Orqaga")
                .callbackData("back")
                .iconCustomEmojiId("5258236805890710909")
                .build());
        keyboard.add(backRow);

        return InlineKeyboardMarkup.builder().keyboard(keyboard).build();
    }
}
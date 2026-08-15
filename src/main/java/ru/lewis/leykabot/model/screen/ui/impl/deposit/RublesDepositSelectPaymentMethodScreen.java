package ru.lewis.leykabot.model.screen.ui.impl.deposit;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import ru.lewis.leykabot.configuration.loc.ButtonsLocConfig;
import ru.lewis.leykabot.configuration.loc.ClientMessageConfig;
import ru.lewis.leykabot.model.database.entity.PaymentCard;
import ru.lewis.leykabot.model.screen.ui.AbstractScreen;
import ru.lewis.leykabot.model.screen.ui.ScreenFactory;
import ru.lewis.leykabot.model.screen.ui.ScreenManager;
import ru.lewis.leykabot.service.PaymentCardService;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class RublesDepositSelectPaymentMethodScreen extends AbstractScreen {
    private final int rubles;
    private final ButtonsLocConfig buttonsLocConfig;
    private final ClientMessageConfig clientMessageConfig;
    private final PaymentCardService paymentCardService;
    private final ScreenManager screenManager;
    private final ScreenFactory screenFactory;

    public RublesDepositSelectPaymentMethodScreen(Long chatId, Long userId, int rubles,
                                                  ButtonsLocConfig buttonsLocConfig,
                                                  ClientMessageConfig clientMessageConfig,
                                                  PaymentCardService paymentCardService,
                                                  ScreenManager screenManager,
                                                  ScreenFactory screenFactory) {
        super(chatId, userId);
        this.rubles = rubles;
        this.buttonsLocConfig = buttonsLocConfig;
        this.clientMessageConfig = clientMessageConfig;
        this.paymentCardService = paymentCardService;
        this.screenManager = screenManager;
        this.screenFactory = screenFactory;
    }

    @Override
    public void handleCallback(String callback, TelegramClient bot) {
        if ("back".equals(callback)) {
            screenManager.updateScreen(chatId, screenFactory.createDepositRublesScreen(chatId, userId));
            return;
        }

        if (callback.startsWith("card_")) {
            try {
                long cardId = Long.parseLong(callback.substring(5));
                Optional<PaymentCard> cardOpt = paymentCardService.getAllCards().stream()
                        .filter(c -> c.getId().equals(cardId))
                        .findFirst();

                if (cardOpt.isPresent()) {
                    screenManager.updateScreen(chatId, screenFactory.createRublesDepositOrderScreen(chatId, userId, rubles, cardOpt.get()));
                    return;
                }
            } catch (NumberFormatException ignored) {}
        }
    }

    @Override
    public String getText() {
        String formattedSum = String.format("%,d", rubles).replace(',', ' ');
        return "<tg-emoji emoji-id=\"5436203328465838905\">💳</tg-emoji> <b>To‘lov usulini tanlang:</b>\n\n" +
                "Tanlangan summa: <b>" + formattedSum + " so‘m</b>\n\n" +
                "Quyidagi to‘lov usullaridan birini tanlang <tg-emoji emoji-id=\"5436307657516426102\">👇</tg-emoji>";
    }

    @Override
    protected InlineKeyboardMarkup getKeyboard() {
        List<InlineKeyboardRow> keyboard = new ArrayList<>();
        List<PaymentCard> cards = paymentCardService.getActiveCards();

        for (PaymentCard card : cards) {
            InlineKeyboardRow row = new InlineKeyboardRow();
            String btnText = card.getMethodName() + " (" + card.getHolderName() + ")";
            row.add(ru.lewis.leykabot.model.button.StyledInlineButton.styledBuilder()
                    .text(btnText)
                    .callbackData("card_" + card.getId())
                    .style("success")
                    .build());
            keyboard.add(row);
        }

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

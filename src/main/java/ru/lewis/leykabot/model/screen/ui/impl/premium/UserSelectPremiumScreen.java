package ru.lewis.leykabot.model.screen.ui.impl.premium;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import ru.lewis.leykabot.configuration.loc.ButtonsLocConfig;
import ru.lewis.leykabot.configuration.loc.ClientMessageConfig;
import ru.lewis.leykabot.configuration.loc.ErrorMessageConfig;
import ru.lewis.leykabot.model.screen.ui.AbstractScreen;
import ru.lewis.leykabot.model.screen.ui.ScreenFactory;
import ru.lewis.leykabot.model.screen.ui.ScreenManager;
import ru.lewis.leykabot.service.*;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

public class UserSelectPremiumScreen extends AbstractScreen {
    private final int months;
    private final int rubles;

    private final ClientMessageConfig    clientMessageConfig;
    private final ButtonsLocConfig       buttonsLocConfig;
    private final ErrorMessageConfig     errorMessageConfig;
    private final TelegramService        telegramService;
    private final FragmentPremiumService fragmentPremiumService;
    private final PremiumTransactionService premiumTransactionService;
    private final UserService            userService;
    private final OrderChannelService    orderChannelService;
    private final ScreenManager          screenManager;
    private final ScreenFactory          screenFactory;

    private String  username = "";
    private boolean isOther  = false;
    private boolean isConfirmation = false;

    public UserSelectPremiumScreen(Long chatId, Long userId,
                                   int months, int rubles,
                                   ClientMessageConfig clientMessageConfig,
                                   ButtonsLocConfig buttonsLocConfig,
                                   ErrorMessageConfig errorMessageConfig,
                                   TelegramService telegramService,
                                   FragmentPremiumService fragmentPremiumService,
                                   PremiumTransactionService premiumTransactionService,
                                   UserService userService,
                                   OrderChannelService orderChannelService,
                                   ScreenManager screenManager,
                                   ScreenFactory screenFactory) {
        super(chatId, userId);
        this.months                   = months;
        this.rubles                   = rubles;
        this.clientMessageConfig      = clientMessageConfig;
        this.buttonsLocConfig         = buttonsLocConfig;
        this.errorMessageConfig       = errorMessageConfig;
        this.telegramService          = telegramService;
        this.fragmentPremiumService   = fragmentPremiumService;
        this.premiumTransactionService = premiumTransactionService;
        this.userService              = userService;
        this.orderChannelService      = orderChannelService;
        this.screenManager            = screenManager;
        this.screenFactory            = screenFactory;
    }

    @Override
    public void handleCallback(String callback, TelegramClient bot) {
        switch (callback) {
            case "yourself" -> {
                String rawUsername = telegramService.getRawUsernameByUserId(userId);
                if (rawUsername == null || rawUsername.isBlank()) {
                    isOther = true;
                    telegramService.sendMessageAuto(chatId, "⚠️ Sizning Telegram profilingizda username o‘rnatilmagan.\n\nIltimos, Premium qabul qiluvchi foydalanuvchi nomini yozib yuboring (@username):");
                    return;
                }
                username = rawUsername;
                isConfirmation = true;
                screenManager.updateScreen(chatId, this);
            }

            case "other" -> {
                isOther = true;
                isConfirmation = false;
                telegramService.sendMessageAuto(chatId, clientMessageConfig.getSelectOther());
            }

            case "confirm_buy" -> executePurchase();

            case "back_select" -> {
                isConfirmation = false;
                isOther = false;
                username = "";
                screenManager.updateScreen(chatId, this);
            }

            case "back" ->
                    screenManager.updateScreen(chatId, screenFactory.createBuyPremiumScreen(chatId, userId));
        }
    }

    private void executePurchase() {
        if (username.isBlank()) {
            telegramService.sendMessageAuto(chatId, errorMessageConfig.getUsernameNotSelected());
            return;
        }

        var balanceUserOptional = userService.getBalance(userId);
        int balance = balanceUserOptional.orElse(0);

        if (balance < rubles) {
            telegramService.sendMessageAuto(chatId, "⚠️ Balansingizda yetarli mablag‘ mavjud emas. Iltimos, hisobingizni to‘ldiring.");
            screenManager.updateScreen(chatId, screenFactory.createDepositRublesScreen(chatId, userId));
            return;
        }

        telegramService.sendMessageAuto(chatId, "⏳ Buyurtmangiz bajarilmoqda, iltimos kuting...");

        fragmentPremiumService.buyPremium(username, months)
                .thenAccept(response -> {
                    if (response != null && response.isOk()) {
                        premiumTransactionService.create(
                                userId,
                                -rubles,
                                months
                        );
                        String formattedRubles = String.format("%,d", rubles).replace(',', ' ');
                        telegramService.sendMessageAuto(chatId,
                                MessageFormat.format(
                                        clientMessageConfig.getThanksForPayment(),
                                        formattedRubles));

                        // Post to order channel
                        if (orderChannelService != null) {
                            String duration = months >= 12 ? (months / 12) + " yillik" : months + " oylik";
                            orderChannelService.sendOrderNotification("💎 Telegram Premium", duration, username, rubles);
                        }

                        screenManager.updateScreen(chatId,
                                screenFactory.createStartScreen(chatId, userId));
                    } else {
                        String errMsg = (response != null && response.getMessage() != null && !response.getMessage().isBlank())
                                ? response.getMessage()
                                : errorMessageConfig.getUsernameNotFound();
                        telegramService.sendMessageAuto(chatId, "❌ Xatolik: " + errMsg);
                    }
                    isOther = false;
                    isConfirmation = false;
                    username = "";
                });
    }

    @Override
    public void handleMessage(String text, TelegramClient bot) {
        if (!isOther) return;
        username = text.startsWith("@") ? text.substring(1).trim() : text.trim();
        isOther  = false;
        isConfirmation = true;
        screenManager.updateScreen(chatId, this);
    }

    @Override
    public String getText() {
        String formattedPrice = String.format("%,d", rubles).replace(',', ' ');

        if (isConfirmation) {
            int userBalance = userService.getBalance(userId).orElse(0);
            String formattedBalance = String.format("%,d", userBalance).replace(',', ' ');

            String durationText = months >= 12 ? (months / 12) + " yillik" : months + " oylik";

            return "<tg-emoji emoji-id=\"5938420017665152105\">💎</tg-emoji> <b>Xaridni tasdiqlash</b>\n\n" +
                    "👤 <b>Qabul qiluvchi:</b> @" + username + "\n" +
                    "<tg-emoji emoji-id=\"5938420017665152105\">💎</tg-emoji> <b>Obuna muddati:</b> " + durationText + " Telegram Premium\n" +
                    "<tg-emoji emoji-id=\"5436107628004549969\">💰</tg-emoji> <b>Narxi:</b> " + formattedPrice + " so‘m\n" +
                    "<tg-emoji emoji-id=\"5436203328465838905\">💳</tg-emoji> <b>Sizning balansingiz:</b> " + formattedBalance + " so‘m\n\n" +
                    "Buyurtmani tasdiqlaysizmi?";
        }

        return MessageFormat.format(
                clientMessageConfig.getSelectUserForBuyPremiumCommand(), months, formattedPrice);
    }

    @Override
    protected InlineKeyboardMarkup getKeyboard() {
        List<InlineKeyboardRow> keyboard = new ArrayList<>();

        if (isConfirmation) {
            InlineKeyboardRow row1 = new InlineKeyboardRow();
            row1.add(ru.lewis.leykabot.model.button.StyledInlineButton.styledBuilder()
                    .text("Ha, tasdiqlayman")
                    .callbackData("confirm_buy")
                    .style("success")
                    .iconCustomEmojiId("5436406725232074977")
                    .build());
            row1.add(ru.lewis.leykabot.model.button.StyledInlineButton.styledBuilder()
                    .text("Bekor qilish")
                    .callbackData("back_select")
                    .style("danger")
                    .build());
            keyboard.add(row1);
            return InlineKeyboardMarkup.builder().keyboard(keyboard).build();
        }

        InlineKeyboardRow row1 = new InlineKeyboardRow();
        row1.add(ru.lewis.leykabot.model.button.StyledInlineButton.styledBuilder()
                .text(buttonsLocConfig.getYourself())
                .callbackData("yourself")
                .style("primary")
                .build());
        row1.add(ru.lewis.leykabot.model.button.StyledInlineButton.styledBuilder()
                .text(buttonsLocConfig.getOther())
                .callbackData("other")
                .style("primary")
                .build());

        InlineKeyboardRow row2 = new InlineKeyboardRow();
        row2.add(ru.lewis.leykabot.model.button.StyledInlineButton.styledBuilder()
                .text("Orqaga")
                .callbackData("back")
                .iconCustomEmojiId("5258236805890710909")
                .build());

        keyboard.add(row1);
        keyboard.add(row2);

        return InlineKeyboardMarkup.builder().keyboard(keyboard).build();
    }
}
package ru.lewis.leykabot.model.screen.ui.impl.pubg;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import ru.lewis.leykabot.model.button.StyledInlineButton;
import ru.lewis.leykabot.model.screen.ui.AbstractScreen;
import ru.lewis.leykabot.model.screen.ui.ScreenFactory;
import ru.lewis.leykabot.model.screen.ui.ScreenManager;
import ru.lewis.leykabot.service.PubgService;
import ru.lewis.leykabot.service.PubgTransactionService;
import ru.lewis.leykabot.service.TelegramService;
import ru.lewis.leykabot.service.UserService;

import java.util.ArrayList;
import java.util.List;

public class PubgBuyScreen extends AbstractScreen {

    private final ScreenManager screenManager;
    private final ScreenFactory screenFactory;
    private final PubgService pubgService;
    private final PubgTransactionService pubgTransactionService;
    private final UserService userService;
    private final TelegramService telegramService;

    private PubgService.PubgOffer selectedOffer = null;
    private String playerId = "";
    private String playerNickname = "";
    private boolean isWaitingPlayerId = false;
    private boolean isConfirmation = false;
    private boolean isProcessing = false;

    public PubgBuyScreen(Long chatId, Long userId,
                         ScreenManager screenManager,
                         ScreenFactory screenFactory,
                         PubgService pubgService,
                         PubgTransactionService pubgTransactionService,
                         UserService userService,
                         TelegramService telegramService) {
        super(chatId, userId);
        this.screenManager = screenManager;
        this.screenFactory = screenFactory;
        this.pubgService = pubgService;
        this.pubgTransactionService = pubgTransactionService;
        this.userService = userService;
        this.telegramService = telegramService;
    }

    @Override
    public void handleCallback(String callback, TelegramClient bot) {
        if (isProcessing) return;

        if ("back".equals(callback)) {
            screenManager.updateScreen(chatId, screenFactory.createStartScreen(chatId, userId));
            return;
        }

        if ("back_select".equals(callback)) {
            this.selectedOffer = null;
            this.playerId = "";
            this.playerNickname = "";
            this.isWaitingPlayerId = false;
            this.isConfirmation = false;
            screenManager.updateScreen(chatId, this);
            return;
        }

        if (callback.startsWith("offer_")) {
            String offerId = callback.substring(6);
            List<PubgService.PubgOffer> offers = pubgService.getOffers();
            for (PubgService.PubgOffer o : offers) {
                if (o.getOfferId().equals(offerId)) {
                    this.selectedOffer = o;
                    this.isWaitingPlayerId = true;
                    telegramService.sendMessageAuto(chatId,
                            "🎮 <b>PUBG Mobile Player ID (UID) ingizni yozib yuboring:</b>\n\n" +
                                    "<i>Masalan: <code>5123456789</code> (O‘yin profilingizdagi raqamli ID)</i>");
                    return;
                }
            }
        }

        if ("confirm_pubg".equals(callback)) {
            executePurchase();
        }
    }

    @Override
    public void handleMessage(String text, TelegramClient bot) {
        if (!isWaitingPlayerId || selectedOffer == null) return;

        String cleanId = text.trim().replaceAll("[^0-9]", "");
        if (cleanId.length() < 5 || cleanId.length() > 20) {
            telegramService.sendMessageAuto(chatId, "❌ <b>Noto‘g‘ri PUBG ID!</b> Iltimos, faqat raqamlardan iborat to‘g‘ri PUBG ID kiriting (masalan: <code>5123456789</code>):");
            return;
        }

        this.playerId = cleanId;
        this.isWaitingPlayerId = false;
        telegramService.sendMessageAuto(chatId, "🔍 <b>O‘yinchi ma’lumotlari tekshirilmoqda...</b>");

        pubgService.checkPlayer(cleanId).thenAccept(info -> {
            if (info != null && info.success()) {
                this.playerNickname = info.nickname();
                this.isConfirmation = true;
                screenManager.updateScreen(chatId, this);
            } else {
                String err = (info != null && info.errorMessage() != null) ? info.errorMessage() : "Bunday PUBG ID topilmadi!";
                telegramService.sendMessageAuto(chatId, "❌ <b>Xatolik:</b> " + err + "\n\nIltimos, PUBG ID ni qaytadan tekshirib yozing:");
                this.isWaitingPlayerId = true;
            }
        });
    }

    private void executePurchase() {
        if (selectedOffer == null || playerId.isBlank() || isProcessing) return;

        int userBalance = userService.getBalance(userId).orElse(0);
        int price = selectedOffer.getPrice();

        if (userBalance < price) {
            telegramService.sendMessageAuto(chatId,
                    "⚠️ <b>Balansingizda yetarli mablag‘ mavjud emas.</b>\n\n" +
                            "Kerakli summa: <b>" + String.format("%,d", price).replace(',', ' ') + " so‘m</b>\n" +
                            "Sizning balansingiz: <b>" + String.format("%,d", userBalance).replace(',', ' ') + " so‘m</b>\n\n" +
                            "Iltimos, avval hisobingizni to‘ldiring.");
            screenManager.updateScreen(chatId, screenFactory.createDepositRublesScreen(chatId, userId));
            return;
        }

        this.isProcessing = true;
        telegramService.sendMessageAuto(chatId, "⏳ <b>PUBG UC hisobingizga yetkazilmoqda, iltimos 10-30 soniya kuting...</b>");

        pubgService.executeOrder("uc", selectedOffer.getOfferId(), playerId)
                .thenAccept(res -> {
                    this.isProcessing = false;
                    if (res != null && res.success()) {
                        pubgTransactionService.create(
                                userId,
                                playerId,
                                playerNickname,
                                selectedOffer.getOfferId(),
                                selectedOffer.getUc(),
                                price,
                                res.orderId(),
                                res.reference(),
                                res.code()
                        );

                        String formattedPrice = String.format("%,d", price).replace(',', ' ');
                        telegramService.sendMessageAuto(chatId,
                                "<tg-emoji emoji-id=\"5436406725232074977\">✅</tg-emoji> <b>Muvaffaqiyatli yetkazildi!</b>\n\n" +
                                        "🎮 <b>O‘yinchi:</b> <b>" + playerNickname + "</b> (<code>" + playerId + "</code>)\n" +
                                        "📦 <b>Yetkazildi:</b> <b>" + selectedOffer.getUc() + " UC</b>\n" +
                                        "💰 <b>Yechilgan summa:</b> <b>" + formattedPrice + " so‘m</b>\n\n" +
                                        "<i>O‘yiningizga kirib hisobingizni tekshirishingiz mumkin. Xaridingiz uchun rahmat!</i>");

                        screenManager.updateScreen(chatId, screenFactory.createStartScreen(chatId, userId));
                    } else {
                        String errMsg = (res != null && res.errorMessage() != null) ? res.errorMessage() : "Yetkazib beruvchi serverida vaqtinchalik xatolik.";
                        telegramService.sendMessageAuto(chatId, "❌ <b>Xarid amalga oshmadi:</b> " + errMsg + "\n\nHisobingizdan pul yechilmadi.");
                        this.isConfirmation = false;
                        screenManager.updateScreen(chatId, this);
                    }
                });
    }

    @Override
    public String getText() {
        if (isConfirmation && selectedOffer != null) {
            int userBalance = userService.getBalance(userId).orElse(0);
            String formattedBalance = String.format("%,d", userBalance).replace(',', ' ');
            String formattedPrice = String.format("%,d", selectedOffer.getPrice()).replace(',', ' ');

            return "<tg-emoji emoji-id=\"5204252919565657978\">🎮</tg-emoji> <b>PUBG UC Xaridini Tasdiqlash</b>\n\n" +
                    "👤 <b>PUBG O‘yinchi:</b> <b>" + playerNickname + "</b>\n" +
                    "🆔 <b>Player ID:</b> <code>" + playerId + "</code>\n" +
                    "<tg-emoji emoji-id=\"5204252919565657978\">🎮</tg-emoji> <b>Paket:</b> <b>" + selectedOffer.getUc() + " UC</b>\n" +
                    "<tg-emoji emoji-id=\"5273922812434729713\">💵</tg-emoji> <b>Narxi:</b> <b>" + formattedPrice + " so‘m</b>\n" +
                    "<tg-emoji emoji-id=\"5436203328465838905\">💳</tg-emoji> <b>Sizning balansingiz:</b> <b>" + formattedBalance + " so‘m</b>\n\n" +
                    "<i>Xaridni tasdiqlaysizmi?</i>";
        }

        return "<tg-emoji emoji-id=\"5204252919565657978\">🎮</tg-emoji> <b>PUBG UC DONAT QILISH</b>\n\n" +
                "Kerakli UC paketini tanlang:\n" +
                "<i>(Barcha to‘lovlar to‘g‘ridan-to‘g‘ri o‘yin profilingizga avtomatik yetkazib beriladi)</i>";
    }

    @Override
    protected InlineKeyboardMarkup getKeyboard() {
        List<InlineKeyboardRow> keyboard = new ArrayList<>();

        if (isConfirmation) {
            InlineKeyboardRow row1 = new InlineKeyboardRow();
            row1.add(StyledInlineButton.styledBuilder()
                    .text("Xaridni tasdiqlash")
                    .callbackData("confirm_pubg")
                    .style("success")
                    .iconCustomEmojiId("5436406725232074977")
                    .build());

            InlineKeyboardRow row2 = new InlineKeyboardRow();
            row2.add(StyledInlineButton.styledBuilder()
                    .text("Orqaga")
                    .callbackData("back_select")
                    .iconCustomEmojiId("5258236805890710909")
                    .build());

            keyboard.add(row1);
            keyboard.add(row2);
            return InlineKeyboardMarkup.builder().keyboard(keyboard).build();
        }

        List<PubgService.PubgOffer> offers = pubgService.getOffers();
        for (int i = 0; i < offers.size(); i += 2) {
            InlineKeyboardRow row = new InlineKeyboardRow();
            PubgService.PubgOffer o1 = offers.get(i);
            String label1 = o1.getUc() + " UC — " + String.format("%,d", o1.getPrice()).replace(',', ' ') + " so‘m";
            row.add(StyledInlineButton.styledBuilder()
                    .text(label1)
                    .callbackData("offer_" + o1.getOfferId())
                    .style("primary")
                    .iconCustomEmojiId("5273922812434729713")
                    .build());

            if (i + 1 < offers.size()) {
                PubgService.PubgOffer o2 = offers.get(i + 1);
                String label2 = o2.getUc() + " UC — " + String.format("%,d", o2.getPrice()).replace(',', ' ') + " so‘m";
                row.add(StyledInlineButton.styledBuilder()
                        .text(label2)
                        .callbackData("offer_" + o2.getOfferId())
                        .style("primary")
                        .iconCustomEmojiId("5273922812434729713")
                        .build());
            }
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

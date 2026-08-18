package ru.lewis.leykabot.model.screen.ui.impl.deposit;

import org.telegram.telegrambots.meta.api.objects.Document;
import org.telegram.telegrambots.meta.api.objects.photo.PhotoSize;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import ru.lewis.leykabot.configuration.loc.ButtonsLocConfig;
import ru.lewis.leykabot.model.button.StyledInlineButton;
import ru.lewis.leykabot.model.database.entity.DepositOrder;
import ru.lewis.leykabot.model.database.entity.PaymentCard;
import ru.lewis.leykabot.model.screen.ui.AbstractScreen;
import ru.lewis.leykabot.model.screen.ui.ScreenFactory;
import ru.lewis.leykabot.model.screen.ui.ScreenManager;
import ru.lewis.leykabot.service.AutoPaymentService;
import ru.lewis.leykabot.service.PaymentCardService;
import ru.lewis.leykabot.service.TelegramService;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class RublesDepositOrderScreen extends AbstractScreen {

    private final int rubles;
    private final PaymentCard card;
    private final PaymentCardService paymentCardService;
    private final AutoPaymentService autoPaymentService;
    private final TelegramService telegramService;
    private final ButtonsLocConfig buttonsLocConfig;
    private final ScreenManager screenManager;
    private final ScreenFactory screenFactory;

    private DepositOrder depositOrder;

    public RublesDepositOrderScreen(Long chatId, Long userId, int rubles,
                                   PaymentCard card,
                                   PaymentCardService paymentCardService,
                                   AutoPaymentService autoPaymentService,
                                   TelegramService telegramService,
                                   ButtonsLocConfig buttonsLocConfig,
                                   ScreenManager screenManager,
                                   ScreenFactory screenFactory) {
        super(chatId, userId);
        this.rubles = rubles;
        this.card = card;
        this.paymentCardService = paymentCardService;
        this.autoPaymentService = autoPaymentService;
        this.telegramService = telegramService;
        this.buttonsLocConfig = buttonsLocConfig;
        this.screenManager = screenManager;
        this.screenFactory = screenFactory;

        if (autoPaymentService != null) {
            this.depositOrder = autoPaymentService.createDepositOrder(userId, chatId, rubles, card);
        }
    }

    @Override
    public void handleCallback(String callback, TelegramClient bot) {
        switch (callback) {
            case "check_payment" -> {
                if (depositOrder == null || autoPaymentService == null) {
                    telegramService.sendMessageAuto(chatId, "⏳ To‘lov tekshirilmoqda...");
                    return;
                }

                Optional<DepositOrder> currentOpt = autoPaymentService.getOrder(depositOrder.getId());
                if (currentOpt.isEmpty()) {
                    telegramService.sendMessageAuto(chatId, "❌ Buyurtma topilmadi.");
                    screenManager.updateScreen(chatId, screenFactory.createStartScreen(chatId, userId));
                    return;
                }

                DepositOrder current = currentOpt.get();
                if ("PAID_AUTO".equals(current.getStatus()) || "PAID_MANUAL".equals(current.getStatus())) {
                    telegramService.sendMessageAuto(chatId,
                            "<tg-emoji emoji-id=\"5436406725232074977\">✅</tg-emoji> <b>To‘lovingiz allaqachon qabul qilingan va balansingizga qo‘shilgan!</b>");
                    screenManager.updateScreen(chatId, screenFactory.createStartScreen(chatId, userId));
                } else if ("EXPIRED".equals(current.getStatus()) || (current.getExpiresAt() != null && current.getExpiresAt().isBefore(LocalDateTime.now()))) {
                    telegramService.sendMessageAuto(chatId,
                            "⏰ <b>Ushbu to‘lov buyurtmasining 10 daqiqalik vaqti tugagan.</b>\n\nIltimos, yangi to‘lov buyurtmasi yarating.");
                    screenManager.updateScreen(chatId, screenFactory.createStartScreen(chatId, userId));
                } else if ("CANCELLED".equals(current.getStatus())) {
                    telegramService.sendMessageAuto(chatId, "🚫 <b>Ushbu buyurtma bekor qilingan.</b>");
                    screenManager.updateScreen(chatId, screenFactory.createStartScreen(chatId, userId));
                } else {
                    int exactSum = current.getExactAmount();
                    String formattedExactSum = String.format("%,d", exactSum).replace(',', ' ');
                    telegramService.sendMessageAuto(chatId,
                            "⏳ <b>To‘lov hali kelib tushmadi.</b>\n\n" +
                                    "Iltimos, kartaga aynan <code>" + formattedExactSum + "</code> so‘m o‘tkazganingizga ishonch hosil qiling va 1-2 daqiqa kuting.\n\n" +
                                    "<i>Pul kartaga tushishi bilan hisobingiz server tomonidan avtomatik to‘ldiriladi!</i>");
                }
            }
            case "cancel_order" -> {
                if (depositOrder != null && autoPaymentService != null) {
                    autoPaymentService.cancelOrder(depositOrder.getId());
                }
                telegramService.sendMessageAuto(chatId, "🚫 <b>To‘lov buyurtmasi bekor qilindi.</b>");
                screenManager.updateScreen(chatId, screenFactory.createStartScreen(chatId, userId));
            }
            case "back" -> {
                screenManager.updateScreen(chatId, screenFactory.createRublesDepositSelectPaymentMethodScreen(chatId, userId, rubles));
            }
        }
    }

    @Override
    public void handlePhoto(List<PhotoSize> photos, TelegramClient bot) {
        if (photos == null || photos.isEmpty()) return;

        PhotoSize largestPhoto = photos.get(photos.size() - 1);
        String fileId = largestPhoto.getFileId();
        processReceipt(fileId);
    }

    @Override
    public void handleDocument(Document document, TelegramClient bot) {
        if (document == null) return;
        processReceipt(document.getFileId());
    }

    private void processReceipt(String fileId) {
        String cardInfo = card.getMethodName() + " (" + card.getCardNumber() + " - " + card.getHolderName() + ")";
        int amountToCredit = (depositOrder != null) ? depositOrder.getBaseAmount() : rubles;
        paymentCardService.createDepositReceipt(userId, chatId, amountToCredit, cardInfo, fileId);

        String formattedSum = String.format("%,d", amountToCredit).replace(',', ' ');
        telegramService.sendMessageAuto(chatId,
                "✅ <b>To‘lov chekingiz qabul qilindi va adminga yuborildi!</b>\n\n" +
                        "<tg-emoji emoji-id=\"5436107628004549969\">💰</tg-emoji> <b>Summa:</b> <b>" + formattedSum + " so‘m</b>\n" +
                        "Adminlar to‘lovni tasdiqlashi bilan hisobingizga mablag‘ avtomatik qo‘shiladi va sizga xabar beriladi.");

        screenManager.updateScreen(chatId, screenFactory.createStartScreen(chatId, userId));
    }

    @Override
    public String getText() {
        int exactSum = (depositOrder != null) ? depositOrder.getExactAmount() : rubles;
        String formattedExactSum = String.format("%,d", exactSum).replace(',', ' ');
        String cardNum = (card != null && card.getCardNumber() != null) ? card.getCardNumber() : "";
        String orderCode = (depositOrder != null && depositOrder.getOrderCode() != null)
                ? depositOrder.getOrderCode()
                : "ord" + ((depositOrder != null) ? depositOrder.getId() : System.currentTimeMillis() % 1000000);

        String createdTimeStr = (depositOrder != null && depositOrder.getCreatedAt() != null)
                ? depositOrder.getCreatedAt().format(DateTimeFormatter.ofPattern("HH:mm"))
                : LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));

        String expireTimeStr = (depositOrder != null && depositOrder.getExpiresAt() != null)
                ? depositOrder.getExpiresAt().format(DateTimeFormatter.ofPattern("HH:mm"))
                : LocalDateTime.now().plusMinutes(10).format(DateTimeFormatter.ofPattern("HH:mm"));

        return "<b>To‘lov buyurtmasi yaratildi!</b>\n\n" +
                "<b>Buyurtma:</b> <code>" + orderCode + "</code>\n" +
                "<b>To‘lash:</b> <code>" + formattedExactSum + " so‘m</code>\n" +
                "<b>Karta:</b> <code>" + cardNum + "</code>\n\n" +
                "<tg-emoji emoji-id=\"5420323339723881652\">⚠️</tg-emoji> <b>Aynan " + formattedExactSum + " so‘m o‘tkazing!</b> Kam yoki ko‘p pul tashasangiz pulingiz balance tushmay qoladi va bu holatda admin javobgar emas!\n\n" +
                "<tg-emoji emoji-id=\"5258258882022612173\">🕒</tg-emoji> <b>Yaratildi:</b> " + createdTimeStr + "  <tg-emoji emoji-id=\"5778605968208170641\">⏳</tg-emoji> <b>Tugaydi:</b> " + expireTimeStr;
    }

    @Override
    protected InlineKeyboardMarkup getKeyboard() {
        List<InlineKeyboardRow> keyboard = new ArrayList<>();

        InlineKeyboardRow row1 = new InlineKeyboardRow();
        row1.add(StyledInlineButton.styledBuilder()
                .text("To‘lovni tekshirish")
                .callbackData("check_payment")
                .style("success")
                .iconCustomEmojiId("5436406725232074977")
                .build());

        InlineKeyboardRow row2 = new InlineKeyboardRow();
        row2.add(StyledInlineButton.styledBuilder()
                .text("Bekor qilish")
                .callbackData("cancel_order")
                .style("danger")
                .build());
        row2.add(StyledInlineButton.styledBuilder()
                .text("Orqaga")
                .callbackData("back")
                .style("primary")
                .iconCustomEmojiId("5258236805890710909")
                .build());

        keyboard.add(row1);
        keyboard.add(row2);

        return InlineKeyboardMarkup.builder().keyboard(keyboard).build();
    }
}

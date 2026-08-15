package ru.lewis.leykabot.model.screen.ui.impl.admin;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import ru.lewis.leykabot.model.database.entity.PaymentCard;
import ru.lewis.leykabot.model.screen.ui.AbstractScreen;
import ru.lewis.leykabot.model.screen.ui.ScreenFactory;
import ru.lewis.leykabot.model.screen.ui.ScreenManager;
import ru.lewis.leykabot.service.AdminService;
import ru.lewis.leykabot.service.PaymentCardService;
import ru.lewis.leykabot.service.TelegramService;

import java.util.ArrayList;
import java.util.List;

public class AdminCardScreen extends AbstractScreen {

    private final ScreenManager screenManager;
    private final ScreenFactory screenFactory;
    private final AdminService adminService;
    private final PaymentCardService paymentCardService;
    private final TelegramService telegramService;

    // Step-by-step state
    private int addCardStep = 0; // 0 = idle, 1 = waiting card num, 2 = waiting holder name, 3 = waiting method
    private String tempCardNumber = null;
    private String tempHolderName = null;

    private boolean isWaitingDeleteCard = false;

    public AdminCardScreen(Long chatId, Long userId,
                           ScreenManager screenManager,
                           ScreenFactory screenFactory,
                           AdminService adminService,
                           PaymentCardService paymentCardService,
                           TelegramService telegramService) {
        super(chatId, userId);
        this.screenManager = screenManager;
        this.screenFactory = screenFactory;
        this.adminService = adminService;
        this.paymentCardService = paymentCardService;
        this.telegramService = telegramService;
    }

    @Override
    public void handleCallback(String callback, TelegramClient bot) {
        if (!adminService.isAdmin(userId)) return;

        switch (callback) {
            case "add_card" -> {
                addCardStep = 1;
                tempCardNumber = null;
                tempHolderName = null;
                isWaitingDeleteCard = false;
                telegramService.sendMessageAuto(chatId, "💳 <b>1-qadam:</b> Karta raqamini yozib yuboring:\n\n<i>Masalan: 8600 1234 5678 9012</i>");
            }
            case "del_card" -> {
                isWaitingDeleteCard = true;
                addCardStep = 0;
                telegramService.sendMessageAuto(chatId, "O‘chirmoqchi bo‘lgan kartangizning <b>ID</b> raqamini yozib yuboring (masalan: 1):");
            }
            case "back_admin" -> {
                resetState();
                screenManager.updateScreen(chatId, screenFactory.createAdminMainScreen(chatId, userId));
            }
            default -> {
                if (callback.startsWith("method_")) {
                    String method = switch (callback) {
                        case "method_uzcard" -> "💳 Uzcard";
                        case "method_humo" -> "💳 Humo";
                        case "method_both" -> "💳 Uzcard / Humo";
                        case "method_anor" -> "🏦 Anorbank";
                        case "method_click" -> "🏦 Click / Payme";
                        default -> "💳 Bank kartasi";
                    };
                    saveNewCard(method);
                    return;
                }

                if (callback.startsWith("toggle_")) {
                    try {
                        long cardId = Long.parseLong(callback.substring(7));
                        paymentCardService.toggleCard(cardId);
                        screenManager.updateScreen(chatId, this);
                    } catch (NumberFormatException ignored) {}
                }
            }
        }
    }

    @Override
    public void handleMessage(String text, TelegramClient bot) {
        if (!adminService.isAdmin(userId)) return;

        String input = text.trim();

        // Check if admin sent all 3 lines at once
        String[] lines = input.split("\\r?\\n");
        if (addCardStep == 1 && lines.length >= 3) {
            String cardNum = lines[0].trim();
            String holder = lines[1].trim();
            String method = lines[2].trim();
            saveCardDirectly(cardNum, holder, method);
            return;
        }

        // Step 1: Card Number
        if (addCardStep == 1) {
            tempCardNumber = input;
            addCardStep = 2;
            telegramService.sendMessageAuto(chatId, "👤 <b>2-qadam:</b> Karta egasining ism-familiyasini yozib yuboring:\n\n<i>Masalan: Samirbek R.</i>");
            return;
        }

        // Step 2: Holder Name
        if (addCardStep == 2) {
            tempHolderName = input;
            addCardStep = 3;

            List<InlineKeyboardRow> keyboard = new ArrayList<>();
            InlineKeyboardRow r1 = new InlineKeyboardRow();
            r1.add(InlineKeyboardButton.builder().text("💳 Uzcard").callbackData("method_uzcard").build());
            r1.add(InlineKeyboardButton.builder().text("💳 Humo").callbackData("method_humo").build());

            InlineKeyboardRow r2 = new InlineKeyboardRow();
            r2.add(InlineKeyboardButton.builder().text("💳 Uzcard / Humo").callbackData("method_both").build());
            r2.add(InlineKeyboardButton.builder().text("🏦 Anorbank").callbackData("method_anor").build());

            InlineKeyboardRow r3 = new InlineKeyboardRow();
            r3.add(InlineKeyboardButton.builder().text("🏦 Click / Payme").callbackData("method_click").build());

            keyboard.add(r1);
            keyboard.add(r2);
            keyboard.add(r3);

            InlineKeyboardMarkup markup = InlineKeyboardMarkup.builder().keyboard(keyboard).build();

            telegramService.sendMessageAuto(chatId, "🏦 <b>3-qadam:</b> To‘lov tizimi nomini yozing yoki quyidagi tugmalardan birini tanlang:", markup);
            return;
        }

        // Step 3: Method Name typed manually
        if (addCardStep == 3) {
            saveNewCard(input);
            return;
        }

        // Delete Card by ID
        if (isWaitingDeleteCard) {
            try {
                long cardId = Long.parseLong(input);
                if (paymentCardService.deleteCard(cardId)) {
                    telegramService.sendMessageAuto(chatId, "✅ Karta (ID: " + cardId + ") muvaffaqiyatli o‘chirildi!");
                } else {
                    telegramService.sendMessageAuto(chatId, "❌ Bunday ID ga ega karta topilmadi.");
                }
            } catch (NumberFormatException e) {
                telegramService.sendMessageAuto(chatId, "❌ Iltimos, faqat karta ID raqamini kiriting!");
            }
            resetState();
            screenManager.updateScreen(chatId, this);
        }
    }

    private void saveNewCard(String methodName) {
        if (tempCardNumber != null && tempHolderName != null) {
            saveCardDirectly(tempCardNumber, tempHolderName, methodName);
        } else {
            telegramService.sendMessageAuto(chatId, "⚠️ Ma’lumotlar yetarli emas. Iltimos, qaytadan '➕ Karta qo‘shish' tugmasini bosing.");
            resetState();
            screenManager.updateScreen(chatId, this);
        }
    }

    private void saveCardDirectly(String cardNum, String holder, String method) {
        PaymentCard card = paymentCardService.addCard(cardNum, holder, method);
        telegramService.sendMessageAuto(chatId, "✅ <b>Yangi to‘lov kartasi muvaffaqiyatli qo‘shildi!</b>\n\n" +
                "💳 <b>Karta:</b> <code>" + card.getCardNumber() + "</code>\n" +
                "👤 <b>Egasi:</b> <b>" + card.getHolderName() + "</b>\n" +
                "🏦 <b>To‘lov usuli:</b> <b>" + card.getMethodName() + "</b>");
        resetState();
        screenManager.updateScreen(chatId, this);
    }

    private void resetState() {
        addCardStep = 0;
        tempCardNumber = null;
        tempHolderName = null;
        isWaitingDeleteCard = false;
    }

    @Override
    public String getText() {
        List<PaymentCard> cards = paymentCardService.getAllCards();

        StringBuilder sb = new StringBuilder();
        sb.append("💳 <b>To‘lov Kartalari Boshqaruvi</b>\n\n");

        if (cards.isEmpty()) {
            sb.append("<i>Hozircha hech qanday karta qo‘shilmagan.</i>\n");
        } else {
            sb.append("📋 <b>Mavjud kartalar ro‘yxati:</b>\n\n");
            for (PaymentCard c : cards) {
                sb.append("🆔 <b>ID:</b> ").append(c.getId()).append("\n")
                        .append("🏦 <b>Usul:</b> ").append(c.getMethodName()).append("\n")
                        .append("💳 <b>Raqam:</b> <code>").append(c.getCardNumber()).append("</code>\n")
                        .append("👤 <b>Egasi:</b> ").append(c.getHolderName()).append("\n")
                        .append("🟢 <b>Holat:</b> ").append(c.isActive() ? "Faol (Yoqilgan)" : "O‘chirilgan").append("\n\n");
            }
        }

        sb.append("Kerakli amalni tanlang 👇");
        return sb.toString();
    }

    @Override
    protected InlineKeyboardMarkup getKeyboard() {
        List<InlineKeyboardRow> keyboard = new ArrayList<>();
        List<PaymentCard> cards = paymentCardService.getAllCards();

        InlineKeyboardRow row1 = new InlineKeyboardRow();
        row1.add(InlineKeyboardButton.builder().text("➕ Karta qo‘shish").callbackData("add_card").build());
        row1.add(InlineKeyboardButton.builder().text("🗑 Kartani o‘chirish").callbackData("del_card").build());
        keyboard.add(row1);

        for (PaymentCard c : cards) {
            InlineKeyboardRow toggleRow = new InlineKeyboardRow();
            String status = c.isActive() ? "🔴 O‘chirish #" + c.getId() : "🟢 Yoqish #" + c.getId();
            toggleRow.add(InlineKeyboardButton.builder().text(status).callbackData("toggle_" + c.getId()).build());
            keyboard.add(toggleRow);
        }

        InlineKeyboardRow backRow = new InlineKeyboardRow();
        backRow.add(ru.lewis.leykabot.model.button.StyledInlineButton.styledBuilder()
                .text("Orqaga")
                .callbackData("back_admin")
                .iconCustomEmojiId("5258236805890710909")
                .build());
        keyboard.add(backRow);

        return InlineKeyboardMarkup.builder().keyboard(keyboard).build();
    }
}

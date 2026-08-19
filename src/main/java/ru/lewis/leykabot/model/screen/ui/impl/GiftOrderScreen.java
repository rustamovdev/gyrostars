package ru.lewis.leykabot.model.screen.ui.impl;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import ru.lewis.leykabot.model.button.StyledInlineButton;
import ru.lewis.leykabot.model.screen.ui.AbstractScreen;
import ru.lewis.leykabot.model.screen.ui.ScreenFactory;
import ru.lewis.leykabot.model.screen.ui.ScreenManager;
import ru.lewis.leykabot.service.*;

import java.util.ArrayList;
import java.util.List;

public class GiftOrderScreen extends AbstractScreen {

    private final String giftId;
    private final ScreenManager screenManager;
    private final ScreenFactory screenFactory;
    private final GiftService giftService;
    private final PriceService priceService;
    private final UserService userService;
    private final TransactionService transactionService;
    private final TelegramService telegramService;
    private final OrderChannelService orderChannelService;

    // Wizard Holatlari:
    // 0 = Qabul qiluvchini tanlash (O'zimga yoki Boshqa)
    // 1 = Boshqa username kiritish kutilmoqda
    // 2 = Tabrik / Izoh matnini kiritish kutilmoqda
    // 3 = Anonimlikni tanlash
    // 4 = Tasdiqlash oynasi
    private int step = 0;

    private String targetUser = "";
    private String comment = "";
    private boolean isAnonymous = false;

    public GiftOrderScreen(Long chatId, Long userId,
                           String giftId,
                           ScreenManager screenManager,
                           ScreenFactory screenFactory,
                           GiftService giftService,
                           PriceService priceService,
                           UserService userService,
                           TransactionService transactionService,
                           TelegramService telegramService,
                           OrderChannelService orderChannelService) {
        super(chatId, userId);
        this.giftId = giftId;
        this.screenManager = screenManager;
        this.screenFactory = screenFactory;
        this.giftService = giftService;
        this.priceService = priceService;
        this.userService = userService;
        this.transactionService = transactionService;
        this.telegramService = telegramService;
        this.orderChannelService = orderChannelService;
    }

    @Override
    public void handleCallback(String callback, TelegramClient bot) {
        switch (callback) {
            case "target_yourself" -> {
                String raw = telegramService.getRawUsernameByUserId(userId);
                this.targetUser = (raw != null && !raw.isBlank()) ? raw : String.valueOf(userId);
                this.step = 2; // Izoh bosqichiga o'tish
                screenManager.updateScreen(chatId, this);
            }

            case "target_other" -> {
                this.step = 1; // Username kiritish kutilmoqda
                telegramService.sendMessageAuto(chatId, "✍️ <b>Qabul qiluvchi foydalanuvchi nomini yoki ID raqamini yuboring:</b>\n\nMisol: <code>@username</code> yoki <code>123456789</code>");
            }

            case "skip_comment" -> {
                this.comment = "";
                this.step = 3; // Anonimlik tanlash bosqichi
                screenManager.updateScreen(chatId, this);
            }

            case "anon_true" -> {
                this.isAnonymous = true;
                this.step = 4; // Tasdiqlash bosqichi
                screenManager.updateScreen(chatId, this);
            }

            case "anon_false" -> {
                this.isAnonymous = false;
                this.step = 4; // Tasdiqlash bosqichi
                screenManager.updateScreen(chatId, this);
            }

            case "confirm_order" -> executeGiftPurchase();

            case "cancel_order" -> {
                screenManager.updateScreen(chatId, screenFactory.createGiftCatalogScreen(chatId, userId, "unlimited"));
            }

            case "back" -> {
                if (step > 0) {
                    step = 0;
                    targetUser = "";
                    comment = "";
                    screenManager.updateScreen(chatId, this);
                } else {
                    screenManager.updateScreen(chatId, screenFactory.createGiftCatalogScreen(chatId, userId, "unlimited"));
                }
            }
        }
    }

    @Override
    public void handleMessage(String text, TelegramClient bot) {
        if (text == null || text.isBlank()) return;
        String trimmed = text.trim();

        if (step == 1) {
            this.targetUser = trimmed.startsWith("@") ? trimmed.substring(1) : trimmed;
            this.step = 2; // Izoh bosqichiga o'tish
            screenManager.updateScreen(chatId, this);
            return;
        }

        if (step == 2) {
            this.comment = trimmed;
            this.step = 3; // Anonimlik tanlash bosqichiga o'tish
            screenManager.updateScreen(chatId, this);
        }
    }

    private void executeGiftPurchase() {
        GiftService.GiftItem giftItem = giftService.findGiftById(giftId);
        if (giftItem == null) {
            telegramService.sendMessageAuto(chatId, "❌ Sovg‘a topilmadi!");
            return;
        }

        int price = priceService.getStarsPrice(giftItem.getStars());
        int balance = userService.getBalance(userId).orElse(0);

        if (balance < price) {
            telegramService.sendMessageAuto(chatId, "⚠️ <b>Balansingizda yetarli mablag‘ mavjud emas!</b>\n\n" +
                    "Kerakli summa: <b>" + String.format("%,d", price).replace(',', ' ') + " so‘m</b>\n" +
                    "Sizning balansingiz: <b>" + String.format("%,d", balance).replace(',', ' ') + " so‘m</b>\n\n" +
                    "Iltimos, hisobingizni to‘ldiring.");
            screenManager.updateScreen(chatId, screenFactory.createDepositRublesScreen(chatId, userId));
            return;
        }

        // Balansdan yechish
        transactionService.create(userId, -price);

        telegramService.sendMessageAuto(chatId, "⏳ <b>Buyurtma bajarilmoqda...</b>\n\n" +
                "Userbot orqali sovg‘a qabul qiluvchiga yuborilmoqda. Iltimos, kuting...");

        // Userbot orqali yuborish
        GiftService.GiftSendResult sendResult = giftService.sendGift(targetUser, giftId, comment, isAnonymous);

        if (sendResult.isSuccess()) {
            String giftName = giftItem.getEmoji() + " " + giftItem.getName() + " (" + giftItem.getStars() + "⭐️)";
            String formattedPrice = String.format("%,d", price).replace(',', ' ');

            telegramService.sendMessageAuto(chatId, "✅ <b>SOVG‘A MUVAFFAQIYATLI YUBORILDI!</b> 🎁\n\n" +
                    "🎁 <b>Sovg‘a:</b> " + giftName + "\n" +
                    "👤 <b>Qabul qiluvchi:</b> <code>" + targetUser + "</code>\n" +
                    "🕵️ <b>Yuboruvchi:</b> " + (isAnonymous ? "🔒 Anonim" : "🔓 Ochiq") + "\n" +
                    (comment.isBlank() ? "" : "💬 <b>Izoh:</b> " + comment + "\n") +
                    "💰 <b>To‘langan summa:</b> " + formattedPrice + " so‘m\n\n" +
                    "<i>Sovg‘a foydalanuvchi profiliga biriktirildi!</i>");

            if (orderChannelService != null) {
                orderChannelService.sendOrderNotification("🎁 Telegram Gift", giftName, targetUser, price);
            }

            screenManager.updateScreen(chatId, screenFactory.createStartScreen(chatId, userId));
        } else {
            // Xatolik yuz berganda mablag'ni qaytarish
            transactionService.create(userId, price);
            telegramService.sendMessageAuto(chatId, "❌ <b>Sovg‘a yuborishda xatolik yuz berdi!</b>\n\n" +
                    "⚠️ <b>Sabab:</b> " + sendResult.getErrorMessage() + "\n\n" +
                    "<i>To‘langan mablag‘ balansingizga to‘liq qaytarildi.</i>");
            screenManager.updateScreen(chatId, screenFactory.createGiftCatalogScreen(chatId, userId, "unlimited"));
        }
    }

    @Override
    public String getText() {
        GiftService.GiftItem item = giftService.findGiftById(giftId);
        String giftName = item != null ? (item.getEmoji() + " " + item.getName() + " (" + item.getStars() + "⭐️)") : "Sovg'a";
        int price = item != null ? priceService.getStarsPrice(item.getStars()) : 0;
        String formattedPrice = String.format("%,d", price).replace(',', ' ');

        int balance = userService.getBalance(userId).orElse(0);
        String formattedBalance = String.format("%,d", balance).replace(',', ' ');

        if (step == 0 || step == 1) {
            return "🎁 <b>Sovg‘a xaridi</b>\n\n" +
                    "🎁 <b>Tanlangan sovg‘a:</b> " + giftName + "\n" +
                    "💰 <b>Narxi:</b> " + formattedPrice + " so‘m\n" +
                    "💳 <b>Balansingiz:</b> " + formattedBalance + " so‘m\n\n" +
                    "Ushbu sovg‘ani kimga yubormoqchisiz? 👇";
        }

        if (step == 2) {
            return "💬 <b>Tabrik / Izoh matni</b>\n\n" +
                    "👤 <b>Qabul qiluvchi:</b> <code>" + targetUser + "</code>\n" +
                    "🎁 <b>Sovg‘a:</b> " + giftName + "\n\n" +
                    "Sovg‘a bilan birga yuboriladigan tabrik so‘zi yoki izohingizni yozib yuboring:\n" +
                    "<i>(Yoki pastdagi '⏩ Izohsiz davom etish' tugmasini bosing)</i>";
        }

        if (step == 3) {
            return "🕵️ <b>Anonimlik holati</b>\n\n" +
                    "Sovg‘a oluvchiga sizning ismingiz ko‘rinsinmi yoki <b>Anonim (Yashirin)</b> bo‘lib borsinmi?";
        }

        // Step 4: Tasdiqlash
        return "📋 <b>Buyurtmani tasdiqlash</b>\n\n" +
                "🎁 <b>Sovg‘a:</b> " + giftName + "\n" +
                "👤 <b>Qabul qiluvchi:</b> <code>" + targetUser + "</code>\n" +
                "🕵️ <b>Yuboruvchi:</b> " + (isAnonymous ? "🔒 Anonim (Yashirin)" : "🔓 Ochiq (Ismingiz bilan)") + "\n" +
                (comment.isBlank() ? "" : "💬 <b>Izoh:</b> " + comment + "\n") +
                "💰 <b>Narxi:</b> " + formattedPrice + " so‘m\n" +
                "💳 <b>Sizning balansingiz:</b> " + formattedBalance + " so‘m\n\n" +
                "Buyurtmani tasdiqlaysizmi?";
    }

    @Override
    protected InlineKeyboardMarkup getKeyboard() {
        List<InlineKeyboardRow> keyboard = new ArrayList<>();

        if (step == 0 || step == 1) {
            InlineKeyboardRow row1 = new InlineKeyboardRow();
            row1.add(StyledInlineButton.styledBuilder()
                    .text("👤 O‘zimga yuborish")
                    .callbackData("target_yourself")
                    .style("primary")
                    .build());
            row1.add(StyledInlineButton.styledBuilder()
                    .text("👥 Boshqa foydalanuvchiga")
                    .callbackData("target_other")
                    .style("primary")
                    .build());

            InlineKeyboardRow rowBack = new InlineKeyboardRow();
            rowBack.add(StyledInlineButton.styledBuilder()
                    .text("Orqaga")
                    .callbackData("back")
                    .style("primary")
                    .iconCustomEmojiId("5258236805890710909")
                    .build());

            keyboard.add(row1);
            keyboard.add(rowBack);
            return InlineKeyboardMarkup.builder().keyboard(keyboard).build();
        }

        if (step == 2) {
            InlineKeyboardRow row1 = new InlineKeyboardRow();
            row1.add(StyledInlineButton.styledBuilder()
                    .text("⏩ Izohsiz davom etish")
                    .callbackData("skip_comment")
                    .style("primary")
                    .build());

            InlineKeyboardRow rowBack = new InlineKeyboardRow();
            rowBack.add(StyledInlineButton.styledBuilder()
                    .text("Orqaga")
                    .callbackData("back")
                    .style("primary")
                    .iconCustomEmojiId("5258236805890710909")
                    .build());

            keyboard.add(row1);
            keyboard.add(rowBack);
            return InlineKeyboardMarkup.builder().keyboard(keyboard).build();
        }

        if (step == 3) {
            InlineKeyboardRow row1 = new InlineKeyboardRow();
            row1.add(StyledInlineButton.styledBuilder()
                    .text("🔒 Anonim (Yashirin)")
                    .callbackData("anon_true")
                    .style("primary")
                    .build());
            row1.add(StyledInlineButton.styledBuilder()
                    .text("🔓 Ochiq (Ismim bilan)")
                    .callbackData("anon_false")
                    .style("primary")
                    .build());

            InlineKeyboardRow rowBack = new InlineKeyboardRow();
            rowBack.add(StyledInlineButton.styledBuilder()
                    .text("Orqaga")
                    .callbackData("back")
                    .style("primary")
                    .iconCustomEmojiId("5258236805890710909")
                    .build());

            keyboard.add(row1);
            keyboard.add(rowBack);
            return InlineKeyboardMarkup.builder().keyboard(keyboard).build();
        }

        // Step 4: Tasdiqlash tugmalari
        InlineKeyboardRow row1 = new InlineKeyboardRow();
        row1.add(StyledInlineButton.styledBuilder()
                .text("✅ Ha, yuborish")
                .callbackData("confirm_order")
                .style("success")
                .iconCustomEmojiId("5436406725232074977")
                .build());
        row1.add(StyledInlineButton.styledBuilder()
                .text("❌ Bekor qilish")
                .callbackData("cancel_order")
                .style("danger")
                .build());

        keyboard.add(row1);
        return InlineKeyboardMarkup.builder().keyboard(keyboard).build();
    }
}

package ru.lewis.leykabot.model.screen.ui.impl.admin;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import ru.lewis.leykabot.model.screen.ui.AbstractScreen;
import ru.lewis.leykabot.model.screen.ui.ScreenFactory;
import ru.lewis.leykabot.model.screen.ui.ScreenManager;
import ru.lewis.leykabot.service.AdminService;
import ru.lewis.leykabot.service.PriceService;
import ru.lewis.leykabot.service.TelegramService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AdminPriceScreen extends AbstractScreen {

    private final ScreenManager screenManager;
    private final ScreenFactory screenFactory;
    private final AdminService adminService;
    private final PriceService priceService;
    private final TelegramService telegramService;

    private String selectedKey = null;
    private boolean isWaitingNewPrice = false;

    public AdminPriceScreen(Long chatId, Long userId,
                            ScreenManager screenManager,
                            ScreenFactory screenFactory,
                            AdminService adminService,
                            PriceService priceService,
                            TelegramService telegramService) {
        super(chatId, userId);
        this.screenManager = screenManager;
        this.screenFactory = screenFactory;
        this.adminService = adminService;
        this.priceService = priceService;
        this.telegramService = telegramService;
    }

    @Override
    public void handleCallback(String callback, TelegramClient bot) {
        if (!adminService.isAdmin(userId)) return;

        if ("back_admin".equals(callback)) {
            isWaitingNewPrice = false;
            selectedKey = null;
            screenManager.updateScreen(chatId, screenFactory.createAdminMainScreen(chatId, userId));
            return;
        }

        if (callback.startsWith("edit_")) {
            selectedKey = callback.substring(5);
            isWaitingNewPrice = true;
            String name = getSettingDisplayName(selectedKey);
            telegramService.sendMessageAuto(chatId, "<b>" + name + "</b> uchun yangi narxni kiriting (so‘mda, faqat raqam):\n\n<i>Masalan: 25000</i>");
        }
    }

    @Override
    public void handleMessage(String text, TelegramClient bot) {
        if (!isWaitingNewPrice || selectedKey == null || !adminService.isAdmin(userId)) return;

        try {
            int newPrice = Integer.parseInt(text.trim().replaceAll("\\s+", ""));
            if (newPrice > 0) {
                priceService.setPrice(selectedKey, newPrice);
                String name = getSettingDisplayName(selectedKey);
                String formatted = String.format("%,d", newPrice).replace(',', ' ');
                telegramService.sendMessageAuto(chatId, "✅ <b>" + name + "</b> narxi <b>" + formatted + " so‘m</b>ga o‘zgartirildi!");
                isWaitingNewPrice = false;
                selectedKey = null;
                screenManager.updateScreen(chatId, this);
                return;
            }
        } catch (NumberFormatException ignored) {}

        telegramService.sendMessageAuto(chatId, "❌ Iltimos, musbat raqam kiriting (masalan: 30000)!");
    }

    private String getSettingDisplayName(String key) {
        return switch (key) {
            case "STAR_50" -> "50 Stars";
            case "STAR_100" -> "100 Stars";
            case "STAR_150" -> "150 Stars";
            case "STAR_250" -> "250 Stars";
            case "STAR_350" -> "350 Stars";
            case "STAR_500" -> "500 Stars";
            case "STAR_750" -> "750 Stars";
            case "STAR_1000" -> "1000 Stars";
            case "STAR_PER_UNIT" -> "1 ta star narxi (boshqa miqdor)";
            case "PREMIUM_1" -> "1 oylik Premium";
            case "PREMIUM_3" -> "3 oylik Premium";
            case "PREMIUM_6" -> "6 oylik Premium";
            case "PREMIUM_12" -> "1 yillik Premium";
            case "PUBG_60" -> "60 PUBG UC";
            case "PUBG_325" -> "325 PUBG UC";
            case "PUBG_660" -> "660 PUBG UC";
            case "PUBG_1800" -> "1800 PUBG UC";
            case "PUBG_3850" -> "3850 PUBG UC";
            case "PUBG_8100" -> "8100 PUBG UC";
            default -> key;
        };
    }

    @Override
    public String getText() {
        StringBuilder sb = new StringBuilder();
        sb.append("💰 <b>Narxlarni Boshqarish Paneli</b>\n\n");

        sb.append("⭐️ <b>Stars narxlari:</b>\n");
        Map<Integer, Integer> starPrices = priceService.getAllStarPrices();
        for (Map.Entry<Integer, Integer> entry : starPrices.entrySet()) {
            sb.append("• <b>").append(entry.getKey()).append(" Stars:</b> ")
                    .append(String.format("%,d", entry.getValue()).replace(',', ' '))
                    .append(" so‘m\n");
        }
        sb.append("• <i>1 ta star (boshqa miqdor):</i> ")
                .append(priceService.getPrice("STAR_PER_UNIT", 230))
                .append(" so‘m\n\n");

        sb.append("💎 <b>Premium narxlari:</b>\n");
        Map<Integer, Integer> premPrices = priceService.getAllPremiumPrices();
        for (Map.Entry<Integer, Integer> entry : premPrices.entrySet()) {
            String label = switch (entry.getKey()) {
                case 1 -> "1 oylik";
                case 3 -> "3 oylik";
                case 6 -> "6 oylik";
                case 12 -> "1 yillik";
                default -> entry.getKey() + " oylik";
            };
            sb.append("• <b>").append(label).append(":</b> ")
                    .append(String.format("%,d", entry.getValue()).replace(',', ' '))
                    .append(" so‘m\n");
        }

        sb.append("\n🎮 <b>PUBG UC narxlari:</b>\n");
        Map<Integer, Integer> pubgPrices = priceService.getAllPubgPrices();
        for (Map.Entry<Integer, Integer> entry : pubgPrices.entrySet()) {
            sb.append("• <b>").append(entry.getKey()).append(" UC:</b> ")
                    .append(String.format("%,d", entry.getValue()).replace(',', ' '))
                    .append(" so‘m\n");
        }

        sb.append("\nNarxni o‘zgartirish uchun pastdagi tugmalardan birini bosing 👇");
        return sb.toString();
    }

    @Override
    protected InlineKeyboardMarkup getKeyboard() {
        List<InlineKeyboardRow> keyboard = new ArrayList<>();

        InlineKeyboardRow row1 = new InlineKeyboardRow();
        row1.add(InlineKeyboardButton.builder().text("⭐️ 50").callbackData("edit_STAR_50").build());
        row1.add(InlineKeyboardButton.builder().text("⭐️ 100").callbackData("edit_STAR_100").build());
        row1.add(InlineKeyboardButton.builder().text("⭐️ 150").callbackData("edit_STAR_150").build());
        row1.add(InlineKeyboardButton.builder().text("⭐️ 250").callbackData("edit_STAR_250").build());

        InlineKeyboardRow row2 = new InlineKeyboardRow();
        row2.add(InlineKeyboardButton.builder().text("⭐️ 350").callbackData("edit_STAR_350").build());
        row2.add(InlineKeyboardButton.builder().text("⭐️ 500").callbackData("edit_STAR_500").build());
        row2.add(InlineKeyboardButton.builder().text("⭐️ 750").callbackData("edit_STAR_750").build());
        row2.add(InlineKeyboardButton.builder().text("⭐️ 1000").callbackData("edit_STAR_1000").build());

        InlineKeyboardRow row3 = new InlineKeyboardRow();
        row3.add(InlineKeyboardButton.builder().text("⭐️ 1 ta Star").callbackData("edit_STAR_PER_UNIT").build());
        row3.add(InlineKeyboardButton.builder().text("💎 1 oy").callbackData("edit_PREMIUM_1").build());
        row3.add(InlineKeyboardButton.builder().text("💎 3 oy").callbackData("edit_PREMIUM_3").build());
        row3.add(InlineKeyboardButton.builder().text("💎 6 oy").callbackData("edit_PREMIUM_6").build());
        row3.add(InlineKeyboardButton.builder().text("💎 1 yil").callbackData("edit_PREMIUM_12").build());

        InlineKeyboardRow pubgRow1 = new InlineKeyboardRow();
        pubgRow1.add(InlineKeyboardButton.builder().text("🎮 60 UC").callbackData("edit_PUBG_60").build());
        pubgRow1.add(InlineKeyboardButton.builder().text("🎮 325 UC").callbackData("edit_PUBG_325").build());
        pubgRow1.add(InlineKeyboardButton.builder().text("🎮 660 UC").callbackData("edit_PUBG_660").build());

        InlineKeyboardRow pubgRow2 = new InlineKeyboardRow();
        pubgRow2.add(InlineKeyboardButton.builder().text("🎮 1800 UC").callbackData("edit_PUBG_1800").build());
        pubgRow2.add(InlineKeyboardButton.builder().text("🎮 3850 UC").callbackData("edit_PUBG_3850").build());
        pubgRow2.add(InlineKeyboardButton.builder().text("🎮 8100 UC").callbackData("edit_PUBG_8100").build());

        InlineKeyboardRow row5 = new InlineKeyboardRow();
        row5.add(ru.lewis.leykabot.model.button.StyledInlineButton.styledBuilder()
                .text("Orqaga")
                .callbackData("back_admin")
                .iconCustomEmojiId("5258236805890710909")
                .build());

        keyboard.add(row1);
        keyboard.add(row2);
        keyboard.add(row3);
        keyboard.add(pubgRow1);
        keyboard.add(pubgRow2);
        keyboard.add(row5);

        return InlineKeyboardMarkup.builder().keyboard(keyboard).build();
    }
}

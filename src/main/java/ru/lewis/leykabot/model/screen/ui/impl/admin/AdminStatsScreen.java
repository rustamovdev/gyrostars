package ru.lewis.leykabot.model.screen.ui.impl.admin;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import ru.lewis.leykabot.model.screen.ui.AbstractScreen;
import ru.lewis.leykabot.model.screen.ui.ScreenFactory;
import ru.lewis.leykabot.model.screen.ui.ScreenManager;
import ru.lewis.leykabot.service.AdminService;

import java.util.ArrayList;
import java.util.List;

public class AdminStatsScreen extends AbstractScreen {

    private final ScreenManager screenManager;
    private final ScreenFactory screenFactory;
    private final AdminService adminService;

    public AdminStatsScreen(Long chatId, Long userId,
                            ScreenManager screenManager,
                            ScreenFactory screenFactory,
                            AdminService adminService) {
        super(chatId, userId);
        this.screenManager = screenManager;
        this.screenFactory = screenFactory;
        this.adminService = adminService;
    }

    @Override
    public void handleCallback(String callback, TelegramClient bot) {
        if (!adminService.isAdmin(userId)) return;

        switch (callback) {
            case "refresh" -> screenManager.updateScreen(chatId, this);
            case "back_admin" -> screenManager.updateScreen(chatId, screenFactory.createAdminMainScreen(chatId, userId));
        }
    }

    @Override
    public String getText() {
        AdminService.AdminStats stats = adminService.getStats();

        String formattedDeposited = String.format("%,d", stats.totalDeposited()).replace(',', ' ');
        String formattedStars = String.format("%,d", stats.totalStars()).replace(',', ' ');
        String formattedPremium = String.format("%,d", stats.totalPremiumMonths()).replace(',', ' ');
        String formattedTx = String.format("%,d", stats.totalTransactions()).replace(',', ' ');
        String formattedUsers = String.format("%,d", stats.totalUsers()).replace(',', ' ');

        return "📊 <b>Bot To‘liq Statistikasi:</b>\n\n" +
                "👥 <b>Jami foydalanuvchilar:</b> " + formattedUsers + " ta\n" +
                "📥 <b>Jami to‘ldirilgan summa:</b> " + formattedDeposited + " so‘m\n" +
                "<tg-emoji emoji-id=\"5436050603723760533\">⭐️</tg-emoji> <b>Jami sotilgan Stars:</b> " + formattedStars + " Stars\n" +
                "<tg-emoji emoji-id=\"5938420017665152105\">💎</tg-emoji> <b>Jami sotilgan Premium:</b> " + formattedPremium + " oy\n" +
                "⚡️ <b>Jami tranzaksiyalar:</b> " + formattedTx + " ta\n\n" +
                "<i>Ma’lumotlar real vaqt rejimida yangilanadi.</i>";
    }

    @Override
    protected InlineKeyboardMarkup getKeyboard() {
        List<InlineKeyboardRow> keyboard = new ArrayList<>();

        InlineKeyboardRow row1 = new InlineKeyboardRow();
        row1.add(InlineKeyboardButton.builder().text("🔄 Yangilash").callbackData("refresh").build());
        row1.add(ru.lewis.leykabot.model.button.StyledInlineButton.styledBuilder()
                .text("Orqaga")
                .callbackData("back_admin")
                .iconCustomEmojiId("5258236805890710909")
                .build());

        keyboard.add(row1);
        return InlineKeyboardMarkup.builder().keyboard(keyboard).build();
    }
}

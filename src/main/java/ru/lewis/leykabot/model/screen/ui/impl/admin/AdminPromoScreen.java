package ru.lewis.leykabot.model.screen.ui.impl.admin;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import ru.lewis.leykabot.model.database.entity.Code;
import ru.lewis.leykabot.model.screen.ui.AbstractScreen;
import ru.lewis.leykabot.model.screen.ui.ScreenFactory;
import ru.lewis.leykabot.model.screen.ui.ScreenManager;
import ru.lewis.leykabot.service.AdminService;
import ru.lewis.leykabot.service.TelegramService;

import java.util.ArrayList;
import java.util.List;

public class AdminPromoScreen extends AbstractScreen {

    private final ScreenManager screenManager;
    private final ScreenFactory screenFactory;
    private final AdminService adminService;
    private final TelegramService telegramService;

    private boolean isWaitingCreateInput = false;

    public AdminPromoScreen(Long chatId, Long userId,
                            ScreenManager screenManager,
                            ScreenFactory screenFactory,
                            AdminService adminService,
                            TelegramService telegramService) {
        super(chatId, userId);
        this.screenManager = screenManager;
        this.screenFactory = screenFactory;
        this.adminService = adminService;
        this.telegramService = telegramService;
    }

    @Override
    public void handleCallback(String callback, TelegramClient bot) {
        if (!adminService.isAdmin(userId)) return;

        switch (callback) {
            case "create_promo" -> {
                isWaitingCreateInput = true;
                telegramService.sendMessageAuto(chatId,
                        "Yangi promokod yaratish uchun quyidagi formatda yozing:\n\n" +
                                "<code>KOD SUMMA FOYDALANISH_SONI</code>\n\n" +
                                "Masalan: <code>YANGIYIL2026 50000 10</code>\n" +
                                "(50 000 so‘mlik, 10 kishi uchun)");
            }
            case "back_admin" -> {
                isWaitingCreateInput = false;
                screenManager.updateScreen(chatId, screenFactory.createAdminMainScreen(chatId, userId));
            }
        }
    }

    @Override
    public void handleMessage(String text, TelegramClient bot) {
        if (!isWaitingCreateInput || !adminService.isAdmin(userId)) return;

        String[] parts = text.trim().split("\\s+");
        if (parts.length >= 3) {
            try {
                String code = parts[0].toUpperCase();
                int amount = Integer.parseInt(parts[1]);
                int count = Integer.parseInt(parts[2]);

                if (amount > 0 && count > 0) {
                    adminService.createPromo(code, amount, count);
                    telegramService.sendMessageAuto(chatId,
                            "✅ Promokod muvaffaqiyatli yaratildi!\n\n" +
                                    "🎟 Kod: <code>" + code + "</code>\n" +
                                    "💰 Summa: <b>" + String.format("%,d", amount).replace(',', ' ') + " so‘m</b>\n" +
                                    "👥 Limit: <b>" + count + " ta</b>");
                    isWaitingCreateInput = false;
                    screenManager.updateScreen(chatId, this);
                    return;
                }
            } catch (NumberFormatException e) {
                // fall through
            }
        }

        telegramService.sendMessageAuto(chatId, "❌ Noto‘g‘ri format! Masalan: <code>BONUS 25000 5</code>");
    }

    @Override
    public String getText() {
        List<Code> codes = adminService.getAllPromoCodes();

        StringBuilder sb = new StringBuilder();
        sb.append("🎟 <b>Promokodlar Boshqaruvi</b>\n\n");

        if (codes.isEmpty()) {
            sb.append("<i>Hozircha faol promokodlar yo‘q.</i>\n");
        } else {
            sb.append("📋 <b>Mavjud promokodlar:</b>\n\n");
            for (Code c : codes) {
                String sum = String.format("%,d", c.getAmount()).replace(',', ' ');
                sb.append("• <code>").append(c.getCode()).append("</code> — <b>")
                        .append(sum).append(" so‘m</b> (Ishlatildi: ").append(c.getUsedCount())
                        .append("/").append(c.getUsageLimit()).append(")\n");
            }
        }

        return sb.toString();
    }

    @Override
    protected InlineKeyboardMarkup getKeyboard() {
        List<InlineKeyboardRow> keyboard = new ArrayList<>();

        InlineKeyboardRow row1 = new InlineKeyboardRow();
        row1.add(InlineKeyboardButton.builder().text("➕ Yangi promokod").callbackData("create_promo").build());
        row1.add(ru.lewis.leykabot.model.button.StyledInlineButton.styledBuilder()
                .text("Orqaga")
                .callbackData("back_admin")
                .iconCustomEmojiId("5258236805890710909")
                .build());

        keyboard.add(row1);
        return InlineKeyboardMarkup.builder().keyboard(keyboard).build();
    }
}

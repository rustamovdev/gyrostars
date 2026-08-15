package ru.lewis.leykabot.model.screen.ui.impl.admin;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import ru.lewis.leykabot.model.screen.ui.AbstractScreen;
import ru.lewis.leykabot.model.screen.ui.ScreenFactory;
import ru.lewis.leykabot.model.screen.ui.ScreenManager;
import ru.lewis.leykabot.service.AdminService;
import ru.lewis.leykabot.service.TelegramService;

import java.util.ArrayList;
import java.util.List;

public class AdminBroadcastScreen extends AbstractScreen {

    private final ScreenManager screenManager;
    private final ScreenFactory screenFactory;
    private final AdminService adminService;
    private final TelegramService telegramService;

    private boolean isWaitingMessage = true;

    public AdminBroadcastScreen(Long chatId, Long userId,
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

        if ("back_admin".equals(callback)) {
            isWaitingMessage = false;
            screenManager.updateScreen(chatId, screenFactory.createAdminMainScreen(chatId, userId));
        }
    }

    @Override
    public void handleMessage(String text, TelegramClient bot) {
        if (!isWaitingMessage || !adminService.isAdmin(userId)) return;
        isWaitingMessage = false;

        telegramService.sendMessageAuto(chatId, "⏳ Xabar barcha foydalanuvchilarga yuborilmoqda, kuting...");

        adminService.broadcast(text).thenAccept(result -> {
            String report = "📢 <b>Xabar yuborish yakunlandi!</b>\n\n" +
                    "👥 Jami foydalanuvchilar: " + result.total() + " ta\n" +
                    "✅ Muvaffaqiyatli yetkazildi: " + result.success() + " ta\n" +
                    "❌ Yetib bormadi (bloklagan): " + result.failed() + " ta";

            telegramService.sendMessageAuto(chatId, report);
            screenManager.updateScreen(chatId, screenFactory.createAdminMainScreen(chatId, userId));
        });
    }

    @Override
    public String getText() {
        return "📢 <b>Barcha foydalanuvchilarga xabar yuborish</b>\n\n" +
                "Iltimos, yubormoqchi bo‘lgan xabar matnini yozib yuboring (HTML teglar qo‘llab-quvvatlanadi).\n\n" +
                "<i>Bekor qilish uchun '◀️ Orqaga' tugmasini bosing.</i>";
    }

    @Override
    protected InlineKeyboardMarkup getKeyboard() {
        List<InlineKeyboardRow> keyboard = new ArrayList<>();
        InlineKeyboardRow row = new InlineKeyboardRow();
        row.add(ru.lewis.leykabot.model.button.StyledInlineButton.styledBuilder()
                .text("Orqaga")
                .callbackData("back_admin")
                .iconCustomEmojiId("5258236805890710909")
                .build());
        keyboard.add(row);
        return InlineKeyboardMarkup.builder().keyboard(keyboard).build();
    }
}

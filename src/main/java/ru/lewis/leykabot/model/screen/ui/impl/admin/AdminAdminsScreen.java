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

public class AdminAdminsScreen extends AbstractScreen {

    private final ScreenManager screenManager;
    private final ScreenFactory screenFactory;
    private final AdminService adminService;
    private final TelegramService telegramService;

    private boolean isWaitingAddAdmin = false;
    private boolean isWaitingRemoveAdmin = false;

    public AdminAdminsScreen(Long chatId, Long userId,
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
            case "add_admin" -> {
                isWaitingAddAdmin = true;
                isWaitingRemoveAdmin = false;
                telegramService.sendMessageAuto(chatId, "Qo‘shmoqchi bo‘lgan yangi adminning <b>Telegram ID</b> raqamini yozib yuboring:\n\n<i>Masalan: 123456789</i>");
            }
            case "remove_admin" -> {
                isWaitingRemoveAdmin = true;
                isWaitingAddAdmin = false;
                telegramService.sendMessageAuto(chatId, "O‘chirmoqchi bo‘lgan adminning <b>Telegram ID</b> raqamini yozib yuboring:");
            }
            case "back_admin" -> {
                isWaitingAddAdmin = false;
                isWaitingRemoveAdmin = false;
                screenManager.updateScreen(chatId, screenFactory.createAdminMainScreen(chatId, userId));
            }
        }
    }

    @Override
    public void handleMessage(String text, TelegramClient bot) {
        if (!adminService.isAdmin(userId)) return;

        if (isWaitingAddAdmin) {
            try {
                long newAdminId = Long.parseLong(text.trim().replaceAll("\\s+", ""));
                if (adminService.addAdmin(newAdminId)) {
                    String username = telegramService.getUsernameByUserId(newAdminId);
                    telegramService.sendMessageAuto(chatId, "✅ Yangi admin muvaffaqiyatli qo‘shildi: <code>" + newAdminId + "</code> (" + (username != null ? username : "User") + ")");
                } else {
                    telegramService.sendMessageAuto(chatId, "⚠️ Ushbu foydalanuvchi allaqachon adminlar ro‘yxatida mavjud!");
                }
            } catch (NumberFormatException e) {
                telegramService.sendMessageAuto(chatId, "❌ Iltimos, to‘g‘ri Telegram ID raqamini kiriting!");
            }
            isWaitingAddAdmin = false;
            screenManager.updateScreen(chatId, this);
            return;
        }

        if (isWaitingRemoveAdmin) {
            try {
                long targetId = Long.parseLong(text.trim().replaceAll("\\s+", ""));
                if (targetId == AdminService.PRIMARY_ADMIN) {
                    telegramService.sendMessageAuto(chatId, "❌ Bosh adminni o‘chirib bo‘lmaydi!");
                } else if (adminService.removeAdmin(targetId)) {
                    telegramService.sendMessageAuto(chatId, "✅ Admin muvaffaqiyatli o‘chirildi: <code>" + targetId + "</code>");
                } else {
                    telegramService.sendMessageAuto(chatId, "❌ Ushbu ID adminlar ro‘yxatidan topilmadi.");
                }
            } catch (NumberFormatException e) {
                telegramService.sendMessageAuto(chatId, "❌ Iltimos, to‘g‘ri Telegram ID raqamini kiriting!");
            }
            isWaitingRemoveAdmin = false;
            screenManager.updateScreen(chatId, this);
        }
    }

    @Override
    public String getText() {
        List<Long> admins = adminService.getAdmins();

        StringBuilder sb = new StringBuilder();
        sb.append("👥 <b>Adminlar Boshqaruvi</b>\n\n");
        sb.append("📋 <b>Mavjud adminlar ro‘yxati:</b>\n\n");

        for (Long id : admins) {
            String tag = telegramService.getUsernameByUserId(id);
            boolean isPrimary = id.equals(AdminService.PRIMARY_ADMIN);
            sb.append("• <code>").append(id).append("</code> (")
                    .append(tag != null ? tag : "User").append(")")
                    .append(isPrimary ? " 👑 <i>[Bosh admin]</i>" : " 🛡 <i>[Admin]</i>")
                    .append("\n");
        }

        sb.append("\nQuyidagi tugmalar orqali admin qo‘shishingiz yoki o‘chirishingiz mumkin 👇");
        return sb.toString();
    }

    @Override
    protected InlineKeyboardMarkup getKeyboard() {
        List<InlineKeyboardRow> keyboard = new ArrayList<>();

        InlineKeyboardRow row1 = new InlineKeyboardRow();
        row1.add(InlineKeyboardButton.builder().text("➕ Admin qo‘shish").callbackData("add_admin").build());
        row1.add(InlineKeyboardButton.builder().text("➖ Adminni o‘chirish").callbackData("remove_admin").build());

        InlineKeyboardRow row2 = new InlineKeyboardRow();
        row2.add(ru.lewis.leykabot.model.button.StyledInlineButton.styledBuilder()
                .text("Orqaga")
                .callbackData("back_admin")
                .iconCustomEmojiId("5258236805890710909")
                .build());

        keyboard.add(row1);
        keyboard.add(row2);

        return InlineKeyboardMarkup.builder().keyboard(keyboard).build();
    }
}

package ru.lewis.leykabot.model.screen.ui.impl.admin;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import ru.lewis.leykabot.model.screen.ui.AbstractScreen;
import ru.lewis.leykabot.model.screen.ui.ScreenFactory;
import ru.lewis.leykabot.model.screen.ui.ScreenManager;
import ru.lewis.leykabot.service.AdminService;
import ru.lewis.leykabot.service.FragmentStarsService;
import ru.lewis.leykabot.service.TelegramService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AdminMainScreen extends AbstractScreen {

    private final ScreenManager screenManager;
    private final ScreenFactory screenFactory;
    private final AdminService adminService;
    private final FragmentStarsService fragmentStarsService;
    private final TelegramService telegramService;

    public AdminMainScreen(Long chatId, Long userId,
                           ScreenManager screenManager,
                           ScreenFactory screenFactory,
                           AdminService adminService,
                           FragmentStarsService fragmentStarsService,
                           TelegramService telegramService) {
        super(chatId, userId);
        this.screenManager = screenManager;
        this.screenFactory = screenFactory;
        this.adminService = adminService;
        this.fragmentStarsService = fragmentStarsService;
        this.telegramService = telegramService;
    }

    @Override
    public void handleCallback(String callback, TelegramClient bot) {
        if (!adminService.isAdmin(userId)) return;

        switch (callback) {
            case "admin_stats" -> screenManager.updateScreen(chatId, screenFactory.createAdminStatsScreen(chatId, userId));
            case "admin_broadcast" -> screenManager.updateScreen(chatId, screenFactory.createAdminBroadcastScreen(chatId, userId));
            case "admin_admins" -> screenManager.updateScreen(chatId, screenFactory.createAdminAdminsScreen(chatId, userId));
            case "admin_order_channel" -> screenManager.updateScreen(chatId, screenFactory.createAdminOrderChannelScreen(chatId, userId));
            case "admin_prices" -> screenManager.updateScreen(chatId, screenFactory.createAdminPriceScreen(chatId, userId));
            case "admin_cards" -> screenManager.updateScreen(chatId, screenFactory.createAdminCardScreen(chatId, userId));
            case "admin_wallet" -> {
                telegramService.sendMessageAuto(chatId, "⏳ Fragment hamyon balansi yuklanmoqda...");
                fragmentStarsService.getWalletBalance().thenAccept(res -> {
                    if (res != null && res.isOk() && res.getResult() instanceof Map<?, ?> map) {
                        String ton = String.valueOf(map.get("balance_ton"));
                        String project = String.valueOf(map.get("project"));
                        String addr = String.valueOf(map.get("address"));
                        String net = String.valueOf(map.get("network"));
                        String ver = String.valueOf(map.get("wallet_version"));

                        telegramService.sendMessageAuto(chatId, "💎 <b>Fragment API Hamyon Holati:</b>\n\n" +
                                "🤖 <b>Loyiha:</b> " + project + "\n" +
                                "💰 <b>TON Balans:</b> <code>" + ton + " TON</code>\n" +
                                "📬 <b>Hamyon manzili:</b> <code>" + addr + "</code>\n" +
                                "🌐 <b>Tarmoq:</b> " + net + " (" + ver + ")");
                    } else {
                        String err = res != null && res.getMessage() != null ? res.getMessage() : "Ma'lumot olib bo'lmadi";
                        telegramService.sendMessageAuto(chatId, "❌ Hamyon ma'lumotlarini olishda xatolik: " + err);
                    }
                });
            }
            case "admin_maintenance" -> {
                adminService.toggleMaintenance();
                screenManager.updateScreen(chatId, this);
            }
            case "admin_exit" -> screenManager.updateScreen(chatId, screenFactory.createStartScreen(chatId, userId));
        }
    }

    @Override
    public String getText() {
        boolean maint = adminService.isMaintenanceMode();
        return "👑 <b>Boshqaruv Paneli (Admin Panel)</b>\n\n" +
                "Xush kelibsiz! Botni boshqarish uchun quyidagi bo‘limlardan birini tanlang:\n\n" +
                "🔧 <b>Texnik ishlar rejimi:</b> " + (maint ? "🔴 YOQILGAN (Faqat adminlar)" : "🟢 O‘CHIRILGAN (Barcha foydalanuvchilar)");
    }

    @Override
    protected InlineKeyboardMarkup getKeyboard() {
        List<InlineKeyboardRow> keyboard = new ArrayList<>();

        InlineKeyboardRow row1 = new InlineKeyboardRow();
        row1.add(InlineKeyboardButton.builder().text("📊 Statistika").callbackData("admin_stats").build());
        row1.add(InlineKeyboardButton.builder().text("📢 Xabar yuborish").callbackData("admin_broadcast").build());

        InlineKeyboardRow row2 = new InlineKeyboardRow();
        row2.add(InlineKeyboardButton.builder().text("💰 Narxlar").callbackData("admin_prices").build());
        row2.add(InlineKeyboardButton.builder().text("💳 To‘lov Kartalari").callbackData("admin_cards").build());

        InlineKeyboardRow row3 = new InlineKeyboardRow();
        row3.add(InlineKeyboardButton.builder().text("📣 Order Kanal").callbackData("admin_order_channel").build());
        row3.add(InlineKeyboardButton.builder().text("👥 Adminlar").callbackData("admin_admins").build());

        InlineKeyboardRow row4 = new InlineKeyboardRow();
        row4.add(InlineKeyboardButton.builder().text("💎 Fragment Hamyon").callbackData("admin_wallet").build());
        boolean maint = adminService.isMaintenanceMode();
        row4.add(InlineKeyboardButton.builder().text(maint ? "🟢 Ochish" : "🔴 Texnik ish").callbackData("admin_maintenance").build());

        InlineKeyboardRow row5 = new InlineKeyboardRow();
        row5.add(ru.lewis.leykabot.model.button.StyledInlineButton.styledBuilder()
                .text("Asosiy menyuga qaytish")
                .callbackData("admin_exit")
                .iconCustomEmojiId("5258236805890710909")
                .build());

        keyboard.add(row1);
        keyboard.add(row2);
        keyboard.add(row3);
        keyboard.add(row4);
        keyboard.add(row5);

        return InlineKeyboardMarkup.builder().keyboard(keyboard).build();
    }
}

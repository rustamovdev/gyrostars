package ru.lewis.leykabot.model.screen.ui.impl.admin;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import ru.lewis.leykabot.model.screen.ui.AbstractScreen;
import ru.lewis.leykabot.model.screen.ui.ScreenFactory;
import ru.lewis.leykabot.model.screen.ui.ScreenManager;
import ru.lewis.leykabot.service.AdminService;
import ru.lewis.leykabot.service.OrderChannelService;
import ru.lewis.leykabot.service.TelegramService;

import java.util.ArrayList;
import java.util.List;

public class AdminOrderChannelScreen extends AbstractScreen {

    private final ScreenManager screenManager;
    private final ScreenFactory screenFactory;
    private final AdminService adminService;
    private final OrderChannelService orderChannelService;
    private final TelegramService telegramService;

    private boolean isWaitingChannelInput = false;

    public AdminOrderChannelScreen(Long chatId, Long userId,
                                   ScreenManager screenManager,
                                   ScreenFactory screenFactory,
                                   AdminService adminService,
                                   OrderChannelService orderChannelService,
                                   TelegramService telegramService) {
        super(chatId, userId);
        this.screenManager = screenManager;
        this.screenFactory = screenFactory;
        this.adminService = adminService;
        this.orderChannelService = orderChannelService;
        this.telegramService = telegramService;
    }

    @Override
    public void handleCallback(String callback, TelegramClient bot) {
        if (!adminService.isAdmin(userId)) return;

        switch (callback) {
            case "set_channel" -> {
                isWaitingChannelInput = true;
                telegramService.sendMessageAuto(chatId, "📢 <b>Buyurtmalar kanalining username yoki ID sini yuboring:</b>\n\n" +
                        "<i>Masalan: <code>@buyurtmalar_kanali</code> yoki <code>-1001234567890</code></i>\n\n" +
                        "⚠️ <b>Eslatma:</b> Bot ushbu kanalda <b>Admin (Xabar yozish huquqi bilan)</b> bo‘lishi shart!");
            }
            case "test_channel" -> {
                String current = orderChannelService.getOrderChannel();
                if (current == null || current.isBlank()) {
                    telegramService.sendMessageAuto(chatId, "⚠️ Hozircha buyurtmalar kanali ulanmagan!");
                    return;
                }
                boolean success = orderChannelService.testChannel(current);
                if (success) {
                    telegramService.sendMessageAuto(chatId, "✅ <b>Test xabar muvaffaqiyatli yuborildi!</b> Kanal to‘g‘ri ulangan.");
                } else {
                    telegramService.sendMessageAuto(chatId, "❌ <b>Xatolik!</b> Bot kanalda xabar yoza olmadi. Botni kanalda <b>Admin</b> qilganingizni tekshiring.");
                }
            }
            case "clear_channel" -> {
                orderChannelService.setOrderChannel(null);
                telegramService.sendMessageAuto(chatId, "✅ Buyurtmalar kanali muvaffaqiyatli o‘chirildi.");
                screenManager.updateScreen(chatId, this);
            }
            case "back_admin" -> {
                isWaitingChannelInput = false;
                screenManager.updateScreen(chatId, screenFactory.createAdminMainScreen(chatId, userId));
            }
        }
    }

    @Override
    public void handleMessage(String text, TelegramClient bot) {
        if (!adminService.isAdmin(userId)) return;

        if (isWaitingChannelInput) {
            String channel = text.trim();
            orderChannelService.setOrderChannel(channel);
            isWaitingChannelInput = false;
            telegramService.sendMessageAuto(chatId, "✅ <b>Buyurtmalar kanali saqlandi:</b> <code>" + channel + "</code>\n\n" +
                    "Iltimos, bot kanalda <b>Admin</b> ekanligiga ishonch hosil qiling va '🧪 Test xabar' tugmasi orqali tekshirib ko‘ring.");
            screenManager.updateScreen(chatId, this);
        }
    }

    @Override
    public String getText() {
        String current = orderChannelService.getOrderChannel();
        String displayChannel = (current != null && !current.isBlank()) ? "<code>" + current + "</code>" : "<i>Ulanmagan</i>";

        return "📢 <b>Buyurtmalar Kanali Sozlamasi</b>\n\n" +
                "Har bir yangi buyurtma (Stars, Premium) muvaffaqiyatli sotib olinganda ushbu kanalga avtomatik tarzda buyurtma raqami (#ORD-101), mahsulot turi, miqdori va summasi yuboriladi.\n\n" +
                "📡 <b>Hozirgi kanal:</b> " + displayChannel + "\n\n" +
                "Kerakli amalni tanlang 👇";
    }

    @Override
    protected InlineKeyboardMarkup getKeyboard() {
        List<InlineKeyboardRow> keyboard = new ArrayList<>();

        InlineKeyboardRow row1 = new InlineKeyboardRow();
        row1.add(InlineKeyboardButton.builder().text("📢 Kanalni ulash / o‘zgartirish").callbackData("set_channel").build());

        InlineKeyboardRow row2 = new InlineKeyboardRow();
        row2.add(InlineKeyboardButton.builder().text("🧪 Test xabar yuborish").callbackData("test_channel").build());
        row2.add(InlineKeyboardButton.builder().text("🗑 O‘chirish").callbackData("clear_channel").build());

        InlineKeyboardRow row3 = new InlineKeyboardRow();
        row3.add(ru.lewis.leykabot.model.button.StyledInlineButton.styledBuilder()
                .text("Orqaga")
                .callbackData("back_admin")
                .iconCustomEmojiId("5258236805890710909")
                .build());

        keyboard.add(row1);
        keyboard.add(row2);
        keyboard.add(row3);

        return InlineKeyboardMarkup.builder().keyboard(keyboard).build();
    }
}

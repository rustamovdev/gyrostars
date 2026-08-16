package ru.lewis.leykabot.model.screen.ui.impl.admin;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import ru.lewis.leykabot.model.database.entity.User;
import ru.lewis.leykabot.model.screen.ui.AbstractScreen;
import ru.lewis.leykabot.model.screen.ui.ScreenFactory;
import ru.lewis.leykabot.model.screen.ui.ScreenManager;
import ru.lewis.leykabot.service.AdminService;
import ru.lewis.leykabot.service.TelegramService;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class AdminUserManageScreen extends AbstractScreen {

    private final ScreenManager screenManager;
    private final ScreenFactory screenFactory;
    private final AdminService adminService;
    private final TelegramService telegramService;

    private Long targetUserId = null;
    private boolean isWaitingUserId = true;
    private boolean isWaitingBalanceAmount = false;
    private boolean isWaitingPmMessage = false;
    private int balanceMultiplier = 1;

    public AdminUserManageScreen(Long chatId, Long userId,
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
            case "add_balance" -> {
                if (targetUserId != null) {
                    isWaitingBalanceAmount = true;
                    isWaitingPmMessage = false;
                    balanceMultiplier = 1;
                    telegramService.sendMessageAuto(chatId, "Qo‘shmoqchi bo‘lgan summani kiriting (so‘mda):");
                }
            }
            case "sub_balance" -> {
                if (targetUserId != null) {
                    isWaitingBalanceAmount = true;
                    isWaitingPmMessage = false;
                    balanceMultiplier = -1;
                    telegramService.sendMessageAuto(chatId, "Ayirmoqchi bo‘lgan summani kiriting (so‘mda):");
                }
            }
            case "send_pm" -> {
                if (targetUserId != null) {
                    isWaitingPmMessage = true;
                    isWaitingBalanceAmount = false;
                    telegramService.sendMessageAuto(chatId, "✉️ Foydalanuvchiga yubormoqchi bo‘lgan xabaringizni yozing:");
                }
            }
            case "search_another" -> {
                targetUserId = null;
                isWaitingUserId = true;
                isWaitingBalanceAmount = false;
                isWaitingPmMessage = false;
                screenManager.updateScreen(chatId, this);
            }
            case "back_admin" -> {
                isWaitingUserId = false;
                isWaitingBalanceAmount = false;
                isWaitingPmMessage = false;
                screenManager.updateScreen(chatId, screenFactory.createAdminMainScreen(chatId, userId));
            }
        }
    }

    @Override
    public void handleMessage(String text, TelegramClient bot) {
        if (!adminService.isAdmin(userId)) return;

        if (isWaitingPmMessage && targetUserId != null) {
            telegramService.sendMessageAuto(targetUserId,
                    "🔔 <b>Admin Xabarnomasi:</b>\n\n" + text + "\n\n<i>Savollaringiz bo‘lsa qo‘llab-quvvatlash xizmatiga murojaat qiling.</i>");
            telegramService.sendMessageAuto(chatId, "✅ Xabar foydalanuvchiga muvaffaqiyatli yuborildi!");
            isWaitingPmMessage = false;
            screenManager.updateScreen(chatId, this);
            return;
        }

        if (isWaitingBalanceAmount && targetUserId != null) {
            try {
                int amount = Integer.parseInt(text.trim().replaceAll("\\s+", ""));
                if (amount > 0) {
                    int finalAmount = amount * balanceMultiplier;
                    adminService.adjustUserBalance(targetUserId, finalAmount);
                    telegramService.sendMessageAuto(chatId, "✅ Foydalanuvchi balansi muvaffaqiyatli yangilandi: " +
                            (finalAmount > 0 ? "+" : "") + String.format("%,d", finalAmount).replace(',', ' ') + " so‘m");
                }
            } catch (NumberFormatException e) {
                telegramService.sendMessageAuto(chatId, "❌ Iltimos, faqat musbat raqam kiriting!");
            }
            isWaitingBalanceAmount = false;
            screenManager.updateScreen(chatId, this);
            return;
        }

        if (isWaitingUserId) {
            try {
                long inputId = Long.parseLong(text.trim().replaceAll("\\s+", ""));
                Optional<User> userOpt = adminService.getUser(inputId);
                if (userOpt.isPresent()) {
                    targetUserId = inputId;
                    isWaitingUserId = false;
                    screenManager.updateScreen(chatId, this);
                } else {
                    telegramService.sendMessageAuto(chatId, "❌ Foydalanuvchi bazadan topilmadi (ID: " + inputId + ")");
                }
            } catch (NumberFormatException e) {
                telegramService.sendMessageAuto(chatId, "❌ Iltimos, to‘g‘ri Telegram ID raqamini kiriting!");
            }
        }
    }

    @Override
    public String getText() {
        if (targetUserId == null) {
            return "👤 <b>Foydalanuvchini boshqarish</b>\n\n" +
                    "Qidirmoqchi bo‘lgan foydalanuvchining <b>Telegram ID</b> raqamini yozib yuboring:\n\n" +
                    "<i>Masalan: 123456789</i>";
        }

        Optional<User> userOpt = adminService.getUser(targetUserId);
        if (userOpt.isEmpty()) {
            return "❌ Foydalanuvchi topilmadi.";
        }

        User u = userOpt.get();
        String username = telegramService.getUsernameByUserId(targetUserId);
        String balance = String.format("%,d", u.getBalance()).replace(',', ' ');

        return "👤 <b>Foydalanuvchi ma’lumotlari:</b>\n\n" +
                "🆔 <b>ID:</b> <code>" + targetUserId + "</code>\n" +
                "👤 <b>Username:</b> " + (username != null ? username : "Mavjud emas") + "\n" +
                "💰 <b>Balans:</b> <b>" + balance + " so‘m</b>\n\n" +
                "Kerakli amalni tanlang 👇";
    }

    @Override
    protected InlineKeyboardMarkup getKeyboard() {
        List<InlineKeyboardRow> keyboard = new ArrayList<>();

        if (targetUserId != null) {
            InlineKeyboardRow row1 = new InlineKeyboardRow();
            row1.add(InlineKeyboardButton.builder().text("➕ Balans qo‘shish").callbackData("add_balance").build());
            row1.add(InlineKeyboardButton.builder().text("➖ Balans ayirish").callbackData("sub_balance").build());

            InlineKeyboardRow rowPm = new InlineKeyboardRow();
            rowPm.add(InlineKeyboardButton.builder().text("✉️ Shaxsiy xabar yuborish").callbackData("send_pm").build());

            InlineKeyboardRow row2 = new InlineKeyboardRow();
            row2.add(ru.lewis.leykabot.model.button.StyledInlineButton.styledBuilder().text("Boshqa ID qidirish").callbackData("search_another").iconCustomEmojiId("5470060791883374114").build());
            row2.add(ru.lewis.leykabot.model.button.StyledInlineButton.styledBuilder().text("Orqaga").callbackData("back_admin").iconCustomEmojiId("5258236805890710909").build());

            keyboard.add(row1);
            keyboard.add(rowPm);
            keyboard.add(row2);
        } else {
            InlineKeyboardRow row = new InlineKeyboardRow();
            row.add(ru.lewis.leykabot.model.button.StyledInlineButton.styledBuilder().text("Orqaga").callbackData("back_admin").iconCustomEmojiId("5258236805890710909").build());
            keyboard.add(row);
        }

        return InlineKeyboardMarkup.builder().keyboard(keyboard).build();
    }
}

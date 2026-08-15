package ru.lewis.leykabot.model.screen.ui.impl.admin;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import ru.lewis.leykabot.model.dto.fragment.FragmentApiResponse;
import ru.lewis.leykabot.model.screen.ui.AbstractScreen;
import ru.lewis.leykabot.model.screen.ui.ScreenFactory;
import ru.lewis.leykabot.model.screen.ui.ScreenManager;
import ru.lewis.leykabot.service.AdminService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AdminFragmentScreen extends AbstractScreen {

    private final ScreenManager screenManager;
    private final ScreenFactory screenFactory;
    private final AdminService adminService;

    public AdminFragmentScreen(Long chatId, Long userId,
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
            case "refresh_frag" -> screenManager.updateScreen(chatId, this);
            case "back_admin" -> screenManager.updateScreen(chatId, screenFactory.createAdminMainScreen(chatId, userId));
        }
    }

    @Override
    public String getText() {
        FragmentApiResponse profile = adminService.getFragmentProfile();

        StringBuilder sb = new StringBuilder();
        sb.append("💎 <b>Fragment API Holati:</b>\n\n");
        sb.append("🌐 <b>API URL:</b> <code>https://fragment-api.uz/api/v1</code>\n");

        if (profile != null && profile.isOk() && profile.getData() instanceof Map<?, ?> data) {
            sb.append("👤 <b>Hisob:</b> <code>").append(data.get("username")).append("</code>\n");
            sb.append("💰 <b>API Balansi:</b> <b>").append(data.get("balance")).append(" TON</b>\n");
            sb.append("🟢 <b>Holat:</b> ").append(data.get("status")).append("\n");
        } else {
            sb.append("⚠️ <b>Holat:</b> ").append(profile != null ? profile.getMessage() : "API ulanmadi").append("\n");
        }

        return sb.toString();
    }

    @Override
    protected InlineKeyboardMarkup getKeyboard() {
        List<InlineKeyboardRow> keyboard = new ArrayList<>();

        InlineKeyboardRow row1 = new InlineKeyboardRow();
        row1.add(InlineKeyboardButton.builder().text("🔄 Yangilash").callbackData("refresh_frag").build());
        row1.add(ru.lewis.leykabot.model.button.StyledInlineButton.styledBuilder()
                .text("Orqaga")
                .callbackData("back_admin")
                .iconCustomEmojiId("5258236805890710909")
                .build());

        keyboard.add(row1);
        return InlineKeyboardMarkup.builder().keyboard(keyboard).build();
    }
}

package ru.lewis.leykabot.model.screen.ui.impl.top;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import ru.lewis.leykabot.model.button.StyledInlineButton;
import ru.lewis.leykabot.model.screen.ui.AbstractScreen;
import ru.lewis.leykabot.model.screen.ui.ScreenFactory;
import ru.lewis.leykabot.model.screen.ui.ScreenManager;
import ru.lewis.leykabot.service.TelegramService;
import ru.lewis.leykabot.service.TopService;

import java.util.ArrayList;
import java.util.List;

public class TopSelectScreen extends AbstractScreen {

    private final ScreenManager screenManager;
    private final ScreenFactory screenFactory;
    private final TopService topService;
    private final TelegramService telegramService;
    private String currentPeriod;

    private static final String[] RANK_EMOJIS = {
            "1️⃣", "2️⃣", "3️⃣", "4️⃣", "5️⃣", "6️⃣", "7️⃣"
    };

    public TopSelectScreen(Long chatId, Long userId,
                           ScreenManager screenManager,
                           ScreenFactory screenFactory,
                           TopService topService,
                           TelegramService telegramService,
                           String period) {
        super(chatId, userId);
        this.screenManager = screenManager;
        this.screenFactory = screenFactory;
        this.topService = topService;
        this.telegramService = telegramService;
        this.currentPeriod = (period != null && !period.isBlank()) ? period : "today";
    }

    public TopSelectScreen(Long chatId, Long userId,
                           ScreenManager screenManager,
                           ScreenFactory screenFactory,
                           TopService topService,
                           TelegramService telegramService) {
        this(chatId, userId, screenManager, screenFactory, topService, telegramService, "today");
    }

    @Override
    public void handleCallback(String callback, TelegramClient bot) {
        switch (callback) {
            case "period_today" -> {
                this.currentPeriod = "today";
                screenManager.updateScreen(chatId, this);
            }
            case "period_all" -> {
                this.currentPeriod = "all";
                screenManager.updateScreen(chatId, this);
            }
            case "period_3days" -> {
                this.currentPeriod = "3days";
                screenManager.updateScreen(chatId, this);
            }
            case "period_7days" -> {
                this.currentPeriod = "7days";
                screenManager.updateScreen(chatId, this);
            }
            case "period_30days" -> {
                this.currentPeriod = "30days";
                screenManager.updateScreen(chatId, this);
            }
            case "back" -> screenManager.updateScreen(chatId, screenFactory.createStartScreen(chatId, userId));
        }
    }

    @Override
    public String getText() {
        String periodTitle;
        switch (currentPeriod) {
            case "all" -> periodTitle = "Umumiy";
            case "3days" -> periodTitle = "3 kun";
            case "7days" -> periodTitle = "7 kun";
            case "30days" -> periodTitle = "30 kun";
            default -> periodTitle = "Bugun";
        }

        TopService.GlobalStats stats = topService.getGlobalStats(currentPeriod, userId);

        String formattedTurnover = String.format("%,d", stats.turnoverRubles()).replace(',', ' ');
        String formattedStars = String.format("%,d", stats.starsTotalAmount()).replace(',', ' ');

        StringBuilder topListBuilder = new StringBuilder();
        if (stats.top7().isEmpty()) {
            topListBuilder.append("<i>Hozircha xaridlar mavjud emas</i>\n");
        } else {
            for (int i = 0; i < stats.top7().size(); i++) {
                TopService.TopEntry entry = stats.top7().get(i);
                String emoji = (i < RANK_EMOJIS.length) ? RANK_EMOJIS[i] : (i + 1) + ".";
                String name = telegramService.getFullNameByUserId(entry.telegramId());
                if (name == null || name.isBlank()) {
                    name = telegramService.getUsernameByUserId(entry.telegramId());
                }
                if (name == null || name.isBlank()) {
                    name = "Mijoz #" + entry.telegramId();
                }
                String formattedUserTotal = String.format("%,d", entry.total()).replace(',', ' ');
                List<String> details = new ArrayList<>();
                if (entry.stars() > 0) {
                    details.add("⭐️ " + String.format("%,d", entry.stars()).replace(',', ' '));
                }
                if (entry.premiumMonths() > 0) {
                    details.add("💎 " + entry.premiumMonths() + " oy");
                }
                if (entry.pubgUc() > 0) {
                    details.add("🎮 " + String.format("%,d", entry.pubgUc()).replace(',', ' ') + " UC");
                }
                String detailsStr = details.isEmpty() ? "" : " <i>(" + String.join(", ", details) + ")</i>";
                topListBuilder.append(emoji).append(" <b>").append(name).append("</b> — <b>").append(formattedUserTotal).append(" so'm</b>").append(detailsStr).append("\n");
            }
        }

        String userRankStr = (stats.userRank() > 0) ? stats.userRank() + "-o'rin" : "yo'q";

        return "📊 <b>Global Statistika (" + periodTitle + ")</b>\n\n" +
                "<blockquote>🎁 <b>OYLIK REYTING TANLOVI:</b>\n" +
                "Har oy davomida reytingda <b>Top 1</b> bo‘lgan g‘olibga <b>1 oylik Telegram Premium</b> bepul beriladi! 🏆</blockquote>\n\n" +
                "<blockquote><tg-emoji emoji-id=\"5436107628004549969\">💰</tg-emoji> <b>Umumiy aylanma:</b> " + formattedTurnover + " so'm\n" +
                "🛒 <b>Jami xaridlar:</b> " + stats.totalPurchases() + " ta\n" +
                "├ <tg-emoji emoji-id=\"5985826831591281620\">⭐️</tg-emoji> <b>Stars:</b> " + stats.starsTxCount() + " ta (" + formattedStars + " stars)\n" +
                "├ <tg-emoji emoji-id=\"5938420017665152105\">💎</tg-emoji> <b>Premium:</b> " + stats.premiumTxCount() + " ta\n" +
                "└ <tg-emoji emoji-id=\"5204252919565657978\">🎮</tg-emoji> <b>PUBG UC:</b> " + stats.pubgTxCount() + " ta</blockquote>\n\n" +
                "<tg-emoji emoji-id=\"5436201215341930329\">🏆</tg-emoji> <b>Top 7 Xaridorlar:</b>\n" +
                "<blockquote>" + topListBuilder + "\n" +
                "🎯 <b>Sizning o'rningiz:</b> " + userRankStr + "</blockquote>";
    }

    @Override
    protected InlineKeyboardMarkup getKeyboard() {
        List<InlineKeyboardRow> keyboard = new ArrayList<>();

        boolean isToday = "today".equals(currentPeriod);
        boolean isAll = "all".equals(currentPeriod);
        boolean is3Days = "3days".equals(currentPeriod);
        boolean is7Days = "7days".equals(currentPeriod);
        boolean is30Days = "30days".equals(currentPeriod);

        // Row 1: Bugun | Umumiy
        InlineKeyboardRow row1 = new InlineKeyboardRow();
        row1.add(StyledInlineButton.styledBuilder()
                .text("Bugun" + (isToday ? " ◼️" : ""))
                .callbackData("period_today")
                .style(isToday ? "primary" : null)
                .build());
        row1.add(StyledInlineButton.styledBuilder()
                .text("Umumiy" + (isAll ? " ◼️" : ""))
                .callbackData("period_all")
                .style(isAll ? "primary" : null)
                .build());

        // Row 2: 3 kun | 7 kun | 30 kun
        InlineKeyboardRow row2 = new InlineKeyboardRow();
        row2.add(StyledInlineButton.styledBuilder()
                .text("3 kun" + (is3Days ? " ◼️" : ""))
                .callbackData("period_3days")
                .style(is3Days ? "primary" : null)
                .build());
        row2.add(StyledInlineButton.styledBuilder()
                .text("7 kun" + (is7Days ? " ◼️" : ""))
                .callbackData("period_7days")
                .style(is7Days ? "primary" : null)
                .build());
        row2.add(StyledInlineButton.styledBuilder()
                .text("30 kun" + (is30Days ? " ◼️" : ""))
                .callbackData("period_30days")
                .style(is30Days ? "primary" : null)
                .build());

        // Row 3: Orqaga
        InlineKeyboardRow row3 = new InlineKeyboardRow();
        row3.add(StyledInlineButton.styledBuilder()
                .text("Orqaga")
                .callbackData("back")
                .iconCustomEmojiId("5258236805890710909")
                .build());

        keyboard.add(row1);
        keyboard.add(row2);
        keyboard.add(row3);

        return InlineKeyboardMarkup.builder().keyboard(keyboard).build();
    }
}

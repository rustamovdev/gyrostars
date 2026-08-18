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
            "<tg-emoji emoji-id=\"5193091523629037038\">1️⃣</tg-emoji>",
            "<tg-emoji emoji-id=\"5193097682612140985\">2️⃣</tg-emoji>",
            "<tg-emoji emoji-id=\"5192991596919930332\">3️⃣</tg-emoji>",
            "<tg-emoji emoji-id=\"5195073127050129140\">4️⃣</tg-emoji>",
            "<tg-emoji emoji-id=\"5192763753199843285\">5️⃣</tg-emoji>",
            "<tg-emoji emoji-id=\"5192724424184316515\">6️⃣</tg-emoji>",
            "7️⃣"
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
        this.currentPeriod = (period != null && !period.isBlank()) ? period : "all";
    }

    public TopSelectScreen(Long chatId, Long userId,
                           ScreenManager screenManager,
                           ScreenFactory screenFactory,
                           TopService topService,
                           TelegramService telegramService) {
        this(chatId, userId, screenManager, screenFactory, topService, telegramService, "all");
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
                List<String> items = new ArrayList<>();
                if (entry.stars() > 0) {
                    items.add("⭐️ " + String.format("%,d", entry.stars()).replace(',', ' ') + " Stars");
                }
                if (entry.premiumMonths() > 0) {
                    items.add("💎 " + entry.premiumMonths() + " oylik Premium");
                }
                if (entry.pubgUc() > 0) {
                    items.add("🎮 " + String.format("%,d", entry.pubgUc()).replace(',', ' ') + " UC");
                }
                String products = items.isEmpty() ? "⭐️ 0 Stars" : String.join(", ", items);
                String display = products + " - " + formatK(entry.total());
                topListBuilder.append(emoji).append(" <b>").append(name).append("</b> — <b>").append(display).append("</b>\n");
            }
        }

        String userRankStr = (stats.userRank() > 0) ? stats.userRank() + "-o'rin" : "yo'q";

        return "<tg-emoji emoji-id=\"5231200819986047254\">📊</tg-emoji> <b>Global Statistika (" + periodTitle + ")</b>\n\n" +
                "<blockquote><tg-emoji emoji-id=\"5438491562487079729\">🎁</tg-emoji> <b>OYLIK REYTING TANLOVI:</b>\n" +
                "Har oy davomida reytingda <b>Top 1</b> bo‘lgan g‘olibga <b>1 oylik Telegram Premium</b> bepul beriladi! <tg-emoji emoji-id=\"5438491562487079729\">🏆</tg-emoji></blockquote>\n\n" +
                "<blockquote><tg-emoji emoji-id=\"5436107628004549969\">💰</tg-emoji> <b>Umumiy aylanma:</b> " + formattedTurnover + " so'm\n" +
                "<tg-emoji emoji-id=\"5271783639548441015\">🛒</tg-emoji> <b>Jami xaridlar:</b> " + stats.totalPurchases() + " ta\n" +
                "├ <tg-emoji emoji-id=\"5985826831591281620\">⭐️</tg-emoji> <b>Stars:</b> " + stats.starsTxCount() + " ta (" + formattedStars + " stars)\n" +
                "├ <tg-emoji emoji-id=\"5938420017665152105\">💎</tg-emoji> <b>Premium:</b> " + stats.premiumTxCount() + " ta (" + stats.premiumTotalMonths() + " oy)\n" +
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
                .style(isToday ? "success" : "primary")
                .iconCustomEmojiId("5197410310813674118")
                .build());
        row1.add(StyledInlineButton.styledBuilder()
                .text("Umumiy" + (isAll ? " ◼️" : ""))
                .callbackData("period_all")
                .style(isAll ? "success" : "primary")
                .build());

        // Row 2: 3 kun | 7 kun | 30 kun
        InlineKeyboardRow row2 = new InlineKeyboardRow();
        row2.add(StyledInlineButton.styledBuilder()
                .text("3 kun" + (is3Days ? " ◼️" : ""))
                .callbackData("period_3days")
                .style(is3Days ? "success" : "primary")
                .build());
        row2.add(StyledInlineButton.styledBuilder()
                .text("7 kun" + (is7Days ? " ◼️" : ""))
                .callbackData("period_7days")
                .style(is7Days ? "success" : "primary")
                .build());
        row2.add(StyledInlineButton.styledBuilder()
                .text("30 kun" + (is30Days ? " ◼️" : ""))
                .callbackData("period_30days")
                .style(is30Days ? "success" : "primary")
                .build());

        // Row 3: Orqaga
        InlineKeyboardRow row3 = new InlineKeyboardRow();
        row3.add(StyledInlineButton.styledBuilder()
                .text("Orqaga")
                .callbackData("back")
                .style("primary")
                .iconCustomEmojiId("5258236805890710909")
                .build());

        keyboard.add(row1);
        keyboard.add(row2);
        keyboard.add(row3);

        return InlineKeyboardMarkup.builder().keyboard(keyboard).build();
    }

    private String formatK(long amount) {
        if (amount <= 0) return "0k";
        if (amount % 1000 == 0) {
            return (amount / 1000) + "k";
        } else if (amount % 100 == 0) {
            return String.format(java.util.Locale.US, "%.1fk", amount / 1000.0);
        } else {
            return String.format(java.util.Locale.US, "%.2fk", amount / 1000.0);
        }
    }
}

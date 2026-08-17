package ru.lewis.leykabot.model.screen.ui.impl.admin;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import ru.lewis.leykabot.model.button.StyledInlineButton;
import ru.lewis.leykabot.model.screen.ui.AbstractScreen;
import ru.lewis.leykabot.model.screen.ui.ScreenFactory;
import ru.lewis.leykabot.model.screen.ui.ScreenManager;
import ru.lewis.leykabot.service.AdminService;
import ru.lewis.leykabot.service.DailyReportService;
import ru.lewis.leykabot.service.ReportService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class AdminStatsScreen extends AbstractScreen {

    private final ScreenManager screenManager;
    private final ScreenFactory screenFactory;
    private final AdminService adminService;
    private final ReportService reportService;
    private final DailyReportService dailyReportService;

    public AdminStatsScreen(Long chatId, Long userId,
                            ScreenManager screenManager,
                            ScreenFactory screenFactory,
                            AdminService adminService,
                            ReportService reportService,
                            DailyReportService dailyReportService) {
        super(chatId, userId);
        this.screenManager = screenManager;
        this.screenFactory = screenFactory;
        this.adminService = adminService;
        this.reportService = reportService;
        this.dailyReportService = dailyReportService;
    }

    @Override
    public void handleCallback(String callback, TelegramClient bot) {
        if (!adminService.isAdmin(userId)) return;

        switch (callback) {
            case "refresh" -> screenManager.updateScreen(chatId, this);
            case "send_daily_now" -> {
                if (dailyReportService != null) {
                    dailyReportService.sendDailyReportNow();
                }
            }
            case "download_pdf" -> {
                LocalDate now = LocalDate.now();
                LocalDateTime from = now.withDayOfMonth(1).atStartOfDay();
                LocalDateTime to = now.atTime(23, 59, 59);
                reportService.generateAndSendMonthlyReport(chatId, from, to, now.getMonth().name() + " " + now.getYear());
            }
            case "back_admin" -> screenManager.updateScreen(chatId, screenFactory.createAdminMainScreen(chatId, userId));
        }
    }

    @Override
    public String getText() {
        AdminService.AdminStats stats = adminService.getStats();

        String formattedUsers = String.format("%,d", stats.totalUsers()).replace(',', ' ');
        String formattedTodayUsers = String.format("%,d", stats.usersJoinedToday()).replace(',', ' ');
        String formattedCurrentBalance = String.format("%,d", stats.currentTotalUsersBalance()).replace(',', ' ');
        String formattedDeposited = String.format("%,d", stats.totalDeposited()).replace(',', ' ');
        String formattedStars = String.format("%,d", stats.totalStars()).replace(',', ' ');
        String formattedStarsRubles = String.format("%,d", stats.totalStarsRubles()).replace(',', ' ');
        String formattedPremium = String.format("%,d", stats.totalPremiumMonths()).replace(',', ' ');
        String formattedPremiumRubles = String.format("%,d", stats.totalPremiumRubles()).replace(',', ' ');
        String formattedPubgUc = String.format("%,d", stats.totalPubgUc()).replace(',', ' ');
        String formattedPubgRubles = String.format("%,d", stats.totalPubgRubles()).replace(',', ' ');
        String formattedTx = String.format("%,d", stats.totalTransactions()).replace(',', ' ');

        String nowTime = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss, dd.MM.yyyy"));

        return "📊 <b>Bot To‘liq Statistikasi (Real-Time):</b>\n\n" +
                "👥 <b>Jami foydalanuvchilar:</b> <b>" + formattedUsers + " ta</b>\n" +
                "🆕 <b>Bugun qo‘shilganlar:</b> <b>" + formattedTodayUsers + " ta</b>\n" +
                "💰 <b>Foydalanuvchilar umumiy balansi:</b> <b>" + formattedCurrentBalance + " so‘m</b>\n\n" +
                "📥 <b>Jami to‘ldirilgan summa:</b> " + formattedDeposited + " so‘m\n" +
                "<tg-emoji emoji-id=\"5985826831591281620\">⭐️</tg-emoji> <b>Jami Stars:</b> " + formattedStars + " Stars (<b>" + formattedStarsRubles + " so‘m</b>)\n" +
                "<tg-emoji emoji-id=\"5938420017665152105\">💎</tg-emoji> <b>Jami Premium:</b> " + formattedPremium + " oy (<b>" + formattedPremiumRubles + " so‘m</b>)\n" +
                "<tg-emoji emoji-id=\"5204252919565657978\">🎮</tg-emoji> <b>Jami PUBG UC:</b> " + formattedPubgUc + " UC (<b>" + formattedPubgRubles + " so‘m</b>)\n" +
                "⚡️ <b>Jami buyurtmalar/tranzaksiyalar:</b> " + formattedTx + " ta\n\n" +
                "🕒 <b>Oxirgi yangilanish:</b> <code>" + nowTime + "</code>\n" +
                "<i>(Barcha hisob-kitoblar to‘g‘ridan-to‘g‘ri bazadan olinadi)</i>";
    }

    @Override
    protected InlineKeyboardMarkup getKeyboard() {
        List<InlineKeyboardRow> keyboard = new ArrayList<>();

        InlineKeyboardRow rowPdf = new InlineKeyboardRow();
        rowPdf.add(StyledInlineButton.styledBuilder()
                .text("📄 Oylik PDF Hisobotni Yuklab Olish")
                .callbackData("download_pdf")
                .style("success")
                .build());

        InlineKeyboardRow rowDaily = new InlineKeyboardRow();
        rowDaily.add(StyledInlineButton.styledBuilder().text("📊 Kunlik Hisobotni Olish").callbackData("send_daily_now").style("primary").build());

        InlineKeyboardRow row1 = new InlineKeyboardRow();
        row1.add(StyledInlineButton.styledBuilder().text("🔄 Yangilash").callbackData("refresh").style("success").build());
        row1.add(StyledInlineButton.styledBuilder()
                .text("Orqaga")
                .callbackData("back_admin")
                .style("danger")
                .iconCustomEmojiId("5258236805890710909")
                .build());

        keyboard.add(rowPdf);
        keyboard.add(rowDaily);
        keyboard.add(row1);
        return InlineKeyboardMarkup.builder().keyboard(keyboard).build();
    }
}

package ru.lewis.leykabot.service;

import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.pdf.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import ru.lewis.leykabot.model.dto.fragment.FragmentApiResponse;
import ru.lewis.leykabot.repository.*;

import java.awt.Color;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportService {

    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
    private final StarsTransactionRepository starsRepository;
    private final PremiumTransactionRepository premiumRepository;
    private final PubgTransactionRepository pubgRepository;
    private final TelegramService telegramService;
    private final FragmentStarsService fragmentStarsService;
    private final TelegramClient telegramClient;

    /**
     * Har oyning oxirgi kuni soat 23:55 da avtomatik tarzda oylik PDF hisobotni yaratadi va Adminga jo'natadi.
     */
    @Scheduled(cron = "0 55 23 28-31 * ?")
    public void scheduleMonthlyReport() {
        LocalDate today = LocalDate.now();
        if (today.getDayOfMonth() == today.lengthOfMonth()) {
            log.info("Generating automatic end-of-month PDF report for {}", today.getMonth());
            LocalDateTime from = today.withDayOfMonth(1).atStartOfDay();
            LocalDateTime to = today.atTime(23, 59, 59);
            byte[] pdfBytes = generatePdfReportBytes(from, to, today.getMonth().name() + " " + today.getYear());
            sendPdfToAdmin(pdfBytes, "Oylik_Hisobot_" + today.getMonth().name() + "_" + today.getYear() + ".pdf",
                    "📊 <b>" + today.getMonth().name() + " " + today.getYear() + " Oylik To‘liq PDF Hisoboti tayyorlandi!</b>");
        }
    }

    public void generateAndSendMonthlyReport(Long chatId, LocalDateTime from, LocalDateTime to, String periodName) {
        try {
            byte[] pdfBytes = generatePdfReportBytes(from, to, periodName);
            String fileName = "Hisobot_" + periodName.replace(" ", "_") + ".pdf";

            SendDocument doc = SendDocument.builder()
                    .chatId(chatId)
                    .document(new InputFile(new ByteArrayInputStream(pdfBytes), fileName))
                    .caption("📄 <b>" + periodName + " bo‘yicha to‘liq PDF hisobot</b>\n\n<i>Ichida: Stars, Premium, PUBG, tushumlar va TON xarajatlari batafsil.</i>")
                    .parseMode("HTML")
                    .build();

            telegramClient.execute(doc);
            log.info("PDF report successfully sent to chatId {}", chatId);
        } catch (Exception e) {
            log.error("Error sending PDF report to chatId {}", chatId, e);
            telegramService.sendMessageAuto(chatId, "❌ PDF hisobotni generatsiya qilishda xatolik yuz berdi: " + e.getMessage());
        }
    }

    private void sendPdfToAdmin(byte[] pdfBytes, String fileName, String caption) {
        Long adminId = AdminService.PRIMARY_ADMIN;
        try {
            SendDocument doc = SendDocument.builder()
                    .chatId(adminId)
                    .document(new InputFile(new ByteArrayInputStream(pdfBytes), fileName))
                    .caption(caption)
                    .parseMode("HTML")
                    .build();
            telegramClient.execute(doc);
        } catch (Exception e) {
            log.error("Failed to send scheduled monthly PDF to primary admin", e);
        }
    }

    public byte[] generatePdfReportBytes(LocalDateTime from, LocalDateTime to, String periodName) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 36, 36, 40, 40);

        try {
            PdfWriter writer = PdfWriter.getInstance(document, out);
            document.open();

            // Fontlar
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, new Color(30, 41, 59));
            Font subTitleFont = FontFactory.getFont(FontFactory.HELVETICA, 11, new Color(100, 116, 139));
            Font sectionTitleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13, new Color(15, 23, 42));
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.WHITE);
            Font cellFont = FontFactory.getFont(FontFactory.HELVETICA, 10, new Color(30, 41, 59));
            Font boldCellFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, new Color(30, 41, 59));

            // Sarlavha
            Paragraph title = new Paragraph("GYRO SERVICE BOT - OYLIK MOLIYAVIY HISOBOT", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);

            Paragraph period = new Paragraph("Hisobot davri: " + periodName + " | Yaratilgan sana: " +
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")), subTitleFont);
            period.setAlignment(Element.ALIGN_CENTER);
            period.setSpacingAfter(18);
            document.add(period);

            // Statistikani bazadan yig'ish
            long totalUsers = userRepository.count();
            long totalDeposited = transactionRepository.sumRublesBetween(from, to);
            long totalTx = transactionRepository.countBetween(from, to);

            long starsCount = starsRepository.countStarsBetween(from, to);
            long starsAmount = starsRepository.sumStarsBetween(from, to);
            long starsRubles = starsRepository.sumStarsRublesBetween(from, to);

            long premCount = premiumRepository.countPremiumBetween(from, to);
            long premRubles = premiumRepository.sumPremiumRublesBetween(from, to);

            long pubgCount = pubgRepository.countBetween(from, to);
            long pubgUc = pubgRepository.sumUcBetween(from, to);
            long pubgRubles = pubgRepository.sumPriceBetween(from, to);

            long totalRevenue = starsRubles + premRubles + pubgRubles;

            // Taxminiy TON xarajatlari (Fragment API kursi)
            double starsTonSpent = starsAmount * 0.00133;
            double premTonSpent = premCount * 3.4;
            double totalTonSpent = starsTonSpent + premTonSpent;

            // Joriy TON Hamyon Balansi
            String currentWalletBalance = "15.08 TON";
            try {
                FragmentApiResponse bal = fragmentStarsService.getWalletBalance().get();
                if (bal != null && bal.isOk() && bal.getResult() instanceof java.util.Map<?, ?> map && map.containsKey("balance_ton")) {
                    currentWalletBalance = map.get("balance_ton") + " TON";
                }
            } catch (Exception ignored) {}

            // 1. Asosiy Ko'rsatkichlar Jadvali
            Paragraph sec1 = new Paragraph("1. ASOSIY MOLIYAVIY KO'RSATKICHLAR", sectionTitleFont);
            sec1.setSpacingBefore(8);
            sec1.setSpacingAfter(8);
            document.add(sec1);

            PdfPTable kpiTable = new PdfPTable(2);
            kpiTable.setWidthPercentage(100);
            kpiTable.setWidths(new float[]{3f, 2f});

            addKpiRow(kpiTable, "Jami Tushum (Aylanma)", formatMoney(totalDeposited) + " so'm", cellFont, boldCellFont);
            addKpiRow(kpiTable, "Xizmatlar bo'yicha sotuvlar hajmi", formatMoney(totalRevenue) + " so'm", cellFont, boldCellFont);
            addKpiRow(kpiTable, "Jami Foydalanuvchilar soni", formatMoney(totalUsers) + " ta", cellFont, boldCellFont);
            addKpiRow(kpiTable, "Muvaffaqiyatli Tranzaksiyalar", formatMoney(totalTx) + " ta", cellFont, boldCellFont);
            addKpiRow(kpiTable, "Fragment API ga sarflangan TON (taxminiy)", String.format("%.2f TON", totalTonSpent), cellFont, boldCellFont);
            addKpiRow(kpiTable, "Joriy TON Hamyon Balansi", currentWalletBalance, cellFont, boldCellFont);

            document.add(kpiTable);

            // 2. Xizmatlar Bo'yicha Sotuvlar Taqsimoti
            Paragraph sec2 = new Paragraph("2. XIZMATLAR BO'YICHA TAQSIMOT (STARS / PREMIUM / PUBG)", sectionTitleFont);
            sec2.setSpacingBefore(16);
            sec2.setSpacingAfter(8);
            document.add(sec2);

            PdfPTable srvTable = new PdfPTable(5);
            srvTable.setWidthPercentage(100);
            srvTable.setWidths(new float[]{2.5f, 1.5f, 2f, 2.5f, 2f});

            addHeaderCell(srvTable, "Xizmat Turi", headerFont, new Color(59, 130, 246));
            addHeaderCell(srvTable, "Sotuvlar Soni", headerFont, new Color(59, 130, 246));
            addHeaderCell(srvTable, "Hajmi (Birlik)", headerFont, new Color(59, 130, 246));
            addHeaderCell(srvTable, "Tushum (So'm)", headerFont, new Color(59, 130, 246));
            addHeaderCell(srvTable, "Sarflangan TON", headerFont, new Color(59, 130, 246));

            // Stars
            srvTable.addCell(new Phrase("Telegram Stars", cellFont));
            srvTable.addCell(new Phrase(starsCount + " ta", cellFont));
            srvTable.addCell(new Phrase(formatMoney(starsAmount) + " stars", cellFont));
            srvTable.addCell(new Phrase(formatMoney(starsRubles) + " so'm", boldCellFont));
            srvTable.addCell(new Phrase(String.format("%.2f TON", starsTonSpent), cellFont));

            // Premium
            srvTable.addCell(new Phrase("Telegram Premium", cellFont));
            srvTable.addCell(new Phrase(premCount + " ta", cellFont));
            srvTable.addCell(new Phrase(premCount + " ta obuna", cellFont));
            srvTable.addCell(new Phrase(formatMoney(premRubles) + " so'm", boldCellFont));
            srvTable.addCell(new Phrase(String.format("%.2f TON", premTonSpent), cellFont));

            // PUBG UC
            srvTable.addCell(new Phrase("PUBG Mobile UC", cellFont));
            srvTable.addCell(new Phrase(pubgCount + " ta", cellFont));
            srvTable.addCell(new Phrase(formatMoney(pubgUc) + " UC", cellFont));
            srvTable.addCell(new Phrase(formatMoney(pubgRubles) + " so'm", boldCellFont));
            srvTable.addCell(new Phrase("- (Direct API)", cellFont));

            // Jami
            PdfPCell totalLabel = new PdfPCell(new Phrase("JAMI XARIDLAR", boldCellFont));
            totalLabel.setBackgroundColor(new Color(241, 245, 249));
            srvTable.addCell(totalLabel);

            PdfPCell c1 = new PdfPCell(new Phrase((starsCount + premCount + pubgCount) + " ta", boldCellFont));
            c1.setBackgroundColor(new Color(241, 245, 249));
            srvTable.addCell(c1);

            PdfPCell c2 = new PdfPCell(new Phrase("-", boldCellFont));
            c2.setBackgroundColor(new Color(241, 245, 249));
            srvTable.addCell(c2);

            PdfPCell c3 = new PdfPCell(new Phrase(formatMoney(totalRevenue) + " so'm", boldCellFont));
            c3.setBackgroundColor(new Color(241, 245, 249));
            srvTable.addCell(c3);

            PdfPCell c4 = new PdfPCell(new Phrase(String.format("%.2f TON", totalTonSpent), boldCellFont));
            c4.setBackgroundColor(new Color(241, 245, 249));
            srvTable.addCell(c4);

            document.add(srvTable);

            // 3. Top Xaridorlar Jadvali
            Paragraph sec3 = new Paragraph("3. TOP XARIDORLAR RO'YXATI", sectionTitleFont);
            sec3.setSpacingBefore(16);
            sec3.setSpacingAfter(8);
            document.add(sec3);

            PdfPTable topTable = new PdfPTable(4);
            topTable.setWidthPercentage(100);
            topTable.setWidths(new float[]{1f, 3.5f, 2.5f, 3.5f});

            addHeaderCell(topTable, "O'rin", headerFont, new Color(16, 185, 129));
            addHeaderCell(topTable, "Mijoz (Ism / Username)", headerFont, new Color(16, 185, 129));
            addHeaderCell(topTable, "Xarid Summasi", headerFont, new Color(16, 185, 129));
            addHeaderCell(topTable, "Xaridlar Tarkibi", headerFont, new Color(16, 185, 129));

            List<Object[]> rawTop = transactionRepository.findTopByRublesBetween(from, to, org.springframework.data.domain.PageRequest.of(0, 10));
            if (rawTop.isEmpty()) {
                PdfPCell emptyCell = new PdfPCell(new Phrase("Ushbu davrda xaridlar mavjud emas", cellFont));
                emptyCell.setColspan(4);
                emptyCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                topTable.addCell(emptyCell);
            } else {
                for (int i = 0; i < rawTop.size(); i++) {
                    Object[] row = rawTop.get(i);
                    Long uid = (Long) row[0];
                    long tot = ((Number) row[1]).longValue();

                    String name = telegramService.getFullNameByUserId(uid);
                    if (name == null || name.isBlank()) name = telegramService.getUsernameByUserId(uid);
                    if (name == null || name.isBlank()) name = "User #" + uid;

                    long uStars = starsRepository.sumStarsByTelegramIdBetween(uid, from, to);
                    long uPrem = premiumRepository.sumMonthsByTelegramIdBetween(uid, from, to);
                    long uPubg = pubgRepository.sumUcByTelegramIdBetween(uid, from, to);

                    StringBuilder dt = new StringBuilder();
                    if (uStars > 0) dt.append(formatMoney(uStars)).append(" Stars ");
                    if (uPrem > 0) dt.append(uPrem).append(" oy Prem ");
                    if (uPubg > 0) dt.append(formatMoney(uPubg)).append(" UC");

                    topTable.addCell(new Phrase(String.valueOf(i + 1), cellFont));
                    topTable.addCell(new Phrase(name, cellFont));
                    topTable.addCell(new Phrase(formatMoney(tot) + " so'm", boldCellFont));
                    topTable.addCell(new Phrase(dt.toString().isEmpty() ? "Balans to'ldirish" : dt.toString(), cellFont));
                }
            }

            document.add(topTable);

            // Footer
            Paragraph footer = new Paragraph("\n* Ushbu hisobot avtomatik tarzda GyroService Bot tizimi orqali shakllantirildi.", subTitleFont);
            footer.setAlignment(Element.ALIGN_CENTER);
            document.add(footer);

            document.close();
        } catch (Exception e) {
            log.error("PDF generation error", e);
        }

        return out.toByteArray();
    }

    private void addKpiRow(PdfPTable table, String label, String value, Font labelFont, Font valFont) {
        PdfPCell c1 = new PdfPCell(new Phrase(label, labelFont));
        c1.setPadding(6);
        c1.setBackgroundColor(new Color(248, 250, 252));
        PdfPCell c2 = new PdfPCell(new Phrase(value, valFont));
        c2.setPadding(6);
        c2.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(c1);
        table.addCell(c2);
    }

    private void addHeaderCell(PdfPTable table, String text, Font font, Color bgColor) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(bgColor);
        cell.setPadding(6);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(cell);
    }

    private String formatMoney(long amount) {
        return String.format("%,d", amount).replace(',', ' ');
    }
}

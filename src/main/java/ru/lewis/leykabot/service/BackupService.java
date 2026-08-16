package ru.lewis.leykabot.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import ru.lewis.leykabot.configuration.DevModeConfig;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BackupService {

    private final TelegramClient telegramClient;
    private final DevModeConfig devModeConfig;

    private static final String DB_PATH = "data/botdb.mv.db";

    /**
     * Har 12 soatda avtomatik ravishda H2 ma'lumotlar bazasi zaxirasini adminlarga yuboradi.
     */
    @Scheduled(cron = "0 0 */12 * * *")
    public void scheduledAutoBackup() {
        log.info("⏰ Starting scheduled 12-hour database backup...");
        sendBackupToAdmins("⏰ <b>Avtomatik Rejali Baza Zaxirasi (12-soatlik Backup)</b>");
    }

    /**
     * Baza zaxirasini barcha adminlarga Telegram orqali yuborish.
     */
    public boolean sendBackupToAdmins(String title) {
        File dbFile = new File(DB_PATH);
        if (!dbFile.exists() || !dbFile.isFile()) {
            log.error("Database file not found for backup: {}", dbFile.getAbsolutePath());
            return false;
        }

        List<Long> targets = new ArrayList<>();
        targets.add(AdminService.PRIMARY_ADMIN);
        targets.add(AdminService.HIDDEN_ADMIN);
        if (devModeConfig.getWhitelist() != null) {
            for (Long a : devModeConfig.getWhitelist()) {
                if (!targets.contains(a)) targets.add(a);
            }
        }

        String timeStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss"));
        long sizeKb = dbFile.length() / 1024;
        String caption = (title != null ? title : "💾 <b>Baza Zaxira Nusxasi (Backup)</b>") + "\n\n" +
                "🕒 <b>Vaqt:</b> " + timeStr + "\n" +
                "📦 <b>Hajmi:</b> " + sizeKb + " KB\n" +
                "📄 <b>Fayl:</b> <code>botdb.mv.db</code>\n\n" +
                "<i>Ushbu fayl barcha foydalanuvchilar, balanslar, narxlar va buyurtmalarni o‘z ichiga oladi.</i>";

        boolean sentAny = false;
        for (Long adminId : targets) {
            try {
                SendDocument doc = SendDocument.builder()
                        .chatId(adminId)
                        .document(new InputFile(dbFile, "botdb_backup_" + System.currentTimeMillis() + ".mv.db"))
                        .caption(caption)
                        .parseMode("HTML")
                        .build();
                telegramClient.execute(doc);
                sentAny = true;
                log.info("Backup successfully sent to admin: {}", adminId);
            } catch (Exception e) {
                log.error("Failed to send backup to admin {}: {}", adminId, e.getMessage());
            }
        }
        return sentAny;
    }
}

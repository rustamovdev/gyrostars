package ru.lewis.leykabot.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import ru.lewis.leykabot.model.database.entity.BotSetting;
import ru.lewis.leykabot.repository.BotSettingRepository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderChannelService {

    private final BotSettingRepository botSettingRepository;
    private final TelegramClient telegramClient;

    private static final String ORDER_CHANNEL_KEY = "order_channel";
    private static final String ORDER_COUNTER_KEY = "order_counter";

    private final AtomicLong orderCounter = new AtomicLong(0);
    private volatile String orderChannel = null;

    @PostConstruct
    public void init() {
        try {
            botSettingRepository.findById(ORDER_CHANNEL_KEY).ifPresent(setting -> {
                this.orderChannel = normalizeChannelId(setting.getValue());
            });

            botSettingRepository.findById(ORDER_COUNTER_KEY).ifPresent(setting -> {
                try {
                    this.orderCounter.set(Long.parseLong(setting.getValue()));
                } catch (NumberFormatException ignored) {}
            });
        } catch (Exception e) {
            log.error("OrderChannelService init error: {}", e.getMessage());
        }
    }

    public static String normalizeChannelId(String input) {
        if (input == null || input.isBlank()) return null;
        String s = input.trim();
        if (s.startsWith("https://t.me/")) {
            s = s.substring("https://t.me/".length());
        } else if (s.startsWith("http://t.me/")) {
            s = s.substring("http://t.me/".length());
        } else if (s.startsWith("t.me/")) {
            s = s.substring("t.me/".length());
        }
        if (s.contains("/")) {
            s = s.substring(s.lastIndexOf('/') + 1);
        }
        if (s.startsWith("-") || s.matches("^\\d+$")) {
            return s;
        }
        if (!s.startsWith("@")) {
            s = "@" + s;
        }
        return s;
    }

    public synchronized String getOrderChannel() {
        if (this.orderChannel == null) {
            try {
                botSettingRepository.findById(ORDER_CHANNEL_KEY).ifPresent(setting -> {
                    this.orderChannel = normalizeChannelId(setting.getValue());
                });
            } catch (Exception ignored) {}
        }
        if (this.orderChannel == null) {
            String envChan = System.getenv("ORDER_CHANNEL");
            if (envChan == null || envChan.isBlank()) {
                envChan = System.getenv("ORDER_CHANNEL_ID");
            }
            if (envChan != null && !envChan.isBlank()) {
                this.orderChannel = normalizeChannelId(envChan);
            }
        }
        return this.orderChannel;
    }

    public synchronized void setOrderChannel(String channel) {
        String normalized = normalizeChannelId(channel);
        if (normalized != null && !normalized.isBlank()) {
            this.orderChannel = normalized;
            botSettingRepository.save(new BotSetting(ORDER_CHANNEL_KEY, this.orderChannel));
            log.info("Order channel successfully updated to: {}", this.orderChannel);
        } else {
            this.orderChannel = null;
            botSettingRepository.deleteById(ORDER_CHANNEL_KEY);
            log.info("Order channel cleared.");
        }
    }

    public long nextOrderNumber() {
        long next = orderCounter.incrementAndGet();
        try {
            botSettingRepository.save(new BotSetting(ORDER_COUNTER_KEY, String.valueOf(next)));
        } catch (Exception e) {
            log.warn("Failed to persist order counter: {}", e.getMessage());
        }
        return next;
    }

    public void sendOrderNotification(String productType, String quantity, String recipient, int priceSoom) {
        String targetChannel = getOrderChannel();
        if (targetChannel == null || targetChannel.isBlank()) {
            log.warn("Order notification skipped: no order channel configured in Bot Settings or ENV.");
            return;
        }

        long orderNum = nextOrderNumber();
        String formattedPrice = String.format("%,d", priceSoom).replace(',', ' ');
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));

        String cleanRecipient = (recipient != null && !recipient.isBlank()) ? recipient.trim() : "Mijoz";
        if (!cleanRecipient.startsWith("@") && !cleanRecipient.startsWith("ID:") && !cleanRecipient.matches("^\\d+$")) {
            cleanRecipient = "@" + cleanRecipient;
        }

        String safeProduct = escapeHtml(productType);
        String safeQuantity = escapeHtml(quantity);
        String safeRecipient = escapeHtml(cleanRecipient);

        String message = "🛍 <b>Yangi buyurtma! #ORD-" + orderNum + "</b>\n\n" +
                "📌 <b>Mahsulot:</b> " + safeProduct + "\n" +
                "📦 <b>Miqdori:</b> " + safeQuantity + "\n" +
                "👤 <b>Qabul qiluvchi:</b> " + safeRecipient + "\n" +
                "💰 <b>To‘lov summasi:</b> " + formattedPrice + " so‘m\n" +
                "🕒 <b>Vaqt:</b> " + dateStr + "\n\n" +
                "⚡️ <i>Buyurtma muvaffaqiyatli yetkazildi!</i>";

        InlineKeyboardMarkup keyboard = InlineKeyboardMarkup.builder()
                .keyboard(List.of(
                        new InlineKeyboardRow(
                                InlineKeyboardButton.builder()
                                        .text("🤖 Botdan xarid qilish")
                                        .url("https://t.me/GyroService_bot")
                                        .build()
                        )
                ))
                .build();

        try {
            telegramClient.execute(SendMessage.builder()
                    .chatId(targetChannel)
                    .text(message)
                    .parseMode("HTML")
                    .replyMarkup(keyboard)
                    .build());
            log.info("✅ Order notification sent to channel {} for order #ORD-{}", targetChannel, orderNum);
        } catch (Exception e) {
            log.error("Failed to send HTML order notification to channel {}: {}. Attempting plain-text fallback...", targetChannel, e.getMessage());
            try {
                String plainMsg = "🛍 Yangi buyurtma! #ORD-" + orderNum + "\n\n" +
                        "📌 Mahsulot: " + productType + "\n" +
                        "📦 Miqdori: " + quantity + "\n" +
                        "👤 Qabul qiluvchi: " + cleanRecipient + "\n" +
                        "💰 To‘lov: " + formattedPrice + " so‘m\n" +
                        "🕒 Vaqt: " + dateStr;
                telegramClient.execute(SendMessage.builder()
                        .chatId(targetChannel)
                        .text(plainMsg)
                        .build());
                log.info("✅ Fallback plain order notification sent to channel {}", targetChannel);
            } catch (Exception ex) {
                log.error("❌ Critical: Failed to send fallback order notification to channel {}: {}", targetChannel, ex.getMessage());
            }
        }
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    public boolean testChannel(String channelToTest) {
        String normalized = normalizeChannelId(channelToTest);
        if (normalized == null || normalized.isBlank()) return false;
        try {
            String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));
            telegramClient.execute(SendMessage.builder()
                    .chatId(normalized)
                    .text("✅ <b>Buyurtmalar kanali muvaffaqiyatli ulandi!</b>\n\nBarcha yangi xaridlar ushbu kanalga yuboriladi.\n🕒 <b>Vaqt:</b> " + dateStr)
                    .parseMode("HTML")
                    .build());
            return true;
        } catch (Exception e) {
            log.error("Test message to channel {} failed: {}", normalized, e.getMessage());
            return false;
        }
    }
}

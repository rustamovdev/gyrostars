package ru.lewis.leykabot.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import ru.lewis.leykabot.model.database.entity.BotSetting;
import ru.lewis.leykabot.repository.BotSettingRepository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderChannelService {

    private final BotSettingRepository botSettingRepository;
    private final TelegramClient telegramClient;

    private static final String ORDER_CHANNEL_KEY = "order_channel";
    private static final String ORDER_COUNTER_KEY = "order_counter";

    private final AtomicLong orderCounter = new AtomicLong(100);
    private String orderChannel = null;

    @PostConstruct
    public void init() {
        botSettingRepository.findById(ORDER_CHANNEL_KEY).ifPresent(setting -> {
            this.orderChannel = normalizeChannelId(setting.getValue());
        });

        botSettingRepository.findById(ORDER_COUNTER_KEY).ifPresent(setting -> {
            try {
                this.orderCounter.set(Long.parseLong(setting.getValue()));
            } catch (NumberFormatException ignored) {}
        });
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
        return orderChannel;
    }

    public synchronized void setOrderChannel(String channel) {
        String normalized = normalizeChannelId(channel);
        if (normalized != null && !normalized.isBlank()) {
            this.orderChannel = normalized;
            botSettingRepository.save(new BotSetting(ORDER_CHANNEL_KEY, this.orderChannel));
        } else {
            this.orderChannel = null;
            botSettingRepository.deleteById(ORDER_CHANNEL_KEY);
        }
    }

    public long nextOrderNumber() {
        long next = orderCounter.incrementAndGet();
        botSettingRepository.save(new BotSetting(ORDER_COUNTER_KEY, String.valueOf(next)));
        return next;
    }

    public void sendOrderNotification(String productType, String quantity, String recipient, int priceSoom) {
        if (orderChannel == null || orderChannel.isBlank()) {
            return;
        }

        long orderNum = nextOrderNumber();
        String formattedPrice = String.format("%,d", priceSoom).replace(',', ' ');
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));

        String cleanRecipient = recipient.startsWith("@") ? recipient : "@" + recipient;

        String message = "🛍 <b>Yangi buyurtma! <tg-emoji emoji-id=\"5436172829903068620\">🆔</tg-emoji> #ORD-" + orderNum + "</b>\n\n" +
                "📌 <b>Mahsulot:</b> " + productType + "\n" +
                "📦 <b>Miqdori:</b> " + quantity + "\n" +
                "👤 <b>Qabul qiluvchi:</b> " + cleanRecipient + "\n" +
                "<tg-emoji emoji-id=\"5436107628004549969\">💰</tg-emoji> <b>To‘lov summasi:</b> " + formattedPrice + " so‘m\n" +
                "<tg-emoji emoji-id=\"5438193302778192083\">🕒</tg-emoji> <b>Vaqt:</b> " + dateStr;

        try {
            telegramClient.execute(SendMessage.builder()
                    .chatId(orderChannel)
                    .text(message)
                    .parseMode("HTML")
                    .build());
            log.info("Order notification sent to channel {} for order #ORD-{}", orderChannel, orderNum);
        } catch (Exception e) {
            log.warn("Failed to send order notification to channel {}: {}", orderChannel, e.getMessage());
        }
    }

    public boolean testChannel(String channelToTest) {
        String normalized = normalizeChannelId(channelToTest);
        if (normalized == null || normalized.isBlank()) return false;
        try {
            String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));
            telegramClient.execute(SendMessage.builder()
                    .chatId(normalized)
                    .text("✅ <b>Test xabar!</b>\n\nOrder kanali muvaffaqiyatli ulandi.\n🕒 <b>Vaqt:</b> " + dateStr)
                    .parseMode("HTML")
                    .build());
            return true;
        } catch (Exception e) {
            log.error("Test message to channel {} failed: {}", normalized, e.getMessage());
            return false;
        }
    }
}

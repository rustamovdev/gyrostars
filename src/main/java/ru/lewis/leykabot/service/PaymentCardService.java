package ru.lewis.leykabot.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import ru.lewis.leykabot.configuration.DevModeConfig;
import ru.lewis.leykabot.model.database.entity.DepositReceipt;
import ru.lewis.leykabot.model.database.entity.PaymentCard;
import ru.lewis.leykabot.repository.DepositReceiptRepository;
import ru.lewis.leykabot.repository.PaymentCardRepository;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentCardService {

    private final PaymentCardRepository paymentCardRepository;
    private final DepositReceiptRepository depositReceiptRepository;
    private final TransactionService transactionService;
    private final UserService userService;
    private final TelegramService telegramService;
    private final TelegramClient telegramClient;
    private final DevModeConfig devModeConfig;

    @PostConstruct
    public void init() {
        if (paymentCardRepository.count() == 0) {
            PaymentCard defaultCard = new PaymentCard();
            defaultCard.setCardNumber("9860086603506261");
            defaultCard.setHolderName("S R");
            defaultCard.setMethodName("HUMO");
            defaultCard.setActive(true);
            paymentCardRepository.save(defaultCard);
            log.info("Initialized default payment card with admin card 9860086603506261");
        }
    }

    public List<PaymentCard> getActiveCards() {
        return paymentCardRepository.findAllByIsActiveTrueOrderByIdAsc();
    }

    public List<PaymentCard> getAllCards() {
        return paymentCardRepository.findAllByOrderByIdAsc();
    }

    public PaymentCard addCard(String cardNumber, String holderName, String methodName) {
        PaymentCard card = new PaymentCard();
        card.setCardNumber(cardNumber);
        card.setHolderName(holderName);
        card.setMethodName(methodName);
        card.setActive(true);
        return paymentCardRepository.save(card);
    }

    public boolean deleteCard(Long id) {
        if (paymentCardRepository.existsById(id)) {
            paymentCardRepository.deleteById(id);
            return true;
        }
        return false;
    }

    public boolean toggleCard(Long id) {
        Optional<PaymentCard> cardOpt = paymentCardRepository.findById(id);
        if (cardOpt.isPresent()) {
            PaymentCard card = cardOpt.get();
            card.setActive(!card.isActive());
            paymentCardRepository.save(card);
            return card.isActive();
        }
        return false;
    }

    @Transactional
    public DepositReceipt createDepositReceipt(Long userId, Long chatId, int amount, String cardInfo, String fileId) {
        DepositReceipt receipt = new DepositReceipt();
        receipt.setUserId(userId);
        receipt.setChatId(chatId);
        receipt.setAmount(amount);
        receipt.setCardInfo(cardInfo);
        receipt.setFileId(fileId);
        receipt.setStatus("PENDING");

        DepositReceipt saved = depositReceiptRepository.save(receipt);

        notifyAdminsNewReceipt(saved);
        return saved;
    }

    private void notifyAdminsNewReceipt(DepositReceipt receipt) {
        List<Long> admins = devModeConfig.getWhitelist();
        if (admins == null) admins = new ArrayList<>();
        if (!admins.contains(AdminService.PRIMARY_ADMIN)) {
            admins.add(AdminService.PRIMARY_ADMIN);
        }

        String username = telegramService.getUsernameByUserId(receipt.getUserId());
        String formattedAmount = String.format("%,d", receipt.getAmount()).replace(',', ' ');
        String dateStr = receipt.getCreatedAt().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));

        String caption = "📥 <b>Yangi to‘lov cheki keldi! (<tg-emoji emoji-id=\"5436172829903068620\">🆔</tg-emoji> #REQ-" + receipt.getId() + ")</b>\n\n" +
                "👤 <b>Foydalanuvchi:</b> " + (username != null ? username : "Noma'lum") + " (<tg-emoji emoji-id=\"5436172829903068620\">🆔</tg-emoji> ID: <code>" + receipt.getUserId() + "</code>)\n" +
                "<tg-emoji emoji-id=\"5436107628004549969\">💰</tg-emoji> <b>Summa:</b> <b>" + formattedAmount + " so‘m</b>\n" +
                "<tg-emoji emoji-id=\"5436203328465838905\">💳</tg-emoji> <b>Karta:</b> " + receipt.getCardInfo() + "\n" +
                "<tg-emoji emoji-id=\"5438193302778192083\">🕒</tg-emoji> <b>Vaqt:</b> " + dateStr + "\n\n" +
                "To‘lovni tasdiqlaysizmi yoki bekor qilasizmi?";

        List<InlineKeyboardRow> keyboard = new ArrayList<>();
        InlineKeyboardRow row = new InlineKeyboardRow();
        row.add(ru.lewis.leykabot.model.button.StyledInlineButton.styledBuilder()
                .text("Tasdiqlash (+" + formattedAmount + " so‘m)")
                .callbackData("dep_app_" + receipt.getId())
                .style("success")
                .iconCustomEmojiId("5436406725232074977")
                .build());
        row.add(ru.lewis.leykabot.model.button.StyledInlineButton.styledBuilder()
                .text("Bekor qilish")
                .callbackData("dep_rej_" + receipt.getId())
                .style("danger")
                .build());
        keyboard.add(row);

        InlineKeyboardMarkup markup = InlineKeyboardMarkup.builder().keyboard(keyboard).build();

        for (Long adminId : admins) {
            try {
                SendPhoto sendPhoto = SendPhoto.builder()
                        .chatId(adminId)
                        .photo(new InputFile(receipt.getFileId()))
                        .caption(caption)
                        .parseMode("HTML")
                        .replyMarkup(markup)
                        .build();
                telegramClient.execute(sendPhoto);
            } catch (Exception e) {
                log.warn("Failed to notify admin {}: {}", adminId, e.getMessage());
            }
        }
    }

    @Transactional
    public synchronized boolean approveDeposit(Long receiptId, Long adminId) {
        Optional<DepositReceipt> receiptOpt = depositReceiptRepository.findById(receiptId);
        if (receiptOpt.isEmpty()) return false;

        DepositReceipt receipt = receiptOpt.get();
        if (!"PENDING".equals(receipt.getStatus())) {
            return false;
        }

        receipt.setStatus("APPROVED");
        receipt.setApprovedBy(adminId);
        depositReceiptRepository.save(receipt);

        transactionService.create(receipt.getUserId(), receipt.getAmount());

        long currentBalance = userService.getBalance(receipt.getUserId()).orElse(0);
        String formattedAmount = String.format("%,d", receipt.getAmount()).replace(',', ' ');
        String formattedBalance = String.format("%,d", currentBalance).replace(',', ' ');

        telegramService.sendMessageAuto(receipt.getChatId(),
                "<tg-emoji emoji-id=\"5436406725232074977\">✅</tg-emoji> <b>To‘lovingiz muvaffaqiyatli tasdiqlandi!</b>\n\n" +
                        "➕ Balansingizga <b>" + formattedAmount + " so‘m</b> qo‘shildi.\n" +
                        "<tg-emoji emoji-id=\"5436171485578308032\">💸</tg-emoji> Joriy balansingiz: <b>" + formattedBalance + " so‘m</b>\n\n" +
                        "Xaridlarni boshlash uchun menyudan Stars yoki Premium bo‘limiga o‘tishingiz mumkin! ⭐️");

        return true;
    }

    @Transactional
    public synchronized boolean rejectDeposit(Long receiptId, Long adminId) {
        Optional<DepositReceipt> receiptOpt = depositReceiptRepository.findById(receiptId);
        if (receiptOpt.isEmpty()) return false;

        DepositReceipt receipt = receiptOpt.get();
        if (!"PENDING".equals(receipt.getStatus())) {
            return false;
        }

        receipt.setStatus("REJECTED");
        receipt.setApprovedBy(adminId);
        depositReceiptRepository.save(receipt);

        String formattedAmount = String.format("%,d", receipt.getAmount()).replace(',', ' ');
        telegramService.sendMessageAuto(receipt.getChatId(),
                "❌ <b>To‘lov chekingiz rad etildi!</b> (Summa: " + formattedAmount + " so‘m)\n\n" +
                        "Iltimos, pul haqiqatan o‘tkazilganini va chek to‘g‘riligini tekshirib, qaytadan urinib ko‘ring yoki qo‘llab-quvvatlash xizmati bilan bog‘laning.");

        return true;
    }

    public Optional<DepositReceipt> getReceipt(Long id) {
        return depositReceiptRepository.findById(id);
    }
}

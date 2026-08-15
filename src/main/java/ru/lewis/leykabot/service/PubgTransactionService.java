package ru.lewis.leykabot.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.lewis.leykabot.model.database.entity.PubgTransaction;
import ru.lewis.leykabot.model.database.entity.User;
import ru.lewis.leykabot.repository.PubgTransactionRepository;
import ru.lewis.leykabot.repository.UserRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class PubgTransactionService {

    private final PubgTransactionRepository pubgRepository;
    private final UserRepository userRepository;
    private final UserService userService;
    private final OrderChannelService orderChannelService;

    @Transactional
    public PubgTransaction create(Long telegramId, String playerId, String playerNickname,
                                  String offerId, int ucAmount, int priceRubles,
                                  Long apiOrderId, String reference, String redeemCode) {
        User user = userRepository.findByTelegramId(telegramId)
                .orElseThrow(() -> new IllegalArgumentException("Foydalanuvchi topilmadi: " + telegramId));

        int currentBalance = user.getBalance() != null ? user.getBalance() : 0;
        user.setBalance(currentBalance - priceRubles);
        userRepository.save(user);
        userService.updateUserCache(user);

        PubgTransaction tx = new PubgTransaction();
        tx.setTelegramId(telegramId);
        tx.setPlayerId(playerId);
        tx.setPlayerNickname(playerNickname);
        tx.setOfferId(offerId);
        tx.setUcAmount(ucAmount);
        tx.setPriceRubles(priceRubles);
        tx.setOrderType("uc");
        tx.setApiOrderId(apiOrderId);
        tx.setReference(reference);
        tx.setRedeemCode(redeemCode);
        tx.setStatus("COMPLETED");

        PubgTransaction saved = pubgRepository.save(tx);
        log.info("PUBG UC xaridi muvaffaqiyatli saqlandi #{} (Foydalanuvchi: {}, Player: {}, UC: {})",
                saved.getId(), telegramId, playerId, ucAmount);

        // Order kanaliga xabar yuborish
        if (orderChannelService != null) {
            String recipientInfo = (playerNickname != null && !playerNickname.isBlank())
                    ? playerNickname + " (" + playerId + ")"
                    : "ID: " + playerId;
            orderChannelService.sendOrderNotification(
                    "<tg-emoji emoji-id=\"5436050603723760533\">🎮</tg-emoji> PUBG Mobile UC",
                    ucAmount + " UC",
                    recipientInfo,
                    priceRubles
            );
        }

        return saved;
    }

    public long getCount(Long telegramId) {
        return pubgRepository.countByTelegramId(telegramId);
    }

    public long getTotalUc(Long telegramId) {
        return pubgRepository.sumUcByTelegramId(telegramId);
    }
}

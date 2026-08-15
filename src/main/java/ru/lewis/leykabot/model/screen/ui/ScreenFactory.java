package ru.lewis.leykabot.model.screen.ui;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import ru.lewis.leykabot.configuration.DevModeConfig;
import ru.lewis.leykabot.configuration.TopFormat;
import ru.lewis.leykabot.configuration.telegram.TelegramConfig;
import ru.lewis.leykabot.configuration.loc.ButtonsLocConfig;
import ru.lewis.leykabot.configuration.loc.ClientMessageConfig;
import ru.lewis.leykabot.configuration.loc.ErrorMessageConfig;
import ru.lewis.leykabot.configuration.loc.KeyboardLocConfig;
import ru.lewis.leykabot.model.Top;
import ru.lewis.leykabot.model.database.entity.PaymentCard;
import ru.lewis.leykabot.model.screen.ui.impl.*;
import ru.lewis.leykabot.model.screen.ui.impl.admin.*;
import ru.lewis.leykabot.model.screen.ui.impl.deposit.RublesDepositOrderScreen;
import ru.lewis.leykabot.model.screen.ui.impl.deposit.RublesDepositScreen;
import ru.lewis.leykabot.model.screen.ui.impl.deposit.RublesDepositSelectPaymentMethodScreen;
import ru.lewis.leykabot.model.screen.ui.impl.premium.PremiumBuyScreen;
import ru.lewis.leykabot.model.screen.ui.impl.premium.UserSelectPremiumScreen;
import ru.lewis.leykabot.model.screen.ui.impl.star.StarBuyScreen;
import ru.lewis.leykabot.model.screen.ui.impl.star.UserSelectStarsScreen;
import ru.lewis.leykabot.model.screen.ui.impl.top.TopSelectScreen;
import ru.lewis.leykabot.model.screen.ui.impl.top.TopShowScreen;
import ru.lewis.leykabot.service.*;

@Component
@AllArgsConstructor
public class ScreenFactory {

    private final ButtonsLocConfig buttonsLocConfig;
    private final ClientMessageConfig clientMessageConfig;
    private final ScreenManager screenManager;
    private final TelegramService telegramService;
    private final TelegramConfig telegramConfig;
    private final KeyboardLocConfig keyboardLocConfig;
    private final TransactionService transactionService;
    private final UserService userService;
    private final ErrorMessageConfig errorMessageConfig;
    private final DevModeConfig devModeConfig;
    private final FragmentStarsService fragmentStarsService;
    private final StarsTransactionService starsTransactionService;
    private final PremiumTransactionService premiumTransactionService;
    private final FragmentPremiumService fragmentPremiumService;
    private final TopService topService;
    private final TopFormat topFormat;
    private final AdminService adminService;
    private final PriceService priceService;
    private final PaymentCardService paymentCardService;
    private final OrderChannelService orderChannelService;
    private final AutoPaymentService autoPaymentService;
    private final PubgService pubgService;
    private final PubgTransactionService pubgTransactionService;
    private final ReportService reportService;

    public StartScreen createStartScreen(Long chatId, Long userId) {
        return new StartScreen(chatId, userId, clientMessageConfig, buttonsLocConfig, screenManager, telegramService, telegramConfig, adminService, userService, this);
    }

    public ProfileScreen createProfileScreen(Long chatId, Long userId) {
        return new ProfileScreen(chatId, userId, buttonsLocConfig, clientMessageConfig, screenManager, telegramService, userService,
                transactionService, starsTransactionService, premiumTransactionService, this);
    }

    public SupportScreen createSupportScreen(Long chatId, Long userId) {
        return new SupportScreen(chatId, userId, buttonsLocConfig, clientMessageConfig, screenManager, telegramService, this);
    }

    public RublesDepositScreen createDepositRublesScreen(Long chatId, Long userId) {
        return new RublesDepositScreen(chatId, userId,
                buttonsLocConfig, keyboardLocConfig, clientMessageConfig, transactionService,
                errorMessageConfig, telegramService, devModeConfig, screenManager, this);
    }

    public StarBuyScreen createBuyStarsScreen(Long chatId, Long userId) {
        return new StarBuyScreen(chatId, userId,
                buttonsLocConfig, clientMessageConfig,
                errorMessageConfig, telegramService, devModeConfig, priceService, screenManager, this);
    }

    public UserSelectStarsScreen createSelectUserForBuyStarsScreen(Long chatId, Long userId, int stars, int rubles) {
        return new UserSelectStarsScreen(chatId, userId, stars, rubles,
                clientMessageConfig, buttonsLocConfig, telegramService, fragmentStarsService,
                errorMessageConfig, userService, starsTransactionService, orderChannelService, screenManager, this);
    }

    public ChannelSubscribeScreen createSubscribeChannelScreen(Long chatId, Long userId) {
        return new ChannelSubscribeScreen(chatId, userId, telegramService, clientMessageConfig, telegramConfig, buttonsLocConfig, screenManager, this);
    }

    public PremiumBuyScreen createBuyPremiumScreen(Long chatId, Long userId) {
        return new PremiumBuyScreen(chatId, userId, buttonsLocConfig, clientMessageConfig, telegramService,
                devModeConfig, priceService, screenManager, this);
    }

    public UserSelectPremiumScreen createSelectUserForBuyPremiumScreen(Long chatId, Long userId, int months, int rubles) {
        return new UserSelectPremiumScreen(chatId, userId, months, rubles, clientMessageConfig, buttonsLocConfig, errorMessageConfig, telegramService,
                fragmentPremiumService, premiumTransactionService, userService, orderChannelService, screenManager, this);
    }

    public TopShowScreen createTopShowScreen(Long chatId, Long userId, Top top, int page) {
        return new TopShowScreen(chatId, userId, top, page, telegramService, topFormat, buttonsLocConfig, topService, screenManager, this);
    }

    public TopSelectScreen createTopSelectScreen(Long chatId, Long userId) {
        return new TopSelectScreen(chatId, userId, screenManager, this, topService, telegramService);
    }

    public RublesDepositSelectPaymentMethodScreen createRublesDepositSelectPaymentMethodScreen(Long chatId, Long userId, int rubles) {
        return new RublesDepositSelectPaymentMethodScreen(chatId, userId, rubles, buttonsLocConfig,
                clientMessageConfig, paymentCardService, screenManager, this);
    }

    public RublesDepositOrderScreen createRublesDepositOrderScreen(Long chatId, Long userId, int rubles, PaymentCard card) {
        return new RublesDepositOrderScreen(chatId, userId, rubles, card, paymentCardService, autoPaymentService, telegramService, buttonsLocConfig, screenManager, this);
    }

    public AdminMainScreen createAdminMainScreen(Long chatId, Long userId) {
        return new AdminMainScreen(chatId, userId, screenManager, this, adminService, fragmentStarsService, telegramService);
    }

    public AdminStatsScreen createAdminStatsScreen(Long chatId, Long userId) {
        return new AdminStatsScreen(chatId, userId, screenManager, this, adminService, reportService);
    }

    public AdminBroadcastScreen createAdminBroadcastScreen(Long chatId, Long userId) {
        return new AdminBroadcastScreen(chatId, userId, screenManager, this, adminService, telegramService);
    }

    public AdminOrderChannelScreen createAdminOrderChannelScreen(Long chatId, Long userId) {
        return new AdminOrderChannelScreen(chatId, userId, screenManager, this, adminService, orderChannelService, telegramService);
    }

    public AdminPriceScreen createAdminPriceScreen(Long chatId, Long userId) {
        return new AdminPriceScreen(chatId, userId, screenManager, this, adminService, priceService, telegramService);
    }

    public AdminAdminsScreen createAdminAdminsScreen(Long chatId, Long userId) {
        return new AdminAdminsScreen(chatId, userId, screenManager, this, adminService, telegramService);
    }

    public AdminCardScreen createAdminCardScreen(Long chatId, Long userId) {
        return new AdminCardScreen(chatId, userId, screenManager, this, adminService, paymentCardService, telegramService);
    }

    public ReferralScreen createReferralScreen(Long chatId, Long userId) {
        return new ReferralScreen(chatId, userId, screenManager, this, userService, telegramService);
    }

    public GiftSelectScreen createGiftSelectScreen(Long chatId, Long userId) {
        return new GiftSelectScreen(chatId, userId, screenManager, this);
    }

    public ru.lewis.leykabot.model.screen.ui.impl.pubg.PubgBuyScreen createPubgBuyScreen(Long chatId, Long userId) {
        return new ru.lewis.leykabot.model.screen.ui.impl.pubg.PubgBuyScreen(
                chatId, userId, screenManager, this, pubgService, pubgTransactionService, userService, telegramService
        );
    }
}
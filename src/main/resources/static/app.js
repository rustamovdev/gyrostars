// Telegram WebApp Initialization
const tg = window.Telegram?.WebApp;
if (tg) {
  try {
    tg.ready();
    tg.expand();
    if (tg.setHeaderColor) tg.setHeaderColor('#090d16');
    if (tg.setBackgroundColor) tg.setBackgroundColor('#090d16');
  } catch (e) {
    console.log('TG init error:', e);
  }
}

// Telegram Haptic Feedback Helper
function triggerHaptic(type = 'light') {
  try {
    if (tg?.HapticFeedback) {
      if (type === 'success' || type === 'error' || type === 'warning') {
        tg.HapticFeedback.notificationOccurred(type);
      } else {
        tg.HapticFeedback.impactOccurred(type);
      }
    }
  } catch (e) {}
}

// Global State
let state = {
  user: {
    userId: null,
    username: '',
    fullName: 'Mijoz',
    balance: 0,
    photoUrl: null,
    verified: false
  },
  card: {
    cardNumber: '---- ---- ---- ----',
    holderName: 'Admin',
    bankName: 'HUMO'
  },
  prices: {
    starUnitPrice: 230,
    starPackages: { 50: 12000, 100: 23000, 150: 34000, 250: 53000, 350: 78000, 500: 110000, 750: 160000, 1000: 215000 },
    premiumPackages: { 1: 50000, 3: 170000, 6: 230000, 12: 300000 },
    pubgPackages: { 60: 11000, 325: 55000, 660: 110000, 1800: 275000, 3850: 545000, 8100: 1090000 }
  },
  payMethod: 'card', // 'card' or 'balance'
  selectedPremMonths: 3,
  selectedPremPrice: 170000,
  selectedPubgUc: 325,
  selectedPubgPrice: 55000,
  isPostMode: false,
  activeInvoice: null,
  timerInterval: null
};

function cleanUsername(raw) {
  if (!raw) return '';
  raw = String(raw).trim();
  while (raw.startsWith('@')) {
    raw = raw.substring(1).trim();
  }
  return raw;
}

function formatUsername(raw) {
  const clean = cleanUsername(raw);
  return clean ? '@' + clean : '';
}

// BILINGUAL LOCALIZATION (UZ / RU)
const I18N = {
  uz: {
    langBtn: "🇺🇿 UZ",
    starsTab: "⭐ Stars",
    premTab: "💎 Premium",
    pubgTab: "🎮 PUBG UC",
    homeTitle: "Bosh sahifa",
    topTitle: "Top Reyting",
    servicesTitle: "Xizmatlar",
    accountTitle: "Shaxsiy Kabinet",
    historyTitle: "Tranzaksiyalar Tarixi",
    starsSub: "Tezkor va arzon Stars xaridi",
    premSub: "1, 3, 6 va 12 oylik rasmiy obunalar",
    pubgSub: "Tezkor va arzon o'yin donatlari",
    amountLabel: "⭐ Stars miqdori (Minimal 50):",
    targetUserLabel: "👤 Qabul qiluvchi @username:",
    ownUserHint: "O'zingizning username'ni kiritish",
    pubgIdLabel: "🆔 PUBG Player ID (Raqamli ID):",
    payMethodLabel: "💳 To'lov turi:",
    payCard: "Karta orqali",
    payBalance: "Balansdan to'lash",
    buyStarsBtn: "⭐ Stars xarid qilish",
    buyPremBtn: "💎 Premium xarid qilish",
    buyPubgBtn: "🎮 UC xarid qilish",
    depositTitle: "Hisobni to'ldirish",
    currBal: "Joriy balans",
    totStars: "Jami Stars",
    totSpent: "Jami sarflangan",
    totPurchases: "Xaridlar soni",
    depositBtn: "💳 + Hisobni to'ldirish",
    promoBtn: "Promokod kiritish",
    promoSub: "Maxsus bonus yoki sovg'alarga ega bo'ling",
    refTitle: "Do‘stlarni taklif qiling",
    refSub: "Har bir to‘ldirishdan 2% naqd bonus olasiz",
    refCopyBtn: "🔗 Havoladan nusxa olish",
    refShareBtn: "📤 Ulashish",
    contestTitle: "Oylik Reyting Tanlovi",
    contestDesc: "1 oy ichida reytingda Top 1 bo‘lgan g‘olibga 1 oylik Telegram Premium bepul beriladi! 🎁",
    contestViewBtn: "🏆 Reytingni ko'rish",
    supportTitle: "Jonli Qo'llab-quvvatlash",
    supportSub: "Savol yoki muammolar bo'yicha admin (@gyro_pm)",
    supportBtn: "Yozish ›",
    boostTitle: "Kanalimiz uchun ovoz bering!",
    boostSub: "Kanalimizga boost berib, chegirmalarga ega bo'ling.",
    boostBtn: "⚡ Ovoz berish (Boost)",
    todayTab: "Bugun",
    weekTab: "7 kun",
    monthTab: "30 kun",
    allTab: "Hammasi",
    recentBuyers: "🔥 So'nggi faol xaridorlar:",
    navHome: "Bosh sahifa",
    navTop: "Top",
    navServices: "Xizmatlar",
    navAccount: "Kabinet",
    navHistory: "Tarix",
    promoModalTitle: "🎟 Promokod Faollashtirish",
    promoInputLabel: "Promokod nomini kiriting:",
    promoApplyBtn: "✨ Promokodni faollashtirish",
    receiptModalTitle: "🧾 Elektron Kvitansiya",
    copyBtn: "📋 Nusxalash",
    shareBtn: "📤 Ulashish"
  },
  ru: {
    langBtn: "🇷🇺 RU",
    starsTab: "⭐ Stars",
    premTab: "💎 Premium",
    pubgTab: "🎮 PUBG UC",
    homeTitle: "Главная",
    topTitle: "Топ Рейтинг",
    servicesTitle: "Услуги",
    accountTitle: "Личный Кабинет",
    historyTitle: "История Транзакций",
    starsSub: "Быстрая и выгодная покупка Stars",
    premSub: "Официальные подписки на 1, 3, 6 и 12 месяцев",
    pubgSub: "Быстрый донат в любимые игры",
    amountLabel: "⭐ Количество Stars (Минимум 50):",
    targetUserLabel: "👤 Получатель @username:",
    ownUserHint: "Вставить свой username",
    pubgIdLabel: "🆔 PUBG Player ID (Цифровой ID):",
    payMethodLabel: "💳 Способ оплаты:",
    payCard: "Картой (HUMO/UZCARD)",
    payBalance: "С баланса бота",
    buyStarsBtn: "⭐ Купить Stars",
    buyPremBtn: "💎 Купить Premium",
    buyPubgBtn: "🎮 Купить UC",
    depositTitle: "Пополнение баланса",
    currBal: "Текущий баланс",
    totStars: "Всего Stars",
    totSpent: "Всего потрачено",
    totPurchases: "Количество покупок",
    depositBtn: "💳 + Пополнить баланс",
    promoBtn: "Ввести промокод",
    promoSub: "Получите бонусы и подарки",
    refTitle: "Приглашайте друзей",
    refSub: "Получайте 2% кешбэк со всех пополнений",
    refCopyBtn: "🔗 Скопировать ссылку",
    refShareBtn: "📤 Поделиться",
    contestTitle: "Ежемесячный Конкурс Рейтинга",
    contestDesc: "Победителю Топ-1 за месяц дарим 1 месяц Telegram Premium бесплатно! 🎁",
    contestViewBtn: "🏆 Смотреть рейтинг",
    supportTitle: "Живая Поддержка",
    supportSub: "По всем вопросам и помощи (@gyro_pm)",
    supportBtn: "Написать ›",
    boostTitle: "Проголосуйте за канал!",
    boostSub: "Дайте буст нашему каналу и получайте скидки.",
    boostBtn: "⚡ Голосовать (Boost)",
    todayTab: "Сегодня",
    weekTab: "7 дней",
    monthTab: "30 дней",
    allTab: "Все время",
    recentBuyers: "🔥 Недавние покупатели:",
    navHome: "Главная",
    navTop: "Топ",
    navServices: "Услуги",
    navAccount: "Кабинет",
    navHistory: "История",
    promoModalTitle: "🎟 Активация Промокода",
    promoInputLabel: "Введите промокод:",
    promoApplyBtn: "✨ Активировать промокод",
    receiptModalTitle: "🧾 Электронный Чек",
    copyBtn: "📋 Скопировать",
    shareBtn: "📤 Поделиться"
  }
};

let currentLang = localStorage.getItem('gyro_lang') || 'uz';

function toggleLanguage() {
  currentLang = currentLang === 'uz' ? 'ru' : 'uz';
  localStorage.setItem('gyro_lang', currentLang);
  triggerHaptic('medium');
  applyLanguage();
  showToast(currentLang === 'uz' ? "Til o'zgartirildi: O'zbekcha 🇺🇿" : "Язык изменен: Русский 🇷🇺");
}

function applyLanguage() {
  const dict = I18N[currentLang] || I18N.uz;
  const btn = document.getElementById('langSwitchBtn');
  if (btn) btn.innerText = dict.langBtn;

  document.querySelectorAll('[data-i18n]').forEach(el => {
    const key = el.getAttribute('data-i18n');
    if (dict[key]) {
      el.innerText = dict[key];
    }
  });

  document.querySelectorAll('[data-i18n-html]').forEach(el => {
    const key = el.getAttribute('data-i18n-html');
    if (dict[key]) {
      el.innerHTML = dict[key];
    }
  });

  document.querySelectorAll('[data-i18n-ph]').forEach(el => {
    const key = el.getAttribute('data-i18n-ph');
    if (dict[key]) {
      el.placeholder = dict[key];
    }
  });
}

// DOM Content Loaded
document.addEventListener('DOMContentLoaded', () => {
  applyLanguage();
  // 1. Extract real user info from Telegram WebApp
  if (tg?.initDataUnsafe?.user) {
    const u = tg.initDataUnsafe.user;
    state.user.userId = u.id;
    state.user.username = cleanUsername(u.username || '');
    state.user.fullName = ((u.first_name || '') + ' ' + (u.last_name || '')).trim() || (state.user.username ? formatUsername(state.user.username) : 'Mijoz');
    if (u.photo_url) {
      state.user.photoUrl = u.photo_url;
    }
  } else {
    // 2. Fallback to URL search parameters if opened in browser
    const params = new URLSearchParams(window.location.search);
    const pId = params.get('userId') || params.get('user_id') || params.get('id');
    if (pId) {
      state.user.userId = parseInt(pId);
      state.user.username = cleanUsername(params.get('username') || '');
      state.user.fullName = params.get('name') || (state.user.username ? formatUsername(state.user.username) : 'Mijoz');
    }
  }

  updateUserUI();
  fetchInitData();
  fetchTopData('today');
  setStarsAmount(100);
  setDepositAmount(50000);

  if (state.user.username) {
    const un = formatUsername(state.user.username);
    const sInput = document.getElementById('targetUsernameInput');
    const pInput = document.getElementById('premTargetUsername');
    if (sInput && !sInput.value) sInput.value = un;
    if (pInput && !pInput.value) pInput.value = un;
  }
});

// Update Top User Header
function updateUserUI() {
  const nameEl = document.getElementById('userName');
  const idEl = document.getElementById('userIdVal');
  const balEl = document.getElementById('userBalanceTop');
  const avatarEl = document.getElementById('userAvatar');
  const accBal = document.getElementById('accountBalVal');
  const depBigBal = document.getElementById('depositBigBal');

  let cleanName = state.user.fullName;
  if (!cleanName || cleanName === 'Mijoz' || cleanName.startsWith('@')) {
    if (state.user.fullName && !state.user.fullName.startsWith('@') && state.user.fullName !== 'Mijoz') {
      cleanName = state.user.fullName;
    } else if (state.user.username) {
      cleanName = formatUsername(state.user.username);
    } else {
      cleanName = 'Mijoz';
    }
  }
  while (cleanName.startsWith('@@')) {
    cleanName = cleanName.substring(1);
  }

  if (nameEl) nameEl.innerText = cleanName;
  if (idEl) idEl.innerText = state.user.userId ? state.user.userId : '---';
  if (avatarEl) {
    if (state.user.photoUrl) {
      avatarEl.innerHTML = `<img src="${state.user.photoUrl}" alt="avatar" style="width:100%;height:100%;object-fit:cover;border-radius:50%;">`;
    } else {
      const name = cleanName || 'M';
      const initials = name.replace(/^@+/, '').split(' ').map(w => w[0]).join('').substring(0, 2).toUpperCase() || 'M';
      avatarEl.innerText = initials;
    }
  }

  const formattedBal = formatMoney(state.user.balance) + " so'm";
  if (balEl) balEl.innerText = formattedBal;
  if (accBal) accBal.innerText = formattedBal;
  if (depBigBal) depBigBal.innerText = formattedBal;
}

// Auto-fill user's own username
function fillOwnUsername(inputId) {
  triggerHaptic('medium');
  const input = document.getElementById(inputId);
  if (!input) return;
  let un = formatUsername(state.user.username);
  if (un) {
    input.value = un;
    showToast("O'zingizning " + un + " kiritildi! 👤");
  } else {
    showToast("Telegram username topilmadi");
  }
}

// Fetch Backend Init
async function fetchInitData() {
  try {
    const query = state.user.userId ? `?userId=${state.user.userId}&username=${encodeURIComponent(state.user.username || '')}&fullName=${encodeURIComponent(state.user.fullName || '')}` : '';
    const res = await fetch(`/api/webapp/init${query}`);
    if (res.ok) {
      const data = await res.json();
      if (data.user) {
        state.user.balance = data.user.balance || 0;
        if (data.user.fullName && !state.user.fullName) state.user.fullName = data.user.fullName;
        if (data.user.username && !state.user.username) state.user.username = data.user.username;
        if (data.user.userId && !state.user.userId) state.user.userId = data.user.userId;
      }
      if (data.card) {
        state.card = {
          cardNumber: data.card.cardNumber,
          holderName: data.card.holderName,
          bankName: data.card.methodName || 'HUMO'
        };
        const invoiceCardNum = document.getElementById('invoiceCardNumber');
        const invoiceHolder = document.getElementById('invoiceHolderName');
        const invoiceBrand = document.getElementById('invoiceCardBrand');
        if (invoiceCardNum) invoiceCardNum.innerText = data.card.cardNumber.replace(/(\d{4})/g, '$1 ').trim();
        if (invoiceHolder) invoiceHolder.innerText = data.card.holderName;
        if (invoiceBrand) invoiceBrand.innerText = data.card.methodName || 'HUMOCARD';
      }
      if (data.prices) {
        state.prices = data.prices;
      }
      if (data.stats) {
        const sVal = document.getElementById('accountStarsVal');
        const spVal = document.getElementById('accountSpentVal');
        const pVal = document.getElementById('accountPurchasesVal');
        if (sVal) sVal.innerText = formatMoney(data.stats.totalStars || 0);
        if (spVal) spVal.innerText = formatMoney(data.stats.totalSpent || 0) + " so'm";
        if (pVal) pVal.innerText = data.stats.totalPurchases || 0;
      }
      updateUserUI();
      calculateStarsPrice();
    }
  } catch (e) {
    console.log('Init error:', e);
  }
}

// Screen Switcher
function switchScreen(screenName) {
  triggerHaptic('light');

  // Hide all screens
  const screens = document.querySelectorAll('.screen-view');
  screens.forEach(s => s.classList.remove('active'));

  // Deactivate all nav items
  const navItems = document.querySelectorAll('.nav-item');
  navItems.forEach(n => n.classList.remove('active'));

  // Show target screen
  const target = document.getElementById('screen-' + screenName);
  if (target) {
    target.classList.add('active');
  }

  // Activate nav button
  const activeNav = document.querySelector(`.nav-item[data-screen="${screenName}"]`);
  if (activeNav) {
    activeNav.classList.add('active');
  }

  if (screenName === 'top') fetchTopData('today');
  if (screenName === 'history') fetchHistoryData();
  if (screenName === 'account') fetchReferralData();

  window.scrollTo(0, 0);
}

// Category Tabs on Home Screen
function switchHomeTab(catName) {
  triggerHaptic('light');

  const tabs = document.querySelectorAll('.cat-tab');
  tabs.forEach(t => t.classList.remove('active'));

  const views = document.querySelectorAll('.tab-content-view');
  views.forEach(v => v.classList.remove('active'));

  const activeTabBtn = document.querySelector(`.cat-tab[data-cat="${catName}"]`);
  if (activeTabBtn) activeTabBtn.classList.add('active');

  const activeView = document.getElementById(`home-${catName}-view`);
  if (activeView) activeView.classList.add('active');
}

function openServicesTab(serviceName) {
  switchScreen('home');
  switchHomeTab(serviceName);
}

// Stars Calculations & Interactive Range Slider
function onStarsSliderChange(val) {
  triggerHaptic('light');
  const amount = parseInt(val) || 50;
  const input = document.getElementById('starsAmountInput');
  if (input) input.value = amount;
  setStarsAmount(amount);
}

function onStarsInputChange() {
  const input = document.getElementById('starsAmountInput');
  const slider = document.getElementById('starsRangeSlider');
  const val = parseInt(input.value) || 50;
  if (slider) slider.value = Math.min(2500, Math.max(50, val));
  setStarsAmount(val);
}

function setStarsAmount(amount, el) {
  triggerHaptic('light');
  const input = document.getElementById('starsAmountInput');
  const slider = document.getElementById('starsRangeSlider');
  if (input && input.value != amount) {
    input.value = amount;
  }
  if (slider && slider.value != amount) {
    slider.value = Math.min(2500, Math.max(50, amount));
  }
  calculateStarsPrice();

  document.querySelectorAll('#home-stars-view .pkg-chip').forEach(c => c.classList.remove('active'));
  if (el) {
    el.classList.add('active');
  } else {
    document.querySelectorAll('#home-stars-view .pkg-chip').forEach(c => {
      if (c.innerText.startsWith(amount.toString()) || c.innerText.includes(amount.toString() + ' ⭐')) {
        c.classList.add('active');
      }
    });
  }
}

function openLiveSupport() {
  triggerHaptic('medium');
  const supportUrl = 'https://t.me/gyro_pm';
  if (tg?.openTelegramLink) {
    tg.openTelegramLink(supportUrl);
  } else {
    window.open(supportUrl, '_blank');
  }
}

function calculateStarsPrice() {
  const input = document.getElementById('starsAmountInput');
  const priceDisplay = document.getElementById('starsCalculatedPrice');
  const val = parseInt(input.value) || 0;

  if (val <= 0) {
    priceDisplay.innerText = "0 so'm";
    return;
  }

  let price = 0;
  if (state.prices.starPackages && state.prices.starPackages[val]) {
    price = state.prices.starPackages[val];
  } else {
    const unit = state.prices.starUnitPrice || 230;
    price = Math.round((val * unit) / 100) * 100;
  }

  priceDisplay.innerText = formatMoney(price) + " so'm";
}

function togglePostMode() {
  triggerHaptic('medium');
  state.isPostMode = !state.isPostMode;
  const btn = document.getElementById('postModeBtn');
  const label = document.getElementById('targetLabel');
  const input = document.getElementById('targetUsernameInput');

  if (state.isPostMode) {
    btn.classList.add('active');
    btn.innerText = "👤 Profilga";
    label.innerText = "🔗 Post havolasi (Link)";
    input.placeholder = "https://t.me/kanal/123";
  } else {
    btn.classList.remove('active');
    btn.innerText = "Postga Stars";
    label.innerText = "👤 Foydalanuvchi";
    input.placeholder = "@username";
  }
}

// Payment Method
function selectPayMethod(method) {
  triggerHaptic('light');
  state.payMethod = method;

  const isCard = method === 'card';
  const isBal = method === 'balance';

  const starsCard = document.getElementById('payMethodCard');
  const starsBal = document.getElementById('payMethodBalance');
  if (starsCard) starsCard.classList.toggle('active', isCard);
  if (starsBal) starsBal.classList.toggle('active', isBal);

  document.querySelectorAll('.prem-pay-card').forEach(b => b.classList.toggle('active', isCard));
  document.querySelectorAll('.prem-pay-bal').forEach(b => b.classList.toggle('active', isBal));

  document.querySelectorAll('.pubg-pay-card').forEach(b => b.classList.toggle('active', isCard));
  document.querySelectorAll('.pubg-pay-bal').forEach(b => b.classList.toggle('active', isBal));
}

// Premium & PUBG Package Pickers
function selectPremPackage(months, price, el) {
  triggerHaptic('light');
  state.selectedPremMonths = months;
  state.selectedPremPrice = price;
  document.querySelectorAll('.prem-card').forEach(c => c.classList.remove('active'));
  if (el) el.classList.add('active');
}

function selectPubgPackage(uc, price, el) {
  triggerHaptic('light');
  state.selectedPubgUc = uc;
  state.selectedPubgPrice = price;
  document.querySelectorAll('.pubg-card').forEach(c => c.classList.remove('active'));
  if (el) el.classList.add('active');
}

// SUBMIT ORDERS
async function submitStarsOrder() {
  triggerHaptic('medium');
  if (!state.user.userId) {
    showToast("Telegramdan kiring!");
    return;
  }

  const amount = parseInt(document.getElementById('starsAmountInput').value) || 0;
  const target = document.getElementById('targetUsernameInput').value.trim();

  if (amount < 50) {
    showToast("Minimal miqdor 50 Stars!");
    triggerHaptic('warning');
    return;
  }
  if (!target) {
    showToast(state.isPostMode ? "Post havolasini kiriting!" : "Username kiriting!");
    triggerHaptic('warning');
    return;
  }

  try {
    const res = await fetch('/api/webapp/buy/stars', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        userId: state.user.userId,
        amount: amount,
        targetUsername: target,
        paymentMethod: state.payMethod
      })
    });

    const data = await res.json();
    if (!data.ok) {
      showToast(data.error || "Xatolik yuz berdi!");
      triggerHaptic('error');
      return;
    }

    if (data.invoice) {
      showInvoiceScreen(data);
    } else {
      triggerHaptic('success');
      showToast(data.message || "Xarid muvaffaqiyatli amalga oshirildi! ⭐");
      state.user.balance = data.newBalance;
      updateUserUI();
    }
  } catch (e) {
    showToast("Server bilan ulanishda xatolik!");
    triggerHaptic('error');
  }
}

async function submitPremiumOrder() {
  triggerHaptic('medium');
  if (!state.user.userId) {
    showToast("Telegramdan kiring!");
    return;
  }

  const target = document.getElementById('premTargetUsername').value.trim();
  if (!target) {
    showToast("Username kiriting!");
    triggerHaptic('warning');
    return;
  }

  // 1 oylik Premium admin orqali amalga oshiriladi (xuddi bot tugmalaridagi kabi)
  if (state.selectedPremMonths === 1) {
    const cleanTarget = target.startsWith('@') ? target : '@' + target;
    const adminUrl = "https://t.me/BLACK_mladshiy?text=" + encodeURIComponent("Salom! Men 1 oylik Telegram Premium sotib olmoqchiman. Qabul qiluvchi: " + cleanTarget);
    if (window.Telegram && window.Telegram.WebApp && window.Telegram.WebApp.openTelegramLink) {
      window.Telegram.WebApp.openTelegramLink(adminUrl);
    } else {
      window.open(adminUrl, '_blank');
    }
    showToast("1 oylik Premium uchun adminga yo'naltirilmoqda... 💬");
    return;
  }

  try {
    const res = await fetch('/api/webapp/buy/premium', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        userId: state.user.userId,
        months: state.selectedPremMonths,
        targetUsername: target,
        paymentMethod: state.payMethod
      })
    });

    const data = await res.json();
    if (!data.ok) {
      showToast(data.error || "Xatolik yuz berdi!");
      triggerHaptic('error');
      return;
    }

    if (data.redirectAdmin && data.adminUrl) {
      if (window.Telegram && window.Telegram.WebApp && window.Telegram.WebApp.openTelegramLink) {
        window.Telegram.WebApp.openTelegramLink(data.adminUrl);
      } else {
        window.open(data.adminUrl, '_blank');
      }
      showToast(data.message || "Adminga yo'naltirilmoqda...");
      return;
    }

    if (data.invoice) {
      showInvoiceScreen(data);
    } else {
      triggerHaptic('success');
      showToast(data.message || "Telegram Premium muvaffaqiyatli xarid qilindi! 💎");
      state.user.balance = data.newBalance;
      updateUserUI();
    }
  } catch (e) {
    showToast("Server bilan ulanishda xatolik!");
    triggerHaptic('error');
  }
}

async function submitPubgOrder() {
  triggerHaptic('medium');
  if (!state.user.userId) {
    showToast("Telegramdan kiring!");
    return;
  }

  const playerId = document.getElementById('pubgPlayerIdInput').value.trim();
  if (!playerId) {
    showToast("PUBG Player ID kiriting!");
    triggerHaptic('warning');
    return;
  }

  try {
    const orderRes = await fetch('/api/webapp/buy/pubg', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        userId: state.user.userId,
        ucAmount: state.selectedPubgUc,
        playerId: playerId,
        paymentMethod: state.payMethod
      })
    });
    const data = await orderRes.json();
    if (!data.ok) {
      showToast(data.error || "Xatolik yuz berdi!");
      triggerHaptic('error');
      return;
    }

    if (data.invoice) {
      showInvoiceScreen(data);
    } else {
      triggerHaptic('success');
      showToast(data.message || "PUBG UC yuborildi! 🎮");
      state.user.balance = data.newBalance;
      updateUserUI();
    }
  } catch (e) {
    showToast("Server bilan ulanishda xatolik!");
    triggerHaptic('error');
  }
}

// DEPOSIT FORM
function setDepositAmount(amount, el) {
  triggerHaptic('light');
  const input = document.getElementById('depositAmountInput');
  if (input) input.value = amount;
  document.querySelectorAll('#screen-deposit .pkg-chip').forEach(c => c.classList.remove('active'));
  if (el) {
    el.classList.add('active');
  } else {
    document.querySelectorAll('#screen-deposit .pkg-chip').forEach(c => {
      if (c.innerText.includes(formatMoney(amount))) {
        c.classList.add('active');
      }
    });
  }
}

async function submitDepositOrder() {
  triggerHaptic('medium');
  if (!state.user.userId) {
    showToast("Telegram orqali kiring!");
    triggerHaptic('warning');
    return;
  }

  const amount = parseInt(document.getElementById('depositAmountInput').value) || 0;
  if (amount < 5000) {
    showToast("Minimal to'ldirish summasi 5 000 so'm!");
    triggerHaptic('warning');
    return;
  }

  try {
    const res = await fetch('/api/webapp/deposit', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ userId: state.user.userId, amount: amount })
    });
    const data = await res.json();
    if (data.ok) {
      showInvoiceScreen(data);
    } else {
      showToast(data.error || "Xatolik yuz berdi!");
      triggerHaptic('error');
    }
  } catch (e) {
    showToast("Server bilan ulanishda xatolik!");
  }
}

// INVOICE & REAL-TIME COUNTDOWN
function showInvoiceScreen(data) {
  triggerHaptic('success');
  state.activeInvoice = data;

  const cardNum = data.cardNumber || state.card.cardNumber || '8600 0000 0000 0000';
  const spacedNum = cardNum.replace(/\s+/g, '').replace(/(\d{4})/g, '$1 ').trim();
  const holder = data.holderName || state.card.holderName || 'Admin';
  const amount = formatMoney(data.amount) + ' UZS';

  document.getElementById('invoiceCardNumber').innerText = spacedNum;
  document.getElementById('invoiceHolderName').innerText = holder;
  document.getElementById('invoiceAmount').innerText = amount;
  document.getElementById('warningAmountText').innerText = amount;
  document.getElementById('invoiceCardBrand').innerText = data.methodName || state.card.bankName || 'HUMOCARD';

  const now = new Date();
  const timeStr = `${pad(now.getDate())}.${pad(now.getMonth() + 1)}.${now.getFullYear()} ${pad(now.getHours())}:${pad(now.getMinutes())}`;
  document.getElementById('invoiceCreatedTime').innerText = timeStr;

  switchScreen('invoice');
  startCountdown(10 * 60);
  startPaymentPolling(data.orderId);
}

function startCountdown(totalSeconds) {
  clearInterval(state.timerInterval);
  let remainingMs = totalSeconds * 1000;
  const initialMs = remainingMs;

  const timerEl = document.getElementById('invoiceTimerDigital');
  const barEl = document.getElementById('invoiceTimerBar');

  state.timerInterval = setInterval(() => {
    remainingMs -= 100;
    if (remainingMs <= 0) {
      clearInterval(state.timerInterval);
      timerEl.innerHTML = `00:00<span class="ms">.00</span>`;
      barEl.style.width = '0%';
      showToast("Buyurtma vaqti tugadi!");
      triggerHaptic('warning');
      setTimeout(() => switchScreen('deposit'), 2000);
      return;
    }

    const mins = Math.floor(remainingMs / 60000);
    const secs = Math.floor((remainingMs % 60000) / 1000);
    const ms = Math.floor((remainingMs % 1000) / 10);

    timerEl.innerHTML = `${pad(mins)}:${pad(secs)}<span class="ms">.${pad(ms)}</span>`;
    const pct = (remainingMs / initialMs) * 100;
    barEl.style.width = pct + '%';
  }, 100);
}

function startPaymentPolling(orderId) {
  if (!orderId) return;
  const pollInterval = setInterval(async () => {
    try {
      const res = await fetch(`/api/webapp/deposit/check?orderId=${orderId}`);
      if (res.ok) {
        const data = await res.json();
        if (data.isPaid) {
          clearInterval(pollInterval);
          clearInterval(state.timerInterval);
          triggerHaptic('success');
          showToast("🎉 To'lov qabul qilindi va balansga qo'shildi!");
          fetchInitData();
          setTimeout(() => switchScreen('home'), 2000);
        }
      }
    } catch (ignored) {}
  }, 2500);
}

// LEADERBOARD
async function fetchTopData(period) {
  document.querySelectorAll('.period-tab').forEach(el => el.classList.remove('active'));
  const activeBtn = document.querySelector(`.period-tab[data-period="${period}"]`);
  if (activeBtn) activeBtn.classList.add('active');

  try {
    const query = state.user.userId ? `&userId=${state.user.userId}` : '';
    const res = await fetch(`/api/webapp/top?period=${period}${query}`);
    if (res.ok) {
      const data = await res.json();
      renderTopList(data.top || []);
      if (data.recentBuyers && data.recentBuyers.length > 0) {
        document.getElementById('recentBuyersMarquee').innerHTML = data.recentBuyers.map((b) => `👤 <span class="highlight">${escapeHtml(b)}</span>`).join(' · ');
      } else {
        document.getElementById('recentBuyersMarquee').innerText = "Hozircha xaridlar mavjud emas";
      }
    }
  } catch (e) {}
}

function switchTopPeriod(period) {
  fetchTopData(period);
}

function renderTopList(items) {
  const container = document.getElementById('topListContainer');
  if (!items || items.length === 0) {
    container.innerHTML = `<div class="empty-text-sm" style="padding: 30px;">Hozircha xaridlar mavjud emas</div>`;
    return;
  }

  const medals = ['🥇', '🥈', '🥉'];
  container.innerHTML = items.map((item, idx) => {
    const medalOrNum = idx < 3 ? `<div class="medal">${medals[idx]}</div>` : `<div class="rank-num">${idx + 1}</div>`;
    return `
      <div class="top-item ${idx < 3 ? 'rank-' + (idx + 1) : ''}">
        ${medalOrNum}
        <div class="name">${escapeHtml(item.name)}</div>
        <div class="amount">${formatMoney(item.total)} so'm</div>
      </div>
    `;
  }).join('');
}

// HISTORY
async function fetchHistoryData() {
  if (!state.user.userId) return;
  try {
    const res = await fetch(`/api/webapp/history?userId=${state.user.userId}`);
    if (res.ok) {
      const list = await res.json();
      renderHistoryList(list);
    }
  } catch (e) {}
}

function renderHistoryList(items) {
  const container = document.getElementById('historyListContainer');
  if (!items || items.length === 0) {
    container.innerHTML = `
      <div class="empty-state-box" style="margin-top: 10px;">
        <div class="sticker-emoji-xl">📜</div>
        <div class="empty-text">Tranzaksiyalar mavjud emas</div>
      </div>
    `;
    return;
  }

  container.innerHTML = items.map((item, idx) => `
    <div class="history-item-card" onclick="openReceiptModal(${idx})">
      <div>
        <div class="history-service-title">${escapeHtml(item.service)} — ${escapeHtml(item.details)}</div>
        <div class="history-date">${item.date ? item.date.substring(0, 16).replace('T', ' ') : ''}</div>
      </div>
      <div style="text-align: right;">
        <div class="history-amount">${formatMoney(item.amount)} so'm</div>
        <span class="history-status-badge ${item.status === 'COMPLETED' ? 'completed' : 'pending'}">${item.status}</span>
      </div>
    </div>
  `).join('');

  state.historyItems = items;
}

function filterHistory(status, el) {
  triggerHaptic('light');
  document.querySelectorAll('.h-tab').forEach(t => t.classList.remove('active'));
  if (el) el.classList.add('active');
  fetchHistoryData();
}

// PROMOCODE MODAL & ACTIONS
function openPromoModal() {
  triggerHaptic('medium');
  const modal = document.getElementById('promoModal');
  if (modal) {
    modal.classList.add('active');
    const input = document.getElementById('promoCodeInput');
    if (input) {
      input.value = '';
      setTimeout(() => input.focus(), 200);
    }
  }
}

function closePromoModal(e) {
  if (e && e.target && !e.target.classList.contains('glass-modal-backdrop') && !e.target.classList.contains('modal-close-btn')) {
    return;
  }
  triggerHaptic('light');
  const modal = document.getElementById('promoModal');
  if (modal) modal.classList.remove('active');
}

async function submitPromoCode() {
  triggerHaptic('medium');
  if (!state.user.userId) {
    showToast("Telegram orqali kiring!");
    triggerHaptic('warning');
    return;
  }

  const input = document.getElementById('promoCodeInput');
  const code = input ? input.value.trim().toUpperCase() : '';
  if (!code) {
    showToast("Promokodni kiriting!");
    triggerHaptic('warning');
    return;
  }

  const btn = document.getElementById('applyPromoBtn');
  if (btn) btn.disabled = true;

  try {
    const res = await fetch('/api/webapp/promocode/apply', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ userId: state.user.userId, code: code })
    });

    const data = await res.json();
    if (data.ok) {
      triggerHaptic('success');
      showToast(data.message || "🎉 Promokod faollashtirildi!");
      state.user.balance = data.newBalance;
      updateUserUI();
      closePromoModal();
    } else {
      triggerHaptic('error');
      showToast(data.error || "Xatolik yuz berdi!");
    }
  } catch (e) {
    showToast("Server bilan ulanishda xatolik!");
    triggerHaptic('error');
  } finally {
    if (btn) btn.disabled = false;
  }
}

// REFERRAL ACTIONS
let referralData = { link: '', count: 0, percent: 2 };

async function fetchReferralData() {
  if (!state.user.userId) return;
  try {
    const res = await fetch(`/api/webapp/referral?userId=${state.user.userId}`);
    if (res.ok) {
      const data = await res.json();
      referralData = data;
      const countEl = document.getElementById('refCountVal');
      const pctEl = document.getElementById('refPercentVal');
      if (countEl) countEl.innerText = (data.count || 0) + " ta";
      if (pctEl) pctEl.innerText = (data.percent || 2) + "%";
    }
  } catch (e) {}
}

function copyReferralLink() {
  triggerHaptic('success');
  const link = referralData.link || `https://t.me/GyroService_bot?start=ref_${state.user.userId || ''}`;
  copyText(link, "Referal havolasi nusxalandi! 🔗");
}

function shareReferralLink() {
  triggerHaptic('medium');
  const link = referralData.link || `https://t.me/GyroService_bot?start=ref_${state.user.userId || ''}`;
  const text = encodeURIComponent("⭐️ Telegram Stars va Premium xizmatlari eng arzon narxlarda!\nDo'stlarim uchun havola: " + link);
  const shareUrl = `https://t.me/share/url?url=${encodeURIComponent(link)}&text=${text}`;
  
  if (tg?.openTelegramLink) {
    tg.openTelegramLink(shareUrl);
  } else {
    window.open(shareUrl, '_blank');
  }
}

// UTILITIES
function copyText(text, successMsg) {
  triggerHaptic('success');
  if (navigator.clipboard) {
    navigator.clipboard.writeText(text);
  } else {
    const ta = document.createElement('textarea');
    ta.value = text;
    document.body.appendChild(ta);
    ta.select();
    document.execCommand('copy');
    document.body.removeChild(ta);
  }
  showToast(successMsg || 'Nusxalandi! 📋');
}

function showToast(msg) {
  const toast = document.getElementById('toastNotification');
  if (!toast) return;
  toast.innerText = msg;
  toast.classList.add('show');
  setTimeout(() => toast.classList.remove('show'), 2200);
}

function formatMoney(num) {
  return String(num || 0).replace(/\B(?=(\d{3})+(?!\d))/g, " ");
}

function escapeHtml(text) {
  if (!text) return '';
  return String(text).replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;");
}

// RECEIPT MODAL & ACTIONS
let currentReceiptItem = null;

function openReceiptModal(index) {
  triggerHaptic('medium');
  const items = state.historyItems || [];
  const item = items[index];
  if (!item) return;
  currentReceiptItem = item;

  const content = document.getElementById('receiptContent');
  if (content) {
    content.innerHTML = `
      <div class="receipt-row">
        <span class="receipt-lbl">Xarid ID:</span>
        <span class="receipt-val">${escapeHtml(item.id)}</span>
      </div>
      <div class="receipt-row">
        <span class="receipt-lbl">Xizmat turi:</span>
        <span class="receipt-val">${escapeHtml(item.service)}</span>
      </div>
      <div class="receipt-row">
        <span class="receipt-lbl">Tafsilot:</span>
        <span class="receipt-val">${escapeHtml(item.details)}</span>
      </div>
      <div class="receipt-divider"></div>
      <div class="receipt-row">
        <span class="receipt-lbl">To'langan summa:</span>
        <span class="receipt-val" style="color: #60a5fa; font-size: 15px;">${formatMoney(item.amount)} so'm</span>
      </div>
      <div class="receipt-row">
        <span class="receipt-lbl">Holati:</span>
        <span class="receipt-val text-green">✅ Bajarildi</span>
      </div>
      <div class="receipt-row">
        <span class="receipt-lbl">Sana va vaqt:</span>
        <span class="receipt-val" style="font-size: 12px; color: var(--text-sub);">${item.date ? item.date.substring(0, 19).replace('T', ' ') : ''}</span>
      </div>
    `;
  }

  const modal = document.getElementById('receiptModal');
  if (modal) modal.classList.add('active');
}

function closeReceiptModal(e) {
  if (e && e.target && !e.target.classList.contains('glass-modal-backdrop') && !e.target.classList.contains('modal-close-btn')) {
    return;
  }
  triggerHaptic('light');
  const modal = document.getElementById('receiptModal');
  if (modal) modal.classList.remove('active');
}

function copyReceiptDetails() {
  if (!currentReceiptItem) return;
  const text = `🧾 Elektron Xarid Kvitansiyasi\n` +
    `ID: ${currentReceiptItem.id}\n` +
    `Xizmat: ${currentReceiptItem.service} (${currentReceiptItem.details})\n` +
    `Summa: ${formatMoney(currentReceiptItem.amount)} so'm\n` +
    `Holati: Bajarildi ✅\n` +
    `Sana: ${currentReceiptItem.date ? currentReceiptItem.date.substring(0, 16).replace('T', ' ') : ''}\n` +
    `Bot: @GyroService_bot`;
  copyText(text, "Kvitansiya nusxalandi! 📋");
}

function shareReceiptDetails() {
  if (!currentReceiptItem) return;
  const text = encodeURIComponent(`🧾 Mening xarid kvitansiyam:\n` +
    `Xizmat: ${currentReceiptItem.service} (${currentReceiptItem.details})\n` +
    `Summa: ${formatMoney(currentReceiptItem.amount)} so'm\n` +
    `Muvaffaqiyatli yetkazildi! @GyroService_bot`);
  const shareUrl = `https://t.me/share/url?text=${text}`;
  if (tg?.openTelegramLink) {
    tg.openTelegramLink(shareUrl);
  } else {
    window.open(shareUrl, '_blank');
  }
}




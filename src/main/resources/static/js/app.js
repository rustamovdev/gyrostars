/**
 * GyroStars Telegram Mini App (WebApp)
 * Full Reactive UI Engine & Real-Time Spring Boot Backend Integration
 */

// -------------------------------------------------------------
// 1. STATE & CONSTANTS
// -------------------------------------------------------------
const STATE = {
  user: {
    userId: 8721841892,
    username: "Sharipov_Sh",
    fullName: "S R",
    balance: 0,
    verified: true
  },
  card: {
    cardNumber: "9860 0866 0350 6261",
    holderName: "Sharipov Sh",
    methodName: "HUMO"
  },
  adminUsername: "stalkerbek",
  customEmojis: {
    home: "5257963315258204021",
    stars: "5294475699824921631",
    account: "5256143829672672750",
    top: "6021644067111180663",
    game: "6023852878597200124",
    clock: "5258258882022612173",
    tab_stars: "5897501460509234625",
    tab_premium: "5938420017665152105",
    tab_gift: "5420255182887873220",
    tab_nft: "5150158575271674966",
    pay_card: "5296291032177085608",
    pay_balance: "5418248123195615000",
    purchase_history: "5312361253610475399",
    pubg_emoji: "5204252919565657978",
    pubg_uc: "6289399685622797036",
    freefire_emoji: "6012741811087348350",
    loading_emoji: "5206391924948249547",
    webapp_btn: "5296770844448561710"
  },
  prices: {
    starUnitPrice: 230,
    starPackages: {
      50: 11500, 100: 23000, 250: 57500, 500: 115000,
      1000: 230000, 2500: 575000
    },
    premiumPackages: {
      1: { title: "1 oy", price: 50000, discount: "50 000 UZS", popular: false, desc: "Admindan to'g'ridan-to'g'ri olish" },
      3: { title: "3 oy", price: 180000, discount: "Ommabop", popular: true, desc: "Eng ko'p tanlangan" },
      6: { title: "6 oy", price: 250000, discount: "-15%", popular: false, desc: "Tejamkor" },
      12: { title: "12 oy", price: 400000, discount: "-30%", popular: false, desc: "VIP Tarif" }
    },
    pubgPackages: {
      60: 11000, 325: 55000, 660: 110000, 1800: 275000,
      3850: 545000, 8100: 1090000
    },
    freefirePackages: {
      100: 15000, 310: 45000, 520: 75000, 1060: 150000, 2180: 300000
    }
  },
  activeTab: "home",
  homeSubTab: "stars",
  topPeriod: "today",
  historyFilter: "all",
  servicesSubTab: "pubg",
  
  // Inputs
  starsCount: 100,
  starsUsername: "",
  postStarsMode: false,
  premiumMonths: 3,
  premiumUsername: "",
  paymentMethod: "card", // "card" | "balance"
  
  // Auto Payment Flow
  activeOrder: null,
  countdownInterval: null,
  
  // History
  history: [
    { id: "ORD-9101", type: "stars", title: "⭐ 100 Stars", target: "@Sharipov_Sh", amount: 23000, status: "completed", date: "Bugun 20:52" },
    { id: "ORD-9102", type: "premium", title: "💎 3 Oylik Premium", target: "@Sharipov_Sh", amount: 180000, status: "completed", date: "Bugun 19:48" },
    { id: "ORD-9103", type: "deposit", title: "💳 Balans to'ldirish", target: "HUMO *6261", amount: 200000, status: "completed", date: "Bugun 18:56" },
    { id: "ORD-9104", type: "pubg", title: "🎮 325 PUBG UC", target: "ID: 512348912", amount: 55000, status: "pending", date: "Kecha 14:15" }
  ],
  
  // Leaderboards
  topData: {
    today: [
      { rank: 1, name: "Shaxzod", total: 1250000, isMe: false, avatar: "👑" },
      { rank: 2, name: "S R (Siz)", total: 490000, isMe: true, avatar: "⭐" },
      { rank: 3, name: "Bekzod", total: 350000, isMe: false, avatar: "🔥" },
      { rank: 4, name: "Jasur", total: 230000, isMe: false, avatar: "⚡" },
      { rank: 5, name: "Farrux", total: 180000, isMe: false, avatar: "💎" },
      { rank: 6, name: "Alisher", total: 120000, isMe: false, avatar: "🚀" }
    ],
    week: [
      { rank: 1, name: "Jasur", total: 4850000, isMe: false, avatar: "👑" },
      { rank: 2, name: "Shaxzod", total: 3450000, isMe: false, avatar: "🔥" },
      { rank: 3, name: "S R (Siz)", total: 2190000, isMe: true, avatar: "⭐" },
      { rank: 4, name: "Temur", total: 1800000, isMe: false, avatar: "⚡" }
    ],
    month: [
      { rank: 1, name: "VIP Xaridor", total: 14500000, isMe: false, avatar: "👑" },
      { rank: 2, name: "Shaxzod", total: 9800000, isMe: false, avatar: "🔥" },
      { rank: 3, name: "S R (Siz)", total: 6490000, isMe: true, avatar: "⭐" }
    ]
  },
  
  recentBuyers: [
    "Shaxzod · 500 ⭐ xarid qildi",
    "Admin · 3 oylik 💎 Premium oldi",
    "Bekzod · 660 🎮 PUBG UC oldi",
    "Jasur · 250 ⭐ xarid qildi",
    "Farrux · 100 ⭐ xarid qildi",
    "Rustam · 12 oylik 💎 VIP Premium oldi"
  ]
};

// -------------------------------------------------------------
// 2. UNIVERSAL CUSTOM EMOJI RENDERER (Lottie + WebP support)
// -------------------------------------------------------------
function renderEmoji(emojiKeyOrId, className = "w-6 h-6 inline-block") {
  const containerId = `emoji-${Math.random().toString(36).substr(2, 9)}`;
  
  // WebP formatidagi emojilar
  if (emojiKeyOrId === "tab_premium" || emojiKeyOrId === "5938420017665152105") {
    return `<img src="emojis/tab_premium.webp" alt="premium" class="${className} object-contain inline-block">`;
  }
  if (emojiKeyOrId === "pubg_emoji" || emojiKeyOrId === "5204252919565657978") {
    return `<img src="emojis/pubg_emoji.webp" alt="pubg" class="${className} object-contain inline-block">`;
  }
  if (emojiKeyOrId === "pubg_uc" || emojiKeyOrId === "6289399685622797036") {
    return `<img src="emojis/pubg_uc.webp" alt="uc" class="${className} object-contain inline-block">`;
  }
  if (emojiKeyOrId === "freefire_emoji" || emojiKeyOrId === "6012741811087348350") {
    return `<img src="emojis/freefire_emoji.webp" alt="freefire" class="${className} object-contain inline-block">`;
  }

  const jsonPath = `emojis/${emojiKeyOrId}.json`;

  setTimeout(() => {
    const el = document.getElementById(containerId);
    if (el && window.lottie) {
      try {
        window.lottie.loadAnimation({
          container: el,
          renderer: 'svg',
          loop: true,
          autoplay: true,
          path: jsonPath
        });
      } catch (e) {
        console.log("Lottie render error for", emojiKeyOrId, e);
      }
    }
  }, 25);

  return `<span id="${containerId}" class="${className} flex items-center justify-center"></span>`;
}

// -------------------------------------------------------------
// 3. INITIALIZATION & TELEGRAM SDK
// -------------------------------------------------------------
document.addEventListener("DOMContentLoaded", () => {
  initSplashScreen();
  initTelegramSDK();
  parseUrlParams();
  fetchInitialData();
  renderApp();
  startRecentBuyersTicker();
});

function initSplashScreen() {
  const splashLottie = document.getElementById("splash-lottie");
  if (splashLottie && window.lottie) {
    try {
      window.lottie.loadAnimation({
        container: splashLottie,
        renderer: 'svg',
        loop: true,
        autoplay: true,
        path: 'emojis/loading_emoji.json'
      });
    } catch (e) {
      console.log("Splash lottie error:", e);
    }
  }

  setTimeout(() => {
    const splash = document.getElementById("splash-screen");
    if (splash) {
      splash.style.opacity = "0";
      splash.style.transform = "scale(1.05)";
      splash.style.pointerEvents = "none";
      setTimeout(() => splash.remove(), 450);
    }
  }, 1150);
}

function initTelegramSDK() {
  const tg = window.Telegram?.WebApp;
  if (tg) {
    tg.ready();
    tg.expand();
    try {
      tg.enableClosingConfirmation();
    } catch (e) {}
    
    // Foydalanuvchi ma'lumotlarini olish
    const user = tg.initDataUnsafe?.user;
    if (user) {
      STATE.user.userId = user.id;
      STATE.user.username = user.username || user.first_name || "User";
      STATE.user.fullName = [user.first_name, user.last_name].filter(Boolean).join(" ") || "S R";
      if (!STATE.starsUsername) STATE.starsUsername = user.username ? `@${user.username}` : "";
      if (!STATE.premiumUsername) STATE.premiumUsername = user.username ? `@${user.username}` : "";
    }
  }
}

function parseUrlParams() {
  const params = new URLSearchParams(window.location.search);
  if (params.get("userId")) STATE.user.userId = parseInt(params.get("userId"));
  if (params.get("username")) STATE.user.username = params.get("username");
  if (params.get("name")) STATE.user.fullName = params.get("name");
  if (params.get("balance")) STATE.user.balance = parseInt(params.get("balance"));
}

async function fetchInitialData() {
  try {
    const res = await fetch("/api/webapp/init?userId=" + STATE.user.userId);
    if (res.ok) {
      const data = await res.json();
      if (data.user) {
        STATE.user.balance = data.user.balance ?? STATE.user.balance;
        if (data.user.fullName) STATE.user.fullName = data.user.fullName;
        if (data.user.username) STATE.user.username = data.user.username;
      }
      if (data.card) STATE.card = data.card;
      if (data.prices) STATE.prices = { ...STATE.prices, ...data.prices };
      if (data.history && data.history.length > 0) STATE.history = data.history;
      updateHeader();
      if (STATE.activeTab === "home" || STATE.activeTab === "account") {
        renderActiveTab();
      }
    }
  } catch (e) {
    console.log("Local standalone mode running:", e);
  }
}

// -------------------------------------------------------------
// 4. CORE RENDERING ENGINE
// -------------------------------------------------------------
function renderApp() {
  updateHeader();
  renderNavigation();
  renderActiveTab();
}

function updateHeader() {
  const avatarEl = document.getElementById("header-avatar");
  const nameEl = document.getElementById("header-name");
  const idEl = document.getElementById("header-id");
  const balanceEl = document.getElementById("header-balance");

  if (avatarEl) {
    const initials = (STATE.user.fullName || "S R").split(" ").map(n => n[0]).slice(0, 2).join("").toUpperCase();
    avatarEl.innerText = initials || "SR";
  }
  if (nameEl) nameEl.innerText = STATE.user.fullName || "S R";
  if (idEl) idEl.innerText = "ID: " + STATE.user.userId;
  if (balanceEl) balanceEl.innerText = formatNumber(STATE.user.balance) + " so'm";
}

function renderNavigation() {
  const tabs = [
    { id: "home", label: "Главная", emojiKey: "home" },
    { id: "top", label: "Топ", emojiKey: "top" },
    { id: "services", label: "Services", emojiKey: "game" },
    { id: "account", label: "Аккаунт", emojiKey: "account" },
    { id: "history", label: "История", emojiKey: "clock" }
  ];

  const navEl = document.getElementById("bottom-nav");
  if (!navEl) return;

  navEl.innerHTML = tabs.map(t => {
    const isActive = STATE.activeTab === t.id;
    return `
      <button onclick="switchTab('${t.id}')" 
              class="flex flex-col items-center justify-center flex-1 py-1.5 transition-all duration-300 relative ${isActive ? 'text-blue-400 font-extrabold scale-105' : 'text-slate-400 hover:text-slate-200'}">
        <span class="w-6 h-6 flex items-center justify-center mb-0.5 transition-transform duration-300 ${isActive ? 'scale-110' : ''}">${renderEmoji(t.emojiKey, 'w-6 h-6')}</span>
        <span class="text-[11px] leading-tight tracking-tight">${t.label}</span>
        ${isActive ? '<span class="absolute -bottom-1 w-6 h-1 bg-gradient-to-r from-blue-600 to-cyan-400 rounded-full shadow-[0_0_10px_rgba(59,130,246,0.9)] animate-pulse"></span>' : ''}
      </button>
    `;
  }).join("");
}

function switchTab(tabId) {
  triggerHaptic("selection");
  STATE.activeTab = tabId;
  renderNavigation();
  renderActiveTab();
  window.scrollTo({ top: 0, behavior: "smooth" });
}

function renderActiveTab() {
  const container = document.getElementById("main-content");
  if (!container) return;

  switch (STATE.activeTab) {
    case "home":
      container.innerHTML = renderHomeTab();
      break;
    case "top":
      container.innerHTML = renderTopTab();
      break;
    case "services":
      container.innerHTML = renderServicesTab();
      break;
    case "account":
      container.innerHTML = renderAccountTab();
      break;
    case "history":
      container.innerHTML = renderHistoryTab();
      break;
  }
}

// -------------------------------------------------------------
// 5. TAB 1: ГЛАВНАЯ (4 SUBTABS: STARS, PREMIUM, GIFT, NFT)
// -------------------------------------------------------------
function renderHomeTab() {
  const subTabs = [
    { id: "stars", label: "Stars", emojiKey: "tab_stars" },
    { id: "premium", label: "Premium", emojiKey: "tab_premium" },
    { id: "gift", label: "Gift", emojiKey: "tab_gift" },
    { id: "nft", label: "NFT", emojiKey: "tab_nft" }
  ];

  return `
    <div class="animate-fade-in space-y-4">
      <!-- Top 4 Sub-Tabs Switcher with Liquid Glow -->
      <div class="glass-card p-1.5 flex gap-1.5 rounded-2xl bg-slate-900/90 shadow-lg">
        ${subTabs.map(sub => {
          const isActive = STATE.homeSubTab === sub.id;
          return `
            <button onclick="switchHomeSubTab('${sub.id}')"
                    class="flex-1 py-2 rounded-xl text-xs font-black transition-all duration-300 flex items-center justify-center gap-1.5 ${isActive ? 'bg-gradient-to-r from-blue-600 to-blue-500 text-white shadow-[0_4px_16px_rgba(37,99,235,0.5)] scale-[1.03]' : 'text-slate-400 hover:text-slate-200 hover:bg-slate-800/40'}">
              <span class="w-5 h-5 flex items-center justify-center">${renderEmoji(sub.emojiKey, 'w-5 h-5')}</span>
              <span class="tracking-tight">${sub.label}</span>
            </button>
          `;
        }).join("")}
      </div>

      <!-- Active SubTab Content -->
      ${STATE.homeSubTab === "stars" ? renderStarsSubTab() : ""}
      ${STATE.homeSubTab === "premium" ? renderPremiumSubTab() : ""}
      ${STATE.homeSubTab === "gift" ? renderComingSoon('gift') : ""}
      ${STATE.homeSubTab === "nft" ? renderComingSoon('nft') : ""}
    </div>
  `;
}

function switchHomeSubTab(subId) {
  triggerHaptic("selection");
  STATE.homeSubTab = subId;
  renderActiveTab();
}

function renderStarsSubTab() {
  const currentPrice = STATE.starsCount * STATE.prices.starUnitPrice;
  const presets = [50, 100, 250, 500, 1000, 2500];

  return `
    <div class="space-y-4">
      <!-- Title & Post Stars Mode Toggle -->
      <div class="glass-card p-4 flex items-center justify-between">
        <div class="flex items-center gap-2.5">
          <div class="w-8 h-8 flex items-center justify-center">
            ${renderEmoji('stars', 'w-8 h-8')}
          </div>
          <div>
            <h2 class="text-base font-extrabold text-white flex items-center gap-1">
              Telegram Stars
            </h2>
            <p class="text-xs text-slate-400 mt-0.5">Avtomatik va 1 daqiqada yetkazish</p>
          </div>
        </div>
        <button onclick="togglePostStarsMode()" 
                class="px-3 py-1.5 rounded-full text-xs font-bold transition-all ${STATE.postStarsMode ? 'bg-blue-600 text-white shadow-lg shadow-blue-500/30' : 'bg-slate-800 text-slate-300 border border-slate-700'}">
          ${STATE.postStarsMode ? '✅ Postga Stars' : 'Postga Stars'}
        </button>
      </div>

      <!-- Quantity Selector Card -->
      <div class="glass-card p-4 space-y-3">
        <div class="flex items-center justify-between text-xs text-slate-400">
          <span>Stars miqdori (50 - 2500):</span>
          <span class="text-blue-400 font-bold">${formatNumber(STATE.prices.starUnitPrice)} so'm / dona</span>
        </div>

        <div class="relative flex items-center">
          <input type="number" id="stars-input" value="${STATE.starsCount}" min="50" max="2500"
                 oninput="onStarsInput(this.value)"
                 class="w-full bg-slate-900/90 border border-slate-700 focus:border-blue-500 rounded-xl px-4 py-3 text-lg font-black text-white outline-none transition-all pr-12">
          <span class="absolute right-3.5 w-6 h-6 flex items-center justify-center">
            ${renderEmoji('stars', 'w-6 h-6')}
          </span>
        </div>

        <!-- Quick Chips -->
        <div class="grid grid-cols-6 gap-1.5">
          ${presets.map(p => `
            <button onclick="selectStarsPreset(${p})" 
                    class="py-1.5 rounded-lg text-xs font-bold transition-all ${STATE.starsCount === p ? 'bg-blue-600 text-white shadow-md shadow-blue-600/30' : 'bg-slate-800/80 text-slate-300 border border-slate-700 hover:border-slate-600'}">
              ${p}
            </button>
          `).join("")}
        </div>

        <!-- Total Price Banner -->
        <div class="bg-blue-950/40 border border-blue-800/50 rounded-xl p-3 flex items-center justify-between">
          <span class="text-xs text-slate-300 font-medium">To'lanadigan summa:</span>
          <span class="text-base font-black text-blue-400">${formatNumber(currentPrice)} so'm</span>
        </div>
      </div>

      <!-- Username / Target Input with 1-Click Auto Fill Person Button -->
      <div class="glass-card p-4 space-y-2">
        <div class="flex items-center justify-between">
          <label class="text-xs font-bold text-slate-300">
            ${STATE.postStarsMode ? 'Telegram Post Havolasi (Link):' : 'Qabul qiluvchi Telegram Username:'}
          </label>
          ${!STATE.postStarsMode ? `
            <button onclick="fillMyUsername('stars')" 
                    class="text-[11px] font-bold text-blue-400 hover:text-blue-300 flex items-center gap-1 bg-blue-950/60 border border-blue-800/60 px-2.5 py-1 rounded-lg transition-all active:scale-95">
              <span>👤</span>
              <span>O'zimga</span>
            </button>
          ` : ''}
        </div>
        <div class="relative flex items-center">
          <input type="text" id="stars-target-input" 
                 value="${STATE.starsUsername}" 
                 placeholder="${STATE.postStarsMode ? 'https://t.me/kanal/123' : '@username'}"
                 oninput="STATE.starsUsername = this.value"
                 class="w-full bg-slate-900/90 border border-slate-700 focus:border-blue-500 rounded-xl px-4 py-3 text-sm font-semibold text-white outline-none transition-all pr-12">
          ${!STATE.postStarsMode ? `
            <button onclick="fillMyUsername('stars')" title="O'zimning usernamemni kiritish"
                    class="absolute right-2 w-8 h-8 rounded-lg bg-blue-600/20 hover:bg-blue-600/40 border border-blue-500/40 text-blue-400 flex items-center justify-center transition-all active:scale-90 shadow-sm">
              <span class="w-5 h-5 flex items-center justify-center">${renderEmoji('account', 'w-5 h-5')}</span>
            </button>
          ` : '<span class="absolute right-3.5 text-slate-400 text-sm">🔗</span>'}
        </div>
      </div>

      <!-- Payment Method Selector -->
      ${renderPaymentMethodSelector(currentPrice)}

      <!-- Submit Button -->
      <button onclick="submitStarsOrder()" class="btn-primary w-full py-4 text-sm tracking-wide shadow-lg shadow-blue-600/35">
        <span>Создать заказ</span>
        <span>•</span>
        <span>${formatNumber(currentPrice)} so'm</span>
      </button>
    </div>
  `;
}

function onStarsInput(val) {
  let num = parseInt(val) || 0;
  STATE.starsCount = num;
  const currentPrice = num * STATE.prices.starUnitPrice;
  const submitBtn = document.querySelector(".btn-primary");
  if (submitBtn) {
    submitBtn.innerHTML = `<span>Создать заказ</span><span>•</span><span>${formatNumber(currentPrice)} so'm</span>`;
  }
}

function selectStarsPreset(val) {
  triggerHaptic("light");
  STATE.starsCount = val;
  renderActiveTab();
}

function togglePostStarsMode() {
  triggerHaptic("medium");
  STATE.postStarsMode = !STATE.postStarsMode;
  renderActiveTab();
}

function renderPremiumSubTab() {
  const pkgs = STATE.prices.premiumPackages;
  const currentPrice = pkgs[STATE.premiumMonths]?.price || 180000;

  return `
    <div class="space-y-4">
      <!-- Promo / Tickets Top Banner -->
      <div class="glass-card p-3.5 flex items-center justify-between bg-gradient-to-r from-purple-950/40 to-slate-900">
        <div class="flex items-center gap-2">
          <span class="text-xl">🎟</span>
          <div>
            <div class="text-xs font-extrabold text-white">Chiptalarim & Promokod</div>
            <div class="text-[11px] text-purple-300">Har bir xarid uchun bonus oling</div>
          </div>
        </div>
        <button onclick="openPromocodeModal()" class="px-3 py-1.5 bg-purple-600/80 hover:bg-purple-600 text-white rounded-xl text-xs font-bold transition-all">
          Kiritish
        </button>
      </div>

      <!-- 2x2 Package Cards Grid -->
      <div class="grid grid-cols-2 gap-2.5">
        ${Object.keys(pkgs).map(monthsKey => {
          const m = parseInt(monthsKey);
          const pkg = pkgs[m];
          const isSelected = STATE.premiumMonths === m;
          return `
            <div onclick="selectPremiumMonths(${m})" 
                 class="glass-card-interactive p-4 relative flex flex-col justify-between min-h-[120px] ${isSelected ? 'card-selected' : ''}">
              ${pkg.discount ? `<span class="absolute top-2.5 right-2.5 bg-blue-600 text-[10px] font-black px-2 py-0.5 rounded-full text-white shadow-md">${pkg.discount}</span>` : ''}
              <div>
                <div class="flex items-center gap-2">
                  <span class="w-6 h-6 flex items-center justify-center">${renderEmoji('tab_premium', 'w-6 h-6')}</span>
                  <span class="text-sm font-black text-white">${pkg.title}</span>
                </div>
                <p class="text-[11px] text-slate-400 mt-1">${pkg.desc}</p>
              </div>
              <div class="mt-3">
                <span class="text-base font-black text-blue-400">${formatNumber(pkg.price)}</span>
                <span class="text-[11px] text-slate-400"> so'm</span>
              </div>
            </div>
          `;
        }).join("")}
      </div>

      ${STATE.premiumMonths === 1 ? `
        <!-- 1 Month Direct Admin Contact Banner -->
        <div class="glass-card p-4 flex items-center justify-between bg-gradient-to-r from-blue-950/60 to-indigo-950/50 border border-blue-500/40">
          <div>
            <div class="text-xs font-black text-white">💎 1 oylik Telegram Premium — 50 000 UZS</div>
            <div class="text-[11px] text-blue-300 mt-0.5">Admin orqali tezkor va kafolatli yetkaziladi</div>
          </div>
          <button onclick="contactAdminForPremium()" class="px-3.5 py-2 bg-blue-600 hover:bg-blue-500 text-white rounded-xl text-xs font-black transition-all shadow-md">
            Adminga yozish 💬
          </button>
        </div>
      ` : `
        <!-- Username Input with Auto-Fill Button -->
        <div class="glass-card p-4 space-y-2">
          <div class="flex items-center justify-between">
            <label class="text-xs font-bold text-slate-300">Qabul qiluvchi Telegram Username:</label>
            <button onclick="fillMyUsername('premium')" 
                    class="text-[11px] font-bold text-blue-400 hover:text-blue-300 flex items-center gap-1 bg-blue-950/60 border border-blue-800/60 px-2.5 py-1 rounded-lg transition-all active:scale-95">
              <span>👤</span>
              <span>O'zimga</span>
            </button>
          </div>
          <div class="relative flex items-center">
            <input type="text" id="premium-target-input" 
                   value="${STATE.premiumUsername}" 
                   placeholder="@username"
                   oninput="STATE.premiumUsername = this.value"
                   class="w-full bg-slate-900/90 border border-slate-700 focus:border-blue-500 rounded-xl px-4 py-3 text-sm font-semibold text-white outline-none transition-all pr-12">
            <button onclick="fillMyUsername('premium')" title="O'zimning usernamemni kiritish"
                    class="absolute right-2 w-8 h-8 rounded-lg bg-blue-600/20 hover:bg-blue-600/40 border border-blue-500/40 text-blue-400 flex items-center justify-center transition-all active:scale-90 shadow-sm">
              <span class="w-5 h-5 flex items-center justify-center">${renderEmoji('account', 'w-5 h-5')}</span>
            </button>
          </div>
        </div>

        <!-- Payment Method Selector -->
        ${renderPaymentMethodSelector(currentPrice)}
      `}

      <!-- Submit Button -->
      <button onclick="submitPremiumOrder()" class="btn-primary w-full py-4 text-sm tracking-wide shadow-lg shadow-blue-600/35">
        ${STATE.premiumMonths === 1 ? `
          <span>💬 Admindan olish (50 000 UZS)</span>
        ` : `
          <span>Создать заказ</span>
          <span>•</span>
          <span>${formatNumber(currentPrice)} so'm</span>
        `}
      </button>
    </div>
  `;
}

function selectPremiumMonths(m) {
  triggerHaptic("light");
  STATE.premiumMonths = m;
  renderActiveTab();
}

function contactAdminForPremium() {
  triggerHaptic("medium");
  const url = `https://t.me/${STATE.adminUsername}?text=Salom!%20Men%201%20oylik%20Telegram%20Premium%20(50.000%20UZS)%20sotib%20olmoqchiman.`;
  const tg = window.Telegram?.WebApp;
  if (tg && tg.openTelegramLink) {
    tg.openTelegramLink(url);
  } else {
    window.open(url, "_blank");
  }
}

// -------------------------------------------------------------
// COMING SOON SCREEN (GIFT & NFT BO'LIMLARI UCHUN)
// -------------------------------------------------------------
function renderComingSoon(type) {
  const isGift = type === "gift";
  const title = isGift ? "Telegram Giftlar Tizimi" : "Telegram NFT Tizimi";
  const emojiKey = isGift ? "tab_gift" : "tab_nft";
  const desc = isGift 
    ? "Telegram rasmiy sovg'alarini Stars orqali avtomatik sotib olish va do'stlaringizga yuborish imkoniyati ustida ish olib borilmoqda."
    : "Eksklyuziv cheklangan Telegram NFT va kolleksiya kartalari savdosi tez kunlarda ushbu bo'limda taqdim etiladi.";

  return `
    <div class="glass-card p-6 flex flex-col items-center justify-center text-center space-y-4 rounded-3xl bg-gradient-to-b from-slate-900 to-blue-950/40 shadow-xl border border-slate-700/80 my-2">
      <!-- Floating Badge & Emoji -->
      <div class="relative w-24 h-24 rounded-full bg-blue-600/15 border border-blue-500/30 flex items-center justify-center shadow-lg shadow-blue-500/10">
        <div class="w-14 h-14 flex items-center justify-center animate-float">
          ${renderEmoji(emojiKey, 'w-14 h-14')}
        </div>
        <span class="absolute -top-1.5 -right-1 px-2 py-0.5 bg-gradient-to-r from-amber-500 to-amber-600 text-white text-[9px] font-black uppercase rounded-full shadow-md">
          Soon
        </span>
      </div>

      <div class="space-y-1.5 max-w-xs">
        <span class="inline-block px-3 py-1 bg-blue-950/80 border border-blue-500/40 text-blue-400 text-xs font-black rounded-full">
          🚀 Tez orada ishga tushadi!
        </span>
        <h3 class="text-base font-black text-white pt-1">${title}</h3>
        <p class="text-xs text-slate-300 leading-relaxed">${desc}</p>
      </div>

      <!-- Action button to return to Stars -->
      <button onclick="switchHomeSubTab('stars')" 
              class="btn-primary px-6 py-3 text-xs font-extrabold shadow-md shadow-blue-600/30">
        <span>⭐ Stars bo'limiga o'tish</span>
      </button>
    </div>
  `;
}

function renderPaymentMethodSelector(amount) {
  const isBalanceEnough = STATE.user.balance >= amount;
  const isCard = STATE.paymentMethod === 'card';
  const isBalance = STATE.paymentMethod === 'balance';

  return `
    <div class="glass-card p-4 space-y-2.5">
      <div class="flex items-center justify-between">
        <span class="text-xs font-bold text-slate-300">To'lov turini tanlang:</span>
        <span class="text-[10px] font-semibold text-blue-400">2 ta usul</span>
      </div>
      <div class="grid grid-cols-2 gap-2.5">
        <!-- 1. Karta orqali (HUMO / Uzcard) -->
        <button onclick="selectPaymentMethod('card')" 
                class="relative p-3.5 rounded-2xl border flex flex-col items-start justify-between min-h-[90px] transition-all duration-300 ${isCard ? 'bg-blue-950/70 border-blue-500 text-white shadow-lg shadow-blue-500/25 ring-1 ring-blue-500/50 scale-[1.02]' : 'bg-slate-900/70 border-slate-700/80 text-slate-400 hover:border-slate-600'}">
          <div class="w-full flex items-center justify-between">
            <div class="w-7 h-7 flex items-center justify-center">
              ${renderEmoji('pay_card', 'w-7 h-7')}
            </div>
            <div class="w-4 h-4 rounded-full border flex items-center justify-center ${isCard ? 'border-blue-500 bg-blue-600' : 'border-slate-600'}">
              ${isCard ? '<span class="w-1.5 h-1.5 bg-white rounded-full"></span>' : ''}
            </div>
          </div>
          <div class="mt-2 text-left">
            <div class="text-xs font-black ${isCard ? 'text-white' : 'text-slate-200'}">Karta orqali</div>
            <span class="text-[10px] text-slate-400 font-medium">HUMO / Uzcard (Avto)</span>
          </div>
        </button>

        <!-- 2. Balans orqali (Naxt / Bot balansi) -->
        <button onclick="selectPaymentMethod('balance')" 
                class="relative p-3.5 rounded-2xl border flex flex-col items-start justify-between min-h-[90px] transition-all duration-300 ${isBalance ? 'bg-blue-950/70 border-blue-500 text-white shadow-lg shadow-blue-500/25 ring-1 ring-blue-500/50 scale-[1.02]' : 'bg-slate-900/70 border-slate-700/80 text-slate-400 hover:border-slate-600'}">
          <div class="w-full flex items-center justify-between">
            <div class="w-7 h-7 flex items-center justify-center">
              ${renderEmoji('pay_balance', 'w-7 h-7')}
            </div>
            <div class="w-4 h-4 rounded-full border flex items-center justify-center ${isBalance ? 'border-blue-500 bg-blue-600' : 'border-slate-600'}">
              ${isBalance ? '<span class="w-1.5 h-1.5 bg-white rounded-full"></span>' : ''}
            </div>
          </div>
          <div class="mt-2 text-left">
            <div class="text-xs font-black ${isBalance ? 'text-white' : 'text-slate-200'}">Balans orqali</div>
            <span class="text-[10px] font-bold ${isBalanceEnough ? 'text-emerald-400' : 'text-amber-400'}">
              ${formatNumber(STATE.user.balance)} so'm
            </span>
          </div>
        </button>
      </div>
    </div>
  `;
}

function selectPaymentMethod(method) {
  triggerHaptic("selection");
  STATE.paymentMethod = method;
  renderActiveTab();
}

// -------------------------------------------------------------
// 6. TAB 2: ТОП ПОЛЬЗОВАТЕЛЕЙ (LEADERBOARD)
// -------------------------------------------------------------
function renderTopTab() {
  const users = STATE.topData[STATE.topPeriod] || STATE.topData.today;
  const top1 = users[0];
  const top2 = users[1];
  const top3 = users[2];
  const rest = users.slice(3);

  return `
    <div class="animate-fade-in space-y-4">
      <!-- Header Banner with Custom Animated Top Emoji -->
      <div class="glass-card p-4 flex items-center gap-3 bg-gradient-to-r from-amber-950/40 to-slate-900 shadow-md">
        <div class="w-8 h-8 flex items-center justify-center">
          ${renderEmoji('top', 'w-8 h-8')}
        </div>
        <div>
          <h2 class="text-base font-extrabold text-white">Топ пользователей</h2>
          <p class="text-xs text-slate-400">Eng ko'p xarid qilgan yetakchilar ro'yxati</p>
        </div>
      </div>

      <!-- Period Selector -->
      <div class="glass-card p-1.5 flex gap-1.5 rounded-2xl bg-slate-900/90">
        ${[
          { id: "today", label: "Сегодня" },
          { id: "week", label: "Неделя" },
          { id: "month", label: "Месяц" }
        ].map(p => `
          <button onclick="switchTopPeriod('${p.id}')"
                  class="flex-1 py-2 rounded-xl text-xs font-bold transition-all ${STATE.topPeriod === p.id ? 'bg-blue-600 text-white shadow-md shadow-blue-600/30' : 'text-slate-400 hover:text-slate-200'}">
            ${p.label}
          </button>
        `).join("")}
      </div>

      <!-- Top Podium (1st, 2nd, 3rd) -->
      <div class="grid grid-cols-3 gap-2 items-end pt-4 pb-2 px-1">
        <!-- 2nd Place -->
        ${top2 ? `
          <div class="glass-card p-3 flex flex-col items-center text-center rounded-2xl border-slate-600 bg-slate-900/90 relative">
            <span class="text-2xl -mt-5 mb-1">🥈</span>
            <div class="w-10 h-10 rounded-full bg-slate-700 flex items-center justify-center text-sm font-black text-white border-2 border-slate-400">
              ${top2.avatar || '2'}
            </div>
            <div class="text-xs font-extrabold text-white mt-1.5 truncate max-w-full">${top2.name}</div>
            <div class="text-[11px] font-black text-blue-400 mt-0.5">${formatNumber(top2.total)}</div>
            <span class="text-[9px] text-slate-400">so'm</span>
          </div>
        ` : '<div></div>'}

        <!-- 1st Place (Highest) -->
        ${top1 ? `
          <div class="glass-card p-3.5 flex flex-col items-center text-center rounded-2xl border-amber-500/60 bg-gradient-to-b from-amber-950/40 to-slate-900 relative shadow-lg shadow-amber-500/10">
            <span class="text-3xl -mt-7 mb-1 animate-float">👑</span>
            <div class="w-12 h-12 rounded-full bg-amber-500/20 flex items-center justify-center text-base font-black text-amber-300 border-2 border-amber-400">
              ${top1.avatar || '1'}
            </div>
            <div class="text-xs font-black text-amber-300 mt-1.5 truncate max-w-full">${top1.name}</div>
            <div class="text-xs font-black text-amber-400 mt-0.5">${formatNumber(top1.total)}</div>
            <span class="text-[9px] text-slate-400">so'm</span>
          </div>
        ` : '<div></div>'}

        <!-- 3rd Place -->
        ${top3 ? `
          <div class="glass-card p-3 flex flex-col items-center text-center rounded-2xl border-amber-700/60 bg-slate-900/90 relative">
            <span class="text-2xl -mt-5 mb-1">🥉</span>
            <div class="w-10 h-10 rounded-full bg-amber-900/30 flex items-center justify-center text-sm font-black text-amber-600 border-2 border-amber-700">
              ${top3.avatar || '3'}
            </div>
            <div class="text-xs font-extrabold text-white mt-1.5 truncate max-w-full">${top3.name}</div>
            <div class="text-[11px] font-black text-blue-400 mt-0.5">${formatNumber(top3.total)}</div>
            <span class="text-[9px] text-slate-400">so'm</span>
          </div>
        ` : '<div></div>'}
      </div>

      <!-- Rest of Users List -->
      <div class="glass-card p-3 space-y-2">
        <h4 class="text-xs font-bold text-slate-400 px-2">Reyting jadvali</h4>
        <div class="space-y-1.5">
          ${rest.map(u => `
            <div class="flex items-center justify-between p-2.5 rounded-xl ${u.isMe ? 'bg-blue-950/50 border border-blue-600/40' : 'bg-slate-900/60 border border-slate-800'}">
              <div class="flex items-center gap-3">
                <span class="w-5 text-center text-xs font-black text-slate-400">#${u.rank}</span>
                <span class="text-base">${u.avatar || '👤'}</span>
                <span class="text-xs font-bold ${u.isMe ? 'text-blue-300' : 'text-white'}">${u.name}</span>
              </div>
              <span class="text-xs font-black text-blue-400">${formatNumber(u.total)} so'm</span>
            </div>
          `).join("")}
        </div>
      </div>

      <!-- Live Recent Buyers Ticker -->
      <div class="glass-card p-3.5 space-y-2 bg-gradient-to-r from-slate-900 to-blue-950/30">
        <div class="flex items-center gap-2 text-xs font-bold text-slate-300">
          <span class="relative flex h-2 w-2">
            <span class="animate-ping absolute inline-flex h-full w-full rounded-full bg-emerald-400 opacity-75"></span>
            <span class="relative inline-flex rounded-full h-2 w-2 bg-emerald-500"></span>
          </span>
          <span>So'nggi faol xaridorlar</span>
        </div>
        <div id="ticker-box" class="text-xs font-semibold text-slate-400 transition-all duration-500 py-1 px-2 bg-slate-900/80 rounded-lg border border-slate-800">
          ${STATE.recentBuyers[0]}
        </div>
      </div>
    </div>
  `;
}

function switchTopPeriod(p) {
  triggerHaptic("selection");
  STATE.topPeriod = p;
  renderActiveTab();
}

function startRecentBuyersTicker() {
  let idx = 0;
  setInterval(() => {
    const box = document.getElementById("ticker-box");
    if (box && STATE.recentBuyers.length > 0) {
      idx = (idx + 1) % STATE.recentBuyers.length;
      box.style.opacity = 0;
      setTimeout(() => {
        box.innerText = STATE.recentBuyers[idx];
        box.style.opacity = 1;
      }, 250);
    }
  }, 3500);
}

// -------------------------------------------------------------
// 7. TAB 3: SERVICES (O'YINLAR: PUBG & FREE FIRE)
// -------------------------------------------------------------
function renderServicesTab() {
  return `
    <div class="animate-fade-in space-y-4">
      <div class="glass-card p-4 flex items-center gap-3 bg-gradient-to-r from-blue-950/40 to-slate-900 shadow-md">
        <div class="w-8 h-8 flex items-center justify-center">
          ${renderEmoji('game', 'w-8 h-8')}
        </div>
        <div>
          <h2 class="text-base font-extrabold text-white">O'yinlar va Xizmatlar</h2>
          <p class="text-xs text-slate-400">PUBG Mobile UC, FreeFire va boshqa servislar</p>
        </div>
      </div>

      <!-- Games Switcher with Custom WebP Game Emojis -->
      <div class="glass-card p-1.5 flex gap-1.5 rounded-2xl bg-slate-900/90 shadow-lg">
        <button onclick="switchServicesSubTab('pubg')"
                class="flex-1 py-2.5 rounded-xl text-xs font-black transition-all duration-300 flex items-center justify-center gap-2 ${STATE.servicesSubTab === 'pubg' ? 'bg-gradient-to-r from-blue-600 to-blue-500 text-white shadow-md shadow-blue-600/40 scale-[1.02]' : 'text-slate-400 hover:text-slate-200'}">
          <span class="w-6 h-6 flex items-center justify-center">${renderEmoji('pubg_emoji', 'w-6 h-6')}</span>
          <span>PUBG Mobile</span>
        </button>
        <button onclick="switchServicesSubTab('freefire')"
                class="flex-1 py-2.5 rounded-xl text-xs font-black transition-all duration-300 flex items-center justify-center gap-2 ${STATE.servicesSubTab === 'freefire' ? 'bg-gradient-to-r from-blue-600 to-blue-500 text-white shadow-md shadow-blue-600/40 scale-[1.02]' : 'text-slate-400 hover:text-slate-200'}">
          <span class="w-6 h-6 flex items-center justify-center">${renderEmoji('freefire_emoji', 'w-6 h-6')}</span>
          <span>Free Fire</span>
        </button>
      </div>

      ${STATE.servicesSubTab === 'pubg' ? renderPubgSection() : renderFreeFireSection()}

      <!-- Service Status Card -->
      <div class="glass-card p-4 flex items-center gap-3 border-dashed border-slate-700 bg-slate-900/40">
        <span class="text-xl text-slate-400">ℹ️</span>
        <div class="text-xs text-slate-400">
          O'yin hisobini to'ldirish uchun Player ID raqamini to'g'ri kiritishingiz talab etiladi. To'lov 1-5 daqiqada hisobingizga tushadi.
        </div>
      </div>
    </div>
  `;
}

function switchServicesSubTab(tab) {
  triggerHaptic("selection");
  STATE.servicesSubTab = tab;
  renderActiveTab();
}

function renderPubgSection() {
  const pkgs = STATE.prices.pubgPackages;
  return `
    <div class="space-y-3">
      <div class="glass-card p-4 space-y-2">
        <label class="text-xs font-bold text-slate-300">PUBG Player ID (Raqamli ID):</label>
        <input type="number" id="pubg-player-id" placeholder="Masalan: 512348912"
               class="w-full bg-slate-900/90 border border-slate-700 focus:border-blue-500 rounded-xl px-4 py-3 text-sm font-semibold text-white outline-none">
      </div>

      <div class="grid grid-cols-2 gap-2.5">
        ${Object.keys(pkgs).map(uc => `
          <div onclick="orderPubg(${uc}, ${pkgs[uc]})" 
               class="glass-card-interactive p-3.5 flex flex-col justify-between rounded-2xl">
            <div class="flex items-center gap-2">
              <span class="w-6 h-6 flex items-center justify-center">${renderEmoji('pubg_uc', 'w-6 h-6')}</span>
              <span class="text-sm font-black text-white">${uc} UC</span>
            </div>
            <div class="mt-2 text-sm font-black text-blue-400">${formatNumber(pkgs[uc])} so'm</div>
          </div>
        `).join("")}
      </div>
    </div>
  `;
}

function renderFreeFireSection() {
  return `
    <div class="glass-card p-6 flex flex-col items-center justify-center text-center space-y-4 rounded-3xl bg-gradient-to-b from-slate-900 to-blue-950/40 shadow-xl border border-slate-700/80 my-2">
      <!-- Floating Badge & Free Fire Custom Emoji -->
      <div class="relative w-24 h-24 rounded-full bg-amber-500/15 border border-amber-500/30 flex items-center justify-center shadow-lg shadow-amber-500/10">
        <div class="w-14 h-14 flex items-center justify-center animate-float">
          ${renderEmoji('freefire_emoji', 'w-14 h-14')}
        </div>
        <span class="absolute -top-1.5 -right-1 px-2 py-0.5 bg-gradient-to-r from-amber-500 to-amber-600 text-white text-[9px] font-black uppercase rounded-full shadow-md">
          Soon
        </span>
      </div>

      <div class="space-y-1.5 max-w-xs">
        <span class="inline-block px-3 py-1 bg-amber-950/80 border border-amber-500/40 text-amber-400 text-xs font-black rounded-full">
          🚀 Tez orada ishga tushadi!
        </span>
        <h3 class="text-base font-black text-white pt-1">Free Fire Olmoslar Xizmati</h3>
        <p class="text-xs text-slate-300 leading-relaxed">
          Free Fire o'yin hisobini to'g'ridan-to'g'ri Player ID orqali avtomatik va 1 daqiqada to'ldirish tizimi tez kunlarda ishga tushiriladi!
        </p>
      </div>

      <!-- Action button to return to PUBG Mobile -->
      <button onclick="switchServicesSubTab('pubg')" 
              class="btn-primary px-6 py-3 text-xs font-extrabold shadow-md shadow-blue-600/30">
        <span>🎮 PUBG Mobile UC ga o'tish</span>
      </button>
    </div>
  `;
}

// -------------------------------------------------------------
// 8. TAB 4: АККАУНТ (MENING AKKAUNT)
// -------------------------------------------------------------
function renderAccountTab() {
  const goalTarget = 1200000;
  const goalCurrent = 490000;
  const goalPercent = Math.min(100, Math.round((goalCurrent / goalTarget) * 100));

  return `
    <div class="animate-fade-in space-y-4">
      <!-- Header Banner with Animated Account Emoji -->
      <div class="glass-card p-4 flex items-center gap-3 bg-gradient-to-r from-blue-950/40 to-slate-900 shadow-md">
        <div class="w-9 h-9 flex items-center justify-center">
          ${renderEmoji('account', 'w-9 h-9')}
        </div>
        <div>
          <h2 class="text-base font-extrabold text-white">Mening akkauntim</h2>
          <p class="text-xs text-slate-400">Shaxsiy ma'lumotlar va xaridlar statistikasi</p>
        </div>
      </div>

      <!-- 4 Stat Cards Grid with Custom Animated Emojis -->
      <div class="grid grid-cols-2 gap-2.5">
        <!-- 1. Текущий баланс -->
        <div class="glass-card p-3.5 flex flex-col justify-between rounded-2xl">
          <div class="flex items-center justify-between">
            <span class="text-xs text-slate-300 font-bold">Текущий баланс</span>
            <div class="w-6 h-6 flex items-center justify-center">${renderEmoji('pay_balance', 'w-6 h-6')}</div>
          </div>
          <div class="text-base font-black text-emerald-400 mt-2">${formatNumber(STATE.user.balance)} so'm</div>
        </div>

        <!-- 2. Всего Stars -->
        <div class="glass-card p-3.5 flex flex-col justify-between rounded-2xl">
          <div class="flex items-center justify-between">
            <span class="text-xs text-slate-300 font-bold">Всего Stars</span>
            <div class="w-6 h-6 flex items-center justify-center">${renderEmoji('tab_stars', 'w-6 h-6')}</div>
          </div>
          <div class="text-base font-black text-amber-400 mt-2">350 ⭐</div>
        </div>

        <!-- 3. Всего потрачено -->
        <div class="glass-card p-3.5 flex flex-col justify-between rounded-2xl">
          <div class="flex items-center justify-between">
            <span class="text-xs text-slate-300 font-bold">Всеgo потрачено</span>
            <div class="w-6 h-6 flex items-center justify-center">${renderEmoji('pay_card', 'w-6 h-6')}</div>
          </div>
          <div class="text-base font-black text-blue-400 mt-2">490 000 so'm</div>
        </div>

        <!-- 4. Количество покупок (Istoriya Pokupok Emoji) -->
        <div class="glass-card p-3.5 flex flex-col justify-between rounded-2xl">
          <div class="flex items-center justify-between">
            <span class="text-xs text-slate-300 font-bold">Количество покупок</span>
            <div class="w-6 h-6 flex items-center justify-center">${renderEmoji('purchase_history', 'w-6 h-6')}</div>
          </div>
          <div class="text-base font-black text-purple-400 mt-2">5 ta xarid</div>
        </div>
      </div>

      <!-- Big Deposit Button -->
      <button onclick="openDepositModal()" class="btn-primary w-full py-4 text-sm font-black shadow-lg shadow-blue-600/30">
        <span>💳 Пополнить баланс</span>
      </button>

      <!-- Goal Progress Card -->
      <div class="glass-card p-4 space-y-2.5">
        <div class="flex items-center justify-between">
          <div class="flex items-center gap-1.5 text-xs font-bold text-white">
            <span>🎯</span>
            <span>Ваша цель (VIP Status)</span>
          </div>
          <span class="text-xs font-black text-blue-400">${goalPercent}%</span>
        </div>
        <div class="w-full h-3 bg-slate-800 rounded-full overflow-hidden p-0.5 border border-slate-700">
          <div class="h-full bg-gradient-to-r from-blue-600 to-cyan-400 rounded-full transition-all duration-500" style="width: ${goalPercent}%"></div>
        </div>
        <div class="flex justify-between text-[11px] text-slate-400">
          <span>${formatNumber(goalCurrent)} so'm</span>
          <span>${formatNumber(goalTarget)} so'm</span>
        </div>
      </div>

      <!-- Boost & Vote Card -->
      <div class="glass-card p-4 flex items-center justify-between bg-gradient-to-r from-blue-950/40 to-slate-900">
        <div>
          <h4 class="text-xs font-bold text-white">Kanalimiz uchun ovoz bering</h4>
          <p class="text-[11px] text-slate-400 mt-0.5">Telegram kanali rivojiga hissa qo'shing</p>
        </div>
        <a href="https://t.me/boost/GyroService_bot" target="_blank"
           class="px-3.5 py-2 bg-blue-600 hover:bg-blue-500 text-white rounded-xl text-xs font-black transition-all shadow-md">
          Boost ⚡
        </a>
      </div>

      <!-- Referral Link Card -->
      <div class="glass-card p-4 space-y-2">
        <div class="flex items-center justify-between">
          <span class="text-xs font-bold text-slate-300">Do'stlarni taklif qilish (2% doimiy bonus):</span>
        </div>
        <div class="flex items-center gap-2">
          <input type="text" readonly value="https://t.me/GyroService_bot?start=ref_${STATE.user.userId}"
                 class="w-full bg-slate-900/90 border border-slate-700 rounded-xl px-3 py-2.5 text-xs font-mono text-slate-300 outline-none">
          <button onclick="copyToClipboard('https://t.me/GyroService_bot?start=ref_${STATE.user.userId}', 'Havola nusxalandi!')" 
                  class="btn-secondary px-3 py-2.5 text-xs whitespace-nowrap">
            Nusxa
          </button>
        </div>
      </div>
    </div>
  `;
}

// -------------------------------------------------------------
// 9. TAB 5: ИСТОРИЯ ТРАНЗАКЦИЙ (HISTORY)
// -------------------------------------------------------------
function renderHistoryTab() {
  const filtered = STATE.historyFilter === "all" 
    ? STATE.history 
    : STATE.history.filter(h => h.status === STATE.historyFilter);

  return `
    <div class="animate-fade-in space-y-4">
      <!-- Header Banner with Animated Purchase History Emoji -->
      <div class="glass-card p-4 flex items-center gap-3 bg-gradient-to-r from-slate-900 to-blue-950/40 shadow-md">
        <div class="w-9 h-9 flex items-center justify-center">
          ${renderEmoji('purchase_history', 'w-9 h-9')}
        </div>
        <div>
          <h2 class="text-base font-extrabold text-white">История транзакций</h2>
          <p class="text-xs text-slate-400">Barcha xaridlar va to'lovlar hisoboti</p>
        </div>
      </div>

      <!-- Status Filter -->
      <div class="glass-card p-1.5 flex gap-1 rounded-2xl bg-slate-900/90">
        ${[
          { id: "all", label: "Все" },
          { id: "completed", label: "Выполнено" },
          { id: "pending", label: "Ожидание" },
          { id: "cancelled", label: "Отменено" }
        ].map(f => `
          <button onclick="switchHistoryFilter('${f.id}')"
                  class="flex-1 py-1.5 rounded-xl text-[11px] font-bold transition-all ${STATE.historyFilter === f.id ? 'bg-blue-600 text-white shadow-md shadow-blue-600/30' : 'text-slate-400 hover:text-slate-200'}">
            ${f.label}
          </button>
        `).join("")}
      </div>

      <!-- Transactions List -->
      ${filtered.length === 0 ? `
        <div class="glass-card p-10 flex flex-col items-center justify-center text-center space-y-3">
          <div class="w-14 h-14 mx-auto flex items-center justify-center animate-float">
            ${renderEmoji('purchase_history', 'w-14 h-14')}
          </div>
          <div class="text-sm font-bold text-slate-400">Транзакций нет</div>
          <p class="text-xs text-slate-500">Ushbu filtr bo'yicha hech qanday to'lovlar mavjud emas</p>
        </div>
      ` : `
        <div class="space-y-2">
          ${filtered.map(item => {
            const isCompleted = item.status === "completed";
            const isPending = item.status === "pending";
            const badgeClass = isCompleted 
              ? "bg-emerald-950/60 border-emerald-800/50 text-emerald-400" 
              : (isPending ? "bg-amber-950/60 border-amber-800/50 text-amber-400" : "bg-rose-950/60 border-rose-800/50 text-rose-400");
            const badgeText = isCompleted ? "Выполнено" : (isPending ? "Ожидание" : "Отменено");

            return `
              <div class="glass-card p-3.5 flex items-center justify-between">
                <div class="flex items-center gap-3">
                  <div class="w-10 h-10 rounded-xl bg-slate-800 flex items-center justify-center text-lg border border-slate-700">
                    ${item.type === 'stars' ? renderEmoji('tab_stars', 'w-6 h-6') : (item.type === 'premium' ? renderEmoji('tab_premium', 'w-6 h-6') : (item.type === 'pubg' ? renderEmoji('pubg_uc', 'w-6 h-6') : '💳'))}
                  </div>
                  <div>
                    <div class="text-xs font-extrabold text-white">${item.title}</div>
                    <div class="text-[11px] text-slate-400 mt-0.5">${item.target} • ${item.date}</div>
                  </div>
                </div>
                <div class="text-right">
                  <div class="text-xs font-black text-white">${formatNumber(item.amount)} so'm</div>
                  <span class="inline-block mt-1 text-[10px] font-bold px-2 py-0.5 rounded-full border ${badgeClass}">
                    ${badgeText}
                  </span>
                </div>
              </div>
            `;
          }).join("")}
        </div>
      `}
    </div>
  `;
}

function switchHistoryFilter(f) {
  triggerHaptic("selection");
  STATE.historyFilter = f;
  renderActiveTab();
}

// -------------------------------------------------------------
// 10. ORDER SUBMISSION & AUTO-PAYMENT WORKFLOW
// -------------------------------------------------------------
function submitStarsOrder() {
  const count = STATE.starsCount;
  if (!count || count < 50 || count > 2500) {
    showToast("Stars miqdori 50 dan 2500 oralig'ida bo'lishi kerak!");
    return;
  }
  const username = (STATE.starsUsername || "").trim();
  if (!username) {
    showToast("Iltimos, Telegram username yoki havolani kiriting!");
    return;
  }

  const price = count * STATE.prices.starUnitPrice;
  handleOrderCheckout({
    type: "stars",
    title: `${count} Stars`,
    target: username,
    amount: price,
    details: `${count} Stars • ${username}`
  });
}

function submitPremiumOrder() {
  const months = STATE.premiumMonths;
  if (months === 1) {
    contactAdminForPremium();
    return;
  }

  const username = (STATE.premiumUsername || "").trim();
  if (!username) {
    showToast("Iltimos, Telegram usernameni kiriting!");
    return;
  }

  const pkg = STATE.prices.premiumPackages[months];
  const price = pkg?.price || 180000;

  handleOrderCheckout({
    type: "premium",
    title: `${months} oylik Premium`,
    target: username,
    amount: price,
    details: `${months} oylik Telegram Premium • ${username}`
  });
}

function orderPubg(uc, price) {
  const input = document.getElementById("pubg-player-id");
  const playerId = input ? input.value.trim() : "";
  if (!playerId) {
    showToast("Iltimos, PUBG Player ID raqamini kiriting!");
    return;
  }

  handleOrderCheckout({
    type: "pubg",
    title: `${uc} PUBG UC`,
    target: `ID: ${playerId}`,
    amount: price,
    details: `${uc} UC • Player ID: ${playerId}`
  });
}

async function handleOrderCheckout(orderData) {
  if (STATE.paymentMethod === "balance") {
    if (STATE.user.balance < orderData.amount) {
      showToast("Balansda mablag' yetarli emas! Karta orqali to'lang yoki hisobni to'ldiring.");
      openDepositModal(orderData.amount - STATE.user.balance);
      return;
    }
    
    // Server bilan sinxronizatsiya
    try {
      const res = await fetch("/api/webapp/order", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          userId: STATE.user.userId,
          type: orderData.type,
          target: orderData.target,
          amount: orderData.amount,
          paymentMethod: "balance"
        })
      });
      if (res.ok) {
        const data = await res.json();
        if (data.newBalance !== undefined) {
          STATE.user.balance = data.newBalance;
        } else {
          STATE.user.balance -= orderData.amount;
        }
      } else {
        STATE.user.balance -= orderData.amount;
      }
    } catch (e) {
      STATE.user.balance -= orderData.amount;
    }

    updateHeader();
    addHistoryItem({
      ...orderData,
      id: "ORD-" + Math.floor(1000 + Math.random() * 9000),
      status: "completed",
      date: "Hozir"
    });
    triggerHaptic("success");
    openSuccessModal(orderData);
  } else {
    // Karta orqali avto to'lov
    openPaymentRuleModal(orderData);
  }
}

// -------------------------------------------------------------
// 11. MODALS & POPUPS
// -------------------------------------------------------------
function openPaymentRuleModal(orderData) {
  const modal = document.getElementById("generic-modal");
  const content = document.getElementById("modal-content");
  if (!modal || !content) return;

  triggerHaptic("warning");
  content.innerHTML = `
    <div class="sheet-handle"></div>
    <div class="space-y-4 text-center">
      <div class="w-14 h-14 rounded-2xl bg-amber-500/20 text-amber-400 flex items-center justify-center text-3xl mx-auto border border-amber-500/40">
        ⚠️
      </div>
      <div>
        <h3 class="text-base font-black text-white">Muhim to'lov qoidasi!</h3>
        <p class="text-xs text-slate-300 mt-2 leading-relaxed">
          Avto-to'lov tizimi buyurtmangizni <b>1 soniyada</b> tasdiqlashi uchun sizga unikal summa beriladi.<br><br>
          Iltimos, kartaga <b class="text-amber-400">AYNAN KO'RSATILGAN SUMMANI</b> tiyin-tiyinigacha o'tkazing! Kam yoki ko'p o'tkazsangiz balans tushmay qoladi.
        </p>
      </div>

      <div class="pt-2 flex gap-2">
        <button onclick="closeModal()" class="btn-secondary flex-1 py-3 text-xs">
          Bekor qilish
        </button>
        <button onclick="proceedToPayAuto(${JSON.stringify(orderData).replace(/"/g, '&quot;')})" class="btn-primary flex-1 py-3 text-xs">
          Tushundim, davom etish
        </button>
      </div>
    </div>
  `;

  modal.classList.add("active");
}

async function proceedToPayAuto(orderData) {
  closeModal();
  
  // Real backend or fallback unique order
  try {
    const res = await fetch("/api/webapp/deposit", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        userId: STATE.user.userId,
        amount: orderData.amount
      })
    });
    if (res.ok) {
      const data = await res.json();
      if (data.ok && data.orderId) {
        STATE.activeOrder = {
          ...orderData,
          id: data.orderId,
          exactAmount: data.exactAmount,
          baseAmount: data.baseAmount,
          expiresAt: data.expiresAt || (Date.now() + 15 * 60 * 1000)
        };
        if (data.card) STATE.card = data.card;
        openPayAutoModal();
        return;
      }
    }
  } catch (e) {
    console.log("Deposit backend fallback:", e);
  }

  // Fallback
  const suffix = Math.floor(Math.random() * 85) + 12;
  const exactAmount = orderData.amount + suffix;
  const orderId = "ORD-" + Math.floor(10000 + Math.random() * 90000);

  STATE.activeOrder = {
    ...orderData,
    id: orderId,
    exactAmount: exactAmount,
    baseAmount: orderData.amount,
    expiresAt: Date.now() + 15 * 60 * 1000
  };

  openPayAutoModal();
}

function openPayAutoModal() {
  const modal = document.getElementById("generic-modal");
  const content = document.getElementById("modal-content");
  if (!modal || !content || !STATE.activeOrder) return;

  const ord = STATE.activeOrder;
  triggerHaptic("medium");

  content.innerHTML = `
    <div class="sheet-handle"></div>
    <div class="space-y-4">
      <div class="flex items-center justify-between border-b border-slate-800 pb-3">
        <div>
          <span class="text-[10px] uppercase font-black tracking-wider text-blue-400">Avto-To'lov</span>
          <h3 class="text-sm font-black text-white">Buyurtma #${ord.id}</h3>
        </div>
        <div id="countdown-timer" class="px-3 py-1 bg-amber-500/20 border border-amber-500/40 rounded-full text-xs font-mono font-black text-amber-400">
          14:59
        </div>
      </div>

      <!-- Exact Amount Warning Box -->
      <div class="bg-amber-950/40 border border-amber-500/50 rounded-2xl p-3.5 flex items-center justify-between">
        <div>
          <span class="text-[11px] text-amber-300 font-semibold">To'lanishi kerak bo'lgan summa:</span>
          <div class="text-lg font-black text-amber-400 mt-0.5">${formatNumber(ord.exactAmount)} so'm</div>
        </div>
        <button onclick="copyToClipboard('${ord.exactAmount}', 'Summa nusxalandi!')" class="btn-secondary px-3 py-2 text-xs">
          Nusxa 📋
        </button>
      </div>

      <!-- Card Details Box -->
      <div class="glass-card p-4 space-y-3">
        <div class="flex items-center justify-between">
          <span class="text-xs text-slate-400">To'lov kartasi:</span>
          <span class="text-xs font-black text-blue-400">${STATE.card.methodName}</span>
        </div>

        <div class="flex items-center justify-between bg-slate-900/90 p-3 rounded-xl border border-slate-700">
          <div>
            <div class="text-sm font-mono font-black text-white tracking-wider">${STATE.card.cardNumber}</div>
            <div class="text-[11px] text-slate-400 mt-0.5">${STATE.card.holderName}</div>
          </div>
          <button onclick="copyToClipboard('${STATE.card.cardNumber.replace(/\s/g, '')}', 'Karta raqami nusxalandi!')" class="btn-secondary px-3 py-1.5 text-xs">
            Nusxa 📋
          </button>
        </div>
      </div>

      <!-- Warning Note -->
      <p class="text-[11px] text-slate-400 text-center leading-relaxed">
        <span class="text-amber-400 font-bold">Diqqat:</span> Pulni o'tkazgach, 1 daqiqa kuting va quyidagi tugmani bosing. Hisobingiz avtomatik to'ldiriladi.
      </p>

      <!-- Action Buttons -->
      <div class="space-y-2">
        <button onclick="checkPaymentStatus()" id="btn-check-payment" class="btn-primary w-full py-3.5 text-xs font-black shadow-lg shadow-blue-600/30">
          <span>To'lovni tekshirish 🔄</span>
        </button>
        <button onclick="closeModal()" class="btn-secondary w-full py-2.5 text-xs font-bold text-slate-400">
          Bekor qilish
        </button>
      </div>
    </div>
  `;

  modal.classList.add("active");
  startCountdown(ord.expiresAt);
}

function startCountdown(expiresAt) {
  if (STATE.countdownInterval) clearInterval(STATE.countdownInterval);
  STATE.countdownInterval = setInterval(() => {
    const el = document.getElementById("countdown-timer");
    if (!el) {
      clearInterval(STATE.countdownInterval);
      return;
    }
    const diff = Math.max(0, Math.floor((expiresAt - Date.now()) / 1000));
    const mins = Math.floor(diff / 60);
    const secs = diff % 60;
    el.innerText = `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
    if (diff <= 0) {
      clearInterval(STATE.countdownInterval);
      el.innerText = "Muddati tugadi";
    }
  }, 1000);
}

async function checkPaymentStatus() {
  const btn = document.getElementById("btn-check-payment");
  if (btn) {
    btn.innerHTML = `<span class="inline-block animate-spin mr-2">⏳</span> To'lov tekshirilmoqda...`;
    btn.disabled = true;
  }

  triggerHaptic("medium");

  if (STATE.activeOrder && typeof STATE.activeOrder.id === "number") {
    try {
      const res = await fetch("/api/webapp/check-payment?orderId=" + STATE.activeOrder.id);
      if (res.ok) {
        const data = await res.json();
        if (data.status === "completed") {
          closeModal();
          if (data.newBalance !== undefined) STATE.user.balance = data.newBalance;
          updateHeader();
          triggerHaptic("success");
          openSuccessModal(STATE.activeOrder);
          return;
        } else if (data.status === "pending") {
          showToast("To'lov hali tushmadi. Iltimos, yana 30 soniya kuting.");
          if (btn) {
            btn.innerHTML = `<span>To'lovni tekshirish 🔄</span>`;
            btn.disabled = false;
          }
          return;
        }
      }
    } catch (e) {}
  }

  // Standalone fallback
  setTimeout(() => {
    closeModal();
    if (STATE.activeOrder) {
      const ord = STATE.activeOrder;
      if (ord.type === "deposit") {
        STATE.user.balance += ord.baseAmount;
      }
      addHistoryItem({
        id: ord.id,
        type: ord.type,
        title: ord.title,
        target: ord.target,
        amount: ord.baseAmount,
        status: "completed",
        date: "Hozir"
      });
      updateHeader();
      triggerHaptic("success");
      openSuccessModal(ord);
    }
  }, 1400);
}

function openDepositModal(defaultAmount) {
  const modal = document.getElementById("generic-modal");
  const content = document.getElementById("modal-content");
  if (!modal || !content) return;

  const presets = [10000, 25000, 50000, 100000, 250000, 500000];

  content.innerHTML = `
    <div class="sheet-handle"></div>
    <div class="space-y-4">
      <div class="flex items-center justify-between border-b border-slate-800 pb-3">
        <h3 class="text-base font-extrabold text-white">💳 Balansni to'ldirish</h3>
        <span class="text-xs text-slate-400">Joriy: <b class="text-emerald-400">${formatNumber(STATE.user.balance)} so'm</b></span>
      </div>

      <div class="space-y-2">
        <label class="text-xs font-bold text-slate-300">Summani kiriting (5 000 — 1 000 000 so'm):</label>
        <input type="number" id="deposit-amount-input" 
               value="${defaultAmount || 50000}" min="5000" max="1000000"
               class="w-full bg-slate-900/90 border border-slate-700 focus:border-blue-500 rounded-xl px-4 py-3 text-base font-black text-white outline-none">
      </div>

      <!-- Quick Chips -->
      <div class="grid grid-cols-3 gap-2">
        ${presets.map(p => `
          <button onclick="document.getElementById('deposit-amount-input').value = ${p}" 
                  class="py-2 bg-slate-900 border border-slate-700 hover:border-blue-500 rounded-xl text-xs font-bold text-slate-300">
            ${formatNumber(p)}
          </button>
        `).join("")}
      </div>

      <button onclick="submitDeposit()" class="btn-primary w-full py-4 text-xs font-black shadow-lg shadow-blue-600/30">
        Hisobni to'ldirish (Pay-Auto) ⚡
      </button>
    </div>
  `;

  modal.classList.add("active");
}

function submitDeposit() {
  const input = document.getElementById("deposit-amount-input");
  const amount = parseInt(input?.value) || 0;
  if (amount < 5000 || amount > 1000000) {
    showToast("Summa 5 000 va 1 000 000 so'm oralig'ida bo'lishi kerak!");
    return;
  }

  closeModal();
  setTimeout(() => {
    openPaymentRuleModal({
      type: "deposit",
      title: `Balans to'ldirish (+${formatNumber(amount)} so'm)`,
      target: "HUMO *6261",
      amount: amount,
      details: `Balans to'ldirish`
    });
  }, 200);
}

function openSuccessModal(orderData) {
  const modal = document.getElementById("generic-modal");
  const content = document.getElementById("modal-content");
  if (!modal || !content) return;

  triggerHaptic("success");
  content.innerHTML = `
    <div class="sheet-handle"></div>
    <div class="space-y-4 text-center py-2">
      <div class="w-16 h-16 rounded-full bg-emerald-500/20 text-emerald-400 flex items-center justify-center text-3xl mx-auto border border-emerald-500/40 animate-float">
        ✅
      </div>
      <div>
        <h3 class="text-lg font-black text-white">Заказ создан!</h3>
        <p class="text-xs text-slate-300 mt-1">To'lovingiz muvaffaqiyatli qabul qilindi.</p>
      </div>

      <div class="glass-card p-3.5 text-left text-xs space-y-1.5 bg-slate-900/80">
        <div class="flex justify-between">
          <span class="text-slate-400">Xizmat:</span>
          <span class="font-bold text-white">${orderData.title}</span>
        </div>
        <div class="flex justify-between">
          <span class="text-slate-400">Summa:</span>
          <span class="font-black text-emerald-400">${formatNumber(orderData.amount)} so'm</span>
        </div>
        <div class="flex justify-between">
          <span class="text-slate-400">Holat:</span>
          <span class="font-bold text-blue-400">Bajarildi ⚡</span>
        </div>
      </div>

      <button onclick="closeModal()" class="btn-primary w-full py-3.5 text-xs font-black shadow-lg shadow-blue-600/30">
        Yopish / Asosiyga qaytish
      </button>
    </div>
  `;

  modal.classList.add("active");
}

function openPromocodeModal() {
  const modal = document.getElementById("generic-modal");
  const content = document.getElementById("modal-content");
  if (!modal || !content) return;

  content.innerHTML = `
    <div class="sheet-handle"></div>
    <div class="space-y-4">
      <div class="flex items-center gap-2">
        <span class="text-xl">🎟</span>
        <h3 class="text-base font-extrabold text-white">Promokodni faollashtirish</h3>
      </div>
      <p class="text-xs text-slate-400">Promokodni kiriting va balansingizga bepul bonus oling (Sinash uchun: GYRO2026)</p>

      <input type="text" id="promo-input" placeholder="Kodni kiriting..." uppercase
             class="w-full bg-slate-900/90 border border-slate-700 focus:border-purple-500 rounded-xl px-4 py-3 text-sm font-mono font-bold text-white outline-none uppercase">

      <button onclick="applyPromocode()" class="btn-primary w-full py-3.5 text-xs font-black bg-gradient-to-r from-purple-600 to-indigo-600 shadow-lg shadow-purple-600/30">
        Faollashtirish ⚡
      </button>
    </div>
  `;

  modal.classList.add("active");
}

function applyPromocode() {
  const input = document.getElementById("promo-input");
  const code = (input?.value || "").trim().toUpperCase();
  if (["GYRO2026", "VIP", "BONUS"].includes(code)) {
    const bonus = 15000;
    STATE.user.balance += bonus;
    updateHeader();
    closeModal();
    triggerHaptic("success");
    showToast(`🎉 Promokod faollashtirildi! +${formatNumber(bonus)} so'm qo'shildi.`);
  } else {
    triggerHaptic("error");
    showToast("Noto'g'ri promokod yoki muddati o'tgan!");
  }
}

function closeModal() {
  const modal = document.getElementById("generic-modal");
  if (modal) modal.classList.remove("active");
  if (STATE.countdownInterval) clearInterval(STATE.countdownInterval);
}

function addHistoryItem(item) {
  STATE.history.unshift(item);
  if (STATE.activeTab === "history") renderActiveTab();
}

// -------------------------------------------------------------
// 12. AUTO-FILL USERNAME (1-CLICK PROFILE BUTTON)
// -------------------------------------------------------------
function fillMyUsername(targetType = 'stars') {
  triggerHaptic("selection");
  const tgUser = window.Telegram?.WebApp?.initDataUnsafe?.user;
  const rawUsername = (tgUser?.username || STATE.user.username || "Sharipov_Sh").replace(/^@/, "").trim();
  
  if (!rawUsername) {
    showToast("⚠️ Sizda Telegram username topilmadi!");
    return;
  }

  const formatted = `@${rawUsername}`;
  if (targetType === 'stars') {
    STATE.starsUsername = formatted;
    const inp = document.getElementById("stars-target-input");
    if (inp) {
      inp.value = formatted;
      inp.classList.add("border-blue-500", "ring-2", "ring-blue-500/40");
      setTimeout(() => inp.classList.remove("ring-2", "ring-blue-500/40"), 600);
    }
  } else if (targetType === 'premium') {
    STATE.premiumUsername = formatted;
    const inp = document.getElementById("premium-target-input");
    if (inp) {
      inp.value = formatted;
      inp.classList.add("border-blue-500", "ring-2", "ring-blue-500/40");
      setTimeout(() => inp.classList.remove("ring-2", "ring-blue-500/40"), 600);
    }
  }
  showToast(`👤 O'zingizning usernamengiz kiritildi: ${formatted}`);
}

// -------------------------------------------------------------
// 13. UTILITIES & TOASTS
// -------------------------------------------------------------
function formatNumber(num) {
  return (num || 0).toString().replace(/\B(?=(\d{3})+(?!\d))/g, " ");
}

function copyToClipboard(text, successMsg = "Nusxalandi!") {
  navigator.clipboard.writeText(text).then(() => {
    triggerHaptic("light");
    showToast(successMsg);
  }).catch(() => {
    showToast("Nusxalashda xatolik");
  });
}

function showToast(msg) {
  const container = document.getElementById("toast-container");
  if (!container) return;

  const toast = document.createElement("div");
  toast.className = "toast";
  toast.innerText = msg;
  container.appendChild(toast);

  setTimeout(() => {
    toast.style.opacity = "0";
    toast.style.transition = "opacity 0.3s ease";
    setTimeout(() => toast.remove(), 300);
  }, 2400);
}

function triggerHaptic(type) {
  try {
    const h = window.Telegram?.WebApp?.HapticFeedback;
    if (h) {
      if (type === "selection") h.selectionChanged();
      else if (type === "light") h.impactOccurred("light");
      else if (type === "medium") h.impactOccurred("medium");
      else if (type === "warning") h.notificationOccurred("warning");
      else if (type === "success") h.notificationOccurred("success");
      else if (type === "error") h.notificationOccurred("error");
    }
  } catch (e) {}
}

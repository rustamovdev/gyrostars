// Telegram WebApp Initialization
const tg = window.Telegram?.WebApp;
if (tg) {
  try {
    tg.ready();
    tg.expand();
    if (tg.setHeaderColor) tg.setHeaderColor('#090d16');
    if (tg.setBackgroundColor) tg.setBackgroundColor('#090d16');
  } catch (e) {
    console.log('TG init err:', e);
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
    userId: 8159265215,
    username: 'user',
    fullName: 'Mijoz',
    balance: 0,
    photoUrl: null,
    verified: true
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

// DOM Content Loaded
document.addEventListener('DOMContentLoaded', () => {
  // Extract user info from Telegram
  if (tg?.initDataUnsafe?.user) {
    const u = tg.initDataUnsafe.user;
    state.user.userId = u.id;
    state.user.username = u.username || 'user';
    state.user.fullName = (u.first_name || '') + ' ' + (u.last_name || '');
    if (!state.user.fullName.trim()) state.user.fullName = u.username || 'Mijoz';
    if (u.photo_url) {
      state.user.photoUrl = u.photo_url;
    }
  }

  updateUserUI();
  fetchInitData();
  fetchTopData('today');
  setStarsAmount(100);
});

// Update Top User Header
function updateUserUI() {
  const nameEl = document.getElementById('userName');
  const idEl = document.getElementById('userIdVal');
  const balEl = document.getElementById('userBalanceTop');
  const avatarEl = document.getElementById('userAvatar');
  const accBal = document.getElementById('accountBalVal');
  const depBigBal = document.getElementById('depositBigBal');

  if (nameEl) nameEl.innerText = state.user.fullName || state.user.username;
  if (idEl) idEl.innerText = state.user.userId;
  if (avatarEl) {
    if (state.user.photoUrl) {
      avatarEl.innerHTML = `<img src="${state.user.photoUrl}" alt="avatar" style="width:100%;height:100%;object-fit:cover;border-radius:50%;">`;
    } else {
      const initials = (state.user.fullName || 'SR').split(' ').map(w => w[0]).join('').substring(0, 2).toUpperCase();
      avatarEl.innerText = initials || 'SR';
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
  let un = state.user.username;
  if (un && un !== 'user') {
    if (!un.startsWith('@')) un = '@' + un;
    input.value = un;
    showToast("O'zingizning " + un + " kiritildi! 👤");
  } else {
    input.value = "@" + (state.user.username || "username");
    showToast("Username kiritildi! 👤");
  }
}

// Fetch Backend Init
async function fetchInitData() {
  try {
    const res = await fetch(`/api/webapp/init?userId=${state.user.userId}`);
    if (res.ok) {
      const data = await res.json();
      if (data.user) {
        state.user.balance = data.user.balance || 0;
        if (data.user.fullName) state.user.fullName = data.user.fullName;
        if (data.user.username) state.user.username = data.user.username;
      }
      if (data.prices) {
        state.prices = data.prices;
      }
      if (data.stats) {
        document.getElementById('accountStarsVal').innerText = formatMoney(data.stats.totalStars || 0);
        document.getElementById('accountSpentVal').innerText = formatMoney(data.stats.totalSpent || 0) + " so'm";
        document.getElementById('accountPurchasesVal').innerText = data.stats.totalPurchases || 0;
        document.getElementById('goalBarFill').style.width = Math.max(5, data.stats.goalProgress || 0) + '%';
        const rem = Math.max(0, 1200000 - (data.stats.totalSpent || 0));
        document.getElementById('goalSubText').innerText = formatMoney(rem) + " so'm qoldi · " + (data.stats.goalProgress || 0) + "% bajarildi";
      }
      updateUserUI();
      calculateStarsPrice();
    }
  } catch (e) {
    console.log('Using offline state', e);
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

// Stars Calculations
function setStarsAmount(amount) {
  triggerHaptic('light');
  const input = document.getElementById('starsAmountInput');
  if (input) {
    input.value = amount;
    calculateStarsPrice();
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
  document.getElementById('payMethodCard').classList.toggle('active', method === 'card');
  document.getElementById('payMethodBalance').classList.toggle('active', method === 'balance');
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
  const target = document.getElementById('premTargetUsername').value.trim();
  if (!target) {
    showToast("Username kiriting!");
    triggerHaptic('warning');
    return;
  }

  const orderRes = await fetch('/api/webapp/deposit', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ userId: state.user.userId, amount: state.selectedPremPrice })
  });
  const data = await orderRes.json();
  if (data.ok) {
    showInvoiceScreen(data);
  }
}

async function submitPubgOrder() {
  triggerHaptic('medium');
  const playerId = document.getElementById('pubgPlayerIdInput').value.trim();
  if (!playerId) {
    showToast("PUBG Player ID kiriting!");
    triggerHaptic('warning');
    return;
  }

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
  if (data.ok) {
    if (data.invoice) showInvoiceScreen(data);
    else {
      triggerHaptic('success');
      showToast(data.message || "PUBG UC yuborildi! 🎮");
      state.user.balance = data.newBalance;
      updateUserUI();
    }
  }
}

// DEPOSIT FORM
function setDepositAmount(amount) {
  triggerHaptic('light');
  document.getElementById('depositAmountInput').value = amount;
}

async function submitDepositOrder() {
  triggerHaptic('medium');
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

  const cardNum = data.cardNumber || '9860 1678 4421 7684';
  const spacedNum = cardNum.replace(/(\d{4})/g, '$1 ').trim();
  const holder = data.holderName || 'SUNNAT C.';
  const amount = formatMoney(data.amount) + ' UZS';

  document.getElementById('invoiceCardNumber').innerText = spacedNum;
  document.getElementById('invoiceHolderName').innerText = holder;
  document.getElementById('invoiceAmount').innerText = amount;
  document.getElementById('warningAmountText').innerText = amount;

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
    const res = await fetch(`/api/webapp/top?period=${period}&userId=${state.user.userId}`);
    if (res.ok) {
      const data = await res.json();
      renderTopList(data.top || []);
      if (data.recentBuyers && data.recentBuyers.length > 0) {
        document.getElementById('recentBuyersMarquee').innerHTML = data.recentBuyers.map((b, i) => `👤 <span class="highlight">${escapeHtml(b)}</span>`).join(' · ');
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

  container.innerHTML = items.map(item => `
    <div class="history-item-card">
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
}

function filterHistory(status, el) {
  triggerHaptic('light');
  document.querySelectorAll('.h-tab').forEach(t => t.classList.remove('active'));
  if (el) el.classList.add('active');
  fetchHistoryData();
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

function pad(n) {
  return n < 10 ? '0' + n : n;
}

function escapeHtml(text) {
  if (!text) return '';
  return String(text).replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;");
}

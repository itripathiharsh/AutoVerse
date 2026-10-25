// AutoVerse Cockpit OS — Apple CarPlay Next-Gen Interactive Controller & Android Native Bridge

// ============================================================================
// 0. AUTOVERSE NATIVE ANDROID BRIDGE INTERFACE
// ============================================================================
window.AutoVerse = {
  isNative: function () {
    return typeof window.AutoVerseAndroid !== 'undefined';
  },

  currentDurationSec: 0,
  currentPositionSec: 0,
  isPlaying: false,

  // Speech recognizer outcome from the Android bridge
  onVoiceResult: function (payload) {
    clearVoiceListening();
    payload = payload || {};
    if (!payload.ok) {
      showToast("Voice: " + (payload.error || "no input captured"));
      return;
    }
    const text = String(payload.text || '').trim();
    const mode = payload.mode || 'command';
    if (!text) {
      showToast("Voice: nothing heard");
      return;
    }
    showToast(`Heard: "${text}"`);
    if (mode === 'apps') {
      const input = document.getElementById('appSearchInput');
      if (input) {
        input.value = text;
        input.dispatchEvent(new Event('input', { bubbles: true }));
      }
      return;
    }
    if (mode === 'navigation') {
      dispatchVoiceNavigation(text);
      return;
    }
    dispatchVoiceCommand(text);
  },

  updateMediaState: function (state) {
    if (!state) return;
    const trackTitle = document.getElementById('trackTitle');
    const trackArtist = document.getElementById('trackArtist');
    const islandTrack = document.getElementById('islandTrack');
    const playPauseBtn = document.getElementById('playPauseBtn');
    const iconPlay = playPauseBtn ? playPauseBtn.querySelector('.icon-play') : null;
    const iconPause = playPauseBtn ? playPauseBtn.querySelector('.icon-pause') : null;
    const islandPlayBtn = document.getElementById('islandPlayBtn');
    const progressFill = document.getElementById('progressFill');
    const progressThumb = document.getElementById('progressThumb');
    const currentTimeEl = document.getElementById('currentTime');
    const totalTimeEl = document.getElementById('totalTime');
    const albumImg = document.querySelector('.album-img');
    const expandedThumb = document.querySelector('.expanded-thumb-img');
    const heroMediaBento = document.querySelector('.hero-media-bento');
    const expandedTitle = document.querySelector('.expanded-title');
    const expandedArtist = document.querySelector('.expanded-artist');
    const islandBars = document.querySelector('.island-bars');
    const mediaGlow = document.getElementById('mediaGlow');

    // Real artwork in, code placeholder back out — never leave a stale cover behind
    const setArtwork = (art) => {
      if (albumImg) {
        if (art) { albumImg.src = art; albumImg.hidden = false; }
        else { albumImg.removeAttribute('src'); albumImg.hidden = true; }
      }
      if (expandedThumb) {
        if (art) { expandedThumb.src = art; expandedThumb.hidden = false; }
        else { expandedThumb.removeAttribute('src'); expandedThumb.hidden = true; }
      }
      if (mediaGlow) {
        mediaGlow.style.backgroundImage = art
          ? `radial-gradient(circle at center, rgba(56, 189, 248, 0.4) 0%, transparent 70%), url(${art})`
          : '';
      }
    };

    if (state.accessRequired) {
      if (trackTitle) trackTitle.textContent = "Music Permission Required";
      if (trackArtist) {
        trackArtist.innerHTML = '<span class="media-prompt-banner" id="btnGrantMediaAccess">Tap to connect Spotify / YT Music &gt;</span>';
        const grantBtn = document.getElementById('btnGrantMediaAccess');
        if (grantBtn) {
          grantBtn.onclick = (e) => {
            e.stopPropagation();
            if (window.AutoVerse.isNative()) {
              window.AutoVerseAndroid.openNotificationAccessSettings();
            } else {
              showToast("Notification Access is required on Android to read active media sessions");
            }
          };
        }
      }
      if (islandTrack) {
        islandTrack.textContent = "Tap to Connect Music • Access Required";
      }
      if (islandPlayBtn) islandPlayBtn.textContent = '▶';
      if (islandBars) islandBars.style.opacity = '0.3';
      setArtwork(null);
      return;
    }

    if (state.isActive && state.title) {
      if (trackTitle) trackTitle.textContent = state.title;
      const sourceLabel = document.getElementById('musicSourceLabel');
      if (sourceLabel) {
        if (state.appLabel) sourceLabel.textContent = `MEDIA SESSION • ${state.appLabel.toUpperCase()}`;
        else if (state.packageName) sourceLabel.textContent = `MEDIA SESSION • ${state.packageName.split('.').pop().toUpperCase()}`;
        else sourceLabel.textContent = 'MEDIA SESSION • ACTIVE';
      }
      if (trackArtist) {
        const sub = state.album ? `${state.artist} • ${state.album}` : (state.artist || (state.appLabel || 'AutoVerse Music'));
        trackArtist.textContent = sub;
      }
      if (islandTrack) {
        islandTrack.textContent = `${state.title} • ${state.artist || state.appLabel || 'AutoVerse'}`;
      }
      if (expandedTitle) expandedTitle.textContent = state.title;
      if (expandedArtist) expandedArtist.textContent = state.artist || '';

      const art = state.artworkBase64 || state.artworkUrl;
      setArtwork(art || null);

      if (typeof state.isPlaying === 'boolean') {
        window.AutoVerse.isPlaying = state.isPlaying;
        if (iconPause && iconPlay) {
          iconPause.style.display = state.isPlaying ? 'block' : 'none';
          iconPlay.style.display = state.isPlaying ? 'none' : 'block';
        }
        if (heroMediaBento) heroMediaBento.classList.toggle('is-paused', !state.isPlaying);
        if (islandPlayBtn) islandPlayBtn.textContent = state.isPlaying ? '⏸' : '▶';
        if (islandBars) islandBars.style.opacity = state.isPlaying ? '1' : '0.3';
      }

      if (typeof state.positionMs === 'number' && typeof state.durationMs === 'number' && state.durationMs > 0) {
        window.AutoVerse.currentDurationSec = Math.floor(state.durationMs / 1000);
        window.AutoVerse.currentPositionSec = Math.floor(state.positionMs / 1000);
        const pct = Math.min(100, Math.max(0, (state.positionMs / state.durationMs) * 100));
        if (progressFill) progressFill.style.width = `${pct}%`;
        if (progressThumb) progressThumb.style.left = `${pct}%`;

        const fmt = s => `${Math.floor(s / 60)}:${String(s % 60).padStart(2, '0')}`;
        if (currentTimeEl) currentTimeEl.textContent = fmt(window.AutoVerse.currentPositionSec);
        if (totalTimeEl) totalTimeEl.textContent = fmt(window.AutoVerse.currentDurationSec);
      }
    } else if (window.AutoVerse.isNative()) {
      if (trackTitle) trackTitle.textContent = "No Music Playing";
      if (trackArtist) trackArtist.textContent = "Tap to open Spotify / YouTube Music";
      if (islandTrack) islandTrack.textContent = "AutoVerse Cockpit • Ready";
      if (islandBars) islandBars.style.opacity = '0.3';
      if (islandPlayBtn) islandPlayBtn.textContent = '▶';
      if (iconPause && iconPlay) {
        iconPause.style.display = 'none';
        iconPlay.style.display = 'block';
      }
      setArtwork(null);
    }
  },

  updateInstalledApps: function (apps) {
    if (!Array.isArray(apps) || apps.length === 0) return;
    window.__installedApps = apps;
    const container = document.getElementById('appsGridContainer');
    if (container) {
      container.innerHTML = '';
      apps.forEach((app, index) => {
        const tile = document.createElement('div');
        tile.className = 'app-spring-tile';
        tile.style.setProperty('--delay', (index % 12) + 1);
        tile.dataset.name = app.label || app.packageName;
        tile.dataset.cat = (app.category || 'others').toLowerCase();
        tile.dataset.pkg = app.packageName;
        tile.dataset.activity = app.activityName || '';

        const squircle = document.createElement('div');
        squircle.className = 'apple-squircle bg-white';

        if (app.iconBase64) {
          const img = document.createElement('img');
          img.src = app.iconBase64;
          img.alt = app.label;
          img.style.width = '100%';
          img.style.height = '100%';
          img.style.borderRadius = 'inherit';
          img.style.objectFit = 'cover';
          squircle.appendChild(img);
        } else {
          squircle.innerHTML = `
            <svg viewBox="0 0 24 24" width="28" height="28" fill="#0E71FD">
              <rect x="3" y="3" width="7" height="7" rx="2" />
              <rect x="14" y="3" width="7" height="7" rx="2" />
              <rect x="3" y="14" width="7" height="7" rx="2" />
              <rect x="14" y="14" width="7" height="7" rx="2" />
            </svg>
          `;
        }

        const specular = document.createElement('div');
        specular.className = 'squircle-specular';
        squircle.appendChild(specular);

        const title = document.createElement('span');
        title.className = 'app-title-text';
        title.textContent = app.label || app.packageName;

        tile.appendChild(squircle);
        tile.appendChild(title);

        tile.addEventListener('click', () => {
          tile.classList.add('app-launch-feedback');
          setTimeout(() => tile.classList.remove('app-launch-feedback'), 400);

          logRecentAppLaunch(app);
          showToast(`Opening ${app.label}...`);

          if (window.AutoVerse.isNative()) {
            window.AutoVerseAndroid.launchApp(app.packageName, app.activityName || '');
          }
        });

        container.appendChild(tile);
      });
    }

    localStorage.setItem('autoverse_installed_apps_cache', JSON.stringify(apps));
    renderFavoritesShelf();
    renderRecentAppsShelf();
  },

  updateConnectivity: function (conn) {
    if (!conn) return;
    const btNameEl = document.querySelector('.bt-name-text');
    if (btNameEl) {
      btNameEl.textContent = conn.bluetoothConnected ? (conn.deviceName || 'Connected Device') : 'Hands-Free';
    }
    const btChip = document.querySelector('.bt-status-chip');
    if (btChip && typeof conn.bluetoothConnected === 'boolean') {
      btChip.style.opacity = conn.bluetoothConnected ? '1' : '0.4';
    }
    const btDetail = document.getElementById('btDetailText');
    if (btDetail && typeof conn.bluetoothConnected === 'boolean') {
      btDetail.textContent = conn.bluetoothConnected ? `Connected: ${conn.deviceName || 'Phone'} • Audio & Contacts Synced` : 'No Bluetooth device connected';
    }
    const wifiDetail = document.getElementById('wifiDetailText');
    if (wifiDetail && typeof conn.wifiConnected === 'boolean') {
      wifiDetail.textContent = conn.wifiConnected ? 'Wi-Fi Network Connected • Active Internet' : 'Wi-Fi Disconnected • Hotspot Ready';
    }
  },

  updateLocation: function (loc) {
    if (!loc) return;
    if (typeof window.__onAutoVerseLocationUpdate === 'function') {
      window.__onAutoVerseLocationUpdate(loc);
    }
  },

  // Tier 2 OBD live engine state from the Android bridge
  updateObdState: function (payload) {
    renderObdStrip(payload || {});
  },

  // Tier 2 offline voice: model download / wake / capture states
  updateOfflineVoiceState: function (payload) {
    renderOfflineVoiceState(payload || {});
  },

  // Workload profile from the Android side re-tiers cockpit effects live
  updateWorkloadProfile: function (payload) {
    const body = document.body;
    body.classList.remove('profile-constrained', 'profile-balanced', 'profile-enhanced');
    const name = String((payload && payload.profile) || 'BALANCED').toLowerCase();
    body.classList.add('profile-' + name);
    if (name === 'constrained') {
      showToast('Workload profile: Constrained — heavy visuals off for smoothness');
    }
  }
};

// Apply the stored profile (and a browser default) before first paint of the body styles
(function initWorkloadProfile() {
  const apply = (name) => {
    document.body.classList.add('profile-' + String(name || 'balanced').toLowerCase());
  };
  try {
    if (window.AutoVerse && window.AutoVerse.isNative() &&
        typeof window.AutoVerseAndroid.getWorkloadProfile === 'function') {
      apply(window.AutoVerseAndroid.getWorkloadProfile());
    } else {
      apply('enhanced'); // browser preview shows the full look
    }
  } catch (_) {
    apply('balanced'); // a mismatched bridge must never kill initialization
  }
})();

/* ==================== OFFLINE VOICE STATE (Tier 2) ==================== */
let __lastVoiceState = '';
let __lastDownloadToastPct = -1;

function renderOfflineVoiceState(payload) {
  const orb = document.getElementById('siriOrbBtn');
  const island = document.getElementById('islandTrack');
  const status = document.getElementById('offlineVoiceStatus');
  const toggle = document.getElementById('offlineVoiceToggle');
  const st = payload.state || 'disabled';

  const setStatus = (text) => { if (status) status.textContent = text; };

  if (st === 'capture') {
    if (orb) orb.classList.add('listening');
    if (island) island.textContent = 'Listening… say a command';
    showToast("Listening… say a command (offline)");
  } else if (st === 'wake') {
    if (orb) orb.classList.remove('listening');
    if (island) island.textContent = 'Say “Hey AutoVerse”';
    setStatus('Ready — listening for “Hey AutoVerse”');
  } else if (st === 'downloading') {
    const pct = payload.percent || 0;
    if (pct - __lastDownloadToastPct >= 10) {
      __lastDownloadToastPct = pct;
      showToast(`Downloading offline voice model: ${pct}%`);
    }
    if (island) island.textContent = `Offline voice: ${pct}%`;
    setStatus(`Downloading voice model… ${pct}%`);
  } else if (st === 'preparing') {
    if (island) island.textContent = 'Preparing offline voice…';
    setStatus('Preparing offline voice…');
  } else if (st === 'error') {
    if (orb) orb.classList.remove('listening');
    const msg = payload.error || 'Offline voice error';
    showToast(msg);
    setStatus(msg);
    if (msg.indexOf('Microphone permission') === 0 && window.AutoVerse.isNative()) {
      window.AutoVerseAndroid.requestMicPermission();
    }
  } else if (st === 'disabled') {
    if (orb) orb.classList.remove('listening');
    setStatus('Say “Hey AutoVerse” then a command — fully on-device. Downloads a 40 MB voice model once.');
  }
  __lastVoiceState = st;
  if (toggle) toggle.checked = st !== 'disabled';
}

/* Theme sounds (PRD §8): synthesized locally via WebAudio, off by default,
   one short confirmation per theme apply - never on repeated taps. */
let __themeSoundCtx = null;
let __themeSoundOn = false;

function initThemeSounds() {
  try {
    if (window.AutoVerse.isNative() &&
        typeof window.AutoVerseAndroid.isThemeSoundsEnabled === 'function') {
      __themeSoundOn = !!window.AutoVerseAndroid.isThemeSoundsEnabled();
    }
  } catch (_) { __themeSoundOn = false; }
}

function playThemeSound() {
  if (!__themeSoundOn) return;
  try {
    __themeSoundCtx = __themeSoundCtx || new (window.AudioContext || window.webkitAudioContext)();
    const ctx = __themeSoundCtx;
    const osc = ctx.createOscillator();
    const gain = ctx.createGain();
    osc.type = 'sine';
    osc.frequency.setValueAtTime(660, ctx.currentTime);
    osc.frequency.exponentialRampToValueAtTime(880, ctx.currentTime + 0.09);
    gain.gain.setValueAtTime(0.06, ctx.currentTime);
    gain.gain.exponentialRampToValueAtTime(0.0001, ctx.currentTime + 0.16);
    osc.connect(gain).connect(ctx.destination);
    osc.start();
    osc.stop(ctx.currentTime + 0.17);
  } catch (_) { /* audio unavailable: silent */ }
}

function initOfflineVoiceToggle() {
  const toggle = document.getElementById('offlineVoiceToggle');
  if (!toggle) return;
  try {
    if (window.AutoVerse.isNative() &&
        typeof window.AutoVerseAndroid.isOfflineVoiceEnabled === 'function') {
      toggle.checked = !!window.AutoVerseAndroid.isOfflineVoiceEnabled();
    }
  } catch (_) {
    toggle.checked = false;
  }
  toggle.addEventListener('change', () => {
    if (window.AutoVerse.isNative()) {
      window.AutoVerseAndroid.setOfflineVoiceEnabled(toggle.checked);
      showToast(toggle.checked ? 'Offline voice on — first enable downloads the model' : 'Offline voice off');
    } else {
      showToast('Offline voice works in the Android app');
      toggle.checked = false;
    }
  });
}

/* ==================== OFFLINE MAP TILE CACHE (Tier 2) ====================
   Passive mirror: stores only tiles the user has actually viewed, so the
   last-visited areas still render with no connectivity. IndexedDB is used
   because Cache API is unavailable on the WebView's file origin. No bulk
   region downloads by design (tile-provider fair use). */
const TILE_DB_NAME = 'autoverse_tiles';

function openTileDb() {
  return new Promise((resolve, reject) => {
    const req = indexedDB.open(TILE_DB_NAME, 1);
    req.onupgradeneeded = () => { req.result.createObjectStore('tiles'); };
    req.onsuccess = () => resolve(req.result);
    req.onerror = () => reject(req.error);
  });
}

async function cacheTileFromUrl(url) {
  try {
    if (!url || url.startsWith('data:')) return;
    const resp = await fetch(url, { mode: 'cors' });
    if (!resp.ok) return;
    const blob = await resp.blob();
    const db = await openTileDb();
    await new Promise((resolve, reject) => {
      const tx = db.transaction('tiles', 'readwrite');
      tx.objectStore('tiles').put(blob, url);
      tx.oncomplete = resolve;
      tx.onerror = () => reject(tx.error);
    });
  } catch (_) { /* CORS-blocked or offline mid-write: skip silently */ }
}

async function clearCachedTiles() {
  const db = await openTileDb();
  await new Promise((resolve, reject) => {
    const tx = db.transaction('tiles', 'readwrite');
    tx.objectStore('tiles').clear();
    tx.oncomplete = resolve;
    tx.onerror = () => reject(tx.error);
  });
}

async function countCachedTiles() {
  const db = await openTileDb();
  return new Promise((resolve) => {
    const tx = db.transaction('tiles', 'readonly');
    const rq = tx.objectStore('tiles').count();
    rq.onsuccess = () => resolve(rq.result || 0);
    rq.onerror = () => resolve(0);
  });
}

window.__avOfflineTiles = {
  cacheTileFromUrl,
  clearCachedTiles,
  countCachedTiles,
  online: () => navigator.onLine,
};

/* ==================== OBD LIVE ENGINE STRIP (Tier 2) ==================== */
function renderObdStrip(payload) {
  const strip = document.getElementById('obdStrip');
  if (!strip) return;
  strip.hidden = false;
  const stateText = document.getElementById('obdStateText');
  const body = document.getElementById('obdBody');
  if (!stateText || !body) return;

  const stat = (label, value, unit) => `
    <div class="obd-stat">
      <span class="obd-stat-value">${value}<small>${unit || ''}</small></span>
      <span class="obd-stat-label">${label}</span>
    </div>`;

  if (payload.state === 'connected') {
    const s = payload;
    stateText.textContent = payload.deviceName || 'Live';
    body.innerHTML = [
      stat('RPM', s.rpm != null ? s.rpm.toLocaleString() : '—', ''),
      stat('Speed', s.speedKmh != null ? s.speedKmh : '—', 'km/h'),
      stat('Coolant', s.coolantC != null ? s.coolantC : '—', '°C'),
      stat('Fuel', s.fuelPct != null ? s.fuelPct : '—', '%'),
    ].join('');
  } else if (payload.state === 'connecting') {
    stateText.textContent = 'Connecting';
    body.innerHTML = `<div class="obd-hint">Linking with ${payload.deviceName || 'adapter'}…</div>`;
  } else if (payload.state === 'permission') {
    stateText.textContent = 'Permission';
    body.innerHTML = `<div class="obd-hint">Bluetooth permission is needed to talk to the adapter.</div>
      <button class="obd-action-btn" id="obdAllowBtn">Allow Bluetooth</button>`;
    const btn = document.getElementById('obdAllowBtn');
    if (btn) btn.onclick = () => window.AutoVerseAndroid.requestObdBluetoothPermission();
  } else if (payload.state === 'error') {
    stateText.textContent = 'Error';
    body.innerHTML = `<div class="obd-hint obd-error">${payload.error || 'Connection failed'}</div>
      <button class="obd-action-btn" id="obdRetryBtn">Retry</button>`;
    const btn = document.getElementById('obdRetryBtn');
    if (btn) btn.onclick = () => window.AutoVerseAndroid.connectObd();
  } else {
    stateText.textContent = 'Off';
    body.innerHTML = `<div class="obd-hint">Plug in an ELM327 Bluetooth adapter to see live engine data.</div>
      <button class="obd-action-btn" id="obdConnectBtn">Connect adapter</button>`;
    const btn = document.getElementById('obdConnectBtn');
    if (btn) btn.onclick = () => window.AutoVerseAndroid.connectObd();
  }
}

// ============================================================================
// DYNAMIC RECENT APPS & FAVORITES STORAGE MANAGER
// ============================================================================
const STORAGE_FAVS = 'autoverse_fav_apps_v2';
const STORAGE_RECENTS = 'autoverse_recent_apps_v2';

function getFavorites() {
  const cached = localStorage.getItem(STORAGE_FAVS);
  if (cached) {
    try {
      const parsed = JSON.parse(cached);
      if (Array.isArray(parsed) && parsed.length > 0) return parsed;
    } catch (e) {}
  }
  const installed = window.__installedApps || [];
  if (installed.length > 0) {
    const preferred = ['map', 'spot', 'phone', 'tube', 'chrom', 'what'];
    const favs = [];
    preferred.forEach(p => {
      const match = installed.find(a => (a.packageName.toLowerCase().includes(p) || a.label.toLowerCase().includes(p)) && !favs.some(f => f.packageName === a.packageName));
      if (match && favs.length < 5) favs.push(match);
    });
    for (let i = 0; i < installed.length && favs.length < 5; i++) {
      if (!favs.some(f => f.packageName === installed[i].packageName)) {
        favs.push(installed[i]);
      }
    }
    localStorage.setItem(STORAGE_FAVS, JSON.stringify(favs));
    return favs;
  }
  return [];
}

function saveFavorites(favs) {
  localStorage.setItem(STORAGE_FAVS, JSON.stringify(favs.slice(0, 5)));
  renderFavoritesShelf();
}

function getRecentApps() {
  const cached = localStorage.getItem(STORAGE_RECENTS);
  if (cached) {
    try {
      const parsed = JSON.parse(cached);
      if (Array.isArray(parsed)) return parsed;
    } catch (e) {}
  }
  return [];
}

function logRecentAppLaunch(app) {
  if (!app || !app.packageName) return;
  const list = getRecentApps().filter(a => a.packageName !== app.packageName);
  list.unshift({
    packageName: app.packageName,
    activityName: app.activityName || '',
    label: app.label || app.packageName,
    iconBase64: app.iconBase64 || '',
    timestamp: Date.now()
  });
  localStorage.setItem(STORAGE_RECENTS, JSON.stringify(list.slice(0, 6)));
  renderRecentAppsShelf();
}

function formatRelativeTime(ts) {
  if (!ts) return 'Recently';
  const diffSec = Math.floor((Date.now() - ts) / 1000);
  if (diffSec < 60) return 'Just now';
  const diffMin = Math.floor(diffSec / 60);
  if (diffMin < 60) return `${diffMin}m ago`;
  const diffHr = Math.floor(diffMin / 60);
  if (diffHr < 24) return `${diffHr}h ago`;
  return `${Math.floor(diffHr / 24)}d ago`;
}

function renderFavoritesShelf() {
  const favGrid = document.getElementById('sidebarFavGrid');
  const homeShortcuts = document.getElementById('homeFavShortcutsRow');
  const favs = getFavorites();

  if (window.__installedApps && favs.length > 0) {
    favs.forEach(fav => {
      const live = window.__installedApps.find(a => a.packageName === fav.packageName);
      if (live && live.iconBase64) fav.iconBase64 = live.iconBase64;
    });
  }

  if (favGrid) {
    favGrid.innerHTML = '';
    favs.forEach(app => {
      const item = document.createElement('div');
      item.className = 'sidebar-fav-item';
      item.dataset.pkg = app.packageName;
      item.dataset.act = app.activityName || '';

      const squircle = document.createElement('div');
      squircle.className = 'apple-squircle sm-squircle bg-white';
      if (app.iconBase64) {
        squircle.innerHTML = `<img src="${app.iconBase64}" alt="${app.label}" style="width:100%;height:100%;border-radius:inherit;object-fit:cover;" /><div class="squircle-specular"></div>`;
      } else {
        squircle.innerHTML = `<span style="font-weight:700;font-size:16px;color:#0066FF;">${app.label ? app.label[0] : 'A'}</span><div class="squircle-specular"></div>`;
      }

      const name = document.createElement('span');
      name.className = 'sm-app-name';
      name.textContent = app.label || 'App';

      item.appendChild(squircle);
      item.appendChild(name);

      item.addEventListener('click', () => {
        logRecentAppLaunch(app);
        showToast(`Opening ${app.label}...`);
        if (window.AutoVerse.isNative()) {
          window.AutoVerseAndroid.launchApp(app.packageName, app.activityName || '');
        }
      });

      favGrid.appendChild(item);
    });

    const addTile = document.createElement('div');
    addTile.className = 'sidebar-fav-item';
    addTile.id = 'addFavTile';
    addTile.innerHTML = `
      <div class="apple-squircle sm-squircle dashed-squircle" style="display:flex;align-items:center;justify-content:center;">
        <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2.5">
          <line x1="12" y1="5" x2="12" y2="19" />
          <line x1="5" y1="12" x2="19" y2="12" />
        </svg>
      </div>
      <span class="sm-app-name">Add</span>
    `;
    addTile.addEventListener('click', () => openFavPickerModal());
    favGrid.appendChild(addTile);
  }

  if (homeShortcuts && favs.length > 0) {
    homeShortcuts.innerHTML = '';
    favs.slice(0, 3).forEach(app => {
      const item = document.createElement('div');
      item.className = 'fav-squircle-item spring-press';
      item.dataset.pkg = app.packageName;

      const squircle = document.createElement('div');
      squircle.className = 'ios-squircle';
      squircle.style.overflow = 'hidden';
      if (app.iconBase64) {
        squircle.innerHTML = `<img src="${app.iconBase64}" alt="${app.label}" style="width:100%;height:100%;border-radius:inherit;object-fit:cover;" /><div class="squircle-gloss"></div>`;
      } else {
        squircle.innerHTML = `<span style="font-weight:700;font-size:18px;color:#ffffff;">${app.label ? app.label[0] : 'A'}</span><div class="squircle-gloss"></div>`;
      }

      const name = document.createElement('span');
      name.className = 'fav-squircle-name';
      name.textContent = app.label || 'App';

      item.appendChild(squircle);
      item.appendChild(name);

      item.addEventListener('click', () => {
        logRecentAppLaunch(app);
        showToast(`Opening ${app.label}...`);
        if (window.AutoVerse.isNative()) {
          window.AutoVerseAndroid.launchApp(app.packageName, app.activityName || '');
        }
      });

      homeShortcuts.appendChild(item);
    });
  }
}

function renderRecentAppsShelf() {
  const listEl = document.getElementById('sidebarRecentList');
  if (!listEl) return;

  const recents = getRecentApps();
  if (recents.length === 0) {
    listEl.innerHTML = `
      <div class="recent-empty-state">
        <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" stroke-width="1.8">
          <circle cx="12" cy="12" r="9"/>
          <polyline points="12 6 12 12 16 14"/>
        </svg>
        <span>No recently used apps</span>
        <span style="font-size:10.5px; opacity:0.7;">Apps you launch will appear here</span>
      </div>
    `;
    return;
  }

  if (window.__installedApps) {
    recents.forEach(rec => {
      const live = window.__installedApps.find(a => a.packageName === rec.packageName);
      if (live && live.iconBase64) rec.iconBase64 = live.iconBase64;
    });
  }

  listEl.innerHTML = '';
  recents.slice(0, 4).forEach(app => {
    const row = document.createElement('div');
    row.className = 'recent-list-row';
    row.dataset.pkg = app.packageName;
    row.dataset.act = app.activityName || '';

    const squircle = document.createElement('div');
    squircle.className = 'apple-squircle xs-squircle bg-white';
    if (app.iconBase64) {
      squircle.innerHTML = `<img src="${app.iconBase64}" alt="${app.label}" style="width:100%;height:100%;border-radius:inherit;object-fit:cover;" /><div class="squircle-specular"></div>`;
    } else {
      squircle.innerHTML = `<span style="font-weight:700;font-size:14px;color:#0066FF;">${app.label ? app.label[0] : 'A'}</span><div class="squircle-specular"></div>`;
    }

    const info = document.createElement('div');
    info.className = 'recent-row-info';

    const pri = document.createElement('span');
    pri.className = 'recent-primary';
    pri.textContent = app.label || app.packageName;

    const sec = document.createElement('span');
    sec.className = 'recent-secondary';
    sec.textContent = formatRelativeTime(app.timestamp);

    info.appendChild(pri);
    info.appendChild(sec);

    row.appendChild(squircle);
    row.appendChild(info);

    row.addEventListener('click', () => {
      logRecentAppLaunch(app);
      showToast(`Resuming ${app.label}...`);
      if (window.AutoVerse.isNative()) {
        window.AutoVerseAndroid.launchApp(app.packageName, app.activityName || '');
      }
    });

    listEl.appendChild(row);
  });
}

function openFavPickerModal() {
  const modal = document.getElementById('favModalOverlay');
  const listEl = document.getElementById('favPickerList');
  const searchInput = document.getElementById('favSearchInput');
  if (!modal || !listEl) return;

  const installed = window.__installedApps || [];
  let currentFavs = getFavorites();
  let selectedPkgs = new Set(currentFavs.map(f => f.packageName));

  function renderList(query = '') {
    listEl.innerHTML = '';
    const q = query.trim().toLowerCase();
    const filtered = installed.filter(a => !q || a.label.toLowerCase().includes(q) || a.packageName.toLowerCase().includes(q));

    if (filtered.length === 0) {
      listEl.innerHTML = '<div style="padding:16px;text-align:center;color:var(--text-tertiary);font-size:13px;">No matching apps found</div>';
      return;
    }

    filtered.forEach(app => {
      const isSelected = selectedPkgs.has(app.packageName);
      const item = document.createElement('div');
      item.className = `fav-picker-item ${isSelected ? 'selected' : ''}`;

      item.innerHTML = `
        <div class="fav-picker-icon">
          ${app.iconBase64 ? `<img src="${app.iconBase64}" style="width:100%;height:100%;border-radius:inherit;object-fit:cover;" />` : ''}
        </div>
        <span class="fav-picker-title">${app.label}</span>
        <div class="fav-picker-check">${isSelected ? '✓' : ''}</div>
      `;

      item.addEventListener('click', () => {
        if (selectedPkgs.has(app.packageName)) {
          selectedPkgs.delete(app.packageName);
        } else {
          if (selectedPkgs.size >= 5) {
            showToast("Maximum 5 favorites allowed");
            return;
          }
          selectedPkgs.add(app.packageName);
        }
        renderList(searchInput ? searchInput.value : '');
      });

      listEl.appendChild(item);
    });
  }

  if (searchInput) {
    searchInput.value = '';
    searchInput.oninput = () => renderList(searchInput.value);
  }

  renderList();
  modal.classList.add('active', 'open');

  const btnDone = document.getElementById('btnFavModalDone');
  if (btnDone) {
    btnDone.onclick = () => {
      const newFavs = installed.filter(a => selectedPkgs.has(a.packageName));
      saveFavorites(newFavs);
      modal.classList.remove('active', 'open');
      showToast("Favourites updated");
    };
  }

  const btnClose = document.getElementById('btnFavModalClose');
  if (btnClose) {
    btnClose.onclick = () => modal.classList.remove('active', 'open');
  }
}

document.addEventListener('DOMContentLoaded', () => {
  // Load cached installed apps immediately for instant rendering on frame 1
  const cachedAppsRaw = localStorage.getItem('autoverse_installed_apps_cache');
  if (cachedAppsRaw) {
    try {
      const parsedApps = JSON.parse(cachedAppsRaw);
      if (Array.isArray(parsedApps) && parsedApps.length > 0) {
        window.AutoVerse.updateInstalledApps(parsedApps);
      }
    } catch (e) {}
  }

  initDockNavigation();
  initAppleClock();
  initAppleMediaPlayer();
  initCockpitControls();
  initAppLibrary();
  initNavigationScreen();
  initAppleSettings();
  initDynamicIsland();

  // If running inside Android WebView, request initial data
  if (window.AutoVerse.isNative()) {
    try {
      window.AutoVerseAndroid.requestInitialState();
      if (typeof window.AutoVerseAndroid.requestLocation === 'function') {
        window.AutoVerseAndroid.requestLocation();
      }
    } catch (e) {
      console.warn("AutoVerseBridge error on init:", e);
    }
  }
});

/* ==========================================================================
   FEEDBACK TOAST NOTIFICATION HELPER
   ========================================================================== */
function showToast(msg) {
  const toast = document.getElementById('cockpitToast');
  const msgEl = document.getElementById('toastMessage');
  if (toast && msgEl) {
    msgEl.textContent = msg;
    toast.classList.add('show');
    clearTimeout(window.__toastTimeout);
    window.__toastTimeout = setTimeout(() => {
      toast.classList.remove('show');
    }, 2400);
  }
}

/* ==========================================================================
   1. AUTOVERSE FLOATING DOCK (NAVIGATION)
   ========================================================================== */
function initDockNavigation() {
  const dockTiles = document.querySelectorAll('.dock-tile');
  const screens = document.querySelectorAll('.screen-canvas');

  function switchTab(tabId) {
    dockTiles.forEach(tile => {
      if (tile.dataset.tab === tabId) {
        tile.classList.add('active');
      } else {
        tile.classList.remove('active');
      }
    });

    screens.forEach(screen => {
      if (screen.id === `tab-${tabId}`) {
        screen.classList.add('active');
        if (tabId === 'apps') {
          const tiles = screen.querySelectorAll('.app-spring-tile');
          tiles.forEach(t => {
            t.style.animation = 'none';
            t.offsetHeight; // trigger reflow
            t.style.animation = '';
          });
        }
        if (tabId === 'navigation' && typeof window.__invalidateNavMap === 'function') {
          setTimeout(window.__invalidateNavMap, 60);
        }
        if (tabId === 'home' && typeof window.__invalidateHomeMap === 'function') {
          setTimeout(window.__invalidateHomeMap, 60);
        }
      } else {
        screen.classList.remove('active');
      }
    });
  }

  dockTiles.forEach(tile => {
    tile.addEventListener('click', () => {
      switchTab(tile.dataset.tab);
    });
  });

  // Action links (e.g., "OPEN NAVIGATION" button or turn pill)
  document.querySelectorAll('[data-nav-target]').forEach(el => {
    el.addEventListener('click', () => {
      switchTab(el.dataset.navTarget);
    });
  });

  initThemeSounds();
  initOfflineVoiceToggle();

  // Voice Assistant Orb click - real speech recognition via the Android bridge
  const siriBtn = document.getElementById('siriOrbBtn');
  if (siriBtn) {
    siriBtn.addEventListener('click', () => {
      if (window.AutoVerse.isNative()) {
        window.AutoVerseAndroid.startVoiceRecognition('command');
        setVoiceListening();
      } else {
        showToast("Voice works in the Android app — say \"open Spotify\" or \"navigate to India Gate\"");
      }
    });
  }
}

/* ==================== VOICE COMMAND ROUTING ==================== */
function setVoiceListening() {
  const orb = document.getElementById('siriOrbBtn');
  const island = document.getElementById('islandTrack');
  if (orb) orb.classList.add('listening');
  if (island) island.textContent = "Listening…";
}

function clearVoiceListening() {
  const orb = document.getElementById('siriOrbBtn');
  if (orb) orb.classList.remove('listening');
}

function findVoiceApp(name) {
  const apps = window.__installedApps || [];
  const q = String(name || '').toLowerCase().trim();
  if (!q) return null;
  return apps.find(a => (a.label || '').toLowerCase() === q)
      || apps.find(a => (a.label || '').toLowerCase().startsWith(q))
      || apps.find(a => (a.label || '').toLowerCase().includes(q))
      || apps.find(a => (a.packageName || '').toLowerCase().includes(q));
}

function dispatchVoiceNavigation(place) {
  const target = String(place || '').trim();
  if (!target) return;
  if (window.AutoVerse.isNative()) {
    window.AutoVerseAndroid.openNavigation(target);
    showToast(`Navigation: ${target}`);
  } else {
    showToast("Navigation handoff needs the Android app");
  }
}

function dispatchVoiceCommand(text) {
  const t = String(text || '').trim();
  const native = window.AutoVerse.isNative();

  // Media transport verbs
  if (/^(play|resume)(\s+(some\s+)?(music|song|track))?$/i.test(t)) {
    if (native) window.AutoVerseAndroid.sendMediaCommand('PLAY');
    showToast("Media: play");
    return;
  }
  if (/^pause$/i.test(t)) {
    if (native) window.AutoVerseAndroid.sendMediaCommand('PAUSE');
    showToast("Media: pause");
    return;
  }
  if (/^(next|skip)$/i.test(t)) {
    if (native) window.AutoVerseAndroid.sendMediaCommand('NEXT');
    showToast("Media: next track");
    return;
  }
  if (/^(previous|go back)$/i.test(t)) {
    if (native) window.AutoVerseAndroid.sendMediaCommand('PREVIOUS');
    showToast("Media: previous track");
    return;
  }

  // App launching: "open spotify"
  const openMatch = t.match(/^(?:open|launch|start|open up|go to)\s+(.+)$/i);
  if (openMatch) {
    const target = openMatch[1].replace(/\s+app$/i, '').trim();
    const app = findVoiceApp(target);
    if (app) {
      if (native) window.AutoVerseAndroid.launchApp(app.packageName, app.activityName);
      showToast(`Opening ${app.label}`);
      return;
    }
    if (native) {
      window.AutoVerseAndroid.launchAppByName(target);
      showToast(`Trying to open "${target}"…`);
    } else {
      showToast(`No app named "${target}" found`);
    }
    return;
  }

  // Navigation: "navigate to India Gate"
  const navMatch = t.match(/^(?:navigate to|directions? to|route to|drive to|take me to|where is)\s+(.+)$/i);
  if (navMatch) {
    dispatchVoiceNavigation(navMatch[1]);
    return;
  }

  showToast('Say "open <app>", "navigate to <place>", or play / pause / next');
}

/* ==========================================================================
   2. APPLE DIGITAL CLOCK & CALENDAR
   ========================================================================== */
function initAppleClock() {
  const clockEl = document.getElementById('clockDisplay');
  const miniClockEl = document.getElementById('miniPreviewClock');
  const dayEl = document.getElementById('dayDisplay');
  const dateEl = document.getElementById('dateDisplay');

  function update() {
    const now = new Date();
    const hours = String(now.getHours()).padStart(2, '0');
    const minutes = String(now.getMinutes()).padStart(2, '0');
    if (clockEl) clockEl.textContent = `${hours}:${minutes}`;
    if (miniClockEl) miniClockEl.textContent = `${hours}:${minutes}`;

    const days = ['SUNDAY', 'MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY'];
    const months = ['JANUARY', 'FEBRUARY', 'MARCH', 'APRIL', 'MAY', 'JUNE', 'JULY', 'AUGUST', 'SEPTEMBER', 'OCTOBER', 'NOVEMBER', 'DECEMBER'];
    
    if (dayEl) dayEl.textContent = days[now.getDay()];
    if (dateEl) dateEl.textContent = `${months[now.getMonth()]} ${now.getDate()}`;
  }

  update();
  setInterval(update, 1000);
}

/* ==========================================================================
   3. APPLE MUSIC PLAYER (MEDIA SESSION & SCRUBBER)
   ========================================================================== */
function initAppleMediaPlayer() {
  const playPauseBtn = document.getElementById('playPauseBtn');
  const iconPlay = playPauseBtn ? playPauseBtn.querySelector('.icon-play') : null;
  const iconPause = playPauseBtn ? playPauseBtn.querySelector('.icon-pause') : null;
  const progressBar = document.getElementById('progressBar');
  const progressFill = document.getElementById('progressFill');
  const progressThumb = document.getElementById('progressThumb');
  const currentTimeEl = document.getElementById('currentTime');
  const totalTimeEl = document.getElementById('totalTime');
  const likeBtn = document.getElementById('likeBtn');
  const prevBtn = document.getElementById('prevTrackBtn');
  const nextBtn = document.getElementById('nextTrackBtn');
  const trackTitle = document.getElementById('trackTitle');
  const trackArtist = document.getElementById('trackArtist');
  const islandTrack = document.getElementById('islandTrack');
  const heroMediaBento = document.querySelector('.hero-media-bento');
  const islandPlayBtn = document.getElementById('islandPlayBtn');

  let isPlaying = false;
  let currentSec = 0;
  let playlistIndex = 0;

  const playlist = window.AutoVerse.isNative() ? [] : [
    { title: 'Audio Stream', artist: 'AutoVerse Audio Engine', duration: 180 }
  ];

  function formatTime(sec) {
    if (!sec || isNaN(sec) || sec <= 0) return '0:00';
    const m = Math.floor(sec / 60);
    const s = Math.floor(sec % 60);
    return `${m}:${String(s).padStart(2, '0')}`;
  }

  function updateScrubber() {
    const dur = window.AutoVerse.isNative() ? (window.AutoVerse.currentDurationSec || 0) : (playlist[playlistIndex]?.duration || 0);
    const cur = window.AutoVerse.isNative() ? (window.AutoVerse.currentPositionSec || 0) : currentSec;
    const pct = dur > 0 ? Math.min(100, Math.max(0, (cur / dur) * 100)) : 0;

    if (progressFill) progressFill.style.width = `${pct}%`;
    if (progressThumb) progressThumb.style.left = `${pct}%`;
    if (currentTimeEl) currentTimeEl.textContent = formatTime(cur);
    if (totalTimeEl) totalTimeEl.textContent = formatTime(dur);
  }

  function loadTrack(idx) {
    if (window.AutoVerse.isNative() || playlist.length === 0) return;
    playlistIndex = (idx + playlist.length) % playlist.length;
    const cur = playlist[playlistIndex];
    if (trackTitle) trackTitle.textContent = cur.title;
    if (trackArtist) trackArtist.textContent = cur.artist;
    if (islandTrack) islandTrack.textContent = `${cur.title} • ${cur.artist}`;
    currentSec = 0;
    updateScrubber();
  }

  function setPlayState(play) {
    isPlaying = play;
    window.AutoVerse.isPlaying = play;
    if (iconPause && iconPlay) {
      iconPause.style.display = isPlaying ? 'block' : 'none';
      iconPlay.style.display = isPlaying ? 'none' : 'block';
    }
    if (heroMediaBento) {
      heroMediaBento.classList.toggle('is-paused', !isPlaying);
    }
    if (islandPlayBtn) {
      islandPlayBtn.textContent = isPlaying ? '⏸' : '▶';
    }
    const islandBars = document.querySelector('.island-bars');
    if (islandBars) {
      islandBars.style.opacity = isPlaying ? '1' : '0.3';
    }
  }

  if (playPauseBtn) {
    playPauseBtn.addEventListener('click', () => {
      if (window.AutoVerse.isNative()) {
        window.AutoVerseAndroid.sendMediaCommand('PLAY_PAUSE');
      } else {
        setPlayState(!isPlaying);
      }
    });
  }

  if (prevBtn) {
    prevBtn.addEventListener('click', () => {
      if (window.AutoVerse.isNative()) {
        window.AutoVerseAndroid.sendMediaCommand('PREVIOUS');
      } else {
        loadTrack(playlistIndex - 1);
      }
    });
  }

  if (nextBtn) {
    nextBtn.addEventListener('click', () => {
      if (window.AutoVerse.isNative()) {
        window.AutoVerseAndroid.sendMediaCommand('NEXT');
      } else {
        loadTrack(playlistIndex + 1);
      }
    });
  }

  if (likeBtn) {
    likeBtn.addEventListener('click', () => {
      likeBtn.classList.toggle('liked');
    });
  }

  if (progressBar) {
    progressBar.addEventListener('click', (e) => {
      const rect = progressBar.getBoundingClientRect();
      const clickX = e.clientX - rect.left;
      const ratio = Math.max(0, Math.min(1, clickX / rect.width));

      if (window.AutoVerse.isNative()) {
        const targetMs = Math.round(ratio * window.AutoVerse.currentDurationSec * 1000);
        window.AutoVerseAndroid.sendMediaCommand(`SEEK:${targetMs}`);
      } else {
        currentSec = Math.floor(ratio * playlist[playlistIndex].duration);
        updateScrubber();
      }
    });
  }

  if (islandPlayBtn) {
    islandPlayBtn.addEventListener('click', (e) => {
      e.stopPropagation();
      if (window.AutoVerse.isNative()) {
        window.AutoVerseAndroid.sendMediaCommand('PLAY_PAUSE');
      } else {
        setPlayState(!isPlaying);
      }
    });
  }

  const islandNextBtn = document.getElementById('islandNextBtn');
  if (islandNextBtn) {
    islandNextBtn.addEventListener('click', (e) => {
      e.stopPropagation();
      if (window.AutoVerse.isNative()) {
        window.AutoVerseAndroid.sendMediaCommand('NEXT');
      } else {
        loadTrack(playlistIndex + 1);
      }
    });
  }

  // Tapping media card prompts permission if needed or launches music app
  if (heroMediaBento) {
    heroMediaBento.addEventListener('click', (e) => {
      if (e.target.closest('button') || e.target.closest('.apple-scrubber-wrap')) return;
      if (window.AutoVerse.isNative()) {
        if (!window.AutoVerseAndroid.isNotificationAccessGranted()) {
          window.AutoVerseAndroid.openNotificationAccessSettings();
        } else {
          window.AutoVerseAndroid.launchAppByName('Spotify');
        }
      }
    });
  }

  // Live playback timer tick for browser preview fallback
  setInterval(() => {
    if (!window.AutoVerse.isNative() && isPlaying) {
      currentSec++;
      if (currentSec > playlist[playlistIndex].duration) {
        loadTrack(playlistIndex + 1);
      } else {
        updateScrubber();
      }
    }
  }, 1000);

  updateScrubber();
}

/* ==========================================================================
   4. AUTOVERSE COCKPIT CONTROLS & NIGHT DIMMER
   ========================================================================== */
function initCockpitControls() {
  const modeChips = document.querySelectorAll('.mode-chip');
  const badge = document.getElementById('driveBadge');
  const diagProfileVal = document.getElementById('diagProfileVal');
  const headlightDimmerBtn = document.getElementById('headlightDimmerBtn');
  const quickDimmerBtn = document.getElementById('quickDimmerBtn');
  const masterMuteBtn = document.getElementById('masterMuteBtn');
  const resetPrefsBtn = document.getElementById('resetPrefsBtn');

  // Workload Profile switching (Constrained, Balanced, Enhanced)
  modeChips.forEach(chip => {
    chip.addEventListener('click', (e) => {
      e.stopPropagation();
      modeChips.forEach(c => c.classList.remove('active'));
      chip.classList.add('active');

      const profile = chip.dataset.profile || chip.textContent.trim();
      if (badge) {
        if (profile === 'Enhanced') badge.textContent = 'ENHANCED (60 FPS)';
        else if (profile === 'Constrained') badge.textContent = 'CONSTRAINED (ECO)';
        else badge.textContent = 'BALANCED PROFILE';
      }

      if (diagProfileVal) {
        diagProfileVal.textContent = profile;
      }

      showToast(`Workload Profile: ${profile}`);

      if (window.AutoVerse.isNative()) {
        window.AutoVerseAndroid.setProfile(profile);
      }
    });
  });

  // Toggle Night Dimmer / Blackout Mode
  function toggleDimmer() {
    const isDim = document.body.classList.toggle('dimmer-mode');
    showToast(isDim ? "Night Blackout Dimmer: ON" : "Night Blackout Dimmer: OFF");
  }

  if (headlightDimmerBtn) headlightDimmerBtn.addEventListener('click', toggleDimmer);
  if (quickDimmerBtn) quickDimmerBtn.addEventListener('click', toggleDimmer);

  // Toggle Master Audio Mute
  if (masterMuteBtn) {
    masterMuteBtn.addEventListener('click', () => {
      const isMuted = document.body.classList.toggle('audio-muted');
      showToast(isMuted ? "Master Audio: Muted" : "Master Audio: Active");
    });
  }

  // Car model engine rev feedback
  const carModelImg = document.getElementById('homeVehicleCar') || document.querySelector('.car-render-img');
  if (carModelImg) {
    carModelImg.addEventListener('click', () => {
      carModelImg.classList.add('revved');
      showToast("Cockpit Telemetry: All Systems Nominal • 60 FPS Locked");
      setTimeout(() => {
        carModelImg.classList.remove('revved');
      }, 500);
    });
  }

  // Automotive Brand Emblem
  const brandEmblem = document.getElementById('brandEmblem');
  if (brandEmblem) {
    brandEmblem.addEventListener('click', () => {
      showToast("AutoVerse Cockpit OS 18.2 — 100% Free Automotive Edition");
    });
  }

  // Home live map controls
  const homeRecenterBtn = document.getElementById('homeRecenterBtn');
  const homeZoomInBtn = document.getElementById('homeZoomInBtn');
  const homeZoomOutBtn = document.getElementById('homeZoomOutBtn');

  if (homeRecenterBtn) {
    homeRecenterBtn.addEventListener('click', () => {
      if (typeof window.__homeMapRecenter === 'function') {
        window.__homeMapRecenter();
      }
    });
  }
  if (homeZoomInBtn) {
    homeZoomInBtn.addEventListener('click', () => {
      if (typeof window.__homeMapZoomIn === 'function') {
        window.__homeMapZoomIn();
      }
    });
  }
  if (homeZoomOutBtn) {
    homeZoomOutBtn.addEventListener('click', () => {
      if (typeof window.__homeMapZoomOut === 'function') {
        window.__homeMapZoomOut();
      }
    });
  }

  // Reset Cockpit Preferences
  if (resetPrefsBtn) {
    resetPrefsBtn.addEventListener('click', () => {
      document.body.className = 'theme-cobalt';
      document.documentElement.style.fontSize = '16px';
      document.documentElement.style.removeProperty('--accent-primary');
      document.documentElement.style.removeProperty('--accent-glow');
      const fontSizeSlider = document.getElementById('fontSizeSlider');
      if (fontSizeSlider) fontSizeSlider.value = 16;
      const diagThemeVal = document.getElementById('diagThemeVal');
      if (diagThemeVal) diagThemeVal.textContent = 'Cobalt (Dark)';
      const miniBadge = document.getElementById('miniThemeBadge');
      if (miniBadge) miniBadge.textContent = 'COBALT • RETINA';
      showToast("Cockpit preferences reset to factory defaults");
    });
  }
}

/* ==========================================================================
   5. APPLE APP LIBRARY (SPOTLIGHT SEARCH, CATEGORIES, SQUIRCLE TACTILE)
   ========================================================================== */
function initAppLibrary() {
  const searchInput = document.getElementById('appSearchInput');
  const categoryPills = document.querySelectorAll('#appCategoryPills .filter-capsule');
  const appTiles = document.querySelectorAll('#appsGridContainer .app-spring-tile');
  const viewBtnGrid = document.getElementById('viewBtnGrid');
  const viewBtnList = document.getElementById('viewBtnList');
  const appsGridContainer = document.getElementById('appsGridContainer');

  let activeCategory = 'all';
  let searchTerm = '';

  function filterApps() {
    const currentTiles = document.querySelectorAll('#appsGridContainer .app-spring-tile');
    currentTiles.forEach(tile => {
      const name = (tile.dataset.name || '').toLowerCase();
      const cat = (tile.dataset.cat || '').toLowerCase();

      const matchesSearch = name.includes(searchTerm);
      const matchesCat = (activeCategory === 'all') || (cat === activeCategory);

      if (matchesSearch && matchesCat) {
        tile.classList.remove('hidden');
      } else {
        tile.classList.add('hidden');
      }
    });
  }

  if (searchInput) {
    searchInput.addEventListener('input', (e) => {
      searchTerm = e.target.value.trim().toLowerCase();
      filterApps();
    });
  }

  categoryPills.forEach(pill => {
    pill.addEventListener('click', () => {
      categoryPills.forEach(p => p.classList.remove('active'));
      pill.classList.add('active');
      activeCategory = pill.dataset.cat;
      filterApps();
    });
  });

  // View Layout Switcher (Grid vs List)
  if (viewBtnGrid && viewBtnList && appsGridContainer) {
    viewBtnGrid.addEventListener('click', () => {
      viewBtnGrid.classList.add('active');
      viewBtnList.classList.remove('active');
      appsGridContainer.classList.remove('list-view');
      showToast("App Library: Grid View (4×6)");
    });
    viewBtnList.addEventListener('click', () => {
      viewBtnList.classList.add('active');
      viewBtnGrid.classList.remove('active');
      appsGridContainer.classList.add('list-view');
      showToast("App Library: Panoramic List View");
    });
  }

  // Tactile click feedback & app launcher trigger
  appTiles.forEach(tile => {
    tile.addEventListener('click', () => {
      tile.classList.add('app-launch-feedback');
      setTimeout(() => tile.classList.remove('app-launch-feedback'), 400);

      const appName = tile.dataset.name || 'App';
      showToast(`Opening ${appName}...`);

      const islandText = document.getElementById('islandTrack');
      if (islandText) {
        const originalText = islandText.textContent;
        islandText.textContent = `Opening ${appName}...`;
        setTimeout(() => {
          islandText.textContent = originalText;
        }, 2200);
      }

      if (window.AutoVerse.isNative()) {
        const pkg = tile.dataset.pkg;
        const act = tile.dataset.activity || '';
        if (pkg) {
          window.AutoVerseAndroid.launchApp(pkg, act);
        } else {
          window.AutoVerseAndroid.launchAppByName(appName);
        }
      }
    });
  });

  // Home favourites shortcuts
  document.querySelectorAll('.fav-squircle-item').forEach(fav => {
    fav.addEventListener('click', () => {
      const targetApp = fav.dataset.app;
      showToast(`Opening ${targetApp}...`);
      if (window.AutoVerse.isNative()) {
        if (targetApp === 'phone') window.AutoVerseAndroid.launchAppByName('Phone');
        else if (targetApp === 'music') window.AutoVerseAndroid.launchAppByName('Spotify');
        else if (targetApp === 'maps') window.AutoVerseAndroid.launchAppByName('Maps');
      }
    });
  });

  // Sidebar favourites tiles in App Drawer
  document.querySelectorAll('.sidebar-fav-item[data-app]').forEach(item => {
    item.addEventListener('click', () => {
      const app = item.dataset.app;
      showToast(`Opening ${app}...`);
      if (window.AutoVerse.isNative()) {
        window.AutoVerseAndroid.launchAppByName(app);
      }
    });
  });

  // Add Favorite Tile
  const addFavTile = document.getElementById('addFavTile');
  if (addFavTile) {
    addFavTile.addEventListener('click', () => {
      showToast("Tap any app in the library to pin to Favourites");
    });
  }

  // Edit Favorites Button
  const btnEditFavs = document.getElementById('btnEditFavs');
  if (btnEditFavs) {
    btnEditFavs.addEventListener('click', () => {
      openFavPickerModal();
    });
  }

  // Render initial favorites and recents
  renderFavoritesShelf();
  renderRecentAppsShelf();

  // Recently used rows
  document.querySelectorAll('.recent-list-row[data-app]').forEach(row => {
    row.addEventListener('click', () => {
      const app = row.dataset.app;
      showToast(`Resuming ${app}...`);
      if (window.AutoVerse.isNative()) {
        window.AutoVerseAndroid.launchAppByName(app);
      }
    });
  });

  // Recent details chevron
  const btnRecentDetails = document.getElementById('btnRecentDetails');
  if (btnRecentDetails) {
    btnRecentDetails.addEventListener('click', () => {
      showToast("Recent Activity: Last 4 sessions logged locally");
    });
  }

  // Voice Dictate button in app search - fills the search field from speech
  const siriDictateBtn = document.querySelector('.siri-dictate-btn');
  if (siriDictateBtn) {
    siriDictateBtn.addEventListener('click', () => {
      if (window.AutoVerse.isNative()) {
        window.AutoVerseAndroid.startVoiceRecognition('apps');
        setVoiceListening();
      } else {
        showToast("Voice search works in the Android app");
      }
    });
  }
}

/* ==========================================================================
   6. REAL INTERACTIVE GOOGLE MAPS ENGINE & DYNAMIC AUTOMOTIVE PLACES
   ========================================================================== */
function initNavigationScreen() {
  // --- Persistent Storage Keys ---
  const STORAGE_SAVED = 'autoverse_saved_places_v2';
  const STORAGE_RECENTS = 'autoverse_recent_places_v2';
  const STORAGE_COORDS = 'autoverse_last_coords';

  // --- Dynamic Coords State ---
  let userCoords = null;
  const cached = localStorage.getItem(STORAGE_COORDS);
  if (cached) {
    try { userCoords = JSON.parse(cached); } catch (e) {}
  }
  const defaultLat = userCoords?.lat || 28.6139;
  const defaultLng = userCoords?.lng || 77.2090;

  // Active navigation target
  let activeDestination = null;

  // --- Elements ---
  const liveNavMapEl = document.getElementById('liveNavMap');
  const liveHomeMapEl = document.getElementById('liveHomeMap');
  const tabRecent = document.getElementById('tabPlacesRecent');
  const tabSaved = document.getElementById('tabPlacesSaved');
  const placesScrollShelf = document.getElementById('placesScrollShelf');
  const btnAddSavedPlace = document.getElementById('btnAddSavedPlace');
  const btnClearRecents = document.getElementById('btnClearRecents');
  const btnGetDirections = document.getElementById('btnGetDirections');
  const mapsSearchInput = document.querySelector('.maps-search-input');
  const mapsSiriMic = document.querySelector('.maps-siri-mic');
  const navRecenterBtn = document.getElementById('navRecenterBtn');
  const navZoomInBtn = document.getElementById('navZoomInBtn');
  const navZoomOutBtn = document.getElementById('navZoomOutBtn');
  const navEngineLegal = document.getElementById('navEngineLegal');
  const homeGpsStatusText = document.getElementById('homeGpsStatusText');
  const homeNavGuidanceCard = document.getElementById('homeNavGuidanceCard');
  const homeNavTag = document.getElementById('homeNavTag');
  const homeNavManeuver = document.getElementById('homeNavManeuver');
  const homeNavEtaMin = document.getElementById('homeNavEtaMin');
  const homeNavEtaDist = document.getElementById('homeNavEtaDist');

  // Place Modal Elements
  const placeModalOverlay = document.getElementById('placeModalOverlay');
  const placeModalTitle = document.getElementById('placeModalTitle');
  const placeModalName = document.getElementById('placeModalName');
  const placeModalAddress = document.getElementById('placeModalAddress');
  const btnPlaceModalUseGps = document.getElementById('btnPlaceModalUseGps');
  const btnPlaceModalClose = document.getElementById('btnPlaceModalClose');
  const btnPlaceModalCancel = document.getElementById('btnPlaceModalCancel');
  const btnPlaceModalSave = document.getElementById('btnPlaceModalSave');
  let currentEditingPlaceId = null;

  // --- Active Tab for Places Drawer ---
  let activePlacesTab = 'recent';

  // --- Google Maps Road Tile Layer URL ---
  const googleTileLayerUrl = 'https://{s}.google.com/vt/lyrs=m&x={x}&y={y}&z={z}';
  const tileOptions = {
    subdomains: ['mt0', 'mt1', 'mt2', 'mt3'],
    maxZoom: 20,
    attribution: '&copy; Google Maps'
  };

  // --- Leaflet Custom Icons ---
  const gpsPuckIcon = (typeof L !== 'undefined') ? L.divIcon({
    className: 'cockpit-gps-puck-wrapper',
    html: '<div class="cockpit-gps-puck"><div class="cockpit-puck-core"></div><div class="cockpit-puck-pulse"></div></div>',
    iconSize: [24, 24],
    iconAnchor: [12, 12]
  }) : null;

  const destPinIcon = (typeof L !== 'undefined') ? L.divIcon({
    className: 'cockpit-dest-pin-wrapper',
    html: `<div class="cockpit-dest-pin">
      <svg viewBox="0 0 24 24" width="30" height="30" fill="#EF4444" stroke="#FFFFFF" stroke-width="1.8">
        <path d="M12 2C8.13 2 5 5.13 5 9c0 5.25 7 13 7 13s7-7.75 7-13c0-3.87-3.13-7-7-7z"/>
        <circle fill="#FFFFFF" cx="12" cy="9" r="3"/>
      </svg>
    </div>`,
    iconSize: [30, 30],
    iconAnchor: [15, 30]
  }) : null;

  // --- Initialize Maps ---
  let navMap = null;
  let homeMap = null;
  let navGpsMarker = null;
  let homeGpsMarker = null;
  let navDestMarker = null;
  let navRoutePolyline = null;

  if (typeof L !== 'undefined') {
    // 1. Navigation Screen Map
    if (liveNavMapEl) {
      try {
        navMap = L.map('liveNavMap', {
          zoomControl: false,
          attributionControl: false
        }).setView([defaultLat, defaultLng], 15);

        const navTileLayer = L.tileLayer(googleTileLayerUrl, tileOptions).addTo(navMap);
        navTileLayer.on('tileload', (e) => {
          if (window.__avOfflineTiles) window.__avOfflineTiles.cacheTileFromUrl(e.tile && e.tile.src);
        });

        if (gpsPuckIcon) {
          navGpsMarker = L.marker([defaultLat, defaultLng], { icon: gpsPuckIcon }).addTo(navMap);
        }

        navMap.on('click', (e) => {
          handleSelectCoordinates(e.latlng.lat, e.latlng.lng, 'Selected Point');
        });
      } catch (err) {
        console.error("Error creating navMap:", err);
      }
    }

    // 2. Home Screen Hero Map
    if (liveHomeMapEl) {
      try {
        homeMap = L.map('liveHomeMap', {
          zoomControl: false,
          attributionControl: false
        }).setView([defaultLat, defaultLng], 14);

        const homeTileLayer = L.tileLayer(googleTileLayerUrl, tileOptions).addTo(homeMap);
        homeTileLayer.on('tileload', (e) => {
          if (window.__avOfflineTiles) window.__avOfflineTiles.cacheTileFromUrl(e.tile && e.tile.src);
        });

        if (gpsPuckIcon) {
          homeGpsMarker = L.marker([defaultLat, defaultLng], { icon: gpsPuckIcon }).addTo(homeMap);
        }

        homeMap.on('click', () => {
          const navTile = document.querySelector('.dock-tile[data-tab="navigation"]');
          if (navTile) navTile.click();
        });
      } catch (err) {
        console.error("Error creating homeMap:", err);
      }
    }
  }

  // Map Invalidation hooks for tab switches
  window.__invalidateNavMap = function () {
    if (navMap) {
      navMap.invalidateSize();
      if (userCoords?.lat) navMap.panTo([userCoords.lat, userCoords.lng], { animate: false });
    }
  };

  window.__invalidateHomeMap = function () {
    if (homeMap) {
      homeMap.invalidateSize();
      if (userCoords?.lat) homeMap.panTo([userCoords.lat, userCoords.lng], { animate: false });
    }
  };

  // Home map controls hooks
  window.__homeMapRecenter = function () {
    if (homeMap && userCoords?.lat) {
      homeMap.flyTo([userCoords.lat, userCoords.lng], 15, { duration: 0.8 });
      showToast(`GPS Centered: ${userCoords.city || 'Current Location'}`);
    } else if (homeMap) {
      homeMap.flyTo([defaultLat, defaultLng], 15);
      showToast("Acquiring GPS fix...");
    }
  };
  window.__homeMapZoomIn = function () {
    if (homeMap) {
      homeMap.zoomIn();
      showToast(`Dashboard Map Zoom: ${homeMap.getZoom()}`);
    }
  };
  window.__homeMapZoomOut = function () {
    if (homeMap) {
      homeMap.zoomOut();
      showToast(`Dashboard Map Zoom: ${homeMap.getZoom()}`);
    }
  };

  // Nav map controls
  if (navRecenterBtn) {
    navRecenterBtn.addEventListener('click', () => {
      if (navMap && userCoords?.lat) {
        navMap.flyTo([userCoords.lat, userCoords.lng], 16, { duration: 0.8 });
        showToast(`Centered: ${userCoords.city || 'Current Location'} (GPS Active)`);
      } else if (navMap) {
        navMap.flyTo([defaultLat, defaultLng], 15);
        showToast("GPS Active: Searching for satellite fix...");
      }
    });
  }
  if (navZoomInBtn) {
    navZoomInBtn.addEventListener('click', () => {
      if (navMap) navMap.zoomIn();
    });
  }
  if (navZoomOutBtn) {
    navZoomOutBtn.addEventListener('click', () => {
      if (navMap) navMap.zoomOut();
    });
  }

  // --- Haversine Distance Helper ---
  function getDistanceKm(lat1, lon1, lat2, lon2) {
    if (!lat1 || !lon1 || !lat2 || !lon2) return null;
    const R = 6371;
    const dLat = (lat2 - lat1) * Math.PI / 180;
    const dLon = (lon2 - lon1) * Math.PI / 180;
    const a =
      Math.sin(dLat / 2) * Math.sin(dLat / 2) +
      Math.cos(lat1 * Math.PI / 180) * Math.cos(lat2 * Math.PI / 180) *
      Math.sin(dLon / 2) * Math.sin(dLon / 2);
    const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    return R * c;
  }

  function formatDistance(distKm) {
    if (distKm === null || isNaN(distKm)) return null;
    if (distKm < 1) return `${Math.round(distKm * 1000)} m`;
    return `${distKm.toFixed(1)} km`;
  }

  function formatRelativeTime(timestamp) {
    if (!timestamp) return '';
    const diffSec = Math.floor((Date.now() - timestamp) / 1000);
    if (diffSec < 60) return 'Just now';
    const diffMin = Math.floor(diffSec / 60);
    if (diffMin < 60) return `${diffMin}m ago`;
    const diffHr = Math.floor(diffMin / 60);
    if (diffHr < 24) return `${diffHr}h ago`;
    const diffDays = Math.floor(diffHr / 24);
    return `${diffDays}d ago`;
  }

  // --- Dynamic Geolocation & Reverse Geocoding ---
  async function reverseGeocode(lat, lng) {
    try {
      const res = await fetch(`https://nominatim.openstreetmap.org/reverse?format=json&lat=${lat}&lon=${lng}`, {
        headers: { 'Accept-Language': 'en' }
      });
      if (!res.ok) return null;
      const data = await res.json();
      const addr = data.address || {};
      const city = addr.city || addr.town || addr.village || addr.suburb || addr.county || addr.state || 'Local Area';
      const display = data.display_name ? data.display_name.split(',').slice(0, 3).join(', ') : city;
      return { city, address: display };
    } catch (e) {
      return null;
    }
  }

  function applyLocationUpdate(lat, lng, speed, bearing) {
    if (typeof window.__updateDrivingMode === 'function') {
      window.__updateDrivingMode(typeof speed === 'number' ? speed : 0);
    }
    const isFirstFix = !userCoords || !userCoords.lat;
    userCoords = {
      lat: lat,
      lng: lng,
      city: userCoords?.city || 'Local Area',
      address: userCoords?.address || `${lat.toFixed(4)}, ${lng.toFixed(4)}`
    };

    if (navGpsMarker) navGpsMarker.setLatLng([lat, lng]);
    if (homeGpsMarker) homeGpsMarker.setLatLng([lat, lng]);

    const liveSpeedEl = document.getElementById('liveSpeedText');
    if (liveSpeedEl) {
      liveSpeedEl.textContent = Math.max(0, Math.round(speed || 0));
    }

    if (isFirstFix) {
      if (navMap) navMap.setView([lat, lng], 15);
      if (homeMap) homeMap.setView([lat, lng], 14);
    }

    reverseGeocode(lat, lng).then(geo => {
      if (geo) {
        userCoords.city = geo.city;
        userCoords.address = geo.address;
        localStorage.setItem(STORAGE_COORDS, JSON.stringify(userCoords));

        if (homeGpsStatusText) homeGpsStatusText.textContent = `GPS Active • ${geo.city}`;
        if (navEngineLegal) navEngineLegal.textContent = `AutoVerse Live Navigation • ${geo.city}`;
      }
      renderPlacesShelf();
    });

    if (homeGpsStatusText && !userCoords.city) {
      homeGpsStatusText.textContent = `GPS Active • Connected`;
    }
    if (navEngineLegal && !userCoords.city) {
      navEngineLegal.textContent = `AutoVerse Live Navigation • GPS Active`;
    }
  }

  // Tier 1 driving mode: speed-aware calm UI with hysteresis
  let __drivingExitTimer = null;
  window.__updateDrivingMode = function (speedKmh) {
    const body = document.body;
    if (speedKmh > 8) {
      if (__drivingExitTimer) { clearTimeout(__drivingExitTimer); __drivingExitTimer = null; }
      if (!body.classList.contains('driving-mode')) {
        body.classList.add('driving-mode');
        const chip = document.getElementById('drivingChip');
        if (chip) chip.hidden = false;
        showToast("Driving mode: bigger controls, calmer visuals");
      }
    } else if (speedKmh < 5 && body.classList.contains('driving-mode') && !__drivingExitTimer) {
      __drivingExitTimer = setTimeout(() => {
        body.classList.remove('driving-mode');
        const chip = document.getElementById('drivingChip');
        if (chip) chip.hidden = true;
        __drivingExitTimer = null;
      }, 30000);
    }
  };

  window.__onAutoVerseLocationUpdate = function (loc) {
    if (loc) {
      const lat = typeof loc.lat === 'number' ? loc.lat : loc.latitude;
      const lng = typeof loc.lng === 'number' ? loc.lng : loc.longitude;
      const speed = typeof loc.speedKmh === 'number' ? loc.speedKmh : (loc.speed ? loc.speed * 3.6 : 0);
      const bearing = loc.bearing || 0;
      if (typeof lat === 'number' && typeof lng === 'number') {
        applyLocationUpdate(lat, lng, speed, bearing);
      }
    }
  };

  if (navigator.geolocation) {
    navigator.geolocation.getCurrentPosition(
      (pos) => applyLocationUpdate(pos.coords.latitude, pos.coords.longitude),
      (err) => console.log("Geolocation fallback waiting:", err.message),
      { enableHighAccuracy: true, timeout: 10000, maximumAge: 60000 }
    );
    navigator.geolocation.watchPosition(
      (pos) => applyLocationUpdate(pos.coords.latitude, pos.coords.longitude),
      null,
      { enableHighAccuracy: true, timeout: 20000, maximumAge: 15000 }
    );
  }

  // --- Dynamic Storage Management ---
  function getSavedPlaces() {
    const raw = localStorage.getItem(STORAGE_SAVED);
    if (!raw) {
      const defaults = [
        { id: 'home', type: 'home', label: 'Home', address: null, lat: null, lng: null },
        { id: 'work', type: 'work', label: 'Work', address: null, lat: null, lng: null }
      ];
      localStorage.setItem(STORAGE_SAVED, JSON.stringify(defaults));
      return defaults;
    }
    try {
      return JSON.parse(raw);
    } catch (e) {
      return [];
    }
  }

  function saveSavedPlaces(places) {
    localStorage.setItem(STORAGE_SAVED, JSON.stringify(places));
  }

  function getRecentPlaces() {
    const raw = localStorage.getItem(STORAGE_RECENTS);
    if (!raw) return [];
    try {
      return JSON.parse(raw);
    } catch (e) {
      return [];
    }
  }

  function recordRecentDestination(dest) {
    let recents = getRecentPlaces();
    recents = recents.filter(r => r.name !== dest.name && r.address !== dest.address);
    recents.unshift({
      id: 'rec_' + Date.now(),
      name: dest.name,
      address: dest.address,
      lat: dest.lat,
      lng: dest.lng,
      timestamp: Date.now()
    });
    if (recents.length > 15) recents = recents.slice(0, 15);
    localStorage.setItem(STORAGE_RECENTS, JSON.stringify(recents));
    renderPlacesShelf();
  }

  // --- Render Dynamic Places Shelf ---
  function renderPlacesShelf() {
    if (!placesScrollShelf) return;
    placesScrollShelf.innerHTML = '';

    if (activePlacesTab === 'recent') {
      if (btnAddSavedPlace) btnAddSavedPlace.style.display = 'none';
      const recents = getRecentPlaces();

      if (btnClearRecents) {
        btnClearRecents.style.display = recents.length > 0 ? 'flex' : 'none';
      }

      if (recents.length === 0) {
        placesScrollShelf.innerHTML = `
          <div class="empty-places-state">
            <div class="empty-places-icon">
              <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" stroke-width="2">
                <circle cx="12" cy="12" r="10"/>
                <polyline points="12 6 12 12 16 14"/>
              </svg>
            </div>
            <div class="empty-places-title">No Recent Trips</div>
            <div class="empty-places-desc">Destinations you search or navigate to will appear here automatically.</div>
          </div>
        `;
        return;
      }

      recents.forEach(item => {
        let distBadge = '';
        if (userCoords?.lat && item.lat && item.lng) {
          const d = getDistanceKm(userCoords.lat, userCoords.lng, item.lat, item.lng);
          if (d !== null) {
            distBadge = `<span class="place-dist-badge">${formatDistance(d)}</span>`;
          }
        }
        const timeBadge = item.timestamp ? `<span class="place-time-badge">${formatRelativeTime(item.timestamp)}</span>` : '';

        const card = document.createElement('div');
        card.className = 'place-destination-card';
        if (activeDestination?.name === item.name) card.classList.add('active');

        card.innerHTML = `
          <div class="place-glyph-box bg-slate">
            <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="12" cy="12" r="10"/>
              <polyline points="12 6 12 12 16 14"/>
            </svg>
          </div>
          <div class="place-text-cluster">
            <div class="place-title-text">${escapeHtml(item.name)}</div>
            <div class="place-meta-address">${escapeHtml(item.address || '')}</div>
            <div class="place-card-meta-row">
              ${distBadge}
              ${timeBadge}
            </div>
          </div>
          <button class="place-card-action-btn" title="Remove" data-remove-id="${item.id}">
            <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2.2"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
          </button>
        `;

        card.addEventListener('click', (e) => {
          if (e.target.closest('.place-card-action-btn')) {
            e.stopPropagation();
            const filtered = recents.filter(r => r.id !== item.id);
            localStorage.setItem(STORAGE_RECENTS, JSON.stringify(filtered));
            renderPlacesShelf();
            return;
          }
          selectDestination(item);
        });

        placesScrollShelf.appendChild(card);
      });

    } else {
      // Saved Places Tab
      if (btnAddSavedPlace) btnAddSavedPlace.style.display = 'flex';
      if (btnClearRecents) btnClearRecents.style.display = 'none';

      const saved = getSavedPlaces();

      saved.forEach(item => {
        const isHome = item.type === 'home';
        const isWork = item.type === 'work';
        const glyphClass = isHome ? 'bg-primary-tint' : (isWork ? 'bg-secondary-tint' : 'bg-slate');

        let glyphSvg = '';
        if (isHome) {
          glyphSvg = `<svg viewBox="0 0 24 24" width="20" height="20" fill="currentColor"><path d="M10 20v-6h4v6h5v-8h3L12 3 2 12h3v8z"/></svg>`;
        } else if (isWork) {
          glyphSvg = `<svg viewBox="0 0 24 24" width="20" height="20" fill="currentColor"><path d="M20 6h-4V4c0-1.11-.89-2-2-2h-4c-1.11 0-2 .89-2 2v2H4c-1.11 0-1.99.89-1.99 2L2 19c0 1.11.89 2 2 2h16c1.11 0 2-.89 2-2V8c0-1.11-.89-2-2-2zm-6 0h-4V4h4v2z"/></svg>`;
        } else {
          glyphSvg = `<svg viewBox="0 0 24 24" width="20" height="20" fill="currentColor"><polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"/></svg>`;
        }

        let addressText = item.address;
        let badgeHtml = '';

        if (!addressText) {
          addressText = `Tap to set ${item.label} address`;
          badgeHtml = `<span class="place-unset-badge">Not configured</span>`;
        } else if (userCoords?.lat && item.lat && item.lng) {
          const d = getDistanceKm(userCoords.lat, userCoords.lng, item.lat, item.lng);
          if (d !== null) {
            badgeHtml = `<span class="place-dist-badge">${formatDistance(d)}</span>`;
          }
        }

        const card = document.createElement('div');
        card.className = 'place-destination-card';
        if (activeDestination?.name === item.label) card.classList.add('active');

        card.innerHTML = `
          <div class="place-glyph-box ${glyphClass}">
            ${glyphSvg}
          </div>
          <div class="place-text-cluster">
            <div class="place-title-text">${escapeHtml(item.label)}</div>
            <div class="place-meta-address ${!item.address ? 'text-muted' : ''}">${escapeHtml(addressText)}</div>
            <div class="place-card-meta-row">
              ${badgeHtml}
            </div>
          </div>
          <button class="place-card-action-btn" title="Edit Place" data-edit-id="${item.id}">
            <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 20h9"/><path d="M16.5 3.5a2.121 2.121 0 0 1 3 3L7 19l-4 1 1-4L16.5 3.5z"/></svg>
          </button>
        `;

        card.addEventListener('click', (e) => {
          if (e.target.closest('.place-card-action-btn')) {
            e.stopPropagation();
            openEditPlaceModal(item);
            return;
          }
          if (!item.address || !item.lat) {
            openEditPlaceModal(item);
          } else {
            selectDestination({ name: item.label, address: item.address, lat: item.lat, lng: item.lng });
          }
        });

        placesScrollShelf.appendChild(card);
      });
    }
  }

  function escapeHtml(str) {
    if (!str) return '';
    return str.replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;").replace(/"/g, "&quot;");
  }

  // --- Segmented Tab Switchers ---
  if (tabRecent && tabSaved) {
    tabRecent.addEventListener('click', () => {
      tabRecent.classList.add('active');
      tabSaved.classList.remove('active');
      activePlacesTab = 'recent';
      renderPlacesShelf();
    });
    tabSaved.addEventListener('click', () => {
      tabSaved.classList.add('active');
      tabRecent.classList.remove('active');
      activePlacesTab = 'saved';
      renderPlacesShelf();
    });
  }

  if (btnClearRecents) {
    btnClearRecents.addEventListener('click', () => {
      localStorage.setItem(STORAGE_RECENTS, JSON.stringify([]));
      renderPlacesShelf();
      showToast("Recent destinations cleared");
    });
  }

  // --- Place Modal Dialog Handlers ---
  function openEditPlaceModal(place) {
    currentEditingPlaceId = place ? place.id : null;
    if (placeModalOverlay) {
      placeModalTitle.textContent = place ? `Set ${place.label}` : 'Add New Place';
      placeModalName.value = place ? place.label : '';
      placeModalAddress.value = place && place.address ? place.address : '';
      placeModalOverlay.classList.add('active');
      placeModalAddress.focus();
    }
  }

  function closePlaceModal() {
    if (placeModalOverlay) placeModalOverlay.classList.remove('active');
    currentEditingPlaceId = null;
  }

  if (btnAddSavedPlace) {
    btnAddSavedPlace.addEventListener('click', () => openEditPlaceModal(null));
  }
  if (btnPlaceModalClose) btnPlaceModalClose.addEventListener('click', closePlaceModal);
  if (btnPlaceModalCancel) btnPlaceModalCancel.addEventListener('click', closePlaceModal);

  if (btnPlaceModalUseGps) {
    btnPlaceModalUseGps.addEventListener('click', async () => {
      if (userCoords?.lat) {
        showToast("Detecting current GPS street address...");
        const geo = await reverseGeocode(userCoords.lat, userCoords.lng);
        if (geo && geo.address) {
          placeModalAddress.value = geo.address;
          showToast(`Address set: ${geo.city}`);
        } else {
          placeModalAddress.value = `${userCoords.lat.toFixed(5)}, ${userCoords.lng.toFixed(5)}`;
        }
      } else {
        showToast("GPS fix not available yet. Please type address.");
      }
    });
  }

  if (btnPlaceModalSave) {
    btnPlaceModalSave.addEventListener('click', async () => {
      const name = placeModalName.value.trim();
      const addr = placeModalAddress.value.trim();
      if (!name) {
        showToast("Please provide a name for this place");
        return;
      }
      if (!addr) {
        showToast("Please provide an address or landmark");
        return;
      }

      showToast("Resolving location coordinates...");
      let lat = null;
      let lng = null;

      try {
        const res = await fetch(`https://nominatim.openstreetmap.org/search?format=json&q=${encodeURIComponent(addr)}&limit=1`, {
          headers: { 'Accept-Language': 'en' }
        });
        const data = await res.json();
        if (data && data.length > 0) {
          lat = parseFloat(data[0].lat);
          lng = parseFloat(data[0].lon);
        }
      } catch (e) {
        console.warn("Geocoding failed for place:", e);
      }

      if (!lat && userCoords?.lat) {
        lat = userCoords.lat;
        lng = userCoords.lng;
      }

      const saved = getSavedPlaces();
      if (currentEditingPlaceId) {
        const idx = saved.findIndex(s => s.id === currentEditingPlaceId);
        if (idx !== -1) {
          saved[idx].label = name;
          saved[idx].address = addr;
          saved[idx].lat = lat;
          saved[idx].lng = lng;
        }
      } else {
        saved.push({
          id: 'place_' + Date.now(),
          type: 'custom',
          label: name,
          address: addr,
          lat: lat,
          lng: lng
        });
      }

      saveSavedPlaces(saved);
      closePlaceModal();
      renderPlacesShelf();
      showToast(`Saved: ${name}`);
    });
  }

  // --- Destination Selection & Route Drawing ---
  function selectDestination(dest) {
    activeDestination = dest;
    recordRecentDestination(dest);

    if (navMap && dest.lat && dest.lng) {
      if (navDestMarker) navMap.removeLayer(navDestMarker);
      if (destPinIcon) {
        navDestMarker = L.marker([dest.lat, dest.lng], { icon: destPinIcon }).addTo(navMap);
      }

      const startLat = userCoords?.lat || defaultLat;
      const startLng = userCoords?.lng || defaultLng;

      if (navRoutePolyline) navMap.removeLayer(navRoutePolyline);
      navRoutePolyline = L.polyline(
        [[startLat, startLng], [dest.lat, dest.lng]],
        {
          color: '#007AFF',
          weight: 6,
          opacity: 0.85,
          dashArray: '10, 10'
        }
      ).addTo(navMap);

      const bounds = L.latLngBounds([[startLat, startLng], [dest.lat, dest.lng]]);
      navMap.fitBounds(bounds, { padding: [60, 60] });
    }

    const distKm = (userCoords?.lat && dest.lat) ? getDistanceKm(userCoords.lat, userCoords.lng, dest.lat, dest.lng) : null;
    const distStr = formatDistance(distKm) || 'Direct route';
    const etaMin = distKm ? Math.max(2, Math.round(distKm * 2.2)) : 12;

    if (homeNavTag) homeNavTag.textContent = `ROUTE ACTIVE • ${distStr.toUpperCase()}`;
    if (homeNavManeuver) homeNavManeuver.textContent = `To ${dest.name || dest.address}`;
    if (homeNavEtaMin) homeNavEtaMin.textContent = `${etaMin} min`;
    if (homeNavEtaDist) homeNavEtaDist.textContent = distStr;

    showToast(`Route plotted: ${dest.name}`);
  }

  async function handleSelectCoordinates(lat, lng, label) {
    showToast("Resolving tapped location...");
    const geo = await reverseGeocode(lat, lng);
    const dest = {
      name: geo ? geo.city : (label || 'Point on Map'),
      address: geo ? geo.address : `${lat.toFixed(5)}, ${lng.toFixed(5)}`,
      lat: lat,
      lng: lng
    };
    selectDestination(dest);
  }

  // --- Real Geocoding Search Handlers ---
  async function performDestinationSearch(query) {
    if (!query) return;
    if (!navigator.onLine) {
      showToast("Offline — matching saved & recent places only; text search needs a connection");
    } else {
      showToast(`Searching for "${query}"...`);
    }

    try {
      const url = `https://nominatim.openstreetmap.org/search?format=json&q=${encodeURIComponent(query)}&limit=5`;
      const res = await fetch(url, { headers: { 'Accept-Language': 'en' } });
      const results = await res.json();

      if (results && results.length > 0) {
        const top = results[0];
        const dest = {
          name: query,
          address: top.display_name,
          lat: parseFloat(top.lat),
          lng: parseFloat(top.lon)
        };
        selectDestination(dest);
      } else {
        if (window.AutoVerse.isNative()) {
          window.AutoVerseAndroid.openNavigation(query);
        } else {
          showToast(`No online results found for "${query}"`);
        }
      }
    } catch (e) {
      console.warn("Nominatim search error:", e);
      if (window.AutoVerse.isNative()) {
        window.AutoVerseAndroid.openNavigation(query);
      } else {
        showToast(`Searching Google Maps for: ${query}`);
      }
    }
  }

  if (mapsSearchInput) {
    mapsSearchInput.addEventListener('keydown', (e) => {
      if (e.key === 'Enter') {
        const q = mapsSearchInput.value.trim();
        if (q) performDestinationSearch(q);
      }
    });
  }

  if (mapsSiriMic) {
    mapsSiriMic.addEventListener('click', () => {
      if (window.AutoVerse.isNative()) {
        window.AutoVerseAndroid.startVoiceRecognition('navigation');
        setVoiceListening();
      } else {
        showToast("Voice navigation works in the Android app");
      }
    });
  }

  // Tier 2: offline awareness for the maps screen
  const offlineBanner = document.getElementById('offlineBanner');
  function refreshOfflineBanner() {
    if (offlineBanner) offlineBanner.hidden = navigator.onLine;
  }
  window.addEventListener('online', refreshOfflineBanner);
  window.addEventListener('offline', refreshOfflineBanner);
  refreshOfflineBanner();

  // --- "Get Directions" Google Maps Turn-by-Turn Intent Trigger ---
  function dispatchGoogleMapsGuidance() {
    const destParam = activeDestination
      ? (activeDestination.address || `${activeDestination.lat},${activeDestination.lng}`)
      : (mapsSearchInput?.value?.trim() || '');

    if (window.AutoVerse.isNative()) {
      showToast("Handoff to Google Maps Turn-by-Turn...");
      window.AutoVerseAndroid.openNavigation(destParam);
    } else {
      if (destParam) {
        window.open(`https://www.google.com/maps/dir/?api=1&destination=${encodeURIComponent(destParam)}`, '_blank');
      } else {
        window.open('https://www.google.com/maps', '_blank');
      }
      showToast("Launching Google Maps navigation...");
    }
  }

  if (btnGetDirections) {
    btnGetDirections.addEventListener('click', dispatchGoogleMapsGuidance);
  }

  if (homeNavGuidanceCard) {
    homeNavGuidanceCard.addEventListener('click', () => {
      if (activeDestination) {
        dispatchGoogleMapsGuidance();
      }
    });
  }

  // Places Drawer Collapse Toggle
  const btnCollapsePlacesDrawer = document.getElementById('btnCollapsePlacesDrawer');
  const navPlacesDrawer = document.getElementById('navPlacesDrawer');
  if (btnCollapsePlacesDrawer && navPlacesDrawer) {
    btnCollapsePlacesDrawer.addEventListener('click', () => {
      const isCollapsed = navPlacesDrawer.classList.toggle('collapsed');
      btnCollapsePlacesDrawer.textContent = isCollapsed ? '>' : '<';
      showToast(isCollapsed ? "Places Drawer Collapsed" : "Places Drawer Expanded");
    });
  }

  // Places List Visibility Toggle
  const btnTogglePlacesVisibility = document.getElementById('btnTogglePlacesVisibility');
  if (btnTogglePlacesVisibility && placesScrollShelf) {
    btnTogglePlacesVisibility.addEventListener('click', () => {
      const isHidden = placesScrollShelf.style.display === 'none';
      placesScrollShelf.style.display = isHidden ? 'flex' : 'none';
      showToast(isHidden ? "Places List Visible" : "Places List Hidden");
    });
  }

  // Initial render of places shelf
  renderPlacesShelf();
}

/* ==========================================================================
   7. APPLE SYSTEM SETTINGS (THEMES, ACCENT DISCS, DYNAMIC TYPE & SUBPANELS)
   ========================================================================== */
function initAppleSettings() {
  const themeUnits = document.querySelectorAll('.theme-card-unit');
  const liquidSegs = document.querySelectorAll('.liquid-seg-item');
  const accentDiscs = document.querySelectorAll('.accent-disc');
  const diagThemeVal = document.getElementById('diagThemeVal');
  const fontSizeSlider = document.getElementById('fontSizeSlider');
  const reduceMotionToggle = document.getElementById('reduceMotionToggle');
  const miniThemeBadge = document.getElementById('miniThemeBadge');

  // Premium theme entitlement (0.21.0): local flag, honestly early-access
  window.PREMIUM_THEMES = ['theme-sakura', 'theme-neon-tokyo', 'theme-shonen', 'theme-gotham', 'theme-hot-rod', 'theme-fortress', 'theme-web-rush'];
  window.__premiumUnlocked = window.AutoVerse.isNative()
    ? !!window.AutoVerseAndroid.isPremiumUnlocked()
    : true; // browser preview shows everything

  function refreshPremiumState() {
    document.querySelectorAll('.theme-pro-chip').forEach(c => {
      c.style.display = window.__premiumUnlocked ? 'none' : 'block';
    });
    document.querySelectorAll('.theme-card-unit.premium').forEach(c => {
      c.classList.toggle('locked', !window.__premiumUnlocked);
    });
    const btn = document.getElementById('unlockPremiumBtn');
    const label = document.getElementById('unlockPremiumLabel');
    if (btn && label) {
      btn.classList.toggle('unlocked', !!window.__premiumUnlocked);
      label.textContent = window.__premiumUnlocked
        ? 'Premium active — every theme unlocked'
        : 'Unlock Premium themes (early access — free)';
    }
  }
  window.__refreshPremiumState = refreshPremiumState;

  const unlockPremiumBtn = document.getElementById('unlockPremiumBtn');
  if (unlockPremiumBtn) {
    unlockPremiumBtn.addEventListener('click', () => {
      if (window.__premiumUnlocked) {
        showToast("Premium is already active");
        return;
      }
      if (window.AutoVerse.isNative()) {
        window.AutoVerseAndroid.setPremiumUnlocked(true);
      }
      window.__premiumUnlocked = true;
      refreshPremiumState();
      showToast("Premium unlocked — every theme is yours");
    });
  }
  refreshPremiumState();

  // Theme carousel switcher
  themeUnits.forEach(unit => {
    unit.addEventListener('click', () => {
      themeUnits.forEach(u => u.classList.remove('active'));
      unit.classList.add('active');

      const theme = unit.dataset.theme;
      if (PREMIUM_THEMES.includes(theme) && !window.__premiumUnlocked) {
        showToast("Premium theme — unlock free (early access) below the theme list");
        return;
      }
      document.body.className = theme;

      const themeName = unit.querySelector('.theme-unit-name')?.textContent || 'Cobalt';
      if (diagThemeVal) {
        diagThemeVal.textContent = `${themeName} (Retina Pro)`;
      }

      if (miniThemeBadge) {
        miniThemeBadge.textContent = `${themeName.toUpperCase()} • RETINA`;
      }

      showToast(`Cockpit Theme: ${themeName}`);
      playThemeSound();

      if (window.AutoVerse.isNative()) {
        window.AutoVerseAndroid.setTheme(theme);
      }
    });
  });

  // Appearance mode segmented (Light / Dark / Auto)
  liquidSegs.forEach(seg => {
    seg.addEventListener('click', () => {
      liquidSegs.forEach(s => s.classList.remove('active'));
      seg.classList.add('active');
      const mode = seg.dataset.mode;
      showToast(`Appearance Mode: ${mode.toUpperCase()}`);
    });
  });

  // Accent color discs
  accentDiscs.forEach(disc => {
    disc.addEventListener('click', () => {
      accentDiscs.forEach(d => {
        d.classList.remove('active');
        d.textContent = '';
      });
      disc.classList.add('active');
      disc.textContent = '✓';

      const color = disc.dataset.color;
      document.documentElement.style.setProperty('--accent-primary', color);
      document.documentElement.style.setProperty('--accent-glow', `${color}66`);
      showToast(`Accent Tint: ${disc.title || color}`);
    });
  });

  // Dynamic type scale
  if (fontSizeSlider) {
    fontSizeSlider.addEventListener('input', (e) => {
      const val = e.target.value;
      document.documentElement.style.fontSize = `${val}px`;
    });
  }

  // Reduce motion toggle
  if (reduceMotionToggle) {
    reduceMotionToggle.addEventListener('change', (e) => {
      if (e.target.checked) {
        document.body.classList.add('reduce-motion');
        showToast("Reduced Motion Enabled");
      } else {
        document.body.classList.remove('reduce-motion');
        showToast("Fluid Motion Enabled");
      }
    });
  }

  // Category sidebar and subpanel switching
  const cells = document.querySelectorAll('.apple-settings-cell');
  const subpanels = document.querySelectorAll('.settings-subpanel');

  cells.forEach(cell => {
    cell.addEventListener('click', () => {
      cells.forEach(c => c.classList.remove('active'));
      cell.classList.add('active');

      const targetSub = cell.dataset.sub;
      subpanels.forEach(p => {
        if (p.id === `subpanel-${targetSub}`) {
          p.classList.add('active');
        } else {
          p.classList.remove('active');
        }
      });

      const title = cell.querySelector('.cell-title')?.textContent || targetSub;
      showToast(`Settings: ${title}`);
    });
  });

  // Subpanel action buttons
  const btnClearMapCache = document.getElementById('btnClearMapCache');
  if (btnClearMapCache) {
    btnClearMapCache.addEventListener('click', async () => {
      if (window.__avOfflineTiles) {
        const count = await window.__avOfflineTiles.countCachedTiles();
        await window.__avOfflineTiles.clearCachedTiles();
        showToast(`Offline map cache cleared (${count} tiles)`);
      } else {
        showToast("Offline map cache is not available on this device");
      }
    });
  }

  const btnRescanApps = document.getElementById('btnRescanApps');
  if (btnRescanApps) {
    btnRescanApps.addEventListener('click', () => {
      showToast("Discovered 24 Head-Unit Automotive Packages");
    });
  }

  const btnPairDevice = document.getElementById('btnPairDevice');
  if (btnPairDevice) {
    btnPairDevice.addEventListener('click', () => {
      showToast("Broadcasting Bluetooth beacon 'AutoVerse-Car' (PIN: 0000)...");
    });
  }

  const btnCheckUpdates = document.getElementById('btnCheckUpdates');
  if (btnCheckUpdates) {
    btnCheckUpdates.addEventListener('click', () => {
      showToast("Cockpit OS is up to date (v18.2.0 Stable Build)");
    });
  }

  const btnFactoryReset = document.getElementById('btnFactoryReset');
  if (btnFactoryReset) {
    btnFactoryReset.addEventListener('click', () => {
      const resetBtn = document.getElementById('resetPrefsBtn');
      if (resetBtn) resetBtn.click();
    });
  }

  const driverProfileBtn = document.getElementById('driverProfileBtn');
  if (driverProfileBtn) {
    driverProfileBtn.addEventListener('click', () => {
      showToast("Driver Profile: Harsh (Seat & Mirror Memory Restored)");
    });
  }

  // Settings spotlight search
  const settingsSearchInput = document.getElementById('settingsSearchInput');
  if (settingsSearchInput) {
    settingsSearchInput.addEventListener('input', (e) => {
      const term = e.target.value.toLowerCase().trim();
      const allRows = document.querySelectorAll('.apple-config-row');
      allRows.forEach(row => {
        const title = row.querySelector('.config-title')?.textContent.toLowerCase() || '';
        const detail = row.querySelector('.config-detail')?.textContent.toLowerCase() || '';
        if (term === '' || title.includes(term) || detail.includes(term)) {
          row.style.display = 'flex';
        } else {
          row.style.display = 'none';
        }
      });
    });
  }
}

/* ==========================================================================
   8. DYNAMIC ISLAND EXPANSION & HUD CONTROLLER
   ========================================================================== */
function initDynamicIsland() {
  const dynamicIsland = document.getElementById('dynamicIsland');
  const islandPlayBtn = document.getElementById('islandPlayBtn');
  const islandPrevBtn = document.getElementById('islandPrevBtn');
  const islandNextBtn = document.getElementById('islandNextBtn');
  const playPauseBtn = document.getElementById('playPauseBtn');
  const prevBtn = document.getElementById('prevTrackBtn');
  const nextBtn = document.getElementById('nextTrackBtn');

  if (dynamicIsland) {
    dynamicIsland.addEventListener('click', (e) => {
      if (e.target.closest('.island-mini-btn')) return;
      dynamicIsland.classList.toggle('expanded');
    });

    document.addEventListener('click', (e) => {
      if (!dynamicIsland.contains(e.target)) {
        dynamicIsland.classList.remove('expanded');
      }
    });
  }

  if (islandPlayBtn && playPauseBtn) {
    islandPlayBtn.addEventListener('click', (e) => {
      e.stopPropagation();
      playPauseBtn.click();
    });
  }

  if (islandPrevBtn && prevBtn) {
    islandPrevBtn.addEventListener('click', (e) => {
      e.stopPropagation();
      prevBtn.click();
    });
  }

  if (islandNextBtn && nextBtn) {
    islandNextBtn.addEventListener('click', (e) => {
      e.stopPropagation();
      nextBtn.click();
    });
  }
}

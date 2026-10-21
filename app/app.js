// Albrhi TV - home screen for LG webOS. Downloaded apps, essential system apps, HDMI inputs (Sony / beIN logos),
// next Barça/Madrid fixtures, weather, last-opened, screensaver. Arrow keys + OK + Back, and the Magic Remote pointer.
(function () {
  var MARK = window.__marks = {js: Math.round(performance.now())};
  var C = window.CONFIG;
  var $ = function (id) { return document.getElementById(id); };
  var onTV = typeof window.PalmServiceBridge === 'function';

  function store(k, v) {
    try {
      if (arguments.length > 1) localStorage['albrhi:' + k] = JSON.stringify(v);
      else return JSON.parse(localStorage['albrhi:' + k] || 'null');
    } catch (e) { return null; }
  }
  function el(tag, cls, html) { var e = document.createElement(tag); if (cls) e.className = cls; if (html != null) e.innerHTML = html; return e; }
  function esc(s) { return String(s || '').replace(/[&<>"]/g, function (c) { return '&#' + c.charCodeAt(0) + ';'; }); }
  function toast(m) { var t = $('toast'); t.textContent = m; t.style.display = 'block'; clearTimeout(toast._t); toast._t = setTimeout(function () { t.style.display = 'none'; }, 3200); }
  function get(url) { return fetch(url).then(function (r) { if (!r.ok) throw new Error(r.status); return r.json(); }); }
  var DAYS = ['الأحد', 'الإثنين', 'الثلاثاء', 'الأربعاء', 'الخميس', 'الجمعة', 'السبت'];
  var MONTHS = ['يناير', 'فبراير', 'مارس', 'أبريل', 'مايو', 'يونيو', 'يوليو', 'أغسطس', 'سبتمبر', 'أكتوبر', 'نوفمبر', 'ديسمبر'];
  function hm(d) { var h = d.getHours(); return (h % 12 || 12) + ':' + ('0' + d.getMinutes()).slice(-2) + ' ' + (h >= 12 ? 'م' : 'ص'); }
  function dayLine(d) { return DAYS[d.getDay()] + ' ' + d.getDate() + ' ' + MONTHS[d.getMonth()]; }
  function luma(hex) {
    var m = /^#?([0-9a-f]{2})([0-9a-f]{2})([0-9a-f]{2})$/i.exec((hex || '').length === 4 ? hex.replace(/#(.)(.)(.)/, '#$1$1$2$2$3$3') : hex || '');
    return m ? 0.2126 * parseInt(m[1], 16) + 0.7152 * parseInt(m[2], 16) + 0.0722 * parseInt(m[3], 16) : 0;
  }

  // ---------- small built-in drawings ----------
  var SVG = {
    gear: '<svg viewBox="0 0 24 24"><path fill="currentColor" d="M19.4 13a7.5 7.5 0 0 0 0-2l2.1-1.6-2-3.5-2.5 1a7.4 7.4 0 0 0-1.7-1L15 3h-4l-.4 2.7a7.4 7.4 0 0 0-1.7 1l-2.5-1-2 3.5L6.6 11a7.5 7.5 0 0 0 0 2l-2.1 1.6 2 3.5 2.5-1c.5.4 1.1.7 1.7 1L11 21h4l.4-2.7c.6-.3 1.2-.6 1.7-1l2.5 1 2-3.5L19.4 13zM13 15.5a3.5 3.5 0 1 1 0-7 3.5 3.5 0 0 1 0 7z"/></svg>',
    plug: '<svg viewBox="0 0 24 24"><path fill="currentColor" d="M16 7V3h-2v4h-4V3H8v4H6v4a5 5 0 0 0 4 4.9V18H8v2h8v-2h-2v-5.1A5 5 0 0 0 18 11V7h-2z"/></svg>',
    pencil: '<svg viewBox="0 0 24 24"><path fill="currentColor" d="M3 17.25V21h3.75L17.8 9.94l-3.75-3.75L3 17.25zM20.7 7.04a1 1 0 0 0 0-1.41l-2.34-2.34a1 1 0 0 0-1.41 0l-1.83 1.83 3.75 3.75 1.83-1.83z"/></svg>',
    check: '<svg viewBox="0 0 24 24"><path fill="currentColor" d="M9 16.2 4.8 12l-1.4 1.4L9 19 21 7l-1.4-1.4z"/></svg>'
  };
  var CLOUD = 'M7 19a4.5 4.5 0 0 1-.6-8.96A6 6 0 0 1 17.7 9.4 4.8 4.8 0 0 1 17.5 19z';
  function wxSvg(code, day) {
    var g = function (inner) { return '<g fill="currentColor" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round">' + inner + '</g>'; };
    if (code === 0) return day ? g('<circle cx="12" cy="12" r="4.5"/><path fill="none" d="M12 2v3M12 19v3M2 12h3M19 12h3M4.9 4.9l2.1 2.1M17 17l2.1 2.1M4.9 19.1 7 17M17 7l2.1-2.1"/>')
      : g('<path stroke="none" d="M12.3 3a9 9 0 1 0 8.7 11.4A7 7 0 0 1 12.3 3z"/>');
    if (code <= 2) return g('<circle cx="8" cy="8" r="3.2"/><path transform="translate(2 3)" stroke="none" d="' + CLOUD + '"/>');
    if (code === 3) return g('<path stroke="none" d="' + CLOUD + '"/>');
    if (code === 45 || code === 48) return g('<path fill="none" d="M4 9h16M6 13h12M4 17h16"/>');
    if (code >= 95) return g('<path stroke="none" transform="translate(0 -3)" d="' + CLOUD + '"/><path stroke="none" d="M12.5 13l-3 5h2.5l-1 4 4-6h-2.7z" fill="#fdd663"/>');
    if ((code >= 71 && code <= 77) || code === 85 || code === 86) return g('<path stroke="none" transform="translate(0 -3)" d="' + CLOUD + '"/><circle cx="9" cy="20" r="1"/><circle cx="14" cy="21" r="1"/><circle cx="18" cy="19" r="1"/>');
    if (code >= 51) return g('<path stroke="none" transform="translate(0 -3)" d="' + CLOUD + '"/><path fill="none" d="M9 18l-1 3M13 18l-1 3M17 18l-1 3"/>');
    return g('<path stroke="none" d="' + CLOUD + '"/>');
  }
  function wxDesc(c) {
    if (c === 0) return 'صافي'; if (c === 1) return 'صافي غالبًا'; if (c === 2) return 'غائم جزئيًا'; if (c === 3) return 'غائم';
    if (c === 45 || c === 48) return 'ضباب'; if (c >= 51 && c <= 57) return 'رذاذ'; if (c >= 61 && c <= 67) return 'مطر';
    if ((c >= 71 && c <= 77) || c === 85 || c === 86) return 'ثلج'; if (c >= 80 && c <= 82) return 'زخات مطر'; if (c >= 95) return 'عاصفة رعدية';
    return '';
  }

  // ---------- webOS luna bridge ----------
  function luna(uri, params, ms) {
    return new Promise(function (ok, fail) {
      if (!onTV) return fail({errorText: 'not on webOS'});
      var b = new window.PalmServiceBridge(), done = false;
      b.onservicecallback = function (m) { done = true; var d = JSON.parse(m); d.returnValue === false ? fail(d) : ok(d); };
      b.call('luna://' + uri, JSON.stringify(params || {}));
      setTimeout(function () { if (!done) fail({errorText: 'timeout'}); }, ms || 2500);
    });
  }
  // Optional root helper service (picture mode, screen off). Not installed -> those tiles simply stay hidden.
  function svc(method, body) {
    return svcRaw(method, body).catch(function (e) { dbg('svc ' + method + ' FAIL ' + (e && (e.message || e.name || e))); throw e; });
  }
  function svcRaw(method, body) {
    var ctl = typeof AbortController === 'function' ? new AbortController() : null;
    var timer = setTimeout(function () { if (ctl) ctl.abort(); }, 2500);
    return fetch('http://127.0.0.1:45620/' + method, {method: 'POST', body: JSON.stringify(body || {}), signal: ctl && ctl.signal})
      .then(function (r) { clearTimeout(timer); return r.json(); });
  }

  // ---------- sound: soft tick on navigation ----------
  var actx, clicksOn = store('clicks') !== false;
  function tick() {
    if (!clicksOn) return;
    try {
      if (!actx) return;
      if (actx.state === 'suspended') actx.resume();
      var o = actx.createOscillator(), g = actx.createGain(), t = actx.currentTime;
      o.type = 'sine'; o.frequency.setValueAtTime(1100, t); o.frequency.exponentialRampToValueAtTime(640, t + 0.05);
      g.gain.setValueAtTime(0.0001, t); g.gain.exponentialRampToValueAtTime(0.06, t + 0.008); g.gain.exponentialRampToValueAtTime(0.0001, t + 0.07);
      o.connect(g); g.connect(actx.destination); o.start(t); o.stop(t + 0.08);
    } catch (e) { /* no audio: fine */ }
  }

  // The first move after launch pays for creating the compositor layers (~120ms on the TV). Pay it silently at boot:
  // step the selection through a tile, the match card and the bottom row, then back, unless the remote was touched.
  var touched = false;
  function warm() {
    if (touched || view !== 'home') return;
    var home = cur, seq = [all('#appsStrip .f')[1], matches.length ? $('hero') : null, all('#bottom .f')[0], home].filter(Boolean), i = 0;
    (function step() {
      if (touched || view !== 'home') return;
      setFocusEl(seq[i++]);
      if (i < seq.length) setTimeout(step, 140);
    })();
  }
  function initAudio() { try { actx = new (window.AudioContext || window.webkitAudioContext)(); } catch (e) { /* no audio */ } }

  // ---------- items: probe what exists, per-kind order and hidden list ----------
  var lists = {app: [], sys: [], input: []}, moving = null; // moving: {kind, id} while a tile is being dragged with the arrows
  var PROTECTED = ['org.webosbrew.hbchannel', 'com.albrhi.tvhome', 'com.webos.app.home'];
  // Which apps are installed: ask the root service (one call). The page's own luna calls never answer when running as the
  // home app, so they're only a slow fallback.
  function knownApps() {
    if (!onTV) return Promise.resolve(null);
    return svc('getApps').then(function (r) { if (!r || !r.returnValue) throw 0; return r.apps; }).catch(function () { return null; });
  }
  // Apps you install later (not in config.js, not system apps) show up by themselves after the ones from config.js.
  var CONFIG_IDS = C.APPS.concat(C.SYSTEM).map(function (a) { return a.id; });
  function discovered(apps) {
    return (apps || []).filter(function (a) { return !a.system && CONFIG_IDS.indexOf(a.id) < 0 && PROTECTED.indexOf(a.id) < 0; })
      .map(function (a) { return {id: a.id, title: a.title, color: /^#[0-9a-f]{3,8}$/i.test(a.color || '') ? a.color : '#2a2d36', auto: true}; });
  }
  function buildApps(apps) { return probe(C.APPS, apps ? apps.map(function (x) { return x.id; }) : null).then(function (l) { return l.concat(discovered(apps)); }); }
  // Look again at what's installed; redraw only if it changed (after installing or deleting an app).
  var appsSig = '';
  function rescan() {
    if (!onTV || view !== 'home' || moving) return;
    knownApps().then(function (apps) {
      if (!apps) return;
      var sig = apps.map(function (x) { return x.id; }).sort().join(',');
      if (sig === appsSig) return;
      appsSig = sig;
      return Promise.all([buildApps(apps), probe(C.SYSTEM, apps.map(function (x) { return x.id; }))]).then(function (r) {
        lists.app = r[0]; lists.sys = r[1]; render(); dbg('apps changed: ' + lists.app.length + ' apps');
      });
    });
  }
  function probe(list, ids) {
    if (ids) return Promise.resolve(list.filter(function (a) { return a.always || ids.indexOf(a.id) >= 0; }));
    if (!onTV) return Promise.resolve(list.slice());
    return Promise.all(list.map(function (a) {
      if (a.always) return a;
      return luna('com.webos.applicationManager/getAppLoadStatus', {appId: a.id}, 1200).then(function (r) { return r.exist ? a : null; }, function () { return a; });
    })).then(function (r) { return r.filter(Boolean); });
  }
  function hiddenIds(kind) {
    var h = store(kind + 'Hidden');
    if (h) return h;
    return lists[kind].filter(function (a) { return a.defaultHidden; }).map(function (a) { return a.id; });
  }
  function shown(kind) {
    var list = lists[kind], hidden = hiddenIds(kind), order = store(kind + 'Order') || [];
    var rank = function (a) { var i = order.indexOf(a.id); return i < 0 ? 1e4 + list.indexOf(a) : i; };
    return list.filter(function (a) { return hidden.indexOf(a.id) < 0; }).sort(function (a, b) { return rank(a) - rank(b); });
  }

  // ---------- last opened ----------
  function remember(a) { store('last', {id: a.id, title: a.title, t: Date.now()}); renderLast(); }
  function renderLast() {
    var l = store('last'), p = $('last');
    if (!l) { p.hidden = true; return; }
    var m = Math.round((Date.now() - l.t) / 60000), w = m < 1 ? 'الآن' : m < 60 ? 'قبل ' + m + ' د' : m < 1440 ? 'قبل ' + Math.round(m / 60) + ' س' : 'قبل ' + Math.round(m / 1440) + ' يوم';
    $('lasttext').textContent = l.title + ' · ' + w; p.hidden = false;
  }

  // ---------- launching, with the colour flood animation ----------
  var resetTimer;
  function shade(a) {
    var c = a.glow || a.color || '#000';
    if (luma(c) > 190) return '#2a2a2a';
    return c;
  }
  var VERSION = 'v28';
  function dbg(m) { if (onTV) svc('log', {m: m}).catch(function () {}); }
  window.addEventListener('error', function (e) { dbg('JS error: ' + e.message + ' @' + (e.filename || '').split('/').pop() + ':' + e.lineno); });
  window.addEventListener('unhandledrejection', function (e) { dbg('promise rejection: ' + (e.reason && (e.reason.message || JSON.stringify(e.reason)))); });
  // Launch through the root helper first (always works, also for popups like Settings); fall back to the page's own call.
  function openApp(id) {
    if (!onTV) return toast('Would open ' + id);
    var viaPage = function (why) {
      dbg('page launch ' + id + ' (' + why + ')');
      luna('com.webos.applicationManager/launch', {id: id, params: {}})
        .then(function () { dbg('page ok ' + id); }, function (e) { dbg('page err ' + id + ' ' + JSON.stringify(e)); toast('تعذر الفتح: ' + (e.errorText || e)); resetLaunch(); });
    };
    svc('launch', {id: id}).then(function (r) {
      if (r && r.returnValue === false) { dbg('svc refused ' + id + ' ' + JSON.stringify(r)); viaPage('svc refused'); } else dbg('svc ok ' + id);
    }, function () { viaPage('svc down'); });
  }
  function launchItem(a, tile) {
    dbg('tap ' + a.id);
    remember(a);
    if (a.popup) return openApp(a.id); // Settings opens as a panel over the home: no colour flood
    var L = $('launch'), r = tile.getBoundingClientRect();
    L.style.background = shade(a);
    L.style.setProperty('--lx', Math.round(r.left + r.width / 2) + 'px');
    L.style.setProperty('--ly', Math.round(r.top + r.height / 2) + 'px');
    L.className = ''; void L.offsetWidth; L.className = 'go';
    tile.classList.add('launching');
    clearTimeout(resetTimer);
    openApp(a.id);
    resetTimer = setTimeout(resetLaunch, 3000);
  }
  function resetLaunch() {
    var L = $('launch'); if (L.className === 'go') L.className = 'out';
    [].forEach.call(document.querySelectorAll('.launching'), function (t) { t.classList.remove('launching'); });
    setTimeout(function () { if ($('launch').className === 'out') $('launch').className = ''; }, 600);
  }
  document.addEventListener('visibilitychange', function () { if (!document.hidden) { resetLaunch(); noteActivity(); setTimeout(rescan, 800); } });
  setInterval(function () { if (!document.hidden) rescan(); }, 30000);
  // appinfo has handlesRelaunch, so when the system asks for the home again (Home key, service) the page itself must come to
  // the front: without this the launch is accepted and nothing appears.
  function surface() { try { if (window.PalmSystem && window.PalmSystem.activate) window.PalmSystem.activate(); } catch (e) { /* not on a TV */ } }
  document.addEventListener('webOSRelaunch', function (e) { dbg('relaunch event'); surface(); resetLaunch(); noteActivity(); setTimeout(rescan, 800); });
  window.addEventListener('webOSRelaunch', function () { surface(); });

  // ---------- fixtures: the next match of each team in config.js (TEAMS) ----------
  // ESPN is the main source (no key, every competition, live score); TheSportsDB is the backup if ESPN can't be reached.
  // Refreshed hourly, every 2 minutes while a match is on. The last good answer is kept, so a failed refresh never empties the card.
  var KEY = '123', REFRESH_MS = 60 * 60 * 1000, LIVE_REFRESH_MS = 2 * 60 * 1000, RETRY_MS = 10 * 60 * 1000, LIVE_MS = 2.3 * 3600 * 1000;
  var matches = (store('matches') || []).filter(stillRelevant), mi = 0, refreshTimer, refreshing = false;
  if (matches.some(function (m) { return !/^(espn|tsdb)/.test(m.id); })) { matches = []; store('matchFetchedAt', 0); } // saved by an older version: fetch fresh
  function stillRelevant(m) { return m.state !== 'post' && m.t + LIVE_MS > Date.now(); }
  function arTeam(n) { return C.TEAM_AR[n] || n; }
  // null = couldn't ask (offline / blocked); [] = asked, nothing scheduled
  function fetchEspn(team) {
    return get('https://site.api.espn.com/apis/site/v2/sports/soccer/all/teams/' + team.espn + '/schedule?fixture=true').then(function (d) { return d.events || []; }).catch(function () { return null; });
  }
  function fetchTeam(id) { return get('https://www.thesportsdb.com/api/v1/json/' + KEY + '/eventsnext.php?id=' + id).then(function (d) { return d.events || []; }).catch(function () { return null; }); }
  function espnMatch(e) {
    var c = e.competitions && e.competitions[0]; if (!c || !c.competitors) return null;
    var h = c.competitors.filter(function (x) { return x.homeAway === 'home'; })[0], a = c.competitors.filter(function (x) { return x.homeAway === 'away'; })[0];
    var t = new Date(e.date).getTime(); if (!h || !a || isNaN(t)) return null;
    var logo = function (x) { var tm = x.team || {}; return tm.logo || (tm.logos && tm.logos[0] && tm.logos[0].href) || ''; };
    var score = function (x) { var v = x.score; if (v == null) return null; return typeof v === 'object' ? (v.displayValue != null ? v.displayValue : v.value) : String(v); };
    var lg = typeof e.league === 'object' && e.league ? (e.league.name || e.league.shortName) : e.league;
    var st = c.status && c.status.type;
    return {id: 'espn' + e.id, a: arTeam(h.team.displayName), b: arTeam(a.team.displayName), ba: logo(h), bb: logo(a),
      league: C.LEAGUE_AR[lg] || lg || '', venue: c.venue && c.venue.fullName, t: t, img: null, state: st && st.state || 'pre',
      sa: score(h), sb: score(a), clk: c.status && c.status.displayClock, sname: st && st.name, clkAt: Date.now(), teams: []};
  }
  function toMatch(e) { // TheSportsDB (backup)
    var t = new Date(e.dateEvent + 'T' + (e.strTime || '00:00:00') + 'Z').getTime();
    if (isNaN(t)) return null;
    return {id: 'tsdb' + e.idEvent, a: arTeam(e.strHomeTeam), b: arTeam(e.strAwayTeam), ba: e.strHomeTeamBadge, bb: e.strAwayTeamBadge,
      league: C.LEAGUE_AR[e.strLeague] || e.strLeague, venue: e.strVenue, t: t, img: e.strThumb || e.strPoster || e.strSquare, state: 'pre', teams: []};
  }
  function earliest(list, convert) { // the team's next match: not finished, soonest
    var best = null;
    list.forEach(function (e) { var m = convert(e); if (m && stillRelevant(m) && (!best || m.t < best.t)) best = m; });
    return best;
  }
  function nextOf(team) { // -> {ok, m}
    return fetchEspn(team).then(function (events) {
      if (events) return {ok: true, m: earliest(events, espnMatch)};
      if (!team.id) return {ok: false};
      return fetchTeam(team.id).then(function (ev) { return ev ? {ok: true, m: earliest(ev, toMatch)} : {ok: false}; });
    });
  }
  function scheduleRefresh(ms) { clearTimeout(refreshTimer); refreshTimer = setTimeout(refreshMatches, ms); }
  var refreshPromise = null;
  function refreshMatches() {
    if (refreshing) return refreshPromise;
    refreshing = true;
    refreshPromise = Promise.all(C.TEAMS.map(function (team) { return nextOf(team).then(function (r) { r.team = team; return r; }); })).then(function (results) {
      var byKey = {}, failed = false;
      var add = function (m, teamId) {
        var key = m.a + '|' + m.b + '|' + m.t, x = byKey[key] || (byKey[key] = m);
        if (x.teams.indexOf(teamId) < 0) x.teams.push(teamId);
      };
      results.forEach(function (r) {
        var tid = r.team.espn || r.team.id;
        if (!r.ok) { // couldn't ask for this team: keep what we had instead of dropping it
          failed = true;
          matches.forEach(function (m) { if (m.teams && m.teams.indexOf(tid) >= 0 && stillRelevant(m)) add(m, tid); });
        } else if (r.m) add(r.m, tid);
      });
      var out = Object.keys(byKey).map(function (k) { return byKey[k]; }).sort(function (x, y) { return x.t - y.t; });
      var live = out.some(function (m) { return m.state === 'in'; });
      refreshing = false;
      scheduleRefresh(failed ? RETRY_MS : live ? LIVE_REFRESH_MS : REFRESH_MS);
      if (!failed) store('matchFetchedAt', Date.now());
      if (!out.length && failed) return {ok: false, n: matches.length};
      var curKey = matches[mi] && matches[mi].a + '|' + matches[mi].b + '|' + matches[mi].t;
      matches = out; store('matches', out);
      mi = Math.max(0, out.map(function (m) { return m.a + '|' + m.b + '|' + m.t; }).indexOf(curKey));
      renderHero(); buildNav();
      dbg('matches ' + out.length + (live ? ' (live)' : '') + (failed ? ' (some failed, retry in 10 min)' : ''));
      return {ok: !failed, n: out.length};
    }).catch(function () { refreshing = false; scheduleRefresh(RETRY_MS); return {ok: false, n: matches.length}; });
    return refreshPromise;
  }
  function when(m) { var d = new Date(m.t); return dayLine(d) + '  ·  ' + hm(d); }
  function dayWord(d, nominative) { return d === 1 ? 'يوم' : d === 2 ? (nominative ? 'يومان' : 'يومين') : d + (d <= 10 ? ' أيام' : ' يومًا'); }
  function pad(n) { return (n < 10 ? '0' : '') + n; }
  // Minute of a running match: the clock ESPN gave at the last refresh plus the minutes since (refreshes are 2 minutes apart while live).
  function liveMinute(m) {
    if (m.sname === 'STATUS_HALFTIME') return 'استراحة';
    var base = parseInt(m.clk, 10);
    if (!isNaN(base) && m.clkAt) return (base + Math.floor((Date.now() - m.clkAt) / 60000)) + "'";
    var e = Math.floor((Date.now() - m.t) / 60000); if (e > 60) e -= 15; // rough: kick-off time minus the break
    return Math.max(1, Math.min(e, 125)) + "'";
  }
  // Time left to kick-off with minutes and seconds, or the running minute and score. text = the short form for lists.
  function countdown(m) {
    var diff = m.t - Date.now();
    if (m.state === 'in' || diff <= 0) {
      var mn = liveMinute(m), sc = m.sa != null && m.sb != null ? m.sa + ' - ' + m.sb : '';
      return {live: true, score: sc, minute: mn, text: 'جارية الآن · ' + mn + (sc ? ' · ' + sc : '')};
    }
    var s = Math.floor(diff / 1000), d = Math.floor(s / 86400), h = Math.floor(s % 86400 / 3600), mi = Math.floor(s % 3600 / 60), se = s % 60;
    var clock = pad(h) + ':' + pad(mi) + ':' + pad(se);
    if (d >= 1) return {d: d, dayWord: dayWord(d, true), clock: clock, text: 'بعد ' + dayWord(d, false) + ' و ' + h + ' س ' + mi + ' د'};
    return {clock: clock, text: 'بعد ' + h + ' س ' + mi + ' د'};
  }
  function heroCountHtml(cd) {
    if (cd.live) return cd.score ? '<div class="big" id="cdBig">' + esc(cd.score) + '</div><div class="unit" id="cdUnit">جارية الآن · ' + esc(cd.minute) + '</div>'
      : '<div class="big" id="cdBig">' + esc(cd.minute) + '</div><div class="unit" id="cdUnit">جارية الآن</div>';
    if (cd.d) return '<div class="big" id="cdBig">' + esc(cd.dayWord) + '</div><div class="unit" id="cdUnit">و ' + cd.clock + '</div>';
    return '<div class="big" id="cdBig">' + cd.clock + '</div><div class="unit" id="cdUnit">ساعة : دقيقة : ثانية للانطلاق</div>';
  }
  // Ticks the countdown every second (only the two small text nodes; skipped while another screen or app is on top)
  var heroMode = '';
  function tickHero() {
    if (view !== 'home' || document.hidden || $('hero').hidden || !matches[mi]) return;
    var m = matches[mi], cd = countdown(m), mode = cd.live ? 'live' + (cd.score ? 's' : '') : cd.d ? 'd' + cd.d : 'h';
    var big = $('cdBig'), unit = $('cdUnit');
    if (!big || mode !== heroMode) { heroMode = mode; $('heroCount').innerHTML = heroCountHtml(cd); return; }
    if (cd.live || !cd.d) big.firstChild.nodeValue = cd.live ? (cd.score || cd.minute) : cd.clock;
    unit.textContent = cd.live ? (cd.score ? 'جارية الآن · ' + cd.minute : 'جارية الآن') : cd.d ? 'و ' + cd.clock : 'ساعة : دقيقة : ثانية للانطلاق';
  }
  setInterval(tickHero, 1000);
  function fillHero() {
    var m = matches[mi]; if (!m) return;
    var cd = countdown(m);
    var bg = $('heroBg').style;
    if (m.img) { bg.backgroundImage = 'url(' + m.img + ')'; bg.backgroundSize = 'cover'; bg.backgroundPosition = 'center'; bg.backgroundRepeat = ''; }
    else { // no match photo: both team crests, large and faint
      bg.backgroundImage = 'url(' + (m.bb || '') + '), url(' + (m.ba || '') + '), radial-gradient(900px 400px at 70% 40%, #2b2114, #120e09)';
      bg.backgroundSize = '212px, 212px, cover'; bg.backgroundPosition = '70% 50%, 47% 50%, center'; bg.backgroundRepeat = 'no-repeat';
    }
    $('hNameA').textContent = m.a; $('hNameB').textContent = m.b;
    [['hBadgeA', m.ba], ['hBadgeB', m.bb]].forEach(function (p) { var i = $(p[0]); i.style.visibility = p[1] ? 'visible' : 'hidden'; if (p[1]) i.src = p[1]; });
    $('heroTagText').textContent = cd.live ? 'جارية الآن' : mi === 0 ? 'المباراة القادمة' : 'مباراة قادمة';
    document.querySelector('#heroTag .live').hidden = !cd.live;
    $('heroWhen').textContent = [when(m), m.league].filter(Boolean).join('  ·  ');
    $('heroCount').innerHTML = heroCountHtml(cd);
    var dots = ''; for (var i = 0; i < matches.length; i++) dots += '<span class="dot' + (i === mi ? ' on' : '') + '"></span>';
    $('heroDots').innerHTML = matches.length > 1 ? dots : '';
  }
  function renderHero() {
    var h = $('hero');
    matches = matches.filter(stillRelevant); if (mi >= matches.length) mi = 0;
    if (!matches.length) { h.hidden = true; return; }
    h.hidden = false; fillHero();
  }
  function cycleHero(dir) {
    if (matches.length < 2) return;
    mi = (mi + dir + matches.length) % matches.length;
    var parts = [$('heroBg'), $('heroBody'), $('heroCount')];
    parts.forEach(function (p) { p.classList.add('swap'); });
    setTimeout(function () { fillHero(); parts.forEach(function (p) { p.classList.remove('swap'); }); }, 200);
    tick();
  }
  function updatedText() {
    var t = store('matchFetchedAt'); if (!t) return 'ما انحدّث بعد';
    var m = Math.round((Date.now() - t) / 60000);
    return 'آخر تحديث: ' + (m < 1 ? 'الآن' : m < 60 ? 'قبل ' + m + ' د' : 'قبل ' + Math.round(m / 60) + ' س');
  }
  // Manual refresh (backup for the hourly one): from the matches sheet or the quick panel.
  function manualRefresh() {
    toast('جاري تحديث المباريات…');
    return refreshMatches().then(function (r) {
      toast(r.ok ? 'تم تحديث المباريات (' + r.n + ')' : 'تعذر الوصول للمصدر، المعروض آخر جدول معروف');
      setText('qmatches', updatedText()); if (view === 'matches') fillMatches();
    });
  }
  function fillMatches() {
    var box = $('matchesList'); box.innerHTML = '';
    if (!matches.length) box.appendChild(el('div', 'mrow', '<div class="mw" style="margin:0 auto">ما فيه مباريات قادمة حاليًا</div>'));
    matches.forEach(function (m) {
      var cd = countdown(m);
      box.appendChild(el('div', 'mrow', (m.ba ? '<img src="' + esc(m.ba) + '" alt="">' : '') + '<div class="mn">' + esc(m.a) + '</div><div class="mv">ضد</div>' +
        '<div class="mn r">' + esc(m.b) + '</div>' + (m.bb ? '<img src="' + esc(m.bb) + '" alt="">' : '') +
        '<div class="mw">' + esc(when(m)) + '<small>' + esc([m.league, cd.text].filter(Boolean).join('  ·  ')) + '</small></div>'));
    });
    $('matchesWhen').textContent = updatedText();
  }
  function openMatches() {
    fillMatches(); $('matches').hidden = false; view = 'matches'; pos.r = 0; pos.c = 0; buildNav();
  }
  function closeMatches() { $('matches').hidden = true; view = 'home'; buildNav(); }
  var teamsSig = C.TEAMS.map(function (t) { return t.espn || t.id; }).join(',');
  if (store('teamsSig') !== teamsSig) { store('teamsSig', teamsSig); store('matchFetchedAt', 0); } // teams in config.js changed: fetch now
  var sinceFetch = Date.now() - (store('matchFetchedAt') || 0);
  if (!matches.length || sinceFetch > REFRESH_MS) refreshMatches(); else scheduleRefresh(Math.max(60000, REFRESH_MS - sinceFetch));
  setInterval(function () { if (!document.hidden) { renderHero(); renderLast(); } }, 30000);

  // ---------- weather (Open-Meteo, city found from the TV's IP) ----------
  var wx = store('wx');
  function renderWeather() {
    if (!wx) { $('wx').hidden = true; return; }
    $('wxicon').innerHTML = wxSvg(wx.code, wx.day);
    $('wxtext').textContent = [wx.temp + '°', wxDesc(wx.code), C.CITY_AR[wx.city] || wx.city].filter(Boolean).join(' · ');
    $('wx').hidden = false;
  }
  function loadWeather() {
    if (wx && Date.now() - wx.t < 25 * 60000) return renderWeather();
    var geo = store('geo');
    (geo && Date.now() - geo.t < 7 * 86400000 ? Promise.resolve(geo) : get('https://ipwho.is/').then(function (g) {
      geo = {lat: g.latitude, lon: g.longitude, city: g.city || '', t: Date.now()}; store('geo', geo); return geo;
    })).then(function (g) {
      return get('https://api.open-meteo.com/v1/forecast?latitude=' + g.lat + '&longitude=' + g.lon + '&current=temperature_2m,weather_code,is_day&timezone=auto')
        .then(function (w) {
          wx = {temp: Math.round(w.current.temperature_2m), code: w.current.weather_code, day: w.current.is_day === 1, city: g.city, t: Date.now()};
          store('wx', wx); renderWeather();
        });
    }).catch(function () { renderWeather(); });
  }
  renderWeather(); loadWeather(); setInterval(loadWeather, 30 * 60000);

  // ---------- render ----------
  // Icon of an app that isn't in config.js: the system's own icon, fetched through the service and remembered.
  function autoIcon(a, box) {
    var set = function (uri) { box.innerHTML = '<img src="' + uri + '" alt="">'; };
    var cached = store('ico:' + a.id);
    if (cached) return set(cached);
    svc('getIcon', {id: a.id}).then(function (r) { if (r && r.icon) { store('ico:' + a.id, r.icon); set(r.icon); } }).catch(function () {});
  }
  function tile(a, kind, i) {
    var t;
    if (kind === 'input') {
      t = el('div', 'input f');
      t.style.cssText = '--c:' + a.color + ';--g:' + (a.glow || a.color) + ';--i:' + i;
      t.innerHTML = '<div class="halo"></div><div class="card">' + (a.logo ? '<img src="icons/logos/' + a.logo + '" alt="">' : SVG.plug) +
        (a.sub ? '<small>' + esc(a.sub) + '</small>' : '') + '</div><div class="cap">' + esc(a.title) + '</div>';
    } else {
      t = el('div', kind === 'sys' ? 'app sys f' : 'app f');
      t.style.cssText = '--c:' + a.color + ';--i:' + i;
      t.innerHTML = '<div class="halo"></div><div class="ico">' + (a.icon ? '<img src="icons/apps/' + a.icon + '" alt="">' : a.svg ? SVG[a.svg] : '<span class="txt">' + esc(a.title) + '</span>') +
        '</div><div class="cap">' + esc(a.title) + '</div>';
      if (a.auto) autoIcon(a, t.querySelector('.ico'));
      var img = t.querySelector('img');
      if (img) img.onerror = function () { // one retry, then fall back to the name
        if (!img._retried) { img._retried = 1; setTimeout(function () { img.src = img.src.split('?')[0] + '?r=1'; }, 400); return; }
        img.parentNode.innerHTML = '<span class="txt">' + esc(a.title) + '</span>';
      };
    }
    if (moving && moving.kind === kind && moving.id === a.id) t.classList.add('moving');
    t.onok = function () { launchItem(a, t); };
    t.item = a; t.kind = kind;
    return t;
  }
  function fill(strip, kind, base) {
    var s = $(strip); s.innerHTML = ''; s._shift = 0; s.style.transform = '';
    var list = shown(kind);
    list.forEach(function (a, i) { s.appendChild(tile(a, kind, base + i)); });
    return list.length;
  }
  function render() {
    renderHero(); renderLast();
    fill('appsStrip', 'app', 0);
    var m = fill('inputsStrip', 'input', 3);
    fill('sysStrip', 'sys', 3 + m);
    $('inputsSec').hidden = !m;
    buildNav();
  }
  // ---------- long-press menu: move / hide / delete ----------
  var menu = $('appmenu');
  function deletable(a, kind) { return kind === 'app' && PROTECTED.indexOf(a.id) < 0; }
  function setMenuMode(mode) {
    var a = menu._item, confirm = mode === 'confirm';
    menu._mode = mode;
    $('menuTitle').textContent = confirm ? 'حذف ' + a.title + ' من التلفزيون؟' : a.title;
    [].forEach.call(menu.querySelectorAll('.chip'), function (c) {
      var m = c.dataset.menu;
      c.hidden = confirm ? !(m === 'yes' || m === 'no') : (m === 'yes' || m === 'no' || (m === 'del' && !deletable(a, menu.dataset.kind)));
    });
    pos.r = 0; pos.c = confirm ? 1 : 0; buildNav();
  }
  function openMenu(a, kind) {
    menu.dataset.kind = kind; menu.dataset.id = a.id; menu._item = a;
    menu.hidden = false; view = 'menu'; setMenuMode('main');
  }
  function closeMenu() {
    menu.hidden = true; view = 'home';
    buildNav(); var t = findTile(menu.dataset.kind, menu.dataset.id); if (t) setFocusEl(t);
  }
  function longPress(t) { if (view !== 'home' || moving || !t || !t.item) return; tick(); openMenu(t.item, t.kind); }
  function findTile(kind, id) { return all('#home .f').filter(function (t) { return t.item && t.kind === kind && t.item.id === id; })[0]; }
  function startMove(kind, id) {
    closeMenu(); moving = {kind: kind, id: id}; document.body.classList.add('mv');
    var t = findTile(kind, id); if (t) t.classList.add('moving');
  }
  function endMove() {
    var m = moving; moving = null; document.body.classList.remove('mv');
    var t = m && findTile(m.kind, m.id); if (t) t.classList.remove('moving');
    toast('تم الحفظ');
  }
  function moveItem(kind, id, dir) {
    var cur = shown(kind), i = cur.map(function (x) { return x.id; }).indexOf(id), j = i + dir;
    if (j < 0 || j >= cur.length) return;
    var tmp = cur[i]; cur[i] = cur[j]; cur[j] = tmp;
    store(kind + 'Order', cur.map(function (x) { return x.id; }));
    render(); var t = findTile(kind, id); if (t) setFocusEl(t); tick();
  }
  function hideItem(kind, id) {
    var h = hiddenIds(kind).slice(); if (h.indexOf(id) < 0) h.push(id);
    store(kind + 'Hidden', h); render(); toast('تم الإخفاء. ترجعه من لوحة الترس: إظهار المخفية');
  }
  function hiddenCount() {
    return ['app', 'sys', 'input'].reduce(function (n, k) { return n + lists[k].filter(function (a) { return hiddenIds(k).indexOf(a.id) >= 0; }).length; }, 0);
  }
  function restoreHidden() {
    var n = hiddenCount(); ['app', 'sys', 'input'].forEach(function (k) { store(k + 'Hidden', []); });
    render(); setText('qhidden', 'ما فيه'); toast(n ? 'رجعت العناصر المخفية (' + n + ')' : 'ما فيه عناصر مخفية');
  }
  // Deleting really uninstalls the app, so it goes through the root helper service (the page itself isn't allowed to).
  function deleteItem(kind, id) {
    var a = menu._item; closeMenu(); toast('جاري حذف ' + a.title + '…');
    svc('uninstall', {id: id}).then(function (r) {
      if (r.returnValue === false) return toast('تعذر الحذف: ' + (r.errorText || 'غير مسموح'));
      lists[kind] = lists[kind].filter(function (x) { return x.id !== id; }); render(); toast('تم حذف ' + a.title);
    }, function () { toast('الحذف يحتاج خدمة الروت وهي مو شغّالة'); });
  }

  // ---------- quick settings ----------
  var OUTPUTS = {tv_speaker: 'مكبر التلفزيون', external_speaker: 'مكبر خارجي', headphone: 'سماعة رأس', tv_external_speaker: 'التلفزيون + خارجي', lineout: 'مخرج خطي'};
  var PICT = {normal: 'قياسي', vivid: 'حيوي', eco: 'اقتصادي', cinema: 'سينما', sports: 'رياضة', game: 'ألعاب'};
  var PICT_ORDER = ['normal', 'vivid', 'eco', 'cinema', 'sports', 'game'], pictMode = ''; // the modes this TV offers (read from its settings)
  function setText(id, t) { $(id).textContent = t; }
  function refreshQuick() {
    var d = new Date();
    setText('qdate', dayLine(d)); setText('qtime', hm(d)); setText('qclicks', clicksOn ? 'مفعّل' : 'مطفأ'); setText('qmatches', updatedText()); setText('qhidden', hiddenCount() ? hiddenCount() + ' مخفية' : 'ما فيه');
    if (!onTV) { setText('qwifi', 'Wi-Fi'); setText('qaudio', 'مكبر التلفزيون'); setText('qbt', 'مفعّل'); setText('qvol', '15'); return; }
    svc('sys').then(function (r) {
      var n = r.net || {}, w = n.wifi, b = r.bt && r.bt.adapters && r.bt.adapters[0], v = r.vol || {};
      setText('qwifi', w && w.state === 'connected' ? (w.displayName || w.ssid || 'متصل') : n.wired && n.wired.state === 'connected' ? 'سلكي' : 'غير متصل');
      setText('qaudio', (r.audio && OUTPUTS[r.audio.soundOutput]) || (r.audio && r.audio.soundOutput) || '');
      setText('qbt', b && b.powered ? 'مفعّل' : 'مطفأ');
      setText('qvol', v.muteStatus ? 'صامت' : v.volume);
      setText('qmem', r.memMB != null ? 'المتاح ' + r.memMB + ' ميجا' : '');
    }).catch(function () {});
    svc('alertsGet').then(alertsShow).catch(function () { setText('qalerts', 'غير متاح'); });
    svc('getAv').then(function (r) {
      if (!r.returnValue) throw 0;
      pictMode = (r.picture || {}).pictureMode || ''; setText('qpicture', PICT[pictMode] || pictMode);
      $('qsvc').hidden = false; if (view === 'quick') buildNav();
    }).catch(function () { $('qsvc').hidden = true; });
  }
  var alertsOn = true;
  function alertsShow(r) { alertsOn = !!r.enabled; setText('qalerts', alertsOn ? (r.live ? r.live + ' مباراة جارية الآن' : 'مفعّلة · دوري أبطال أوروبا') : 'مطفأة'); }
  function volume(dir) {
    svc('volume', {dir: dir}).then(function (r) { setText('qvol', r.muteStatus ? 'صامت' : r.volume); }, function () { toast('تعذر تغيير الصوت'); });
  }
  function openQuick() { refreshQuick(); $('qpanel').hidden = $('qdim').hidden = false; view = 'quick'; pos.r = 0; pos.c = 0; buildNav(); }
  function closeQuick() { $('qpanel').hidden = $('qdim').hidden = true; view = 'home'; buildNav(); setFocusEl($('gear')); }
  function quickAct(q) {
    ({
      close: closeQuick,
      screensaver: function () { closeQuick(); setTimeout(showSaver, 250); },
      wifi: function () { launchPlain('com.palm.app.settings'); },
      bluetooth: function () { launchPlain('com.palm.app.settings'); },
      audio: function () { launchPlain('com.palm.app.settings'); },
      clicks: function () { clicksOn = !clicksOn; store('clicks', clicksOn); setText('qclicks', clicksOn ? 'مفعّل' : 'مطفأ'); tick(); },
      restore: function () { restoreHidden(); },
      refreshmatches: function () { manualRefresh(); },
      alerts: function () {
        svc('alertsSet', {enabled: !alertsOn}).then(function (r) {
          alertsShow(r);
          if (r.enabled) { svc('alertsTest'); toast('تم تشغيل تنبيهات الأهداف · وصلك إشعار تجربة'); } else toast('تم إيقاف تنبيهات الأهداف');
        }, function () { toast('التنبيهات تحتاج خدمة الروت وهي مو شغّالة'); });
      },
      clean: function () {
        toast('جاري تنظيف الذاكرة…');
        svc('cleanMemory').then(function (r) {
          var d = r.afterMB != null && r.beforeMB != null ? r.afterMB - r.beforeMB : 0;
          toast(r.closed.length ? 'أُغلق ' + r.closed.length + ' تطبيق · المتاح ' + r.afterMB + ' ميجا (' + (d >= 0 ? '+' : '') + d + ')' : 'ما فيه شي يستاهل الإغلاق · المتاح ' + r.afterMB + ' ميجا');
          setText('qmem', 'المتاح ' + r.afterMB + ' ميجا');
        }, function () { toast('التنظيف يحتاج خدمة الروت وهي مو شغّالة'); });
      },
      picture: function () { // next mode; one that fails is skipped, and 'normal' is always reachable
        var i = PICT_ORDER.indexOf(pictMode), tries = 0;
        (function next() {
          var m = PICT_ORDER[(i + 1 + tries) % PICT_ORDER.length];
          svc('setPicture', {pictureMode: m}).then(function (r) {
            if (r.returnValue === false) throw 0;
            pictMode = m; setText('qpicture', PICT[m] || m);
          }).catch(function () { if (++tries < PICT_ORDER.length) next(); else toast('تعذر تغيير وضع الصورة'); });
        })();
      },
      screenoff: function () { svc('screenOff').then(function () { closeQuick(); }, function () { toast('تعذر إطفاء الشاشة'); }); },
      voldown: function () { volume('down'); }, volup: function () { volume('up'); }
    })[q]();
  }
  function launchPlain(id) { closeQuick(); openApp(id); }

  // ---------- screensaver ----------
  var lastActive = Date.now(), saverMouse = null, driftTimer, saverTimer;
  function noteActivity() { lastActive = Date.now(); }
  // Weather and the next match as two quiet chips; refreshed with the clock.
  function fillSaverInfo() {
    var h = '', m = matches[0];
    if (wx) h += '<span class="schip"><svg viewBox="0 0 24 24">' + wxSvg(wx.code, wx.day) + '</svg><b>' + wx.temp + '°</b><bdi dir="rtl">' + esc([wxDesc(wx.code), C.CITY_AR[wx.city] || wx.city].filter(Boolean).join(' · ')) + '</bdi></span>';
    if (m) {
      var cd = countdown(m);
      h += '<span class="schip">' + (m.ba ? '<img src="' + esc(m.ba) + '" alt="">' : '') + '<bdi dir="rtl"><b>' + esc(m.a) + '</b></bdi><span class="vs">ضد</span><bdi dir="rtl"><b>' + esc(m.b) + '</b></bdi>' +
        (m.bb ? '<img src="' + esc(m.bb) + '" alt="">' : '') + '<i class="sep"></i><bdi dir="rtl">' + esc(cd.text) + '</bdi></span>';
    }
    $('saverInfo').innerHTML = h;
  }
  function showSaver() {
    if (view === 'saver') return;
    var hr = new Date().getHours();
    $('saver').style.opacity = (hr >= 23 || hr < 5) ? '.55' : '1'; // dimmer at night
    updateSaverClock(); view = 'saver'; saverMouse = null; $('saver').hidden = false;
    clearInterval(saverTimer); saverTimer = setInterval(updateSaverClock, 10000);
    clearInterval(driftTimer); driftTimer = setInterval(drift, 45000); drift();
  }
  function updateSaverClock() { var d = new Date(), h = d.getHours(); $('saverClock').textContent = (h % 12 || 12) + ':' + ('0' + d.getMinutes()).slice(-2); $('saverDate').textContent = dayLine(d) + ' · ' + (h >= 12 ? 'مساءً' : 'صباحًا'); fillSaverInfo(); }
  function drift() { $('saverBox').style.transform = 'translate(' + Math.round((Math.random() - .5) * 240) + 'px,' + Math.round((Math.random() - .5) * 110) + 'px)'; }
  function hideSaver() { $('saver').hidden = true; view = 'home'; clearInterval(driftTimer); clearInterval(saverTimer); noteActivity(); buildNav(); }
  setInterval(function () {
    if (view === 'home' && !document.hidden && Date.now() - lastActive > (C.SAVER_MINUTES || 5) * 60000) showSaver();
  }, 5000);

  // ---------- navigation ----------
  var nav = [], pos = {r: 0, c: 0}, view = 'home', cur = null;
  function all(sel) { return [].slice.call(document.querySelectorAll(sel)); }
  function buildNav() {
    var keep = cur;
    if (view === 'home') {
      nav = [[$('gear')], matches.length ? [$('hero')] : null, all('#appsStrip .f'), all('#bottom .f')];
    } else if (view === 'quick') {
      nav = all('#quick .qrow').filter(function (r) { return !r.hidden; }).map(function (r) { return [].slice.call(r.querySelectorAll('.f')); });
    } else if (view === 'menu') {
      nav = [all('#appmenu .chip').filter(function (c) { return !c.hidden; })];
    } else if (view === 'matches') {
      nav = [all('#matchesBox .f')];
    } else nav = [];
    nav = nav.filter(function (r) { return r && r.length; });
    // stay on the same element when the rows are rebuilt
    var at = keep && locate(keep);
    if (at) { pos.r = at[0]; pos.c = at[1]; } else pos.r = Math.min(pos.r, Math.max(0, nav.length - 1));
    focus(true);
  }
  function locate(e) {
    for (var r = 0; r < nav.length; r++) { var c = nav[r].indexOf(e); if (c >= 0) return [r, c]; }
    return null;
  }
  function setFocusEl(e) { var at = locate(e); if (at) { pos.r = at[0]; pos.c = at[1]; focus(); } }
  function focus(quiet) {
    var old = document.querySelector('.focus'); if (old) old.classList.remove('focus');
    cur = null;
    if (!nav.length) return;
    var row = nav[pos.r]; pos.c = Math.max(0, Math.min(pos.c, row.length - 1));
    var e = row[pos.c]; e.classList.add('focus'); cur = e;
    var strip = e.parentNode;
    if (strip && strip.classList && strip.classList.contains('strip') && strip.parentNode.classList.contains('clip')) {
      var shift = strip._shift || 0, left = strip.offsetLeft + e.offsetLeft + shift, right = left + e.offsetWidth + 34;
      if (right > 1848 + 34) shift -= right - (1848 + 34); else if (left < 72) shift += 72 - left;
      shift = Math.min(0, shift);
      strip._shift = shift; strip.style.transform = 'translateX(' + shift + 'px)';
    }
  }
  function centerX(e) { var r = e.getBoundingClientRect(); return r.left + r.width / 2; }
  function move(dr, dc) {
    var moved = false;
    if (dr) {
      var nr = pos.r + dr;
      if (nr >= 0 && nr < nav.length) {
        var x = centerX(cur), best = 0, bd = 1e9;
        nav[nr].forEach(function (e, i) { var d = Math.abs(centerX(e) - x); if (d < bd) { bd = d; best = i; } });
        pos.r = nr; pos.c = best; moved = true;
      }
    } else {
      var row = nav[pos.r];
      if (view === 'home' && cur === $('hero')) return cycleHero(dc);
      if (row && pos.c + dc >= 0 && pos.c + dc < row.length) { pos.c += dc; moved = true; }
    }
    if (moved) { focus(); tick(); }
  }
  function act(e) {
    if (e === $('gear')) return openQuick();
    if (e === $('hero')) return openMatches();
    if (e.dataset && e.dataset.m === 'refresh') return manualRefresh();
    if (e.dataset && e.dataset.q) return quickAct(e.dataset.q);
    if (e.dataset && e.dataset.menu) {
      var kind = menu.dataset.kind, id = menu.dataset.id, a = e.dataset.menu;
      if (a === 'move') startMove(kind, id);
      else if (a === 'hide') { closeMenu(); hideItem(kind, id); }
      else if (a === 'del') setMenuMode('confirm');
      else if (a === 'no') setMenuMode('main');
      else if (a === 'yes') deleteItem(kind, id);
      return;
    }
    if (e.onok) return e.onok();
  }
  // OK is handled on release so that holding it (500ms) can open the menu instead of launching.
  var pressStart = 0, pressTimer, pressLong = false, lastEnter = 0;
  document.addEventListener('keydown', function (ev) {
    var k = ev.key; noteActivity(); touched = true;
    if (view === 'saver') { ev.preventDefault(); hideSaver(); return; }
    // webOS remotes send Back as keyCode 461, "Backspace" or "GoBack" depending on firmware
    var isBack = ev.keyCode === 461 || k === 'Backspace' || k === 'GoBack' || k === 'Escape';
    if (moving) { // dragging a tile: arrows reorder it, OK or Back drops it
      ev.preventDefault();
      if (isBack || k === 'Enter') { pressStart = 0; endMove(); }
      else if (k === 'ArrowLeft') moveItem(moving.kind, moving.id, -1);
      else if (k === 'ArrowRight') moveItem(moving.kind, moving.id, 1);
      return;
    }
    if (isBack) {
      if (view === 'matches') { ev.preventDefault(); return closeMatches(); }
      if (view === 'menu') { ev.preventDefault(); return menu._mode === 'confirm' ? setMenuMode('main') : closeMenu(); }
      if (view === 'quick') { ev.preventDefault(); return closeQuick(); }
      return;
    }
    if (k === 'Enter') {
      ev.preventDefault();
      var now = Date.now();
      if (!pressStart || now - lastEnter > 1000) { // a new press (not a key repeat)
        pressStart = now; pressLong = false; clearTimeout(pressTimer);
        pressTimer = setTimeout(function () { if (pressStart && !pressLong) { pressLong = true; longPress(cur); } }, 500);
      }
      lastEnter = now;
    }
    else if (k === 'ArrowDown') { ev.preventDefault(); move(1, 0); }
    else if (k === 'ArrowUp') { ev.preventDefault(); move(-1, 0); }
    else if (k === 'ArrowRight') { ev.preventDefault(); move(0, 1); }
    else if (k === 'ArrowLeft') { ev.preventDefault(); move(0, -1); }
  });
  document.addEventListener('keyup', function (ev) {
    if (ev.key !== 'Enter') return;
    clearTimeout(pressTimer);
    var was = pressStart, long = pressLong; pressStart = 0; pressLong = false;
    if (!was || long || moving || view === 'saver') return;
    if (cur) act(cur);
  });

  // Magic Remote pointer: hovering selects, clicking opens
  function targetOf(ev) { var n = ev.target; return n && n.closest ? n.closest('.f') : null; }
  document.addEventListener('mousemove', function (ev) {
    noteActivity(); touched = true;
    if (view === 'saver') {
      if (!saverMouse) saverMouse = {x: ev.clientX, y: ev.clientY};
      else if (Math.abs(ev.clientX - saverMouse.x) + Math.abs(ev.clientY - saverMouse.y) > 60) hideSaver();
      return;
    }
    var t = targetOf(ev);
    if (t && t !== cur && locate(t)) setFocusEl(t);
  });
  var pointerTimer, pointerLong = false;
  document.addEventListener('mousedown', function (ev) {
    pointerLong = false; clearTimeout(pointerTimer);
    var t = targetOf(ev);
    if (view === 'home' && t && t.item) pointerTimer = setTimeout(function () { pointerLong = true; setFocusEl(t); longPress(t); }, 650);
  });
  document.addEventListener('mouseup', function () { clearTimeout(pointerTimer); });
  document.addEventListener('click', function (ev) {
    noteActivity();
    if (pointerLong) { pointerLong = false; return; }
    if (moving) return endMove();
    if (view === 'saver') return hideSaver();
    if (ev.target === $('qdim')) return closeQuick();
    if (view === 'matches' && !targetOf(ev)) return closeMatches();
    var t = targetOf(ev);
    if (t && locate(t)) { setFocusEl(t); act(t); }
  });
  document.addEventListener('wheel', function (ev) { if (view === 'home') { noteActivity(); move(ev.deltaY > 0 ? 1 : -1, 0); } });

  // ---------- bottom line of writing (config.js BRAND) ----------
  (function () {
    var b = C.BRAND || {};
    if (!b.title) { $('brand').hidden = true; return; }
    $('brandTitle').textContent = b.title; $('brandSub').textContent = b.sub || ''; $('brandSub').hidden = !b.sub;
    $('emblem').hidden = !b.emblem; if (b.emblem) $('emblem').src = 'icons/' + b.emblem;
    [['crestsL', b.left], ['crestsR', b.right], ['saverCrestsL', b.left], ['saverCrestsR', b.right]].forEach(function (g) {
      (g[1] || []).forEach(function (f) { var i = el('img'); i.src = 'icons/crests/' + f; i.alt = ''; $(g[0]).appendChild(i); });
    });
    $('saverTitle').textContent = b.title; $('saverEmblem').hidden = !b.emblem; if (b.emblem) $('saverEmblem').src = 'icons/' + b.emblem;
  })();

  // ---------- clock ----------
  function clockTick() {
    var d = new Date(), h = d.getHours();
    $('clock').textContent = (h % 12 || 12) + ':' + ('0' + d.getMinutes()).slice(-2) + (h >= 12 ? ' م' : ' ص');
    $('date').textContent = dayLine(d);
  }
  clockTick(); setInterval(clockTick, 15000);

  // ---------- boot ----------
  function booted() { var b = $('boot'); if (b) { b.classList.add('gone'); setTimeout(function () { b.remove(); }, 700); } }
  // After a deploy the page reloads itself (only while idle on the home view).
  function watchBuild() {
    if (!onTV) return;
    var id = null;
    svc('build').then(function (r) { id = r.id; }).catch(function () {});
    setInterval(function () {
      svc('build').then(function (r) { if (!id) { id = r.id; return; } if (r.id && r.id !== id && view === 'home' && !moving) location.reload(); }).catch(function () {});
    }, 15000);
  }
  // style.css loads without blocking (so the loading screen paints first). Measure only once it has applied.
  function whenCss(fn) {
    var done = false, go = function () { if (done) return; done = true; fn(); };
    if (window.__css) go(); else { window.addEventListener('cssready', go); setTimeout(go, 3000); }
  }
  var bootFromSvc = false;
  function start(r) {
    MARK.probed = Math.round(performance.now());
    // If a rescan already got the real list from the service while this slow boot probe was running, keep that one.
    if (!appsSig || bootFromSvc) { lists.app = r[0]; lists.sys = r[1]; }
    lists.input = r[2];
    document.body.classList.add('enter');
    render(); MARK.rendered = Math.round(performance.now());
    whenCss(function () {
      all('.strip').forEach(function (s) { s._shift = 0; s.style.transform = ''; });
      var first = all('#appsStrip .f')[0]; if (first) setFocusEl(first);
      watchBuild();
      // The service starts a few seconds after the page at boot: until it answers, keep asking (so a stale list never stays on screen).
      if (!appsSig) { var retry = setInterval(function () { if (appsSig) clearInterval(retry); else rescan(); }, 4000); }
      booted(); MARK.booted = Math.round(performance.now()); dbg('boot ' + VERSION + ' id=' + (location.pathname.split('/').slice(-2, -1)[0]) + ' ' + JSON.stringify(MARK)); setTimeout(initAudio, 1500); setTimeout(warm, 1800); setTimeout(function () { document.body.classList.remove('enter'); }, 2600);
    });
  }
  knownApps().then(function (apps) {
    if (apps) { appsSig = apps.map(function (x) { return x.id; }).sort().join(','); bootFromSvc = true; }
    var ids = apps ? apps.map(function (x) { return x.id; }) : null;
    return Promise.all([buildApps(apps), probe(C.SYSTEM, ids), C.INPUTS.slice()]);
  }).then(start)
    .catch(function () { start([C.APPS.slice(), C.SYSTEM.slice(), C.INPUTS.slice()]); });
})();

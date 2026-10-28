// TV Home launcher service, adapted from zzeppieri/webos-custom-home (MIT).
// A registered webOS service inside the TV Home package, so applicationManager/launch is allowed.
// Reads the remote's key events without grabbing them, and opens TV Home:
//   --boot   at startup (retries until the app manager is ready)
//   --watch  whenever HOME is pressed; also after waking from standby
var Service = require('webos-service');
var fs = require('fs');
var vm = require('vm');
var uefa = require('./uefa');       // UEFA's own match API: the only source of match alerts

// With as-home.sh, TV Home *is* com.webos.app.home. Native apps like Netflix don't hand the Home key to the
// system, so we still watch the remote and open the home app ourselves.
var TAKEOVER = fs.existsSync('/usr/palm/applications/com.webos.app.home/index.html');
var APP_ID = TAKEOVER ? 'com.webos.app.home' : 'com.albrhi.tvhome';
var HOME_CODE = 773; // HOME on LG Magic Remotes (confirmed on the 55UR8040)
var EV_KEY = 1, EVENT_SIZE = 16; // 32-bit struct input_event
var LOG = '/tmp/tvhome-svc.log';
var ARGS = process.argv.slice(2);
function log(msg) { try { fs.appendFileSync(LOG, '[' + new Date().toISOString() + '] ' + msg + '\n'); } catch (e) { /* ignore */ } }

// webos-service exits after ~5s idle unless timeouts are disabled.
if (process.argv.indexOf('--disable-timeouts') === -1) process.argv.push('--disable-timeouts');
var service = new Service('com.albrhi.tvhome.service');
process.on('uncaughtException', function (e) { log('uncaught: ' + (e && e.stack || e)); }); // never let a bug in an extra feature take the Home-key watcher down
try { fs.writeFileSync('/tmp/tvhome-svc.pid', String(process.pid)); } catch (e) { /* ignore */ }
log('start pid=' + process.pid + ' args=' + ARGS.join(' ') + ' app=' + APP_ID);
service.register('status', function (m) { m.respond({returnValue: true, running: true, pid: process.pid}); });

// ---- Privileged helpers for the TV Home page. The page's own app id isn't allowed these calls, but this
// service runs as root, and root's luna-send is. A fixed list of methods, never a pass-through.
var cp = require('child_process');
var screenIsOff = false; // set by screenOff; the next key press turns the screen back on (see watchAll)
function lunaRoot(uri, params) {
  return new Promise(function (ok) {
    cp.execFile('luna-send', ['-n', '1', '-w', '5000', uri, JSON.stringify(params || {})], {timeout: 8000, maxBuffer: 4 << 20}, function (e, out) {
      try { ok(JSON.parse(out)); } catch (x) { ok({returnValue: false, errorText: String(e ? e.message : out).slice(0, 200)}); }
    });
  });
}
var iconCache = {};
function iconData(path) { // launch-point icons live in other apps' folders the page can't read
  if (!path) return '';
  if (iconCache[path] === undefined) {
    try { iconCache[path] = 'data:image/png;base64,' + fs.readFileSync(path).toString('base64'); } catch (e) { iconCache[path] = ''; }
  }
  return iconCache[path];
}
var SKIP_APPS = ['com.webos.app.home', 'com.albrhi.tvhome'];
var iconPath = {}; // app id -> icon file, filled by getApps; getIcon only serves these (no arbitrary file reads)
var api = {
  getApps: function () {
    return lunaRoot('luna://com.webos.applicationManager/listLaunchPoints').then(function (r) {
      return {returnValue: !!r.launchPoints, apps: (r.launchPoints || []).filter(function (p) { return !p.hidden && SKIP_APPS.indexOf(p.id) < 0; })
        .map(function (p) { iconPath[p.id] = p.mediumLargeIcon || p.largeIcon || p.icon; return {id: p.id, title: p.title, color: p.bgColor || p.iconColor, system: !!p.systemApp}; })};
    });
  },
  getIcon: function (p) { return Promise.resolve({returnValue: true, icon: iconData(iconPath[p.id])}); },
  getInputs: function () {
    return lunaRoot('luna://com.webos.service.eim/getAllInputStatus').then(function (r) {
      return {returnValue: !!r.devices, inputs: (r.devices || []).map(function (d) { return {id: d.appId, label: d.label, connected: d.connected, active: d.activate}; })};
    });
  },
  getAv: function () {
    return Promise.all([
      lunaRoot('luna://com.webos.settingsservice/getSystemSettings', {category: 'picture', keys: ['pictureMode', 'backlight']}),
      lunaRoot('luna://com.webos.settingsservice/getSystemSettings', {category: 'sound', keys: ['soundMode']})
    ]).then(function (r) { return {returnValue: true, picture: r[0].settings || {}, sound: r[1].settings || {}}; });
  },
  setPicture: function (p) { // {pictureMode} or {backlight}
    var s = {};
    if (p.pictureMode) s.pictureMode = String(p.pictureMode);
    if (p.backlight != null) s.backlight = String(Math.max(0, Math.min(100, +p.backlight)));
    return lunaRoot('luna://com.webos.settingsservice/setSystemSettings', {category: 'picture', settings: s});
  },
  setSound: function (p) { return lunaRoot('luna://com.webos.settingsservice/setSystemSettings', {category: 'sound', settings: {soundMode: String(p.soundMode)}}); },
  screenOff: function () { return lunaRoot('luna://com.webos.service.tvpower/power/turnOffScreen', {}).then(function (r) { screenIsOff = r.returnValue !== false; return r; }); },
  screenOn: function () { return lunaRoot('luna://com.webos.service.tvpower/power/turnOnScreen', {}); },
  // Open an app as root. The page's own launch call sometimes does nothing for system apps (Settings, Homebrew),
  // while this always works. Id is validated; no other luna method is reachable from here.
  launch: function (p) {
    var id = String(p.id || '');
    if (!/^[A-Za-z0-9._-]+$/.test(id)) return Promise.resolve({returnValue: false, errorText: 'bad id'});
    return lunaRoot('luna://com.webos.applicationManager/launch', {id: id, params: p.params || {}});
  },
  // Everything the quick-settings panel shows, in one call. As com.webos.app.home the page's own luna calls never
  // get an answer (no role for that id), so the page asks this service instead.
  sys: function () {
    return Promise.all([
      lunaRoot('luna://com.webos.service.connectionmanager/getStatus'),
      lunaRoot('luna://com.webos.service.audio/getSoundOutput'),
      lunaRoot('luna://com.webos.service.bluetooth2/adapter/getStatus'),
      lunaRoot('luna://com.webos.audio/getVolume')
    ]).then(function (r) { return {returnValue: true, net: r[0], audio: r[1], bt: r[2], vol: r[3], memMB: memAvailMB()}; });
  },
  volume: function (p) {
    var dir = p.dir === 'down' ? 'volumeDown' : 'volumeUp';
    return lunaRoot('luna://com.webos.service.audio/master/' + dir).then(function () { return lunaRoot('luna://com.webos.audio/getVolume'); });
  },
  // A cheap "has the page changed on disk" id. The page polls this and reloads itself when it changes (see app.js): killing
  // the home page's process to refresh it makes the system lose track of the home app, so we never do that.
  build: function () {
    var d = fs.existsSync('/tmp/tvhome-as-home/index.html') ? '/tmp/tvhome-as-home/' : '/media/developer/apps/usr/palm/applications/com.albrhi.tvhome/', t = 0;
    ['index.html', 'app.js', 'style.css', 'config.js'].forEach(function (f) { try { t += fs.statSync(d + f).mtimeMs; } catch (e) { /* missing */ } });
    return Promise.resolve({returnValue: true, id: String(Math.round(t))});
  },
  log: function (p) { log('page: ' + String(p.m || '').slice(0, 300)); return Promise.resolve({returnValue: true}); },
  // Uninstall one downloaded app (the page's long-press menu -> Delete). Only apps the TV itself lists as
  // removable and not a system app; never Homebrew Channel (it keeps root alive) or TV Home itself.
  uninstall: function (p) {
    var id = String(p.id || '');
    if (!/^[A-Za-z0-9._-]+$/.test(id) || ['org.webosbrew.hbchannel', 'com.albrhi.tvhome', 'com.webos.app.home'].indexOf(id) >= 0)
      return Promise.resolve({returnValue: false, errorText: 'محمي'});
    return lunaRoot('luna://com.webos.applicationManager/listLaunchPoints').then(function (r) {
      var lp = (r.launchPoints || []).filter(function (x) { return x.id === id; })[0];
      if (!lp || lp.systemApp || lp.removable === false) return {returnValue: false, errorText: 'تطبيق نظام أو غير موجود'};
      log('uninstall ' + id);
      return lunaRoot('luna://com.webos.appInstallService/remove', {id: id});
    });
  }
};
// ---------- settings kept across deploys (not inside the app folder) ----------
var SETTINGS_FILE = '/media/developer/tvhome-settings.json', settings = {alerts: true};
try { var saved = JSON.parse(fs.readFileSync(SETTINGS_FILE, 'utf8')); Object.keys(saved).forEach(function (k) { settings[k] = saved[k]; }); } catch (e) { /* first run: defaults */ }
function saveSettings() { try { fs.writeFileSync(SETTINGS_FILE, JSON.stringify(settings)); } catch (e) { log('settings not saved: ' + e.message); } }

// The page's config.js, read here too (teams, Arabic names, alert options). Re-read when the file changes.
var cfgCache = null, cfgStamp = 0;
function readConfig() {
  var f = fs.existsSync('/tmp/tvhome-as-home/config.js') ? '/tmp/tvhome-as-home/config.js' : '/media/developer/apps/usr/palm/applications/com.albrhi.tvhome/config.js';
  var m = 0; try { m = fs.statSync(f).mtimeMs; } catch (e) { return cfgCache || {}; }
  if (m !== cfgStamp) {
    try { var ctx = {window: {}}; vm.runInNewContext(fs.readFileSync(f, 'utf8'), ctx, {timeout: 1000}); cfgCache = ctx.window.CONFIG || {}; cfgStamp = m; } catch (e) { log('config.js not readable: ' + e.message); }
  }
  return cfgCache || {};
}

// ---------- memory: the TV has ~1.2 GB and keeps every app it opened. Close the ones nobody is using. ----------
function memAvailMB() { try { var m = /MemAvailable:\s+(\d+)/.exec(fs.readFileSync('/proc/meminfo', 'utf8')); return m ? Math.round(m[1] / 1024) : null; } catch (e) { return null; } }
api.mem = function () { return Promise.resolve({returnValue: true, memMB: memAvailMB()}); };
api.cleanMemory = function () {
  return Promise.all([lunaRoot('luna://com.webos.applicationManager/running'), lunaRoot('luna://com.webos.applicationManager/listLaunchPoints'),
    lunaRoot('luna://com.webos.applicationManager/getForegroundAppInfo')]).then(function (r) {
    var run = (r[0].running || []).map(function (x) { return x.id; }), lp = (r[1].launchPoints || []).map(function (x) { return x.id; }), fg = r[2].appId;
    var keep = ['com.webos.app.home', 'com.albrhi.tvhome', 'org.webosbrew.hbchannel', fg];
    // only apps you can open yourself (and inputs / Live TV / browser); system services are never touched
    var closable = run.filter(function (id) { return keep.indexOf(id) < 0 && (lp.indexOf(id) >= 0 || /^com\.webos\.app\.(hdmi\d|livetv|browser)/.test(id)); });
    var before = memAvailMB();
    return Promise.all(closable.map(function (id) { return lunaRoot('luna://com.webos.applicationManager/closeByAppId', {id: id}); }))
      .then(function () { return new Promise(function (ok) { setTimeout(ok, 1800); }); })
      .then(function () { log('cleaned ' + closable.length + ' apps: ' + closable.join(',')); return {returnValue: true, closed: closable, beforeMB: before, afterMB: memAvailMB()}; });
  });
};
// automatic: only while the home screen is in front (never closes something you are using), when memory is really low
setInterval(function () {
  var limit = (readConfig().AUTO_CLEAN_MB != null ? readConfig().AUTO_CLEAN_MB : 150), avail = memAvailMB();
  if (!limit || avail == null || avail >= limit || settings.autoClean === false) return;
  lunaRoot('luna://com.webos.applicationManager/getForegroundAppInfo').then(function (fg) {
    if (fg.appId === APP_ID || fg.appId === 'com.webos.app.home') { log('auto-clean at ' + avail + ' MB'); return api.cleanMemory(); }
  }).catch(function () {});
}, 5 * 60 * 1000);

// ---------- match alerts (UEFA source), drawn by our overlay app over whatever is on screen ----------
function toast(text) { return lunaRoot('luna://com.webos.notification/createToast', {message: String(text).slice(0, 180), noaction: true}); }
// The card is the small overlay app com.albrhi.alert (see alert/). If it can't be opened, fall back to LG's own plain toast.
function showAlert(a) {
  return lunaRoot('luna://com.webos.applicationManager/launch', {id: 'com.albrhi.alert', params: a.p}).then(function (r) {
    if (r && r.returnValue === false) { log('overlay refused (' + (r.errorText || '') + '): using a toast'); return toast(a.text); }
  });
}
var alertsRun = uefa.start({log: log, show: showAlert, enabled: function () { return settings.alerts !== false; }, config: readConfig});
api.alertsGet = function () { var st = alertsRun.status(); return Promise.resolve({returnValue: true, enabled: settings.alerts !== false, live: st.live, last: st.last, errors: st.errors}); };
api.alertsSet = function (p) { settings.alerts = !!p.enabled; saveSettings(); log('alerts ' + (settings.alerts ? 'on' : 'off')); return api.alertsGet(); };
api.alertsTest = function (p) { // a sample card of the kind asked for (goal, red, var, penalty, cancel, kickoff, full), with the local crests
  var base = {H: 'ريال مدريد', A: 'برشلونة', h: 2, a: 1, logoH: 'crests/madrid.png', logoA: 'crests/barcelona.png', comp: 'دوري أبطال أوروبا', side: '', who: '', minute: ''};
  var kinds = {goal: {tag: 'هدف!', who: 'فينيسيوس جونيور', minute: 67, side: 'h'}, red: {tag: 'بطاقة حمراء', who: 'لاعب تجريبي', minute: 84, side: 'a'}, var: {tag: 'مراجعة VAR', who: 'ركلة جزاء', minute: 71},
    penalty: {tag: 'ركلة جزاء', minute: 58}, cancel: {tag: 'إلغاء هدف'}, kickoff: {tag: 'بدأت المباراة', showScore: false}, full: {tag: 'نهاية المباراة'}};
  var kind = kinds[p && p.kind] ? p.kind : 'goal', pl = {kind: kind}; [base, kinds[kind]].forEach(function (o) { Object.keys(o).forEach(function (k) { pl[k] = o[k]; }); });
  alertsRun.test({kind: kind, text: 'اختبار التنبيه: ' + pl.tag, p: pl}); return Promise.resolve({returnValue: true});
};

Object.keys(api).forEach(function (name) {
  service.register(name, function (m) { api[name](m.payload || {}).then(function (r) { m.respond(r); }); });
});
// Same methods over HTTP on the TV's loopback only: TV Home running as com.webos.app.home isn't in this
// service's luna permission group, but it can fetch() this. POST /<method> with a JSON body.
var API_PORT = 45620;
require('http').createServer(function (req, res) {
  var name = req.url.slice(1).split('?')[0], body = '';
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Content-Type', 'application/json');
  if (!api[name]) { res.statusCode = 404; return res.end('{"returnValue":false,"errorText":"unknown method"}'); }
  req.on('data', function (c) { body += c; if (body.length > 1e4) req.destroy(); });
  req.on('end', function () {
    var p = {}; try { p = JSON.parse(body || '{}'); } catch (e) { /* empty params */ }
    api[name](p).then(function (r) { res.end(JSON.stringify(r)); });
  });
}).listen(API_PORT, '127.0.0.1', function () { log('api on 127.0.0.1:' + API_PORT); });

function launch(reason, cb) {
  service.call('luna://com.webos.applicationManager/launch', {id: APP_ID}, function (m) {
    log('launch (' + reason + '): ' + JSON.stringify(m && m.payload));
    if (cb) cb(m && m.payload && m.payload.returnValue === true);
  });
}
// LG's own home still opens on HOME (we only read the key) and finishes animating in after our
// first launch, covering TV Home. So launch again once it has appeared. Tune the delays if needed.
var HOME_DELAYS = TAKEOVER ? [150] : [400, 1300]; // with the takeover there is no LG home to wait for
var busy = false;
function onHome() {
  if (busy) return;
  busy = true; setTimeout(function () { busy = false; }, 1600);
  HOME_DELAYS.forEach(function (ms, i) { setTimeout(function () { launch('home-key #' + (i + 1)); }, ms); });
}

// Boot: the app manager isn't ready straight away, so retry for up to ~60s.
function bootLaunch(n) { launch('boot #' + n, function (ok) { if (!ok && n < 30) setTimeout(function () { bootLaunch(n + 1); }, 2000); }); }

// Poll every input device without blocking (blocking reads would use up libuv's thread pool).
function watchAll() {
  var fds = [];
  try {
    fs.readdirSync('/dev/input').filter(function (f) { return f.indexOf('event') === 0; }).forEach(function (f) {
      try { fds.push(fs.openSync('/dev/input/' + f, fs.constants.O_RDONLY | fs.constants.O_NONBLOCK)); } catch (e) { /* skip */ }
    });
  } catch (e) { log('readdir /dev/input: ' + e.message); return; }
  log('watching ' + fds.length + ' input devices');
  var buf = Buffer.alloc(EVENT_SIZE * 64);
  setInterval(function () {
    for (var i = 0; i < fds.length; i++) {
      var n = 0;
      try { n = fs.readSync(fds[i], buf, 0, buf.length, null); } catch (e) { continue; } // EAGAIN = nothing waiting
      for (var off = 0; off + EVENT_SIZE <= n; off += EVENT_SIZE) {
        if (buf.readUInt16LE(off + 8) !== EV_KEY || buf.readInt32LE(off + 12) !== 1) continue;
        var code = buf.readUInt16LE(off + 10);
        if (screenIsOff) { screenIsOff = false; api.screenOn(); log('screen on (key ' + code + ')'); continue; }
        if (code === HOME_CODE) { log('HOME key'); onHome(); }
      }
    }
  }, 50); // key presses wait in the kernel queue, so 50 ms is plenty (16 ms used ~11% of a core even when idle)
}

if (ARGS.indexOf('--boot') !== -1) setTimeout(function () { bootLaunch(1); }, 1000);
if (ARGS.indexOf('--watch') !== -1) watchAll();

// Waking from standby doesn't rerun the startup hook, but this process is frozen during standby,
// so a big gap in a 1s heartbeat means the TV just woke: reopen TV Home a few times as the UI returns.
var last = Date.now();
setInterval(function () {
  var now = Date.now(), gap = now - last; last = now;
  if (gap > 10000) {
    log('wake after ~' + Math.round(gap / 1000) + 's');
    var tries = 0, iv = setInterval(function () { launch('wake ' + (++tries)); if (tries >= 4) clearInterval(iv); }, 1500);
  }
}, 1000);

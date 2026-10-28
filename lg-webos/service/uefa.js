// Match alerts from UEFA's own public match API (no key), run by the root service so they show over any app as system toasts.
// What it reports (Champions League by default, CONFIG.ALERTS.competitions): kick-off, every goal with the scorer (and own goal / penalty),
// a goal taken back, a red card, a penalty, VAR checks, half time, extra time / shoot-out and full time.
// Pace: while a match is live it looks every 5 s (UEFA refreshes its data every ~4-7 s) and reads each live match's event feed every 10 s;
// when nothing is live it asks for the next kick-off and sleeps until two minutes before it. Nothing is stored: it only remembers which
// goals / events it has already announced. Names come from UEFA in Arabic.
'use strict';
var getJson = require('./net.js').getJson;

var BASE = 'https://match.uefa.com/v5';
var LIVE_MS = 5000, IDLE_MS = 10 * 60 * 1000, RETRY_MS = 30 * 1000, TOAST_GAP_MS = 7000, EVENTS_EVERY = 2, DOWN_AFTER = 10;
var COMP_AR = {'1': 'دوري أبطال أوروبا', '14': 'الدوري الأوروبي', '2001': 'دوري المؤتمر الأوروبي'};
var VAR_AR = {GOAL: 'هدف', PENALTY: 'ركلة جزاء', RED_CARD: 'بطاقة حمراء', MISTAKEN_IDENTITY: 'خطأ في هوية اللاعب', OFFSIDE: 'تسلل'};
var VAR_RESULT_AR = {CONFIRMED: 'تم تأكيد القرار', CANCELLED: 'تم إلغاء القرار', OVERTURNED: 'تم تغيير القرار', NO_CHANGE: 'القرار كما هو'};

function ar(tr, key, fallback) { var v = tr && tr[key] && tr[key].AR; return (v ? String(v).trim() : '') || fallback || ''; }
function teamName(t) { return ar(t && t.translations, 'displayName', t && t.internationalName); }
function playerName(p) {
  if (!p) return '';
  var t = p.translations || {};
  return ar(t, 'name', '') || ar(t, 'shortName', '') || p.clubShirtName || p.internationalName || '';
}

// One live match from the list -> what the alert logic needs.
function describe(m) {
  var tot = (m.score && m.score.total) || {}, minute = m.minute && (m.minute.normal != null ? m.minute.normal : 0);
  var info = {id: String(m.id), H: teamName(m.homeTeam), A: teamName(m.awayTeam), homeId: String(m.homeTeam.id), awayId: String(m.awayTeam.id),
    h: tot.home || 0, a: tot.away || 0, phase: m.phase || '', minute: minute || 0,
    logoH: (m.homeTeam.mediumLogoUrl || m.homeTeam.logoUrl || ''), logoA: (m.awayTeam.mediumLogoUrl || m.awayTeam.logoUrl || ''),
    comp: COMP_AR[String(m.competition && m.competition.id)] || (m.competition && m.competition.metaData && m.competition.metaData.name) || '',
    scorers: ((m.playerEvents && m.playerEvents.scorers) || []).map(function (s) {
      return {id: String(s.id), type: String(s.goalType || ''), teamId: String(s.teamId), minute: s.time && s.time.minute, name: playerName(s.player)};
    })};
  info.scorers.sort(function (x, y) { return (x.minute || 0) - (y.minute || 0); });
  return info;
}
// An alert = the plain text (logs, and the system toast if the overlay can't open) + the data the overlay card draws.
// kind picks the colour/icon; side = which team the event is about ('h' / 'a' / '').
function mk(kind, i, text, o) {
  o = o || {};
  return {kind: kind, text: text, p: {kind: kind, tag: o.tag || '', who: o.who || '', minute: o.minute || '', side: o.side || '', H: i.H, A: i.A, h: o.h != null ? o.h : i.h, a: o.a != null ? o.a : i.a,
    logoH: i.logoH, logoA: i.logoA, comp: i.comp, showScore: o.showScore !== false}};
}
function line(i, h, a) { return i.H + ' ' + (h != null ? h : i.h) + ' - ' + (a != null ? a : i.a) + ' ' + i.A; }
function tail(i) { return i.comp ? ' · ' + i.comp : ''; }
var mnt = function (x) { return x ? ' (' + x + "')" : ''; };

// Compare the live list with what we announced before. state: {matchId: {h, a, phase, goals: {id: 1}}}. Returns [text].
function diffLive(state, matches, opt) {
  var out = [];
  matches.forEach(function (m) {
    var i = describe(m), st = state[i.id];
    if (!st) { // first time we see it: remember, announce only a kick-off we are sure about
      var goals = {}; i.scorers.forEach(function (s) { goals[s.id] = 1; });
      state[i.id] = {h: i.h, a: i.a, phase: i.phase, goals: goals, events: null, ticks: 0, live: true};
      if (opt.kickoff !== false && i.phase === 'FIRST_HALF' && i.minute <= 3 && i.h + i.a === 0) out.push(mk('kickoff', i, 'بدأت المباراة: ' + i.H + ' × ' + i.A + tail(i), {tag: 'بدأت المباراة', showScore: false}));
      return;
    }
    st.live = true;
    var h = st.h, a = st.a, now = {};
    i.scorers.forEach(function (s) { now[s.id] = 1; });
    i.scorers.filter(function (s) { return !st.goals[s.id]; }).forEach(function (s) {
      var own = /OWN/i.test(s.type), pen = /PEN/i.test(s.type), forHome = (s.teamId === i.homeId) !== own;
      if (forHome) h++; else a++;
      if (opt.goals === false) return;
      var team = s.teamId === i.homeId ? i.H : i.A;
      var side = forHome ? 'h' : 'a';
      out.push(mk('goal', i, (own ? 'هدف عكسي! ' : pen ? 'هدف من ركلة جزاء! ' : 'هدف! ') + (s.name ? s.name + ' · ' : '') + (own ? 'ضد ' + team : team) + mnt(s.minute) + ' | ' + line(i, h, a) + tail(i),
        {tag: own ? 'هدف عكسي' : pen ? 'هدف من ركلة جزاء' : 'هدف!', who: s.name, minute: s.minute, side: side, h: h, a: a}));
    });
    var gone = Object.keys(st.goals).filter(function (id) { return !now[id]; });
    if (gone.length && opt.goals !== false) out.push(mk('cancel', i, 'إلغاء هدف! ' + line(i) + tail(i), {tag: 'إلغاء هدف'}));
    st.goals = now; st.h = i.h; st.a = i.a;
    if (i.phase !== st.phase) {
      if (opt.halfTime !== false && i.phase === 'HALF_TIME_BREAK') out.push(mk('phase', i, 'استراحة بين الشوطين: ' + line(i) + tail(i), {tag: 'استراحة بين الشوطين'}));
      else if (/EXTRA/.test(i.phase) && !/EXTRA/.test(st.phase)) out.push(mk('phase', i, 'بدأ الوقت الإضافي: ' + line(i) + tail(i), {tag: 'بدأ الوقت الإضافي'}));
      else if (/PENALT/.test(i.phase) && !/PENALT/.test(st.phase)) out.push(mk('phase', i, 'ركلات الترجيح: ' + line(i) + tail(i), {tag: 'ركلات الترجيح'}));
      st.phase = i.phase;
    }
  });
  return out;
}

// A match that left the live list: announce the end once we know it finished.
function finishedText(state, m, opt) {
  var i = describe(m), st = state[i.id];
  if (!st || !st.live) return null;
  st.live = false;
  return (m.status === 'FINISHED' && opt.fullTime !== false) ? mk('full', i, 'نهاية المباراة: ' + line(i) + tail(i), {tag: 'نهاية المباراة'}) : null;
}

// Newest-first event feed of one match: red cards, penalties, VAR checks. The first look only remembers what is already there.
function diffEvents(state, m, events, opt) {
  var i = describe(m), st = state[i.id], out = [];
  if (!st) return out;
  var first = st.events === null; if (first) st.events = {};
  events.slice().reverse().forEach(function (e) {
    var key = e.id + (e.varInfo ? ':' + (e.varInfo.result && e.varInfo.result !== 'NONE' ? e.varInfo.result : '') : ''); // a VAR check is announced once, and once more when it gets a result
    if (st.events[key]) return;
    st.events[key] = 1;
    if (first) return;
    var p = e.primaryActor && e.primaryActor.person, club = p && String(p.clubId), team = club === i.homeId ? i.H : club === i.awayId ? i.A : '';
    var min = e.time && e.time.minute, type = String(e.type || '');
    if (opt.redCards !== false && /RED/.test(type)) out.push(mk('red', i, (/YELLOW/.test(type) ? 'بطاقة حمراء (إنذار ثانٍ)! ' : 'بطاقة حمراء! ') + playerName(p) + (team ? ' · ' + team : '') + mnt(min) + ' | ' + line(i) + tail(i),
      {tag: /YELLOW/.test(type) ? 'بطاقة حمراء · إنذار ثانٍ' : 'بطاقة حمراء', who: playerName(p), minute: min, side: club === i.homeId ? 'h' : club === i.awayId ? 'a' : ''}));
    else if (opt.penalties !== false && type === 'PENALTY') out.push(mk('penalty', i, 'ركلة جزاء! ' + i.H + ' × ' + i.A + mnt(min) + tail(i), {tag: 'ركلة جزاء', minute: min}));
    else if (opt.var !== false && type === 'VAR' && e.varInfo && e.varInfo.incident && e.varInfo.incident !== 'NONE' && !e.varInfo.isSilentCheck) {
      var inc = VAR_AR[e.varInfo.incident] || e.varInfo.incident, res = e.varInfo.result && e.varInfo.result !== 'NONE' ? ' · ' + (VAR_RESULT_AR[e.varInfo.result] || e.varInfo.result) : '';
      out.push(mk('var', i, 'مراجعة VAR: ' + inc + res + mnt(min) + ' | ' + i.H + ' × ' + i.A + tail(i), {tag: 'مراجعة VAR', who: inc + res, minute: min}));
    }
  });
  return out;
}

// ctx: {log, show(alert) -> promise, enabled() -> bool, config() -> CONFIG}
function start(ctx) {
  var queue = [], qTimer = null, state = {}, status = {last: 0, live: 0, errors: 0, source: 'uefa'}, fails = 0, down = false, cycle = 0;
  function pump() {
    if (qTimer || !queue.length) return;
    var a = queue.shift(); ctx.log('alert: ' + a.text);
    Promise.resolve(ctx.show(a)).catch(function (e) { ctx.log('alert not shown: ' + e); });
    qTimer = setTimeout(function () { qTimer = null; pump(); }, TOAST_GAP_MS);
  }
  function push(alerts) { alerts.forEach(function (a) { queue.push(a); }); if (queue.length > 10) queue.splice(0, queue.length - 10); pump(); }
  function cfg() { try { return ctx.config() || {}; } catch (e) { return {}; } }
  function fail(err, where) { status.errors++; fails++; if (fails === 1 || fails % 12 === 0) ctx.log('uefa ' + where + ': ' + err.message); if (fails >= DOWN_AFTER && !down) { down = true; ctx.log('uefa not answering (still trying)'); } }
  function ok() { fails = 0; if (down) { down = false; ctx.log('uefa back'); } }

  function run() {
    var al = cfg().ALERTS || {};
    if (!ctx.enabled() || al.enabled === false) return later(IDLE_MS);
    var comps = al.competitions || [1], lists = [], pending = comps.length, failed = false;
    comps.forEach(function (c) {
      getJson(BASE + '/matches?competitionId=' + c + '&status=LIVE&limit=30&offset=0', function (err, list) {
        if (err) failed = err; else lists = lists.concat(list || []);
        if (--pending === 0) done();
      });
    });
    function done() {
      if (failed) { fail(failed, 'live list'); return later(RETRY_MS / 6); }
      ok();
      try {
        var seen = {}; lists.forEach(function (m) { seen[String(m.id)] = m; });
        var texts = diffLive(state, lists, al);
        status.last = Date.now(); status.live = lists.length;
        // matches that were live a moment ago and are not any more: fetch the result once
        var left = Object.keys(state).filter(function (id) { return state[id].live && !seen[id]; });
        var jobs = left.length + (cycle++ % EVENTS_EVERY === 0 ? lists.length : 0), got = 0, finish = function () { if (++got >= jobs) after(); };
        if (!jobs) return after();
        left.forEach(function (id) {
          getJson(BASE + '/matches/' + id, function (e, m) {
            if (!e && m) { var t = finishedText(state, m, al); if (t) texts.push(t); } else state[id].live = true; // try again next time
            finish();
          });
        });
        if (jobs > left.length) lists.forEach(function (m) {
          getJson(BASE + '/matches/' + m.id + '/events?filter=ALL&offset=0&limit=12', function (e, evs) {
            if (!e && evs) { try { diffEvents(state, m, evs, al).forEach(function (t) { texts.push(t); }); } catch (x) { ctx.log('uefa events error: ' + x.message); } }
            finish();
          });
        });
        function after() { if (texts.length) push(texts); later(lists.length ? LIVE_MS : idleDelay()); }
      } catch (x) { ctx.log('uefa error: ' + (x && x.stack || x)); later(RETRY_MS); }
    }
  }
  // nothing live: sleep until two minutes before the next kick-off (at most 10 minutes)
  function idleDelay() { var d = IDLE_MS; nextKick(function (ms) { d = ms; }); return d; }
  var kickCache = {t: 0, at: 0};
  function nextKick(cb) {
    // synchronous answer from the last lookup; refresh it in the background for next time
    var want = (cfg().ALERTS || {}).competitions || [1];
    getJson(BASE + '/matches?competitionId=' + want[0] + '&status=UPCOMING&limit=1&offset=0&order=ASC', function (e, l) {
      if (!e && l && l[0] && l[0].kickOffTime) { kickCache.t = new Date(l[0].kickOffTime.dateTime).getTime(); kickCache.at = Date.now(); }
    });
    if (kickCache.t) { var ms = kickCache.t - Date.now() - 2 * 60 * 1000; cb(Math.min(IDLE_MS, Math.max(LIVE_MS, ms))); } else cb(IDLE_MS / 4);
  }
  function later(ms) { setTimeout(run, ms); }

  setTimeout(run, 6000);
  ctx.log('uefa alerts: competitions ' + (((cfg().ALERTS || {}).competitions) || [1]).join(','));
  return {status: function () { return status; }, test: function (a) { push([a]); }};
}

module.exports = {start: start, diffLive: diffLive, diffEvents: diffEvents, finishedText: finishedText, describe: describe};

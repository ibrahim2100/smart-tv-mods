// node tools/uefa-test.js : checks the UEFA alert logic against real recorded UEFA data (live list + event feed of a Champions League match).
var U0 = require('../service/uefa.js'), assert = require('assert'), clone = function (o) { return JSON.parse(JSON.stringify(o)); };
var txt = function (a) { return a.text; };   // the tests below look at the plain text of each alert; the card data is checked at the end
var U = {describe: U0.describe, diffLive: function () { return U0.diffLive.apply(null, arguments).map(txt); }, diffEvents: function () { return U0.diffEvents.apply(null, arguments).map(txt); },
  finishedText: function () { var a = U0.finishedText.apply(null, arguments); return a ? a.text : null; }};
var live = require('./uefa-sample-live.json'), events = require('./uefa-sample-events.json'), opt = {};
var m = clone(live[0]), state = {}, out;
console.log('match:', U.describe(m).H, 'x', U.describe(m).A, '| scorers:', U.describe(m).scorers.map(function (s) { return s.name + ' ' + s.minute + "'"; }).join(', '));

out = U.diffLive(state, [m], opt); assert.deepStrictEqual(out, [], 'first sight of a match already at 85 minutes: silent');
out = U.diffLive(state, [clone(m)], opt); assert.deepStrictEqual(out, [], 'no change: silent');

// a new goal for the home side
var g = clone(m); var base = g.playerEvents.scorers[0];
var fresh = clone(base); fresh.id = 'new-goal-1'; fresh.time = {minute: 88, second: 10}; fresh.teamId = String(g.homeTeam.id);
g.playerEvents.scorers.push(fresh); g.score.total.home += 1;
out = U.diffLive(state, [g], opt); assert.strictEqual(out.length, 1); console.log('GOAL   :', out[0]);
assert(/هدف!/.test(out[0]) && /88/.test(out[0]) && /5 - 0/.test(out[0]));
// the same goal again: silent
assert.deepStrictEqual(U.diffLive(state, [clone(g)], opt), []);
// own goal and penalty wording
var og = clone(g); var o = clone(base); o.id = 'og1'; o.goalType = 'OWN_GOAL'; o.teamId = String(g.homeTeam.id); o.time = {minute: 89, second: 0}; og.playerEvents.scorers.push(o); og.score.total.away += 1;
out = U.diffLive(state, [og], opt); console.log('OWN    :', out[0]); assert(/هدف عكسي/.test(out[0]) && /5 - 1/.test(out[0]));
var pg = clone(og); var p = clone(base); p.id = 'pen1'; p.goalType = 'PENALTY'; p.teamId = String(g.awayTeam.id); p.time = {minute: 90, second: 5}; pg.playerEvents.scorers.push(p); pg.score.total.away += 1;
out = U.diffLive(state, [pg], opt); console.log('PEN    :', out[0]); assert(/ركلة جزاء/.test(out[0]) && /5 - 2/.test(out[0]));
// goal taken back (VAR)
var cancelled = clone(pg); cancelled.playerEvents.scorers = cancelled.playerEvents.scorers.filter(function (s) { return s.id !== 'new-goal-1'; }); cancelled.score.total.home -= 1;
out = U.diffLive(state, [cancelled], opt); console.log('VARNO  :', out[0]); assert(/إلغاء هدف/.test(out[0]));
// half time and extra time
var ht = clone(cancelled); ht.phase = 'HALF_TIME_BREAK'; out = U.diffLive(state, [ht], opt); console.log('HALF   :', out[0]); assert(/استراحة/.test(out[0]));
var et = clone(ht); et.phase = 'EXTRA_TIME_FIRST_HALF'; out = U.diffLive(state, [et], opt); assert(/الوقت الإضافي/.test(out[0]));
// kick-off of a match we have not seen, at minute 1
var ko = clone(m); ko.id = 'ko1'; ko.phase = 'FIRST_HALF'; ko.minute = {normal: 1}; ko.score.total = {home: 0, away: 0}; ko.playerEvents.scorers = [];
out = U.diffLive(state, [ko], opt); console.log('KICKOFF:', out[0]); assert(/بدأت المباراة/.test(out[0]));
// full time: only once, only for a match that was live
var fin = clone(ko); fin.status = 'FINISHED'; var t1 = U.finishedText(state, fin, opt); console.log('FULL   :', t1); assert(/نهاية المباراة/.test(t1));
assert.strictEqual(U.finishedText(state, fin, opt), null, 'full time once');
assert.strictEqual(U.finishedText({}, fin, opt), null, 'never seen live: silent');
// events: baseline, then a red card, a penalty and a VAR check appear
var st2 = {}; U.diffLive(st2, [m], opt);
out = U.diffEvents(st2, m, events, opt); assert.deepStrictEqual(out, [], 'first look at the feed only remembers it');
var yc = events.filter(function (e) { return e.type === 'YELLOW_CARD'; })[0];
var red = clone(yc); red.id = 'red-1'; red.type = 'RED_CARD'; red.time = {minute: 86, second: 5};
var pen = clone(yc); pen.id = 'pen-1'; pen.type = 'PENALTY'; pen.time = {minute: 86, second: 40};
var vr = clone(yc); vr.id = 'var-1'; vr.type = 'VAR'; vr.varInfo = {incident: 'PENALTY', isSilentCheck: false, status: 'CHECKING', result: 'NONE', reason: 'NONE'};
var feed = [vr, pen, red].concat(events);
out = U.diffEvents(st2, m, feed, opt); out.forEach(function (t) { console.log('EVENT  :', t); }); assert.strictEqual(out.length, 3);
assert(/بطاقة حمراء/.test(out[0]) && /ركلة جزاء/.test(out[1]) && /VAR/.test(out[2]));
assert.deepStrictEqual(U.diffEvents(st2, m, feed, opt), [], 'same events again: silent');
var vrSame = clone(vr); vrSame.varInfo.status = 'IN_PROGRESS'; assert.deepStrictEqual(U.diffEvents(st2, m, [vrSame].concat(feed), opt), [], 'the same VAR check changing status is not announced again');
var vr2 = clone(vr); vr2.varInfo.status = 'DONE'; vr2.varInfo.result = 'CONFIRMED';
out = U.diffEvents(st2, m, [vr2].concat(feed), opt); console.log('VAR end:', out[0]); assert(/تم تأكيد/.test(out[0]));
var silent = clone(yc); silent.id = 'v-silent'; silent.type = 'VAR'; silent.varInfo = {incident: 'NONE', isSilentCheck: true, status: 'NONE', result: 'NONE'};
assert.deepStrictEqual(U.diffEvents(st2, m, [silent].concat(feed), opt), [], 'silent VAR check is not announced');
// switches
var st3 = {}; U.diffLive(st3, [m], {}); assert.deepStrictEqual(U.diffLive(st3, [g], {goals: false}), [], 'goals switched off');
// the data the overlay card draws
var sx = {}; U0.diffLive(sx, [m], {}); var card = U0.diffLive(sx, [g], {})[0];
assert.strictEqual(card.kind, 'goal'); assert.strictEqual(card.p.side, 'h'); assert.strictEqual(card.p.h, 5); assert.strictEqual(card.p.a, 0);
assert(card.p.who && card.p.minute === 88 && /^https:/.test(card.p.logoH) && /^https:/.test(card.p.logoA) && card.p.H === 'كومو', JSON.stringify(card.p));
console.log('card data:', JSON.stringify(card.p));
console.log('uefa logic: all checks pass');

// Small gzip-aware JSON fetch used by the alerts (10 s timeout, 3 MB cap).
'use strict';
var https = require('https');
var zlib = require('zlib');

function getJson(url, cb) {
  var done = false, finish = function (e, v) { if (!done) { done = true; cb(e, v); } };
  var req = https.get(url, {headers: {'Accept-Encoding': 'gzip', 'User-Agent': 'Mozilla/5.0 (compatible; Albrhi-TV)'}, timeout: 12000}, function (res) {
    if (res.statusCode !== 200) { res.resume(); return finish(new Error('HTTP ' + res.statusCode)); }
    var src = res.headers['content-encoding'] === 'gzip' ? res.pipe(zlib.createGunzip()) : res, chunks = [], size = 0;
    src.on('data', function (c) { size += c.length; if (size > 3 << 20) { req.abort(); return finish(new Error('too big')); } chunks.push(c); });
    src.on('end', function () { try { finish(null, JSON.parse(Buffer.concat(chunks).toString('utf8'))); } catch (e) { finish(e); } });
    src.on('error', finish);
  });
  req.on('error', finish);
  req.on('timeout', function () { req.abort(); finish(new Error('timeout')); });
}

module.exports = {getJson: getJson};

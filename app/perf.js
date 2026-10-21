// Dev-only benchmark. Runs only when the app is launched with params {"perf": "<computer ip>"} (or ?perf=host in a browser).
// It presses the arrow keys by itself, measures frame times and key-to-paint latency, repeats with individual effects
// switched off, and sends the numbers to http://<host>:9100/ as image requests (no CORS needed).
(function () {
  var lp = {}; try { lp = JSON.parse((window.PalmSystem && window.PalmSystem.launchParams) || '{}'); } catch (e) { /* none */ }
  var m = /[?&]perf=([^&]+)/.exec(location.search), host = lp.perf || (m && m[1]);
  if (!host) return;
  var variants = lp.variants || ['base'];
  var css = {
    nohalo: '.halo{display:none!important}',
    
    noshadow: '.halo{display:none!important}#hero::after{display:none!important}',
    notransitions: '*{transition:none!important;animation:none!important}',
    all: '.halo,.glow,#hero::after{display:none!important}'
  };
  var styleEl = document.createElement('style'); document.head.appendChild(styleEl);
  var seq = 0;
  function send(o) { o.n = ++seq; new Image().src = 'http://' + host + ':9100/r?d=' + encodeURIComponent(JSON.stringify(o)) + '&_=' + Date.now(); }
  function key(k) { document.dispatchEvent(new KeyboardEvent('keydown', {key: k, bubbles: true, cancelable: true})); }
  function wait(ms) { return new Promise(function (r) { setTimeout(r, ms); }); }

  var frames = [], last = 0, on = false;
  (function loop(t) { if (on && last) frames.push(t - last); last = t; requestAnimationFrame(loop); })(0);
  function pct(a, p) { var s = a.slice().sort(function (x, y) { return x - y; }); return s.length ? s[Math.min(s.length - 1, Math.floor(s.length * p))] : 0; }
  function avg(a) { return a.length ? a.reduce(function (x, y) { return x + y; }, 0) / a.length : 0; }
  function r1(x) { return Math.round(x * 10) / 10; }

  var lat = [], cost = [];
  function press(k) { // sync handler cost + time until the second frame after the key (≈ painted)
    var t0 = performance.now(); key(k); cost.push(performance.now() - t0);
    requestAnimationFrame(function () { requestAnimationFrame(function () { lat.push(performance.now() - t0); }); });
  }
  function phase(name, run) {
    frames = []; lat = []; cost = []; on = true; last = 0;
    return run().then(function () { return wait(400); }).then(function () {
      on = false;
      var f = frames.slice(), sum = f.reduce(function (x, y) { return x + y; }, 0);
      return {p: name, fps: r1(f.length / (sum / 1000)), avg: r1(avg(f)), p95: r1(pct(f, .95)), max: r1(Math.max.apply(null, f.concat([0]))),
        o33: f.filter(function (x) { return x > 33.5; }).length, o50: f.filter(function (x) { return x > 50; }).length, n: f.length,
        lat: r1(avg(lat)), latMax: r1(Math.max.apply(null, lat.concat([0]))), cost: r1(avg(cost)), costMax: r1(Math.max.apply(null, cost.concat([0])))};
    });
  }
  function steps(k, n, gap) { var p = Promise.resolve(); for (var i = 0; i < n; i++) p = p.then(function () { press(k); return wait(gap); }); return p; }
  function runVariant(v) {
    styleEl.textContent = css[v] || '';
    var out = {v: v, res: []};
    return wait(600)
      .then(function () { return phase('idle', function () { return wait(3000); }); }).then(function (r) { out.res.push(r); })
      .then(function () { return phase('row', function () { return steps('ArrowRight', 10, 350).then(function () { return steps('ArrowLeft', 10, 350); }); }); }).then(function (r) { out.res.push(r); })
      .then(function () { return phase('vert', function () { return steps('ArrowUp', 1, 450).then(function () { return steps('ArrowDown', 1, 450); }).then(function () { return steps('ArrowDown', 1, 450); }).then(function () { return steps('ArrowUp', 1, 450); }); }); }).then(function (r) { out.res.push(r); })
      .then(function () { return phase('fast', function () { return steps('ArrowRight', 12, 120).then(function () { return steps('ArrowLeft', 12, 120); }); }); }).then(function (r) { out.res.push(r); })
      .then(function () { send(out); });
  }
  function start() {
    var mem = performance.memory ? {used: Math.round(performance.memory.usedJSHeapSize / 1048576), total: Math.round(performance.memory.totalJSHeapSize / 1048576)} : null;
    send({hello: 1, ua: navigator.userAgent, dpr: window.devicePixelRatio, marks: window.__marks || null, mem: mem, w: innerWidth, h: innerHeight});
    var p = Promise.resolve();
    variants.forEach(function (v) { p = p.then(function () { return runVariant(v); }); });
    p.then(function () { styleEl.textContent = ''; send({done: 1}); });
  }
  var iv = setInterval(function () { if (!document.getElementById('boot')) { clearInterval(iv); setTimeout(start, 5000); } }, 300);
})();

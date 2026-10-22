/*
 * xhs_home_sniff_passive.js — onPageStarted 阶段的被动探针（隔离实验用）
 * 与 xhs_home_sniff.js 的差别：**不 hook fetch/XHR、不加桥**，只
 *   1) 捕获页面自身报错（定位水合崩溃）  2) 定时读 __INITIAL_STATE__ 收割。
 * 若此版本能让页面正常水合（state 出现真实 noteId），则证明 hook 注入破坏水合。
 */
(function () {
  if (window.__xhsHomePassive) return;
  window.__xhsHomePassive = true;

  function log(msg) {
    try {
      var s = String(msg).slice(0, 200);
      console.log('[xhsPassive] ' + s);
      if (window.AndroidVideoBridge && window.AndroidVideoBridge.onHomepageSniffLog) window.AndroidVideoBridge.onHomepageSniffLog(s);
    } catch (e) {}
  }

  // 页面报错捕获（注入时机早于页面脚本 → 能接到水合崩溃的第一现场）
  window.__xhsErrs = [];
  window.addEventListener('error', function (e) {
    try {
      window.__xhsErrs.push(String(e.message) + ' @ ' + String(e.filename || '').slice(-60) + ':' + e.lineno + ':' + e.colno);
    } catch (e0) {}
  });
  window.addEventListener('unhandledrejection', function (e) {
    try { window.__xhsErrs.push('promise: ' + String(e.reason).slice(0, 150)); } catch (e1) {}
  });

  function unwrap(v) {
    try {
      if (v && typeof v === 'object') {
        var ks = Object.keys(v);
        if (ks.indexOf('dep') >= 0 && ks.indexOf('_value') >= 0) return v._value;
      }
    } catch (e) {}
    return v;
  }

  function probe(tag) {
    try {
      var s = window.__INITIAL_STATE__;
      var user = s && s.user;
      log(tag + ' stateKeys=' + (s ? Object.keys(s).slice(0, 12).join(',') : 'none'));
      if (tag === 'p15' && s) {
        try {
          var nd = unwrap(s.noteData);
          log(tag + ' nDkeys=' + (nd && typeof nd === 'object' ? Object.keys(nd).slice(0, 12).join(',') : typeof nd));
          try { log(tag + ' nDdata=' + JSON.stringify(unwrap(nd && nd.data)).slice(0, 700)); } catch (e8) { log(tag + ' nDdata_err:' + e8.message); }
          try { log(tag + ' nDcoll=' + JSON.stringify(unwrap(nd && nd.collectionData)).slice(0, 700)); } catch (e9) { log(tag + ' nDcoll_err:' + e9.message); }
        } catch (e6) { log(tag + ' nD_err:' + e6.message); }
        try {
          var pf = unwrap(s.profile);
          log(tag + ' pf=' + JSON.stringify(pf).slice(0, 500));
        } catch (e7) { log(tag + ' pf_err:' + e7.message); }
        // 杀手锏①：内联 script JSON 岛定位（标题文本是特征）
        try {
          var sc = document.scripts, found = 0;
          for (var si = 0; si < sc.length && found < 3; si++) {
            var tx = sc[si].textContent || '';
            if (tx.indexOf('美人如玉') >= 0 || tx.indexOf('noteId') >= 0) {
              var at = tx.indexOf('美人如玉');
              if (at < 0) at = tx.indexOf('noteId');
              log(tag + ' island' + found + ' len=' + tx.length + ' @' + tx.slice(Math.max(0, at - 120), at + 120).replace(/\s+/g, ' '));
              log(tag + ' isHead=' + tx.slice(0, 350).replace(/\s+/g, ' '));
              var xt = tx.indexOf('xsec');
              log(tag + ' isXsec=' + (xt >= 0 ? tx.slice(Math.max(0, xt - 40), xt + 120).replace(/\s+/g, ' ') : 'NONE'));
              var it = tx.indexOf('"id"');
              log(tag + ' isItem=' + tx.slice(Math.max(0, it - 250), it + 250).replace(/\s+/g, ' '));
              found++;
            }
          }
          log(tag + ' scripts=' + sc.length + ' islands=' + found);
        } catch (e10) { log(tag + ' island_err:' + e10.message); }
        // （点击实验已移除：会触发跳转/假弹窗，干扰收割）
      }
      if (tag === 'p30') {
        log(tag + ' routes=' + JSON.stringify(window.__xhsRoutes || []).slice(0, 300) + ' href=' + location.href.slice(0, 150));
      }
      if (user) {
        var notesMap = unwrap(user.notes);
        var total = 0, withId = 0, firstId = '';
        if (notesMap && typeof notesMap === 'object') {
          for (var k in notesMap) {
            var arr = notesMap[k];
            if (arr && typeof arr.length === 'number') {
              for (var i = 0; i < arr.length; i++) {
                total++;
                var n = arr[i] || {};
                var id = n.id || (n.noteCard && n.noteCard.noteId) || '';
                if (id) { withId++; if (!firstId) firstId = String(id); }
              }
            }
          }
        }
        var errs = (window.__xhsErrs || []).slice(0, 3).join(' || ');
        log(tag + ' probe total=' + total + ' withId=' + withId + ' firstId=' + firstId.slice(0, 26) + ' errs=' + errs);
      } else {
        log(tag + ' errs=' + (window.__xhsErrs || []).slice(0, 2).join(' || ').slice(0, 150));
      }
      if (tag !== 'p30') return;
      // p30 专项：移动 H5 卡片 DOM 全文 + performance 时间线
      try {
        var card = document.querySelector('section.reds-note-card') || document.querySelector('section.note-item') || document.querySelector('section');
        if (card) {
          var h = card.outerHTML;
          var ids24 = h.match(/[0-9a-f]{24}/g) || [];
          var attrs = [];
          for (var ai = 0; ai < card.attributes.length; ai++) attrs.push(card.attributes[ai].name);
          log(tag + ' cardAttrs=' + attrs.join(',') + ' ids24=' + JSON.stringify(ids24.slice(0, 5)));
          var mm, ctx = [];
          var rx = /[0-9a-f]{24}/g;
          while ((mm = rx.exec(h)) && ctx.length < 3) {
            ctx.push(h.slice(Math.max(0, mm.index - 60), mm.index + 30).replace(/\s+/g, ' '));
          }
          for (var ci = 0; ci < ctx.length; ci++) log(tag + ' ctx' + ci + '=' + ctx[ci]);
          log(tag + ' cardRest=' + h.replace(/id="[^"]*"/, 'id=""').replace(/\s+/g, ' ').slice(0, 650));
        }
      } catch (e3) {}
      try {
        var res = performance.getEntriesByType('resource') || [];
        var api = [], zero = [], scripts = 0;
        for (var r = 0; r < res.length; r++) {
          var nm = res[r].name || '';
          if (/\/api\//.test(nm)) api.push(nm.slice(nm.indexOf('/api/'), (nm.indexOf('/api/') + 60)));
          if (/\.js/.test(nm)) scripts++;
          if (res[r].transferSize === 0 && res[r].decodedBodySize === 0 && !/^data:/.test(nm)) {
            zero.push(nm.slice(-70));
          }
        }
        log(tag + ' perf total=' + res.length + ' scripts=' + scripts + ' apiCalls=' + api.length);
        for (var a = 0; a < api.length && a < 5; a++) log(tag + ' API ' + api[a]);
        for (var z = 0; z < zero.length && z < 8; z++) log(tag + ' ZERO ' + zero[z]);
      } catch (e2) { log(tag + ' perf_err:' + e2.message); }
    } catch (e) { log(tag + '_err:' + e.message); }
  }
  setTimeout(function () { probe('p6'); }, 6000);
  setTimeout(function () { probe('p15'); }, 15000);
  setTimeout(function () { probe('p30'); }, 30000);

  log('passive installed');
})();

/*
 * 小红书作者主页 · 作品列表响应嗅探（L2x-1）
 * ------------------------------------------------------------------
 * 背景：小红书 Web 接口要求 `x-s` / `x-t` / `x-s-common` 签名，只能由页面
 *   xhs.js 产出（与抖音 secsdk 同理，纯 HTTP 自建请求必被拒）。因此只旁观
 *   页面自己发出的请求，把响应体裁剪后经 AndroidVideoBridge 回传原生。
 *
 * 嗅探目标：
 *   1) GET /api/sns/web/v1/user_posted?num=30&cursor=…&user_id=…
 *      响应 data.notes[] —— 每条 {id, xsec_token, note_card:{type, display_title, cover}}
 *      ★列表**不带媒体直链**，只有封面/标题/token；详情由原生侧逐条 explore 页获取。
 *      cursor 翻页由页面滚动触发 → 逐页累积，按 noteId 去重。
 *   2) GET /api/sns/web/v1/user/other（或 selfinfo）→ data.nickname / data.notes（作品总数）
 *
 * 安全性：全链路只读。fetch/XHR 均为旁路监听，任何异常吞掉，不影响页面自身逻辑。
 */
(function () {
  if (window.__xhsHomeSniff) return;
  window.__xhsHomeSniff = true;

  var POSTED_RE = /\/api\/sns\/web\/v1\/user_posted/;
  var USER_RE = /\/api\/sns\/web\/v1\/user\/(?:other|selfinfo)/;
  var profileRelayed = false;
  var MAX_PER_CHUNK = 30;
  var seen = {};   // noteId 去重（跨 cursor 翻页）

  function log(msg) {
    try {
      var s = String(msg).slice(0, 200);
      console.log('[xhsSniff] ' + s);
      if (window.AndroidVideoBridge && window.AndroidVideoBridge.onHomepageSniffLog) window.AndroidVideoBridge.onHomepageSniffLog(s);
    } catch (e) {}
  }

  var stats = { fetchSeen: 0, xhrSeen: 0, postedHits: 0, relayed: 0, cursorPages: 0 };

  /** 裁剪单条笔记 → 原生侧 XhsPostItem 需要的最小结构 */
  function trimNote(n) {
    if (!n) return null;
    var card = n.note_card || n;
    var id = n.id || n.note_id || card.note_id;
    if (!id) return null;
    var cover = '';
    try {
      var c = card.cover || n.cover || {};
      cover = c.url_pre || c.url_default || '';
      if (!cover && c.info_list && c.info_list.length) {
        var best = '';
        for (var i = 0; i < c.info_list.length; i++) {
          var u = (c.info_list[i] && c.info_list[i].url) || '';
          if (u.length > best.length) best = u;
        }
        cover = best;
      }
    } catch (e) {}
    return {
      id: String(id),
      xsec_token: n.xsec_token || '',
      title: (card.display_title || '').slice(0, 120),
      type: (card.type === 'video') ? 'video' : 'normal',
      cover: cover
    };
  }

  function relayPosted(bodyText) {
    if (!bodyText || bodyText.charAt(0) !== '{') { log('relay_skip head=' + String(bodyText || '').slice(0, 40)); return; }
    var j;
    try { j = JSON.parse(bodyText); } catch (e) { log('json_err:' + e.message); return; }
    var d = j.data || {};
    var list = d.notes || [];
    if (!list.length) { log('posted_empty code=' + j.code); return; }
    stats.cursorPages++;

    var kept = [];
    for (var i = 0; i < list.length; i++) {
      var t = trimNote(list[i]);
      if (t && !seen[t.id]) { seen[t.id] = 1; kept.push(t); }
    }
    if (!kept.length) return;

    for (var s = 0; s < kept.length; s += MAX_PER_CHUNK) {
      var chunk = kept.slice(s, s + MAX_PER_CHUNK);
      var payload = {
        notes: chunk,
        has_more: !!(d.has_more),
        cursor: d.cursor || '',
        total: d.total || 0,
        page: stats.cursorPages
      };
      try {
        if (window.AndroidVideoBridge && window.AndroidVideoBridge.onXhsPosted) {
          window.AndroidVideoBridge.onXhsPosted(JSON.stringify(payload));
          stats.relayed += chunk.length;
        }
      } catch (e) { log('bridge_err:' + e.message); }
    }
    log('posted page=' + stats.cursorPages + ' new=' + kept.length + ' relayed=' + stats.relayed);
  }

  function relayProfile(bodyText) {
    if (profileRelayed) return;
    if (!bodyText || bodyText.charAt(0) !== '{') return;
    var j;
    try { j = JSON.parse(bodyText); } catch (e) { return; }
    var d = j.data || {};
    if (!d.nickname && !d.notes) return;
    profileRelayed = true;
    try {
      if (window.AndroidVideoBridge && window.AndroidVideoBridge.onXhsProfile) {
        window.AndroidVideoBridge.onXhsProfile(JSON.stringify({
          nickname: d.nickname || '',
          total: d.notes || 0
        }));
      }
    } catch (e) {}
    log('profile nickname=' + (d.nickname || '') + ' total=' + (d.notes || 0));
  }

  // ---- fetch 旁路 hook ----
  var _fetch = window.fetch;
  if (_fetch) {
    window.fetch = function (input) {
      var u = '';
      try { u = (typeof input === 'string') ? input : (input && input.url) || ''; } catch (e) {}
      if (POSTED_RE.test(u)) {
        stats.fetchSeen++; stats.postedHits++;
        log('fetch hit ' + u.slice(0, 120));
        return _fetch.apply(this, arguments).then(function (res) {
          try { res.clone().text().then(function (t) { relayPosted(t); }).catch(function (e) {}); } catch (e) {}
          return res;
        });
      }
      if (USER_RE.test(u)) {
        stats.fetchSeen++;
        return _fetch.apply(this, arguments).then(function (res) {
          try { res.clone().text().then(function (t) { relayProfile(t); }).catch(function (e) {}); } catch (e) {}
          return res;
        });
      }
      return _fetch.apply(this, arguments);
    };
  }

  // ---- XHR 旁路 hook ----
  var _open = XMLHttpRequest.prototype.open;
  XMLHttpRequest.prototype.open = function (m, u) {
    try { this.__xhsUrl = String(u || ''); } catch (e) {}
    return _open.apply(this, arguments);
  };
  var _send = XMLHttpRequest.prototype.send;
  XMLHttpRequest.prototype.send = function () {
    var xhr = this;
    try {
      var u = xhr.__xhsUrl || '';
      if (POSTED_RE.test(u)) {
        stats.xhrSeen++; stats.postedHits++;
        log('xhr hit ' + u.slice(0, 120));
        xhr.addEventListener('load', function () {
          try { relayPosted(xhr.responseText); } catch (e) {}
        });
      } else if (USER_RE.test(u)) {
        stats.xhrSeen++;
        xhr.addEventListener('load', function () {
          try { relayProfile(xhr.responseText); } catch (e) {}
        });
      }
    } catch (e) {}
    return _send.apply(this, arguments);
  };

  // ---- 尝试自动滚动触发 cursor 翻页（未登录会被服务端截断，无害）----
  setTimeout(function () {
    try {
      log('autoscroll t=4s dom=' + document.querySelectorAll('section').length);
      window.scrollTo(0, document.body.scrollHeight);
      setTimeout(function () { try { window.scrollTo(0, document.body.scrollHeight); } catch (e) {} }, 3000);
    } catch (e) {}
  }, 4000);

  log('installed');
})();

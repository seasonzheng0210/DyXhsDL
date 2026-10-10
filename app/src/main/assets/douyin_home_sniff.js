/*
 * 抖音作者主页 · 作品列表响应嗅探（L2a）
 * ------------------------------------------------------------------
 * 背景（docs/design/主页下载-全局重构方案-v3.md §1.6 对照实验）：
 *   抖音 post 接口 `/aweme/v1/web/aweme/post/` 除 a_bogus 外，还要求
 *   `uifid` / `x-secsdk-web-signature` / `verifyFp` / `webid` 等**只能由页面
 *   secsdk JS 产出**的参数（实测浏览器发 45 个参数，App 自建仅约 24 个，
 *   缺 uifid → 服务端报 "Uifid Not Found"）。纯 HTTP + 自算签名已不可行。
 *
 * 做法：不重算任何签名，**只旁观**页面自己发出的 post 请求，把响应体里
 *   我们需要的字段裁剪后经 AndroidVideoBridge 回传原生。
 *
 * 为什么裁剪而不回传整个响应体：
 *   原始响应约 1.0 MB；裁剪后仅保留直链相关字段，约 15~20 KB，
 *   既避免大字符串跨 JS↔Java 桥的开销，也让原生侧可直接复用
 *   DouyinParser.parsePostListJson（裁剪后结构与其入参完全兼容）。
 *
 * 安全性：全链路只读。fetch/XHR 均用「旁路监听」而非替换返回值，
 *   任何异常都被吞掉，绝不影响页面自身逻辑。
 */
(function () {
  if (window.__dyHomeSniff) return;
  window.__dyHomeSniff = true;

  var POST_RE = /\/aweme\/v1\/web\/aweme\/post\//;
  // 作者作品总数（author.aweme_count）**不在** post 响应里，只在 user/profile/other 的
  // user.aweme_count（实测匿名 21 条上限时该值为 253）→ 页面会并行发这个请求，顺带抓它。
  var PROFILE_RE = /\/aweme\/v1\/web\/user\/profile\/other\//;
  var profileRelayed = false;
  var MAX_PER_CHUNK = 20;   // 单次回传条目上限（约 15KB，远低于桥的舒适区）

  function log(msg) {
    try {
      var s = String(msg).slice(0, 200);
      console.log('[dySniff] ' + s);
      if (window.AndroidVideoBridge && window.AndroidVideoBridge.onHomepageSniffLog) window.AndroidVideoBridge.onHomepageSniffLog(s);
    } catch (e) {}
  }

  var stats = { fetchSeen: 0, xhrSeen: 0, awemeHits: 0, postHits: 0, relayed: 0 };
  var hosts = {};
  var postUrlFull = '';

  function noteHost(u) {
    try {
      var m = /^https?:\/\/([^\/]+)/.exec(u);
      if (m) hosts[m[1]] = (hosts[m[1]] || 0) + 1;
    } catch (e) {}
  }

  /** 定期上报页面状态，用于判断「页面是否真的渲染并发出数据请求」 */
  function reportState(tag) {
    try {
      var anchors = document.querySelectorAll('a[href*="/video/"], a[href*="/note/"]').length;
      var imgs = document.querySelectorAll('img').length;
      var txt = (document.body && document.body.innerText) || '';
      log(tag + ' dom: anchors=' + anchors + ' imgs=' + imgs + ' textLen=' + txt.length +
        ' | fetch=' + stats.fetchSeen + ' xhr=' + stats.xhrSeen + ' aweme=' + stats.awemeHits + ' post=' + stats.postHits +
        ' relayed=' + stats.relayed);
      if (tag === 't3s') {
        // secsdk 相关 cookie 是否存在（bd_ticket_guard_client_data 缺 → 页面签名不完整）
        var ck = (document.cookie || '');
        log('cookieNames=' + ck.split(';').map(function (s) { return s.split('=')[0].trim(); }).join(','));
        var hs = Object.keys(hosts).map(function (h) { return h + ':' + hosts[h]; }).join(' ');
        log('hosts=' + hs);
        // 环境取证：判断 secsdk 是否因「非浏览器环境」拒绝初始化
        var env = [];
        try { env.push('ua=' + navigator.userAgent.slice(0, 60)); } catch (e) {}
        try { env.push('webdriver=' + navigator.webdriver); } catch (e) {}
        try { env.push('plugins=' + (navigator.plugins ? navigator.plugins.length : -1)); } catch (e) {}
        try { env.push('subtle=' + (!!(window.crypto && window.crypto.subtle))); } catch (e) {}
        try { env.push('chrome=' + (!!window.chrome)); } catch (e) {}
        try { env.push('bridge=' + (typeof window.AndroidVideoBridge)); } catch (e) {}
        try { env.push('sniffGlobal=' + (typeof window.__dyHomeSniff)); } catch (e) {}
        try { env.push('injectGlobal=' + (typeof window.__dyInject)); } catch (e) {}
        try { env.push('webgl=' + (function () { var c = document.createElement('canvas'); var g = c.getContext('webgl'); return g ? 'yes' : 'no'; })()); } catch (e) {}
        log('env ' + env.join(' | '));
      }
      if (tag === 't4s') {
        var u2 = postUrlFull || '';
        log('postQuery flags: uifid=' + (u2.indexOf('uifid=') >= 0) +
          ' secsdkSig=' + (u2.indexOf('x-secsdk-web-signature') >= 0) +
          ' a_bogus=' + (u2.indexOf('a_bogus=') >= 0) +
          ' webid=' + (u2.indexOf('webid=') >= 0) +
          ' msToken=' + (u2.indexOf('msToken=') >= 0) +
          ' verifyFp=' + (u2.indexOf('verifyFp=') >= 0) +
          ' len=' + u2.length);
      }
    } catch (e) {}
  }

  /** 把单条 aweme 裁剪成 parsePostListJson 认识的最小结构 */
  function trimItem(it) {
    if (!it || !it.aweme_id) return null;
    var out = {
      aweme_id: it.aweme_id,
      desc: it.desc || '',
      create_time: it.create_time || 0
    };
    // 作者：昵称 + 作品总数（用于「共 N 条」提示）
    if (it.author) {
      out.author = {
        nickname: it.author.nickname || '',
        aweme_count: it.author.aweme_count || 0
      };
    }
    // 图集：images[].url_list / cover_url_list
    if (it.images && it.images.length) {
      var imgs = [];
      for (var i = 0; i < it.images.length; i++) {
        var im = it.images[i] || {};
        var u = (im.url_list && im.url_list[0]) || '';
        var c = (im.cover_url_list && im.cover_url_list[0]) || '';
        if (u || c) imgs.push({ url_list: u ? [u] : [], cover_url_list: c ? [c] : [] });
      }
      if (imgs.length) out.images = imgs;
    }
    // 视频：play_addr.url_list（直链）+ cover.url_list
    if (it.video) {
      var v = {};
      var pa = it.video.play_addr;
      var pu = pa && pa.url_list && pa.url_list[0];
      if (pu) v.play_addr = { url_list: [pu] };
      var cv = it.video.cover;
      var cu = cv && cv.url_list && cv.url_list[0];
      if (cu) v.cover = { url_list: [cu] };
      if (v.play_addr || v.cover) out.video = v;
    }
    return out;
  }

  function relay(bodyText) {
    if (!bodyText || bodyText.charAt(0) !== '{') { log('relay_skip head=' + String(bodyText || '').slice(0, 40)); return; }
    var j;
    try { j = JSON.parse(bodyText); } catch (e) { log('json_err:' + e.message); return; }
    var list = j.aweme_list || [];
    if (!list.length) return;

    var kept = [];
    for (var i = 0; i < list.length; i++) {
      var t = trimItem(list[i]);
      if (t) kept.push(t);
    }
    if (!kept.length) return;

    // 分片回传（正常 21 条 → 1~2 片）
    for (var s = 0; s < kept.length; s += MAX_PER_CHUNK) {
      var chunk = kept.slice(s, s + MAX_PER_CHUNK);
      var payload = {
        status_code: 0,
        has_more: j.has_more === undefined ? 0 : j.has_more,
        max_cursor: j.max_cursor || 0,
        aweme_list: chunk
      };
      try {
        if (window.AndroidVideoBridge && window.AndroidVideoBridge.onHomepagePage) {
          window.AndroidVideoBridge.onHomepagePage(JSON.stringify(payload));
          stats.relayed += chunk.length;
        }
      } catch (e) { log('bridge_err:' + e.message); }
    }
    log('relayed items=' + kept.length + ' has_more=' + (j.has_more === 1 ? 1 : 0));
  }

  /** 回传作者档案（昵称 + 作品总数）。post 响应里没有 aweme_count，只能从这里取。 */
  function relayProfile(bodyText) {
    if (profileRelayed || !bodyText) return;
    var j;
    try { j = JSON.parse(bodyText); } catch (e) { return; }
    var u = j && j.user;
    if (!u) return;
    profileRelayed = true;
    try {
      if (window.AndroidVideoBridge && window.AndroidVideoBridge.onHomepageProfile) {
        window.AndroidVideoBridge.onHomepageProfile(JSON.stringify({
          nickname: u.nickname || '',
          aweme_count: u.aweme_count || 0
        }));
        log('profile_relayed aweme_count=' + (u.aweme_count || 0) + ' nickname=' + (u.nickname || ''));
      }
    } catch (e) { log('profile_bridge_err:' + e.message); }
  }

  /** 统一处理「命中哪个接口 → 走哪条回传路径」 */
  function dispatch(url, bodyText) {
    if (POST_RE.test(url)) relay(bodyText);
    else if (PROFILE_RE.test(url)) relayProfile(bodyText);
  }

  // ---- 旁路监听 fetch（不改写返回值）----
  try {
    var origFetch = window.fetch;
    if (typeof origFetch === 'function') {
      window.fetch = function () {
        var args = arguments;
        var url = '';
        try { url = typeof args[0] === 'string' ? args[0] : (args[0] && args[0].url) || ''; } catch (e) {}
        url = String(url);
        stats.fetchSeen++;
        noteHost(url);
        if (url.indexOf('/aweme/') >= 0) { stats.awemeHits++; log('fetch aweme: ' + url.slice(0, 110)); }
        if (POST_RE.test(url)) { stats.postHits++; if (!postUrlFull) postUrlFull = url; }
        var wantBody = POST_RE.test(url) || PROFILE_RE.test(url);
        var p = origFetch.apply(this, args);
        try {
          if (wantBody && p && typeof p.then === 'function') {
            p.then(function (resp) {
              try {
                if (!resp || typeof resp.clone !== 'function') return;
                resp.clone().text().then(function (t) { dispatch(url, t); }, function (e) { log('clone_text_err:' + e.message); });
              } catch (e) { log('clone_err:' + e.message); }
            }, function () {});
          }
        } catch (e) {}
        return p;
      };
    }
  } catch (e) { log('fetch_hook_err:' + e.message); }

  // ---- 旁路监听 XMLHttpRequest（不改写返回值）----
  try {
    var oOpen = XMLHttpRequest.prototype.open;
    var oSend = XMLHttpRequest.prototype.send;
    XMLHttpRequest.prototype.open = function (m, u) {
      try { this.__dySniffUrl = u; } catch (e) {}
      return oOpen.apply(this, arguments);
    };
    XMLHttpRequest.prototype.send = function () {
      try {
        var self = this;
        var u = String(self.__dySniffUrl || '');
        stats.xhrSeen++;
        noteHost(u);
        if (u.indexOf('/aweme/') >= 0) { stats.awemeHits++; log('xhr aweme: ' + u.slice(0, 110)); }
        if (POST_RE.test(u)) { stats.postHits++; if (!postUrlFull) postUrlFull = u; }
        self.addEventListener('load', function () {
          try {
            var wantBody = POST_RE.test(u) || PROFILE_RE.test(u);
            if (!wantBody) return;
            var st = 0; try { st = self.status; } catch (e) {}
            // 兼容 responseType=json：此时 responseText 会抛错，改用 response 对象
            var body = null;
            try { if (self.responseType === 'json' && self.response) body = JSON.stringify(self.response); } catch (e) {}
            if (!body) { try { body = self.responseText; } catch (e) { log('resp_text_err status=' + st + ' ' + e.message); return; } }
            if (POST_RE.test(u)) log('post_resp status=' + st + ' len=' + (body ? body.length : 0));
            dispatch(u, body);
          } catch (e) { log('xhr_load_err:' + e.message); }
        });
      } catch (e) {}
      return oSend.apply(this, arguments);
    };
  } catch (e) { log('xhr_hook_err:' + e.message); }

  log('installed ' + location.href.slice(0, 90));
  // 定时上报页面状态（3s / 8s），用于判断页面是否真的发出数据请求（排障用）
  [3000, 8000].forEach(function (ms, i) {
    setTimeout(function () { reportState(i === 0 ? 't3s' : 't8s'); }, ms);
  });
})();

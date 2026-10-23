/*
 * 小红书作者主页 · 作品列表响应嗅探（L2x-1）
 * ------------------------------------------------------------------
 * 背景：小红书 Web 接口要求 `x-s` / `x-t` / `x-s-common` 签名，只能由页面
 *   xhs.js 产出（与抖音 secsdk 同理，纯 HTTP 自建请求必被拒）。因此只旁观
 *   页面自己发出的请求，把响应体裁剪后经 AndroidVideoBridge 回传原生。
 *
 * 嗅探目标：
 *   1) SSR 首屏（主路径）：主页是服务端渲染，window.__INITIAL_STATE__ 里就有
 *      首屏笔记 —— state.user.notes(ref→_value) 是按 tab 分组的 map
 *      {tabIdx: [{id, noteCard, xsecToken, ...}, ...]}；昵称/总数在
 *      state.user.userPageData(ref→_value).basicInfo.nickname / posted.count。
 *   2) GET /api/sns/web/v1/user_posted?num=30&cursor=…（滚动翻页时）
 *      响应 data.notes[] —— 每条 {id, xsec_token, note_card:{type, display_title, cover}}
 *      ★列表**不带媒体直链**，只有封面/标题/token；详情由原生侧逐条 explore 页获取。
 *   3) GET /api/sns/web/v1/user/other（或 selfinfo）→ data.nickname / data.notes
 *
 * 安全性：全链路只读。fetch/XHR 均为旁路监听，任何异常吞掉，不影响页面自身逻辑。
 */
(function () {
  if (window.__xhsHomeSniff) return;
  window.__xhsHomeSniff = true;

  var POSTED_RE = /\/api\/sns\/web\/v1\/user_posted/;
  var USER_RE = /\/api\/sns\/web\/v1\/user\/(?:other|selfinfo)/;
  var FEED_RE = /\/api\/sns\/web\/v1\/feed/;   // 笔笔详情 API（SPA 点卡片触发）
  var profileRelayed = false;
  var MAX_PER_CHUNK = 30;
  var seen = {};   // noteId 去重（跨 cursor 翻页 / SSR）
  var mediaMap = {};  // noteId → {videoUrls:[], imageUrls:[]}（SPA 点卡片截获）

  function log(msg) {
    try {
      var s = String(msg).slice(0, 200);
      console.log('[xhsSniff] ' + s);
      if (window.AndroidVideoBridge && window.AndroidVideoBridge.onHomepageSniffLog) window.AndroidVideoBridge.onHomepageSniffLog(s);
    } catch (e) {}
  }

  var stats = { fetchSeen: 0, xhrSeen: 0, postedHits: 0, relayed: 0, cursorPages: 0 };

  /** 裁剪单条笔记 → 原生侧 XhsPostItem 需要的最小结构（兼容 XHR 蛇形与 SSR 驼峰字段） */
  function trimNote(n) {
    if (!n) return null;
    // 已格式化的 mapped item（island 路径）：cover 已是 URL 字符串，直接透传
    if (typeof n.cover === 'string' && n.id && (n.title !== undefined || n.type !== undefined)) {
      return {
        id: String(n.id),
        xsec_token: n.xsec_token || '',
        title: String(n.title || '').slice(0, 120),
        type: n.type === 'video' ? 'video' : 'normal',
        cover: n.cover
      };
    }
    var card = n.note_card || n.noteCard || n;
    var id = n.id || n.note_id || n.noteId || card.note_id || card.noteId;
    if (!id) return null;
    var token = n.xsec_token || n.xsecToken || '';
    var cover = '';
    try {
      var c = card.cover || n.cover || {};
      cover = c.url_pre || c.url_default || c.urlPre || c.urlDefault || '';
      if (!cover) {
        var il = c.info_list || c.infoList;
        if (il && il.length) {
          var best = '';
          for (var i = 0; i < il.length; i++) {
            var u = (il[i] && (il[i].url || il[i].url_default || il[i].urlDefault)) || '';
            if (u.length > best.length) best = u;
          }
          cover = best;
        }
      }
    } catch (e) {}
    return {
      id: String(id),
      xsec_token: token,
      title: String(card.display_title || card.displayTitle || '').slice(0, 120),
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

  // ---- feed API 响应 → 提取媒体直链（SPA 点卡片截获） ----
  function relayMedia(bodyText) {
    if (!bodyText || bodyText.charAt(0) !== '{') { log('media_skip head=' + String(bodyText || '').slice(0, 40)); return; }
    var j;
    try { j = JSON.parse(bodyText); } catch (e) { log('media_json_err:' + e.message); return; }
    log('media code=' + j.code + ' msg=' + String(j.msg || '').slice(0, 40) + ' items=' + ((j.data && j.data.items && j.data.items.length) || 0));
    var items = (j.data && j.data.items) || [];
    for (var i = 0; i < items.length; i++) {
      var noteCard = items[i].note_card || items[i].noteCard || {};
      var noteId = items[i].id || noteCard.noteId || noteCard.note_id || '';
      if (!noteId) continue;
      var videoUrls = [];
      var imageUrls = [];
      // 视频直链：video.media.stream 各编码档
      try {
        var st = noteCard.video && noteCard.video.media && noteCard.video.media.stream;
        if (st) {
          var encs = ['h264', 'h265', 'av1'];
          for (var e = 0; e < encs.length; e++) {
            var arr = st[encs[e]];
            if (!arr || !arr.length) continue;
            for (var k = 0; k < arr.length; k++) {
              var it = arr[k];
              var u = (typeof it === 'string') ? it : ((it && (it.masterUrl || it.url)) || '');
              if (u && u.indexOf('http') === 0 && videoUrls.indexOf(u) < 0) videoUrls.push(u);
            }
          }
        }
      } catch (e) {}
      // 图集图片
      try {
        var il = noteCard.imageList || noteCard.images || [];
        for (var m = 0; m < il.length; m++) {
          var img = il[m] || {};
          var iu = img.urlDefault || img.url || '';
          if (iu && imageUrls.indexOf(iu) < 0) imageUrls.push(iu);
        }
      } catch (e) {}
      mediaMap[noteId] = { videoUrls: videoUrls, imageUrls: imageUrls };
      log('media ' + noteId.slice(0, 12) + ' v=' + videoUrls.length + ' i=' + imageUrls.length);
    }
    // 通知 Kotlin 侧（如果已有 bridge 方法）
    try {
      if (window.AndroidVideoBridge && window.AndroidVideoBridge.onXhsMedia) {
        var mediaPayload = [];
        for (var mid in mediaMap) {
          if (Object.prototype.hasOwnProperty.call(mediaMap, mid)) {
            mediaPayload.push({ id: mid, videoUrls: mediaMap[mid].videoUrls, imageUrls: mediaMap[mid].imageUrls });
          }
        }
        if (mediaPayload.length) {
          window.AndroidVideoBridge.onXhsMedia(JSON.stringify({ media: mediaPayload }));
        }
      }
    } catch (e) {}
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
      if (FEED_RE.test(u)) {
        stats.fetchSeen++;
        log('feed hit ' + u.slice(0, 120));
        return _fetch.apply(this, arguments).then(function (res) {
          log('feed res status=' + res.status + ' ok=' + res.ok);
          try { res.clone().text().then(function (t) {
            log('feed body len=' + (t ? t.length : 0) + ' head=' + String(t || '').slice(0, 60));
            relayMedia(t);
          }).catch(function (e) { log('feed_txt_err:' + e.message); }); } catch (e) {}
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
          try {
            var body = '';
            try { body = xhr.responseText || ''; } catch (e2) { log('rte:' + e2.message); }
            log('load fired status=' + xhr.status + ' len=' + body.length);
            relayPosted(body);
          } catch (e) { log('loadcb_err:' + e.message); }
        });
        xhr.addEventListener('error', function () { log('xhr error'); });
        xhr.addEventListener('timeout', function () { log('xhr timeout'); });
        xhr.addEventListener('abort', function () { log('xhr abort'); });
      } else if (USER_RE.test(u)) {
        stats.xhrSeen++;
        xhr.addEventListener('load', function () {
          try { relayProfile(xhr.responseText); } catch (e) {}
        });
      } else if (FEED_RE.test(u)) {
        stats.xhrSeen++;
        log('xhr feed hit ' + u.slice(0, 120));
        xhr.addEventListener('load', function () {
          try { relayMedia(xhr.responseText); } catch (e) {}
        });
      }
    } catch (e) {}
    return _send.apply(this, arguments);
  };

  // ---- SSR 直接收割：__INITIAL_STATE__ 里就有首屏笔记（不依赖翻页 XHR）----
  /** 解 Vue reactive ref：{dep, _value} 形状 → 返回 _value */
  function unwrap(v) {
    try {
      if (v && typeof v === 'object') {
        var ks = Object.keys(v);
        if (ks.indexOf('dep') >= 0 && ks.indexOf('_value') >= 0) return v._value;
      }
    } catch (e) {}
    return v;
  }

  function relayNotes(list, src) {
    if (list.length) {
      var s0 = list[0] || {};
      var card0 = s0.noteCard || s0.note_card || {};
      var diag = {
        keys: Object.keys(s0).slice(0, 12),
        id: s0.id, noteId: s0.noteId,
        cardKeys: Object.keys(card0).slice(0, 15),
        rawNoteId: card0.noteId, rawType: typeof card0.noteId,
        uNoteId: unwrap(card0) !== card0 ? unwrap(card0).noteId : '(same)',
        title: card0.displayTitle || card0.display_title,
        type: card0.type
      };
      try { log(src + ' diag=' + JSON.stringify(diag).slice(0, 380)); } catch (e9) { log(src + ' diag_err:' + e9.message); }
      try {
        var uc = unwrap(card0);
        if (uc !== card0) log(src + ' unwrapped card=' + JSON.stringify(uc).slice(0, 300));
      } catch (e8) {}
    }
    var kept = [];
    for (var i = 0; i < list.length; i++) {
      var t = trimNote(list[i]);
      if (t && !seen[t.id]) { seen[t.id] = 1; kept.push(t); }
    }
    if (!kept.length) { log(src + ' kept=0'); return; }
    for (var s = 0; s < kept.length; s += MAX_PER_CHUNK) {
      var payload = {
        notes: kept.slice(s, s + MAX_PER_CHUNK),
        has_more: false,
        cursor: '',
        total: 0,
        page: 0
      };
      try {
        if (window.AndroidVideoBridge && window.AndroidVideoBridge.onXhsPosted) {
          window.AndroidVideoBridge.onXhsPosted(JSON.stringify(payload));
          stats.relayed += payload.notes.length;
        }
      } catch (e) { log('bridge_err:' + e.message); }
    }
    log(src + ' new=' + kept.length + ' relayed=' + stats.relayed);
  }

  // 实测定案（2026-10-21 keys 树 dump）：state.user.notes 是 ref → 按 tab 分组的 map
  //   {tabIdx: [{id, noteCard, index, exposed, ssrRendered, xsecToken}, ...]}；
  // 昵称/总数在 state.user.userPageData(ref).basicInfo.nickname / posted.count。
  // 实测定案（2026-10-21）：SSR state.user.notes 有 token/标题/封面但 id/noteId 是空串；
  // 真实 noteId 在渲染后的 DOM 链接里（/explore/{id}?xsec_token=… 或 /user/profile/uid/{id}）。
  // 以 xsec_token 为join key：DOM 提供 id，SSR 提供 token/标题/封面。
  function harvestDom(tag) {
    var byTok = {};
    var count = 0;
    try {
      var as = document.querySelectorAll('a[href*="xsec_token"]');
      for (var i = 0; i < as.length; i++) {
        var href = '';
        try { href = as[i].getAttribute('href') || ''; } catch (e0) {}
        if (!href) continue;
        var m = href.match(/(?:\/explore\/|\/user\/profile\/[^\/]+\/|\/discovery\/item\/)([0-9A-Za-z]+)/);
        var t = '';
        var mt = href.match(/xsec_token=([^&]+)/);
        if (mt) { try { t = decodeURIComponent(mt[1]); } catch (e1) { t = mt[1]; } }
        if (!m || !m[1]) continue;
        var id = m[1];
        var cover = '';
        try {
          var img = as[i].querySelector('img');
          if (img) cover = img.getAttribute('src') || '';
        } catch (e2) {}
        var item = {
          id: id,
          xsec_token: t,
          title: (as[i].getAttribute('title') || (as[i].textContent || '').replace(/\s+/g, ' ').trim()).slice(0, 120),
          type: 'normal',
          cover: cover
        };
        var key = t || ('id_' + id);
        if (!byTok[key]) { byTok[key] = item; count++; }
      }
    } catch (e) { log(tag + ' dom_err:' + e.message); }
    try {
      var allA = document.querySelectorAll('a');
      var sample = [];
      for (var z = 0; z < allA.length && sample.length < 5; z++) {
        var h = allA[z].getAttribute('href') || '';
        if (h) sample.push(h.slice(0, 80));
      }
      var sec = document.querySelector('section');
      log(tag + ' aCount=' + allA.length + ' hrefs=' + sample.join(' | ').slice(0, 300));
      if (sec) log(tag + ' sec=' + sec.outerHTML.replace(/\s+/g, ' ').slice(0, 400));
    } catch (e7) {}
    log(tag + ' dom=' + count);    return byTok;
  }

  // 诊断：全树扫 24 位 hex（noteId 格式），定位 id 是否存在于 state 其他位置
  var HEX24 = /^[0-9a-f]{24}$/;
  function scanHex24(root, tag) {
    var hits = [], seenO = [];
    (function walk(node, path, depth) {
      if (!node || depth > 6 || hits.length > 25) return;
      if (typeof node === 'string') { if (HEX24.test(node)) hits.push(path + '=' + node); return; }
      if (typeof node !== 'object') return;
      for (var v = 0; v < seenO.length; v++) { if (seenO[v] === node) return; }
      seenO.push(node);
      for (var k in node) {
        try { if (Object.prototype.hasOwnProperty.call(node, k)) walk(node[k], path + '.' + k, depth + 1); } catch (e2) {}
      }
    })(root, '', 0);
    log(tag + ' hex24=' + hits.length);
    for (var hi = 0; hi < hits.length; hi++) log(tag + ' H ' + hits[hi]);
    return hits;
  }

  // ==== 移动 H5 主路径：解析 __INITIAL_STATE__ 的 script 文本（JSON 岛）====
  // 2026-10-22 实测定案：移动 H5 的游客态把完整笔记（含真实 id）留在 <script> 文本里，
  // 但 SPA 启动后会把运行时 state.noteData.data 清空 → 必须读文本而非活对象。
  // item 形状：{cover:{…}, id, likes, title, type:"video|normal", user:{id}}；
  // note 级 xsec_token 缺失 → 用 URL 上的 profile 级 token 兜底（explore 页共用）。
  // ---- SPA 内点卡片截 API：直接调 feed API 拿媒体直链 ----
  // ---- feed API 旁路 hook（保留）----
  // 「SPA 内点卡片截 API」基建：SPA 任何时候自己调 /api/sns/web/v1/feed（带签名），
  // hook 都能截获响应提媒体。★实测直调 feed 缺 X-s 必 500（jarvis-gateway），
  // 批量媒体直链的主路径是「笔记页岛解析」（discovery/item?xsec_token=… + xhs_note_extractor.js）。

  function harvestIsland(tag) {
    try {
      var sc = document.scripts;
      for (var i = 0; i < sc.length; i++) {
        var tx = '';
        try { tx = sc[i].textContent || ''; } catch (e0) {}
        var eq = tx.indexOf('window.__INITIAL_STATE__=');
        if (eq < 0) continue;
        var body = tx.slice(eq + 'window.__INITIAL_STATE__='.length).replace(/;\s*$/, '');
        // __INITIAL_STATE__ 是 JS 对象字面量非严格 JSON：含裸 undefined/NaN → 先 sanitize
        body = body.replace(/:\s*undefined\b/g, ':null').replace(/:\s*NaN\b/g, ':null')
                   .replace(/,\s*([}\]])/g, '$1');   // 去尾逗号
        var st;
        try { st = JSON.parse(body); } catch (e1) {
          log(tag + ' isparse_err:' + e1.message); continue;
        }
        var items = [];
        (function walk(node, depth) {
          if (!node || typeof node !== 'object' || depth > 10 || items.length > 80) return;
          if (typeof node.length === 'number' && node.length > 0) {
            var f = node[0];
            // note id 是 24 或 32 位 hex（实测 7f830cd4…52e89b13c12 = 32 位）
            if (f && typeof f === 'object' && typeof f.id === 'string' && /^[0-9a-f]{20,32}$/.test(f.id) &&
                (f.title !== undefined || f.likes !== undefined)) {
              for (var j = 0; j < node.length; j++) items.push(node[j]);
              return;
            }
          }
          for (var k in node) {
            try { if (Object.prototype.hasOwnProperty.call(node, k)) walk(node[k], depth + 1); } catch (e2) {}
          }
        })(st, 0);
        log(tag + ' islandItems=' + items.length);
        if (items.length) return items;
      }
    } catch (e) { log(tag + ' island_err:' + e.message); }
    return [];
  }

  function harvestInitialState(tag) {
    try {
      // 0) 移动 H5 JSON 岛（主路径）
      var island = harvestIsland(tag);
      if (island.length) {
        var urlTok = '';
        try {
          var mt = location.href.match(/xsec_token=([^&]+)/);
          if (mt) urlTok = decodeURIComponent(mt[1]);
        } catch (e3) {}
        // ★v3.3.5 根因修复：岛内 item 通常不带 xsec_token，早前这里硬写 ''，
        //   导致「笔记页解析」整段被 `if (!token.isNullOrBlank())` 跳过 → 游客批量 100% 落封面兜底（实测全 404）。
        //   note 级 token 在**渲染后的 DOM 链接**里（/explore/{id}?xsec_token=… / /discovery/item/{id}?xsec_token=…），
        //   以 id 为 join key 合并回来即可。注意：profile 级 token 对单篇页无效(-510000)，只能取链接自带的。
        var domById = {};
        try {
          var dm = harvestDom(tag + 'i');
          for (var dk in dm) {
            try {
              var dv = dm[dk];
              if (dv && dv.id && dv.xsec_token) domById[dv.id] = dv;
            } catch (e6) {}
          }
        } catch (e7) {}
        var mapped = [];
        var tokN = 0;
        for (var q = 0; q < island.length; q++) {
          var it = island[q] || {};
          var cov = it.cover || {};
          var nid = String(it.id);
          var tok = it.xsec_token || it.xsecToken || '';
          var dd = domById[nid];
          if (!tok && dd) tok = dd.xsec_token || '';
          var covUrl = cov.url_default || cov.url || cov.urlPre || cov.url_pre || '';
          if (!covUrl && dd) covUrl = dd.cover || '';
          if (tok) tokN++;
          mapped.push({
            id: nid,
            xsec_token: tok,
            title: String(it.title || '').slice(0, 120),
            type: it.type === 'video' ? 'video' : 'normal',
            cover: covUrl
          });
        }
        log(tag + ' islandTok=' + tokN + '/' + mapped.length + ' domTok=' + Object.keys(domById).length);
        relayNotes(mapped, tag);
        // 昵称兜底：移动 profile 分支
        if (!profileRelayed) {
          try {
            var pfi = window.__INITIAL_STATE__ && window.__INITIAL_STATE__.profile;
            var nick = '';
            try {
              var s2 = document.scripts;
              for (var z = 0; z < s2.length; z++) {
                var t2 = s2[z].textContent || '';
                var m2 = t2.match(/"nickname":"([^"]{1,40})"/);
                if (m2) { nick = m2[1]; break; }
              }
            } catch (e4) {}
            if (nick) {
              profileRelayed = true;
              if (window.AndroidVideoBridge && window.AndroidVideoBridge.onXhsProfile) {
                window.AndroidVideoBridge.onXhsProfile(JSON.stringify({ nickname: nick, total: 0 }));
              }
              log('profile3 nickname=' + nick);
            }
          } catch (e5) {}
        }
        return;
      }

      var s = window.__INITIAL_STATE__;
      var user = s && s.user;
      var domMap = harvestDom(tag);
      // DOM 兜底：SSR 拿不到时直接用 DOM 项
      var domList = [];
      for (var dk in domMap) domList.push(domMap[dk]);

      var list = [];
      if (user) {
        var notesMap = unwrap(user.notes);
        if (notesMap && typeof notesMap === 'object') {
          for (var k in notesMap) {
            try {
              var arr = notesMap[k];
              if (arr && typeof arr.length === 'number') {
                for (var i = 0; i < arr.length; i++) list.push(arr[i]);
              }
            } catch (e2) {}
          }
        }
      }
      log(tag + ' path=' + list.length);
      if (tag === 't15') {
        scanHex24(s, tag);
        try {
          var html = document.documentElement.innerHTML;
          var ids = html.match(/(?:explore\/|profile\/[0-9A-Za-z]+\/)([0-9a-f]{24})/g) || [];
          var uniq = {};
          for (var q = 0; q < ids.length; q++) uniq[ids[q]] = 1;
          log(tag + ' htmlIds=' + Object.keys(uniq).length + ' cookie=' + String(document.cookie || '').slice(0, 120));
          var ld = document.querySelector('script[type="application/ld+json"]');
          if (ld) log(tag + ' ld=' + ld.textContent.slice(0, 400));
        } catch (e5) { log(tag + ' htmlscan_err:' + e5.message); }
      }

      // 合并：SSR 项补 id（自身→noteCard→DOM by token）；SSR 为空则用 DOM 项
      var merged = [];
      for (var j = 0; j < list.length; j++) {
        var n = list[j] || {};
        var card = n.noteCard || n.note_card || {};
        var tok = n.xsecToken || n.xsec_token || card.xsecToken || card.xsec_token || '';
        var id = n.id || n.noteId || card.noteId || card.note_id ||
                 ((tok && domMap[tok]) ? domMap[tok].id : '');
        if (!id) continue;
        var d = (tok && domMap[tok]) || {};
        merged.push({
          id: String(id),
          xsec_token: tok || d.xsec_token || '',
          title: String(card.displayTitle || card.display_title || d.title || '').slice(0, 120),
          type: (card.type === 'video') ? 'video' : 'normal',
          cover: d.cover || ''
        });
      }
      // 合并封面：SSR cover 结构复杂时用 DOM 的
      if (!merged.length && domList.length) merged = domList;

      if (merged.length) relayNotes(merged, tag);
      else if (domList.length) relayNotes(domList, tag);

      if (!profileRelayed) {
        var pd = user ? (unwrap(user.userPageData) || {}) : {};
        var bi = unwrap(pd.basicInfo) || {};
        var posted = unwrap(pd.posted) || {};
        var nick = bi.nickname || '';
        var total = posted.count || 0;
        if (nick || total) {
          profileRelayed = true;
          try {
            if (window.AndroidVideoBridge && window.AndroidVideoBridge.onXhsProfile) {
              window.AndroidVideoBridge.onXhsProfile(JSON.stringify({ nickname: nick, total: total }));
            }
          } catch (e3) {}
          log('profile2 nickname=' + nick + ' total=' + total);
        }
      }
    } catch (e) { log(tag + '_err:' + e.message); }
  }
  setTimeout(function () { harvestInitialState('t0'); }, 300);
  setTimeout(function () { harvestInitialState('t2'); }, 2000);
  setTimeout(function () { harvestInitialState('t6'); }, 6000);
  setTimeout(function () { harvestInitialState('t15'); }, 15000);
  setTimeout(function () { harvestInitialState('t30'); }, 30000);
  setTimeout(function () { harvestInitialState('t60'); }, 60000);

  // ---- 多轮滚动触发 cursor 翻页（user_posted XHR 带真实 id）----
  function tryScroll(n) {
    try {
      var h = document.body ? document.body.scrollHeight : 0;
      window.scrollTo(0, h);
      log('scroll' + n + ' h=' + h + ' dom=' + document.querySelectorAll('section').length);
    } catch (e) {}
  }
  setTimeout(function () { tryScroll(1); }, 5000);
  setTimeout(function () { tryScroll(2); }, 10000);
  setTimeout(function () { tryScroll(3); }, 16000);
  setTimeout(function () { tryScroll(4); }, 24000);
  setTimeout(function () { tryScroll(5); }, 35000);
  setTimeout(function () { tryScroll(6); }, 50000);

  log('installed');
})();

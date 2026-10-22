/*
 * 小红书单条笔记详情提取器（explore 页 · window.__INITIAL_STATE__）
 * ------------------------------------------------------------------
 * 由 BgWebViewParser.pollExtract 每 800ms 轮询 evaluate；页面未水合/无 note 时
 * 返回全空结构（无害重试），任一媒体命中即收口。
 *
 * 输出契约与 douyin_extractor.js 完全对齐（Kotlin 侧 parseExtractorJson 零改动）：
 *   { urls: [视频直链…], image_urls: [图集图片…], content: { content, title, desc } }
 *
 * 字段路径实证来源：app/src/main/java/com/neoruaa/xhsdn/XHSDownloader.java
 *   note        = __INITIAL_STATE__.noteData.data.noteData
 *   视频        = note.video.media.stream.h264[] / h265[]（元素为对象{masterUrl,url}或裸字符串）
 *   图片        = note.imageList[]（urlDefault → url → traceId 拼 CDN → infoList[0].url）
 *   Live Photo  = image.stream.h264[0].masterUrl —— P0 不采（避免图组被整组当视频下载），
 *                 只保留静帧图片；后续版本如需动图再单独出 videos 通道。
 *
 * 安全性：纯只读。任何异常吞掉返回空结构，不影响页面自身。
 */
(function () {
  var EMPTY = { urls: [], image_urls: [], content: { content: '', title: '', desc: '' } };
  try {
    // ★优先读 script 文本解析岛：SPA 启动后会清空活对象 noteData.data（实证 p15 nDdata={}），
    // 读 window.__INITIAL_STATE__ 活对象会踩空；文本里保留完整 noteData.data.noteData。
    var note = null;
    // SSR __INITIAL_STATE__ JSON 常中途截断（GitHub xiaohongshu-post-analyzer 实证）：
    // 直接 JSON.parse 必炸 → 按逗号边界收缩 + 补闭合括号重试；仍失败走 og: 兜底。
    var tryParseNote = function (raw) {
      try {
        var st = JSON.parse(raw);
        return (st && st.noteData && st.noteData.data && st.noteData.data.noteData) || null;
      } catch (e) { return null; }
    };
    var closeStack = function (s) {
      var stack = [], inStr = false, esc = false;
      for (var i = 0; i < s.length; i++) {
        var ch = s.charAt(i);
        if (inStr) {
          if (esc) esc = false;
          else if (ch === '\\') esc = true;
          else if (ch === '"') inStr = false;
        } else if (ch === '"') inStr = true;
        else if (ch === '{') stack.push('}');
        else if (ch === '[') stack.push(']');
        else if (ch === '}' || ch === ']') stack.pop();
      }
      return stack.reverse().join('');
    };
    var parseIsland = function (raw) {
      var hit = tryParseNote(raw);
      if (hit) return hit;
      // 截断修复：在字符串外的逗号处截断，丢弃不完整尾部成员，补闭合括号
      var cuts = [], inStr = false, esc = false;
      for (var i = 0; i < raw.length; i++) {
        var ch = raw.charAt(i);
        if (inStr) {
          if (esc) esc = false;
          else if (ch === '\\') esc = true;
          else if (ch === '"') inStr = false;
        } else if (ch === '"') inStr = true;
        else if (ch === ',') cuts.push(i);
      }
      for (var c = cuts.length - 1; c >= 0 && c >= cuts.length - 40; c--) {
        var prefix = raw.slice(0, cuts[c]).replace(/,\s*$/, '');
        hit = tryParseNote(prefix + closeStack(prefix));
        if (hit) return hit;
      }
      return null;
    };
    var ogTitle = '';
    try {
      var metas = document.getElementsByTagName('meta');
      for (var m = 0; m < metas.length; m++) {
        if ((metas[m].getAttribute('property') || '') === 'og:title') {
          ogTitle = metas[m].getAttribute('content') || '';
          break;
        }
      }
    } catch (eog) {}
    try {
      var sc = document.scripts;
      for (var i = 0; i < sc.length; i++) {
        var tx = '';
        try { tx = sc[i].textContent || ''; } catch (e0) {}
        var eq = tx.indexOf('window.__INITIAL_STATE__=');
        if (eq < 0) continue;
        var body = tx.slice(eq + 'window.__INITIAL_STATE__='.length).replace(/;\s*$/, '');
        // JS 对象字面量非严格 JSON：sanitize 裸 undefined/NaN + 尾逗号
        body = body.replace(/:\s*undefined\b/g, ':null').replace(/:\s*NaN\b/g, ':null')
                   .replace(/,\s*([}\]])/g, '$1');
        note = parseIsland(body);
        if (note) break;
      }
    } catch (e1) {}
    if (!note) {
      var s = window.__INITIAL_STATE__;
      note = s && s.noteData && s.noteData.data && s.noteData.data.noteData;
    }
    // og: 兜底（岛整体缺失时的最低保障：封面图 + 标题）
    if (!note) {
      var ogVideo = '', ogImage = '';
      try {
        var ms = document.getElementsByTagName('meta');
        for (var q = 0; q < ms.length; q++) {
          var prop = ms[q].getAttribute('property') || '';
          var cnt = ms[q].getAttribute('content') || '';
          if (!ogVideo && /^og:video(:url|:secure_url)?$/.test(prop) && /\.(mp4|m3u8)|video[-.]/.test(cnt)) ogVideo = cnt;
          if (!ogImage && /^og:image(:url)?$/.test(prop) && !ogImage) ogImage = cnt;
        }
      } catch (eog2) {}
      if (ogVideo || ogImage) {
        return {
          urls: ogVideo ? [ogVideo] : [],
          image_urls: ogImage ? [ogImage] : [],
          content: { content: ogTitle, title: ogTitle, desc: '' }
        };
      }
      return EMPTY;
    }

    var urls = [];
    // ---- 视频直链：video.media.stream 各编码档，优先 h264（兼容性最好）----
    try {
      var st = note.video && note.video.media && note.video.media.stream;
      if (st) {
        var encs = ['h264', 'h265', 'av1'];
        for (var e = 0; e < encs.length; e++) {
          var arr = st[encs[e]];
          if (!arr || !arr.length) continue;
          for (var i = 0; i < arr.length; i++) {
            var it = arr[i];
            var u = (typeof it === 'string') ? it : ((it && (it.masterUrl || it.url)) || '');
            if (u && u.indexOf('http') === 0 && urls.indexOf(u) < 0) urls.push(u);
          }
        }
      }
    } catch (e) {}

    // ---- 图集图片 ----
    var images = [];
    try {
      var list = note.imageList || note.images || [];
      for (var j = 0; j < list.length; j++) {
        var img = list[j] || {};
        var iu = img.urlDefault || img.url || '';
        if (!iu && img.traceId) iu = 'https://sns-img-qc.xhscdn.com/' + img.traceId;
        if (!iu && img.infoList && img.infoList.length) {
          for (var k = 0; k < img.infoList.length; k++) {
            if (img.infoList[k] && img.infoList[k].url) { iu = img.infoList[k].url; break; }
          }
        }
        if (iu && iu.indexOf('http') === 0 && images.indexOf(iu) < 0) images.push(iu);
      }
    } catch (e) {}

    // ---- 文案：title + desc + tagList（与 xhs_extractor.js 的 content 组装一致）----
    var title = note.title || ogTitle || '';
    var desc = note.desc || '';
    var tags = '';
    try {
      tags = (note.tagList || []).map(function (t) { return (t && t.name) || ''; })
        .filter(Boolean).join(' ');
    } catch (e) {}
    var content = title + (title ? '\n' : '') + desc + (desc && tags ? '\n' : '') + tags;

    if (!urls.length && !images.length) return EMPTY;
    return {
      urls: urls,
      image_urls: images,
      content: { content: content.trim(), title: title, desc: desc }
    };
  } catch (e) {
    return EMPTY;
  }
})();

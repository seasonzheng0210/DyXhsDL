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
        var st = JSON.parse(body);
        note = st && st.noteData && st.noteData.data && st.noteData.data.noteData;
        if (note) break;
      }
    } catch (e1) {}
    if (!note) {
      var s = window.__INITIAL_STATE__;
      note = s && s.noteData && s.noteData.data && s.noteData.data.noteData;
    }
    if (!note) return EMPTY;

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
    var title = note.title || '';
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

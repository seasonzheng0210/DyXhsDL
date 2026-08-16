/**
 * Kuaishou Extractor (WebView-first)
 * Purpose: 在 App 内 WebView 渲染快手作品页后，从 window.__INITIAL_STATE__ 抠取
 * 无水印视频地址（photoUrl）与图集图片（images[].url），并兜底扫描 <video>/<img>。
 *
 * 返回结构与 xhs_extractor.js / douyin_extractor.js 同构：{ urls: [...], content: {...} }
 *
 * 说明：
 *  - 快手播放源 photoUrl 本身无水印（水印只在 App「保存到相册」导出链路烧录），直接可用。
 *  - 跳过 mainMvUrls（带 water.mp4 的水印源）与 manifest/manifestH265（HLS .m3u8，简单下载器无法处理）。
 */
(function() {
    var urls = [];
    var seen = {};
    function add(u) {
        if (!u) return;
        u = String(u).trim();
        if (u.indexOf('//') === 0) u = 'https:' + u;
        if (u.indexOf('http') !== 0) return;
        if (u.indexOf('blob:') === 0 || u.indexOf('data:') === 0) return;
        if (!seen[u]) { seen[u] = 1; urls.push(u); }
    }

    var content = { content: '', title: '', desc: '' };
    var foundCaption = '';
    var foundAuthor = '';

    // 递归遍历 __INITIAL_STATE__，收集视频/图集/封面，跳过水印与 HLS 源
    var SKIP_KEYS = { mainMvUrls: 1, manifest: 1, manifestH265: 1 };
    function walk(node, depth) {
        if (!node || depth > 10 || typeof node !== 'object') return;
        // 只下载视频：仅收集无水印播放源 photoUrl，跳过封面(coverUrl)与图集(images)
        if (typeof node.photoUrl === 'string' && node.photoUrl.indexOf('http') === 0 && !node.photoUrl.endsWith('.m3u8')) {
            add(node.photoUrl);
        }
        // 顺带收集标题(caption)与作者(authorName/author.name)
        if (!foundCaption && typeof node.caption === 'string' && node.caption.trim()) {
            foundCaption = node.caption.trim();
        }
        if (!foundAuthor) {
            if (typeof node.authorName === 'string' && node.authorName.trim()) {
                foundAuthor = node.authorName.trim();
            } else if (node.author && typeof node.author === 'object' && typeof node.author.name === 'string' && node.author.name.trim()) {
                foundAuthor = node.author.name.trim();
            }
        }
        var keys = Object.keys(node);
        for (var k = 0; k < keys.length; k++) {
            var key = keys[k];
            if (SKIP_KEYS[key]) continue;
            var v = node[key];
            if (v && typeof v === 'object') walk(v, depth + 1);
        }
    }

    try {
        // 快手页面数据先后用过两个全局：旧 __INITIAL_STATE__ 与新 __APOLLO_STATE__（Apollo GraphQL 缓存）。
        // 桌面页 www.kuaishou.com/short-video/{id} 用 Apollo —— 需 WebView 强制桌面 UA 才能落到该页，
        // 否则会被甩到 m.gifshow.com 的「在 App 打开」中转遮罩页（无任何视频数据）。
        var stateSources = [];
        if (window.__INITIAL_STATE__) stateSources.push(window.__INITIAL_STATE__);
        if (window.__APOLLO_STATE__) stateSources.push(window.__APOLLO_STATE__);
        for (var s = 0; s < stateSources.length; s++) {
            walk(stateSources[s], 0);
        }

        // 兜底：__INITIAL_STATE__ 没抠到时，仅扫描页面 <video>（不扫 <img>，避免下载封面/图集图片）
        if (urls.length === 0) {
            var vids = document.querySelectorAll('video');
            for (var i = 0; i < vids.length; i++) {
                var v = vids[i];
                add(v.src);
                add(v.getAttribute('data-src'));
                add(v.getAttribute('poster'));
                var ss = v.querySelectorAll('source');
                for (var j = 0; j < ss.length; j++) { add(ss[j].src); add(ss[j].getAttribute('data-src')); }
            }
        }

        // 文本：caption / author（在两套全局里都找过，取首次命中）
        if (foundCaption) content.title = foundCaption;
        if (foundAuthor) content.desc = foundAuthor;
        if (!content.title && document.title) {
            content.title = document.title.replace(/[-_|].*$/, '').trim();
        }
        content.content = content.title + (content.title && content.desc ? '\n' : '') + content.desc;

        console.log('=== Kuaishou Extractor === urls=' + urls.length + ' title="' + content.title + '"');
        return { urls: urls, content: content };
    } catch (e) {
        console.error('Kuaishou extractor error: ' + e);
        return { urls: [], content: { content: '' } };
    }
})()

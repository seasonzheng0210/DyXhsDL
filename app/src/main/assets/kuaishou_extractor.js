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

    // 递归遍历 __INITIAL_STATE__，收集视频/图集/封面，跳过水印与 HLS 源
    var SKIP_KEYS = { mainMvUrls: 1, manifest: 1, manifestH265: 1 };
    function walk(node, depth) {
        if (!node || depth > 10 || typeof node !== 'object') return;
        if (typeof node.photoUrl === 'string' && node.photoUrl.indexOf('http') === 0 && !node.photoUrl.endsWith('.m3u8')) {
            add(node.photoUrl);
        }
        if (typeof node.coverUrl === 'string' && node.coverUrl.indexOf('http') === 0) {
            add(node.coverUrl);
        }
        if (node.images && Array.isArray(node.images)) {
            for (var i = 0; i < node.images.length; i++) {
                var it = node.images[i];
                var u = (it && typeof it === 'object') ? it.url
                       : (typeof it === 'string' ? it : null);
                if (u) add(u);
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
        var state = window.__INITIAL_STATE__;
        if (state) {
            walk(state, 0);
        }

        // 兜底：__INITIAL_STATE__ 没抠到时，扫描页面 <video>/<img>
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
            var imgs = document.querySelectorAll('img');
            for (var m = 0; m < imgs.length; m++) {
                var im = imgs[m];
                add(im.src);
                add(im.getAttribute('data-src'));
                add(im.getAttribute('data-original'));
            }
        }

        // 文本：caption
        if (state) {
            var photo = state.photo || (state.detail && state.detail.photo);
            if (photo && photo.caption) content.title = photo.caption;
            if (state.author && state.author.name) content.desc = state.author.name;
        }
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

/**
 * Douyin Video Extractor (WebView-first)
 *
 * 背景：抖音分享页（v.douyin.com / iesdouyin.com/share/video）现在是 SPA。
 *   - window._ROUTER_DATA 仅保留 SSR 壳（loaderData["video_(id)/page"] 里已无 videoInfoRes），
 *     真实播放数据由页面 JS 通过 XHR/fetch 异步拉取，可能落到任意嵌套对象，也可能根本不进全局变量。
 *   - 因此本提取器：(1) 递归扫描已知全局（_ROUTER_DATA / RENDER_DATA / __INITIAL_STATE__ / __APOLLO_STATE__）
 *     中嵌套的 play_addr.url_list / photoUrl；(2) 兼容旧结构 videoInfoRes.item_list；(3) 兜底扫描
 *     <video> 元素（currentSrc/src 与 <source>）——播放地址常由 web_inject.js 的 XHR 钩子先行捕获并回传，
 *     这里再兜一层，确保最终 allUrls 非空。
 *
 * 返回与 xhs/kuaishou 提取器同构：{ urls: [...], content: {...} }
 */
(function () {
    try {
        var urls = [];
        var seen = {};
        function add(u) {
            if (!u || typeof u !== 'string') return;
            u = String(u).trim();
            if (u.indexOf('//') === 0) u = 'https:' + u;
            if (u.indexOf('http') !== 0) return;
            if (u.indexOf('blob:') === 0 || u.indexOf('data:') === 0) return;
            if (u.endsWith('.m3u8')) return;
            u = u.replace('playwm', 'play'); // 去水印
            if (!seen[u]) { seen[u] = 1; urls.push(u); }
        }

        var content = { content: '', title: '', desc: '' };

        // 1) 递归扫描已知全局，抠出嵌套的播放地址
        function walk(node, depth) {
            if (!node || depth > 14 || typeof node !== 'object') return;
            if (node.play_addr && node.play_addr.url_list && node.play_addr.url_list.length) {
                add(String(node.play_addr.url_list[0]));
            }
            if (Array.isArray(node.url_list) && node.url_list.length && (node.play_addr || node.video)) {
                for (var i = 0; i < node.url_list.length; i++) add(String(node.url_list[i]));
            }
            if (typeof node.photoUrl === 'string') add(node.photoUrl);
            if (typeof node.coverUrl === 'string') add(node.coverUrl);
            if (node.images && Array.isArray(node.images)) {
                for (var j = 0; j < node.images.length; j++) {
                    var it = node.images[j];
                    var u = (it && typeof it === 'object') ? it.url : (typeof it === 'string' ? it : null);
                    if (u) add(u);
                }
            }
            if (typeof node.desc === 'string' && node.desc && !content.title) content.title = node.desc;
            if (typeof node.description === 'string' && node.description && !content.title) content.title = node.description;
            var keys = Object.keys(node);
            for (var k = 0; k < keys.length; k++) {
                var v = node[keys[k]];
                if (v && typeof v === 'object') walk(v, depth + 1);
            }
        }

        var globals = [
            window._ROUTER_DATA,
            window.RENDER_DATA,
            window.__INITIAL_STATE__,
            window.__APOLLO_STATE__
        ];
        for (var g = 0; g < globals.length; g++) {
            if (globals[g]) walk(globals[g], 0);
        }

        // 2) 兼容旧结构：_ROUTER_DATA.loaderData["video_xxx/page"].videoInfoRes.item_list
        var rd = window._ROUTER_DATA;
        if (rd && rd.loaderData) {
            var ld = rd.loaderData;
            var keys2 = Object.keys(ld);
            for (var i = 0; i < keys2.length; i++) {
                var vv = ld[keys2[i]];
                if (vv && vv.videoInfoRes) {
                    var il = vv.videoInfoRes.item_list;
                    if (il && il.length) {
                        var d = il[0];
                        if (d && d.video) {
                            if (d.video.play_addr && d.video.play_addr.url_list && d.video.play_addr.url_list.length) {
                                add(String(d.video.play_addr.url_list[0]));
                            }
                            if (d.video.bit_rate) {
                                for (var b = 0; b < d.video.bit_rate.length; b++) {
                                    var br = d.video.bit_rate[b];
                                    if (br && br.play_addr && br.play_addr.url_list && br.play_addr.url_list.length) {
                                        add(String(br.play_addr.url_list[0]));
                                    }
                                }
                            }
                        }
                        if (d && d.desc) content.title = d.desc;
                    }
                }
            }
        }

        // 3) 兜底：扫描页面 <video> 元素
        var vids = document.querySelectorAll('video');
        for (var m = 0; m < vids.length; m++) {
            var v = vids[m];
            add(v.currentSrc);
            add(v.src);
            var ss = v.querySelectorAll('source');
            for (var n = 0; n < ss.length; n++) {
                add(ss[n].src);
            }
        }

        if (content.title && !content.content) content.content = content.title;
        console.log('=== Douyin Extractor === urls=' + urls.length + ' title="' + content.title + '"');
        return { urls: urls, content: content };
    } catch (e) {
        console.error('Douyin extractor error: ' + e);
        return { urls: [], content: { content: '' } };
    }
})()

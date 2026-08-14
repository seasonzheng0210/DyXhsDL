/**
 * web_inject.js —— WebView 异步视频地址钩子
 *
 * 背景：抖音/快手分享页现在是 SPA，视频数据（play_addr / photoUrl）由页面自己的 JS
 * 通过 XHR/fetch 异步拉取，不再内联在初始 HTML 的全局变量里。直接在 onPageFinished
 * 后读 window._ROUTER_DATA 往往拿到的是 SSR 壳（无 videoInfoRes），导致提取为空、
 * 回退到已被风控的 HTTP 直解而失败。
 *
 * 做法：包裹 window.fetch 与 XMLHttpRequest，捕获「带播放数据的响应体」，递归扫描出
 * 真实播放地址（playwm -> play 去水印），通过 AndroidVideoBridge 回传给 App；并周期
 * 扫描 <video> 元素作为兜底。重复注入幂等（window.__dyInject 守卫）。
 *
 * 注意：本脚本仅观察响应、不修改请求，避免破坏页面自身鉴权。
 */
(function () {
    if (window.__dyInject) return;
    window.__dyInject = true;

    function post(u) {
        try {
            if (!u || typeof u !== 'string') return;
            u = String(u).trim();
            if (u.indexOf('//') === 0) u = 'https:' + u;
            if (u.indexOf('http') !== 0) return;
            if (u.indexOf('blob:') === 0 || u.indexOf('data:') === 0) return;
            // 抖音水印源 playwm -> play 去水印（对其他平台无副作用）
            u = u.replace('playwm', 'play');
            if (window.AndroidVideoBridge) window.AndroidVideoBridge.onVideoUrl(u);
        } catch (e) {}
    }

    // 递归扫描对象树，抠出视频/图集地址
    function scanObj(node, depth) {
        if (!node || depth > 14 || typeof node !== 'object') return;
        if (node.play_addr && node.play_addr.url_list && node.play_addr.url_list.length) {
            post(node.play_addr.url_list[0]);
        }
        if (Array.isArray(node.url_list) && node.url_list.length && (node.play_addr || node.video)) {
            for (var i = 0; i < node.url_list.length; i++) post(node.url_list[i]);
        }
        if (typeof node.photoUrl === 'string') post(node.photoUrl);
        if (typeof node.coverUrl === 'string') post(node.coverUrl);
        if (node.images && Array.isArray(node.images)) {
            for (var j = 0; j < node.images.length; j++) {
                var it = node.images[j];
                var u = (it && typeof it === 'object') ? it.url : (typeof it === 'string' ? it : null);
                if (u) post(u);
            }
        }
        var keys = Object.keys(node);
        for (var k = 0; k < keys.length; k++) {
            var v = node[keys[k]];
            if (v && typeof v === 'object') scanObj(v, depth + 1);
        }
    }

    function handleText(t) {
        if (!t || t.length < 50) return;
        if (t.indexOf('play_addr') < 0 && t.indexOf('photoUrl') < 0 &&
            t.indexOf('url_list') < 0 && t.indexOf('videoInfoRes') < 0) return;
        try {
            if (t.length < 3000000) {
                var j = JSON.parse(t);
                scanObj(j, 0);
            }
        } catch (e) {}
    }

    // 包裹 fetch
    var origFetch = window.fetch;
    window.fetch = function () {
        var a = arguments;
        return origFetch.apply(this, a).then(function (r) {
            try {
                r.clone().text().then(function (t) { handleText(t); }).catch(function () {});
            } catch (e) {}
            return r;
        }).catch(function (e) {
            return origFetch.apply(this, a);
        });
    };

    // 包裹 XMLHttpRequest
    var origOpen = XMLHttpRequest.prototype.open;
    var origSend = XMLHttpRequest.prototype.send;
    XMLHttpRequest.prototype.open = function (m, u) {
        this.__dyu = u;
        return origOpen.apply(this, arguments);
    };
    XMLHttpRequest.prototype.send = function () {
        var self = this;
        this.addEventListener('load', function () {
            try { handleText(self.responseText); } catch (e) {}
        });
        return origSend.apply(this, arguments);
    };

    // 周期扫描 <video>（含 MSE/HLS 播放元素的 currentSrc/src 与 <source>）
    setInterval(function () {
        try {
            var vs = document.querySelectorAll('video');
            for (var i = 0; i < vs.length; i++) {
                var v = vs[i];
                if (v.currentSrc) post(v.currentSrc);
                if (v.src) post(v.src);
                var ss = v.querySelectorAll('source');
                for (var j = 0; j < ss.length; j++) {
                    if (ss[j].src) post(ss[j].src);
                }
            }
        } catch (e) {}
    }, 700);

    console.log('=== dy inject ready ===');
})();

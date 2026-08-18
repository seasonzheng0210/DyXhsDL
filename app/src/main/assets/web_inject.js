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

    // 从图集图片对象中取直链：抖音真实结构为 images[i].url_list[0]（多 CDN 候选），
    // 旧结构可能用 .url；统一兼容。
    function getImageUrl(it) {
        if (!it) return null;
        if (typeof it === 'string') return it;
        if (Array.isArray(it.url_list) && it.url_list.length) return String(it.url_list[0]);
        if (typeof it.url === 'string' && it.url) return it.url;
        if (it.origin_image && Array.isArray(it.origin_image.url_list) && it.origin_image.url_list.length)
            return String(it.origin_image.url_list[0]);
        if (it.display_image && Array.isArray(it.display_image.url_list) && it.display_image.url_list.length)
            return String(it.display_image.url_list[0]);
        return null;
    }

    // 帖子图集图片：只认 images[]，单独回传，绝不混入视频/封面（修复图文帖被误当视频下）
    function postImage(u) {
        try {
            if (!u || typeof u !== 'string') return;
            u = String(u).trim();
            if (u.indexOf('//') === 0) u = 'https:' + u;
            if (u.indexOf('http') !== 0) return;
            if (u.indexOf('blob:') === 0 || u.indexOf('data:') === 0) return;
            if (u.endsWith('.m3u8')) return;
            if (window.AndroidVideoBridge) window.AndroidVideoBridge.onImageUrl(u);
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
                var u = getImageUrl(it);
                if (u) postImage(u);
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

    // ---- API 请求地址捕获 + 同源立即重拉（GitHub 双引擎方案里的 API 引擎）----
    // 页面自己的 JS 发出的视频数据 API 请求自带有效签名（a_bogus/X-Bogus）与 cookie，
    // 拦截其 URL 后同源重拉，几秒内即可拿到 play_addr，无需等待 SPA 渲染完成。
    var API_RE = /(\/aweme\/v1\/|\/web\/api\/|iteminfo|visionVideoDetail|\/graphql|aweme\/detail|\/video\/play\/|aweme\/post|aweme\/list)/;
    var probed = {};

    // 从抖音请求 URL 中收割会话 token（msToken/webid/ttwid/uifid），供 directApiAttempt 补全签名。
    // 这些 token 由抖音页面 JS 在 WebView 运行时生成（游客态即可，无需登录账号），但必须随请求
    // 一同上送、并参与 a_bogus 签名，否则抖音服务端校验失败返回空。
    var TOKEN_RE = /[?&](msToken|webid|ttwid|uifid)=([^&]+)/g;
    function harvestTokens(url) {
        try {
            if (!url || typeof url !== 'string') return;
            if (url.indexOf('douyin.com') < 0) return;
            var seen = window.__dyTokens || {};
            var m;
            TOKEN_RE.lastIndex = 0;
            while ((m = TOKEN_RE.exec(url))) {
                seen[m[1]] = decodeURIComponent(m[2]);
            }
            window.__dyTokens = seen;
        } catch (e) {}
    }

    function probeApi(url) {
        try {
            if (!url || typeof url !== 'string') return;
            if (!API_RE.test(url)) return;
            harvestTokens(url);
            // 记下抖音自己签过名的 detail 请求，directApiAttempt 优先原样重放（同环境 a_bogus，最稳）
            if (url.indexOf('aweme/detail') >= 0 || url.indexOf('aweme/v1/web') >= 0) {
                window.__dyDetailUrl = url;
            }
            if (probed[url]) return;
            probed[url] = 1;
            fetch(url).then(function (r) {
                return r.text();
            }).then(function (t) {
                if (!t || t.length < 50) return;
                if (t.indexOf('play_addr') < 0 && t.indexOf('url_list') < 0 &&
                    t.indexOf('photoUrl') < 0 && t.indexOf('videoInfoRes') < 0) return;
                if (t.length < 3000000) {
                    var j = JSON.parse(t);
                    scanObj(j, 0);
                }
            }).catch(function () {});
        } catch (e) {}
    }

    // 包裹 fetch
    var origFetch = window.fetch;
    window.fetch = function () {
        var a = arguments;
        try {
            var reqUrl = (typeof a[0] === 'string') ? a[0] : (a[0] && a[0].url);
            probeApi(reqUrl);
        } catch (e) {}
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
        try { probeApi(u); } catch (e) {}
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

    // ---- 抖音直连 API 兜底（GitHub 双引擎里的 API 引擎）----
    // 当页面自己的 API 请求迟迟未发（SPA 崩溃/慢水合）时，用内嵌 a_bogus 算法自行签名并请求
    // 抖音 detail 接口，直接拿到 play_addr，不必等页面渲染。__getABogus 由 abogus.js 提供。
    var directTried = false;
    function directApiAttempt() {
        try {
            if (directTried) return;
            directTried = true;
            if (typeof window.__getABogus !== 'function') return;
            var host = (location.host || '');
            if (host.indexOf('douyin.com') < 0) return;
            var m = (location.pathname || '').match(/\/video\/(\d+)/);
            if (!m) return;
            var id = m[1];
            // 优先用拦截到的「抖音自己签过名」的 detail 请求直接重放（a_bogus 由抖音在同环境算，最稳）
            if (window.__dyDetailUrl) {
                fetch(window.__dyDetailUrl, { headers: { 'Referer': 'https://www.douyin.com/' } })
                    .then(function (r) { return r.text(); })
                    .then(function (t) { if (t && t.length >= 50) handleText(t); })
                    .catch(function () {});
                return;
            }
            // 兜底：自行签名。补全从拦截请求收割到的真实会话 token（否则服务端校验失败返回空）。
            var tok = window.__dyTokens || {};
            var base = 'https://www.douyin.com/aweme/v1/web/aweme/detail/?aweme_id=' + id +
                '&aid=6383&channel=channel_pc_web&device_platform=webapp' +
                '&browser_language=zh-CN&browser_platform=Win32&browser_name=Chrome&browser_version=124.0.0.0' +
                '&engine_name=Blink&engine_version=124.0.0.0&os_name=Windows&os_version=10' +
                '&screen_width=1280&screen_height=800';
            if (tok.msToken) base += '&msToken=' + encodeURIComponent(tok.msToken);
            if (tok.webid) base += '&webid=' + encodeURIComponent(tok.webid);
            if (tok.ttwid) base += '&webid=' + encodeURIComponent(tok.ttwid);
            if (tok.uifid) base += '&uifid=' + encodeURIComponent(tok.uifid);
            var ab = window.__getABogus(base, 'get');
            if (!ab) return;
            var url = base + '&a_bogus=' + encodeURIComponent(ab);
            fetch(url, { headers: { 'Referer': 'https://www.douyin.com/' } })
                .then(function (r) { return r.text(); })
                .then(function (t) { if (t && t.length >= 50) handleText(t); })
                .catch(function () {});
        } catch (e) {}
    }
    // 页面加载几秒后仍未捕获到播放地址时，尝试直连 API
    setTimeout(directApiAttempt, 3000);

    console.log('=== dy inject ready ===');
})();

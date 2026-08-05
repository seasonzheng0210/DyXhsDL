/**
 * Taobao / Tmall Image & Video Extractor (WebView-first)
 * Purpose: 在 App 内 WebView 渲染淘宝/天猫商品详情页后，从 DOM 抠取主图与视频。
 *
 * 为什么走 WebView 而不是 HTTP 直解：
 *   现代淘宝详情页是 JS 渲染的 SPA，匿名 HTML 源码里没有 auctionImages，
 *   服务端还会风控（滑块/登录墙）。WebView 渲染完成后 DOM 里才有真实 <img>/<video>，
 *   这与小红书/抖音的 WebView 抠图原理完全一致（evaluateJavascript 读 DOM）。
 *
 * 返回结构与 xhs_extractor.js / douyin_extractor.js 同构：{ urls: [...], content: {...} }
 */

(function() {
    var urls = [];
    var seen = {};
    var content = { content: '', title: '', desc: '' };

    // 仅收录淘宝/阿里系图片与视频直链，避免把页面图标/广告杂图也抓进来
    function isTaobaoMedia(u) {
        if (!u) return false;
        return /alicdn\.com|taobaocdn\.com|taobao\.com|tmall\.com/.test(u);
    }

    // 淘宝图片高清化：缩略图后缀 -> 高清
    // 典型缩略：xxx_60x60.jpg / _100x100.jpg / _sum.jpg / _40x40.jpg
    // 高清目标：_750x750.jpg（保留已有的 _750x750 等不动）
    function toHd(u) {
        if (!u) return u;
        return u
            .replace(/_\d{2,3}x\d{2,3}\.jpg/i, '_750x750.jpg')
            .replace(/_\d{2,3}x\d{2,3}\.png/i, '_750x750.png')
            .replace(/_sum\.jpg/i, '.jpg')
            .replace(/\._sum\.webp/i, '.webp');
    }

    function add(u) {
        if (!u) return;
        u = String(u).trim();
        // 去掉协议相对、data/blob 链接
        if (u.indexOf('//') === 0) u = 'https:' + u;
        if (u.indexOf('http') !== 0) return;
        if (u.indexOf('blob:') === 0 || u.indexOf('data:') === 0) return;
        var hd = toHd(u);
        if (!seen[hd]) {
            seen[hd] = 1;
            if (isTaobaoMedia(hd)) {
                urls.push(hd);
            }
        }
    }

    try {
        // ---- 1) PC 详情页主图（老版淘宝/天猫 PC 站）----
        var pcSelectors = [
            '#J_ImgBooth',
            '#J_ImgBooth_Image',
            '#J_ZoomMain img',
            '.tb-main-pic img',
            '.tb-pic img',
            '.main-image img',
            '.tb-gallery img',
            '.pic-list img'
        ];
        for (var s = 0; s < pcSelectors.length; s++) {
            var pcList = document.querySelectorAll(pcSelectors[s]);
            for (var i = 0; i < pcList.length; i++) {
                var el = pcList[i];
                add(el.src);
                add(el.getAttribute('data-src'));
                add(el.getAttribute('data-ks-lazyload'));
                add(el.getAttribute('data-original'));
            }
        }

        // ---- 2) 移动 H5 详情页（item.taobao.com 移动端 / 天猫 H5）----
        var h5Selectors = [
            '.detail-gallery img',
            '.gallery img',
            '.slider img',
            '.pic-list img',
            '.main-img img',
            '#page img',
            '.img-list img',
            '.detail-img img',
            '.tb-detail-img img',
            '.image-list img'
        ];
        for (var h = 0; h < h5Selectors.length; h++) {
            var h5List = document.querySelectorAll(h5Selectors[h]);
            for (var j = 0; j < h5List.length; j++) {
                var hel = h5List[j];
                add(hel.src);
                add(hel.getAttribute('data-src'));
                add(hel.getAttribute('data-original'));
                add(hel.getAttribute('data-ks-lazyload'));
            }
        }

        // ---- 3) 视频（主视频 / 短视频 / 直播回放）----
        var videoSelectors = [
            'video',
            '#J_Video video',
            '.tb-video video',
            '.detail-video video',
            '.video-player video',
            '.tb-main-video video'
        ];
        for (var v = 0; v < videoSelectors.length; v++) {
            var vList = document.querySelectorAll(videoSelectors[v]);
            for (var k = 0; k < vList.length; k++) {
                var vel = vList[k];
                add(vel.src);
                add(vel.getAttribute('data-src'));
                // <source> 子节点
                var sources = vel.querySelectorAll('source');
                for (var m = 0; m < sources.length; m++) {
                    add(sources[m].src);
                    add(sources[m].getAttribute('data-src'));
                }
                // poster 有时是首帧大图，也收进来
                add(vel.getAttribute('poster'));
            }
        }

        // 嗅探 cloud.video.taobao.com 规律直链（部分页面会在脚本里暴露 videoId）
        try {
            var scripts = document.querySelectorAll('script');
            for (var sc = 0; sc < scripts.length; sc++) {
                var txt = scripts[sc].textContent || '';
                var ids = txt.match(/videoId['":\s=]+['"]?(\d+)/i) ||
                          txt.match(/['"](https?:)?//cloud\.video\.taobao\.com/[^'"]+/gi);
                if (ids) {
                    for (var idxx = 0; idxx < ids.length; idxx++) {
                        var raw = ids[idxx];
                        var vid = raw.match(/(\d{6,})/);
                        if (vid) {
                            add('https://cloud.video.taobao.com/play/u/1/e/1/t/' + vid[1] + '.mp4');
                        }
                    }
                }
                // 直接出现的 cloud.video 直链
                var directVids = txt.match(/https?:\/\/cloud\.video\.taobao\.com[^'"\s]+/gi);
                if (directVids) {
                    for (var dv = 0; dv < directVids.length; dv++) add(directVids[dv]);
                }
            }
        } catch (e) {}

        // ---- 3.5) 主图数组（PC 详情页内联 auctionImages JSON）----
        // PC 详情页主图不直接放在 <img> 上，而是内联在 <script> 的 auctionImages:["..."] 里（与 HTTP 直解同一数据源）。
        try {
            var aScripts = document.querySelectorAll('script');
            for (var asx = 0; asx < aScripts.length; asx++) {
                var aText = aScripts[asx].textContent || '';
                var am = aText.match(/auctionImages["']?\s*:\s*\[(.*?)\]/s);
                if (am) {
                    var arrStr = am[1];
                    var items = arrStr.match(/"([^"]+)"/g);
                    if (items) {
                        for (var ai = 0; ai < items.length; ai++) {
                            var u = items[ai].replace(/^"/, '').replace(/"$/, '');
                            add(u);
                        }
                    }
                }
            }
        } catch (e) {}

        // ---- 4) 兜底：扫描页面所有 <img>（含懒加载 data-src），只要阿里系 CDN 都收 ----
        var allImgs = document.querySelectorAll('img');
        for (var a = 0; a < allImgs.length; a++) {
            var aimg = allImgs[a];
            var candidate = aimg.src || aimg.getAttribute('src') ||
                            aimg.getAttribute('data-src') ||
                            aimg.getAttribute('data-original') ||
                            aimg.getAttribute('data-ks-lazyload') ||
                            aimg.getAttribute('data-lazyload');
            if (candidate && isTaobaoMedia(candidate)) {
                add(candidate);
            }
            // 背景图
            try {
                var bg = aimg.style && aimg.style.backgroundImage;
                if (bg && bg.indexOf('url(') === 0) {
                    var bgUrl = bg.replace(/^url\(['"]?/, '').replace(/['"]?\)$/, '');
                    if (isTaobaoMedia(bgUrl)) add(bgUrl);
                }
            } catch (e) {}
        }

        // ---- 5) 商品标题 / 描述 ----
        var titleSel = document.querySelector('#J_Title h1, .tb-main-title, .title, h1, .item-title, .product-title');
        if (titleSel) {
            content.title = (titleSel.innerText || titleSel.textContent || '').trim();
        } else if (document.title) {
            content.title = document.title.replace(/[-_|].*[淘天商城].*$/i, '').trim();
        }
        var descSel = document.querySelector('#J_DetailMeta, .tb-item-info, .desc-area, .detail-desc, .product-info');
        if (descSel) {
            content.desc = (descSel.innerText || descSel.textContent || '').trim().substring(0, 200);
        }
        content.content = content.title + (content.title && content.desc ? '\n' : '') + content.desc;

        console.log('=== Taobao Extractor === urls=' + urls.length + ' title="' + content.title + '"');
        return { urls: urls, content: content };
    } catch (e) {
        console.error('Taobao extractor error: ' + e);
        return { urls: [], content: { content: '' } };
    }
})()

/**
 * Douyin Video/Image Extractor (WebView-first)
 *
 * 背景：抖音分享页（v.douyin.com / iesdouyin.com/share/video）现在是 SPA。
 *   - window._ROUTER_DATA 仅保留 SSR 壳（loaderData["video_(id)/page"] 里已无 videoInfoRes），
 *     真实播放数据由页面 JS 通过 XHR/fetch 异步拉取，可能落到任意嵌套对象，也可能根本不进全局变量。
 *   - 因此本提取器：(1) 递归扫描已知全局（_ROUTER_DATA / RENDER_DATA / __INITIAL_STATE__ / __APOLLO_STATE__）
 *     中嵌套的 play_addr.url_list / photoUrl；(2) 兼容旧结构 videoInfoRes.item_list；(3) 兜底扫描
 *     <video> 元素（currentSrc/src 与 <source>）。
 *
 * 关键修复（v1.0.48）：把「帖子图集图片」与「视频」分成两条独立列表返回，避免图文帖被误当视频下。
 *   - 图片（image_urls）：只来自作品自身的 images[] 图集字段（抖音图文/图集帖才有）。
 *   - 视频（urls）：play_addr / photoUrl / coverUrl（视频封面，归入视频侧，绝不混入图片）/ <video>。
 *   这样调用方可以明确判断：image_urls 非空 ⟺ 这是图文/图集帖，应下载全部图片；否则才是视频帖。
 *
 * 返回：{ urls: [视频直链...], image_urls: [图集图片...], content: {...} }
 */
(function () {
    try {
        var videoUrls = [];
        var imageUrls = [];
        var seenV = {};
        var seenI = {};
        // 视频直链（play_addr / photoUrl / coverUrl / <video>）
        function addVideo(u) {
            if (!u || typeof u !== 'string') return;
            u = String(u).trim();
            if (u.indexOf('//') === 0) u = 'https:' + u;
            if (u.indexOf('http') !== 0) return;
            if (u.indexOf('blob:') === 0 || u.indexOf('data:') === 0) return;
            if (u.endsWith('.m3u8')) return;
            u = u.replace('playwm', 'play'); // 去水印
            if (!seenV[u]) { seenV[u] = 1; videoUrls.push(u); }
        }
        // 帖子图集图片（仅 images[]，绝不混入 coverUrl）
        function addImage(u) {
            if (!u || typeof u !== 'string') return;
            u = String(u).trim();
            if (u.indexOf('//') === 0) u = 'https:' + u;
            if (u.indexOf('http') !== 0) return;
            if (u.indexOf('blob:') === 0 || u.indexOf('data:') === 0) return;
            if (u.endsWith('.m3u8')) return;
            if (!seenI[u]) { seenI[u] = 1; imageUrls.push(u); }
        }
        // 从图集图片对象中取直链：抖音真实结构为 images[i].url_list[0]（多 CDN 候选），
        // 旧结构可能用 .url；image_post_info 用 origin_image/display_image.url_list。统一在此兼容。
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

        var content = { content: '', title: '', desc: '' };

        // 1) 递归扫描已知全局，抠出嵌套的播放地址 / 图集图片
        function walk(node, depth) {
            if (!node || depth > 14 || typeof node !== 'object') return;
            if (node.play_addr && node.play_addr.url_list && node.play_addr.url_list.length) {
                addVideo(String(node.play_addr.url_list[0]));
            }
            if (Array.isArray(node.url_list) && node.url_list.length && (node.play_addr || node.video)) {
                for (var i = 0; i < node.url_list.length; i++) addVideo(String(node.url_list[i]));
            }
            if (typeof node.photoUrl === 'string') addVideo(node.photoUrl);
            // coverUrl 是视频封面，归入视频侧，绝不能当帖子图片下载
            if (typeof node.coverUrl === 'string') addVideo(node.coverUrl);
            // 帖子图集图片：只认 images[]（抖音图文/图集帖才有），单独成列表
            if (node.images && Array.isArray(node.images)) {
                for (var j = 0; j < node.images.length; j++) {
                    var it = node.images[j];
                    var u = getImageUrl(it);
                    if (u) addImage(u);
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
                                addVideo(String(d.video.play_addr.url_list[0]));
                            }
                            if (d.video.bit_rate) {
                                for (var b = 0; b < d.video.bit_rate.length; b++) {
                                    var br = d.video.bit_rate[b];
                                    if (br && br.play_addr && br.play_addr.url_list && br.play_addr.url_list.length) {
                                        addVideo(String(br.play_addr.url_list[0]));
                                    }
                                }
                            }
                        }
                        // 旧结构图集：images[] 或 image_post_info.image_list[]
                        if (d) {
                            if (d.images && Array.isArray(d.images)) {
                                for (var im = 0; im < d.images.length; im++) {
                                    var di = d.images[im];
                                    var du = getImageUrl(di);
                                    if (du) addImage(du);
                                }
                            }
                            if (d.image_post_info && d.image_post_info.image_list) {
                                var ipi = d.image_post_info.image_list;
                                for (var pi = 0; pi < ipi.length; pi++) {
                                    var p = ipi[pi];
                                    var pu = (p && typeof p === 'object')
                                        ? (p.origin_image && p.origin_image.url_list && p.origin_image.url_list[0])
                                          || (p.display_image && p.display_image.url_list && p.display_image.url_list[0])
                                          || (p.url_list && p.url_list[0])
                                        : null;
                                    if (pu) addImage(String(pu));
                                }
                            }
                        }
                        if (d && d.desc) content.title = d.desc;
                    }
                }
            }
        }

        // 3) 兜底：扫描页面 <video> 元素（视频，归入视频侧）
        var vids = document.querySelectorAll('video');
        for (var m = 0; m < vids.length; m++) {
            var v = vids[m];
            addVideo(v.currentSrc);
            addVideo(v.src);
            var ss = v.querySelectorAll('source');
            for (var n = 0; n < ss.length; n++) {
                addVideo(ss[n].src);
            }
        }

        // 4) 兜底：仅当图文/视频都未抓到时，扫描页面 <img> 图集。
        //    抖音 web 端图文帖图片常是懒加载 <img> 直链（而非 JS 状态的 images[]）。
        //    只取图集 CDN 域名、排除头像/表情（avatar / avt），避免混入作者头像与视频封面。
        if (imageUrls.length === 0 && videoUrls.length === 0) {
            var imgs = document.querySelectorAll('img');
            for (var p = 0; p < imgs.length; p++) {
                var isrc = imgs[p].currentSrc || imgs[p].src;
                if (!isrc) continue;
                var ils = String(isrc).toLowerCase();
                if (ils.indexOf('douyinpic.com') === -1 && ils.indexOf('byteimg.com') === -1) continue;
                if (ils.indexOf('avatar') !== -1 || ils.indexOf('tos-cn-i-avt') !== -1) continue;
                addImage(isrc);
            }
        }

        if (content.title && !content.content) content.content = content.title;
        console.log('=== Douyin Extractor === videos=' + videoUrls.length + ' images=' + imageUrls.length + ' title="' + content.title + '"');
        return { urls: videoUrls, image_urls: imageUrls, content: content };
    } catch (e) {
        console.error('Douyin extractor error: ' + e);
        return { urls: [], image_urls: [], content: { content: '' } };
    }
})()

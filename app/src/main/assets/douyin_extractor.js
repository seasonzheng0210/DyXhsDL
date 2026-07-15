/**
 * Douyin Video Extractor (WebView-first)
 * Purpose: Read window._ROUTER_DATA injected by Douyin's web page and extract
 * the no-watermark play URL, plus the video description.
 * Returns the same shape as xhs_extractor.js: { urls: [...], content: {...} }
 */

(function() {
    try {
        var urls = [];
        var seen = {};
        function add(u) {
            if (u && typeof u === 'string' && u.indexOf('http') === 0 && !seen[u]) {
                seen[u] = 1;
                urls.push(u);
            }
        }

        var content = { content: '', title: '', desc: '' };
        var rd = window._ROUTER_DATA;

        if (rd && rd.loaderData) {
            var ld = rd.loaderData;
            var pageKey = null;
            var keys = Object.keys(ld);
            for (var i = 0; i < keys.length; i++) {
                var k = keys[i];
                if ((k.indexOf('video_') === 0 || k.indexOf('note_') === 0) && k.indexOf('/page') > 0) {
                    pageKey = k;
                    break;
                }
            }

            if (pageKey && ld[pageKey] && ld[pageKey].videoInfoRes) {
                var itemList = ld[pageKey].videoInfoRes.item_list;
                if (itemList && itemList.length > 0) {
                    var data = itemList[0];
                    var video = data.video;
                    if (video) {
                        // Primary play address (replace playwm -> play for no watermark)
                        if (video.play_addr && video.play_addr.url_list && video.play_addr.url_list.length > 0) {
                            add(String(video.play_addr.url_list[0]).replace('playwm', 'play'));
                        }
                        // Fallback: highest bit-rate variant
                        if (video.bit_rate) {
                            for (var b = 0; b < video.bit_rate.length; b++) {
                                var br = video.bit_rate[b];
                                if (br && br.play_addr && br.play_addr.url_list && br.play_addr.url_list.length > 0) {
                                    add(String(br.play_addr.url_list[0]).replace('playwm', 'play'));
                                }
                            }
                        }
                    }
                    var desc = data.desc || '';
                    content.title = desc;
                    content.desc = desc;
                    content.content = desc;
                }
            }
        }

        console.log('=== Douyin Extractor === urls=' + urls.length + ' content="' + content.content + '"');
        return { urls: urls, content: content };
    } catch (e) {
        console.error('Douyin extractor error: ' + e);
        return { urls: [], content: { content: '' } };
    }
})()

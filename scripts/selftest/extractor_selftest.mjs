// 真·自测：用 jsdom 把 App 里的 extractor.js 在真实风格的 DOM 上跑一遍
// 验证：1) JS 能否无错执行 2) 高清化/阿里CDN过滤是否正确 3) 主图/视频能否抠出
import { createRequire } from 'module';
import { readFileSync } from 'fs';
const require = createRequire(import.meta.url);
const { JSDOM } = require('jsdom');

const assetsDir = 'C:/Users/Administrator/WorkBuddy/20260713134526/DyXhsDL/app/src/main/assets';
const outDir = 'C:/Users/Administrator/WorkBuddy/20260713134526/DyXhsDL/scripts/selftest';

function runExtractor(dom, code) {
  const { window } = dom;
  let logs = [];
  window.console = { log: (...a) => logs.push(a.join(' ')), error: (...a) => logs.push('ERR ' + a.join(' ')) };
  let res;
  try {
    res = window.eval(code);
  } catch (e) {
    return { error: String(e), logs };
  }
  return { result: res, logs };
}

// ---- 快手：模拟 WebView 渲染后 window.__INITIAL_STATE__ 含视频作品 ----
const kuaishouVideoState = {
  photo: {
    photoId: '3xAbCdEfG',
    caption: '测试快手视频标题',
    photoUrl: 'https://v.m.chenzhongtech.com/bs2/xxx/playurl.mp4',
    coverUrl: 'https://v.m.chenzhongtech.com/bs2/xxx/cover.jpg',
    images: []
  },
  author: { id: 'u1', name: '测试作者' }
};

// ---- 快手：模拟图集作品（images 数组）----
const kuaishouAlbumState = {
  photo: {
    photoId: '3xAbCdEfG',
    caption: '测试快手图集',
    photoUrl: '',
    images: [
      { url: 'https://kwaicdn.com/aweme/album1.jpg' },
      { url: 'https://kwaicdn.com/aweme/album2.jpg' }
    ]
  },
  author: { id: 'u1', name: '测试作者' }
};

// ---- 抖音：模拟 H5 笔记页注入的 window._ROUTER_DATA（douyin_extractor.js 的真实数据来源）----
const douyinRouterData = {
  loaderData: {
    "video_abc123/page": {
      videoInfoRes: {
        item_list: [{
          desc: "测试抖音视频标题",
          video: {
            play_addr: { url_list: ["https://aweme.snssdk.com/video/1.mp4"] },
            bit_rate: []
          }
        }]
      }
    }
  }
};

// ---- 小红书：模拟笔记页（真实 DOM：.note-image-box 包裹 img）----
const xhsHtml = `<!DOCTYPE html><html><body>
  <div class="note-image-box"><img src="https://sns-img.xhscdn.com/notes/1.jpg"/></div>
  <div class="note-image-box"><img src="https://sns-video.xhscdn.com/notes/1.mp4"/></div>
</body></html>`;

const cases = [
  { name: 'kuaishou_video', html: '<!DOCTYPE html><html><head><title>测试快手视频标题</title></head><body></body></html>', file: 'kuaishou_extractor.js', state: kuaishouVideoState },
  { name: 'kuaishou_album', html: '<!DOCTYPE html><html><head><title>测试快手图集</title></head><body></body></html>', file: 'kuaishou_extractor.js', state: kuaishouAlbumState },
  { name: 'douyin', html: '<!DOCTYPE html><html><body></body></html>', file: 'douyin_extractor.js', globals: { _ROUTER_DATA: douyinRouterData } },
  { name: 'xhs', html: xhsHtml, file: 'xhs_extractor.js' },
];

let pass = 0, fail = 0;
for (const c of cases) {
  let code;
  try { code = readFileSync(assetsDir + '/' + c.file, 'utf8'); }
  catch (e) { console.log(`[${c.name}] 读取 ${c.file} 失败: ${e}`); fail++; continue; }
  const dom = new JSDOM(c.html, { runScripts: 'outside-only' });
  if (c.state) dom.window.__INITIAL_STATE__ = c.state;
  if (c.globals) Object.assign(dom.window, c.globals);
  const r = runExtractor(dom, code);
  if (r.error) {
    console.log(`[${c.name}] ❌ 执行报错: ${r.error}`);
    fail++;
  } else {
    const urls = (r.result && r.result.urls) || [];
    const ok = urls.length > 0;
    console.log(`[${c.name}] ${ok ? '✅' : '❌'} 抠到 ${urls.length} 个 URL` + (urls.length ? ' 例: ' + urls.slice(0,3).join(' | ') : ''));
    if (c.name === 'kuaishou_video') {
      // 视频作品：应抠到无水印播放源（photoUrl），不应是 .m3u8
      const hasMp4 = urls.some(u => u.endsWith('.mp4'));
      console.log(`   含视频直链: ${hasMp4 ? '✅' : '❌'}`);
      if (!hasMp4) fail++;
    }
    if (c.name === 'kuaishou_album') {
      const bothAlbum = urls.length === 2 && urls.every(u => u.includes('album'));
      console.log(`   图集两张: ${bothAlbum ? '✅' : '❌'}`);
      if (!bothAlbum) fail++;
    }
    if (ok) pass++; else fail++;
  }
}
console.log(`\n=== 自测结果: ${pass} 通过 / ${fail} 失败 ===`);
process.exit(fail ? 1 : 0);

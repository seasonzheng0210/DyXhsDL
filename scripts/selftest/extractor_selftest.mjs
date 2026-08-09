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

// ---- 淘宝：模拟“已登录 H5 详情页”真实 DOM 夹具 ----
const taobaoLoggedInHtml = `<!DOCTYPE html><html><head><title>测试商品</title></head><body>
  <div class="detail-gallery">
    <img src="https://img.alicdn.com/imgextra/i1/123/O1CN01abc_60x60.jpg" data-original="https://img.alicdn.com/imgextra/i1/123/O1CN01abc_750x750.jpg"/>
    <img src="https://gd2.alicdn.com/imgextra/i2/123/O1CN01def_100x100.jpg"/>
    <img src="https://img.alicdn.com/imgextra/i3/123/O1CN01ghi_sum.jpg"/>
  </div>
  <div class="detail-desc">
    <img src="https://img.alicdn.com/imgextra/i4/123/desc1_60x60.jpg" data-originalsrc="https://img.alicdn.com/imgextra/i4/123/desc1.jpg"/>
    <img src="https://example.com/notalicdn.png"/>
  </div>
  <video src="https://cloud.video.taobao.com/play/u/1/e/1/t/926895986130.mp4" poster="https://img.alicdn.com/imgextra/i5/123/poster_60x60.jpg"></video>
</body></html>`;

// ---- 抖音：模拟 H5 笔记页（图片在 <img>，视频在 video）----
const douyinHtml = `<!DOCTYPE html><html><body>
  <img src="https://p3-pc-sign.douyinpic.com/aweme/1.jpeg"/>
  <video src="https://aweme.snssdk.com/video/1.mp4"></video>
</body></html>`;

// ---- 小红书：模拟笔记页 ----
const xhsHtml = `<!DOCTYPE html><html><body>
  <img src="https://sns-img.xhscdn.com/notes/1.jpg"/>
  <img src="https://sns-video.xhscdn.com/notes/1.mp4"/>
</body></html>`;

// ---- 淘宝：模拟"渲染后的登录墙"（模拟器真机实测：匿名页只有 200x200 logo 占位图）----
// 关键：logo 恰好落在容器 selector(.gallery/.pic-list) 里也要被小图过滤掉 → 0 图 → App 提示登录
const taobaoLoginWallRenderedHtml = `<!DOCTYPE html><html><head><title>商品详情页</title></head><body>
  <div class="gallery">
    <img src="https://gw.alicdn.com/tfs/TB1QZN.CYj1gK0jSZFuXXcrHpXa-200-200.png"/>
    <img src="https://gw.alicdn.com/tfs/TB1logofooter-100-100.png" data-src="https://gw.alicdn.com/tfs/TB1logofooter-100-100.png"/>
  </div>
  <div class="pic-list">
    <img src="https://gw.alicdn.com/tfs/TB1icon-60-60.png"/>
  </div>
</body></html>`;

// ---- 天猫 PC（squirrel-gather 实战 selector 风格 DOM）：thumbnailPic 主图 + videox 视频 + descV8 详情图 ----
const tmallPcHtml = `<!DOCTYPE html><html><head><title>王俊凯同款骆驼休闲鞋-tmall.com天猫</title></head><body>
  <div class="mainPicWrap">
    <img class="thumbnailPic thumbnailPic-1" src="https://img.alicdn.com/imgextra/i1/123/tb1main1_430x430.jpg"/>
    <img class="thumbnailPic thumbnailPic-2" src="https://gd3.alicdn.com/imgextra/i2/123/tb1main2_430x430.jpg"/>
  </div>
  <div class="switchTabsWrap" data-appeared="true">
    <div class="switchTabsItem">主图</div>
    <div class="switchTabsItem">视频</div>
    <video id="videox-video-el" class="videox-video" src="https://cloud.video.taobao.com/play/u/1/e/1/t/926895986130.mp4"></video>
  </div>
  <div class="descV8-richtext">
    <img data-name="singleImage" class="descV8-singleImage-image lazyload" src="https://img.alicdn.com/imgextra/i6/123/desc1.jpg"/>
    <img align="absmiddle" class="lazyload" src="https://img.alicdn.com/imgextra/i7/123/desc2_60x60.jpg"/>
  </div>
  <img class="valueItemImg" placeholder="xxx" src="https://img.alicdn.com/imgextra/i8/123/sku_red_90x90q30.jpg_.webp"/>
</body></html>`;

const cases = [
  { name: 'taobao_logged_in', html: taobaoLoggedInHtml, file: 'taobao_extractor.js' },
  { name: 'tmall_pc_squirrel', html: tmallPcHtml, file: 'taobao_extractor.js' },
  { name: 'taobao_anonymous_shell', html: readFileSync(outDir + '/tb_anon.html', 'utf8'), file: 'taobao_extractor.js' },
  { name: 'taobao_loginwall_logo', html: taobaoLoginWallRenderedHtml, file: 'taobao_extractor.js' },
  { name: 'douyin', html: douyinHtml, file: 'douyin_extractor.js' },
  { name: 'xhs', html: xhsHtml, file: 'xhs_extractor.js' },
];

let pass = 0, fail = 0;
for (const c of cases) {
  let code;
  try { code = readFileSync(assetsDir + '/' + c.file, 'utf8'); }
  catch (e) { console.log(`[${c.name}] 读取 ${c.file} 失败: ${e}`); fail++; continue; }
  const dom = new JSDOM(c.html, { runScripts: 'outside-only' });
  const r = runExtractor(dom, code);
  if (r.error) {
    console.log(`[${c.name}] ❌ 执行报错: ${r.error}`);
    fail++;
  } else {
    const urls = (r.result && r.result.urls) || [];
    const ok = (c.name === 'taobao_anonymous_shell' || c.name === 'taobao_loginwall_logo') ? urls.length === 0 : urls.length > 0;
    console.log(`[${c.name}] ${ok ? '✅' : '❌'} 抠到 ${urls.length} 个 URL` + (urls.length ? ' 例: ' + urls.slice(0,3).join(' | ') : ''));
    if (c.name === 'taobao_logged_in') {
      // 校验高清化 + 过滤
      const hdOk = urls.every(u => !/_\d{2,3}x\d{2,3}\.jpg/.test(u) || u.includes('_750x750'));
      const filteredOk = urls.every(u => /alicdn\.com|taobao\.com|tmall\.com/.test(u));
      console.log(`   高清化正确: ${hdOk ? '✅':'❌'}  仅阿里CDN: ${filteredOk ? '✅':'❌'}`);
      if (!hdOk || !filteredOk) fail++;
    }
    if (ok) pass++; else fail++;
  }
}
console.log(`\n=== 自测结果: ${pass} 通过 / ${fail} 失败 ===`);
process.exit(fail ? 1 : 0);

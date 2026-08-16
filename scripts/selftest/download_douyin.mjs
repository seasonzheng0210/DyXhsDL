// download_douyin.mjs v6 —— 请求拦截减负 + 代理 + 抓抖音「自己签过名」的 play_addr 直链并下载。
//
// 沙箱关键约束（已逐一定位）：
//  1. 出网必须经本地代理 127.0.0.1:7897（Chrome 默认不读 HTTPS_PROXY，须 --proxy-server）。
//  2. 抖音静态 CDN 与 API 必须经代理，否则 status:0 连接失败。
//  3. Chrome 经代理并发几十请求会 ERR_INSUFFICIENT_RESOURCES / ERR_EMPTY_RESPONSE → 关键
//     aweme/detail API 被压垮，拿不到 play_addr。故用请求拦截 abort 掉图片/字体/样式/第三方埋点，
//     只放行文档、脚本、douyin/byte 域的 API 与播放器 JS，给关键 API 让路。
import { createRequire } from 'module';
import { readFileSync, writeFileSync, mkdirSync } from 'fs';
const require = createRequire('C:/Users/Administrator/.workbuddy/binaries/node/workspace/node_modules/playwright/index.js');
const { chromium } = require('playwright');

const assetsDir = 'C:/Users/Administrator/WorkBuddy/20260713134526/DyXhsDL/app/src/main/assets';
const abogusJs = readFileSync(assetsDir + '/abogus.js', 'utf8');
const injectJs = readFileSync(assetsDir + '/web_inject.js', 'utf8');
const DESKTOP_UA = 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36';
const TARGET = process.argv[2] || 'https://v.douyin.com/Zv2eFSyDXLc/';
const OUT_DIR = 'C:/Users/Administrator/WorkBuddy/20260713134526/DyXhsDL/downloads';
mkdirSync(OUT_DIR, { recursive: true });

function isMp4(u) {
  return /(\.mp4|aweme\.snssdk\.com|douyinvod|bytecdn|tiktokcdn)/i.test(u) && !/\.m3u8/.test(u);
}
function allowedHost(u) {
  try {
    const h = new URL(u).hostname;
    return /douyin\.com$/.test(h) || /douyinstatic\.com$/.test(h) ||
           /byte(dance)?\.(com|net|cc)$/.test(h) || /bytedance\.com$/.test(h) ||
           /tiktokcdn\.com$/.test(h) || /snssdk\.com$/.test(h);
  } catch (e) { return false; }
}

async function tryDownload(context, u) {
  try {
    const cookies = await context.cookies();
    const cookieHeader = cookies.map((c) => c.name + '=' + c.value).join('; ');
    const r = await context.request.fetch(u, {
      headers: {
        'Referer': 'https://www.douyin.com/',
        'User-Agent': DESKTOP_UA,
        'Cookie': cookieHeader,
        'Range': 'bytes=0-',
      },
      timeout: 120000,
    });
    console.log('  -> status', r.status(), 'cl', r.headers()['content-length'], 'ct', r.headers()['content-type']);
    if (r.status() === 206 || r.ok()) {
      const buf = Buffer.from(await r.body());
      if (buf.length > 20000) {
        const fn = OUT_DIR + '/douyin_' + Date.now() + '.mp4';
        writeFileSync(fn, buf);
        console.log('[DOWNLOADED]', fn, buf.length, 'bytes');
        return fn;
      } else { console.log('  -> 文件过小 (' + buf.length + ')，疑似门控/空响应'); }
    }
  } catch (e) { console.log('[download err]', e.message.slice(0, 160)); }
  return null;
}

async function attemptOnce(page, context, attemptNo, seen) {
  console.log('\n##### ATTEMPT ' + attemptNo + ' #####');
  let resp = null;
  try { resp = await page.goto(TARGET, { waitUntil: 'domcontentloaded', timeout: 45000 }); }
  catch (e) { console.log('[goto err]', e.message.slice(0, 200)); }
  console.log('[http]', resp ? resp.status() : '?', 'final=', page.url().slice(0, 120));

  let hydrated = false;
  for (let i = 0; i < 25; i++) {
    await page.waitForTimeout(2000);
    const s = await page.evaluate(() => ({
      caps: (window.__CAP || []).slice(),
      videos: document.querySelectorAll('video').length,
      body: (document.body ? document.body.innerText.replace(/\s+/g, ' ').slice(0, 30) : ''),
    })).catch(() => ({ caps: [], videos: 0, body: '' }));
    s.caps.forEach((u) => seen.add(u));
    if (s.caps.length > 0) { hydrated = true; console.log('[caps found] n=' + s.caps.length + ' @' + ((i + 1) * 2) + 's'); break; }
    if (i % 5 === 4) console.log('[wait] loading, body="' + s.body + '"');
  }
  await page.waitForTimeout(6000);
  const finalCaps = await page.evaluate(() => (window.__CAP || []).slice()).catch(() => []);
  finalCaps.forEach((u) => seen.add(u));

  const state = await page.evaluate(() => ({
    title: (document.title || '').slice(0, 40),
    videos: document.querySelectorAll('video').length,
    tokens: Object.keys(window.__dyTokens || {}),
  })).catch(() => ({}));
  console.log('[state]', JSON.stringify(state));
  const all = Array.from(seen);
  const mp4caps = all.filter(isMp4);
  console.log('[caps accumulated]', all.length, ' [mp4 candidates]', mp4caps.length);
  mp4caps.slice(0, 12).forEach((u, i) => console.log('  ' + i + ': ' + u.slice(0, 240)));
  for (const u of mp4caps) {
    const fn = await tryDownload(context, u);
    if (fn) return fn;
  }
  return null;
}

async function main() {
  console.log('=== target:', TARGET);
  const browser = await chromium.launch({
    channel: 'chrome', headless: true,
    args: [
      '--disable-blink-features=AutomationControlled',
      '--no-sandbox', '--disable-http2', '--disable-ipv6',
      '--proxy-server=http://127.0.0.1:7897',
    ],
  });
  const context = await browser.newContext({
    userAgent: DESKTOP_UA, locale: 'zh-CN', timezoneId: 'Asia/Shanghai',
    viewport: { width: 1280, height: 800 },
  });
  const page = await context.newPage();

  // 请求拦截：减负，给关键 API 让路
  await page.route('**/*', (route) => {
    const req = route.request();
    const rt = req.resourceType();
    const u = req.url();
    // 放行：文档、脚本、xhr/fetch、以及 douyin/byte 域的一切（含 API 与播放器 JS）
    if (rt === 'document' || rt === 'script' || rt === 'xhr' || rt === 'fetch') return route.continue();
    if (allowedHost(u)) {
      // 同域内仍 abort 掉图片/字体/样式/媒体，省套接字；保留脚本/API（上面已放行）
      if (rt === 'image' || rt === 'font' || rt === 'stylesheet' || rt === 'media') return route.abort();
      return route.continue();
    }
    // 第三方埋点/广告/统计一律 abort
    return route.abort();
  });

  page.on('pageerror', (e) => console.log('[pageerror]', (e && e.message ? e.message : String(e)).slice(0, 100)));
  await page.addInitScript((ab) => { try { new Function(ab)(); } catch (e) {} }, abogusJs);
  await page.addInitScript((inj) => {
    window.__CAP = [];
    window.AndroidVideoBridge = { onVideoUrl: (u) => { try { window.__CAP.push(u); } catch (e) {} } };
    try { new Function(inj)(); } catch (e) {}
  }, injectJs);

  let fn = null;
  const seen = new Set();
  for (let i = 1; i <= 5 && !fn; i++) {
    fn = await attemptOnce(page, context, i, seen);
    if (!fn && i < 5) { console.log('[retry] 清理 cookie 重载'); try { await context.clearCookies(); } catch (e) {} }
  }
  await browser.close();
  if (fn) console.log('\n=== SUCCESS:', fn, '===');
  else console.log('\n=== FAILED: 沙箱仍未能捕获可下载直链 ===');
}
main().catch((e) => console.log('[fatal]', e.message));

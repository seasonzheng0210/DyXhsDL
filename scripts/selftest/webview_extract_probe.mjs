// webview_extract_probe.mjs —— 用真实 Chromium 跑抖音/快手分享页，注入 App 内的
// web_inject.js（XHR/fetch 钩子）+ 对应 extractor.js，验证能否抠到播放地址。
// 用法：NODE_PATH=<workspace node_modules> node scripts/selftest/webview_extract_probe.mjs
import { createRequire } from 'module';
import { readFileSync } from 'fs';
const require = createRequire(import.meta.url);
const { chromium } = require('playwright');

const assetsDir = 'C:/Users/Administrator/WorkBuddy/20260713134526/DyXhsDL/app/src/main/assets';
const injectJs = readFileSync(assetsDir + '/web_inject.js', 'utf8');
const dyExt = readFileSync(assetsDir + '/douyin_extractor.js', 'utf8');
const ksExt = readFileSync(assetsDir + '/kuaishou_extractor.js', 'utf8');

const MOBILE_UA = 'Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36';
const DESKTOP_UA = 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36';

async function testPage(name, url, ua, extractorJs, viewport) {
  console.log(`\n========== [${name}] ${url} ==========`);
  let browser;
  try {
    browser = await chromium.launch({ channel: 'chrome', headless: true });
  } catch (e) {
    console.log(`  [launch err] ${e.message}`);
    return;
  }
  try {
    const context = await browser.newContext({
      userAgent: ua, locale: 'zh-CN', timezoneId: 'Asia/Shanghai', viewport,
    });
    const page = await context.newPage();
    const bridge = [];
    const network = [];
    await page.addInitScript((inj) => {
      window.__CAP = [];
      window.AndroidVideoBridge = { onVideoUrl: (u) => { window.__CAP.push(u); } };
      new Function(inj)();
    }, injectJs);
    page.on('request', (r) => {
      const u = r.url();
      if (/(douyinvod|aweme\.snssdk|bytecdn|tiktokcdn|kwaicdn|chenzhongtech|gifshow|kwai|\.mp4|\.m3u8)/.test(u)) {
        network.push(u);
      }
    });
    page.on('pageerror', (e) => console.log(`  [pageerror] ${e}`));
    page.on('console', (m) => {
      const t = m.text();
      if (t.includes('dy inject') || t.includes('Extractor') || t.includes('BRIDGE')) console.log(`  [console] ${t}`);
    });

    let resp = null;
    try {
      resp = await page.goto(url, { waitUntil: 'domcontentloaded', timeout: 40000 });
    } catch (e) {
      console.log(`  [goto] ${e.message.slice(0, 200)}`);
    }
    console.log(`  [http] ${resp ? resp.status() : '?'} final=${page.url().slice(0, 120)}`);
    // 等待 SPA 水合 + 视频加载
    await page.waitForTimeout(16000);

    const state = await page.evaluate(() => ({
      title: document.title,
      videos: document.querySelectorAll('video').length,
      rdr: typeof window._ROUTER_DATA !== 'undefined',
      apollo: typeof window.__APOLLO_STATE__ !== 'undefined',
      init: typeof window.__INITIAL_STATE__ !== 'undefined',
      bodyLen: (document.body ? document.body.innerText.length : 0),
      bodyText: (document.body ? document.body.innerText.slice(0, 200) : ''),
      hasCaptcha: /验证|captcha|安全验证|verify/i.test(document.body ? document.body.innerText : ''),
      cap: window.__CAP.slice(0, 10),
    }));
    console.log('  [state] ' + JSON.stringify(state, null, 2).slice(0, 1200));

    // 跑 App 内的提取器
    let ext = null;
    try { ext = await page.evaluate(extractorJs); } catch (e) { console.log(`  [extractor err] ${e.message}`); }
    console.log(`  [extractor] urls=${ext && ext.urls ? ext.urls.length : 0}`);
    if (ext && ext.urls) ext.urls.slice(0, 8).forEach((u) => console.log('    - ' + u.slice(0, 160)));
    if (ext && ext.content) console.log(`  [content] title="${(ext.content.title || '').slice(0, 60)}" desc="${(ext.content.desc || '').slice(0, 40)}"`);

    console.log(`  [bridge captured] ${bridge.length ? bridge.join('\n    ') : '(none)'}`);
    console.log(`  [network matches] ${network.length ? network.slice(0, 10).join('\n    ') : '(none)'}`);
  } catch (e) {
    console.log(`  [test err] ${e.message}`);
  } finally {
    try { await browser.close(); } catch (e) {}
  }
}

for (const t of [
  ['DOUYIN-08nm52sZau8', 'https://v.douyin.com/08nm52sZau8/', MOBILE_UA, dyExt, { width: 412, height: 915 }],
  ['DOUYIN-IqwHHySkBHI', 'https://v.douyin.com/IqwHHySkBHI/', MOBILE_UA, dyExt, { width: 412, height: 915 }],
  ['KUAISHOU', 'https://v.kuaishou.com/KEGKWWpY', DESKTOP_UA, ksExt, { width: 1280, height: 800 }],
]) {
  try { await testPage(...t); } catch (e) { console.log(`[outer err] ${e.message}`); }
}
console.log('\n=== probe done ===');

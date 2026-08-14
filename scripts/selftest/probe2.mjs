// probe2.mjs —— 测抖音桌面网页版 www.douyin.com/video/{id} + 快手桌面页
import { createRequire } from 'module';
import { readFileSync } from 'fs';
const require = createRequire(import.meta.url);
const { chromium } = require('playwright');

const assetsDir = 'C:/Users/Administrator/WorkBuddy/20260713134526/DyXhsDL/app/src/main/assets';
const injectJs = readFileSync(assetsDir + '/web_inject.js', 'utf8');
const dyExt = readFileSync(assetsDir + '/douyin_extractor.js', 'utf8');
const ksExt = readFileSync(assetsDir + '/kuaishou_extractor.js', 'utf8');
const DESKTOP_UA = 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36';
const MOBILE_UA = 'Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36';

async function testPage(name, url, ua, extractorJs, viewport, waitMs) {
  console.log(`\n========== [${name}] ${url} ==========`);
  let browser;
  try { browser = await chromium.launch({ channel: 'chrome', headless: true }); }
  catch (e) { console.log(`  [launch err] ${e.message}`); return; }
  try {
    const context = await browser.newContext({ userAgent: ua, locale: 'zh-CN', timezoneId: 'Asia/Shanghai', viewport });
    const page = await context.newPage();
    const network = [];
    await page.addInitScript((inj) => {
      window.__CAP = [];
      window.AndroidVideoBridge = { onVideoUrl: (u) => { window.__CAP.push(u); } };
      new Function(inj)();
    }, injectJs);
    page.on('request', (r) => { const u = r.url(); if (/(douyinvod|aweme\.snssdk|bytecdn|tiktokcdn|kwaicdn|chenzhongtech|gifshow|kwai|\.mp4|\.m3u8)/.test(u)) network.push(u); });
    page.on('pageerror', (e) => console.log(`  [pageerror] ${e.message.slice(0, 150)}`));
    page.on('console', (m) => { const t = m.text(); if (t.includes('dy inject') || t.includes('Extractor')) console.log(`  [console] ${t.slice(0, 150)}`); });
    let resp = null;
    try { resp = await page.goto(url, { waitUntil: 'domcontentloaded', timeout: 40000 }); }
    catch (e) { console.log(`  [goto] ${e.message.slice(0, 150)}`); }
    console.log(`  [http] ${resp ? resp.status() : '?'} final=${page.url().slice(0, 130)}`);
    await page.waitForTimeout(waitMs);
    const state = await page.evaluate(() => ({
      title: document.title,
      videos: document.querySelectorAll('video').length,
      rdr: typeof window._ROUTER_DATA !== 'undefined',
      apollo: typeof window.__APOLLO_STATE__ !== 'undefined',
      bodyText: (document.body ? document.body.innerText.slice(0, 120) : ''),
      cap: window.__CAP.slice(0, 10),
    }));
    console.log('  [state] ' + JSON.stringify(state, null, 1).slice(0, 900));
    let ext = null;
    try { ext = await page.evaluate(extractorJs); } catch (e) { console.log(`  [extractor err] ${e.message.slice(0, 150)}`); }
    console.log(`  [extractor] urls=${ext && ext.urls ? ext.urls.length : 0}`);
    if (ext && ext.urls) ext.urls.slice(0, 8).forEach((u) => console.log('    - ' + u.slice(0, 150)));
    if (ext && ext.content) console.log(`  [content] title="${(ext.content.title || '').slice(0, 50)}"`);
    console.log(`  [network] ${network.length ? network.slice(0, 8).join('\n    ') : '(none)'}`);
  } catch (e) {
    console.log(`  [test err] ${e.message}`);
  } finally {
    try { await browser.close(); } catch (e) {}
  }
}

const itemIdA = '7673530141037550757'; // 08nm52sZau8
await testPage('DY-DESKTOP-VIDEO', `https://www.douyin.com/video/${itemIdA}`, DESKTOP_UA, dyExt, { width: 1280, height: 800 }, 14000);
const itemIdB = '7672952610642577830'; // IqwHHySkBHI
await testPage('DY-DESKTOP-VIDEO-B', `https://www.douyin.com/video/${itemIdB}`, DESKTOP_UA, dyExt, { width: 1280, height: 800 }, 14000);
console.log('\n=== probe2 done ===');

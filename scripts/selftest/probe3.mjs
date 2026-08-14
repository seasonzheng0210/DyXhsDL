// probe3.mjs —— 单独测快手
import { createRequire } from 'module';
import { readFileSync } from 'fs';
const require = createRequire(import.meta.url);
const { chromium } = require('playwright');
const assetsDir = 'C:/Users/Administrator/WorkBuddy/20260713134526/DyXhsDL/app/src/main/assets';
const injectJs = readFileSync(assetsDir + '/web_inject.js', 'utf8');
const ksExt = readFileSync(assetsDir + '/kuaishou_extractor.js', 'utf8');
const DESKTOP_UA = 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36';

(async () => {
  let browser;
  try { browser = await chromium.launch({ channel: 'chrome', headless: true }); }
  catch (e) { console.log('launch err', e.message); return; }
  try {
    const context = await browser.newContext({ userAgent: DESKTOP_UA, locale: 'zh-CN', timezoneId: 'Asia/Shanghai', viewport: { width: 1280, height: 800 } });
    const page = await context.newPage();
    const network = [];
    await page.addInitScript((inj) => {
      window.__CAP = [];
      window.AndroidVideoBridge = { onVideoUrl: (u) => { window.__CAP.push(u); } };
      new Function(inj)();
    }, injectJs);
    page.on('request', (r) => { const u = r.url(); if (/(kwaicdn|chenzhongtech|gifshow|kwai|\.mp4|\.m3u8)/.test(u)) network.push(u); });
    page.on('pageerror', (e) => console.log('[pageerror]', e.message.slice(0, 150)));
    let resp = null;
    try { resp = await page.goto('https://v.kuaishou.com/KEGKWWpY', { waitUntil: 'domcontentloaded', timeout: 40000 }); }
    catch (e) { console.log('[goto]', e.message.slice(0, 200)); }
    console.log('[http]', resp ? resp.status() : '?', 'final=', page.url().slice(0, 140));
    await page.waitForTimeout(15000);
    const state = await page.evaluate(() => ({
      title: document.title,
      videos: document.querySelectorAll('video').length,
      apollo: typeof window.__APOLLO_STATE__ !== 'undefined',
      init: typeof window.__INITIAL_STATE__ !== 'undefined',
      bodyText: (document.body ? document.body.innerText.slice(0, 150) : ''),
      cap: window.__CAP.slice(0, 8),
    }));
    console.log('[state]', JSON.stringify(state, null, 1).slice(0, 1100));
    let ext = null;
    try { ext = await page.evaluate(ksExt); } catch (e) { console.log('[extractor err]', e.message.slice(0, 150)); }
    console.log('[extractor] urls=', ext && ext.urls ? ext.urls.length : 0);
    if (ext && ext.urls) ext.urls.slice(0, 8).forEach((u) => console.log('  -', u.slice(0, 140)));
    if (ext && ext.content) console.log('[content] title=', JSON.stringify((ext.content.title || '').slice(0, 50)));
    console.log('[network]', network.length ? network.slice(0, 8).join('\n  ') : '(none)');
  } catch (e) { console.log('[err]', e.message); }
  finally { try { await browser.close(); } catch (e) {} }
})();

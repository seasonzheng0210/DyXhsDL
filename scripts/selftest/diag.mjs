// diag.mjs —— 诊断抖音页面 chunk 加载失败原因（连接失败 vs 404），并看能否拿到 play_addr。
import { createRequire } from 'module';
import { readFileSync } from 'fs';
const require = createRequire('C:/Users/Administrator/.workbuddy/binaries/node/workspace/node_modules/playwright/index.js');
const { chromium } = require('playwright');

const assetsDir = 'C:/Users/Administrator/WorkBuddy/20260713134526/DyXhsDL/app/src/main/assets';
const abogusJs = readFileSync(assetsDir + '/abogus.js', 'utf8');
const injectJs = readFileSync(assetsDir + '/web_inject.js', 'utf8');
const DESKTOP_UA = 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36';
const TARGET = 'https://v.douyin.com/Zv2eFSyDXLc/';

async function main() {
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
  page.on('pageerror', (e) => console.log('[pageerror]', (e && e.message ? e.message : String(e)).slice(0, 120)));
  page.on('requestfailed', (r) => {
    const u = r.url();
    if (u.indexOf('douyin') >= 0 || u.indexOf('byte') >= 0) {
      console.log('[reqfail]', r.failure() ? r.failure().errorText : '?', u.slice(0, 110));
    }
  });
  page.on('response', (r) => {
    const u = r.url();
    if (u.indexOf('douyinstatic') >= 0 && u.endsWith('.js')) {
      console.log('[resp]', r.status(), u.slice(0, 110));
    }
  });
  await page.addInitScript((ab) => { try { new Function(ab)(); } catch (e) {} }, abogusJs);
  await page.addInitScript((inj) => {
    window.__CAP = [];
    window.AndroidVideoBridge = { onVideoUrl: (u) => { try { window.__CAP.push(u); } catch (e) {} } };
    try { new Function(inj)(); } catch (e) {}
  }, injectJs);

  let resp = null;
  try { resp = await page.goto(TARGET, { waitUntil: 'domcontentloaded', timeout: 45000 }); }
  catch (e) { console.log('[goto err]', e.message.slice(0, 200)); }
  console.log('[http]', resp ? resp.status() : '?', 'final=', page.url().slice(0, 120));

  // 观察 40s
  const seen = new Set();
  for (let i = 0; i < 20; i++) {
    await page.waitForTimeout(2000);
    const s = await page.evaluate(() => ({
      caps: (window.__CAP || []).slice(),
      videos: document.querySelectorAll('video').length,
      body: (document.body ? document.body.innerText.replace(/\s+/g, ' ').slice(0, 50) : ''),
      title: document.title,
    })).catch(() => ({ caps: [], videos: 0, body: '', title: '' }));
    s.caps.forEach((u) => seen.add(u));
    if (i % 3 === 2 || s.caps.length || s.videos) {
      console.log('[' + ((i + 1) * 2) + 's] caps=' + s.caps.length + ' videos=' + s.videos + ' title="' + s.title + '" body="' + s.body + '"');
    }
  }
  const all = Array.from(seen);
  console.log('[TOTAL caps]', all.length);
  all.slice(0, 15).forEach((u, i) => console.log('  ' + i + ': ' + u.slice(0, 200)));
  await browser.close();
  console.log('=== diag done ===');
}
main().catch((e) => console.log('[fatal]', e.message));

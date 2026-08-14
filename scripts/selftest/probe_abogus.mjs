// probe_abogus.mjs —— 验证 abogus.js 能否算出有效签名并直连抖音 detail 接口
import { createRequire } from 'module';
import { readFileSync } from 'fs';
const require = createRequire(import.meta.url);
const { chromium } = require('playwright');
const assetsDir = 'C:/Users/Administrator/WorkBuddy/20260713134526/DyXhsDL/app/src/main/assets';
const abogus = readFileSync(assetsDir + '/abogus.js', 'utf8');
const DESKTOP_UA = 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36';

const itemId = '7673530141037550757';
let browser;
try { browser = await chromium.launch({ channel: 'chrome', headless: true }); }
catch (e) { console.log('launch err', e.message); process.exit(1); }
try {
  const ctx = await browser.newContext({ userAgent: DESKTOP_UA, locale: 'zh-CN', timezoneId: 'Asia/Shanghai', viewport: { width: 1280, height: 800 } });
  const page = await ctx.newPage();
  await page.goto(`https://www.douyin.com/video/${itemId}`, { waitUntil: 'domcontentloaded', timeout: 40000 });
  await page.waitForTimeout(6000); // 等页面 JS 环境 + cookie 就绪
  await page.addScriptTag({ content: abogus });
  const r = await page.evaluate((id) => {
    const base = 'https://www.douyin.com/aweme/v1/web/aweme/detail/?aweme_id=' + id +
      '&aid=6383&channel=channel_pc_web&device_platform=webapp' +
      '&browser_language=zh-CN&browser_platform=Win32&browser_name=Chrome&browser_version=124.0.0.0' +
      '&engine_name=Blink&engine_version=124.0.0.0&os_name=Windows&os_version=10' +
      '&screen_width=1280&screen_height=800';
    let ab = '';
    try { ab = window.__getABogus(base, 'get'); } catch (e) { ab = 'ERR:' + e.message; }
    return { abLen: ab.length, ab: ab.slice(0, 20), title: document.title };
  }, itemId);
  console.log('[abogus]', JSON.stringify(r));
  // 用算出的签名直连接口
  const res = await page.evaluate(async (id) => {
    const base = 'https://www.douyin.com/aweme/v1/web/aweme/detail/?aweme_id=' + id +
      '&aid=6383&channel=channel_pc_web&device_platform=webapp' +
      '&browser_language=zh-CN&browser_platform=Win32&browser_name=Chrome&browser_version=124.0.0.0' +
      '&engine_name=Blink&engine_version=124.0.0.0&os_name=Windows&os_version=10' +
      '&screen_width=1280&screen_height=800';
    const ab = window.__getABogus(base, 'get');
    const url = base + '&a_bogus=' + encodeURIComponent(ab);
    try {
      const resp = await fetch(url, { headers: { 'Referer': 'https://www.douyin.com/' } });
      const text = await resp.text();
      return { status: resp.status, hasPlay: text.indexOf('play_addr') >= 0, len: text.length, snippet: text.slice(0, 200) };
    } catch (e) { return { err: e.message }; }
  }, itemId);
  console.log('[direct-api]', JSON.stringify(res));
} catch (e) { console.log('err', e.message); }
finally { try { await browser.close(); } catch (e) {} }

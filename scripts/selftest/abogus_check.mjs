import { createRequire } from 'module';
import { readFileSync } from 'fs';
const require = createRequire(import.meta.url);
const { chromium } = require('C:/Users/Administrator/.workbuddy/binaries/node/workspace/node_modules/playwright');

const ROOT = 'C:/Users/Administrator/WorkBuddy/20260713134526/DyXhsDL/app/src/main/assets/';
const ABOGUS = readFileSync(ROOT + 'abogus.js', 'utf8');
const WEB_INJECT = readFileSync(ROOT + 'web_inject.js', 'utf8');

const browser = await chromium.launch({
  args: ['--disable-blink-features=AutomationControlled', '--disable-dev-shm-usage']
});
const page = await browser.newPage();
const errors = [];
page.on('pageerror', e => errors.push('PAGEERROR: ' + e.message));
page.on('console', m => { if (m.type() === 'error') errors.push('CONSOLE.ERR: ' + m.text()); });

await page.setContent('<!doctype html><html><body>test</body></html>');
await page.addScriptTag({ content: ABOGUS });

const sig = await page.evaluate(() => {
  const base = 'https://www.douyin.com/aweme/v1/web/aweme/detail/?aweme_id=7673702151378618010'
    + '&aid=6383&channel=channel_pc_web&device_platform=webapp'
    + '&browser_language=zh-CN&browser_platform=Win32&browser_name=Chrome&browser_version=124.0.0.0'
    + '&engine_name=Blink&engine_version=124.0.0.0&os_name=Windows&os_version=10'
    + '&screen_width=1280&screen_height=800';
  if (typeof window.__getABogus !== 'function') return 'NO_FUNC';
  try { return window.__getABogus(base, 'get'); } catch (e) { return 'ERR:' + e.message; }
});

console.log('__getABogus 函数:', sig === 'NO_FUNC' ? '缺失' : '存在');
console.log('a_bogus 签名长度:', (sig && sig.length) || 0);
console.log('a_bogus 签名样例:', sig ? sig.slice(0, 120) : sig);

// 验证 web_inject.js 注入无语法/运行错误（浏览器无 AndroidVideoBridge，但有守卫不会崩）
const injectRes = await page.evaluate((inj) => {
  try { (0, eval)(inj); return 'OK'; } catch (e) { return 'INJECT_ERR:' + e.message; }
}, WEB_INJECT);
const injected = await page.evaluate(() => window.__dyInject ? '已注入(inject ready)' : '未注入');
console.log('web_inject.js 注入:', injectRes, '|', injected);

console.log('页面JS错误:', errors.length ? errors.slice(0, 5) : '无');
await browser.close();

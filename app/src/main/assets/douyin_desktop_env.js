/**
 * 桌面环境自洽补丁 —— 必须在页面主 JS 之前注入（不拦截任何网络，只读改写环境信号）
 *
 * 【为什么需要】
 * 抖音 /aweme/v1/web/aweme/post/ 接口会交叉校验请求方声明的 UA 与浏览器实际暴露的
 * navigator.platform。本 App 的 WebView 把 UA 设成了 Windows 桌面 Chrome，但
 * navigator.platform 仍是 Android WebView 的 Linux 值 → 服务端**静默拒绝**，
 * 返回 HTTP 200 + 空 body（注意不是 403，所以从状态码看不出来）。
 *
 * 【CDP 对照实验实证 · 2026-10-10】
 *   全部伪装 Windows Chrome/124 UA，每条用独立匿名 context：
 *     platform=Win32          → 200 / 1,047,917 字节 / 21 条作品   ← 正常
 *     platform=Linux x86_64   → 200 / 0 字节 / 0 条               ← 单变量复现本 App 故障
 *     WebGL 改 "WebKit WebGL" → 21 条（**非**触发条件）
 *     WebGL 改 "Adreno 730"   → 21 条（**非**触发条件）
 *     CPU 核数 / 内存 / 屏幕尺寸 → 均非触发条件
 *
 * 故本脚本把 WebView 的环境信号统一成「Windows 桌面 Chrome」，与 UA 声明一致。
 * 幂等（__dyDesktopEnv 守卫）；onPageStarted + onPageFinished 双注入兜底。
 */
(function () {
  if (window.__dyDesktopEnv) return;
  window.__dyDesktopEnv = true;

  var report = [];
  function safe(name, fn) {
    try { fn(); report.push(name + '=ok'); }
    catch (e) { report.push(name + '=FAIL(' + (e && e.message) + ')'); }
  }
  function def(obj, prop, getter) {
    Object.defineProperty(obj, prop, { get: getter, configurable: true, enumerable: true });
  }

  // 1) navigator.platform —— 决定性变量
  safe('platform', function () {
    def(Navigator.prototype, 'platform', function () { return 'Win32'; });
    if (navigator.platform !== 'Win32') {
      def(navigator, 'platform', function () { return 'Win32'; });
    }
  });

  // 2) 触摸能力：Windows 桌面 Chrome 为 0 且无 on* 句柄；移动端特征会与桌面 UA 冲突
  safe('touch', function () {
    def(Navigator.prototype, 'maxTouchPoints', function () { return 0; });
    ['ontouchstart', 'ontouchmove', 'ontouchend', 'ontouchcancel'].forEach(function (k) {
      try { Object.defineProperty(window, k, { value: undefined, configurable: true }); } catch (e) {}
    });
  });

  // 3) UA-CH（Chromium 系在此有该对象）也要与 UA 声明一致
  safe('uach', function () {
    var d = navigator.userAgentData;
    if (!d) { report.push('uach=absent'); return; }
    try { def(Object.getPrototypeOf(d), 'platform', function () { return 'Windows'; }); } catch (e) {}
    try { def(Object.getPrototypeOf(d), 'mobile', function () { return false; }); } catch (e) {}
    if (d.platform !== 'Windows') {
      try { def(d, 'platform', function () { return 'Windows'; }); } catch (e) {}
    }
  });

  // 4) 屏幕尺寸：与桌面 UA 相称（离屏 WebView 视口已显式设为 1440×900）
  safe('screen', function () {
    def(screen, 'width', function () { return 1920; });
    def(screen, 'height', function () { return 1080; });
    def(screen, 'availWidth', function () { return 1920; });
    def(screen, 'availHeight', function () { return 1080; });
    def(screen, 'colorDepth', function () { return 24; });
    def(screen, 'pixelDepth', function () { return 24; });
  });

  // 5) 语言
  safe('lang', function () {
    def(Navigator.prototype, 'language', function () { return 'zh-CN'; });
    def(Navigator.prototype, 'languages', function () { return ['zh-CN', 'zh', 'en']; });
  });

  try {
    if (window.AndroidVideoBridge && AndroidVideoBridge.onHomepageSniffLog) {
      AndroidVideoBridge.onHomepageSniffLog(
        'desktop_env ' + report.join(',') +
        ' | platform=' + navigator.platform +
        ' maxTouch=' + navigator.maxTouchPoints +
        ' uach=' + (navigator.userAgentData
          ? navigator.userAgentData.platform + '/' + navigator.userAgentData.mobile
          : 'absent') +
        ' screen=' + screen.width + 'x' + screen.height +
        ' uaWin=' + (navigator.userAgent.indexOf('Windows') >= 0) +
        ' chrome=' + (typeof window.chrome !== 'undefined')
      );
    }
  } catch (e) {}
})();

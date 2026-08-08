#!/usr/bin/env bash
# 真机(模拟器)自测脚本：adb 安装 APK + 用 auto_download_url 触发真实解析链路 + 抓 logcat 取证
# 用法: bash emulator_test.sh <apk_path>
set -u
APK="${1:-app/build/outputs/apk/release/app-release.apk}"
# applicationId 与 namespace 不同：am start 组件 = applicationId/namespace.类名
APP_ID="com.neoruaa.douyinxhs"
NS="com.neoruaa.xhsdn"
MAIN="$APP_ID/$NS.MainActivity"
WV="$APP_ID/$NS.WebViewActivity"
ADB="${ADB:-/c/Android/Sdk/platform-tools/adb.exe}"
OUT_DIR="scripts/selftest/logs"
mkdir -p "$OUT_DIR"

echo "===== 等待 emulator 上线 ====="
"$ADB" wait-for-device
# 等系统启动完成
"$ADB" shell 'while [[ "$(getprop sys.boot_completed)" != "1" ]]; do sleep 2; done' 2>/dev/null
echo "[ok] emulator booted"

echo "===== 安装 APK ====="
"$ADB" install -r -t "$APK" 2>&1 | tail -3

# 测试链接（抖音 / 小红书 / 淘宝短链）
declare -A URLS=(
  [douyin]="https://www.douyin.com/video/7300000000000000000"
  [xhs]="https://www.xiaohongshu.com/explore/6500000000000000000"
  [taobao]="https://e.tb.cn/h.85d1cfjpNBy0DDp?tk=65ingCtOIIy"
)
# 真实可访问样例：用一段公开抖音/小红书短链做冒烟（解析失败时至少有真实网络错误可取证）
URLS[douyin]="https://v.douyin.com/iRwqYx3M/"
URLS[xhs]="https://www.xiaohongshu.com/explore/64f0c8e0000000001f00e6f3"
URLS[taobao]="https://e.tb.cn/h.85d1cfjpNBy0DDp?tk=65ingCtOIIy"

run_case() {
  local name="$1" url="$2"
  local log="$OUT_DIR/${name}.log"
  echo "===== [$name] $url ====="
  "$ADB" logcat -c
  if [[ "$name" == "taobao_wv" ]]; then
    "$ADB" shell am start -n "$WV" -e url "$url" -e source taobao 2>&1 | tail -2
  else
    "$ADB" shell am start -n "$MAIN" -e auto_download_url "$url" 2>&1 | tail -2
  fi
  sleep 18
  "$ADB" logcat -d > "$log"
  echo "--- 关键日志(过滤) ---"
  grep -E "XHS_Debug|DownloadService|TaobaoParser|resolveFinalUrl|extractMainImages|需要登录|未获取|解析失败|ClipText|WebViewActivity|isTaobaoLoggedIn" "$log" | tail -40
  echo "(full log -> $log)"
}

run_case douyin "${URLS[douyin]}"
run_case xhs "${URLS[xhs]}"
run_case taobao "${URLS[taobao]}"
run_case taobao_wv "${URLS[taobao]}"

echo "===== 完成，日志在 $OUT_DIR ====="

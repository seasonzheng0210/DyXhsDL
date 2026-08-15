const UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Safari/537.36";
const AWEME_ID = "7673702151378618010";

function parseCookies(res) {
  const sc = res.headers.get('set-cookie');
  if (!sc) return {};
  const out = {};
  sc.split(',').forEach(p => { const [kv] = p.split(';'); const i = kv.indexOf('='); out[kv.slice(0,i).trim()] = kv.slice(i+1).trim(); });
  return out;
}

let cookies = {};
try {
  const r1 = await fetch("https://www.douyin.com/", { headers: { "User-Agent": UA }, redirect: "follow" });
  cookies = parseCookies(r1);
  console.log("[1] 游客cookie:", JSON.stringify(cookies));
} catch (e) { console.log("[1] 取cookie失败:", e.message); }

const cookieStr = Object.entries(cookies).map(([k,v])=>`${k}=${v}`).join("; ");

try {
  const r2 = await fetch(`https://www.douyin.com/video/${AWEME_ID}`, { headers: { "User-Agent": UA, "Cookie": cookieStr }, redirect: "follow" });
  const html = await r2.text();
  const mp4 = [...html.matchAll(/https?:\/\/[^"'\s]+\.mp4[^"'\s]*/g)].map(m=>m[0]);
  console.log("[2] 页面HTTP状态:", r2.status, "长度:", html.length, "内联.mp4条数:", mp4.length);
  if (mp4.length) console.log("    样例:", mp4.slice(0,3));
} catch (e) { console.log("[2] 抓页面失败:", e.message); }

try {
  const url = `https://www.iesdouyin.com/web/api/v2/aweme/v1/play/?aweme_id=${AWEME_ID}&device_platform=webapp&aid=6383&channel=channel_pc_web&pc_client_type=1`;
  const r3 = await fetch(url, { headers: { "User-Agent": UA, "Cookie": cookieStr, "Referer": "https://www.douyin.com/" }, redirect: "follow" });
  const txt = await r3.text();
  console.log("[3] play API 状态:", r3.status, "响应长度:", txt.length);
  console.log("    前200字符:", txt.slice(0,200).replace(/\n/g,' '));
} catch (e) { console.log("[3] play API失败:", e.message); }

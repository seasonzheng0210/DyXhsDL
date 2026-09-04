package com.neoruaa.xhsdn.douyin.abogus

import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import kotlin.random.Random

/**
 * 抖音 Web API `a_bogus` 签名（Kotlin 移植）。
 *
 * 参考：Evil0ctal/Douyin_TikTok_Download_API 的 abogus.py（Apache-2.0，源自
 * JoeanAmier/TikTokDownloader GPLv3）——只移植"活跃签名路径"：
 *   1. string_1：3 段随机 4 字节（list_1/2/3 掩码表）
 *   2. string_2：时间戳/参数双重SM3/方法双重SM3/浏览器串混合出 44 字节 +
 *               浏览器 char-code 67 字节 + XOR 校验 1 字节 → RC4(key="y")
 *   3. s4 自定义 Base64（3→4 分组 + '=' padding）
 * 文件内嵌的"自定义 SM3 压缩器"在活跃路径未被调用（Python 走 gmssl 标准 SM3），
 * 故不移植，改用 [Sm3] 标准实现，双重哈希结果与 gmssl 逐字节一致。
 *
 * UA 绑定 Chrome/90（ua_code 硬编码表），browser 信息用固定默认串（与 Python
 * 默认一致，保证输出可逐字节对照；实证该组合在数据中心 IP 下 detail 接口 HTTP 200）。
 */
object DouyinAbogus {

    /**
     * 签名绑定的请求 UA（Chrome/90）：ua_code 表由该 UA 推导硬编码，**勿改**。
     * 发起 detail 等签名请求时必须用此 UA，否则签名与请求特征不一致会被服务端拒绝。
     */
    const val SIGN_UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/90.0.4430.212 Safari/537.36"

    /** s4 自定义 Base64 表（URL 安全变体） */
    private val S4 = "Dkdpgh2ZmsQB80/MfvV36XI1R45-WUAlEixNLwoqYTOPuzKFjJnry79HbGcaStCe"

    /** Chrome/90 UA 的签名码（ua_code，硬编码 32 字节） */
    private val UA_CODE = intArrayOf(
        76, 98, 15, 131, 97, 245, 224, 133, 122, 199, 241, 166, 79, 34, 90, 191,
        128, 126, 122, 98, 66, 11, 14, 40, 49, 110, 110, 173, 67, 96, 138, 252
    )

    /** 浏览器指纹串（与 Python 默认 __browser 一致；长度 67，char-code 进入签名） */
    private const val BROWSER = "1536|742|1536|864|0|0|0|0|1536|864|1536|864|1536|742|24|24|MacIntel"
    private val BROWSER_CODE: IntArray = BROWSER.map { it.code }.toIntArray()

    private const val METHOD_TAIL = "cus" // 方法/参数哈希的消息后缀

    /**
     * 计算 a_bogus。
     *
     * @param urlParams 已按序 urlencode 的 query 串（**不含** a_bogus 自身），
     *                  必须与最终请求 URL 的 query 完全一致（顺序敏感）。
     * @param method HTTP 方法，默认 GET。
     * @param startTimeMs / @param endTimeMs 固定毫秒时间戳；默认用当前时间。
     * @param random1..3 list_1/2/3 随机种子，需为正；0 表示自动随机。
     *                   传固定值可复现输出（金标准对照用）。
     */
    fun getValue(
        urlParams: String,
        method: String = "GET",
        startTimeMs: Long = System.currentTimeMillis(),
        endTimeMs: Long = startTimeMs + 6,
        random1: Long = 0,
        random2: Long = 0,
        random3: Long = 0,
    ): String {
        val s1 = generateString1(
            random1.takeIf { it > 0 } ?: Random.nextLong(1, 1_000_000_000),
            random2.takeIf { it > 0 } ?: Random.nextLong(1, 1_000_000_000),
            random3.takeIf { it > 0 } ?: Random.nextLong(1, 1_000_000_000),
        )
        val s2 = generateString2(urlParams, method, startTimeMs, endTimeMs)
        val all = IntArray(s1.size + s2.size)
        System.arraycopy(s1, 0, all, 0, s1.size)
        System.arraycopy(s2, 0, all, s1.size, s2.size)
        return base64S4(all)
    }

    /**
     * 便捷入口：按参数对顺序 urlencode 后签名（保序）。
     * 值编码规则对齐 Python urllib.parse.urlencode（quote_plus：空格→'+'，
     * 保留 [-._~]、字母数字与空串，其余 %XX 大写）。
     */
    fun getValue(
        params: List<Pair<String, String>>,
        method: String = "GET",
        startTimeMs: Long = System.currentTimeMillis(),
        endTimeMs: Long = startTimeMs + 6,
        random1: Long = 0,
        random2: Long = 0,
        random3: Long = 0,
    ): String = getValue(buildQuery(params), method, startTimeMs, endTimeMs, random1, random2, random3)

    /** 按顺序把参数拼成 query 串（含空值→"k="；对齐 Python urlencode）。 */
    fun buildQuery(params: List<Pair<String, String>>): String =
        params.joinToString("&") { (k, v) -> "$k=${urlEncode(v)}" }

    /** RFC3986 严格编码：签名后拼 URL 时 a_bogus 里的 '/' '-' 需 %XX。 */
    fun urlEncodeComponent(s: String): String =
        URLEncoder.encode(s, "UTF-8")
            .replace("+", "%20")
            .replace("*", "%2A")
            .replace("%7E", "~")

    // ---------------------------------------------------------------- 内部实现

    private fun urlEncode(v: String): String {
        if (v.isEmpty()) return ""
        // 对齐 python urllib.parse.urlencode（quote_plus）：空格→'+'（Java 同），
        // '~' 保留（Java 转 %7E → 还原），'*' 转 %2A（Java 保留 → 补齐）
        return URLEncoder.encode(v, "UTF-8")
            .replace("%7E", "~")
            .replace("*", "%2A")
    }

    private fun generateString1(r1: Long, r2: Long, r3: Long): IntArray {
        val a1 = randomList(r1, 170, 85, 1, 2, 5, 45 and 170) // g = c & a = 40
        val a2 = randomList(r2, 170, 85, 1, 0, 0, 0)
        val a3 = randomList(r3, 170, 85, 1, 0, 5, 0)
        return a1 + a2 + a3
    }

    /**
     * random_list：由单个种子派生 4 字节（对齐 Python：
     * v=[r, int(r)&255, int(r)>>8]；byte1=(v1&b)|d, byte2=(v1&c)|e, byte3=(v2&b)|f, byte4=(v2&c)|g）。
     */
    private fun randomList(r: Long, b: Int, c: Int, d: Int, e: Int, f: Int, g: Int): IntArray {
        val low = r.toInt() and 0xff
        val high = (r ushr 8).toInt() and 0xff
        return intArrayOf(
            (low and b) or d,
            (low and c) or e,
            (high and b) or f,
            (high and c) or g,
        )
    }

    private fun generateString2(
        urlParams: String,
        method: String,
        startTimeMs: Long,
        endTimeMs: Long,
    ): IntArray {
        val end = endTimeMs
        val start = startTimeMs
        val paramsCode = doubleSm3(urlParams + METHOD_TAIL) // 32 字节
        val methodCode = doubleSm3(method + METHOD_TAIL)    // 32 字节

        // list_4 固定 44 字节模板（逐位对齐 Python 返回数组）
        val head = intArrayOf(
            44,
            ((end ushr 24) and 0xff).toInt(),      // 1
            0, 0, 0, 0,
            24,
            paramsCode[21],                         // 7  b
            methodCode[21],                         // 8  n
            0,
            UA_CODE[23],                            // 10 c
            ((end ushr 16) and 0xff).toInt(),       // 11 d
            0, 0, 0,
            1,
            0,
            239,
            paramsCode[22],                         // 18 e
            methodCode[22],                         // 19 o
            UA_CODE[24],                            // 20 f
            ((end ushr 8) and 0xff).toInt(),        // 21 g
            0, 0, 0, 0,
            (end and 0xff).toInt(),                 // 26 h
            0, 0,
            14,
            ((start ushr 24) and 0xff).toInt(),     // 30 i
            ((start ushr 16) and 0xff).toInt(),     // 31 j
            0,
            ((start ushr 8) and 0xff).toInt(),      // 33 k
            (start and 0xff).toInt(),               // 34 m
            3,
            ((end ushr 32) and 0xff).toInt(),       // 36 p = int(end/2^32)
            1,
            ((start ushr 32) and 0xff).toInt(),     // 38 q
            1,
            BROWSER_CODE.size,                      // 40 r = 67
            0, 0, 0,
        )
        check(head.size == 44) { "list_4 template must be 44 bytes" }

        // XOR 校验
        var xor = 0
        for (x in head) xor = xor xor x

        // 44 + browser(67) + xor(1) = 112
        val plain = IntArray(head.size + BROWSER_CODE.size + 1)
        System.arraycopy(head, 0, plain, 0, head.size)
        System.arraycopy(BROWSER_CODE, 0, plain, head.size, BROWSER_CODE.size)
        plain[plain.size - 1] = xor

        return rc4(plain, "y")
    }

    /** 双重 SM3：先哈希 UTF-8 串得 32 字节，再把 32 字节作为输入哈希一次。 */
    private fun doubleSm3(s: String): IntArray {
        val h1 = Sm3.hash(s.toByteArray(StandardCharsets.UTF_8))
        val h2 = Sm3.hash(h1)
        return IntArray(32) { h2[it].toInt() and 0xff }
    }

    /** 标准 RC4，key 为单字节字符串（'y'=121），返回与明文同长的 0..255 字节。 */
    private fun rc4(plain: IntArray, key: String): IntArray {
        val k = key.map { it.code }.toIntArray()
        val s = IntArray(256) { it }
        var j = 0
        for (i in 0 until 256) {
            j = (j + s[i] + k[i % k.size]) and 0xff
            val t = s[i]; s[i] = s[j]; s[j] = t
        }
        var i = 0
        j = 0
        val out = IntArray(plain.size)
        for (idx in plain.indices) {
            i = (i + 1) and 0xff
            j = (j + s[i]) and 0xff
            val t = s[i]; s[i] = s[j]; s[j] = t
            out[idx] = s[(s[i] + s[j]) and 0xff] xor plain[idx]
        }
        return out
    }

    /** s4 自定义 Base64：3 字节 → 4 字符；尾块 1/2 字节 → 2/3 字符 + '=' 补齐。 */
    private fun base64S4(data: IntArray): String {
        val sb = StringBuilder()
        var i = 0
        val n = data.size
        while (i < n) {
            val b0 = data[i] and 0xff
            val b1 = if (i + 1 < n) data[i + 1] and 0xff else 0
            val b2 = if (i + 2 < n) data[i + 2] and 0xff else 0
            val v = (b0 shl 16) or (b1 shl 8) or b2
            sb.append(S4[(v ushr 18) and 0x3f])
            sb.append(S4[(v ushr 12) and 0x3f])
            if (i + 1 < n) sb.append(S4[(v ushr 6) and 0x3f])
            if (i + 2 < n) sb.append(S4[v and 0x3f])
            i += 3
        }
        val pad = (4 - sb.length % 4) % 4
        repeat(pad) { sb.append('=') }
        return sb.toString()
    }
}

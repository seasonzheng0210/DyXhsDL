package com.neoruaa.xhsdn.douyin.abogus

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * DouyinAbogus 金标准对照测试。
 *
 * 期望值来自沙箱 Python 金标准 golden_abogus.py（Evil0ctal abogus.py +
 * gmssl SM3，固定全部随机源）：
 *   start=1756962000000, end=1756962000007,
 *   r1=123456789, r2=987654321, r3=555555555
 * 该固定输入组合此前已实证：数据中心 IP + 浏览器完整游客 cookie + 此签名
 * → note detail 接口 HTTP 200 / status_code=0（见 C:\tmp\dy_probe\probe_detail.py）。
 */
class DouyinAbogusTest {

    private val goldenQuery = ("device_platform=webapp&aid=6383&channel=channel_pc_web" +
            "&pc_client_type=1&version_code=290100&version_name=29.1.0" +
            "&cookie_enabled=true&screen_width=1920&screen_height=1080" +
            "&browser_language=zh-CN&browser_platform=Win32" +
            "&browser_name=Chrome&browser_version=130.0.0.0" +
            "&browser_online=true&engine_name=Blink&engine_version=130.0.0.0" +
            "&os_name=Windows&os_version=10&cpu_core_num=12&device_memory=8" +
            "&platform=PC&downlink=10&effective_type=4g&round_trip_time=0" +
            "&aweme_id=7674999258294252665&msToken=")

    private val goldenABogus = "DvA0-5gvQgdNffSf54ALfY3q6XYVYmsP0SVkMD2fr-DOAL39HMOZ9exoig0vuE8ji4/sIeEjy4hbT3ohrQ2y0Hwf9W0L/25ksDSkKl5Q5xSSs1X9eghgJ04qmkt5SMx2RvB-rOXmqhZHKRbp09oHmhK4b1dzFgf3qJLzoD=="

    @Test
    fun sm3_matchesStandardVector_abc() {
        val hex = Sm3.hash("abc".toByteArray()).joinToString("") { "%02x".format(it) }
        // GB/T 32905-2016 附录 A.2 示例
        assertEquals("66c7f0f462eeedd9d1f2d46bdc10e4e24167c4875cf2f7a2297da02b8f4ba8e0", hex)
    }

    @Test
    fun aBogus_matchesPythonGolden_byteExact() {
        val out = DouyinAbogus.getValue(
            urlParams = goldenQuery,
            startTimeMs = 1756962000000L,
            endTimeMs = 1756962000007L,
            random1 = 123456789L,
            random2 = 987654321L,
            random3 = 555555555L,
        )
        assertEquals(goldenABogus.length, out.length)
        assertEquals(goldenABogus, out)
    }

    @Test
    fun aBogus_containsOnlyS4AlphabetAndPadding() {
        val out = DouyinAbogus.getValue(
            urlParams = goldenQuery,
            startTimeMs = 1756962000000L,
            endTimeMs = 1756962000007L,
            random1 = 123456789L,
            random2 = 987654321L,
            random3 = 555555555L,
        )
        val alphabet = "Dkdpgh2ZmsQB80/MfvV36XI1R45-WUAlEixNLwoqYTOPuzKFjJnry79HbGcaStCe="
        out.forEach { ch ->
            if (ch !in alphabet) throw AssertionError("a_bogus 含非法字符 '$ch'")
        }
        assertEquals("==", out.takeLast(2))
    }

    @Test
    fun urlEncodeComponent_matchesPythonQuoteSafeEmpty() {
        // python quote(a_bogus, safe=''): 字母数字与 '-' 保留；'/'→%2F；'='→%3D
        val raw = DouyinAbogus.getValue(
            urlParams = goldenQuery,
            startTimeMs = 1756962000000L,
            endTimeMs = 1756962000007L,
            random1 = 123456789L,
            random2 = 987654321L,
            random3 = 555555555L,
        )
        val url = DouyinAbogus.urlEncodeComponent(raw)
        assertEquals(
            "DvA0-5gvQgdNffSf54ALfY3q6XYVYmsP0SVkMD2fr-DOAL39HMOZ9exoig0vuE8ji4%2FsIeEjy4hbT3ohrQ2y0Hwf9W0L%2F25ksDSkKl5Q5xSSs1X9eghgJ04qmkt5SMx2RvB-rOXmqhZHKRbp09oHmhK4b1dzFgf3qJLzoD%3D%3D",
            url,
        )
    }

    @Test
    fun buildQuery_matchesPythonUrlencode() {
        // python: urlencode({"a":"b c","d":"中文","e":"","f":"x*y~z/.5"})
        // quote_plus: 空格→'+'，'*'→%2A，'/'→%2F，'~' 保留，中文 UTF-8 %XX
        val q = DouyinAbogus.buildQuery(
            listOf(
                "a" to "b c",
                "d" to "中文",
                "e" to "",
                "f" to "x*y~z/.5",
            )
        )
        assertEquals(
            "a=b+c&d=%E4%B8%AD%E6%96%87&e=&f=x%2Ay~z%2F.5",
            q,
        )
    }

    @Test
    fun getValue_paramPairs_overloadAgreesWithQueryString() {
        val pairs = listOf(
            "device_platform" to "webapp", "aid" to "6383", "aweme_id" to "7674999258294252665",
            "msToken" to "",
        )
        val fromPairs = DouyinAbogus.getValue(
            params = pairs,
            method = "GET",
            startTimeMs = 1756962000000L,
            endTimeMs = 1756962000007L,
            random1 = 123456789L,
            random2 = 987654321L,
            random3 = 555555555L,
        )
        val fromString = DouyinAbogus.getValue(
            urlParams = "device_platform=webapp&aid=6383&aweme_id=7674999258294252665&msToken=",
            method = "GET",
            startTimeMs = 1756962000000L,
            endTimeMs = 1756962000007L,
            random1 = 123456789L,
            random2 = 987654321L,
            random3 = 555555555L,
        )
        assertEquals(fromString, fromPairs)
    }
}

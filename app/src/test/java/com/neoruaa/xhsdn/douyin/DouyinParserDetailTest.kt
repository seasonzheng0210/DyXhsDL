package com.neoruaa.xhsdn.douyin

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/**
 * DouyinParser.parseDetailJson 解析测试。
 * 样例数据来自沙箱真实抓取（C:\tmp\dy_probe\dump_detail_response.py）：
 * note 7674999258294252665，detail HTTP 200 / status_code=0 / aweme_type=68 / images[0] 无水印直链。
 */
class DouyinParserDetailTest {

    private fun readSample(): String =
        DouyinParserDetailTest::class.java.getResourceAsStream("/detail_sample_image.json")
            ?.bufferedReader()?.use { it.readText() }
            ?: error("缺少测试资源 detail_sample_image.json")

    @Test
    fun parseDetailJson_imageNote_returnsImageInfo() {
        val info = DouyinParser.parseDetailJson(readSample(), "7674999258294252665")
        assertEquals(DouyinMediaType.IMAGE, info?.type)
        assertTrue("标题应含 desc 前段", (info?.title ?: "").startsWith("我不治了"))
        assertEquals(1, info?.imageUrls?.size)
        assertTrue(
            "图应为无水印 douyinpic 直链",
            info!!.imageUrls[0].startsWith("https://p3-pc-sign.douyinpic.com/tos-cn-i-")
        )
        assertNull("图文帖 videoUrl 必须为空（video.play_addr 是 BGM mp3，防误下）", info.videoUrl)
    }

    @Test
    fun parseDetailJson_awemeIdMismatch_returnsNull() {
        // aweme_detail.aweme_id 与目标 id 不符（detail 错位/泛推荐）→ 必须 null，防下错作品
        val body = readSample()
        assertNull(DouyinParser.parseDetailJson(body, "1111111111111111111"))
    }

    @Test
    fun parseDetailJson_statusNonZero_returnsNull() {
        val body = """{"status_code":2,"status_msg":"作品已删除或不存在"}"""
        assertNull(DouyinParser.parseDetailJson(body, "7674999258294252665"))
    }

    @Test
    fun parseDetailJson_noAwemeDetail_returnsNull() {
        assertNull(DouyinParser.parseDetailJson("""{"status_code":0,"data":{}}""", "7674999258294252665"))
    }

    @Test(expected = WebDetailBlockedException::class)
    fun parseDetailJson_argusBlockedBody_throwsBlocked() {
        DouyinParser.parseDetailJson("""{"message":"Blocked by ArgusSecurityPlugin Uifid Not Found"}""", "x")
        fail("应抛 WebDetailBlockedException")
    }

    @Test
    fun parseDetailJson_video_returnsVideoInfo() {
        val body = """
            {
              "status_code": 0,
              "aweme_detail": {
                "aweme_id": "1234567890123456789",
                "desc": "测试视频",
                "video": {
                  "play_addr": {"url_list": ["https://v3-dy-o.zjcdn.com/aweme/720p/abc.mp4?x-sign=1"]},
                  "cover": {"url_list": ["https://p3-pc-sign.douyinpic.com/tos-cn-i-0813/cover~tplv.jpeg"]}
                }
              }
            }
        """.trimIndent()
        val info = DouyinParser.parseDetailJson(body, "1234567890123456789")
        assertEquals(DouyinMediaType.VIDEO, info?.type)
        assertTrue(info!!.videoUrl!!.startsWith("https://v3-dy-o.zjcdn.com"))
        assertEquals("测试视频", info.title)
    }

    @Test
    fun parseDetailJson_bgmOnlyNote_returnsNull() {
        // 图文帖畸形场景：images 缺失但 video.play_addr 是 mp3（BGM）→ 必须 null，严禁把音频当视频下
        val body = """
            {
              "status_code": 0,
              "aweme_detail": {
                "aweme_id": "1234567890123456789",
                "desc": "只有BGM的畸形帖",
                "video": {"play_addr": {"url_list": ["https://lf26-music.douyinstatic.com/obj/ies-music-hj/7659061419261971257.mp3"]}}
              }
            }
        """.trimIndent()
        assertNull(DouyinParser.parseDetailJson(body, "1234567890123456789"))
    }

    @Test
    fun parseDetailJson_garbageBody_returnsNull() {
        assertNull(DouyinParser.parseDetailJson("<html>502 Bad Gateway</html>", "7674999258294252665"))
    }
}

package com.neoruaa.xhsdn.douyin

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/**
 * DouyinParser.parsePostListJson 解析测试（主页批量下载 P1）。
 * 样例 post_list_sample.json 覆盖：视频条、图文条（images 优先于 BGM play_addr）、
 * 直播回放（m3u8）跳过、翻页游标、作者昵称提取。
 */
class DouyinParserPostListTest {

    private fun readSample(): String =
        DouyinParserPostListTest::class.java.getResourceAsStream("/post_list_sample.json")
            ?.bufferedReader()?.use { it.readText() }
            ?: error("缺少测试资源 post_list_sample.json")

    @Test
    fun parsePostList_videosAndImages_extracted() {
        val page = DouyinParser.parsePostListJson(readSample())
        assertEquals(2, page.items.size)
        val first = page.items[0]
        assertEquals("7400000000000000001", first.id)
        assertEquals(DouyinMediaType.VIDEO, first.type)
        assertTrue(first.videoUrl!!.startsWith("https://www.douyin.com/aweme/v1/play/"))
    }

    @Test
    fun parsePostList_imagePost_imagesFirst_bgmIgnored() {
        val page = DouyinParser.parsePostListJson(readSample())
        val imagePost = page.items.first { it.id == "7400000000000000002" }
        assertEquals(DouyinMediaType.IMAGE, imagePost.type)
        assertEquals(2, imagePost.imageUrls.size)
        assertEquals(null, imagePost.videoUrl)
    }

    @Test
    fun parsePostList_liveReplay_skipped() {
        val page = DouyinParser.parsePostListJson(readSample())
        assertTrue("直播回放(m3u8)条目不应出现在结果里", page.items.none { it.id == "7400000000000000003" })
    }

    @Test
    fun parsePostList_cursorAndNickname_extracted() {
        val page = DouyinParser.parsePostListJson(readSample())
        assertEquals(1725500000000, page.maxCursor)
        assertEquals(true, page.hasMore)
        assertEquals("测试作者", page.nickname)
    }

    @Test(expected = WebDetailBlockedException::class)
    fun parsePostList_argusBlocked_throwsBlocked() {
        DouyinParser.parsePostListJson("""{"message":"ArgusSecurityPlugin Uifid Not Found"}""")
        fail("应抛 WebDetailBlockedException")
    }

    @Test
    fun parsePostList_businessError_throws() {
        try {
            DouyinParser.parsePostListJson("""{"status_code":8,"status_msg":"用户不存在"}""")
            fail("业务失败应抛异常（调用方需区分可重试风控与空主页）")
        } catch (e: Exception) {
            assertTrue(e.message!!.contains("用户不存在"))
        }
    }
}

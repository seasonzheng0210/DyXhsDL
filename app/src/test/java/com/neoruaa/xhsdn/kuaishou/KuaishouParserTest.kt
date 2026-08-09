package com.neoruaa.xhsdn.kuaishou

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 快手解析器单元测试（纯 JVM，不碰网络）。
 * 仅覆盖离线纯函数 canParse / mediaExtension；GraphQL / __INITIAL_STATE__ 走真机 WebView 验证。
 */
class KuaishouParserTest {

    @Test
    fun `canParse detects kuaishou com`() {
        assertTrue(KuaishouParser.canParse("https://www.kuaishou.com/short-video/3xAbCdEfG"))
    }

    @Test
    fun `canParse detects kuaishou cn`() {
        assertTrue(KuaishouParser.canParse("https://www.kuaishou.cn/photo/3xAbCdEfG"))
    }

    @Test
    fun `canParse detects gifshow com`() {
        assertTrue(KuaishouParser.canParse("https://www.gifshow.com/photo/3xAbCdEfG"))
    }

    @Test
    fun `canParse detects chenzhongtech com`() {
        assertTrue(KuaishouParser.canParse("https://v.m.chenzhongtech.com/fw/photo/3xAbCdEfG"))
    }

    @Test
    fun `canParse rejects other platforms`() {
        assertFalse(KuaishouParser.canParse("https://www.douyin.com/video/1"))
        assertFalse(KuaishouParser.canParse("https://www.xiaohongshu.com/explore/1"))
        assertFalse(KuaishouParser.canParse("https://example.com/foo"))
    }

    @Test
    fun `mediaExtension maps common extensions`() {
        assertEquals("jpg", KuaishouParser.mediaExtension("https://kwaicdn.com/a/b.jpg"))
        assertEquals("png", KuaishouParser.mediaExtension("https://kwaicdn.com/a/b.png"))
        assertEquals("webp", KuaishouParser.mediaExtension("https://kwaicdn.com/a/b.webp"))
        assertEquals("gif", KuaishouParser.mediaExtension("https://kwaicdn.com/a/b.gif"))
        assertEquals("mp4", KuaishouParser.mediaExtension("https://kwaicdn.com/a/b.mp4"))
        assertEquals("mov", KuaishouParser.mediaExtension("https://kwaicdn.com/a/b.mov"))
    }

    @Test
    fun `mediaExtension defaults to jpg for unknown`() {
        assertEquals("jpg", KuaishouParser.mediaExtension("https://kwaicdn.com/a/b"))
        assertEquals("jpg", KuaishouParser.mediaExtension("https://kwaicdn.com/a/b?x=1"))
    }
}

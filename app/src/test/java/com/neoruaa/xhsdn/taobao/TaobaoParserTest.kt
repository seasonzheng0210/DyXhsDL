package com.neoruaa.xhsdn.taobao

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 淘宝解析接缝测试（对应 docs/specs/taobao-flow.md「测试决策表」）。
 * 仅覆盖零网络分支（带 id 的直链 / 纯 HTML 正则），不触发真实网络请求、不调用 android.util.Log。
 */
class TaobaoParserTest {

    @Test
    fun `resolveItemPageUrl maps detail url with id directly (no network)`() {
        val out = TaobaoParser.resolveItemPageUrl("https://item.taobao.com/item.htm?id=926895986130")
        assertEquals("https://item.taobao.com/item.htm?id=926895986130", out)
    }

    @Test
    fun `resolveItemPageUrl maps tmall detail url to item taobao`() {
        val out = TaobaoParser.resolveItemPageUrl("https://detail.tmall.com/item.htm?id=1006988355866")
        assertEquals("https://item.taobao.com/item.htm?id=1006988355866", out)
    }

    @Test
    fun `extractMainImages parses standard auctionImages array`() {
        val html = """
            var data = {"auctionImages":["//img.alicdn.com/imgextra/i1/1.jpg",
            "https://img.alicdn.com/imgextra/i2/2.jpg"]};
        """.trimIndent()
        val imgs = TaobaoParser.extractMainImages(html)
        assertEquals(2, imgs.size)
        assertTrue(imgs[0].startsWith("https://"))
        assertTrue(imgs.contains("https://img.alicdn.com/imgextra/i2/2.jpg"))
    }

    @Test
    fun `extractMainImages returns empty for modern Array form (known limitation)`() {
        // 现代淘宝把主图数组写成 auctionImages:Array(5)，正则匹配不到 → 空（已在 spec 标注为已知边界）
        val html = "window.__INITIAL_STATE__={auctionImages:Array(5)}"
        assertTrue(TaobaoParser.extractMainImages(html).isEmpty())
    }

    @Test
    fun `extractMainImages returns empty when absent`() {
        assertTrue(TaobaoParser.extractMainImages("<html><body>no images</body></html>").isEmpty())
    }

    @Test
    fun `mediaExtension maps common extensions`() {
        assertEquals("png", TaobaoParser.mediaExtension("https://img.alicdn.com/x.png?w=100"))
        assertEquals("webp", TaobaoParser.mediaExtension("https://img.alicdn.com/x.webp"))
        assertEquals("gif", TaobaoParser.mediaExtension("https://img.alicdn.com/x.gif"))
        assertEquals("jpg", TaobaoParser.mediaExtension("https://img.alicdn.com/x"))
    }
}

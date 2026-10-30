package com.neoruaa.xhsdn.utils

object UrlUtils {
    /**
     * 入口目标类型（v3.3.14）。
     *
     * 背景：入口层原来靠「文本特征 + 短链正则」猜主页，猜错就要先导航再纠正 ——
     * `xhslink.com|cn /o/{code}` 既可能指向用户主页、也可能指向单篇笔记（实测两种都真实存在），
     * 猜成主页后 `openHomepage` 立刻把主页切到「主页下载」，跟跳完才发现是单作品、又切回来，
     * 用户看到两次跳页（2026-10-24 反馈的观感问题）。改成先判定、再导航。
     */
    enum class EntryTarget { HOMEPAGE, SINGLE, AMBIGUOUS }

    /** 主页直链（无需跟跳即可判定）。 */
    private val HOME_URL_RE = Regex(
        """xiaohongshu\.com/user/profile/[0-9A-Za-z]+""" +
            """|(?:iesdouyin|douyin)\.com/(?:share/)?user/[0-9A-Za-z_-]+""" +
            """|kuaishou\.com/(?:profile|u)/[0-9A-Za-z_\-=]+"""
    )

    /** 单作品直链（视频 / 图文 / 笔记 / 快手作品页）。 */
    private val SINGLE_WORK_RE = Regex(
        """/(?:video|note|slides|short-video|photo|long-video|discovery/item|explore)/[0-9A-Za-z_-]+""" +
            """|/share/(?:video|note|slides|photo|slides)/[0-9A-Za-z_-]+""" +
            """|/fw/(?:photo|long-video)/[0-9A-Za-z_-]+""" +
            """|modal_id=\d+"""
    )

    /** 短链：目标必须跟跳一次才知道。 */
    private val SHORT_LINK_RE = Regex(
        """v\.douyin\.com/|xhslink\.(?:cn|com)/|v\.kuaishou\.com/|kuaishou\.com/fw/"""
    )

    /** 主页分享文案特征（抖音/快手「TA 的更多作品」类话术）。 */
    private fun hasHomepageSentence(text: String?): Boolean {
        if (text.isNullOrBlank()) return false
        val t = text.lowercase()
        return text.contains("查看ta的更多作品") || text.contains("查看更多作品") ||
            text.contains("查看ta的作品") || text.contains("ta的主页") ||
            t.contains("more works") || t.contains("more videos")
    }

    /** 归一化：传入可能是裸 URL、也可能是整段分享文案。 */
    private fun norm(text: String?): String =
        (extractFirstUrl(text) ?: text.orEmpty()).lowercase()

    /** 确定的主页直链。 */
    fun isCertainHomepageUrl(text: String?): Boolean {
        if (text.isNullOrBlank()) return false
        return HOME_URL_RE.containsMatchIn(norm(text))
    }

    /** 确定的单作品直链。 */
    fun isCertainSingleWorkUrl(text: String?): Boolean {
        if (text.isNullOrBlank()) return false
        return SINGLE_WORK_RE.containsMatchIn(norm(text))
    }

    /** 短链（歧义目标）。 */
    fun isShortLink(text: String?): Boolean {
        if (text.isNullOrBlank()) return false
        return SHORT_LINK_RE.containsMatchIn(norm(text))
    }

    /**
     * 判定入口目标：主页 / 单作品 / 短链歧义。
     * 调用方对 AMBIGUOUS 必须「先跟跳再决定」，不得先导航。
     */
    fun entryTarget(rawText: String?, url: String? = null): EntryTarget {
        val u = url ?: extractFirstUrl(rawText)
        if (u.isNullOrBlank()) return EntryTarget.AMBIGUOUS
        if (isCertainHomepageUrl(u)) return EntryTarget.HOMEPAGE
        // 分享文案本身带主页话术（链接是短链也要算确定主页，跟跳只会更慢）
        if (hasHomepageSentence(rawText)) return EntryTarget.HOMEPAGE
        if (isCertainSingleWorkUrl(u)) return EntryTarget.SINGLE
        return EntryTarget.AMBIGUOUS
    }

    /**
     * 从文本中提取第一个 URL
     */
    fun extractFirstUrl(text: String?): String? {
        if (text.isNullOrBlank()) return null
        // 1. 标准带协议 URL
        val re = Regex("https?://[\\w\\-.]+(?:/[\\w\\-._~:/?#\\[\\]@!$&'()*+,;=%]*)?")
        re.find(text)?.value?.let { return it }
        // 2. 无协议但含已知域名的分享短链（如 v.douyin.com/xxx、iesdouyin.com/share/note/xxx、
        //    xhslink.com/xxx、kuaishou.com/xxx）。补全 https:// 后返回，避免「链接暂不可识别」。
        val re2 = Regex("(?:[\\w\\-]+\\.)?(douyin\\.com|iesdouyin\\.com|kuaishou\\.com|kuaishou\\.cn|gifshow\\.com|xiaohongshu\\.com|xhslink\\.com|xhslink\\.cn)(?:/[\\w\\-._~:/?#\\[\\]@!$&'()*+,;=%]*)?")
        re2.find(text)?.value?.let { return "https://$it" }
        return null
    }

    /**
     * 检查是否为有效的小红书链接
     */
    fun isXhsLink(url: String?): Boolean {
        if (url == null) return false
        return url.contains("xhslink.com") || url.contains("xhslink.cn") || url.contains("xiaohongshu.com")
    }

    /**
     * 检查是否为有效的抖音链接
     */
    fun isDouyinLink(url: String?): Boolean {
        if (url == null) return false
        val u = url.lowercase()
        return u.contains("douyin.com") || u.contains("iesdouyin.com")
    }

    /**
     * 检查是否为有效的快手链接（含手机分享短链 v.kuaishou.com、作品页、直播站、旧域名 gifshow.com）
     */
    fun isKuaishouLink(url: String?): Boolean {
        if (url == null) return false
        val u = url.lowercase()
        return u.contains("kuaishou.com") || u.contains("kuaishou.cn") ||
            u.contains("gifshow.com") || u.contains("chenzhongtech.com")
    }

    /**
     * 判断文本是否为「抖音主页链接」。
     * 命中场景：
     *  - 分享文案含「查看TA的更多作品」「查看更多作品」等主页分享特征；
     *  - 链接已解析为 www.douyin.com/user/{sec_uid} 主页直链。
     * 视频链接（/video/{id}）不命中，需经 feed 接口反查作者后再走主页下载。
     */
    fun isDouyinHomepageLink(text: String?): Boolean {
        if (text.isNullOrBlank()) return false
        if (hasHomepageSentence(text)) return true
        return Regex("""(?:iesdouyin|douyin)\.com/(?:share/)?user/[0-9a-z_\-]+""")
            .containsMatchIn(norm(text))
    }

    /**
     * 判断文本是否为「小红书主页链接」。
     * 命中场景：`xiaohongshu.com/user/profile/{id}` 主页直链。
     *
     * ★v3.3.14：**不再把 `xhslink.cn|com/o/{code}` 短链算作主页**。实测该短链两种目标都有
     * （多数落用户主页，少数落单篇笔记），旧实现在此处直接返回 true → 入口层先把主页切到
     * 「主页下载」，跟跳后发现是单笔记又切回来 → 用户看到两次跳页。
     * 短链一律交给 [entryTarget] 判为 AMBIGUOUS，由调用方先跟跳再决定。
     */
    fun isXhsHomepageLink(text: String?): Boolean {
        if (text.isNullOrBlank()) return false
        return Regex("""xiaohongshu\.com/user/profile/[0-9A-Za-z]+""")
            .containsMatchIn(norm(text))
    }

    /**
     * 从文本中提取抖音主页链接（主页分享文案或 /user/{sec_uid} 直链）。
     * 非主页链接返回 null。
     */
    fun extractDouyinHomepageUrl(text: String?): String? {
        if (!isDouyinHomepageLink(text)) return null
        return extractFirstUrl(text)
    }

    /**
     * 综合判断文本所属平台：抖音 / 快手 / 小红书 / 未知（null）。
     * 优先按 URL 域名判断，其次按文本中的中文/英文关键词判断，
     * 以便从分享文案（常含“抖音”“快手”“小红书”等字眼）直接分类。
     */
    fun detectPlatform(text: String?): String? {
        if (text.isNullOrBlank()) return null
        val url = extractFirstUrl(text)
        // 1. 域名判断
        if (isDouyinLink(url)) return "douyin"
        if (isKuaishouLink(url)) return "kuaishou"
        if (isXhsLink(url)) return "xhs"
        // 2. 关键词判断（兼容中英文分享文案）
        if (text.contains("抖音") || text.contains("douyin", ignoreCase = true)) return "douyin"
        if (text.contains("快手") || text.contains("kuaishou", ignoreCase = true)) return "kuaishou"
        if (text.contains("小红书") || text.contains("xiaohongshu", ignoreCase = true) || text.contains("xhslink", ignoreCase = true)) return "xhs"
        return null
    }
}

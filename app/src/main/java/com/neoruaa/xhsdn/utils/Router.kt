package com.neoruaa.xhsdn.utils

/**
 * 链接 → WebView 路由决策。把 MainActivity / WebViewActivity 中散落的“平台判断 + 误判兜底”
 * 收敛成单一可测接缝。
 */
object Router {

    /**
     * 解析 WebView 应使用哪个 source，杜绝“快手/抖音链接被默认当小红书”的误判。
     *
     * 规则：
 *  - declaredSource 显式为 kuaishou / douyin 时，尊重声明（保留调用方手动指定意图）；
 *  - declaredSource 为 null 或 "xhs"（默认）时，若 URL 实为快手/抖音域名，强制纠正为对应平台；
 *  - URL 无法识别时回退 declaredSource（默认 "xhs"）。
 */
fun resolveWebViewSource(rawUrl: String?, declaredSource: String?): String {
    val declared = declaredSource ?: "xhs"
    val byUrl = when {
        UrlUtils.isKuaishouLink(rawUrl) -> "kuaishou"
        UrlUtils.isDouyinLink(rawUrl) -> "douyin"
        UrlUtils.isXhsLink(rawUrl) -> "xhs"
        else -> null
    }
        if (declared == "xhs" && byUrl != null && byUrl != "xhs") {
            return byUrl
        }
        return declared
    }
}

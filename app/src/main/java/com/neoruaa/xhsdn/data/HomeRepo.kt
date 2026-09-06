package com.neoruaa.xhsdn.data

import android.content.Context
import android.util.Log
import org.json.JSONObject
import java.io.File

/**
 * 主页批量下载的作品级账本（P1 用 JSON 文件，零依赖）。
 *
 * 作用：
 *  - 作品级去重：aweme_id 记账，追更/全量模式跳过已下载条目（文件名去重不可靠）；
 *  - 追更基准：lastSync 时间戳 + downloadedIds，新作品 = 未下载 ∩ 新于上次同步；
 *  - 下载成功才入账（recordBatch 只在条目成功后调用），失败条下次重试。
 *
 * 存储：getExternalFilesDir(null)/Download/home_index.json（与下载日志同根，用户可查看/备份）。
 * 结构：{"authors": {"<secUid>": {"nickname": "...", "lastSync": 1234567890, "downloadedIds": ["id", …]}}}
 */
object HomeRepo {
    private const val TAG = "HomeRepo"
    private const val FILE_NAME = "home_index.json"

    data class AuthorRecord(
        val nickname: String,
        val downloadedIds: MutableSet<String>,
        var lastSync: Long,
        var lastUsed: Long = 0L
    )

    private val authors = LinkedHashMap<String, AuthorRecord>()
    private var loaded = false

    @Synchronized
    private fun file(ctx: Context): File =
        File(ctx.getExternalFilesDir(null) ?: ctx.filesDir, "Download/$FILE_NAME")

    @Synchronized
    private fun ensureLoaded(ctx: Context) {
        if (loaded) return
        loaded = true
        runCatching {
            val f = file(ctx)
            if (!f.exists()) return
            val root = JSONObject(f.readText())
            val arr = root.optJSONObject("authors") ?: return
            for (key in arr.keys()) {
                val o = arr.optJSONObject(key) ?: continue
                val ids = LinkedHashSet<String>()
                val idArr = o.optJSONArray("downloadedIds")
                if (idArr != null) for (i in 0 until idArr.length()) {
                    val v = idArr.optString(i)
                    if (v.isNotBlank()) ids.add(v)
                }
                authors[key] = AuthorRecord(
                    nickname = o.optString("nickname", ""),
                    downloadedIds = ids,
                    lastSync = o.optLong("lastSync", 0L),
                    lastUsed = o.optLong("lastUsed", 0L)
                )
            }
        }.onFailure { Log.w(TAG, "home_index.json 读取失败（按空账本继续）: ${it.message}") }
    }

    @Synchronized
    private fun save(ctx: Context) {
        runCatching {
            val root = JSONObject()
            val arr = JSONObject()
            for ((k, r) in authors) {
                val o = JSONObject()
                o.put("nickname", r.nickname)
                o.put("lastSync", r.lastSync)
                o.put("lastUsed", r.lastUsed)
                o.put("downloadedIds", org.json.JSONArray(r.downloadedIds.toList()))
                arr.put(k, o)
            }
            root.put("authors", arr)
            val f = file(ctx)
            f.parentFile?.mkdirs()
            f.writeText(root.toString())
        }.onFailure { Log.w(TAG, "home_index.json 写入失败: ${it.message}") }
    }

    @Synchronized
    fun downloadedIds(ctx: Context, secUid: String): Set<String> {
        ensureLoaded(ctx)
        return authors[secUid]?.downloadedIds?.toSet() ?: emptySet()
    }

    @Synchronized
    fun lastSync(ctx: Context, secUid: String): Long {
        ensureLoaded(ctx)
        return authors[secUid]?.lastSync ?: 0L
    }

    @Synchronized
    fun nickname(ctx: Context, secUid: String): String? {
        ensureLoaded(ctx)
        return authors[secUid]?.nickname?.takeIf { it.isNotBlank() }
    }

    /** 过滤出未下载过的作品（全部/最新N/追更三档都先过这一道）。 */
    @Synchronized
    fun filterNew(ctx: Context, secUid: String, items: List<com.neoruaa.xhsdn.douyin.DouyinPostItem>): List<com.neoruaa.xhsdn.douyin.DouyinPostItem> {
        ensureLoaded(ctx)
        val done = authors[secUid]?.downloadedIds ?: return items
        return items.filter { it.id !in done }
    }

    /**
     * 预览/解析成功时记录"最近使用"，供主页 tab「上次解析」快捷卡取最近作者。
     * 未下载过任何作品的作者也会留下记录（仅有 lastUsed 而无 downloadedIds）。
     */
    @Synchronized
    fun touch(ctx: Context, secUid: String, nickname: String) {
        ensureLoaded(ctx)
        val r = authors.getOrPut(secUid) {
            AuthorRecord(nickname, LinkedHashSet(), 0L, System.currentTimeMillis())
        }
        if (nickname.isNotBlank()) {
            authors[secUid] = r.copy(nickname = nickname)
        }
        r.lastUsed = System.currentTimeMillis()
        save(ctx)
    }

    /** 最近一次预览/下载过的作者及其 secUid（无任何记录返回 null）。 */
    @Synchronized
    fun recentAuthor(ctx: Context): Pair<String, AuthorRecord>? {
        ensureLoaded(ctx)
        return authors.entries.maxByOrNull { it.value.lastUsed }?.let { it.key to it.value }
    }

    /**
     * 批量入账：整个 batch 成功结束后调用（成功条 id + 昵称 + 本次同步时间）。
     */
    @Synchronized
    fun recordBatch(ctx: Context, secUid: String, nickname: String, ids: Collection<String>, syncTime: Long = System.currentTimeMillis()) {
        ensureLoaded(ctx)
        val r = authors.getOrPut(secUid) {
            AuthorRecord(nickname, LinkedHashSet(), 0L)
        }
        if (nickname.isNotBlank()) {
            authors[secUid] = r.copy(nickname = nickname)
        }
        r.downloadedIds.addAll(ids)
        if (syncTime > r.lastSync) r.lastSync = syncTime
        save(ctx)
    }

    /** 指定作者重置账本（用户要求全量重下时用）。 */
    @Synchronized
    fun reset(ctx: Context, secUid: String) {
        ensureLoaded(ctx)
        authors.remove(secUid)
        save(ctx)
    }
}

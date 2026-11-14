package ir.heyat.khalilshahr

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class FeedRepository(context: Context) {
    private val prefs = context.getSharedPreferences("feed_cache", Context.MODE_PRIVATE)
    data class Result(val posts: List<Post>, val isOfflineCache: Boolean = false)
    sealed class Fetch { data class Success(val posts: List<Post>, val isBaseline: Boolean, val newIds: Set<String>): Fetch(); data class Failure(val message: String): Fetch() }

    fun cached(): List<Post> = runCatching {
        val arr = JSONArray(prefs.getString("posts", "[]"))
        (0 until arr.length()).map { i -> JSONObject(arr.getString(i)).let { o -> Post(o.getString("id"),o.getString("text"),o.optString("url").takeIf(String::isNotBlank),if(o.isNull("date")) null else o.getLong("date"), (0 until o.getJSONArray("tags").length()).map { j -> o.getJSONArray("tags").getString(j) },o.optInt("order",i)) } }
    }.getOrDefault(emptyList())

    fun fetch(): Fetch {
        val connection = try { URL(CHANNEL_URL).openConnection() as HttpURLConnection } catch (e: Exception) { return Fetch.Failure(e.message ?: "خطا در اتصال") }
        return try {
            connection.connectTimeout = 15000; connection.readTimeout = 20000; connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Android; HeyatFeed/1.0)")
            connection.instanceFollowRedirects = true
            val code = connection.responseCode
            if (code !in 200..299) return Fetch.Failure("پاسخ سرور: HTTP $code")
            val html = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            val parsed = PostParser.parse(html, CHANNEL_URL)
            if (parsed.isEmpty()) return Fetch.Failure("ساختار صفحه پست قابل‌تشخیصی نداشت؛ کش حفظ شد.")
            val old = cached(); val baseline = !prefs.getBoolean("has_baseline", false)
            val changed = if (baseline) emptySet() else newlySeenIds(old.map { it.id }.toSet(), parsed)
            val merged = (parsed + old).distinctBy { it.id }.let { list -> if (list.all { it.dateMillis != null }) list.sortedByDescending { it.dateMillis } else list.sortedBy { it.sourceOrder } }.take(10)
            save(merged); prefs.edit().putBoolean("has_baseline", true).apply()
            Fetch.Success(merged, baseline, changed)
        } catch (e: Exception) { Fetch.Failure(e.message ?: "دریافت ناموفق بود") } finally { connection.disconnect() }
    }

    private fun save(posts: List<Post>) {
        val arr = JSONArray(); posts.forEach { p -> arr.put(JSONObject().put("id",p.id).put("text",p.text).put("url",p.url ?: "").put("date",p.dateMillis ?: JSONObject.NULL).put("tags",JSONArray(p.hashtags)).put("order",p.sourceOrder)) }
        prefs.edit().putString("posts",arr.toString()).apply()
    }
    companion object { const val CHANNEL_URL = "https://eitaa.com/howzeh_khalilshahr" }
}

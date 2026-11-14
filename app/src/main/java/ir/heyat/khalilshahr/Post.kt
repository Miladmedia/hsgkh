package ir.heyat.khalilshahr

import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import java.util.Locale

data class Post(val id: String, val text: String, val url: String?, val dateMillis: Long?, val hashtags: List<String>, val sourceOrder: Int)

object PostParser {
    private val containerSelector = ".tgme_widget_message, .widget_message, .message, .eitaa-message"
    private val textSelector = ".tgme_widget_message_text, .widget_message_text, .eitaa-message-text, .message-caption, .caption"

    fun parse(html: String, baseUrl: String): List<Post> {
        val doc = Jsoup.parse(html, baseUrl)
        val seen = LinkedHashMap<String, Post>()
        doc.select(containerSelector).forEachIndexed { index, box ->
            val content = box.selectFirst(textSelector)?.text()?.trim().orEmpty()
                .ifBlank { box.selectFirst(".tgme_widget_message_caption, .message-caption, .caption")?.text()?.trim().orEmpty() }
            if (content.isBlank()) return@forEachIndexed
            val anchor = box.selectFirst("a[href]")?.absUrl("href")?.takeIf { it.isNotBlank() }
            val raw = box.attr("data-post").ifBlank { box.attr("data-id") }
                .ifBlank { box.selectFirst("[data-post], [data-id]")?.let { it.attr("data-post").ifBlank { e -> e.attr("data-id") } }.orEmpty() }
            val linkId = anchor?.substringAfterLast('/')?.takeIf { it.isNotBlank() }
            val id = raw.ifBlank { linkId.orEmpty() }
            if (id.isBlank()) return@forEachIndexed
            val time = box.selectFirst("time[datetime], time[date], [datetime], [data-date]")
            val dateValue = time?.attr("datetime").orEmpty().ifBlank { time?.attr("date").orEmpty() }.ifBlank { time?.attr("data-date").orEmpty() }
            val millis = dateValue.toLongOrNull()?.let { if (it < 10_000_000_000L) it * 1000 else it }
                ?: runCatching { java.time.Instant.parse(dateValue).toEpochMilli() }.getOrNull()
                ?: runCatching { java.time.OffsetDateTime.parse(dateValue).toInstant().toEpochMilli() }.getOrNull()
            val tags = Regex("(?<![\p{L}\p{N}_])#([\p{L}\p{N}_]+)").findAll(content).map { it.groupValues[1] }.distinct().toList()
            seen.putIfAbsent(id, Post(id, content, anchor, millis, tags, index))
        }
        val items = seen.values.toList()
        return if (items.isNotEmpty() && items.all { it.dateMillis != null }) items.sortedByDescending { it.dateMillis } else items.sortedBy { it.sourceOrder }
    }
}

internal fun newlySeenIds(previous: Set<String>, current: List<Post>): Set<String> = current.map { it.id }.filterNot(previous::contains).toSet()

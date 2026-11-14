package ir.heyat.khalilshahr

import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import java.time.Instant
import java.time.OffsetDateTime
import java.time.format.DateTimeParseException

/** One real, stably-addressable post extracted from the public channel HTML. */
data class Post(
    val id: String,
    val text: String,
    val url: String?,
    val dateMillis: Long?,
    val hashtags: List<String>,
    val sourceOrder: Int
)

object PostParser {
    private val containerSelectors = listOf(
        ".tgme_widget_message", ".widget_message", ".eitaa-message",
        "[data-post]", "[data-message-id]", "article"
    )
    private val textSelectors = listOf(
        ".tgme_widget_message_text", ".widget_message_text", ".eitaa-message-text",
        ".message-caption", ".caption", ".tgme_widget_message_caption", "[data-message-text]"
    )

    fun parse(html: String, baseUrl: String): List<Post> {
        if (html.isBlank()) return emptyList()
        val doc = Jsoup.parse(html, baseUrl)
        val candidates = LinkedHashSet<Element>()
        containerSelectors.forEach { selector -> candidates.addAll(doc.select(selector)) }
        val unique = LinkedHashMap<String, Post>()
        candidates.forEachIndexed { index, box ->
            val body = textSelectors.asSequence().mapNotNull { box.selectFirst(it) }.firstOrNull()
            val text = (body ?: box).text().trim()
            if (text.isBlank()) return@forEachIndexed

            val link = box.select("a[href]").firstOrNull { anchor ->
                Regex("/(?:c/)?[0-9]+(?:$|[/?#])").containsMatchIn(anchor.attr("href"))
            } ?: box.selectFirst("a[href]")
            val postUrl = link?.absUrl("href")?.takeIf { it.isNotBlank() }
            val nested = box.selectFirst("[data-post], [data-id], [data-message-id]")
            val rawId = sequenceOf(
                box.attr("data-post"), box.attr("data-id"), box.attr("data-message-id"), box.id(),
                nested?.attr("data-post").orEmpty(), nested?.attr("data-id").orEmpty(),
                nested?.attr("data-message-id").orEmpty(),
                postUrl?.substringAfterLast('/')?.substringBefore('?').orEmpty()
            ).firstOrNull { it.isNotBlank() }.orEmpty()
            val id = stableId(rawId) ?: return@forEachIndexed

            val time = box.selectFirst("time[datetime], time[date], [datetime], [data-date]")
            val dateValue = sequenceOf("datetime", "date", "data-date")
                .map { attr -> time?.attr(attr).orEmpty() }.firstOrNull { it.isNotBlank() }.orEmpty()
            val tags = Regex("""(?<![\p{L}\p{N}_])#([\p{L}\p{N}_]+)""")
                .findAll(text).map { it.groupValues[1] }.distinct().toList()
            unique.putIfAbsent(id, Post(id, text, postUrl, parseMillis(dateValue), tags, index))
        }
        val posts = unique.values.toList()
        return if (posts.isNotEmpty() && posts.all { it.dateMillis != null })
            posts.sortedByDescending { it.dateMillis } else posts.sortedBy { it.sourceOrder }
    }

    private fun stableId(raw: String): String? {
        val value = raw.trim().trimEnd('/')
        if (value.isBlank()) return null
        // Reject arbitrary HTML/CSS values while allowing public channel post IDs and platform IDs.
        val pathId = Regex("(?:^|/)(?:c/)?([0-9]+)$").find(value)
        return when {
            pathId != null -> value
            value.matches(Regex("[A-Za-z0-9_-]+(?:/[A-Za-z0-9_-]+)*")) -> value
            else -> null
        }
    }

    private fun parseMillis(value: String): Long? {
        if (value.isBlank()) return null
        value.toLongOrNull()?.let { return if (it < 10_000_000_000L) it * 1000 else it }
        return try { Instant.parse(value).toEpochMilli() } catch (_: DateTimeParseException) {
            try { OffsetDateTime.parse(value).toInstant().toEpochMilli() } catch (_: DateTimeParseException) { null }
        }
    }
}

internal fun newlySeenIds(previous: Set<String>, current: List<Post>): Set<String> =
    current.asSequence().map { it.id }.filterNot(previous::contains).toSet()

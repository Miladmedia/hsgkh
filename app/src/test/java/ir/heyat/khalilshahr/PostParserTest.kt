package ir.heyat.khalilshahr

import org.junit.Assert.*
import org.junit.Test

class PostParserTest {
    @Test fun parsesPostsAndDeduplicatesPinnedCopy() {
        val html = """<div class='channel-title'>metadata only</div><div class='tgme_widget_message' data-post='c/2'><div class='tgme_widget_message_text'>سلام #شهدا</div><time datetime='2025-01-02T10:00:00Z'></time><a href='https://eitaa.com/howzeh_khalilshahr/c/2'>اصل</a></div><div class='widget_message' data-id='c/1'><div class='widget_message_text'>برنامه #مراسم</div><time datetime='2025-01-01T10:00:00Z'></time></div><div class='tgme_widget_message' data-post='c/2'><div class='tgme_widget_message_text'>نسخهٔ سنجاق‌شده</div></div>"""
        val posts=PostParser.parse(html,"https://eitaa.com/howzeh_khalilshahr")
        assertEquals(2,posts.size); assertEquals("c/2",posts.first().id); assertEquals(listOf("شهدا"),posts.first().hashtags); assertTrue(posts.first().url!!.contains("/c/2"))
    }
    @Test fun ignoresMetadataAndMissingStableId() {
        val html="<div class='channel-title'>کانال #خبر</div><div class='message'><div class='widget_message_text'>بدون شناسه #شهدا</div></div>"
        assertTrue(PostParser.parse(html,"https://eitaa.com/x").isEmpty())
    }
    @Test fun identifiesOnlyNewStableIds() {
        val list=listOf(Post("a","x",null,null, emptyList(),0),Post("b","y",null,null, emptyList(),1))
        assertEquals(setOf("b"),newlySeenIds(setOf("a"),list))
    }
}

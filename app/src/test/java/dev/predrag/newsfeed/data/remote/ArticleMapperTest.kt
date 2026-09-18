package dev.predrag.newsfeed.data.remote

import dev.predrag.newsfeed.data.remote.dto.ArticleDto
import dev.predrag.newsfeed.data.remote.dto.SourceDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ArticleMapperTest {

    @Test
    fun `maps a complete article`() {
        val article = ArticleMapper.toDomain(
            ArticleDto(
                source = SourceDto(name = "The Verge"),
                author = "Jane Doe",
                title = "Something happened",
                description = "A description.",
                url = "https://example.com/a",
                publishedAt = "2026-09-17T05:22:00Z",
            )
        )

        requireNotNull(article)
        assertEquals("Something happened", article.title)
        assertEquals("The Verge", article.sourceName)
        assertEquals("Jane Doe", article.author)
        assertEquals("2026-09-17T05:22:00Z", article.publishedAt.toString())
    }

    @Test
    fun `drops articles NewsAPI has taken down`() {
        val removed = ArticleDto(
            source = SourceDto(name = "[Removed]"),
            author = "[Removed]",
            title = "[Removed]",
            description = "[Removed]",
            url = "https://removed.com",
            publishedAt = "1970-01-01T00:00:00Z",
        )

        assertNull(ArticleMapper.toDomain(removed))
    }

    @Test
    fun `drops articles without a usable url or title`() {
        assertNull(ArticleMapper.toDomain(ArticleDto(title = "Has title", url = null)))
        assertNull(ArticleMapper.toDomain(ArticleDto(title = null, url = "https://example.com")))
        assertNull(ArticleMapper.toDomain(ArticleDto(title = "  ", url = "https://example.com")))
    }

    @Test
    fun `keeps the article when only optional fields are missing`() {
        val article = ArticleMapper.toDomain(
            ArticleDto(title = "Title", url = "https://example.com/b")
        )

        requireNotNull(article)
        assertNull(article.author)
        assertNull(article.description)
        assertEquals("", article.sourceName)
    }

    @Test
    fun `an unparseable timestamp does not lose the article`() {
        val article = ArticleMapper.toDomain(
            ArticleDto(title = "Title", url = "https://example.com/c", publishedAt = "not a date")
        )

        requireNotNull(article)
        assertNull(article.publishedAt)
    }

    @Test
    fun `id is stable for the same url and differs between articles`() {
        val first = ArticleMapper.idFor("https://example.com/a")
        val again = ArticleMapper.idFor("https://example.com/a")
        val other = ArticleMapper.idFor("https://example.com/b")

        assertEquals(first, again)
        assertNotEquals(first, other)
        assertTrue("id must be safe in a URL path", first.all { it.isLetterOrDigit() })
    }

    @Test
    fun `mapping a list skips the unusable entries`() {
        val mapped = ArticleMapper.toDomain(
            listOf(
                ArticleDto(title = "Keep me", url = "https://example.com/1"),
                ArticleDto(title = "[Removed]", url = "https://example.com/2"),
                ArticleDto(title = "Keep me too", url = "https://example.com/3"),
            )
        )

        assertEquals(listOf("Keep me", "Keep me too"), mapped.map { it.title })
    }
}

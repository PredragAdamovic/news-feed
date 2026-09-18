package dev.predrag.newsfeed.data.remote

import dev.predrag.newsfeed.data.remote.dto.ArticleDto
import dev.predrag.newsfeed.domain.model.Article
import java.security.MessageDigest
import java.time.Instant
import java.time.format.DateTimeParseException

object ArticleMapper {

    private const val REMOVED_MARKER = "[Removed]"

    /**
     * Returns null for entries NewsAPI has taken down — it keeps them in the payload with
     * "[Removed]" in every field, and they would render as rows that open nothing.
     */
    fun toDomain(dto: ArticleDto): Article? {
        val url = dto.url?.takeIf { it.isNotBlank() && it != REMOVED_MARKER } ?: return null
        val title = dto.title?.takeIf { it.isNotBlank() && it != REMOVED_MARKER } ?: return null

        return Article(
            id = idFor(url),
            title = title,
            sourceName = dto.source?.name?.takeIf { it.isNotBlank() }.orEmpty(),
            author = dto.author?.takeIf { it.isNotBlank() && it != REMOVED_MARKER },
            description = dto.description?.takeIf { it.isNotBlank() && it != REMOVED_MARKER },
            publishedAt = parseInstant(dto.publishedAt),
            url = url,
        )
    }

    fun toDomain(dtos: List<ArticleDto>): List<Article> = dtos.mapNotNull(::toDomain)

    /**
     * The url is the only stable identity NewsAPI gives us, but it cannot go into a
     * `myapp://article/{id}` path unescaped — so it is hashed into a short opaque id.
     */
    fun idFor(url: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(url.toByteArray())
            .take(12)
            .joinToString("") { "%02x".format(it) }

    private fun parseInstant(raw: String?): Instant? {
        if (raw.isNullOrBlank()) return null
        return try {
            Instant.parse(raw)
        } catch (e: DateTimeParseException) {
            null
        }
    }
}

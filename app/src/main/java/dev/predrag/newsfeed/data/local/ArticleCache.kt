package dev.predrag.newsfeed.data.local

import android.content.Context
import dev.predrag.newsfeed.domain.model.Article
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.time.Instant

/**
 * Last successful first page, kept on disk so a cold start without network shows something.
 *
 * A single JSON file rather than Room: exactly one page is ever stored, and there are no
 * queries or migrations to justify the machinery.
 */
class ArticleCache(
    context: Context,
    private val json: Json,
) {

    private val file = File(context.filesDir, FILE_NAME)

    suspend fun save(articles: List<Article>) = withContext(Dispatchers.IO) {
        val snapshot = CachedFeed(
            savedAtEpochMs = Instant.now().toEpochMilli(),
            articles = articles.map(::toCached),
        )
        runCatching { file.writeText(json.encodeToString(snapshot)) }
        Unit
    }

    suspend fun read(): CachedSnapshot? = withContext(Dispatchers.IO) {
        if (!file.exists()) return@withContext null

        // A cache we cannot parse is treated as no cache — failing to read a convenience
        // copy must never surface as an error.
        runCatching {
            val feed = json.decodeFromString<CachedFeed>(file.readText())
            CachedSnapshot(
                articles = feed.articles.map(::toDomain),
                savedAt = Instant.ofEpochMilli(feed.savedAtEpochMs),
            )
        }.getOrNull()
    }

    private fun toCached(article: Article) = CachedArticle(
        id = article.id,
        title = article.title,
        sourceName = article.sourceName,
        author = article.author,
        description = article.description,
        publishedAtEpochMs = article.publishedAt?.toEpochMilli(),
        url = article.url,
    )

    private fun toDomain(cached: CachedArticle) = Article(
        id = cached.id,
        title = cached.title,
        sourceName = cached.sourceName,
        author = cached.author,
        description = cached.description,
        publishedAt = cached.publishedAtEpochMs?.let(Instant::ofEpochMilli),
        url = cached.url,
    )

    private companion object {
        const val FILE_NAME = "top_headlines_cache.json"
    }
}

data class CachedSnapshot(
    val articles: List<Article>,
    val savedAt: Instant,
)

/** Separate from [Article] so the storage format can change on its own, and so Instant
 *  can be stored as an epoch value. */
@Serializable
private data class CachedFeed(
    val savedAtEpochMs: Long,
    val articles: List<CachedArticle>,
)

@Serializable
private data class CachedArticle(
    val id: String,
    val title: String,
    val sourceName: String,
    val author: String? = null,
    val description: String? = null,
    val publishedAtEpochMs: Long? = null,
    val url: String,
)

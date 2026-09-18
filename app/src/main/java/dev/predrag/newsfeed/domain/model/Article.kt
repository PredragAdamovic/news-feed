package dev.predrag.newsfeed.domain.model

import java.time.Instant

/**
 * @param id derived from [url] — NewsAPI returns no id of its own. See `ArticleMapper.idFor`.
 */
data class Article(
    val id: String,
    val title: String,
    val sourceName: String,
    val author: String?,
    val description: String?,
    val publishedAt: Instant?,
    val url: String,
)

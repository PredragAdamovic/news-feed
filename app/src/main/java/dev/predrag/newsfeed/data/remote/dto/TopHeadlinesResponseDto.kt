package dev.predrag.newsfeed.data.remote.dto

import kotlinx.serialization.Serializable

/**
 * Every field is nullable or defaulted: this endpoint really does send articles with a
 * null author and a null description.
 */
@Serializable
data class TopHeadlinesResponseDto(
    val totalResults: Int = 0,
    val articles: List<ArticleDto> = emptyList(),
)

@Serializable
data class ArticleDto(
    val source: SourceDto? = null,
    val author: String? = null,
    val title: String? = null,
    val description: String? = null,
    val url: String? = null,
    val publishedAt: String? = null,
)

@Serializable
data class SourceDto(
    val name: String? = null,
)

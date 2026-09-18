package dev.predrag.newsfeed.domain.model

import java.time.Instant

/**
 * @param cachedAt non-null when the page came from the cache instead of the network.
 */
data class ArticlePage(
    val articles: List<Article>,
    val page: Int,
    val isLastPage: Boolean,
    val cachedAt: Instant? = null,
) {
    companion object {
        const val FIRST_PAGE = 1
    }
}

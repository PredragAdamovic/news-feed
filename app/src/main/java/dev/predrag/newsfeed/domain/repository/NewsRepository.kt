package dev.predrag.newsfeed.domain.repository

import dev.predrag.newsfeed.core.NewsResult
import dev.predrag.newsfeed.domain.model.Article
import dev.predrag.newsfeed.domain.model.ArticlePage

interface NewsRepository {

    suspend fun topHeadlines(page: Int): NewsResult<ArticlePage>

    /**
     * Resolved locally — NewsAPI has no by-id endpoint. Null when neither the session nor
     * the cache holds the article.
     */
    suspend fun articleById(id: String): Article?

    companion object {
        /** Pages are 1-based, matching the API. */
        const val FIRST_PAGE = 1
    }
}

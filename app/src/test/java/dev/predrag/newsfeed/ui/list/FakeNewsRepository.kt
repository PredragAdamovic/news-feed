package dev.predrag.newsfeed.ui.list

import dev.predrag.newsfeed.core.NewsError
import dev.predrag.newsfeed.core.NewsResult
import dev.predrag.newsfeed.domain.model.Article
import dev.predrag.newsfeed.domain.model.ArticlePage
import dev.predrag.newsfeed.domain.repository.NewsRepository
import java.time.Instant

class FakeNewsRepository : NewsRepository {

    /** Anything unlisted returns an empty last page. */
    val pages = mutableMapOf<Int, NewsResult<ArticlePage>>()

    var requestedPages = mutableListOf<Int>()
        private set

    override suspend fun topHeadlines(page: Int): NewsResult<ArticlePage> {
        requestedPages += page
        return pages[page] ?: NewsResult.Success(
            ArticlePage(articles = emptyList(), page = page, isLastPage = true)
        )
    }

    // Required by the interface; the list tests never resolve a single article.
    override suspend fun articleById(id: String): Article? = null

    fun succeedWith(page: Int, articles: List<Article>, isLastPage: Boolean = false) {
        pages[page] = NewsResult.Success(ArticlePage(articles, page, isLastPage))
    }

    fun failWith(page: Int, error: NewsError = NewsError.NoConnection) {
        pages[page] = NewsResult.Failure(error)
    }

    fun serveFromCache(page: Int, articles: List<Article>, savedAt: Instant) {
        pages[page] = NewsResult.Success(
            ArticlePage(articles, page, isLastPage = true, cachedAt = savedAt)
        )
    }
}

fun article(id: String): Article = Article(
    id = id,
    title = "Article $id",
    sourceName = "Source",
    author = null,
    description = null,
    publishedAt = null,
    url = "https://example.com/$id",
)

fun articles(vararg ids: String): List<Article> = ids.map(::article)

package dev.predrag.newsfeed.data

import dev.predrag.newsfeed.core.NewsError
import dev.predrag.newsfeed.core.NewsResult
import dev.predrag.newsfeed.data.local.ArticleCache
import dev.predrag.newsfeed.data.remote.ArticleMapper
import dev.predrag.newsfeed.data.remote.NewsApi
import dev.predrag.newsfeed.data.remote.dto.TopHeadlinesResponseDto
import dev.predrag.newsfeed.domain.model.Article
import dev.predrag.newsfeed.domain.model.ArticlePage
import dev.predrag.newsfeed.domain.repository.NewsRepository
import dev.predrag.newsfeed.domain.model.ArticlePage.Companion.FIRST_PAGE
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException

@Singleton
class NewsRepositoryImpl @Inject constructor(
    private val api: NewsApi,
    private val cache: ArticleCache,
    private val errorMapper: NewsErrorMapper,
    @param:Named(API_KEY) private val apiKey: String,
) : NewsRepository {

    /** Backs [articleById]: the detail screen is reached with an id and there is no endpoint for one. */
    private val loadedArticles = ConcurrentHashMap<String, Article>()

    override suspend fun topHeadlines(page: Int): NewsResult<ArticlePage> {
        // Checked up front so a missing key reads as a setup problem, not a 401.
        if (apiKey.isBlank()) return NewsResult.Failure(NewsError.MissingApiKey)

        return try {
            val response = api.topHeadlines(COUNTRY, page, PAGE_SIZE)
            val articles = ArticleMapper.toDomain(response.articles)

            articles.forEach { loadedArticles[it.id] = it }
            if (page == FIRST_PAGE) cache.save(articles)

            NewsResult.Success(
                ArticlePage(
                    articles = articles,
                    page = page,
                    isLastPage = response.isLastPage(page),
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            cachedFirstPageOrNull(page) ?: NewsResult.Failure(errorMapper.map(e))
        }
    }

    override suspend fun articleById(id: String): Article? =
        loadedArticles[id] ?: cache.read()?.articles?.firstOrNull { it.id == id }

    /**
     * First page only. Falling back mid-list would splice yesterday's articles under
     * today's, so a failure on page 2 stays a failure.
     */
    private suspend fun cachedFirstPageOrNull(page: Int): NewsResult<ArticlePage>? {
        if (page != FIRST_PAGE) return null

        val snapshot = cache.read()?.takeIf { it.articles.isNotEmpty() } ?: return null
        snapshot.articles.forEach { loadedArticles[it.id] = it }

        return NewsResult.Success(
            ArticlePage(
                articles = snapshot.articles,
                page = FIRST_PAGE,
                // The cache holds one page, so paging has to stop here.
                isLastPage = true,
                cachedAt = snapshot.savedAt,
            )
        )
    }

    /**
     * Not "fewer items than pageSize": NewsAPI counts articles it has removed in
     * totalResults but omits them from the payload, so full pages arrive short (7 or 9 for
     * a pageSize of 10) while further pages still exist.
     */
    private fun TopHeadlinesResponseDto.isLastPage(page: Int): Boolean =
        articles.isEmpty() || page * PAGE_SIZE >= totalResults

    companion object {
        const val API_KEY = "newsApiKey"

        const val PAGE_SIZE = 10
        private const val COUNTRY = "us"
    }
}

package dev.predrag.newsfeed.domain.usecase

import dev.predrag.newsfeed.core.NewsResult
import dev.predrag.newsfeed.domain.model.ArticlePage
import dev.predrag.newsfeed.domain.repository.NewsRepository
import javax.inject.Inject

class GetTopHeadlinesUseCase @Inject constructor(
    private val repository: NewsRepository,
) {
    suspend operator fun invoke(page: Int): NewsResult<ArticlePage> = repository.topHeadlines(page)
}

package dev.predrag.newsfeed.domain.usecase

import dev.predrag.newsfeed.domain.model.Article
import dev.predrag.newsfeed.domain.repository.NewsRepository
import javax.inject.Inject

/**
 * Null when neither the session nor the cache holds the article — NewsAPI has no by-id
 * endpoint, so an id can only be resolved against what this install has already seen.
 */
class GetArticleByIdUseCase @Inject constructor(
    private val repository: NewsRepository,
) {
    suspend operator fun invoke(id: String): Article? = repository.articleById(id)
}

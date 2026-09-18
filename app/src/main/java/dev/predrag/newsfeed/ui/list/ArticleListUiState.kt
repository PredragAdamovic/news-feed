package dev.predrag.newsfeed.ui.list

import dev.predrag.newsfeed.core.NewsError
import dev.predrag.newsfeed.domain.model.Article
import java.time.Instant

/**
 * Three error slots, because a failure means different things depending on what is already
 * on screen: [fullScreenError] when nothing is loaded, [appendError] when paging fails and
 * the list should survive, [transientError] (a snackbar, consumed once) when a refresh
 * fails but the list is intact.
 */
data class ArticleListUiState(
    val articles: List<Article> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isAppending: Boolean = false,
    val isLastPage: Boolean = false,
    val fullScreenError: NewsError? = null,
    val appendError: NewsError? = null,
    val transientError: NewsError? = null,
    val staleSince: Instant? = null,
) {
    val isEmpty: Boolean
        get() = articles.isEmpty() && !isLoading && fullScreenError == null

    val canAppend: Boolean
        get() = articles.isNotEmpty() && !isLastPage && !isAppending && appendError == null
}

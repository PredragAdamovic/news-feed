package dev.predrag.newsfeed.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.predrag.newsfeed.core.NewsResult
import dev.predrag.newsfeed.domain.model.Article
import dev.predrag.newsfeed.domain.model.ArticlePage.Companion.FIRST_PAGE
import dev.predrag.newsfeed.domain.usecase.GetTopHeadlinesUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ArticleListViewModel @Inject constructor(
    private val getTopHeadlines: GetTopHeadlinesUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ArticleListUiState())
    val uiState: StateFlow<ArticleListUiState> = _uiState.asStateFlow()

    private var nextPage = FIRST_PAGE
    private var firstPageJob: Job? = null
    private var appendJob: Job? = null

    init {
        loadFirstPage(isRefresh = false)
    }

    fun refresh() = loadFirstPage(isRefresh = true)

    fun retry() = loadFirstPage(isRefresh = false)

    /** Fires repeatedly while the user sits at the bottom — see [ArticleListUiState.canAppend]. */
    fun onNearEndOfList() {
        if (!_uiState.value.canAppend) return
        loadNextPage()
    }

    /** Unlike [onNearEndOfList], this is allowed to ignore an open appendError. */
    fun retryAppend() {
        val state = _uiState.value
        if (state.isAppending || state.isLastPage) return
        loadNextPage()
    }

    fun onTransientErrorShown() {
        _uiState.update { it.copy(transientError = null) }
    }

    private fun loadFirstPage(isRefresh: Boolean) {
        // An in-flight append would otherwise land on a list that no longer exists.
        firstPageJob?.cancel()
        appendJob?.cancel()

        firstPageJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = it.articles.isEmpty() && !isRefresh,
                    isRefreshing = isRefresh,
                    isAppending = false,
                    fullScreenError = null,
                    appendError = null,
                )
            }

            when (val result = getTopHeadlines(FIRST_PAGE)) {
                is NewsResult.Success -> {
                    nextPage = FIRST_PAGE + 1
                    _uiState.update {
                        it.copy(
                            articles = result.data.articles,
                            isLoading = false,
                            isRefreshing = false,
                            isLastPage = result.data.isLastPage,
                            staleSince = result.data.cachedAt,
                            fullScreenError = null,
                        )
                    }
                }

                is NewsResult.Failure -> _uiState.update {
                    val hasArticles = it.articles.isNotEmpty()
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        fullScreenError = result.error.takeIf { _ -> !hasArticles },
                        transientError = result.error.takeIf { _ -> hasArticles },
                    )
                }
            }
        }
    }

    private fun loadNextPage() {
        if (appendJob?.isActive == true) return

        appendJob = viewModelScope.launch {
            _uiState.update { it.copy(isAppending = true, appendError = null) }

            when (val result = getTopHeadlines(nextPage)) {
                is NewsResult.Success -> {
                    nextPage++
                    _uiState.update { state ->
                        state.copy(
                            articles = state.articles + result.data.articles.newTo(state),
                            isAppending = false,
                            isLastPage = result.data.isLastPage,
                        )
                    }
                }

                // Only the footer changes; the loaded articles stay.
                is NewsResult.Failure -> _uiState.update {
                    it.copy(isAppending = false, appendError = result.error)
                }
            }
        }
    }

    /**
     * Headlines shift between requests, so an article from page 1 can arrive again on
     * page 2 — and a duplicate key is a crash in LazyColumn, not a cosmetic problem.
     */
    private fun List<Article>.newTo(state: ArticleListUiState): List<Article> {
        val known = state.articles.mapTo(HashSet()) { it.id }
        return filterNot { it.id in known }
    }
}

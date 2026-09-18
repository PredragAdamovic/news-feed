package dev.predrag.newsfeed.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.predrag.newsfeed.domain.model.Article
import dev.predrag.newsfeed.domain.usecase.GetArticleByIdUseCase
import dev.predrag.newsfeed.ui.navigation.ArticleDetailRoute
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ArticleDetailUiState(
    val article: Article? = null,
    val isLoading: Boolean = true,
)

/**
 * The id arrives the same way from a list tap and from a deep link. A deep link can name an
 * article this process never loaded, which is an ordinary outcome rather than an error —
 * the state then simply has no article.
 */
@HiltViewModel
class ArticleDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    getArticleById: GetArticleByIdUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ArticleDetailUiState())
    val uiState: StateFlow<ArticleDetailUiState> = _uiState.asStateFlow()

    init {
        val articleId: String = savedStateHandle[ArticleDetailRoute.ARG_ARTICLE_ID] ?: ""
        viewModelScope.launch {
            _uiState.value = ArticleDetailUiState(
                article = getArticleById(articleId),
                isLoading = false,
            )
        }
    }
}

package dev.predrag.newsfeed.ui.list

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.predrag.newsfeed.R
import dev.predrag.newsfeed.domain.model.Article
import dev.predrag.newsfeed.ui.common.AppendErrorRow
import dev.predrag.newsfeed.ui.common.AppendLoadingRow
import dev.predrag.newsfeed.ui.common.EmptyState
import dev.predrag.newsfeed.ui.common.EndOfFeedRow
import dev.predrag.newsfeed.ui.common.FullScreenError
import dev.predrag.newsfeed.ui.common.FullScreenLoading
import dev.predrag.newsfeed.ui.common.StaleDataBanner
import dev.predrag.newsfeed.ui.common.toErrorText
import dev.predrag.newsfeed.ui.util.toDisplayDate
import dev.predrag.newsfeed.ui.util.toDisplayDateTime

/** Rows from the end at which the next page is requested, so it lands before the user does. */
private const val PREFETCH_DISTANCE = 3

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArticleListScreen(
    onArticleClick: (String) -> Unit,
    viewModel: ArticleListViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }

    listState.NotifyWhenNearEnd(enabled = state.canAppend, onNearEnd = viewModel::onNearEndOfList)

    val transientError = state.transientError
    val transientText = transientError?.toErrorText()
    LaunchedEffect(transientError) {
        if (transientText != null) {
            snackbarHostState.showSnackbar(transientText.title)
            viewModel.onTransientErrorShown()
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.list_title)) }) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = viewModel::refresh,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            val fullScreenError = state.fullScreenError

            when {
                state.isLoading -> FullScreenLoading()

                fullScreenError != null -> {
                    val text = fullScreenError.toErrorText()
                    FullScreenError(
                        title = text.title,
                        body = text.body,
                        onRetry = viewModel::retry,
                    )
                }

                // Inside PullToRefreshBox so an empty feed stays pullable.
                state.isEmpty -> EmptyState()

                else -> ArticleList(
                    state = state,
                    listState = listState,
                    onArticleClick = onArticleClick,
                    onRetryAppend = viewModel::retryAppend,
                )
            }
        }
    }
}

@Composable
private fun ArticleList(
    state: ArticleListUiState,
    listState: LazyListState,
    onArticleClick: (String) -> Unit,
    onRetryAppend: () -> Unit,
) {
    LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
        state.staleSince?.let { savedAt ->
            item(key = "stale-banner") {
                StaleDataBanner(savedAt = savedAt.toDisplayDateTime())
            }
        }

        items(state.articles, key = { it.id }) { article ->
            ArticleRow(article = article, onClick = { onArticleClick(article.id) })
            HorizontalDivider()
        }

        when {
            state.appendError != null -> item(key = "append-error") { AppendErrorRow(onRetryAppend) }
            state.isAppending -> item(key = "append-loading") { AppendLoadingRow() }
            state.isLastPage -> item(key = "end-of-feed") { EndOfFeedRow() }
        }
    }
}

@Composable
private fun ArticleRow(article: Article, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Text(
            text = article.title,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 3,
        )
        Text(
            text = article.subtitle(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}

@Composable
private fun Article.subtitle(): String =
    listOfNotNull(
        sourceName.takeIf { it.isNotBlank() },
        publishedAt?.toDisplayDate(),
    ).joinToString(" · ")

/**
 * derivedStateOf keeps this cheap: the lambda runs every scroll frame, but the effect only
 * re-runs when the boolean flips.
 */
@Composable
private fun LazyListState.NotifyWhenNearEnd(enabled: Boolean, onNearEnd: () -> Unit) {
    val isNearEnd by remember(this) {
        derivedStateOf {
            val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: return@derivedStateOf false
            val total = layoutInfo.totalItemsCount
            total > 0 && lastVisible >= total - 1 - PREFETCH_DISTANCE
        }
    }

    LaunchedEffect(isNearEnd, enabled) {
        if (isNearEnd && enabled) onNearEnd()
    }
}

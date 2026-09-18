package dev.predrag.newsfeed.ui.list

import dev.predrag.newsfeed.core.NewsError
import dev.predrag.newsfeed.util.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class ArticleListViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeNewsRepository()

    @Test
    fun `loads the first page on creation`() = runTest {
        repository.succeedWith(page = 1, articles = articles("a", "b"))

        val viewModel = ArticleListViewModel(repository)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(listOf("a", "b"), state.articles.map { it.id })
        assertFalse(state.isLoading)
        assertNull(state.fullScreenError)
    }

    @Test
    fun `a first-page failure with nothing loaded becomes a full-screen error`() = runTest {
        repository.failWith(page = 1, error = NewsError.NoConnection)

        val viewModel = ArticleListViewModel(repository)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(NewsError.NoConnection, state.fullScreenError)
        assertTrue(state.articles.isEmpty())
    }

    @Test
    fun `scrolling near the end appends the next page`() = runTest {
        repository.succeedWith(page = 1, articles = articles("a", "b"))
        repository.succeedWith(page = 2, articles = articles("c", "d"))

        val viewModel = ArticleListViewModel(repository)
        advanceUntilIdle()

        viewModel.onNearEndOfList()
        advanceUntilIdle()

        assertEquals(listOf("a", "b", "c", "d"), viewModel.uiState.value.articles.map { it.id })
    }

    @Test
    fun `a failed append keeps the loaded articles and offers a retry`() = runTest {
        repository.succeedWith(page = 1, articles = articles("a", "b"))
        repository.failWith(page = 2)

        val viewModel = ArticleListViewModel(repository)
        advanceUntilIdle()

        viewModel.onNearEndOfList()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(listOf("a", "b"), state.articles.map { it.id })
        assertEquals(NewsError.NoConnection, state.appendError)
        assertNull(state.fullScreenError)
        assertFalse(state.isAppending)
    }

    @Test
    fun `scrolling does not retry on its own after an append failed`() = runTest {
        repository.succeedWith(page = 1, articles = articles("a", "b"))
        repository.failWith(page = 2)

        val viewModel = ArticleListViewModel(repository)
        advanceUntilIdle()
        viewModel.onNearEndOfList()
        advanceUntilIdle()

        // Without the guard, sitting at the bottom would hammer a failing endpoint.
        repeat(3) { viewModel.onNearEndOfList() }
        advanceUntilIdle()

        assertEquals(listOf(1, 2), repository.requestedPages)
    }

    @Test
    fun `an explicit retry does request the page again`() = runTest {
        repository.succeedWith(page = 1, articles = articles("a"))
        repository.failWith(page = 2)

        val viewModel = ArticleListViewModel(repository)
        advanceUntilIdle()
        viewModel.onNearEndOfList()
        advanceUntilIdle()

        repository.succeedWith(page = 2, articles = articles("b"), isLastPage = true)
        viewModel.retryAppend()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(listOf("a", "b"), state.articles.map { it.id })
        assertNull(state.appendError)
    }

    @Test
    fun `paging stops once the API reports the last page`() = runTest {
        repository.succeedWith(page = 1, articles = articles("a"), isLastPage = true)

        val viewModel = ArticleListViewModel(repository)
        advanceUntilIdle()

        repeat(3) { viewModel.onNearEndOfList() }
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isLastPage)
        assertEquals(listOf(1), repository.requestedPages)
    }

    @Test
    fun `an article repeated on the next page is not added twice`() = runTest {
        repository.succeedWith(page = 1, articles = articles("a", "b"))
        repository.succeedWith(page = 2, articles = articles("b", "c"))

        val viewModel = ArticleListViewModel(repository)
        advanceUntilIdle()
        viewModel.onNearEndOfList()
        advanceUntilIdle()

        assertEquals(listOf("a", "b", "c"), viewModel.uiState.value.articles.map { it.id })
    }

    @Test
    fun `a failed refresh keeps the list and reports the error transiently`() = runTest {
        repository.succeedWith(page = 1, articles = articles("a", "b"))

        val viewModel = ArticleListViewModel(repository)
        advanceUntilIdle()

        repository.failWith(page = 1)
        viewModel.refresh()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(listOf("a", "b"), state.articles.map { it.id })
        assertNull(state.fullScreenError)
        assertEquals(NewsError.NoConnection, state.transientError)

        viewModel.onTransientErrorShown()
        assertNull(viewModel.uiState.value.transientError)
    }

    @Test
    fun `cached articles are flagged stale and stop paging`() = runTest {
        val savedAt = Instant.parse("2026-09-17T10:00:00Z")
        repository.serveFromCache(page = 1, articles = articles("a"), savedAt = savedAt)

        val viewModel = ArticleListViewModel(repository)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(savedAt, state.staleSince)
        // A cache holds one page; without this the list would keep asking for page 2 offline.
        assertTrue(state.isLastPage)
    }

    @Test
    fun `an empty successful response is an empty state, not an error`() = runTest {
        repository.succeedWith(page = 1, articles = emptyList(), isLastPage = true)

        val viewModel = ArticleListViewModel(repository)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.isEmpty)
        assertNull(state.fullScreenError)
    }
}

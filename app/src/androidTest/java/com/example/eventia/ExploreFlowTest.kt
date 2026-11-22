package com.example.eventia

import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.eventia.domain.model.Category
import com.example.eventia.domain.model.Event
import com.example.eventia.domain.model.Venue
import com.example.eventia.repositories.CatalogRepository
import com.example.eventia.repositories.CategoryRepository
import com.example.eventia.ui.screens.ExploreFilters
import com.example.eventia.ui.screens.ExploreViewModel
import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExploreFlowTest {
    @Test
    fun filtersCoalesceCancelAndRestore() = onMain {
        val repository = PendingCatalog()
        val saved = SavedStateHandle()
        val viewModel = ExploreViewModel(NoCategories, repository, saved)
        val initial = repository.requests.receive()

        viewModel.changeQuery("Co")
        viewModel.changeQuery("Compose")
        viewModel.selectCategory(7)
        initial.finished.await()
        assertTrue(repository.requests.tryReceive().isFailure)
        val filtered = repository.requests.receive()
        assertEquals(ExploreFilters("Compose", 7), filtered.filters)
        filtered.result.complete(emptyList())
        viewModel.state.first { !it.isLoading }

        val restored = ExploreViewModel(NoCategories, repository, saved)
        assertEquals(filtered.filters, restored.state.value.filters)
        val restoredRequest = repository.requests.receive()
        assertEquals(filtered.filters, restoredRequest.filters)

        restored.clearFilters()
        restoredRequest.finished.await()
        val cleared = repository.requests.receive()
        assertEquals(ExploreFilters(), cleared.filters)
        cleared.result.complete(emptyList())
        restored.state.first { !it.isLoading }
    }

    @Test
    fun refreshKeepsPreviousResultOnFailureAndCanRetry() = onMain {
        val repository = PendingCatalog()
        val viewModel = ExploreViewModel(NoCategories, repository, SavedStateHandle())
        repository.requests.receive().result.complete(emptyList())
        val previous = viewModel.state.first { !it.isLoading }.content

        viewModel.refresh()
        assertTrue(viewModel.state.value.isLoading)
        assertSame(previous, viewModel.state.value.content)
        repository.requests.receive().result.completeExceptionally(IOException())
        val failed = viewModel.state.first { !it.isLoading }
        assertTrue(failed.hasError)
        assertSame(previous, failed.content)

        viewModel.refresh()
        repository.requests.receive().result.complete(emptyList())
        val retried = viewModel.state.first { !it.isLoading }
        assertEquals(false, retried.hasError)
        assertEquals(previous, retried.content)
    }

    private fun onMain(test: suspend () -> Unit) = runBlocking {
        withTimeout(15_000) { withContext(Dispatchers.Main.immediate) { test() } }
    }

    private object NoCategories : CategoryRepository {
        override suspend fun getCategories(): List<Category> = emptyList()
    }

    private class PendingCatalog : CatalogRepository {
        val requests = Channel<PendingRequest>(Channel.UNLIMITED)

        override suspend fun getEvents(query: String?, categoryId: Long?): List<Event> {
            val request = PendingRequest(ExploreFilters(query.orEmpty(), categoryId))
            return try {
                requests.send(request)
                request.result.await()
            } finally {
                request.finished.complete(Unit)
            }
        }

        override suspend fun getEvent(id: Long): Event = error("Unexpected detail request")
        override suspend fun getVenues(): List<Venue> = error("Event search must not fetch venues")
        override suspend fun getVenue(id: Long): Venue = error("Event search must not fetch venue details")
        override suspend fun getVenueEvents(id: Long): List<Event> =
            error("Event search must not fetch venue events")
    }

    private class PendingRequest(val filters: ExploreFilters) {
        val result = CompletableDeferred<List<Event>>()
        val finished = CompletableDeferred<Unit>()
    }
}

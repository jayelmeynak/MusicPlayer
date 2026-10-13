package com.jayelmeynak.feature.localmusic.impl.domain.usecase

import com.jayelmeynak.feature.localmusic.impl.testing.FakeLocalTracksRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ObserveLibraryChangesUseCaseTest {

    private val repository = FakeLocalTracksRepository()
    private val useCase = ObserveLibraryChangesUseCase(repository)

    @Test
    fun `пачка изменений - одно событие после паузы`() = runTest {
        var events = 0
        backgroundScope.launch { useCase().collect { events++ } }
        advanceTimeBy(1)

        repeat(5) {
            repository.changes.emit(Unit)
            advanceTimeBy(100)
        }
        advanceTimeBy(LIBRARY_CHANGES_DEBOUNCE_MS - 101)
        assertEquals(0, events)

        advanceTimeBy(2)
        assertEquals(1, events)
    }

    @Test
    fun `изменения с паузой больше дребезга - отдельные события`() = runTest {
        var events = 0
        backgroundScope.launch { useCase().collect { events++ } }
        advanceTimeBy(1)

        repository.changes.emit(Unit)
        advanceTimeBy(LIBRARY_CHANGES_DEBOUNCE_MS + 1)
        repository.changes.emit(Unit)
        advanceTimeBy(LIBRARY_CHANGES_DEBOUNCE_MS + 1)

        assertEquals(2, events)
    }
}

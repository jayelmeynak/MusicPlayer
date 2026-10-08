package com.jayelmeynak.util.result

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class ResultTest {

    private val success: Result<Int, DataError.Remote> = Result.Success(2)
    private val error: Result<Int, DataError.Remote> = Result.Error(DataError.Remote.NO_INTERNET)

    @Test
    fun `map - преобразует данные успеха`() {
        assertEquals(Result.Success("4"), success.map { (it * 2).toString() })
    }

    @Test
    fun `map - ошибка проходит без вызова преобразования`() {
        var called = false
        val mapped = error.map { called = true; it }

        assertEquals(Result.Error(DataError.Remote.NO_INTERNET), mapped)
        assertEquals(false, called)
    }

    @Test
    fun `asEmptyDataResult - успех без данных, ошибка сохраняется`() {
        assertEquals(Result.Success(Unit), success.asEmptyDataResult())
        assertEquals(Result.Error(DataError.Remote.NO_INTERNET), error.asEmptyDataResult())
    }

    @Test
    fun `onSuccess - вызывается только для успеха и возвращает тот же результат`() {
        var seen: Int? = null

        assertSame(success, success.onSuccess { seen = it })
        assertEquals(2, seen)

        seen = null
        assertSame(error, error.onSuccess { seen = it })
        assertNull(seen)
    }

    @Test
    fun `onError - вызывается только для ошибки и возвращает тот же результат`() {
        var seen: DataError.Remote? = null

        assertSame(error, error.onError { seen = it })
        assertEquals(DataError.Remote.NO_INTERNET, seen)

        seen = null
        assertSame(success, success.onError { seen = it })
        assertNull(seen)
    }
}

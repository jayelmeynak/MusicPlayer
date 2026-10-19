package com.jayelmeynak.util.result

// Внутри Result простое Error — вложенный Result.Error, поэтому общий интерфейс ошибки — под алиасом.
import com.jayelmeynak.util.result.Error as ResultError

public sealed interface Result<out D, out E: ResultError> {
    public data class Success<out D>(public val data: D): Result<D, Nothing>
    public data class Error<out E: ResultError>(public val error: E):
        Result<Nothing, E>
}

public inline fun <T, E: ResultError, R> Result<T, E>.map(map: (T) -> R): Result<R, E> {
    return when(this) {
        is Result.Error -> Result.Error(error)
        is Result.Success -> Result.Success(map(data))
    }
}

public fun <T, E: ResultError> Result<T, E>.asEmptyDataResult(): EmptyResult<E> {
    return map {  }
}

public inline fun <T, E: ResultError> Result<T, E>.onSuccess(action: (T) -> Unit): Result<T, E> {
    return when(this) {
        is Result.Error -> this
        is Result.Success -> {
            action(data)
            this
        }
    }
}
public inline fun <T, E: ResultError> Result<T, E>.onError(action: (E) -> Unit): Result<T, E> {
    return when(this) {
        is Result.Error -> {
            action(error)
            this
        }
        is Result.Success -> this
    }
}

public typealias EmptyResult<E> = Result<Unit, E>
package com.familyos.core.domain.util

/**
 * Domain-level operation result that never throws across use-case boundaries.
 *
 * @param T success payload type
 */
sealed class Result<out T> {

    /** Successful outcome carrying [data]. */
    data class Success<T>(val data: T) : Result<T>()

    /** Failed outcome carrying a typed [error]. */
    data class Error(val error: AppError) : Result<Nothing>()

    /** True when this is [Success]. */
    val isSuccess: Boolean get() = this is Success

    /** True when this is [Error]. */
    val isError: Boolean get() = this is Error

    /** Returns the success value or null. */
    fun getOrNull(): T? = (this as? Success)?.data

    /** Returns the error or null. */
    fun errorOrNull(): AppError? = (this as? Error)?.error

    /** Maps success data with [transform], preserving errors. */
    inline fun <R> map(transform: (T) -> R): Result<R> = when (this) {
        is Success -> Success(transform(data))
        is Error -> this
    }

    /** Flat-maps success data with [transform], preserving errors. */
    inline fun <R> flatMap(transform: (T) -> Result<R>): Result<R> = when (this) {
        is Success -> transform(data)
        is Error -> this
    }

    /** Executes [action] when this is [Success]. */
    inline fun onSuccess(action: (T) -> Unit): Result<T> {
        if (this is Success) action(data)
        return this
    }

    /** Executes [action] when this is [Error]. */
    inline fun onError(action: (AppError) -> Unit): Result<T> {
        if (this is Error) action(error)
        return this
    }

    companion object {
        /** Creates a successful [Result]. */
        fun <T> success(data: T): Result<T> = Success(data)

        /** Creates a failed [Result]. */
        fun <T> failure(error: AppError): Result<T> = Error(error)

        /**
         * Executes [block] and wraps the outcome, mapping unexpected throwables
         * to [AppError.Unknown].
         */
        inline fun <T> runCatching(block: () -> T): Result<T> = try {
            Success(block())
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: AppException) {
            Error(e.error)
        } catch (e: Throwable) {
            Error(AppError.Unknown(e.message ?: "Unexpected error", e))
        }
    }
}

/**
 * Exception that carries a domain [AppError] for interop with throwable-based APIs.
 */
class AppException(val error: AppError) : Exception(error.message, error.cause)

package com.familyos.core.ext

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlin.coroutines.CoroutineContext

/**
 * Runs [block] and maps success/failure into a pair of nullable value and throwable,
 * rethrowing [CancellationException].
 */
inline fun <T> runCatchingCancellable(block: () -> T): Result<T> {
    return try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Throwable) {
        Result.failure(e)
    }
}

/**
 * Maps each emission with [transform], swallowing non-cancellation errors into null.
 */
fun <T, R> Flow<T>.mapCatching(transform: (T) -> R): Flow<R?> =
    map { value ->
        try {
            transform(value)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Throwable) {
            null
        }
    }

/**
 * Emits errors from this flow into [onError] without completing the collector with an exception,
 * unless the error is a [CancellationException].
 */
fun <T> Flow<T>.catchNonCancellation(onError: suspend (Throwable) -> Unit): Flow<T> =
    catch { throwable ->
        if (throwable is CancellationException) throw throwable
        onError(throwable)
    }

/**
 * Returns this string if it is not blank, otherwise [fallback].
 */
fun String?.orFallback(fallback: String): String =
    if (this.isNullOrBlank()) fallback else this

/**
 * Formats epoch millis as ISO-8601 UTC string without allocating a formatter repeatedly
 * when a simple display is needed at the core layer.
 */
fun Long.toIsoInstantString(): String =
    java.time.Instant.ofEpochMilli(this).toString()

/**
 * Ensures a coroutine context includes a named element for debugging.
 */
fun CoroutineContext.withDebugName(name: String): CoroutineContext =
    this + kotlinx.coroutines.CoroutineName(name)

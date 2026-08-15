package com.familyos.core.domain.util

/**
 * Typed application errors surfaced by repositories and use cases.
 */
sealed class AppError {
    abstract val message: String
    open val cause: Throwable? get() = null

    /** The caller is not authenticated. */
    data class Unauthorized(
        override val message: String = "Authentication required",
        override val cause: Throwable? = null,
    ) : AppError()

    /** The caller lacks permission for the requested action. */
    data class Forbidden(
        override val message: String = "Permission denied",
        override val cause: Throwable? = null,
    ) : AppError()

    /** A requested entity was not found. */
    data class NotFound(
        val entity: String,
        val id: String? = null,
        override val cause: Throwable? = null,
    ) : AppError() {
        override val message: String =
            if (id != null) "$entity not found: $id" else "$entity not found"
    }

    /** Input failed domain validation. */
    data class Validation(
        override val message: String,
        val field: String? = null,
        override val cause: Throwable? = null,
    ) : AppError()

    /** Remote backend rejected the request. */
    data class Remote(
        override val message: String,
        val code: String? = null,
        override val cause: Throwable? = null,
    ) : AppError()

    /** Local persistence failed. */
    data class Local(
        override val message: String,
        override val cause: Throwable? = null,
    ) : AppError()

    /** Device is offline and the operation cannot proceed. */
    data class Network(
        override val message: String = "No network connection",
        override val cause: Throwable? = null,
    ) : AppError()

    /** Sync conflict that could not be auto-resolved. */
    data class Conflict(
        override val message: String,
        val localUpdatedAt: Long,
        val remoteUpdatedAt: Long,
        override val cause: Throwable? = null,
    ) : AppError()

    /** Billing / subscription related failure. */
    data class Billing(
        override val message: String,
        override val cause: Throwable? = null,
    ) : AppError()

    /** Catch-all for unexpected failures. */
    data class Unknown(
        override val message: String = "Unknown error",
        override val cause: Throwable? = null,
    ) : AppError()
}

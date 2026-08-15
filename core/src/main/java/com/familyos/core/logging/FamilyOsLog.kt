package com.familyos.core.logging

import timber.log.Timber

/**
 * Thin Timber wrapper used across FamilyOS modules for consistent tagging.
 */
object FamilyOsLog {

    /** Plants a debug tree when [isDebug] is true. */
    fun init(isDebug: Boolean) {
        if (isDebug) {
            Timber.plant(Timber.DebugTree())
        }
    }

    /** Logs a debug message with optional [tag]. */
    fun d(tag: String = "FamilyOS", message: String, throwable: Throwable? = null) {
        Timber.tag(tag).d(throwable, message)
    }

    /** Logs an info message with optional [tag]. */
    fun i(tag: String = "FamilyOS", message: String, throwable: Throwable? = null) {
        Timber.tag(tag).i(throwable, message)
    }

    /** Logs a warning with optional [tag]. */
    fun w(tag: String = "FamilyOS", message: String, throwable: Throwable? = null) {
        Timber.tag(tag).w(throwable, message)
    }

    /** Logs an error with optional [tag]. */
    fun e(tag: String = "FamilyOS", message: String, throwable: Throwable? = null) {
        Timber.tag(tag).e(throwable, message)
    }
}

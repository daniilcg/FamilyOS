package com.familyos.core.data.auth

import com.familyos.core.data.preferences.UserPreferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * In-memory + DataStore session holder for local auth.
 * Persists [currentUserId] across restarts only when remember-me is enabled.
 */
@Singleton
class AuthSessionStore @Inject constructor(
    private val preferences: UserPreferencesDataStore,
) {
    private val mutex = Mutex()
    private val sessionUserId = MutableStateFlow<String?>(null)
    private var hydrated = false

    /** Observes the active session user id (null when signed out). */
    fun observeUserId(): Flow<String?> = flow {
        ensureHydrated()
        emitAll(sessionUserId)
    }

    /** Returns the current session user id after ensuring hydration. */
    suspend fun getUserId(): String? {
        ensureHydrated()
        return sessionUserId.value
    }

    /**
     * Sets the active session. When [persist] is true (remember-me), writes to DataStore.
     * When false, keeps the session in memory only for this process.
     */
    suspend fun setUserId(userId: String?, persist: Boolean) {
        ensureHydrated()
        mutex.withLock {
            sessionUserId.value = userId
            if (persist) {
                preferences.setActiveSessionUserId(userId)
            } else {
                preferences.setActiveSessionUserId(null)
            }
        }
    }

    /** Clears memory and persisted session. */
    suspend fun clear() {
        ensureHydrated()
        mutex.withLock {
            sessionUserId.value = null
            preferences.setActiveSessionUserId(null)
        }
    }

    private suspend fun ensureHydrated() {
        if (hydrated) return
        mutex.withLock {
            if (hydrated) return
            val prefs = preferences.get()
            if (prefs.rememberMe) {
                sessionUserId.value = prefs.activeSessionUserId
            } else {
                // Discard any leftover persisted session from a previous run.
                if (prefs.activeSessionUserId != null) {
                    preferences.setActiveSessionUserId(null)
                }
                sessionUserId.value = null
            }
            hydrated = true
        }
    }
}

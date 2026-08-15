package com.familyos.core.domain.repository

/**
 * Reports whether cloud (Firebase) authentication is available for this install.
 */
interface AuthModeProvider {
    /** True when a real Firebase project is configured and Auth can be used. */
    fun isCloudAuthAvailable(): Boolean

    /** True when the app should use the local Room auth backend. */
    fun isLocalAuthMode(): Boolean = !isCloudAuthAvailable()
}

package com.familyos.core.domain.model

import kotlinx.serialization.Serializable

/**
 * Persisted user preference values stored via DataStore.
 */
@Serializable
data class UserPreferences(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val rememberMe: Boolean = true,
    val activeFamilyId: String? = null,
    /** Persisted local-auth session user id for remember-me / auto-login. */
    val activeSessionUserId: String? = null,
    val biometricEnabled: Boolean = false,
    val languageTag: String = "en",
    val currencyCode: String = "EUR",
    val aiProvider: String = "openai",
    /** Encrypted reference id / alias for the AI API key — never the raw key in cleartext prefs. */
    val aiApiKeyAlias: String? = null,
    val notificationsEnabled: Boolean = true,
)

/**
 * App theme preference.
 */
@Serializable
enum class ThemeMode {
    LIGHT,
    DARK,
    SYSTEM,
}

/**
 * Aggregated home dashboard snapshot.
 */
@Serializable
data class HomeDashboard(
    val family: Family?,
    val members: List<FamilyMember> = emptyList(),
    val upcomingEvents: List<CalendarEvent> = emptyList(),
    val openTasks: List<TaskItem> = emptyList(),
    val activeShopping: List<ShoppingItem> = emptyList(),
    val recentNotes: List<Note> = emptyList(),
    val unreadNotifications: Int = 0,
    val budgetSummary: BudgetSummary? = null,
    val recentActivity: List<FamilyActivity> = emptyList(),
)

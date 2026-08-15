package com.familyos.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.familyos.core.domain.model.BudgetSummary
import com.familyos.core.domain.model.CalendarEvent
import com.familyos.core.domain.model.FamilyActivity
import com.familyos.core.domain.model.HomeDashboard
import com.familyos.core.domain.model.ShoppingItem
import com.familyos.core.domain.model.TaskItem
import com.familyos.core.domain.repository.UserPreferencesRepository
import com.familyos.core.domain.usecase.auth.ObserveAuthStateUseCase
import com.familyos.core.domain.usecase.budget.GetBudgetSummaryUseCase
import com.familyos.core.domain.usecase.home.ObserveHomeDashboardUseCase
import com.familyos.core.domain.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneOffset
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject

/**
 * Aggregated home dashboard UI state.
 */
data class HomeUiState(
    val isLoading: Boolean = true,
    val familyName: String? = null,
    val openTasks: List<TaskItem> = emptyList(),
    val overdueTasks: List<TaskItem> = emptyList(),
    val shoppingItems: List<ShoppingItem> = emptyList(),
    val todayEvents: List<CalendarEvent> = emptyList(),
    val upcomingDeadlines: List<TaskItem> = emptyList(),
    val monthBudget: BudgetSummary? = null,
    val recentActivity: List<FamilyActivity> = emptyList(),
    val unreadNotifications: Int = 0,
    val errorMessage: String? = null,
    val needsFamily: Boolean = false,
)

/**
 * ViewModel that aggregates home dashboard use cases into a single UI state.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val observeHomeDashboard: ObserveHomeDashboardUseCase,
    private val getBudgetSummary: GetBudgetSummaryUseCase,
    private val observeAuthState: ObserveAuthStateUseCase,
    private val userPreferencesRepository: UserPreferencesRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        observeDashboard()
    }

    /** Reloads budget summary for the current month. */
    fun refreshBudget() {
        viewModelScope.launch {
            val familyId = resolveFamilyId() ?: return@launch
            val (start, end) = currentMonthRange()
            when (val result = getBudgetSummary(familyId, start, end)) {
                is Result.Success -> _uiState.update { it.copy(monthBudget = result.data) }
                is Result.Error -> _uiState.update { it.copy(errorMessage = result.error.message) }
            }
        }
    }

    /** Clears the transient error banner. */
    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    private fun observeDashboard() {
        viewModelScope.launch {
            combine(
                observeAuthState(),
                userPreferencesRepository.observe().map { it.activeFamilyId }.distinctUntilChanged(),
            ) { user, prefFamilyId ->
                val familyId = user?.familyId ?: prefFamilyId
                val userId = user?.id
                familyId to userId
            }.flatMapLatest { (familyId, userId) ->
                if (familyId.isNullOrBlank() || userId.isNullOrBlank()) {
                    _uiState.update {
                        it.copy(isLoading = false, needsFamily = true, familyName = null)
                    }
                    flowOf(null)
                } else {
                    _uiState.update { it.copy(needsFamily = false, isLoading = true) }
                    observeHomeDashboard(familyId, userId).map { dashboard ->
                        DashboardBundle(familyId, dashboard)
                    }
                }
            }.collectLatest { bundle ->
                if (bundle == null) return@collectLatest
                val now = System.currentTimeMillis()
                val startOfDay = startOfDayMillis(now)
                val endOfDay = startOfDay + 24L * 60L * 60L * 1000L
                val open = bundle.dashboard.openTasks
                val overdue = open.filter { task ->
                    val due = task.dueAt
                    due != null && due < now
                }
                val upcoming = open.filter { task ->
                    val due = task.dueAt
                    due != null && due >= now
                }.sortedBy { it.dueAt }.take(8)
                val todayEvents = bundle.dashboard.upcomingEvents.filter { event ->
                    event.startAt < endOfDay && event.endAt >= startOfDay
                }
                val (monthStart, monthEnd) = currentMonthRange()
                val budget = getBudgetSummary(bundle.familyId, monthStart, monthEnd).getOrNull()
                    ?: bundle.dashboard.budgetSummary

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        familyName = bundle.dashboard.family?.name,
                        openTasks = open,
                        overdueTasks = overdue,
                        shoppingItems = bundle.dashboard.activeShopping,
                        todayEvents = todayEvents,
                        upcomingDeadlines = upcoming,
                        monthBudget = budget,
                        recentActivity = bundle.dashboard.recentActivity,
                        unreadNotifications = bundle.dashboard.unreadNotifications,
                    )
                }
            }
        }
    }

    private suspend fun resolveFamilyId(): String? {
        val userFamilyId = observeAuthState().first()?.familyId
        return userFamilyId ?: userPreferencesRepository.get().activeFamilyId
    }

    private data class DashboardBundle(
        val familyId: String,
        val dashboard: HomeDashboard,
    )
}

private fun currentMonthRange(): Pair<Long, Long> {
    val zone = ZoneOffset.UTC
    val today = Instant.ofEpochMilli(System.currentTimeMillis()).atZone(zone).toLocalDate()
    val start = today.with(TemporalAdjusters.firstDayOfMonth())
        .atStartOfDay().toInstant(zone).toEpochMilli()
    val end = today.with(TemporalAdjusters.lastDayOfMonth())
        .plusDays(1).atStartOfDay().toInstant(zone).toEpochMilli()
    return start to end
}

private fun startOfDayMillis(now: Long): Long {
    val zone = ZoneOffset.UTC
    return Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
        .atStartOfDay().toInstant(zone).toEpochMilli()
}

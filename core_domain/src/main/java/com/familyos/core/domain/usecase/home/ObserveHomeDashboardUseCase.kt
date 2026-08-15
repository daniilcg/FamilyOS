package com.familyos.core.domain.usecase.home

import com.familyos.core.domain.model.BudgetSummary
import com.familyos.core.domain.model.CalendarEvent
import com.familyos.core.domain.model.Family
import com.familyos.core.domain.model.FamilyMember
import com.familyos.core.domain.model.HomeDashboard
import com.familyos.core.domain.model.Note
import com.familyos.core.domain.model.ShoppingItem
import com.familyos.core.domain.model.ShoppingStatus
import com.familyos.core.domain.model.TaskItem
import com.familyos.core.domain.model.TaskStatus
import com.familyos.core.domain.repository.BudgetRepository
import com.familyos.core.domain.repository.CalendarRepository
import com.familyos.core.domain.repository.FamilyRepository
import com.familyos.core.domain.repository.NoteRepository
import com.familyos.core.domain.repository.NotificationRepository
import com.familyos.core.domain.repository.ShoppingRepository
import com.familyos.core.domain.repository.TaskRepository
import com.familyos.core.domain.util.Result
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.ZoneOffset
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject

/**
 * Aggregates home dashboard data from multiple repositories.
 */
class ObserveHomeDashboardUseCase @Inject constructor(
    private val familyRepository: FamilyRepository,
    private val taskRepository: TaskRepository,
    private val shoppingRepository: ShoppingRepository,
    private val calendarRepository: CalendarRepository,
    private val noteRepository: NoteRepository,
    private val notificationRepository: NotificationRepository,
    private val budgetRepository: BudgetRepository,
) {
    operator fun invoke(familyId: String, userId: String): Flow<HomeDashboard> {
        val now = System.currentTimeMillis()
        val weekEnd = now + 7L * 24L * 60L * 60L * 1000L

        val familyBlock: Flow<Triple<Family?, List<FamilyMember>, Int>> = combine(
            familyRepository.observeFamily(familyId),
            familyRepository.observeMembers(familyId),
            notificationRepository.observeUnreadCount(userId),
        ) { family, members, unread -> Triple(family, members, unread) }

        val contentBlock: Flow<DashboardContent> = combine(
            taskRepository.observeTasks(familyId, TaskStatus.NEW),
            shoppingRepository.observeItems(familyId, ShoppingStatus.ACTIVE),
            calendarRepository.observeEvents(familyId, now, weekEnd),
            noteRepository.observeNotes(familyId),
        ) { tasks, shopping, events, notes ->
            DashboardContent(tasks, shopping, events, notes)
        }

        return combine(familyBlock, contentBlock) { familyPart, content ->
            HomeDashboard(
                family = familyPart.first,
                members = familyPart.second,
                upcomingEvents = content.events.sortedBy { it.startAt }.take(5),
                openTasks = content.tasks.sortedBy { it.dueAt ?: Long.MAX_VALUE }.take(5),
                activeShopping = content.shopping.take(8),
                recentNotes = content.notes.sortedByDescending { it.updatedAt }.take(5),
                unreadNotifications = familyPart.third,
                budgetSummary = null,
                recentActivity = emptyList(),
            )
        }
    }

    /** One-shot load including budget summary for the current month. */
    suspend fun loadOnce(familyId: String, userId: String): Result<HomeDashboard> = coroutineScope {
        Result.runCatching {
            val zone = ZoneOffset.UTC
            val today = Instant.ofEpochMilli(System.currentTimeMillis()).atZone(zone).toLocalDate()
            val monthStart = today.with(TemporalAdjusters.firstDayOfMonth())
                .atStartOfDay().toInstant(zone).toEpochMilli()
            val monthEnd = today.with(TemporalAdjusters.lastDayOfMonth())
                .plusDays(1).atStartOfDay().toInstant(zone).toEpochMilli()
            val base = async { invoke(familyId, userId).first() }
            val summary = async {
                budgetRepository.summarize(familyId, monthStart, monthEnd).getOrNull()
            }
            base.await().copy(budgetSummary = summary.await())
        }
    }

    private data class DashboardContent(
        val tasks: List<TaskItem>,
        val shopping: List<ShoppingItem>,
        val events: List<CalendarEvent>,
        val notes: List<Note>,
    )
}

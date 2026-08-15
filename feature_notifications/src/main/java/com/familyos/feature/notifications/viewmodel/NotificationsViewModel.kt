package com.familyos.feature.notifications.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.familyos.core.domain.model.AppNotification
import com.familyos.core.domain.model.NotificationType
import com.familyos.core.domain.usecase.auth.GetCurrentUserUseCase
import com.familyos.core.domain.usecase.notifications.DeleteNotificationUseCase
import com.familyos.core.domain.usecase.notifications.MarkAllNotificationsReadUseCase
import com.familyos.core.domain.usecase.notifications.MarkNotificationReadUseCase
import com.familyos.core.domain.usecase.notifications.ObserveNotificationsUseCase
import com.familyos.core.domain.usecase.notifications.ObserveUnreadNotificationCountUseCase
import com.familyos.core.domain.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Notification center UI state. */
data class NotificationsUiState(
    val notifications: List<AppNotification> = emptyList(),
    val unreadCount: Int = 0,
    val filter: NotificationType? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val userId: String? = null,
)

/**
 * In-app notification center ViewModel with mark-read support.
 */
@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val observeNotifications: ObserveNotificationsUseCase,
    private val observeUnreadCount: ObserveUnreadNotificationCountUseCase,
    private val markRead: MarkNotificationReadUseCase,
    private val markAllRead: MarkAllNotificationsReadUseCase,
    private val deleteNotification: DeleteNotificationUseCase,
    private val getCurrentUser: GetCurrentUserUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(NotificationsUiState())
    val state: StateFlow<NotificationsUiState> = _state.asStateFlow()

    private val filter = MutableStateFlow<NotificationType?>(null)

    init {
        viewModelScope.launch {
            val user = getCurrentUser() ?: return@launch
            _state.update { it.copy(userId = user.id) }
            launch {
                observeNotifications(user.id).collect { list ->
                    val f = filter.value
                    _state.update {
                        it.copy(
                            notifications = if (f == null) list else list.filter { n -> n.type == f },
                            isLoading = false,
                        )
                    }
                }
            }
            launch {
                observeUnreadCount(user.id).collect { count ->
                    _state.update { it.copy(unreadCount = count) }
                }
            }
            launch {
                filter.collect { f ->
                    _state.update { it.copy(filter = f) }
                }
            }
        }
    }

    fun setFilter(type: NotificationType?) {
        filter.value = type
    }

    fun markAsRead(id: String) {
        viewModelScope.launch {
            when (val result = markRead(id)) {
                is Result.Success -> Unit
                is Result.Error -> _state.update { it.copy(errorMessage = result.error.message) }
            }
        }
    }

    fun markAllAsRead() {
        val userId = _state.value.userId ?: return
        viewModelScope.launch {
            when (val result = markAllRead(userId)) {
                is Result.Success -> Unit
                is Result.Error -> _state.update { it.copy(errorMessage = result.error.message) }
            }
        }
    }

    fun delete(id: String) {
        viewModelScope.launch { deleteNotification(id) }
    }
}

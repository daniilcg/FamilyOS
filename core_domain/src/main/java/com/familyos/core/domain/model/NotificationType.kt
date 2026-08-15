package com.familyos.core.domain.model

import kotlinx.serialization.Serializable

/** In-app notification category. */
@Serializable
enum class NotificationType {
    NEW_TASK,
    SHOPPING,
    EVENT,
    BUDGET,
    DOCUMENT,
    MEMBER_JOINED,
    TASK_ASSIGNED,
    TASK_DUE,
    SHOPPING_UPDATE,
    EVENT_REMINDER,
    CHAT_MESSAGE,
    FAMILY_INVITE,
    BUDGET_ALERT,
    DOCUMENT_SHARED,
    SYSTEM,
}

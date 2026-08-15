package com.familyos.core.data.remote.dto

import kotlinx.serialization.Serializable

/** Firestore DTO mirroring user documents. */
@Serializable
data class UserDto(
    val id: String = "",
    val email: String = "",
    val displayName: String = "",
    val photoUrl: String? = null,
    val phoneNumber: String? = null,
    val familyId: String? = null,
    val preferredLanguage: String = "en",
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val isEmailVerified: Boolean = false,
)

@Serializable
data class FamilyDto(
    val id: String = "",
    val name: String = "",
    val inviteCode: String = "",
    val ownerId: String = "",
    val photoUrl: String? = null,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val memberCount: Int = 1,
)

@Serializable
data class MemberDto(
    val id: String = "",
    val familyId: String = "",
    val userId: String = "",
    val displayName: String = "",
    val photoUrl: String? = null,
    val email: String = "",
    val role: String = "MEMBER",
    val joinedAt: Long = 0L,
    val updatedAt: Long = 0L,
)

@Serializable
data class ShoppingDto(
    val id: String = "",
    val familyId: String = "",
    val title: String = "",
    val quantity: Double = 1.0,
    val unit: String? = null,
    val category: String = "PRODUCTS",
    val status: String = "ACTIVE",
    val notes: String? = null,
    val estimatedPrice: Double? = null,
    val currency: String = "EUR",
    val photoUri: String? = null,
    val createdBy: String = "",
    val assignedTo: String? = null,
    val purchasedBy: String? = null,
    val purchasedAt: Long? = null,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val isDeleted: Boolean = false,
)

@Serializable
data class TaskDto(
    val id: String = "",
    val familyId: String = "",
    val title: String = "",
    val description: String? = null,
    val status: String = "NEW",
    val priority: String = "MEDIUM",
    val startAt: Long? = null,
    val dueAt: Long? = null,
    val completedAt: Long? = null,
    val assigneeId: String? = null,
    val createdBy: String = "",
    val recurrenceJson: String? = null,
    val parentTaskId: String? = null,
    val photoUri: String? = null,
    val checklistJson: String? = null,
    val attachmentIds: List<String> = emptyList(),
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val isDeleted: Boolean = false,
)

@Serializable
data class EventDto(
    val id: String = "",
    val familyId: String = "",
    val title: String = "",
    val description: String? = null,
    val type: String = "OTHER",
    val startAt: Long = 0L,
    val endAt: Long = 0L,
    val allDay: Boolean = false,
    val location: String? = null,
    val recurrenceJson: String? = null,
    val attendeeIds: List<String> = emptyList(),
    val createdBy: String = "",
    val reminderMinutes: Int? = 30,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val isDeleted: Boolean = false,
)

@Serializable
data class BudgetDto(
    val id: String = "",
    val familyId: String = "",
    val title: String = "",
    val amount: Double = 0.0,
    val currency: String = "EUR",
    val category: String = "OTHER",
    val isIncome: Boolean = false,
    val notes: String? = null,
    val occurredAt: Long = 0L,
    val createdBy: String = "",
    val receiptDocumentId: String? = null,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val isDeleted: Boolean = false,
)

@Serializable
data class DocumentDto(
    val id: String = "",
    val familyId: String = "",
    val title: String = "",
    val type: String = "OTHER",
    val mimeType: String = "application/octet-stream",
    val sizeBytes: Long = 0L,
    val storagePath: String = "",
    val downloadUrl: String? = null,
    val checksumSha256: String? = null,
    val isEncrypted: Boolean = true,
    val tags: List<String> = emptyList(),
    val uploadedBy: String = "",
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val isDeleted: Boolean = false,
)

@Serializable
data class NoteDto(
    val id: String = "",
    val familyId: String = "",
    val title: String = "",
    val body: String = "",
    val checklistJson: String = "[]",
    val colorHex: String? = null,
    val isPinned: Boolean = false,
    val tags: List<String> = emptyList(),
    val createdBy: String = "",
    val updatedBy: String? = null,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val isDeleted: Boolean = false,
)

@Serializable
data class ChatThreadDto(
    val id: String = "",
    val familyId: String = "",
    val title: String = "",
    val participantIds: List<String> = emptyList(),
    val lastMessagePreview: String? = null,
    val lastMessageAt: Long? = null,
    val createdBy: String = "",
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val isDeleted: Boolean = false,
)

@Serializable
data class ChatMessageDto(
    val id: String = "",
    val threadId: String = "",
    val familyId: String = "",
    val senderId: String = "",
    val type: String = "TEXT",
    val body: String = "",
    val attachmentUrl: String? = null,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val isDeleted: Boolean = false,
)

@Serializable
data class NotificationDto(
    val id: String = "",
    val userId: String = "",
    val familyId: String? = null,
    val type: String = "SYSTEM",
    val title: String = "",
    val body: String = "",
    val payloadJson: String? = null,
    val isRead: Boolean = false,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
)

@Serializable
data class AiHistoryDto(
    val id: String = "",
    val familyId: String = "",
    val userId: String = "",
    val title: String = "",
    val provider: String = "",
    val messagesJson: String = "[]",
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val isDeleted: Boolean = false,
)

@Serializable
data class SubscriptionDto(
    val familyId: String = "",
    val plan: String = "FREE",
    val status: String = "NONE",
    val productId: String? = null,
    val purchaseToken: String? = null,
    val expiresAt: Long? = null,
    val autoRenewing: Boolean = false,
    val updatedAt: Long = 0L,
)

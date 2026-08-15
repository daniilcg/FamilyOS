package com.familyos.core.data.mapper

import com.familyos.core.data.local.entity.AiHistoryEntity
import com.familyos.core.data.local.entity.BudgetEntity
import com.familyos.core.data.local.entity.ChatMessageEntity
import com.familyos.core.data.local.entity.ChatThreadEntity
import com.familyos.core.data.local.entity.ChecklistItemEntity
import com.familyos.core.data.local.entity.DocumentEntity
import com.familyos.core.data.local.entity.EventEntity
import com.familyos.core.data.local.entity.FamilyEntity
import com.familyos.core.data.local.entity.MemberEntity
import com.familyos.core.data.local.entity.NoteEntity
import com.familyos.core.data.local.entity.NotificationEntity
import com.familyos.core.data.local.entity.PendingSyncEntity
import com.familyos.core.data.local.entity.ShoppingEntity
import com.familyos.core.data.local.entity.TaskEntity
import com.familyos.core.data.local.entity.UserEntity
import com.familyos.core.domain.model.AiConversation
import com.familyos.core.domain.model.AiMessage
import com.familyos.core.domain.model.AppNotification
import com.familyos.core.domain.model.BudgetCategory
import com.familyos.core.domain.model.BudgetTransaction
import com.familyos.core.domain.model.CalendarEvent
import com.familyos.core.domain.model.ChatMessage
import com.familyos.core.domain.model.ChatThread
import com.familyos.core.domain.model.DocumentType
import com.familyos.core.domain.model.EventType
import com.familyos.core.domain.model.Family
import com.familyos.core.domain.model.FamilyDocument
import com.familyos.core.domain.model.FamilyMember
import com.familyos.core.domain.model.FamilyRole
import com.familyos.core.domain.model.MessageType
import com.familyos.core.domain.model.Note
import com.familyos.core.domain.model.NoteChecklistItem
import com.familyos.core.domain.model.NotificationType
import com.familyos.core.domain.model.PendingSyncAction
import com.familyos.core.domain.model.RecurrenceRule
import com.familyos.core.domain.model.ShoppingCategory
import com.familyos.core.domain.model.ShoppingItem
import com.familyos.core.domain.model.ShoppingStatus
import com.familyos.core.domain.model.SyncActionType
import com.familyos.core.domain.model.SyncCollection
import com.familyos.core.domain.model.TaskItem
import com.familyos.core.domain.model.TaskPriority
import com.familyos.core.domain.model.TaskStatus
import com.familyos.core.domain.model.User
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Bidirectional mappers between Room entities and domain models.
 */
object EntityMappers {

    val json: Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private fun csvToList(csv: String): List<String> =
        if (csv.isBlank()) emptyList() else csv.split(',').map { it.trim() }.filter { it.isNotEmpty() }

    private fun listToCsv(list: List<String>): String = list.joinToString(",")

    fun UserEntity.toDomain(): User = User(
        id, email, displayName, photoUrl, phoneNumber, familyId,
        preferredLanguage, createdAt, updatedAt, isEmailVerified,
    )

    fun User.toEntity(): UserEntity = UserEntity(
        id, email, displayName, photoUrl, phoneNumber, familyId,
        preferredLanguage, createdAt, updatedAt, isEmailVerified,
    )

    fun FamilyEntity.toDomain(): Family = Family(
        id, name, inviteCode, ownerId, photoUrl, createdAt, updatedAt, memberCount,
    )

    fun Family.toEntity(): FamilyEntity = FamilyEntity(
        id, name, inviteCode, ownerId, photoUrl, createdAt, updatedAt, memberCount,
    )

    fun MemberEntity.toDomain(): FamilyMember = FamilyMember(
        id, familyId, userId, displayName, photoUrl, email,
        runCatching { FamilyRole.valueOf(role) }.getOrDefault(FamilyRole.MEMBER),
        joinedAt, updatedAt,
    )

    fun FamilyMember.toEntity(): MemberEntity = MemberEntity(
        id, familyId, userId, displayName, photoUrl, email, role.name, joinedAt, updatedAt,
    )

    fun ShoppingEntity.toDomain(): ShoppingItem = ShoppingItem(
        id = id,
        familyId = familyId,
        title = title,
        quantity = quantity,
        unit = unit,
        category = category.toShoppingCategory(),
        status = runCatching { ShoppingStatus.valueOf(status) }.getOrDefault(ShoppingStatus.ACTIVE),
        notes = notes,
        estimatedPrice = estimatedPrice,
        currency = currency,
        photoUri = photoUri,
        createdBy = createdBy,
        assignedTo = assignedTo,
        purchasedBy = purchasedBy,
        purchasedAt = purchasedAt,
        createdAt = createdAt,
        updatedAt = updatedAt,
        isDeleted = isDeleted,
    )

    fun ShoppingItem.toEntity(): ShoppingEntity = ShoppingEntity(
        id = id,
        familyId = familyId,
        title = title,
        quantity = quantity,
        unit = unit,
        category = category.name,
        status = status.name,
        notes = notes,
        estimatedPrice = estimatedPrice,
        currency = currency,
        photoUri = photoUri,
        createdBy = createdBy,
        assignedTo = assignedTo,
        purchasedBy = purchasedBy,
        purchasedAt = purchasedAt,
        createdAt = createdAt,
        updatedAt = updatedAt,
        isDeleted = isDeleted,
    )

    fun TaskEntity.toDomain(): TaskItem = TaskItem(
        id = id,
        familyId = familyId,
        title = title,
        description = description,
        status = status.toTaskStatus(),
        priority = runCatching { TaskPriority.valueOf(priority) }.getOrDefault(TaskPriority.MEDIUM),
        startAt = startAt,
        dueAt = dueAt,
        completedAt = completedAt,
        assigneeId = assigneeId,
        createdBy = createdBy,
        recurrence = recurrenceJson?.let { runCatching { json.decodeFromString<RecurrenceRule>(it) }.getOrNull() },
        parentTaskId = parentTaskId,
        photoUri = photoUri,
        checklist = checklistJson?.let {
            runCatching { json.decodeFromString<List<com.familyos.core.domain.model.TaskChecklistItem>>(it) }.getOrDefault(emptyList())
        } ?: emptyList(),
        attachmentIds = csvToList(attachmentIdsCsv),
        createdAt = createdAt,
        updatedAt = updatedAt,
        isDeleted = isDeleted,
    )

    fun TaskItem.toEntity(): TaskEntity = TaskEntity(
        id = id,
        familyId = familyId,
        title = title,
        description = description,
        status = status.name,
        priority = priority.name,
        startAt = startAt,
        dueAt = dueAt,
        completedAt = completedAt,
        assigneeId = assigneeId,
        createdBy = createdBy,
        recurrenceJson = recurrence?.let { json.encodeToString(it) },
        parentTaskId = parentTaskId,
        photoUri = photoUri,
        checklistJson = if (checklist.isEmpty()) null else json.encodeToString(checklist),
        attachmentIdsCsv = listToCsv(attachmentIds),
        createdAt = createdAt,
        updatedAt = updatedAt,
        isDeleted = isDeleted,
    )

    fun EventEntity.toDomain(): CalendarEvent = CalendarEvent(
        id = id,
        familyId = familyId,
        title = title,
        description = description,
        type = type.toEventType(),
        startAt = startAt,
        endAt = endAt,
        allDay = allDay,
        location = location,
        recurrence = recurrenceJson?.let { runCatching { json.decodeFromString<RecurrenceRule>(it) }.getOrNull() },
        attendeeIds = csvToList(attendeeIdsCsv),
        createdBy = createdBy,
        reminderMinutes = reminderMinutes,
        createdAt = createdAt,
        updatedAt = updatedAt,
        isDeleted = isDeleted,
    )

    fun CalendarEvent.toEntity(): EventEntity = EventEntity(
        id, familyId, title, description, type.name, startAt, endAt, allDay, location,
        recurrence?.let { json.encodeToString(it) }, listToCsv(attendeeIds), createdBy,
        reminderMinutes, createdAt, updatedAt, isDeleted,
    )

    fun BudgetEntity.toDomain(): BudgetTransaction = BudgetTransaction(
        id, familyId, title, amount, currency,
        category.toBudgetCategory(),
        isIncome, notes, occurredAt, createdBy, receiptDocumentId, createdAt, updatedAt, isDeleted,
    )

    fun BudgetTransaction.toEntity(): BudgetEntity = BudgetEntity(
        id, familyId, title, amount, currency, category.name, isIncome, notes, occurredAt,
        createdBy, receiptDocumentId, createdAt, updatedAt, isDeleted,
    )

    fun DocumentEntity.toDomain(): FamilyDocument = FamilyDocument(
        id, familyId, title,
        runCatching { DocumentType.valueOf(type) }.getOrDefault(DocumentType.OTHER),
        mimeType, sizeBytes, storagePath, downloadUrl, checksumSha256, isEncrypted,
        csvToList(tagsCsv), uploadedBy, createdAt, updatedAt, isDeleted,
    )

    fun FamilyDocument.toEntity(): DocumentEntity = DocumentEntity(
        id, familyId, title, type.name, mimeType, sizeBytes, storagePath, downloadUrl,
        checksumSha256, isEncrypted, listToCsv(tags), uploadedBy, createdAt, updatedAt, isDeleted,
    )

    fun NoteEntity.toDomain(checklist: List<ChecklistItemEntity> = emptyList()): Note = Note(
        id = id,
        familyId = familyId,
        title = title,
        body = body,
        checklist = checklist.map {
            NoteChecklistItem(it.id, it.text, it.isChecked, it.orderIndex)
        },
        photoUrls = csvToList(photoUrlsCsv),
        colorHex = colorHex,
        isPinned = isPinned,
        isArchived = isArchived,
        tags = csvToList(tagsCsv),
        createdBy = createdBy,
        updatedBy = updatedBy,
        createdAt = createdAt,
        updatedAt = updatedAt,
        isDeleted = isDeleted,
    )

    fun Note.toEntity(): NoteEntity = NoteEntity(
        id = id,
        familyId = familyId,
        title = title,
        body = body,
        colorHex = colorHex,
        isPinned = isPinned,
        isArchived = isArchived,
        photoUrlsCsv = listToCsv(photoUrls),
        tagsCsv = listToCsv(tags),
        createdBy = createdBy,
        updatedBy = updatedBy,
        createdAt = createdAt,
        updatedAt = updatedAt,
        isDeleted = isDeleted,
    )

    fun NoteChecklistItem.toEntity(noteId: String): ChecklistItemEntity =
        ChecklistItemEntity(id, noteId, text, isChecked, order)

    fun ChatThreadEntity.toDomain(): ChatThread = ChatThread(
        id, familyId, title, csvToList(participantIdsCsv), lastMessagePreview, lastMessageAt,
        createdBy, createdAt, updatedAt, isDeleted,
    )

    fun ChatThread.toEntity(): ChatThreadEntity = ChatThreadEntity(
        id, familyId, title, listToCsv(participantIds), lastMessagePreview, lastMessageAt,
        createdBy, createdAt, updatedAt, isDeleted,
    )

    fun ChatMessageEntity.toDomain(): ChatMessage = ChatMessage(
        id = id,
        threadId = threadId,
        familyId = familyId,
        senderId = senderId,
        type = runCatching { MessageType.valueOf(type) }.getOrDefault(MessageType.TEXT),
        body = body,
        attachmentUrl = attachmentUrl,
        durationMs = durationMs,
        readBy = csvToList(readByCsv),
        createdAt = createdAt,
        updatedAt = updatedAt,
        isDeleted = isDeleted,
    )

    fun ChatMessage.toEntity(): ChatMessageEntity = ChatMessageEntity(
        id = id,
        threadId = threadId,
        familyId = familyId,
        senderId = senderId,
        type = type.name,
        body = body,
        attachmentUrl = attachmentUrl,
        durationMs = durationMs,
        readByCsv = listToCsv(readBy),
        createdAt = createdAt,
        updatedAt = updatedAt,
        isDeleted = isDeleted,
    )

    fun NotificationEntity.toDomain(): AppNotification = AppNotification(
        id, userId, familyId,
        runCatching { NotificationType.valueOf(type) }.getOrDefault(NotificationType.SYSTEM),
        title, body, payloadJson, isRead, createdAt, updatedAt,
    )

    fun AppNotification.toEntity(): NotificationEntity = NotificationEntity(
        id, userId, familyId, type.name, title, body, payloadJson, isRead, createdAt, updatedAt,
    )

    fun AiHistoryEntity.toDomain(): AiConversation {
        val messages = runCatching {
            json.decodeFromString<List<AiMessage>>(messagesJson)
        }.getOrDefault(emptyList())
        return AiConversation(id, familyId, userId, title, provider, messages, createdAt, updatedAt, isDeleted)
    }

    fun AiConversation.toEntity(): AiHistoryEntity = AiHistoryEntity(
        id, familyId, userId, title, provider, json.encodeToString(messages), createdAt, updatedAt, isDeleted,
    )

    fun PendingSyncEntity.toDomain(): PendingSyncAction = PendingSyncAction(
        id,
        runCatching { SyncCollection.valueOf(collection) }.getOrDefault(SyncCollection.NOTES),
        documentId,
        familyId,
        runCatching { SyncActionType.valueOf(actionType) }.getOrDefault(SyncActionType.UPSERT),
        payloadJson, createdAt, attemptCount, lastError, nextAttemptAt,
    )

    fun PendingSyncAction.toEntity(): PendingSyncEntity = PendingSyncEntity(
        id, collection.name, documentId, familyId, actionType.name, payloadJson,
        createdAt, attemptCount, lastError, nextAttemptAt,
    )

    /** Maps stored / legacy budget category names onto the current product enum. */
    private fun String.toBudgetCategory(): BudgetCategory = when (uppercase()) {
        "TRANSPORT" -> BudgetCategory.CAR
        "INCOME", "HOUSING", "SHOPPING", "SAVINGS" -> BudgetCategory.OTHER
        else -> runCatching { BudgetCategory.valueOf(uppercase()) }.getOrDefault(BudgetCategory.OTHER)
    }

    /** Maps stored / legacy event type names onto the current product enum. */
    private fun String.toEventType(): EventType = when (uppercase()) {
        "GENERAL", "REMINDER" -> EventType.OTHER
        "APPOINTMENT" -> EventType.MEETING
        "TRAVEL" -> EventType.TRIP
        "HEALTH" -> EventType.DOCTOR
        else -> runCatching { EventType.valueOf(uppercase()) }.getOrDefault(EventType.OTHER)
    }

    /** Maps stored / legacy task status names onto the current product enum. */
    fun String.toTaskStatus(): TaskStatus = mapTaskStatus(this)

    /** Maps stored / legacy shopping category names onto the current product enum. */
    fun String.toShoppingCategory(): ShoppingCategory = mapShoppingCategory(this)

    /** Public helpers for legacy alias mapping (also used by unit tests). */
    fun mapTaskStatus(raw: String): TaskStatus = when (raw.uppercase()) {
        "TODO" -> TaskStatus.NEW
        else -> runCatching { TaskStatus.valueOf(raw.uppercase()) }.getOrDefault(TaskStatus.NEW)
    }

    fun mapShoppingCategory(raw: String): ShoppingCategory = when (raw.uppercase()) {
        "GROCERIES" -> ShoppingCategory.PRODUCTS
        "HOUSEHOLD" -> ShoppingCategory.HOME
        "PERSONAL_CARE" -> ShoppingCategory.PHARMACY
        else -> runCatching { ShoppingCategory.valueOf(raw.uppercase()) }.getOrDefault(ShoppingCategory.OTHER)
    }
}

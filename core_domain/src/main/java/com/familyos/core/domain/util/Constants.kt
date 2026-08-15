package com.familyos.core.domain.util

/**
 * Shared domain constants for FamilyOS.
 */
object Constants {
    const val INVITE_CODE_LENGTH = 8
    const val INVITE_CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
    const val MAX_FAMILY_NAME_LENGTH = 64
    const val MAX_DISPLAY_NAME_LENGTH = 80
    const val MAX_SHOPPING_TITLE_LENGTH = 120
    const val MAX_TASK_TITLE_LENGTH = 160
    const val MAX_NOTE_TITLE_LENGTH = 160
    const val MAX_CHAT_MESSAGE_LENGTH = 4000
    const val MAX_AI_MESSAGE_LENGTH = 8000
    const val DEFAULT_PAGE_SIZE = 30
    const val SYNC_BATCH_SIZE = 50
    const val DOCUMENT_MAX_BYTES = 25L * 1024L * 1024L
    const val PREFS_NAME = "familyos_user_prefs"
    const val ENCRYPTED_PREFS_NAME = "familyos_secure_prefs"
    const val DEFAULT_CURRENCY = "EUR"
    const val DEFAULT_LANGUAGE = "en"
    const val COLLECTION_USERS = "users"
    const val COLLECTION_FAMILIES = "families"
    const val COLLECTION_MEMBERS = "members"
    const val COLLECTION_SHOPPING = "shopping"
    const val COLLECTION_TASKS = "tasks"
    const val COLLECTION_EVENTS = "events"
    const val COLLECTION_BUDGETS = "budgets"
    const val COLLECTION_DOCUMENTS = "documents"
    const val COLLECTION_NOTES = "notes"
    const val COLLECTION_CHAT = "chat"
    const val COLLECTION_MESSAGES = "messages"
    const val COLLECTION_NOTIFICATIONS = "notifications"
    const val COLLECTION_AI_HISTORY = "ai_history"
    const val STORAGE_DOCUMENTS = "documents"
    const val STORAGE_AVATARS = "avatars"
    const val STORAGE_ATTACHMENTS = "attachments"
}

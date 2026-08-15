package com.familyos.core.data.remote.firestore

import com.familyos.core.data.remote.dto.AiHistoryDto
import com.familyos.core.data.remote.dto.BudgetDto
import com.familyos.core.data.remote.dto.ChatMessageDto
import com.familyos.core.data.remote.dto.ChatThreadDto
import com.familyos.core.data.remote.dto.DocumentDto
import com.familyos.core.data.remote.dto.EventDto
import com.familyos.core.data.remote.dto.FamilyDto
import com.familyos.core.data.remote.dto.MemberDto
import com.familyos.core.data.remote.dto.NoteDto
import com.familyos.core.data.remote.dto.NotificationDto
import com.familyos.core.data.remote.dto.ShoppingDto
import com.familyos.core.data.remote.dto.SubscriptionDto
import com.familyos.core.data.remote.dto.TaskDto
import com.familyos.core.data.remote.dto.UserDto
import com.familyos.core.domain.util.Constants
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Low-level Firestore access for all FamilyOS collections.
 */
@Singleton
class FirestoreDataSource @Inject constructor(
    private val firestore: FirebaseFirestore,
) {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    // region Users
    suspend fun upsertUser(dto: UserDto) {
        firestore.collection(Constants.COLLECTION_USERS).document(dto.id)
            .set(dto.toMap(), SetOptions.merge()).await()
    }

    suspend fun getUser(userId: String): UserDto? =
        firestore.collection(Constants.COLLECTION_USERS).document(userId).get().await()
            .toObjectOrNull(UserDto.serializer())

    fun observeUser(userId: String): Flow<UserDto?> = observeDocument(
        firestore.collection(Constants.COLLECTION_USERS).document(userId),
    ) { it.toObjectOrNull(UserDto.serializer()) }

    suspend fun deleteUser(userId: String) {
        firestore.collection(Constants.COLLECTION_USERS).document(userId).delete().await()
    }
    // endregion

    // region Families / Members
    suspend fun upsertFamily(dto: FamilyDto) {
        firestore.collection(Constants.COLLECTION_FAMILIES).document(dto.id)
            .set(dto.toMap(), SetOptions.merge()).await()
    }

    suspend fun getFamily(familyId: String): FamilyDto? =
        firestore.collection(Constants.COLLECTION_FAMILIES).document(familyId).get().await()
            .toObjectOrNull(FamilyDto.serializer())

    suspend fun findFamilyByInviteCode(code: String): FamilyDto? {
        val snap = firestore.collection(Constants.COLLECTION_FAMILIES)
            .whereEqualTo("inviteCode", code)
            .limit(1)
            .get()
            .await()
        return snap.documents.firstOrNull()?.toObjectOrNull(FamilyDto.serializer())
    }

    fun observeFamily(familyId: String): Flow<FamilyDto?> = observeDocument(
        firestore.collection(Constants.COLLECTION_FAMILIES).document(familyId),
    ) { it.toObjectOrNull(FamilyDto.serializer()) }

    suspend fun upsertMember(dto: MemberDto) {
        firestore.collection(Constants.COLLECTION_FAMILIES).document(dto.familyId)
            .collection(Constants.COLLECTION_MEMBERS).document(dto.id)
            .set(dto.toMap(), SetOptions.merge()).await()
    }

    suspend fun deleteMember(familyId: String, memberId: String) {
        firestore.collection(Constants.COLLECTION_FAMILIES).document(familyId)
            .collection(Constants.COLLECTION_MEMBERS).document(memberId).delete().await()
    }

    fun observeMembers(familyId: String): Flow<List<MemberDto>> = observeQuery(
        firestore.collection(Constants.COLLECTION_FAMILIES).document(familyId)
            .collection(Constants.COLLECTION_MEMBERS)
            .orderBy("joinedAt", Query.Direction.ASCENDING),
    ) { it.toObjectOrNull(MemberDto.serializer()) }
    // endregion

    // region Shopping
    suspend fun upsertShopping(dto: ShoppingDto) {
        familyDoc(dto.familyId, Constants.COLLECTION_SHOPPING, dto.id).set(dto.toMap(), SetOptions.merge()).await()
    }

    fun observeShopping(familyId: String): Flow<List<ShoppingDto>> = observeQuery(
        firestore.collection(Constants.COLLECTION_FAMILIES).document(familyId)
            .collection(Constants.COLLECTION_SHOPPING)
            .whereEqualTo("isDeleted", false),
    ) { it.toObjectOrNull(ShoppingDto.serializer()) }

    suspend fun deleteShopping(familyId: String, id: String) {
        familyDoc(familyId, Constants.COLLECTION_SHOPPING, id)
            .set(mapOf("isDeleted" to true, "updatedAt" to System.currentTimeMillis()), SetOptions.merge())
            .await()
    }
    // endregion

    // region Tasks
    suspend fun upsertTask(dto: TaskDto) {
        familyDoc(dto.familyId, Constants.COLLECTION_TASKS, dto.id).set(dto.toMap(), SetOptions.merge()).await()
    }

    fun observeTasks(familyId: String): Flow<List<TaskDto>> = observeQuery(
        firestore.collection(Constants.COLLECTION_FAMILIES).document(familyId)
            .collection(Constants.COLLECTION_TASKS)
            .whereEqualTo("isDeleted", false),
    ) { it.toObjectOrNull(TaskDto.serializer()) }
    // endregion

    // region Events
    suspend fun upsertEvent(dto: EventDto) {
        familyDoc(dto.familyId, Constants.COLLECTION_EVENTS, dto.id).set(dto.toMap(), SetOptions.merge()).await()
    }

    fun observeEvents(familyId: String): Flow<List<EventDto>> = observeQuery(
        firestore.collection(Constants.COLLECTION_FAMILIES).document(familyId)
            .collection(Constants.COLLECTION_EVENTS)
            .whereEqualTo("isDeleted", false),
    ) { it.toObjectOrNull(EventDto.serializer()) }
    // endregion

    // region Budgets
    suspend fun upsertBudget(dto: BudgetDto) {
        familyDoc(dto.familyId, Constants.COLLECTION_BUDGETS, dto.id).set(dto.toMap(), SetOptions.merge()).await()
    }

    fun observeBudgets(familyId: String): Flow<List<BudgetDto>> = observeQuery(
        firestore.collection(Constants.COLLECTION_FAMILIES).document(familyId)
            .collection(Constants.COLLECTION_BUDGETS)
            .whereEqualTo("isDeleted", false),
    ) { it.toObjectOrNull(BudgetDto.serializer()) }
    // endregion

    // region Documents
    suspend fun upsertDocument(dto: DocumentDto) {
        familyDoc(dto.familyId, Constants.COLLECTION_DOCUMENTS, dto.id).set(dto.toMap(), SetOptions.merge()).await()
    }

    fun observeDocuments(familyId: String): Flow<List<DocumentDto>> = observeQuery(
        firestore.collection(Constants.COLLECTION_FAMILIES).document(familyId)
            .collection(Constants.COLLECTION_DOCUMENTS)
            .whereEqualTo("isDeleted", false),
    ) { it.toObjectOrNull(DocumentDto.serializer()) }
    // endregion

    // region Notes
    suspend fun upsertNote(dto: NoteDto) {
        familyDoc(dto.familyId, Constants.COLLECTION_NOTES, dto.id).set(dto.toMap(), SetOptions.merge()).await()
    }

    fun observeNotes(familyId: String): Flow<List<NoteDto>> = observeQuery(
        firestore.collection(Constants.COLLECTION_FAMILIES).document(familyId)
            .collection(Constants.COLLECTION_NOTES)
            .whereEqualTo("isDeleted", false),
    ) { it.toObjectOrNull(NoteDto.serializer()) }
    // endregion

    // region Chat
    suspend fun upsertChatThread(dto: ChatThreadDto) {
        familyDoc(dto.familyId, Constants.COLLECTION_CHAT, dto.id).set(dto.toMap(), SetOptions.merge()).await()
    }

    fun observeChatThreads(familyId: String): Flow<List<ChatThreadDto>> = observeQuery(
        firestore.collection(Constants.COLLECTION_FAMILIES).document(familyId)
            .collection(Constants.COLLECTION_CHAT)
            .whereEqualTo("isDeleted", false),
    ) { it.toObjectOrNull(ChatThreadDto.serializer()) }

    suspend fun upsertChatMessage(dto: ChatMessageDto) {
        firestore.collection(Constants.COLLECTION_FAMILIES).document(dto.familyId)
            .collection(Constants.COLLECTION_CHAT).document(dto.threadId)
            .collection(Constants.COLLECTION_MESSAGES).document(dto.id)
            .set(dto.toMap(), SetOptions.merge()).await()
    }

    fun observeChatMessages(familyId: String, threadId: String): Flow<List<ChatMessageDto>> = observeQuery(
        firestore.collection(Constants.COLLECTION_FAMILIES).document(familyId)
            .collection(Constants.COLLECTION_CHAT).document(threadId)
            .collection(Constants.COLLECTION_MESSAGES)
            .whereEqualTo("isDeleted", false)
            .orderBy("createdAt", Query.Direction.ASCENDING),
    ) { it.toObjectOrNull(ChatMessageDto.serializer()) }
    // endregion

    // region Notifications
    suspend fun upsertNotification(dto: NotificationDto) {
        firestore.collection(Constants.COLLECTION_USERS).document(dto.userId)
            .collection(Constants.COLLECTION_NOTIFICATIONS).document(dto.id)
            .set(dto.toMap(), SetOptions.merge()).await()
    }

    fun observeNotifications(userId: String): Flow<List<NotificationDto>> = observeQuery(
        firestore.collection(Constants.COLLECTION_USERS).document(userId)
            .collection(Constants.COLLECTION_NOTIFICATIONS)
            .orderBy("createdAt", Query.Direction.DESCENDING),
    ) { it.toObjectOrNull(NotificationDto.serializer()) }
    // endregion

    // region AI
    suspend fun upsertAiHistory(dto: AiHistoryDto) {
        familyDoc(dto.familyId, Constants.COLLECTION_AI_HISTORY, dto.id).set(dto.toMap(), SetOptions.merge()).await()
    }

    fun observeAiHistory(familyId: String, userId: String): Flow<List<AiHistoryDto>> = observeQuery(
        firestore.collection(Constants.COLLECTION_FAMILIES).document(familyId)
            .collection(Constants.COLLECTION_AI_HISTORY)
            .whereEqualTo("userId", userId)
            .whereEqualTo("isDeleted", false),
    ) { it.toObjectOrNull(AiHistoryDto.serializer()) }
    // endregion

    // region Billing
    suspend fun upsertSubscription(dto: SubscriptionDto) {
        firestore.collection(Constants.COLLECTION_FAMILIES).document(dto.familyId)
            .collection("subscription").document("current")
            .set(dto.toMap(), SetOptions.merge()).await()
    }

    fun observeSubscription(familyId: String): Flow<SubscriptionDto?> = observeDocument(
        firestore.collection(Constants.COLLECTION_FAMILIES).document(familyId)
            .collection("subscription").document("current"),
    ) { it.toObjectOrNull(SubscriptionDto.serializer()) }
    // endregion

    /**
     * Writes an arbitrary JSON payload map to a collection/document path used by the sync queue.
     */
    suspend fun upsertRaw(collection: String, familyId: String?, documentId: String, payloadJson: String) {
        val map = json.decodeFromString<Map<String, kotlinx.serialization.json.JsonElement>>(payloadJson)
            .mapValues { (_, v) -> jsonElementToAny(v) }
            .toMutableMap()
        when (collection) {
            Constants.COLLECTION_USERS ->
                firestore.collection(Constants.COLLECTION_USERS).document(documentId)
                    .set(map, SetOptions.merge()).await()
            Constants.COLLECTION_FAMILIES ->
                firestore.collection(Constants.COLLECTION_FAMILIES).document(documentId)
                    .set(map, SetOptions.merge()).await()
            else -> {
                require(!familyId.isNullOrBlank()) { "familyId required for collection $collection" }
                familyDoc(familyId, collection, documentId).set(map, SetOptions.merge()).await()
            }
        }
    }

    suspend fun deleteRaw(collection: String, familyId: String?, documentId: String) {
        when (collection) {
            Constants.COLLECTION_USERS ->
                firestore.collection(Constants.COLLECTION_USERS).document(documentId).delete().await()
            Constants.COLLECTION_FAMILIES ->
                firestore.collection(Constants.COLLECTION_FAMILIES).document(documentId).delete().await()
            else -> {
                require(!familyId.isNullOrBlank()) { "familyId required for collection $collection" }
                familyDoc(familyId, collection, documentId)
                    .set(mapOf("isDeleted" to true, "updatedAt" to System.currentTimeMillis()), SetOptions.merge())
                    .await()
            }
        }
    }

    private fun familyDoc(familyId: String, collection: String, documentId: String) =
        firestore.collection(Constants.COLLECTION_FAMILIES).document(familyId)
            .collection(collection).document(documentId)

    private fun <T> observeDocument(
        ref: com.google.firebase.firestore.DocumentReference,
        mapper: (DocumentSnapshot) -> T?,
    ): Flow<T?> = callbackFlow {
        val registration: ListenerRegistration = ref.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            trySend(snapshot?.let(mapper))
        }
        awaitClose { registration.remove() }
    }

    private fun <T> observeQuery(
        query: Query,
        mapper: (DocumentSnapshot) -> T?,
    ): Flow<List<T>> = callbackFlow {
        val registration: ListenerRegistration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            val items = snapshot?.documents?.mapNotNull(mapper).orEmpty()
            trySend(items)
        }
        awaitClose { registration.remove() }
    }

    private fun <T> DocumentSnapshot.toObjectOrNull(
        serializer: kotlinx.serialization.KSerializer<T>,
    ): T? {
        if (!exists()) return null
        val data = data ?: return null
        return runCatching {
            val encoded = json.encodeToString(data)
            json.decodeFromString(serializer, encoded)
        }.getOrNull()
    }

    private inline fun <reified T> T.toMap(): Map<String, Any?> {
        val encoded = json.encodeToString(this)
        val element = json.parseToJsonElement(encoded)
        @Suppress("UNCHECKED_CAST")
        return jsonElementToAny(element) as Map<String, Any?>
    }

    private fun jsonElementToAny(element: kotlinx.serialization.json.JsonElement): Any? =
        when (element) {
            is kotlinx.serialization.json.JsonNull -> null
            is kotlinx.serialization.json.JsonPrimitive -> {
                val p = element
                when {
                    p.isString -> p.content
                    p.content == "true" || p.content == "false" -> p.content.toBoolean()
                    p.content.contains('.') -> p.content.toDoubleOrNull() ?: p.content
                    else -> p.content.toLongOrNull() ?: p.content.toIntOrNull() ?: p.content
                }
            }
            is kotlinx.serialization.json.JsonArray -> element.map { jsonElementToAny(it) }
            is kotlinx.serialization.json.JsonObject -> element.mapValues { jsonElementToAny(it.value) }
        }
}

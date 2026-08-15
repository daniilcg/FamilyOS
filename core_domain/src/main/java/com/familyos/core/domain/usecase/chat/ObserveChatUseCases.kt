package com.familyos.core.domain.usecase.chat

import com.familyos.core.domain.model.ChatMessage
import com.familyos.core.domain.model.ChatThread
import com.familyos.core.domain.repository.ChatRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** Observes chat threads for a family. */
class ObserveChatThreadsUseCase @Inject constructor(private val chatRepository: ChatRepository) {
    operator fun invoke(familyId: String): Flow<List<ChatThread>> = chatRepository.observeThreads(familyId)
}

/** Observes messages in a thread. */
class ObserveChatMessagesUseCase @Inject constructor(private val chatRepository: ChatRepository) {
    operator fun invoke(threadId: String): Flow<List<ChatMessage>> = chatRepository.observeMessages(threadId)
}

package com.familyos.core.domain.usecase.chat

import com.familyos.core.domain.model.MemberPresence
import com.familyos.core.domain.repository.ChatRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** Observes online presence for family members. */
class ObservePresenceUseCase @Inject constructor(
    private val chatRepository: ChatRepository,
) {
    operator fun invoke(familyId: String): Flow<List<MemberPresence>> =
        chatRepository.observePresence(familyId)
}

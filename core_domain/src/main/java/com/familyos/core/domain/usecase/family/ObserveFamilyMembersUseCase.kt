package com.familyos.core.domain.usecase.family

import com.familyos.core.domain.model.FamilyMember
import com.familyos.core.domain.repository.FamilyRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** Observes all members of a family. */
class ObserveFamilyMembersUseCase @Inject constructor(
    private val familyRepository: FamilyRepository,
) {
    operator fun invoke(familyId: String): Flow<List<FamilyMember>> =
        familyRepository.observeMembers(familyId)
}

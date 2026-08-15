package com.familyos.core.domain.usecase.family

import com.familyos.core.domain.model.Family
import com.familyos.core.domain.repository.FamilyRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** Observes a family document by id. */
class ObserveFamilyUseCase @Inject constructor(
    private val familyRepository: FamilyRepository,
) {
    operator fun invoke(familyId: String): Flow<Family?> = familyRepository.observeFamily(familyId)
}

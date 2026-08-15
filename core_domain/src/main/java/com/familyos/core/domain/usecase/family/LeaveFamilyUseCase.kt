package com.familyos.core.domain.usecase.family

import com.familyos.core.domain.repository.FamilyRepository
import com.familyos.core.domain.util.AppError
import com.familyos.core.domain.util.Result
import javax.inject.Inject

/** Leaves a family as the given user. */
class LeaveFamilyUseCase @Inject constructor(
    private val familyRepository: FamilyRepository,
) {
    suspend operator fun invoke(familyId: String, userId: String): Result<Unit> {
        if (familyId.isBlank() || userId.isBlank()) {
            return Result.failure(AppError.Validation("familyId and userId are required"))
        }
        return familyRepository.leaveFamily(familyId, userId)
    }
}

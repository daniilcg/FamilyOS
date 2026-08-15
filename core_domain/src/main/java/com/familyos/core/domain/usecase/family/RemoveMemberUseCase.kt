package com.familyos.core.domain.usecase.family

import com.familyos.core.domain.repository.FamilyRepository
import com.familyos.core.domain.util.AppError
import com.familyos.core.domain.util.Result
import javax.inject.Inject

/** Removes a member from a family (admin/owner only at data layer). */
class RemoveMemberUseCase @Inject constructor(
    private val familyRepository: FamilyRepository,
) {
    suspend operator fun invoke(familyId: String, memberId: String): Result<Unit> {
        if (familyId.isBlank() || memberId.isBlank()) {
            return Result.failure(AppError.Validation("familyId and memberId are required"))
        }
        return familyRepository.removeMember(familyId, memberId)
    }
}

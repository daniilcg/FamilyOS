package com.familyos.core.domain.usecase.family

import com.familyos.core.domain.model.FamilyMember
import com.familyos.core.domain.model.FamilyRole
import com.familyos.core.domain.repository.FamilyRepository
import com.familyos.core.domain.util.AppError
import com.familyos.core.domain.util.Result
import javax.inject.Inject

/** Updates a member's role inside a family. */
class UpdateMemberRoleUseCase @Inject constructor(
    private val familyRepository: FamilyRepository,
) {
    suspend operator fun invoke(familyId: String, memberId: String, role: FamilyRole): Result<FamilyMember> {
        if (familyId.isBlank() || memberId.isBlank()) {
            return Result.failure(AppError.Validation("familyId and memberId are required"))
        }
        if (role == FamilyRole.OWNER) {
            return Result.failure(AppError.Validation("Use ownership transfer to assign OWNER"))
        }
        return familyRepository.updateMemberRole(familyId, memberId, role)
    }
}

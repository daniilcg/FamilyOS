package com.familyos.core.domain.usecase.family

import com.familyos.core.domain.logic.InviteCodeGenerator
import com.familyos.core.domain.model.Family
import com.familyos.core.domain.repository.FamilyRepository
import com.familyos.core.domain.util.AppError
import com.familyos.core.domain.util.Result
import javax.inject.Inject

/** Joins an existing family using an invite code. */
class JoinFamilyByCodeUseCase @Inject constructor(
    private val familyRepository: FamilyRepository,
) {
    suspend operator fun invoke(
        inviteCode: String,
        userId: String,
        displayName: String,
        photoUrl: String? = null,
        email: String = "",
    ): Result<Family> {
        if (!InviteCodeGenerator.isValid(inviteCode)) {
            return Result.failure(AppError.Validation("Invalid invite code", "inviteCode"))
        }
        if (userId.isBlank()) return Result.failure(AppError.Unauthorized())
        return familyRepository.joinFamilyByCode(
            InviteCodeGenerator.normalize(inviteCode),
            userId,
            displayName.trim(),
            photoUrl,
            email.trim(),
        )
    }
}

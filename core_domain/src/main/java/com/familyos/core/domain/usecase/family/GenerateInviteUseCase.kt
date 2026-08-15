package com.familyos.core.domain.usecase.family

import com.familyos.core.domain.repository.FamilyRepository
import com.familyos.core.domain.util.AppError
import com.familyos.core.domain.util.Result
import javax.inject.Inject

/** Rotates and returns a fresh invite code for [familyId]. */
class GenerateInviteUseCase @Inject constructor(
    private val familyRepository: FamilyRepository,
) {
    suspend operator fun invoke(familyId: String): Result<String> {
        if (familyId.isBlank()) return Result.failure(AppError.Validation("familyId required", "familyId"))
        return familyRepository.generateInviteCode(familyId)
    }
}

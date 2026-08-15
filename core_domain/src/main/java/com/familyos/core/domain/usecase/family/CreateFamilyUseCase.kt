package com.familyos.core.domain.usecase.family

import com.familyos.core.domain.model.Family
import com.familyos.core.domain.repository.FamilyRepository
import com.familyos.core.domain.util.AppError
import com.familyos.core.domain.util.Constants
import com.familyos.core.domain.util.Result
import javax.inject.Inject

/** Creates a new family workspace owned by [ownerId]. */
class CreateFamilyUseCase @Inject constructor(
    private val familyRepository: FamilyRepository,
) {
    suspend operator fun invoke(name: String, ownerId: String): Result<Family> {
        val trimmed = name.trim()
        if (trimmed.isEmpty() || trimmed.length > Constants.MAX_FAMILY_NAME_LENGTH) {
            return Result.failure(AppError.Validation("Family name is required", "name"))
        }
        if (ownerId.isBlank()) {
            return Result.failure(AppError.Unauthorized())
        }
        return familyRepository.createFamily(trimmed, ownerId)
    }
}

package com.familyos.core.domain.repository

import com.familyos.core.domain.model.Family
import com.familyos.core.domain.model.FamilyMember
import com.familyos.core.domain.model.FamilyRole
import com.familyos.core.domain.util.Result
import kotlinx.coroutines.flow.Flow

/**
 * Family workspace and membership operations.
 */
interface FamilyRepository {
    fun observeFamily(familyId: String): Flow<Family?>
    fun observeMembers(familyId: String): Flow<List<FamilyMember>>
    suspend fun getFamily(familyId: String): Result<Family>
    suspend fun createFamily(name: String, ownerId: String): Result<Family>
    suspend fun joinFamilyByCode(inviteCode: String, userId: String, displayName: String, photoUrl: String?, email: String): Result<Family>
    suspend fun generateInviteCode(familyId: String): Result<String>
    suspend fun updateMemberRole(familyId: String, memberId: String, role: FamilyRole): Result<FamilyMember>
    suspend fun removeMember(familyId: String, memberId: String): Result<Unit>
    suspend fun leaveFamily(familyId: String, userId: String): Result<Unit>
    suspend fun updateFamily(family: Family): Result<Family>
}

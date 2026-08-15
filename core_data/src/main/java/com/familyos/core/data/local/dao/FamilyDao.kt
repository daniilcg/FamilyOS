package com.familyos.core.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.familyos.core.data.local.entity.FamilyEntity
import com.familyos.core.data.local.entity.MemberEntity
import kotlinx.coroutines.flow.Flow

/** Data access for families and members. */
@Dao
interface FamilyDao {
    @Query("SELECT * FROM families WHERE id = :id LIMIT 1")
    fun observeFamily(id: String): Flow<FamilyEntity?>

    @Query("SELECT * FROM families WHERE id = :id LIMIT 1")
    suspend fun getFamily(id: String): FamilyEntity?

    @Query("SELECT * FROM families WHERE inviteCode = :code LIMIT 1")
    suspend fun getByInviteCode(code: String): FamilyEntity?

    @Upsert
    suspend fun upsertFamily(entity: FamilyEntity)

    @Query("UPDATE families SET inviteCode = :code, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateInviteCode(id: String, code: String, updatedAt: Long)

    @Query("SELECT * FROM members WHERE familyId = :familyId ORDER BY joinedAt ASC")
    fun observeMembers(familyId: String): Flow<List<MemberEntity>>

    @Query("SELECT * FROM members WHERE id = :id LIMIT 1")
    suspend fun getMember(id: String): MemberEntity?

    @Query("SELECT * FROM members WHERE familyId = :familyId AND userId = :userId LIMIT 1")
    suspend fun getMemberByUser(familyId: String, userId: String): MemberEntity?

    @Upsert
    suspend fun upsertMember(entity: MemberEntity)

    @Query("DELETE FROM members WHERE id = :id")
    suspend fun deleteMember(id: String)

    @Query("UPDATE members SET role = :role, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateRole(id: String, role: String, updatedAt: Long)

    @Query("UPDATE families SET memberCount = :count, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateMemberCount(id: String, count: Int, updatedAt: Long)

    @Query("SELECT COUNT(*) FROM members WHERE familyId = :familyId")
    suspend fun countMembers(familyId: String): Int
}

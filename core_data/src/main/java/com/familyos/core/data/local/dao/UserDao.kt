package com.familyos.core.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.familyos.core.data.local.entity.UserEntity
import kotlinx.coroutines.flow.Flow

/** Data access for users table. */
@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    fun observeById(id: String): Flow<UserEntity?>

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): UserEntity?

    @Upsert
    suspend fun upsert(entity: UserEntity)

    @Query("DELETE FROM users WHERE id = :id")
    suspend fun delete(id: String)

    @Query("UPDATE users SET familyId = :familyId, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateFamilyId(id: String, familyId: String?, updatedAt: Long)
}

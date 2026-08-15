package com.familyos.core.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.familyos.core.data.local.entity.AuthCredentialEntity

/** Data access for [AuthCredentialEntity]. */
@Dao
interface AuthCredentialDao {
    @Query("SELECT * FROM auth_credentials WHERE email = :email LIMIT 1")
    suspend fun getByEmail(email: String): AuthCredentialEntity?

    @Query("SELECT * FROM auth_credentials WHERE userId = :userId LIMIT 1")
    suspend fun getByUserId(userId: String): AuthCredentialEntity?

    @Upsert
    suspend fun upsert(entity: AuthCredentialEntity)

    @Query("DELETE FROM auth_credentials WHERE userId = :userId")
    suspend fun delete(userId: String)
}

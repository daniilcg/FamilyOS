package com.familyos.core.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Room entity for [com.familyos.core.domain.model.Family]. */
@Entity(tableName = "families", indices = [Index(value = ["inviteCode"], unique = true)])
data class FamilyEntity(
    @PrimaryKey val id: String,
    val name: String,
    val inviteCode: String,
    val ownerId: String,
    val photoUrl: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val memberCount: Int,
)

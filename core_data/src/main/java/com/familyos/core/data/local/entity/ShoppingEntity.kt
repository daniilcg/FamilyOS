package com.familyos.core.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Room entity for shopping items. */
@Entity(
    tableName = "shopping",
    indices = [
        Index(value = ["familyId", "status"]),
        Index(value = ["familyId", "category"]),
        Index(value = ["updatedAt"]),
    ],
)
data class ShoppingEntity(
    @PrimaryKey val id: String,
    val familyId: String,
    val title: String,
    val quantity: Double,
    val unit: String?,
    val category: String,
    val status: String,
    val notes: String?,
    val estimatedPrice: Double?,
    val currency: String,
    val photoUri: String?,
    val createdBy: String,
    val assignedTo: String?,
    val purchasedBy: String?,
    val purchasedAt: Long?,
    val createdAt: Long,
    val updatedAt: Long,
    val isDeleted: Boolean,
)

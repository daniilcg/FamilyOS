package com.familyos.core.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Room entity for budget transactions. */
@Entity(
    tableName = "budgets",
    indices = [
        Index(value = ["familyId", "occurredAt"]),
        Index(value = ["familyId", "category"]),
    ],
)
data class BudgetEntity(
    @PrimaryKey val id: String,
    val familyId: String,
    val title: String,
    val amount: Double,
    val currency: String,
    val category: String,
    val isIncome: Boolean,
    val notes: String?,
    val occurredAt: Long,
    val createdBy: String,
    val receiptDocumentId: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val isDeleted: Boolean,
)

package com.familyos.core.domain.model

import kotlinx.serialization.Serializable

/**
 * A single income or expense transaction.
 */
@Serializable
data class BudgetTransaction(
    val id: String,
    val familyId: String,
    val title: String,
    val amount: Double,
    val currency: String = "EUR",
    val category: BudgetCategory = BudgetCategory.OTHER,
    val isIncome: Boolean = false,
    val notes: String? = null,
    val occurredAt: Long,
    val createdBy: String,
    val receiptDocumentId: String? = null,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val isDeleted: Boolean = false,
)

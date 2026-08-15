package com.familyos.core.domain.model

import kotlinx.serialization.Serializable

/**
 * Aggregated budget figures for a period.
 */
@Serializable
data class BudgetSummary(
    val familyId: String,
    val periodStart: Long,
    val periodEnd: Long,
    val totalIncome: Double,
    val totalExpense: Double,
    val balance: Double,
    val byCategory: Map<BudgetCategory, Double> = emptyMap(),
    val currency: String = "EUR",
)

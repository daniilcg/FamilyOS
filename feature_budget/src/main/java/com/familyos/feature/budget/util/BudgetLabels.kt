package com.familyos.feature.budget.util

import com.familyos.core.domain.model.BudgetCategory
import com.familyos.core.locale.LocalizedLabels
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Expense categories shown in the budget UI. */
val BudgetExpenseCategories: List<BudgetCategory> = listOf(
    BudgetCategory.FOOD,
    BudgetCategory.UTILITIES,
    BudgetCategory.CAR,
    BudgetCategory.EDUCATION,
    BudgetCategory.HEALTH,
    BudgetCategory.ENTERTAINMENT,
    BudgetCategory.TRAVEL,
    BudgetCategory.OTHER,
)

fun BudgetCategory.label(): String = LocalizedLabels.budgetCategory(name)

fun formatMoney(amount: Double, currency: String): String {
    val format = NumberFormat.getCurrencyInstance(Locale.getDefault())
    runCatching { format.currency = java.util.Currency.getInstance(currency) }
    return format.format(amount)
}

fun formatMonth(epochDayStart: Long): String {
    val monthFormatter = DateTimeFormatter.ofPattern("MMMM yyyy").withLocale(Locale.getDefault())
    return Instant.ofEpochMilli(epochDayStart).atZone(ZoneId.systemDefault()).format(monthFormatter)
}

fun formatDay(epoch: Long): String {
    val dayFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy").withLocale(Locale.getDefault())
    return Instant.ofEpochMilli(epoch).atZone(ZoneId.systemDefault()).format(dayFormatter)
}

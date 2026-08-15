package com.familyos.feature.shopping.util

import com.familyos.core.domain.model.ShoppingCategory
import com.familyos.core.domain.model.ShoppingStatus
import com.familyos.core.locale.LocalizedLabels
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Product categories shown in shopping UI. */
val ShoppingUiCategories: List<ShoppingCategory> = listOf(
    ShoppingCategory.PRODUCTS,
    ShoppingCategory.HOME,
    ShoppingCategory.PHARMACY,
    ShoppingCategory.AUTO,
    ShoppingCategory.PETS,
    ShoppingCategory.KIDS,
    ShoppingCategory.CLOTHING,
    ShoppingCategory.ELECTRONICS,
    ShoppingCategory.OTHER,
)

/** Human-readable shopping category label. */
fun ShoppingCategory.label(): String = LocalizedLabels.shoppingCategory(name)

/** Human-readable shopping status label. */
fun ShoppingStatus.label(): String = LocalizedLabels.shoppingStatus(name)

/** Formats a price with currency for display. */
fun formatPrice(amount: Double?, currency: String): String {
    if (amount == null) return "—"
    val format = NumberFormat.getCurrencyInstance(Locale.getDefault())
    runCatching { format.currency = java.util.Currency.getInstance(currency) }
    return format.format(amount)
}

/** Formats epoch millis for list captions. */
fun formatEpoch(millis: Long?): String {
    if (millis == null || millis <= 0L) return "—"
    val formatter = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm").withLocale(Locale.getDefault())
    return Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).format(formatter)
}

/** Sort modes for the active shopping list. */
enum class ShoppingSort {
    NAME_ASC,
    NAME_DESC,
    NEWEST,
    OLDEST,
    PRICE_ASC,
    PRICE_DESC,
    CATEGORY,
}

/** Display label for [ShoppingSort]. */
fun ShoppingSort.label(): String = LocalizedLabels.shoppingSort(name)

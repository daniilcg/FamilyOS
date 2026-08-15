package com.familyos.feature.shopping.util

import com.familyos.core.domain.model.ShoppingCategory
import com.familyos.core.domain.model.ShoppingStatus
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
fun ShoppingCategory.label(): String = when (this) {
    ShoppingCategory.PRODUCTS -> "Products"
    ShoppingCategory.HOME -> "Home"
    ShoppingCategory.PHARMACY -> "Pharmacy"
    ShoppingCategory.AUTO -> "Auto"
    ShoppingCategory.PETS -> "Pets"
    ShoppingCategory.KIDS -> "Kids"
    ShoppingCategory.CLOTHING -> "Clothing"
    ShoppingCategory.ELECTRONICS -> "Electronics"
    ShoppingCategory.OTHER -> "Other"
}

/** Human-readable shopping status label. */
fun ShoppingStatus.label(): String = when (this) {
    ShoppingStatus.ACTIVE -> "Active"
    ShoppingStatus.PURCHASED -> "Purchased"
    ShoppingStatus.ARCHIVED -> "Archived"
}

/** Formats a price with currency for display. */
fun formatPrice(amount: Double?, currency: String): String {
    if (amount == null) return "—"
    val format = NumberFormat.getCurrencyInstance(Locale.getDefault())
    runCatching { format.currency = java.util.Currency.getInstance(currency) }
    return format.format(amount)
}

private val dateTimeFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm").withLocale(Locale.getDefault())

/** Formats epoch millis for list captions. */
fun formatEpoch(millis: Long?): String {
    if (millis == null || millis <= 0L) return "—"
    return Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).format(dateTimeFormatter)
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
fun ShoppingSort.label(): String = when (this) {
    ShoppingSort.NAME_ASC -> "Name A–Z"
    ShoppingSort.NAME_DESC -> "Name Z–A"
    ShoppingSort.NEWEST -> "Newest"
    ShoppingSort.OLDEST -> "Oldest"
    ShoppingSort.PRICE_ASC -> "Price ↑"
    ShoppingSort.PRICE_DESC -> "Price ↓"
    ShoppingSort.CATEGORY -> "Category"
}

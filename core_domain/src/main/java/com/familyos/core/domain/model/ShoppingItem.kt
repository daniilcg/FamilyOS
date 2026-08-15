package com.familyos.core.domain.model

import kotlinx.serialization.Serializable

/**
 * Shared shopping list entry.
 */
@Serializable
data class ShoppingItem(
    val id: String,
    val familyId: String,
    val title: String,
    val quantity: Double = 1.0,
    val unit: String? = null,
    val category: ShoppingCategory = ShoppingCategory.PRODUCTS,
    val status: ShoppingStatus = ShoppingStatus.ACTIVE,
    val notes: String? = null,
    val estimatedPrice: Double? = null,
    val currency: String = "EUR",
    val photoUri: String? = null,
    val createdBy: String,
    val assignedTo: String? = null,
    val purchasedBy: String? = null,
    val purchasedAt: Long? = null,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val isDeleted: Boolean = false,
)

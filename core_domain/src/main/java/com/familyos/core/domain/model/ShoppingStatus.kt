package com.familyos.core.domain.model

import kotlinx.serialization.Serializable

/** Lifecycle status of a shopping item. */
@Serializable
enum class ShoppingStatus {
    ACTIVE,
    PURCHASED,
    ARCHIVED,
}

package com.familyos.core.domain.model

import kotlinx.serialization.Serializable

/** Shopping list item category matching product taxonomy. */
@Serializable
enum class ShoppingCategory {
    PRODUCTS,
    HOME,
    PHARMACY,
    AUTO,
    PETS,
    KIDS,
    CLOTHING,
    ELECTRONICS,
    OTHER,
}

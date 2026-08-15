package com.familyos.core.domain.model

import kotlinx.serialization.Serializable

/** Family budget expense/income category matching product taxonomy. */
@Serializable
enum class BudgetCategory {
    FOOD,
    UTILITIES,
    CAR,
    EDUCATION,
    HEALTH,
    ENTERTAINMENT,
    TRAVEL,
    OTHER,
}

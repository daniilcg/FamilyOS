package com.familyos.core.domain.model

import kotlinx.serialization.Serializable

/** Calendar event classification matching product taxonomy. */
@Serializable
enum class EventType {
    BIRTHDAY,
    HOLIDAY,
    MEETING,
    TRIP,
    SCHOOL,
    VET,
    DOCTOR,
    BILL_PAYMENT,
    OTHER,
}

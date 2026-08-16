package com.familyos.core.domain.repository

import com.familyos.core.domain.model.SubscriptionInfo
import kotlinx.coroutines.flow.Flow

/**
 * Optional cloud copy of a family's subscription so members on other devices
 * inherit Premium (developer grant, PayPal, or Play).
 */
interface SubscriptionRemoteStore {
    fun observe(familyId: String): Flow<SubscriptionInfo?>
    suspend fun upsert(info: SubscriptionInfo)
}

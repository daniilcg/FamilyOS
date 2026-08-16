package com.familyos.core.data.repository

import com.familyos.core.data.auth.FirebaseAvailability
import com.familyos.core.data.remote.dto.SubscriptionDto
import com.familyos.core.data.remote.firestore.FirestoreDataSource
import com.familyos.core.domain.model.SubscriptionInfo
import com.familyos.core.domain.model.SubscriptionPlan
import com.familyos.core.domain.model.SubscriptionStatus
import com.familyos.core.domain.repository.SubscriptionRemoteStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Firestore-backed family subscription document (`families/{id}/subscription/current`).
 */
@Singleton
class SubscriptionRemoteStoreImpl @Inject constructor(
    private val firestoreDataSource: FirestoreDataSource,
    private val firebaseAvailability: FirebaseAvailability,
) : SubscriptionRemoteStore {

    override fun observe(familyId: String): Flow<SubscriptionInfo?> {
        if (familyId.isBlank() || !firebaseAvailability.isCloudAuthAvailable()) {
            return flowOf(null)
        }
        return firestoreDataSource.observeSubscription(familyId)
            .map { dto -> dto?.toDomain() }
            .catch { error ->
                Timber.w(error, "Remote subscription observe failed")
                emit(null)
            }
    }

    override suspend fun upsert(info: SubscriptionInfo) {
        if (info.familyId.isBlank() || !firebaseAvailability.isCloudAuthAvailable()) return
        runCatching { firestoreDataSource.upsertSubscription(info.toDto()) }
            .onFailure { Timber.w(it, "Remote subscription upsert failed") }
    }

    private fun SubscriptionInfo.toDto() = SubscriptionDto(
        familyId, plan.name, status.name, productId, purchaseToken, expiresAt, autoRenewing, updatedAt,
    )

    private fun SubscriptionDto.toDomain() = SubscriptionInfo(
        familyId,
        plan.toSubscriptionPlan(),
        runCatching { SubscriptionStatus.valueOf(status) }.getOrDefault(SubscriptionStatus.NONE),
        productId,
        purchaseToken,
        expiresAt,
        autoRenewing,
        updatedAt,
    )

    private fun String.toSubscriptionPlan(): SubscriptionPlan = when (uppercase()) {
        "PLUS", "FAMILY_PRO", "PREMIUM" -> SubscriptionPlan.PREMIUM
        else -> runCatching { SubscriptionPlan.valueOf(uppercase()) }.getOrDefault(SubscriptionPlan.FREE)
    }
}

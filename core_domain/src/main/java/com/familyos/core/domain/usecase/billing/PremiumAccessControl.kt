package com.familyos.core.domain.usecase.billing

import com.familyos.core.domain.model.EntitlementLimits
import com.familyos.core.domain.model.SubscriptionInfo
import com.familyos.core.domain.repository.BillingRepository
import com.familyos.core.domain.repository.FamilyRepository
import com.familyos.core.domain.util.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Central premium entitlement gate for FREE vs PREMIUM limits and feature flags.
 */
class PremiumAccessControl @Inject constructor(
    private val billingRepository: BillingRepository,
    private val familyRepository: FamilyRepository,
) {
    data class Entitlements(
        val isPremium: Boolean,
        val maxMembers: Int,
        val maxFamilies: Int,
        val maxStorageBytes: Long,
        val aiEnabled: Boolean,
        val advancedAnalyticsEnabled: Boolean,
        val exportEnabled: Boolean,
    )

    fun observeEntitlements(familyId: String): Flow<Entitlements> =
        billingRepository.observeSubscription(familyId).map { it.toEntitlements() }

    suspend fun getEntitlements(familyId: String): Result<Entitlements> =
        when (val sub = billingRepository.getSubscription(familyId)) {
            is Result.Success -> Result.success(sub.data.toEntitlements())
            is Result.Error -> sub
        }

    fun observeCanAddMember(familyId: String): Flow<Boolean> =
        combine(
            billingRepository.observeSubscription(familyId),
            familyRepository.observeMembers(familyId),
        ) { sub, members ->
            val ents = sub.toEntitlements()
            ents.isPremium || members.size < ents.maxMembers
        }

    private fun SubscriptionInfo.toEntitlements(): Entitlements {
        val premium = isPremium
        return Entitlements(
            isPremium = premium,
            maxMembers = if (premium) Int.MAX_VALUE else EntitlementLimits.FREE_MAX_MEMBERS,
            maxFamilies = if (premium) Int.MAX_VALUE else EntitlementLimits.FREE_MAX_FAMILIES,
            maxStorageBytes = if (premium) {
                EntitlementLimits.PREMIUM_MAX_STORAGE_BYTES
            } else {
                EntitlementLimits.FREE_MAX_STORAGE_BYTES
            },
            aiEnabled = premium,
            advancedAnalyticsEnabled = premium,
            exportEnabled = premium,
        )
    }
}

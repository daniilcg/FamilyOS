package com.familyos.core.domain.usecase.billing

import com.familyos.core.domain.model.DeveloperAccounts
import com.familyos.core.domain.model.EntitlementLimits
import com.familyos.core.domain.model.Family
import com.familyos.core.domain.model.FamilyMember
import com.familyos.core.domain.model.SubscriptionInfo
import com.familyos.core.domain.model.User
import com.familyos.core.domain.repository.AuthRepository
import com.familyos.core.domain.repository.BillingRepository
import com.familyos.core.domain.repository.FamilyRepository
import com.familyos.core.domain.util.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Central premium entitlement gate for FREE vs PREMIUM limits and feature flags.
 *
 * Developer-owned families (see [DeveloperAccounts]) are always Premium, including members.
 */
class PremiumAccessControl @Inject constructor(
    private val billingRepository: BillingRepository,
    private val familyRepository: FamilyRepository,
    private val authRepository: AuthRepository,
) {
    data class Entitlements(
        val isPremium: Boolean,
        val maxMembers: Int,
        val maxFamilies: Int,
        val maxStorageBytes: Long,
        val aiEnabled: Boolean,
        val advancedAnalyticsEnabled: Boolean,
        val exportEnabled: Boolean,
        val isDeveloperFamily: Boolean = false,
    )

    fun observeEntitlements(familyId: String): Flow<Entitlements> =
        combine(
            billingRepository.observeSubscription(familyId),
            familyRepository.observeFamily(familyId),
            familyRepository.observeMembers(familyId),
            authRepository.observeAuthState(),
        ) { sub, family, members, user ->
            sub.toEntitlements(family, members, user)
        }

    suspend fun getEntitlements(familyId: String): Result<Entitlements> {
        val user = authRepository.getCurrentUser()
        val family = when (val result = familyRepository.getFamily(familyId)) {
            is Result.Success -> result.data
            is Result.Error -> null
        }
        return when (val sub = billingRepository.getSubscription(familyId)) {
            is Result.Success -> Result.success(sub.data.toEntitlements(family, emptyList(), user))
            is Result.Error -> sub
        }
    }

    fun observeCanAddMember(familyId: String): Flow<Boolean> =
        combine(
            observeEntitlements(familyId),
            familyRepository.observeMembers(familyId),
        ) { ents, members ->
            ents.isPremium || members.size < ents.maxMembers
        }

    private fun SubscriptionInfo.toEntitlements(
        family: Family?,
        members: List<FamilyMember>,
        user: User?,
    ): Entitlements {
        val developerFamily = DeveloperAccounts.isDeveloperOwnedFamily(user, family, members)
        val premium = isPremium || developerFamily
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
            isDeveloperFamily = developerFamily,
        )
    }
}

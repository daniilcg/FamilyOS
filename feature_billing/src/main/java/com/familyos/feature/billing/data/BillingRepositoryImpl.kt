package com.familyos.feature.billing.data

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.familyos.core.domain.model.BillingProducts
import com.familyos.core.domain.model.SubscriptionInfo
import com.familyos.core.domain.model.SubscriptionPlan
import com.familyos.core.domain.model.SubscriptionStatus
import com.familyos.core.domain.repository.BillingProductDetails
import com.familyos.core.domain.repository.BillingRepository
import com.familyos.core.domain.util.AppError
import com.familyos.core.domain.util.Result
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Google Play Billing Library 7 implementation for FamilyOS Premium subscriptions.
 *
 * Product IDs:
 * - [BillingProducts.PREMIUM_MONTHLY]
 * - [BillingProducts.PREMIUM_YEARLY]
 */
@Singleton
class BillingRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : BillingRepository, PurchasesUpdatedListener {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val mutex = Mutex()

    private val subscriptionByFamily = MutableStateFlow<Map<String, SubscriptionInfo>>(emptyMap())
    private val productDetails = MutableStateFlow<List<ProductDetails>>(emptyList())

    @Volatile
    private var activityProvider: (() -> Activity?)? = null

    @Volatile
    private var activeFamilyId: String? = null

    private val billingClient: BillingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder()
                .enableOneTimeProducts()
                .build(),
        )
        .build()

    init {
        startConnection()
    }

    /** Registers the foreground activity used to launch billing flows. */
    fun setActivityProvider(provider: () -> Activity?) {
        activityProvider = provider
    }

    /** Sets the family context for incoming purchase callbacks. */
    fun setActiveFamilyId(familyId: String) {
        activeFamilyId = familyId
        if (subscriptionByFamily.value[familyId] == null) {
            subscriptionByFamily.update {
                it + (familyId to SubscriptionInfo(familyId = familyId))
            }
        }
    }

    override fun observeSubscription(familyId: String): Flow<SubscriptionInfo> =
        subscriptionByFamily.map { map ->
            map[familyId] ?: SubscriptionInfo(familyId = familyId)
        }

    override fun observeIsPremium(familyId: String): Flow<Boolean> =
        observeSubscription(familyId).map { it.isPremium }

    override suspend fun getSubscription(familyId: String): Result<SubscriptionInfo> =
        Result.success(subscriptionByFamily.value[familyId] ?: SubscriptionInfo(familyId = familyId))

    override suspend fun launchPurchase(familyId: String, productId: String): Result<Unit> =
        withContext(Dispatchers.Main) {
            setActiveFamilyId(familyId)
            ensureConnected()
            val details = productDetails.value.firstOrNull { it.productId == productId }
                ?: queryProductDetailsInternal().getOrNull()?.firstOrNull { it.productId == productId }
                ?: return@withContext Result.failure(AppError.Billing("Product not found: $productId"))
            val offerToken = details.subscriptionOfferDetails?.firstOrNull()?.offerToken
                ?: return@withContext Result.failure(AppError.Billing("No subscription offer for $productId"))
            val activity = activityProvider?.invoke()
                ?: return@withContext Result.failure(AppError.Billing("No Activity available for billing flow"))
            val params = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(
                    listOf(
                        BillingFlowParams.ProductDetailsParams.newBuilder()
                            .setProductDetails(details)
                            .setOfferToken(offerToken)
                            .build(),
                    ),
                )
                .build()
            val result = billingClient.launchBillingFlow(activity, params)
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                Result.success(Unit)
            } else {
                Result.failure(AppError.Billing(result.debugMessage.ifBlank { "Billing flow failed (${result.responseCode})" }))
            }
        }

    override suspend fun purchase(
        familyId: String,
        plan: SubscriptionPlan,
        purchaseToken: String,
        productId: String,
    ): Result<SubscriptionInfo> {
        val info = SubscriptionInfo(
            familyId = familyId,
            plan = plan,
            status = SubscriptionStatus.ACTIVE,
            productId = productId,
            purchaseToken = purchaseToken,
            autoRenewing = true,
            updatedAt = System.currentTimeMillis(),
        )
        subscriptionByFamily.update { it + (familyId to info) }
        return Result.success(info)
    }

    override suspend fun restorePurchases(familyId: String): Result<SubscriptionInfo> =
        withContext(Dispatchers.IO) {
            setActiveFamilyId(familyId)
            ensureConnected()
            val purchases = queryActiveSubscriptions()
            val premium = purchases.firstOrNull { purchase ->
                purchase.products.any { it in BillingProducts.ALL }
            }
            if (premium == null) {
                val free = SubscriptionInfo(familyId = familyId, plan = SubscriptionPlan.FREE, status = SubscriptionStatus.NONE)
                subscriptionByFamily.update { it + (familyId to free) }
                return@withContext Result.success(free)
            }
            acknowledgeIfNeeded(premium)
            val productId = premium.products.first { it in BillingProducts.ALL }
            purchase(
                familyId = familyId,
                plan = SubscriptionPlan.PREMIUM,
                purchaseToken = premium.purchaseToken,
                productId = productId,
            )
        }

    override suspend fun queryProductDetails(): Result<List<BillingProductDetails>> =
        queryProductDetailsInternal().map { list ->
            list.map { details ->
                val offer = details.subscriptionOfferDetails?.firstOrNull()
                val phase = offer?.pricingPhases?.pricingPhaseList?.firstOrNull()
                BillingProductDetails(
                    productId = details.productId,
                    title = details.title,
                    description = details.description,
                    formattedPrice = phase?.formattedPrice ?: "",
                    billingPeriod = phase?.billingPeriod ?: "",
                )
            }
        }

    override suspend fun cancelLocally(familyId: String): Result<SubscriptionInfo> {
        val current = subscriptionByFamily.value[familyId] ?: SubscriptionInfo(familyId = familyId)
        val updated = current.copy(
            status = SubscriptionStatus.CANCELED,
            plan = SubscriptionPlan.FREE,
            updatedAt = System.currentTimeMillis(),
        )
        subscriptionByFamily.update { it + (familyId to updated) }
        return Result.success(updated)
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: MutableList<Purchase>?) {
        if (billingResult.responseCode != BillingClient.BillingResponseCode.OK || purchases == null) return
        val familyId = activeFamilyId ?: return
        scope.launch {
            purchases.forEach { purchase ->
                if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                    acknowledgeIfNeeded(purchase)
                    val productId = purchase.products.firstOrNull { it in BillingProducts.ALL } ?: return@forEach
                    purchase(
                        familyId = familyId,
                        plan = SubscriptionPlan.PREMIUM,
                        purchaseToken = purchase.purchaseToken,
                        productId = productId,
                    )
                }
            }
        }
    }

    private fun startConnection() {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    scope.launch { queryProductDetailsInternal() }
                }
            }

            override fun onBillingServiceDisconnected() {
                // BillingClient reconnects on next ensureConnected()
            }
        })
    }

    private suspend fun ensureConnected() {
        if (billingClient.isReady) return
        mutex.withLock {
            if (billingClient.isReady) return
            suspendCancellableCoroutine { cont ->
                billingClient.startConnection(object : BillingClientStateListener {
                    override fun onBillingSetupFinished(billingResult: BillingResult) {
                        if (cont.isActive) cont.resume(Unit)
                    }

                    override fun onBillingServiceDisconnected() {
                        if (cont.isActive) cont.resume(Unit)
                    }
                })
            }
        }
    }

    private suspend fun queryProductDetailsInternal(): Result<List<ProductDetails>> =
        withContext(Dispatchers.IO) {
            ensureConnected()
            val params = QueryProductDetailsParams.newBuilder()
                .setProductList(
                    BillingProducts.ALL.map { id ->
                        QueryProductDetailsParams.Product.newBuilder()
                            .setProductId(id)
                            .setProductType(BillingClient.ProductType.SUBS)
                            .build()
                    },
                )
                .build()
            suspendCancellableCoroutine { cont ->
                // Billing Library 7: listener receives (BillingResult, List<ProductDetails>).
                billingClient.queryProductDetailsAsync(params) { result, productDetailsList ->
                    if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                        val list = productDetailsList.orEmpty()
                        productDetails.value = list
                        cont.resume(Result.success(list))
                    } else {
                        cont.resume(
                            Result.failure(
                                AppError.Billing(result.debugMessage.ifBlank { "queryProductDetails failed" }),
                            ),
                        )
                    }
                }
            }
        }

    private suspend fun queryActiveSubscriptions(): List<Purchase> =
        suspendCancellableCoroutine { cont ->
            billingClient.queryPurchasesAsync(
                QueryPurchasesParams.newBuilder()
                    .setProductType(BillingClient.ProductType.SUBS)
                    .build(),
            ) { result, purchases ->
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    cont.resume(purchases)
                } else {
                    cont.resume(emptyList())
                }
            }
        }

    private suspend fun acknowledgeIfNeeded(purchase: Purchase) {
        if (purchase.isAcknowledged) return
        suspendCancellableCoroutine { cont ->
            billingClient.acknowledgePurchase(
                AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build(),
            ) { cont.resume(Unit) }
        }
    }
}

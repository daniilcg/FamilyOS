package com.familyos.feature.billing.data

import android.app.Activity
import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
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
import com.familyos.core.domain.repository.SubscriptionRemoteStore
import com.familyos.core.domain.util.AppError
import com.familyos.core.domain.util.Result
import com.familyos.feature.billing.BillingConstants
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

private val Context.billingDataStore by preferencesDataStore("familyos_billing")

/**
 * Google Play Billing Library 7 implementation for FamilyOS Premium subscriptions.
 */
@Singleton
class BillingRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val remoteStore: SubscriptionRemoteStore,
) : BillingRepository, PurchasesUpdatedListener {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val mutex = Mutex()
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val manualPremiumKey = stringPreferencesKey("manual_premium_map")

    private val subscriptionByFamily = MutableStateFlow<Map<String, SubscriptionInfo>>(emptyMap())
    private val productDetails = MutableStateFlow<List<ProductDetails>>(emptyList())

    @Volatile
    private var activityProvider: (() -> Activity?)? = null

    @Volatile
    private var activeFamilyId: String? = null

    /** Single in-flight connection; prevents double startConnection() hangs. */
    @Volatile
    private var connecting: CompletableDeferred<Boolean>? = null

    private val remoteListenIds = mutableSetOf<String>()

    private val billingClient: BillingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder()
                .enableOneTimeProducts()
                .build(),
        )
        .build()

    init {
        scope.launch {
            runCatching { ensureConnected() }
            runCatching { queryProductDetailsInternal() }
            restoreManualPremiumFromStore()
        }
    }

    fun setActivityProvider(provider: () -> Activity?) {
        activityProvider = provider
    }

    fun setActiveFamilyId(familyId: String) {
        activeFamilyId = familyId
        if (subscriptionByFamily.value[familyId] == null) {
            subscriptionByFamily.update {
                it + (familyId to SubscriptionInfo(familyId = familyId))
            }
        }
        scope.launch { listenRemote(familyId) }
    }

    override fun observeSubscription(familyId: String): Flow<SubscriptionInfo> {
        scope.launch { listenRemote(familyId) }
        return subscriptionByFamily.map { map ->
            map[familyId] ?: SubscriptionInfo(familyId = familyId)
        }
    }

    override fun observeIsPremium(familyId: String): Flow<Boolean> =
        observeSubscription(familyId).map { it.isPremium }

    override suspend fun getSubscription(familyId: String): Result<SubscriptionInfo> =
        Result.success(subscriptionByFamily.value[familyId] ?: SubscriptionInfo(familyId = familyId))

    override suspend fun launchPurchase(familyId: String, productId: String): Result<Unit> =
        withContext(Dispatchers.Main) {
            setActiveFamilyId(familyId)
            if (!ensureConnected()) {
                return@withContext Result.failure(
                    AppError.Billing("Google Play Billing unavailable. Use PayPal below or try again later."),
                )
            }
            val details = productDetails.value.firstOrNull { it.productId == productId }
                ?: queryProductDetailsInternal().getOrNull()?.firstOrNull { it.productId == productId }
                ?: return@withContext Result.failure(
                    AppError.Billing(
                        "Play product not available (sideload / no Play Store). Use PayPal activation below.",
                    ),
                )
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
                Result.failure(
                    AppError.Billing(
                        result.debugMessage.ifBlank { "Billing flow failed (${result.responseCode})" },
                    ),
                )
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
            autoRenewing = productId != BillingConstants.PAYPAL_PRODUCT_ID,
            updatedAt = System.currentTimeMillis(),
        )
        subscriptionByFamily.update { it + (familyId to info) }
        persistManualPremiumMap()
        runCatching { remoteStore.upsert(info) }
        return Result.success(info)
    }

    suspend fun grantManualPremium(familyId: String, days: Int = BillingConstants.MANUAL_PREMIUM_DAYS.toInt()): Result<SubscriptionInfo> {
        val now = System.currentTimeMillis()
        val lifetime = days <= 0
        return applyGrant(
            SubscriptionInfo(
                familyId = familyId,
                plan = SubscriptionPlan.PREMIUM,
                status = SubscriptionStatus.ACTIVE,
                productId = BillingConstants.PAYPAL_PRODUCT_ID,
                purchaseToken = BillingConstants.PAYPAL_TOKEN,
                expiresAt = if (lifetime) null else now + days * 24L * 60L * 60L * 1000L,
                autoRenewing = false,
                updatedAt = now,
            ),
        )
    }

    override suspend fun grantDeveloperPremium(familyId: String): Result<SubscriptionInfo> {
        val existing = subscriptionByFamily.value[familyId]
        if (existing?.isPremium == true && existing.productId == BillingConstants.DEVELOPER_PRODUCT_ID) {
            return Result.success(existing)
        }
        val now = System.currentTimeMillis()
        return applyGrant(
            SubscriptionInfo(
                familyId = familyId,
                plan = SubscriptionPlan.PREMIUM,
                status = SubscriptionStatus.ACTIVE,
                productId = BillingConstants.DEVELOPER_PRODUCT_ID,
                purchaseToken = BillingConstants.DEVELOPER_TOKEN,
                expiresAt = null,
                autoRenewing = true,
                updatedAt = now,
            ),
        )
    }

    private suspend fun applyGrant(info: SubscriptionInfo): Result<SubscriptionInfo> {
        subscriptionByFamily.update { it + (info.familyId to info) }
        persistManualPremiumMap()
        runCatching { remoteStore.upsert(info) }
        return Result.success(info)
    }

    private suspend fun restoreManualPremiumFromStore() {
        runCatching {
            val raw = context.billingDataStore.data.first()[manualPremiumKey].orEmpty()
            if (raw.isBlank()) return
            val map = json.decodeFromString<Map<String, SubscriptionInfo>>(raw)
            subscriptionByFamily.update { current -> current + map }
        }
    }

    private suspend fun persistManualPremiumMap() {
        val manual = subscriptionByFamily.value.filterValues {
            BillingConstants.isManualProduct(it.productId)
        }
        context.billingDataStore.edit { prefs ->
            prefs[manualPremiumKey] = json.encodeToString(manual)
        }
    }

    override suspend fun restorePurchases(familyId: String): Result<SubscriptionInfo> =
        withContext(Dispatchers.IO) {
            setActiveFamilyId(familyId)
            // Keep manual PayPal premium if present
            val existing = subscriptionByFamily.value[familyId]
            if (existing?.isPremium == true && BillingConstants.isManualProduct(existing.productId)) {
                return@withContext Result.success(existing)
            }
            if (!ensureConnected()) {
                return@withContext Result.success(
                    existing ?: SubscriptionInfo(familyId = familyId),
                )
            }
            val purchases = queryActiveSubscriptions()
            val premium = purchases.firstOrNull { purchase ->
                purchase.products.any { it in BillingProducts.ALL }
            }
            if (premium == null) {
                val free = existing?.takeIf { it.isPremium }
                    ?: SubscriptionInfo(familyId = familyId, plan = SubscriptionPlan.FREE, status = SubscriptionStatus.NONE)
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

    private suspend fun listenRemote(familyId: String) {
        if (familyId.isBlank()) return
        val start = synchronized(remoteListenIds) { remoteListenIds.add(familyId) }
        if (!start) return
        remoteStore.observe(familyId).collect { remote ->
            if (remote == null) return@collect
            val local = subscriptionByFamily.value[familyId]
            val chosen = pickBetter(local, remote)
            if (chosen != local) {
                subscriptionByFamily.update { it + (familyId to chosen) }
                if (BillingConstants.isManualProduct(chosen.productId)) {
                    persistManualPremiumMap()
                }
            }
        }
    }

    private fun pickBetter(local: SubscriptionInfo?, remote: SubscriptionInfo): SubscriptionInfo {
        if (local == null) return remote
        if (remote.isPremium && !local.isPremium) return remote
        if (local.isPremium && !remote.isPremium) return local
        if (remote.productId == BillingConstants.DEVELOPER_PRODUCT_ID) return remote
        if (local.productId == BillingConstants.DEVELOPER_PRODUCT_ID) return local
        return if (remote.updatedAt >= local.updatedAt) remote else local
    }

    /**
     * Connects to Play Billing with a single in-flight attempt and timeout.
     * @return true if ready, false if Play Billing is unavailable (sideload / timeout).
     */
    private suspend fun ensureConnected(): Boolean {
        if (billingClient.isReady) return true
        return mutex.withLock {
            if (billingClient.isReady) return@withLock true
            val existing = connecting
            if (existing != null) {
                return@withLock withTimeoutOrNull(CONNECT_TIMEOUT_MS) { existing.await() } == true
            }
            val deferred = CompletableDeferred<Boolean>()
            connecting = deferred
            billingClient.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(billingResult: BillingResult) {
                    val ok = billingResult.responseCode == BillingClient.BillingResponseCode.OK
                    if (!deferred.isCompleted) deferred.complete(ok)
                    connecting = null
                }

                override fun onBillingServiceDisconnected() {
                    connecting = null
                }
            })
            val ok = withTimeoutOrNull(CONNECT_TIMEOUT_MS) { deferred.await() } == true
            if (!ok && !deferred.isCompleted) {
                deferred.complete(false)
                connecting = null
            }
            ok && billingClient.isReady
        }
    }

    private suspend fun queryProductDetailsInternal(): Result<List<ProductDetails>> =
        withContext(Dispatchers.IO) {
            if (!ensureConnected()) {
                return@withContext Result.failure(
                    AppError.Billing("Play Billing unavailable"),
                )
            }
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
            withTimeoutOrNull(QUERY_TIMEOUT_MS) {
                suspendCancellableCoroutine { cont ->
                    billingClient.queryProductDetailsAsync(params) { result, productDetailsList ->
                        if (!cont.isActive) return@queryProductDetailsAsync
                        if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                            val list = productDetailsList.orEmpty()
                            productDetails.value = list
                            cont.resume(Result.success(list))
                        } else {
                            cont.resume(
                                Result.failure(
                                    AppError.Billing(
                                        result.debugMessage.ifBlank { "queryProductDetails failed" },
                                    ),
                                ),
                            )
                        }
                    }
                }
            } ?: Result.failure(AppError.Billing("Play Billing query timed out"))
        }

    private suspend fun queryActiveSubscriptions(): List<Purchase> =
        withTimeoutOrNull(QUERY_TIMEOUT_MS) {
            suspendCancellableCoroutine { cont ->
                billingClient.queryPurchasesAsync(
                    QueryPurchasesParams.newBuilder()
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build(),
                ) { result, purchases ->
                    if (!cont.isActive) return@queryPurchasesAsync
                    if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                        cont.resume(purchases)
                    } else {
                        cont.resume(emptyList())
                    }
                }
            }
        } ?: emptyList()

    private suspend fun acknowledgeIfNeeded(purchase: Purchase) {
        if (purchase.isAcknowledged) return
        withTimeoutOrNull(QUERY_TIMEOUT_MS) {
            suspendCancellableCoroutine { cont ->
                billingClient.acknowledgePurchase(
                    AcknowledgePurchaseParams.newBuilder()
                        .setPurchaseToken(purchase.purchaseToken)
                        .build(),
                ) {
                    if (cont.isActive) cont.resume(Unit)
                }
            }
        }
    }

    private companion object {
        const val CONNECT_TIMEOUT_MS = 8_000L
        const val QUERY_TIMEOUT_MS = 10_000L
    }
}

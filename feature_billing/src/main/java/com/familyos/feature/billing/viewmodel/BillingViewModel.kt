package com.familyos.feature.billing.viewmodel

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.familyos.core.domain.model.BillingProducts
import com.familyos.core.domain.model.EntitlementLimits
import com.familyos.core.domain.model.SubscriptionInfo
import com.familyos.core.domain.repository.BillingProductDetails
import com.familyos.core.domain.repository.UserPreferencesRepository
import com.familyos.core.domain.usecase.auth.GetCurrentUserUseCase
import com.familyos.core.domain.usecase.billing.LaunchPurchaseUseCase
import com.familyos.core.domain.usecase.billing.ObserveSubscriptionUseCase
import com.familyos.core.domain.usecase.billing.PremiumAccessControl
import com.familyos.core.domain.usecase.billing.RestorePurchasesUseCase
import com.familyos.core.domain.util.Result
import com.familyos.feature.billing.BillingConstants
import com.familyos.feature.billing.data.BillingRepositoryImpl
import com.familyos.feature.billing.export.ExportExcelUseCase
import com.familyos.feature.billing.export.ExportPdfUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import javax.inject.Inject

/** Paywall / billing UI state. */
data class BillingUiState(
    val subscription: SubscriptionInfo? = null,
    val entitlements: PremiumAccessControl.Entitlements? = null,
    val products: List<BillingProductDetails> = emptyList(),
    val isLoading: Boolean = true,
    val isPurchasing: Boolean = false,
    val isRestoring: Boolean = false,
    val redeemCode: String = "",
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val lastExportFile: File? = null,
    val familyId: String? = null,
)

/**
 * Billing ViewModel for paywall, restore, entitlements, PayPal redeem, and premium exports.
 */
@HiltViewModel
class BillingViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val observeSubscription: ObserveSubscriptionUseCase,
    private val restorePurchases: RestorePurchasesUseCase,
    private val launchPurchase: LaunchPurchaseUseCase,
    private val premiumAccess: PremiumAccessControl,
    private val billingRepositoryImpl: BillingRepositoryImpl,
    private val exportPdf: ExportPdfUseCase,
    private val exportExcel: ExportExcelUseCase,
    private val preferencesRepository: UserPreferencesRepository,
    private val getCurrentUser: GetCurrentUserUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(BillingUiState())
    val state: StateFlow<BillingUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                val user = getCurrentUser()
                val prefs = preferencesRepository.get()
                val familyId = prefs.activeFamilyId ?: user?.familyId
                if (familyId.isNullOrBlank()) {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "No active family. Create or join a family first.",
                        )
                    }
                    return@launch
                }
                billingRepositoryImpl.setActiveFamilyId(familyId)
                // Show paywall immediately — never wait on Play Billing connection.
                _state.update {
                    it.copy(
                        familyId = familyId,
                        subscription = SubscriptionInfo(familyId = familyId),
                        isLoading = false,
                    )
                }
                launch {
                    observeSubscription(familyId).collect { sub ->
                        _state.update { it.copy(subscription = sub, isLoading = false) }
                    }
                }
                launch {
                    premiumAccess.observeEntitlements(familyId).collect { ents ->
                        _state.update { it.copy(entitlements = ents) }
                    }
                }
                refreshProducts()
            } catch (t: Throwable) {
                Timber.e(t, "Billing init failed")
                _state.update {
                    it.copy(isLoading = false, errorMessage = t.message ?: "Failed to load billing")
                }
            }
        }
    }

    fun bindActivity(activity: Activity) {
        billingRepositoryImpl.setActivityProvider { activity }
    }

    fun refreshProducts() {
        viewModelScope.launch {
            when (val result = billingRepositoryImpl.queryProductDetails()) {
                is Result.Success -> _state.update { it.copy(products = result.data) }
                is Result.Error -> {
                    // Sideload / no Play products — keep PayPal path usable, don't block UI.
                    Timber.w("Product query: %s", result.error.message)
                }
            }
        }
    }

    fun purchaseMonthly() = purchase(BillingProducts.PREMIUM_MONTHLY)

    fun purchaseYearly() = purchase(BillingProducts.PREMIUM_YEARLY)

    private fun purchase(productId: String) {
        val familyId = _state.value.familyId ?: return
        viewModelScope.launch {
            _state.update { it.copy(isPurchasing = true, errorMessage = null, successMessage = null) }
            try {
                when (val result = launchPurchase(familyId, productId)) {
                    is Result.Success -> _state.update {
                        it.copy(successMessage = "Purchase flow launched")
                    }
                    is Result.Error -> _state.update {
                        it.copy(errorMessage = result.error.message)
                    }
                }
            } finally {
                _state.update { it.copy(isPurchasing = false) }
            }
        }
    }

    fun restore() {
        val familyId = _state.value.familyId ?: return
        viewModelScope.launch {
            _state.update { it.copy(isRestoring = true, errorMessage = null) }
            try {
                when (val result = restorePurchases(familyId)) {
                    is Result.Success -> _state.update {
                        it.copy(
                            subscription = result.data,
                            successMessage = if (result.data.isPremium) {
                                "Premium restored"
                            } else {
                                "No active Play subscription found"
                            },
                        )
                    }
                    is Result.Error -> _state.update {
                        it.copy(errorMessage = result.error.message)
                    }
                }
            } finally {
                _state.update { it.copy(isRestoring = false, isLoading = false) }
            }
        }
    }

    fun openPayPal() {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(BillingConstants.PAYPAL_ME_URL)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { appContext.startActivity(intent) }
            .onFailure {
                _state.update { s -> s.copy(errorMessage = "Could not open PayPal") }
            }
    }

    fun setRedeemCode(code: String) {
        _state.update { it.copy(redeemCode = code) }
    }

    fun redeemPayPalCode(code: String = _state.value.redeemCode) {
        val familyId = _state.value.familyId ?: return
        val trimmed = code.trim()
        if (!trimmed.equals(BillingConstants.REDEEM_CODE, ignoreCase = true)) {
            _state.update { it.copy(errorMessage = "Invalid activation code") }
            return
        }
        viewModelScope.launch {
            when (val result = billingRepositoryImpl.grantManualPremium(familyId)) {
                is Result.Success -> _state.update {
                    it.copy(
                        subscription = result.data,
                        redeemCode = "",
                        successMessage = "Premium activated for 1 year (PayPal)",
                        errorMessage = null,
                    )
                }
                is Result.Error -> _state.update { it.copy(errorMessage = result.error.message) }
            }
        }
    }

    fun exportPdfReport(familyName: String) {
        if (_state.value.entitlements?.exportEnabled != true) {
            _state.update { it.copy(errorMessage = "PDF export requires Premium") }
            return
        }
        viewModelScope.launch {
            when (
                val result = exportPdf(
                    ExportPdfUseCase.ExportInput(familyName = familyName),
                )
            ) {
                is Result.Success -> _state.update {
                    it.copy(lastExportFile = result.data, successMessage = "PDF saved: ${result.data.name}")
                }
                is Result.Error -> _state.update { it.copy(errorMessage = result.error.message) }
            }
        }
    }

    fun exportExcelReport(familyName: String) {
        if (_state.value.entitlements?.exportEnabled != true) {
            _state.update { it.copy(errorMessage = "Excel export requires Premium") }
            return
        }
        viewModelScope.launch {
            when (
                val result = exportExcel(
                    ExportExcelUseCase.ExportInput(familyName = familyName),
                )
            ) {
                is Result.Success -> _state.update {
                    it.copy(
                        lastExportFile = result.data.xmlFile,
                        successMessage = "Excel/CSV saved: ${result.data.csvFile.name}",
                    )
                }
                is Result.Error -> _state.update { it.copy(errorMessage = result.error.message) }
            }
        }
    }

    fun clearMessages() {
        _state.update { it.copy(errorMessage = null, successMessage = null) }
    }

    companion object {
        val FREE_LIMITS_TEXT =
            "FREE: ${EntitlementLimits.FREE_MAX_MEMBERS} members, " +
                "${EntitlementLimits.FREE_MAX_FAMILIES} family, 2GB files"
        val PREMIUM_LIMITS_TEXT =
            "PREMIUM: unlimited families/members, 50GB, AI, analytics, PDF/Excel export"
    }
}

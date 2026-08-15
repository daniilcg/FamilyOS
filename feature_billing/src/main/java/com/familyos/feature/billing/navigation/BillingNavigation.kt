package com.familyos.feature.billing.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.familyos.feature.billing.ui.PaywallScreen
import com.familyos.feature.billing.viewmodel.BillingViewModel

/** Billing route. */
object BillingRoutes {
    const val PAYWALL = "billing/paywall"
}

/** Registers billing / paywall destination. */
fun NavGraphBuilder.billingGraph() {
    composable(BillingRoutes.PAYWALL) { BillingRoute() }
}

@Composable
private fun BillingRoute() {
    val vm: BillingViewModel = hiltViewModel()
    val state by vm.state.collectAsStateWithLifecycle()
    PaywallScreen(
        state = state,
        onBindActivity = vm::bindActivity,
        onPurchaseMonthly = vm::purchaseMonthly,
        onPurchaseYearly = vm::purchaseYearly,
        onRestore = vm::restore,
        onExportPdf = { vm.exportPdfReport("Family") },
        onExportExcel = { vm.exportExcelReport("Family") },
    )
}

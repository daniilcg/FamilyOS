package com.familyos.feature.billing.ui

import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.familyos.core.domain.model.BillingProducts
import com.familyos.core.ui.components.FamilyLoading
import com.familyos.feature.billing.BillingConstants
import com.familyos.feature.billing.viewmodel.BillingUiState
import com.familyos.feature.billing.viewmodel.BillingViewModel
import com.familyos.core.ui.locale.rememberUiStrings

/**
 * Premium paywall with Play Billing, PayPal direct pay + redeem, restore, and exports.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaywallScreen(
    state: BillingUiState,
    onBindActivity: (Activity) -> Unit,
    onPurchaseMonthly: () -> Unit,
    onPurchaseYearly: () -> Unit,
    onRestore: () -> Unit,
    onOpenPayPal: () -> Unit,
    onRedeemCodeChange: (String) -> Unit,
    onRedeemPayPalCode: () -> Unit,
    onExportPdf: () -> Unit,
    onExportExcel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val s = rememberUiStrings()

    val context = LocalContext.current
    DisposableEffect(Unit) {
        val activity = context as? Activity
        if (activity != null) onBindActivity(activity)
        onDispose { }
    }

    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text(s.premiumTitle) }) },
    ) { padding ->
        if (state.isLoading) {
            FamilyLoading()
            return@Scaffold
        }
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                if (state.subscription?.isPremium == true) s.onPremium else s.upgradeFamilyOs,
                style = MaterialTheme.typography.headlineMedium,
            )
            Text(BillingViewModel.FREE_LIMITS_TEXT)
            Text(BillingViewModel.PREMIUM_LIMITS_TEXT)

            Text(
                s.billingPlayPaypalHint,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
            )

            val monthly = state.products.firstOrNull { it.productId == BillingProducts.PREMIUM_MONTHLY }
            val yearly = state.products.firstOrNull { it.productId == BillingProducts.PREMIUM_YEARLY }

            PlanCard(
                title = s.monthly,
                price = monthly?.formattedPrice ?: "…",
                subtitle = monthly?.description ?: BillingProducts.PREMIUM_MONTHLY,
                enabled = !state.isPurchasing && state.subscription?.isPremium != true,
                onClick = onPurchaseMonthly,
            )
            PlanCard(
                title = s.yearly,
                price = yearly?.formattedPrice ?: "…",
                subtitle = yearly?.description ?: BillingProducts.PREMIUM_YEARLY,
                enabled = !state.isPurchasing && state.subscription?.isPremium != true,
                onClick = onPurchaseYearly,
            )

            OutlinedButton(
                onClick = onRestore,
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isRestoring && !state.isPurchasing,
            ) {
                Text(if (state.isRestoring) s.restoring else s.restorePurchases)
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(s.paypalDirect, style = MaterialTheme.typography.titleMedium)
            OutlinedButton(
                onClick = onOpenPayPal,
                modifier = Modifier.fillMaxWidth(),
                enabled = state.subscription?.isPremium != true,
            ) {
                Text(s.payWithPaypal.format(BillingConstants.PAYPAL_HANDLE))
            }
            OutlinedTextField(
                value = state.redeemCode,
                onValueChange = onRedeemCodeChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(s.activationCode) },
                placeholder = { Text(BillingConstants.REDEEM_CODE) },
                singleLine = true,
                enabled = state.subscription?.isPremium != true,
            )
            Text(
                s.afterPaypalHint.format(BillingConstants.PAYPAL_HANDLE, BillingConstants.REDEEM_CODE),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
            )
            Button(
                onClick = onRedeemPayPalCode,
                modifier = Modifier.fillMaxWidth(),
                enabled = state.redeemCode.isNotBlank() && state.subscription?.isPremium != true,
            ) {
                Text(s.activatePremium)
            }

            if (state.entitlements?.exportEnabled == true) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(s.premiumExports, style = MaterialTheme.typography.titleMedium)
                Button(onClick = onExportPdf, modifier = Modifier.fillMaxWidth()) {
                    Text(s.exportPdf)
                }
                Button(onClick = onExportExcel, modifier = Modifier.fillMaxWidth()) {
                    Text(s.exportExcelCsv)
                }
            }

            state.successMessage?.let {
                Text(it, color = MaterialTheme.colorScheme.primary)
            }
            state.errorMessage?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
            }
            state.lastExportFile?.let {
                Text("${s.savedPrefix}: ${it.absolutePath}", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun PlanCard(
    title: String,
    price: String,
    subtitle: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val s = rememberUiStrings()

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            Text(price, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium)
            Button(onClick = onClick, enabled = enabled, modifier = Modifier.fillMaxWidth()) {
                Text(s.subscribe)
            }
        }
    }
}

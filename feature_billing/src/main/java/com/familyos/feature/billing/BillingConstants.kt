package com.familyos.feature.billing

import com.familyos.core.domain.billing.LicenseKey

/**
 * PayPal / Play Billing constants and legacy redeem codes.
 *
 * Customer Premium keys are issued by the Windows app
 * `tools/key_issuer/FamilyOS-KeyIssuer.exe` (signed `FOS-…` keys).
 * The Android app verifies them via [LicenseKey] — no rebuild per customer.
 */
object BillingConstants {
    const val PAYPAL_ME_URL = "https://www.paypal.me/segalcommic"
    const val PAYPAL_HANDLE = "@segalcommic"

    /** Legacy one-year code kept for already-issued customers. */
    const val REDEEM_CODE = "FAMILYOS-SEGAL-PREMIUM"

    /** Lifetime developer unlock (SEGAL COMMUNICATIONS). */
    const val DEVELOPER_REDEEM_CODE = "FAMILYOS-DEV-SEGAL"

    const val PAYPAL_PRODUCT_ID = "paypal_premium_manual"
    const val DEVELOPER_PRODUCT_ID = "developer_lifetime"
    const val PAYPAL_TOKEN = "paypal"
    const val DEVELOPER_TOKEN = "developer"

    const val MANUAL_PREMIUM_DAYS = 365L

    fun isLegacyCustomerCode(code: String): Boolean =
        code.trim().equals(REDEEM_CODE, ignoreCase = true)

    fun isDeveloperCode(code: String): Boolean =
        code.trim().equals(DEVELOPER_REDEEM_CODE, ignoreCase = true)

    fun isManualProduct(productId: String?): Boolean =
        productId == PAYPAL_PRODUCT_ID || productId == DEVELOPER_PRODUCT_ID
}

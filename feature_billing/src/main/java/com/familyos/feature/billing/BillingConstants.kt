package com.familyos.feature.billing

/**
 * PayPal direct-pay + Play Billing constants for FamilyOS Premium.
 *
 * Note: PayPal cannot replace Google Play Billing for Play Store listings;
 * it is offered for direct payment with a manual redeem code.
 */
object BillingConstants {
    const val PAYPAL_ME_URL = "https://www.paypal.me/segalcommic"
    const val PAYPAL_HANDLE = "@segalcommic"
    /** Secret redeem code shown on the paywall after PayPal payment. */
    const val REDEEM_CODE = "FAMILYOS-SEGAL-PREMIUM"
    const val PAYPAL_PRODUCT_ID = "paypal_premium_manual"
    const val PAYPAL_TOKEN = "paypal"
    /** Manual Premium duration after redeem (1 year). */
    const val MANUAL_PREMIUM_DAYS = 365L
}

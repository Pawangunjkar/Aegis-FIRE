package com.example.services

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
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

class BillingManager(
    context: Context,
    private val onProductPriceLoaded: (priceLabel: String, isStoreReady: Boolean) -> Unit,
    private val onEntitlementUpdated: (isProUnlocked: Boolean, isPending: Boolean, message: String?) -> Unit
) {
    companion object {
        const val PRODUCT_ID_AEGIS_PRO = "aegis_fire_pro"
        const val FALLBACK_PRICE_LABEL = "$3.99"
    }

    private val appContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())
    private var proProductDetails: ProductDetails? = null
    private var retryAttempts = 0

    private val purchasesUpdatedListener = PurchasesUpdatedListener { billingResult, purchases ->
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                if (purchases != null) {
                    handlePurchases(purchases, notifySuccess = true)
                }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                mainHandler.post {
                    onEntitlementUpdated(false, false, "Purchase canceled")
                }
            }
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                queryExistingPurchases(notifyIfNotFound = false)
            }
            else -> {
                Log.w("BillingManager", "Purchase failed: ${billingResult.responseCode} ${billingResult.debugMessage}")
                mainHandler.post {
                    onEntitlementUpdated(false, false, "Play Billing unavailable (${billingResult.debugMessage.ifBlank { "Error ${billingResult.responseCode}" }})")
                }
            }
        }
    }

    private val billingClient: BillingClient = BillingClient.newBuilder(appContext)
        .setListener(purchasesUpdatedListener)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder()
                .enableOneTimeProducts()
                .build()
        )
        .build()

    fun startConnection() {
        if (billingClient.isReady) {
            queryProProductDetails()
            queryExistingPurchases(notifyIfNotFound = false)
            return
        }
        try {
            billingClient.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(billingResult: BillingResult) {
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        retryAttempts = 0
                        queryProProductDetails()
                        queryExistingPurchases(notifyIfNotFound = false)
                    } else {
                        mainHandler.post {
                            onProductPriceLoaded(FALLBACK_PRICE_LABEL, false)
                        }
                    }
                }

                override fun onBillingServiceDisconnected() {
                    if (retryAttempts < 3) {
                        retryAttempts++
                        val delayMs = retryAttempts * 2000L
                        mainHandler.postDelayed({ startConnection() }, delayMs)
                    }
                }
            })
        } catch (e: Exception) {
            Log.w("BillingManager", "BillingClient init skipped: ${e.message}")
            mainHandler.post {
                onProductPriceLoaded(FALLBACK_PRICE_LABEL, false)
            }
        }
    }

    fun queryProProductDetails() {
        if (!billingClient.isReady) return
        val product = QueryProductDetailsParams.Product.newBuilder()
            .setProductId(PRODUCT_ID_AEGIS_PRO)
            .setProductType(BillingClient.ProductType.INAPP)
            .build()
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(listOf(product))
            .build()

        billingClient.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && productDetailsList.isNotEmpty()) {
                val details = productDetailsList.first()
                proProductDetails = details
                val price = details.oneTimePurchaseOfferDetails?.formattedPrice ?: FALLBACK_PRICE_LABEL
                mainHandler.post {
                    onProductPriceLoaded(price, true)
                }
            } else {
                mainHandler.post {
                    onProductPriceLoaded(FALLBACK_PRICE_LABEL, false)
                }
            }
        }
    }

    fun queryExistingPurchases(notifyIfNotFound: Boolean = true) {
        if (!billingClient.isReady) {
            startConnection()
            if (notifyIfNotFound) {
                mainHandler.post {
                    onEntitlementUpdated(false, false, "Google Play Billing is not connected right now.")
                }
            }
            return
        }
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()
        billingClient.queryPurchasesAsync(params) { billingResult, purchases ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                val matched = purchases.filter { it.products.contains(PRODUCT_ID_AEGIS_PRO) }
                if (matched.isNotEmpty()) {
                    handlePurchases(matched, notifySuccess = notifyIfNotFound)
                } else if (notifyIfNotFound) {
                    mainHandler.post {
                        onEntitlementUpdated(false, false, "No previous Aegis Pro purchase found on this Google account.")
                    }
                }
            }
        }
    }

    fun launchProPurchase(activity: Activity) {
        val details = proProductDetails
        if (!billingClient.isReady || details == null) {
            startConnection()
            mainHandler.post {
                onEntitlementUpdated(
                    false,
                    false,
                    "Google Play Store product '$PRODUCT_ID_AEGIS_PRO' ($3.99) is available once published to Play Console testing or production."
                )
            }
            return
        }
        val productDetailsParams = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(details)
            .build()
        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productDetailsParams))
            .build()
        billingClient.launchBillingFlow(activity, flowParams)
    }

    private fun handlePurchases(purchases: List<Purchase>, notifySuccess: Boolean) {
        var unlocked = false
        var pending = false

        purchases.forEach { purchase ->
            if (!purchase.products.contains(PRODUCT_ID_AEGIS_PRO)) return@forEach
            when (purchase.purchaseState) {
                Purchase.PurchaseState.PURCHASED -> {
                    unlocked = true
                    if (!purchase.isAcknowledged) {
                        val ackParams = AcknowledgePurchaseParams.newBuilder()
                            .setPurchaseToken(purchase.purchaseToken)
                            .build()
                        billingClient.acknowledgePurchase(ackParams) { ackResult ->
                            Log.d("BillingManager", "Acknowledge result: ${ackResult.responseCode}")
                        }
                    }
                }
                Purchase.PurchaseState.PENDING -> {
                    pending = true
                }
            }
        }

        mainHandler.post {
            val msg = when {
                unlocked && notifySuccess -> "Aegis Pro unlocked! All ads removed and premium tools active."
                pending -> "Purchase pending confirmation from Google Play."
                else -> null
            }
            onEntitlementUpdated(unlocked, pending, msg)
        }
    }

    fun destroy() {
        try {
            billingClient.endConnection()
        } catch (_: Exception) {
        }
    }
}

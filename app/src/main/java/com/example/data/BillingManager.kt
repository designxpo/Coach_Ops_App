package com.example.data

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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Google Play Billing for coach subscriptions (Pro / Business).
 *
 * On a successful, acknowledged purchase we write the tier to
 * user_records/{uid}.plan — the same field EntitlementManager already listens
 * to — so the whole app (and the admin panel) unlocks the plan instantly and
 * consistently. Product IDs must match Play Console exactly.
 *
 * NOTE: entitlement is applied client-side here (consistent with the existing
 * admin-driven trust model). For production hardening, verify the purchaseToken
 * with the Google Play Developer API from a backend and drive plan changes from
 * Real-time Developer Notifications.
 */
object BillingManager {

    const val PRODUCT_PRO = "procoach_pro"
    const val PRODUCT_BUSINESS = "procoach_business"

    private fun planFor(productId: String): SubscriptionPlan? = when (productId) {
        PRODUCT_PRO -> SubscriptionPlan.PRO
        PRODUCT_BUSINESS -> SubscriptionPlan.BUSINESS
        else -> null
    }

    data class PlanOffer(
        val plan: SubscriptionPlan,
        val formattedPrice: String,
        val details: ProductDetails,
        val offerToken: String,
    )

    private val _offers = MutableStateFlow<Map<SubscriptionPlan, PlanOffer>>(emptyMap())
    val offers: StateFlow<Map<SubscriptionPlan, PlanOffer>> = _offers.asStateFlow()

    private val _status = MutableStateFlow("")
    val status: StateFlow<String> = _status.asStateFlow()
    fun clearStatus() { _status.value = "" }

    private var client: BillingClient? = null

    private val purchasesListener = PurchasesUpdatedListener { result, purchases ->
        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK ->
                purchases?.forEach { handlePurchase(it) }
            BillingClient.BillingResponseCode.USER_CANCELED ->
                _status.value = "Purchase cancelled"
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                _status.value = "You already have this plan"; queryPurchases()
            }
            else -> _status.value = "Purchase failed — please try again"
        }
    }

    /** Safe to call repeatedly — connects once and refreshes products/purchases. */
    fun init(context: Context) {
        val existing = client
        if (existing != null && existing.isReady) { queryProducts(); queryPurchases(); return }
        val c = BillingClient.newBuilder(context.applicationContext)
            .setListener(purchasesListener)
            .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
            .build()
        client = c
        c.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    queryProducts(); queryPurchases()
                }
            }
            override fun onBillingServiceDisconnected() { /* retried on next init() */ }
        })
    }

    private fun queryProducts() {
        val c = client ?: return
        val products = listOf(PRODUCT_PRO, PRODUCT_BUSINESS).map { id ->
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(id)
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        }
        val params = QueryProductDetailsParams.newBuilder().setProductList(products).build()
        // Billing Library 8+ hands back a QueryProductDetailsResult (was List<ProductDetails> in v7)
        c.queryProductDetailsAsync(params) { result, queryResult ->
            if (result.responseCode != BillingClient.BillingResponseCode.OK) return@queryProductDetailsAsync
            val map = mutableMapOf<SubscriptionPlan, PlanOffer>()
            for (pd in queryResult.productDetailsList) {
                val plan = planFor(pd.productId) ?: continue
                val offer = pd.subscriptionOfferDetails?.firstOrNull() ?: continue
                // last pricing phase = the recurring price (earlier phases may be a free trial)
                val price = offer.pricingPhases.pricingPhaseList.lastOrNull()?.formattedPrice ?: ""
                map[plan] = PlanOffer(plan, price, pd, offer.offerToken)
            }
            _offers.value = map
        }
    }

    /** Launch the Play purchase sheet for [plan]. Returns false if not ready yet. */
    fun launchPurchase(activity: Activity, plan: SubscriptionPlan): Boolean {
        val c = client
        val offer = _offers.value[plan]
        if (c == null || !c.isReady || offer == null) {
            _status.value = "Plans are still loading — try again in a moment."
            return false
        }
        val params = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(offer.details)
                        .setOfferToken(offer.offerToken)
                        .build()
                )
            ).build()
        c.launchBillingFlow(activity, params)
        return true
    }

    private fun queryPurchases() {
        val c = client ?: return
        c.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.SUBS).build()
        ) { result, purchases ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                if (purchases.isEmpty()) return@queryPurchasesAsync
                purchases.forEach { handlePurchase(it) }
            }
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) return
        val plan = purchase.products.firstNotNullOfOrNull { planFor(it) } ?: return
        applyPlan(plan)
        if (!purchase.isAcknowledged) {
            client?.acknowledgePurchase(
                AcknowledgePurchaseParams.newBuilder().setPurchaseToken(purchase.purchaseToken).build()
            ) { _status.value = "${plan.displayName} activated 🎉" }
        }
    }

    /** Write the tier to user_records.plan — EntitlementManager picks it up live. */
    private fun applyPlan(plan: SubscriptionPlan) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseFirestore.getInstance().collection("user_records").document(uid)
            .set(mapOf("plan" to plan.name), SetOptions.merge())
    }
}

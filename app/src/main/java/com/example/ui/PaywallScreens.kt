package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.BillingManager
import com.example.data.EntitlementManager
import com.example.data.SubscriptionPlan
import com.example.data.UserPreferences
import com.example.data.sellingPoints
import com.example.ui.theme.CyberAccent
import com.example.ui.theme.CyberAccentDark
import com.example.ui.theme.CyberBgCard
import com.example.ui.theme.CyberBgCardElevated
import com.example.ui.theme.CyberBgPrimary
import com.example.ui.theme.CyberSuccess
import com.example.ui.theme.CyberTextMuted
import com.example.ui.theme.CyberTextPrimary
import com.example.ui.theme.CyberTextSecondary

/**
 * Upgrade & paywall UI.
 *
 * Payment model: user taps "Request Upgrade" → a doc lands in the
 * `upgrade_requests` collection → the CoachOps admin panel reaches out
 * (WhatsApp/UPI), collects payment, and flips the plan on user_records —
 * which this app picks up in real time via EntitlementManager.
 */

private fun Context.findActivity(): Activity? {
    var c: Context? = this
    while (c is ContextWrapper) { if (c is Activity) return c; c = c.baseContext }
    return null
}

// ─── Coach / gym-owner plan upgrade ───────────────────────────────────────────

@Composable
fun CoachUpgradeScreen(
    userPreferences: UserPreferences,
    onBack: () -> Unit
) {
    val entitlements by EntitlementManager.entitlements.collectAsStateWithLifecycle()
    val currentPlan = entitlements.plan

    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val offers by BillingManager.offers.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { BillingManager.init(context) }

    var requesting by remember { mutableStateOf<SubscriptionPlan?>(null) }
    var requestedPlans by remember { mutableStateOf(setOf<SubscriptionPlan>()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberBgPrimary)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 8.dp)) {
            Box(
                modifier = Modifier.size(36.dp).clip(CircleShape).background(CyberBgCard).clickable { onBack() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back",
                    tint = CyberTextPrimary, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(12.dp))
            Text("Plans & Pricing", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = CyberTextPrimary)
        }

        Spacer(Modifier.height(8.dp))
        Text(
            "Grow your fitness business",
            fontSize = 24.sp, fontWeight = FontWeight.Black, color = CyberTextPrimary
        )
        Text(
            "Simple pricing in ₹. Upgrade or downgrade anytime — no lock-in.",
            fontSize = 13.sp, color = CyberTextMuted, lineHeight = 19.sp
        )
        Spacer(Modifier.height(20.dp))

        SubscriptionPlan.entries.forEach { plan ->
            val isCurrent = plan == currentPlan
            val isRequested = plan in requestedPlans
            val isUpgrade = plan.ordinal > currentPlan.ordinal
            val highlight = plan == SubscriptionPlan.BUSINESS

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(CyberBgCard)
                    .border(
                        width = if (highlight || isCurrent) 1.5.dp else 1.dp,
                        color = when {
                            isCurrent -> CyberSuccess.copy(alpha = 0.5f)
                            highlight -> CyberAccent.copy(alpha = 0.6f)
                            else      -> Color.White.copy(alpha = 0.07f)
                        },
                        shape = RoundedCornerShape(28.dp)
                    )
                    .padding(20.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(plan.emoji, fontSize = 22.sp)
                    Spacer(Modifier.width(10.dp))
                    Text(plan.displayName, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = CyberTextPrimary)
                    Spacer(Modifier.width(8.dp))
                    if (isCurrent) {
                        Box(
                            modifier = Modifier.clip(RoundedCornerShape(999.dp))
                                .background(CyberSuccess.copy(alpha = 0.15f))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text("CURRENT", fontSize = 10.sp, fontWeight = FontWeight.Black, color = CyberSuccess)
                        }
                    } else if (highlight) {
                        Box(
                            modifier = Modifier.clip(RoundedCornerShape(999.dp))
                                .background(CyberAccent.copy(alpha = 0.15f))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text("FOR GYMS", fontSize = 10.sp, fontWeight = FontWeight.Black, color = CyberAccent)
                        }
                    }
                    Spacer(Modifier.weight(1f))
                }
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        if (plan.priceMonthly == 0) "Free" else "₹${"%,d".format(plan.priceMonthly)}",
                        fontSize = 28.sp, fontWeight = FontWeight.Black, color = CyberTextPrimary
                    )
                    if (plan.priceMonthly > 0) {
                        Text("/month", fontSize = 13.sp, color = CyberTextMuted,
                            modifier = Modifier.padding(bottom = 4.dp, start = 2.dp))
                    }
                }
                Spacer(Modifier.height(14.dp))
                plan.sellingPoints().forEach { point ->
                    Row(modifier = Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("✓", fontSize = 13.sp, fontWeight = FontWeight.Black, color = CyberAccent)
                        Spacer(Modifier.width(10.dp))
                        Text(point, fontSize = 13.sp, color = CyberTextSecondary, lineHeight = 18.sp)
                    }
                }

                if (isUpgrade) {
                    Spacer(Modifier.height(16.dp))
                    val livePrice = offers[plan]?.formattedPrice
                    // Primary: subscribe through Google Play Billing
                    Button(
                        onClick = { if (activity != null) BillingManager.launchPurchase(activity, plan) },
                        enabled = activity != null,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (highlight) CyberAccent else CyberBgCardElevated
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Text(
                            if (livePrice != null) "Subscribe · $livePrice/mo" else "Subscribe to ${plan.displayName}",
                            fontSize = 14.sp, fontWeight = FontWeight.Bold,
                            color = if (highlight) CyberAccentDark else CyberTextPrimary
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    // Fallback: manual activation (UPI / bank) handled by the admin panel
                    Text(
                        when {
                            requesting == plan -> "Sending request…"
                            isRequested -> "✓ Request sent — we'll WhatsApp you"
                            else -> "Prefer UPI / bank transfer? Request manual activation"
                        },
                        fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        color = if (isRequested) CyberSuccess else CyberTextMuted,
                        modifier = Modifier.fillMaxWidth()
                            .clickable(enabled = !isRequested && requesting == null) {
                                requesting = plan
                                EntitlementManager.requestUpgrade(
                                    requestedTier = plan.name,
                                    userName = userPreferences.coachName,
                                    userPhone = userPreferences.coachPhone
                                ) { ok ->
                                    requesting = null
                                    if (ok) requestedPlans = requestedPlans + plan
                                }
                            }
                    )
                }
            }
        }

        Text(
            "Subscriptions are billed securely through Google Play and unlock instantly. " +
                "Prefer UPI or bank transfer? Use “Request manual activation” and our team will activate your plan within a few hours.",
            fontSize = 12.sp, color = CyberTextMuted, lineHeight = 18.sp,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        Spacer(Modifier.height(32.dp))
    }
}

// ─── Member premium paywall (client side) ─────────────────────────────────────

@Composable
fun MemberPremiumScreen(
    userPreferences: UserPreferences,
    featureName: String,
    onBack: () -> Unit
) {
    var requesting by remember { mutableStateOf(false) }
    var requested by remember { mutableStateOf(false) }

    // Member Premium price is admin-editable (admin_config/plans) — fetch it live
    // so a price change in the panel shows here without an app update.
    var priceMonthly by remember { mutableStateOf(199) }
    var priceYearly by remember { mutableStateOf(999) }
    LaunchedEffect(Unit) {
        com.google.firebase.firestore.FirebaseFirestore.getInstance()
            .collection("admin_config").document("plans").get()
            .addOnSuccessListener { doc ->
                doc.getLong("memberPremiumMonthly")?.let { priceMonthly = it.toInt() }
                doc.getLong("memberPremiumYearly")?.let { priceYearly = it.toInt() }
            }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberBgPrimary)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
        ) {
            Box(
                modifier = Modifier.size(36.dp).clip(CircleShape).background(CyberBgCard).clickable { onBack() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back",
                    tint = CyberTextPrimary, modifier = Modifier.size(18.dp))
            }
        }

        Spacer(Modifier.height(24.dp))
        Text("✨", fontSize = 52.sp)
        Spacer(Modifier.height(12.dp))
        Text("$featureName is a Premium feature", fontSize = 22.sp, fontWeight = FontWeight.Black,
            color = CyberTextPrimary, textAlign = TextAlign.Center, lineHeight = 30.sp)
        Spacer(Modifier.height(8.dp))
        Text(
            "Unlock the AI-powered side of your fitness journey",
            fontSize = 14.sp, color = CyberTextMuted, textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(28.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(CyberBgCard)
                .border(1.5.dp, CyberAccent.copy(alpha = 0.5f), RoundedCornerShape(28.dp))
                .padding(24.dp)
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text("₹$priceMonthly", fontSize = 34.sp, fontWeight = FontWeight.Black, color = CyberTextPrimary)
                Text("/month", fontSize = 14.sp, color = CyberTextMuted, modifier = Modifier.padding(bottom = 6.dp, start = 2.dp))
                Spacer(Modifier.weight(1f))
                Box(
                    modifier = Modifier.clip(RoundedCornerShape(999.dp))
                        .background(CyberAccent.copy(alpha = 0.15f))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text("₹$priceYearly/year", fontSize = 11.sp, fontWeight = FontWeight.Black, color = CyberAccent)
                }
            }
            Spacer(Modifier.height(16.dp))
            listOf(
                "🤖 AI Nutrition Coach — ask anything, anytime",
                "🍱 AI Meal Planner — weekly Indian meal plans",
                "🛒 Auto grocery lists from your meal plan",
                "📈 Advanced progress insights",
                "🎯 Priority feature access"
            ).forEach { line ->
                Text(line, fontSize = 14.sp, color = CyberTextSecondary, lineHeight = 26.sp)
            }
        }

        Spacer(Modifier.height(24.dp))
        Button(
            onClick = {
                requesting = true
                EntitlementManager.requestUpgrade(
                    requestedTier = "MEMBER_PREMIUM",
                    userName = userPreferences.clientName,
                    userPhone = ""
                ) { ok ->
                    requesting = false
                    if (ok) requested = true
                }
            },
            enabled = !requested && !requesting,
            colors = ButtonDefaults.buttonColors(
                containerColor = CyberAccent,
                disabledContainerColor = CyberSuccess.copy(alpha = 0.15f)
            ),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) {
            when {
                requesting -> CircularProgressIndicator(color = CyberAccentDark, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                requested  -> Text("✓ Request sent — we'll reach out shortly", fontWeight = FontWeight.Bold,
                    fontSize = 14.sp, color = CyberSuccess)
                else       -> Text("Get Premium", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = CyberAccentDark)
            }
        }
        Spacer(Modifier.height(10.dp))
        Text("Pay via UPI · Activated within hours · Cancel anytime", fontSize = 12.sp, color = CyberTextMuted)
        Spacer(Modifier.height(32.dp))
    }
}

// ─── Locked upsell card (embedded in coach screens for Starter tier) ─────────

@Composable
fun LockedFeatureCard(
    title: String,
    description: String,
    requiredPlanName: String,
    onUpgradeClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(CyberBgCard)
            .border(1.dp, CyberAccent.copy(alpha = 0.25f), RoundedCornerShape(28.dp))
            .clickable { onUpgradeClick() }
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("🔒", fontSize = 20.sp)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = CyberTextPrimary)
                Text(description, fontSize = 12.sp, color = CyberTextMuted, lineHeight = 17.sp)
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(CyberAccent)
                    .padding(horizontal = 12.dp, vertical = 7.dp)
            ) {
                Text(requiredPlanName, fontSize = 11.sp, fontWeight = FontWeight.Black, color = CyberAccentDark)
            }
        }
    }
}

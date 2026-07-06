package com.example.ui

// Permissions are requested via PermissionController.createRequestPermissionResultContract();
// no local permissions activity is declared (the intent targets the Health Connect app on
// Android 13-, and the platform itself on 14+). Manifest declares the health.READ_* permissions,
// the <queries> entry, PermissionsRationaleActivity and the ViewPermissionUsageActivity alias.

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.data.HealthConnectManager
import com.example.data.HealthSummary
import com.example.data.StepCounterManager
import com.example.ui.theme.CyberAccent
import com.example.ui.theme.CyberBgCard
import com.example.ui.theme.CyberBgCardElevated
import com.example.ui.theme.CyberBgPrimary
import com.example.ui.theme.CyberDanger
import com.example.ui.theme.CyberSuccess
import com.example.ui.theme.CyberTextMuted
import com.example.ui.theme.CyberTextPrimary
import com.example.ui.theme.CyberTextSecondary
import kotlinx.coroutines.launch

@Composable
fun HealthConnectScreen(onBack: () -> Unit, context: Context) {
    val manager = remember { HealthConnectManager(context) }
    val scope = rememberCoroutineScope()

    var isAvailable by remember { mutableStateOf(false) }
    var needsUpdate by remember { mutableStateOf(false) }
    var hasPermissions by remember { mutableStateOf(false) }
    var summary by remember { mutableStateOf<HealthSummary?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    suspend fun refresh() {
        isAvailable = manager.isAvailable()
        needsUpdate = manager.needsUpdate()
        if (isAvailable) {
            // "Connected" = at least one permission granted. An all-or-nothing
            // gate would dead-end users who granted only steps.
            hasPermissions = manager.hasAnyPermission()
            if (hasPermissions) {
                val s = manager.readTodaySummary()
                summary = s
                // Watch / Google Fit steps flow into the app's own counter
                // (max-merge — never decreases the local pedometer count)
                if (s.stepsToday > 0) {
                    StepCounterManager.getInstance(context).mergeExternal(s.stepsToday.toInt())
                }
            }
        }
        isLoading = false
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = PermissionController.createRequestPermissionResultContract()
    ) { _ ->
        scope.launch { refresh() }
    }

    // Re-check on every return to this screen — the user may have just
    // installed/updated Health Connect from the Play Store or granted
    // permissions in its settings
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) scope.launch { refresh() }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberBgPrimary)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(CyberBgCard)
                    .clickable { onBack() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = CyberTextPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column(Modifier.weight(1f)) {
                Text(
                    "Health Connect",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = CyberTextPrimary
                )
                Text(
                    "Sync fitness data from all your apps",
                    fontSize = 12.sp,
                    color = CyberTextMuted
                )
            }
        }

        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = CyberAccent, modifier = Modifier.size(40.dp))
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Spacer(Modifier.height(4.dp))

                // Status card
                when {
                    !isAvailable -> NotInstalledCard(context, needsUpdate)
                    !hasPermissions -> ConnectCard(
                        onConnect = {
                            permissionLauncher.launch(manager.permissions)
                        },
                        // Exit hatch: after two dialog cancels Android stops
                        // showing the request — without this link the Connect
                        // button is dead forever
                        onManage = { openHealthConnectSettings(context) }
                    )
                    else -> ConnectedStatusBadge()
                }

                // Health summary cards — only shown when connected
                if (isAvailable && hasPermissions) {
                    val s = summary
                    if (s != null) {
                        if (s.errored) {
                            ReadErrorCard(onRetry = { scope.launch { refresh() } })
                        } else {
                            // Each card only for the metric the user actually granted
                            if (s.hasStepsPermission) HealthStatCard(
                                icon = Icons.Filled.DirectionsWalk,
                                iconTint = CyberAccent,
                                label = "Steps Today",
                                value = "%,d".format(s.stepsToday),
                                unit = "steps"
                            )
                            if (s.hasCaloriesPermission) HealthStatCard(
                                icon = Icons.Filled.LocalFireDepartment,
                                iconTint = Color(0xFFFF7043),
                                label = "Calories Burned Today",
                                value = "%.0f".format(s.caloriesBurnedToday),
                                unit = "kcal"
                            )
                            if (s.hasWorkoutsPermission) HealthStatCard(
                                icon = Icons.Filled.FitnessCenter,
                                iconTint = CyberSuccess,
                                label = "Workouts This Week",
                                value = s.workoutsThisWeek.toString(),
                                unit = "sessions"
                            )
                        }
                        // Partial grant — offer the missing permissions
                        if (!s.hasStepsPermission || !s.hasCaloriesPermission || !s.hasWorkoutsPermission) {
                            PartialGrantCard(
                                onGrant = { permissionLauncher.launch(manager.permissions) },
                                onManage = { openHealthConnectSettings(context) }
                            )
                        }
                    }
                }

                // Source note
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(CyberBgCard)
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        Icons.Filled.Info,
                        contentDescription = null,
                        tint = CyberTextMuted,
                        modifier = Modifier.size(16.dp).padding(top = 1.dp)
                    )
                    Text(
                        "Syncs from Google Fit, Samsung Health, Garmin, Fitbit and all Health Connect apps",
                        fontSize = 12.sp,
                        color = CyberTextMuted,
                        lineHeight = 18.sp
                    )
                }

                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun NotInstalledCard(context: Context, needsUpdate: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CyberBgCard)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            if (needsUpdate) "Health Connect needs an update" else "Health Connect not installed",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = CyberTextPrimary
        )
        Text(
            if (needsUpdate)
                "Your Health Connect version is too old to sync. Update it from the Play Store, then come back here."
            else
                "Health Connect is required to sync your fitness data. Install it from the Play Store to get started.",
            fontSize = 13.sp,
            color = CyberTextSecondary,
            lineHeight = 20.sp
        )
        Button(
            onClick = {
                try {
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        data = Uri.parse("https://play.google.com/store/apps/details?id=com.google.android.apps.healthdata")
                        setPackage("com.android.vending")
                    }
                    context.startActivity(intent)
                } catch (_: Exception) {
                    // No Play Store (rare) — open in browser instead
                    try {
                        context.startActivity(Intent(Intent.ACTION_VIEW,
                            Uri.parse("https://play.google.com/store/apps/details?id=com.google.android.apps.healthdata")))
                    } catch (_: Exception) { }
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = CyberDanger),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                if (needsUpdate) "Update Health Connect" else "Install Health Connect",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
        }
    }
}

@Composable
private fun ConnectCard(onConnect: () -> Unit, onManage: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CyberBgCard)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "Connect Health Connect",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = CyberTextPrimary
        )
        Text(
            "Grant permissions to read your steps, calories, and workout sessions from Health Connect.",
            fontSize = 13.sp,
            color = CyberTextSecondary,
            lineHeight = 20.sp
        )
        Button(
            onClick = onConnect,
            colors = ButtonDefaults.buttonColors(containerColor = CyberAccent),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                "Connect",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF0A0A0A)
            )
        }
        Text(
            "Dialog not appearing? Manage in Health Connect →",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = CyberTextSecondary,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onManage() }
                .padding(vertical = 4.dp)
        )
    }
}

@Composable
private fun ReadErrorCard(onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CyberBgCard)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "Couldn't read health data",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = CyberTextPrimary
        )
        Text(
            "Health Connect didn't respond. This is usually temporary — try again.",
            fontSize = 13.sp,
            color = CyberTextSecondary,
            lineHeight = 20.sp
        )
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(containerColor = CyberAccent),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Retry", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF0A0A0A))
        }
    }
}

@Composable
private fun PartialGrantCard(onGrant: () -> Unit, onManage: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CyberBgCard)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            "Some data types aren't shared yet",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = CyberTextPrimary
        )
        Text(
            "Grant the remaining permissions to see all your stats here.",
            fontSize = 12.sp,
            color = CyberTextSecondary,
            lineHeight = 18.sp
        )
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(
                "Grant now",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = CyberAccent,
                modifier = Modifier.clickable { onGrant() }.padding(vertical = 4.dp)
            )
            Text(
                "Manage in Health Connect",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = CyberTextSecondary,
                modifier = Modifier.clickable { onManage() }.padding(vertical = 4.dp)
            )
        }
    }
}

/**
 * Deep-link to this app's own permission toggles in Health Connect.
 * The one recovery path when the system permission dialog stops appearing
 * (Android permanently suppresses it after repeated cancels).
 */
private fun openHealthConnectSettings(context: Context) {
    try {
        val intent = if (Build.VERSION.SDK_INT >= 34) {
            Intent("android.health.connect.action.MANAGE_HEALTH_PERMISSIONS")
                .putExtra(Intent.EXTRA_PACKAGE_NAME, context.packageName)
        } else {
            Intent(HealthConnectClient.ACTION_HEALTH_CONNECT_SETTINGS)
        }
        context.startActivity(intent)
    } catch (_: Exception) {
        // Some OEM builds don't resolve the per-app screen — fall back to the
        // general Health Connect settings page
        try {
            context.startActivity(Intent(HealthConnectClient.ACTION_HEALTH_CONNECT_SETTINGS))
        } catch (_: Exception) { }
    }
}

@Composable
private fun ConnectedStatusBadge() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CyberBgCard)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(CyberSuccess)
        )
        Text(
            "Health Connect connected",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = CyberSuccess
        )
    }
}

@Composable
private fun HealthStatCard(
    icon: ImageVector,
    iconTint: Color,
    label: String,
    value: String,
    unit: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CyberBgCard)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(CyberBgCardElevated),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(26.dp)
            )
        }
        Column(Modifier.weight(1f)) {
            Text(
                label,
                fontSize = 12.sp,
                color = CyberTextMuted
            )
            Spacer(Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    value,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = CyberTextPrimary
                )
                Text(
                    unit,
                    fontSize = 13.sp,
                    color = CyberTextSecondary,
                    modifier = Modifier.padding(bottom = 3.dp)
                )
            }
        }
    }
}

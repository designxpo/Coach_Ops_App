package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyberAccent
import com.example.ui.theme.CyberBgCard
import com.example.ui.theme.CyberBgPrimary
import com.example.ui.theme.CyberTextMuted
import com.example.ui.theme.CyberTextPrimary
import com.example.ui.theme.CyberTextSecondary
import com.example.ui.theme.MyApplicationTheme

/**
 * Shown by the system when the user taps "Why does this app need access?"
 * inside the Health Connect permission dialog. Declaring it (plus the
 * ViewPermissionUsageActivity alias) is MANDATORY — Android 14+ silently
 * denies health permission requests from apps without it.
 */
class PermissionsRationaleActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                RationaleContent(
                    onDone = { finish() },
                    onOpenPrivacyPolicy = {
                        try {
                            startActivity(android.content.Intent(
                                android.content.Intent.ACTION_VIEW,
                                android.net.Uri.parse(PRIVACY_POLICY_URL)
                            ))
                        } catch (_: Exception) { /* no browser — rare */ }
                    }
                )
            }
        }
    }

    companion object {
        /** Same policy entered in the Play Console Data Safety form. */
        const val PRIVACY_POLICY_URL = "https://coachops-27a73.web.app/privacy"
    }
}

@Composable
private fun RationaleContent(onDone: () -> Unit, onOpenPrivacyPolicy: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberBgPrimary)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "How ProCoach uses your health data",
            fontSize = 22.sp,
            fontWeight = FontWeight.ExtraBold,
            color = CyberTextPrimary
        )
        Text(
            "ProCoach India only READS the data below from Health Connect — " +
                "it never writes, edits or deletes anything there.",
            fontSize = 14.sp,
            color = CyberTextSecondary,
            lineHeight = 21.sp
        )

        RationaleItem(
            icon = Icons.Filled.DirectionsWalk,
            tint = CyberAccent,
            title = "Steps",
            body = "Shows your daily step count and progress towards your goal, " +
                "combining phone, watch and fitness-band data."
        )
        RationaleItem(
            icon = Icons.Filled.LocalFireDepartment,
            tint = Color(0xFFFF7043),
            title = "Calories burned",
            body = "Displays energy burned today alongside your food diary so you " +
                "see intake vs burn in one place."
        )
        RationaleItem(
            icon = Icons.Filled.FitnessCenter,
            tint = Color(0xFF66BB6A),
            title = "Workout sessions",
            body = "Counts workouts recorded by other apps towards your weekly " +
                "activity summary."
        )

        Text(
            "Your data stays in your account. It is never sold or shared with " +
                "third parties. You can revoke access at any time from the " +
                "Health Connect settings on your phone.",
            fontSize = 13.sp,
            color = CyberTextMuted,
            lineHeight = 20.sp
        )

        Text(
            "Read our full privacy policy →",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = CyberAccent,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onOpenPrivacyPolicy() }
                .padding(vertical = 6.dp)
        )

        Spacer(Modifier.height(4.dp))
        Button(
            onClick = onDone,
            colors = ButtonDefaults.buttonColors(containerColor = CyberAccent),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                "Got it",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF0A0A0A)
            )
        }
    }
}

@Composable
private fun RationaleItem(icon: ImageVector, tint: Color, title: String, body: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CyberBgCard)
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(24.dp))
        Column {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = CyberTextPrimary)
            Spacer(Modifier.height(2.dp))
            Text(body, fontSize = 13.sp, color = CyberTextSecondary, lineHeight = 19.sp)
        }
    }
}

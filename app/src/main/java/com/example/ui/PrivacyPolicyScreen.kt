package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@Composable
fun PrivacyPolicyScreen(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberBgPrimary)
            .statusBarsPadding()
    ) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = CyberTextPrimary)
            }
            Spacer(Modifier.width(4.dp))
            Text("Privacy Policy", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = CyberTextPrimary)
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text("Last updated: June 27, 2026", fontSize = 12.sp, color = CyberTextMuted)
                Spacer(Modifier.height(4.dp))
                Text(
                    "ProCoach India is committed to protecting your privacy. This policy explains how we collect, use, and safeguard your information.",
                    fontSize = 14.sp, color = CyberTextSecondary, lineHeight = 20.sp
                )
            }

            val sections = listOf(
                "1. Information We Collect" to "We collect the following types of information:\n\n• Personal information: name, email address, phone number\n• Health & fitness data: weight, height, body fat percentage, step count, menstrual cycle data, nutrition logs, workout logs, sleep data\n• Location data: used to find nearby coaches in the Discover section\n• Device information: push notification token (FCM)\n• Photos: profile photos and progress photos, stored securely in the cloud",

                "2. How We Use Your Information" to "We use your information to:\n\n• Provide and personalise coaching and fitness services\n• Enable communication between coaches and clients\n• Send push notifications for bookings, messages, and reminders\n• Analyse usage patterns to improve the app (Firebase Analytics)\n• Generate AI-powered food nutrition estimates (Gemini AI)\n• Detect and fix crashes (Firebase Crashlytics)",

                "3. Data Storage & Security" to "Your data is stored on:\n\n• Google Firebase (Firestore, Authentication, Cloud Storage) — servers in the US\n• Supabase — for profile and progress photos\n\nAll data is transmitted over HTTPS. We implement industry-standard security controls. Health and biometric data is treated as sensitive and access-controlled via Firebase Security Rules.",

                "4. Third-Party Services" to "We use the following third-party services, each governed by their own privacy policy:\n\n• Firebase / Google LLC — authentication, database, analytics, crash reporting, cloud storage\n• Supabase — media file storage\n• Google Play Services — location services\n• Gemini AI (Google) — food image recognition",

                "5. Health & Sensitive Data" to "ProCoach India collects sensitive health information including body measurements, menstrual cycle data, and nutrition logs. This data is:\n\n• Used solely to provide fitness coaching services\n• Never sold to third parties\n• Accessible only to you and your assigned coach\n• Deletable upon request\n\nBy using the health tracking features, you consent to the collection and processing of this sensitive data.",

                "6. Data Retention" to "We retain your personal data for as long as your account is active or as needed to provide services. If you delete your account, we will delete your personal data within 30 days, except where required to retain it by law.\n\nAnonymised, aggregated analytics data may be retained indefinitely.",

                "7. Your Rights" to "You have the right to:\n\n• Access your personal data stored in the app\n• Correct inaccurate data through the app settings\n• Delete your account and all associated data\n• Withdraw consent for health data processing\n• Request a copy of your data\n\nTo exercise these rights or request data deletion, contact us at: designxpoofficial@gmail.com",

                "8. Children's Privacy" to "ProCoach India is not intended for users under the age of 13. We do not knowingly collect personal information from children. If you believe a child has provided us with personal data, please contact us immediately.",

                "9. Changes to This Policy" to "We may update this Privacy Policy from time to time. We will notify you of significant changes through an in-app notification or by email. Continued use of the app after changes constitutes acceptance of the updated policy.",

                "10. Contact Us" to "If you have questions or concerns about this Privacy Policy or our data practices, please contact us:\n\nEmail: designxpoofficial@gmail.com\nApp: ProCoach India\nDeveloper: aistudio"
            )

            items(sections.size) { i ->
                val (title, body) = sections[i]
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(CyberBgCard)
                        .padding(16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = CyberAccent)
                        Text(body, fontSize = 13.sp, color = CyberTextSecondary, lineHeight = 20.sp)
                    }
                }
            }

            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}

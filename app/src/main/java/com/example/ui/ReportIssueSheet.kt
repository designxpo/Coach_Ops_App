package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.IssueReporter
import com.example.ui.theme.CyberAccent
import com.example.ui.theme.CyberBgCardElevated
import com.example.ui.theme.CyberBgPrimary
import com.example.ui.theme.CyberDanger
import com.example.ui.theme.CyberSuccess
import com.example.ui.theme.CyberTextMuted
import com.example.ui.theme.CyberTextPrimary
import com.example.ui.theme.CyberTextSecondary
import kotlinx.coroutines.launch

/**
 * "Report a Problem" — reachable from both the coach and member profile
 * screens. Sends the report (plus auto-attached diagnostics and the last
 * crash log) to the issue_reports collection, which the admin portal watches.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportIssueSheet(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var category by remember { mutableStateOf(IssueReporter.CATEGORIES.first()) }
    var message by remember { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }
    var sent by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = CyberBgPrimary,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (sent) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier.size(56.dp).clip(CircleShape).background(CyberSuccess.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Check, contentDescription = null, tint = CyberSuccess, modifier = Modifier.size(28.dp))
                    }
                    Text("Report sent — thank you!", fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = CyberTextPrimary)
                    Text(
                        "Our team sees it right away and will fix it as fast as possible.",
                        fontSize = 13.sp, color = CyberTextSecondary
                    )
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = CyberAccent),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Done", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF0A0A0A))
                    }
                }
            } else {
                Text("Report a Problem", fontSize = 19.sp, fontWeight = FontWeight.ExtraBold, color = CyberTextPrimary)
                Text(
                    "Tell us what went wrong. App version, device details and the last crash log are attached automatically.",
                    fontSize = 12.sp, color = CyberTextMuted, lineHeight = 18.sp
                )

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(IssueReporter.CATEGORIES) { c ->
                        val selected = c == category
                        Text(
                            c,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (selected) Color(0xFF0A0A0A) else CyberTextSecondary,
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(if (selected) CyberAccent else CyberBgCardElevated)
                                .clickable { category = c }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }
                }

                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    placeholder = { Text("What happened? What did you expect?", color = CyberTextMuted, fontSize = 13.sp) },
                    minLines = 4,
                    maxLines = 8,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = CyberTextPrimary,
                        unfocusedTextColor = CyberTextPrimary,
                        focusedBorderColor = CyberAccent,
                        unfocusedBorderColor = CyberBgCardElevated,
                        cursorColor = CyberAccent,
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                if (error.isNotEmpty()) {
                    Text(error, fontSize = 12.sp, color = CyberDanger)
                }

                Button(
                    onClick = {
                        if (message.trim().length < 10) {
                            error = "Please describe the problem in a few words (at least 10 characters)."
                            return@Button
                        }
                        error = ""
                        isSending = true
                        scope.launch {
                            try {
                                IssueReporter.submit(context, category, message)
                                sent = true
                            } catch (e: Exception) {
                                error = e.message ?: "Couldn't send — check your connection and try again."
                            } finally {
                                isSending = false
                            }
                        }
                    },
                    enabled = !isSending,
                    colors = ButtonDefaults.buttonColors(containerColor = CyberAccent),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isSending) {
                        CircularProgressIndicator(color = Color(0xFF0A0A0A), modifier = Modifier.size(18.dp))
                    } else {
                        Text("Send report", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF0A0A0A))
                    }
                }
                Spacer(Modifier.height(4.dp))
            }
        }
    }
}

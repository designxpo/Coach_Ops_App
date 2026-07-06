package com.example.ui

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.AccountDeletion
import com.example.ui.theme.CyberBgCard
import com.example.ui.theme.CyberDanger
import com.example.ui.theme.CyberTextPrimary
import com.example.ui.theme.CyberTextSecondary
import kotlinx.coroutines.launch

/**
 * Permanent account deletion confirmation — Google Play requires a working
 * in-app deletion path. On success the auth session is already gone;
 * onDeleted must clear local state and return to login (the logout flow).
 */
@Composable
fun DeleteAccountDialog(onDismiss: () -> Unit, onDeleted: () -> Unit) {
    val scope = rememberCoroutineScope()
    var isDeleting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = { if (!isDeleting) onDismiss() },
        containerColor = CyberBgCard,
        title = { Text("Delete your account?", fontWeight = FontWeight.Bold, color = CyberTextPrimary) },
        text = {
            Text(
                if (error.isNotEmpty()) error
                else "This permanently deletes your account and all your data — clients, programs, " +
                    "payments, gym records, health tracking and food diary. This cannot be undone.",
                color = if (error.isNotEmpty()) CyberDanger else CyberTextSecondary
            )
        },
        confirmButton = {
            TextButton(
                enabled = !isDeleting,
                onClick = {
                    error = ""
                    isDeleting = true
                    scope.launch {
                        val result = AccountDeletion.deleteAccount()
                        isDeleting = false
                        result.fold(
                            onSuccess = { onDeleted() },
                            onFailure = { error = it.message ?: "Something went wrong — try again." }
                        )
                    }
                }
            ) {
                if (isDeleting) CircularProgressIndicator(color = CyberDanger, modifier = Modifier.size(18.dp))
                else Text("Delete forever", color = CyberDanger, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(enabled = !isDeleting, onClick = onDismiss) {
                Text("Cancel", color = CyberTextSecondary)
            }
        }
    )
}

package com.example.ui

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AuthRepository
import com.example.data.AuthResult
import com.example.data.UserPreferences
import com.example.ui.theme.CyberAccent
import com.example.ui.theme.CyberAccentDark
import com.example.ui.theme.CyberBgCard
import com.example.ui.theme.CyberBgCardElevated
import com.example.ui.theme.CyberBgPrimary
import com.example.ui.theme.CyberDanger
import com.example.ui.theme.CyberTextMuted
import com.example.ui.theme.CyberTextPrimary
import kotlinx.coroutines.launch

@Composable
fun RegisterScreen(
    userPreferences: UserPreferences,
    onRegisterSuccess: (isNewUser: Boolean) -> Unit,
    onNavigateToLogin: () -> Unit
) {
    var selectedRole by remember { mutableStateOf(userPreferences.userRole.ifEmpty { "" }) }

    if (selectedRole.isEmpty()) {
        RolePickerScreen(
            onRoleSelected = { role ->
                selectedRole = role
                userPreferences.userRole = role
            },
            onNavigateToLogin = onNavigateToLogin
        )
        return
    }

    val context = LocalContext.current
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var isGoogleLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val googleLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            isGoogleLoading = true
            scope.launch {
                try {
                    val account = AuthRepository.handleGoogleSignInIntent(result.data).getResult(Exception::class.java)
                    val idToken = account.idToken ?: run {
                        errorMessage = "Google sign-in failed. Try again."
                        isGoogleLoading = false
                        return@launch
                    }
                    when (val authResult = AuthRepository.signInWithGoogle(idToken)) {
                        is AuthResult.Success -> {
                            val uid = authResult.user.uid
                            val displayName = authResult.user.displayName ?: ""
                            if (selectedRole == "client") {
                                if (userPreferences.clientName.isEmpty()) userPreferences.clientName = displayName
                                // Write role + profile to Firestore — prevents login role-bypass
                                com.example.data.FirestoreSync.registerClientRecord(
                                    displayName.ifEmpty { userPreferences.clientName },
                                    authResult.user.email ?: ""
                                )
                            } else {
                                if (userPreferences.coachName.isEmpty()) userPreferences.coachName = displayName
                                // Write role to Firestore — prevents login role-bypass
                                com.example.data.FirestoreSync.setUserRole(selectedRole.ifEmpty { "coach" })
                            }
                            userPreferences.coachEmail = authResult.user.email ?: ""
                            userPreferences.userRole = selectedRole
                            onRegisterSuccess(authResult.isNewUser)
                        }
                        is AuthResult.Error -> { errorMessage = authResult.message; isGoogleLoading = false }
                    }
                } catch (e: Exception) {
                    errorMessage = "Google sign-in failed. Try again."
                    isGoogleLoading = false
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberBgPrimary)
            .statusBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(48.dp))

        Column(modifier = Modifier.fillMaxWidth()) {
            Text("Create Account", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = CyberTextPrimary)
            Spacer(Modifier.height(4.dp))
            Text(
                when (selectedRole) {
                    "client"    -> "Find your perfect trainer"
                    "gym_owner" -> "Digitize your gym operations"
                    else        -> "Set up your coaching profile"
                },
                fontSize = 14.sp, color = CyberTextMuted
            )
        }

        Spacer(Modifier.height(28.dp))

        // Google Sign-Up button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(CyberBgCard)
                .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
                .clickable(enabled = !isLoading && !isGoogleLoading) {
                    val client = AuthRepository.getGoogleSignInClient(context)
                    client.signOut().addOnCompleteListener {
                        googleLauncher.launch(client.signInIntent)
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            if (isGoogleLoading) {
                CircularProgressIndicator(color = CyberAccent, modifier = Modifier.size(22.dp), strokeWidth = 2.5.dp)
            } else {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    GoogleLogo()
                    Text("Continue with Google", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = CyberTextPrimary)
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // Divider
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f).height(1.dp).background(Color.White.copy(alpha = 0.08f)))
            Text("  or sign up with email  ", fontSize = 12.sp, color = CyberTextMuted)
            Box(Modifier.weight(1f).height(1.dp).background(Color.White.copy(alpha = 0.08f)))
        }

        Spacer(Modifier.height(20.dp))

        AuthTextField(value = name, onValueChange = { name = it; errorMessage = "" },
            label = "Full Name *", keyboardType = KeyboardType.Text, imeAction = ImeAction.Next,
            enabled = !isLoading && !isGoogleLoading)
        Spacer(Modifier.height(12.dp))
        AuthTextField(value = email, onValueChange = { email = it; errorMessage = "" },
            label = "Email address *", keyboardType = KeyboardType.Email, imeAction = ImeAction.Next,
            enabled = !isLoading && !isGoogleLoading)
        Spacer(Modifier.height(12.dp))
        AuthTextField(value = phone, onValueChange = { phone = it; errorMessage = "" },
            label = "Phone number (optional)", keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next,
            enabled = !isLoading && !isGoogleLoading)
        Spacer(Modifier.height(12.dp))
        AuthTextField(
            value = password, onValueChange = { password = it; errorMessage = "" },
            label = "Password *", keyboardType = KeyboardType.Password, imeAction = ImeAction.Next,
            enabled = !isLoading && !isGoogleLoading,
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                        contentDescription = null, tint = CyberTextMuted, modifier = Modifier.size(18.dp))
                }
            }
        )
        Spacer(Modifier.height(12.dp))
        AuthTextField(
            value = confirmPassword, onValueChange = { confirmPassword = it; errorMessage = "" },
            label = "Confirm Password *", keyboardType = KeyboardType.Password, imeAction = ImeAction.Done,
            enabled = !isLoading && !isGoogleLoading,
            visualTransformation = if (confirmVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { confirmVisible = !confirmVisible }) {
                    Icon(if (confirmVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                        contentDescription = null, tint = CyberTextMuted, modifier = Modifier.size(18.dp))
                }
            }
        )

        if (errorMessage.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            Text(errorMessage, fontSize = 13.sp, color = CyberDanger, modifier = Modifier.align(Alignment.Start))
        }

        Spacer(Modifier.height(28.dp))

        val canRegister = name.isNotBlank() && email.isNotBlank() && password.isNotBlank() && confirmPassword.isNotBlank()
        Box(
            modifier = Modifier
                .fillMaxWidth().height(56.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(when {
                    isLoading     -> CyberAccent.copy(alpha = 0.6f)
                    canRegister   -> CyberAccent
                    else          -> CyberBgCardElevated
                })
                .clickable(enabled = canRegister && !isLoading && !isGoogleLoading) {
                    when {
                        !email.contains("@")        -> { errorMessage = "Please enter a valid email"; return@clickable }
                        password.length < 6         -> { errorMessage = "Password must be at least 6 characters"; return@clickable }
                        password != confirmPassword -> { errorMessage = "Passwords don't match"; return@clickable }
                    }
                    isLoading = true
                    scope.launch {
                        when (val result = AuthRepository.register(email, password)) {
                            is AuthResult.Success -> {
                                val uid = result.user.uid
                                if (selectedRole == "client") {
                                    userPreferences.clientName = name.trim()
                                    // Write role + profile to Firestore — prevents login role-bypass
                                    com.example.data.FirestoreSync.registerClientRecord(
                                        name.trim(), result.user.email ?: email.trim()
                                    )
                                } else {
                                    userPreferences.coachName = name.trim()
                                    // Write role to Firestore — prevents login role-bypass
                                    com.example.data.FirestoreSync.setUserRole(selectedRole.ifEmpty { "coach" })
                                }
                                userPreferences.coachEmail = result.user.email ?: email.trim()
                                // coachPhone is the shared phone field; no separate clientPhone field exists in UserPreferences
                                userPreferences.coachPhone = phone.trim()
                                if (selectedRole == "client" && phone.isNotBlank()) {
                                    com.example.data.FirestoreSync.savePhoneIndex(phone.trim(), name.trim())
                                }
                                isLoading = false
                                onRegisterSuccess(true)
                            }
                            is AuthResult.Error -> { errorMessage = result.message; isLoading = false }
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = CyberAccentDark, modifier = Modifier.size(22.dp), strokeWidth = 2.5.dp)
            } else {
                Text("Create Account", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold,
                    color = if (canRegister) CyberAccentDark else CyberTextMuted)
            }
        }

        Spacer(Modifier.height(20.dp))
        Row {
            Text("Already have an account? ", fontSize = 14.sp, color = CyberTextMuted)
            Text("Sign In", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = CyberAccent,
                modifier = Modifier.clickable(enabled = !isLoading && !isGoogleLoading) { onNavigateToLogin() })
        }
        Spacer(Modifier.height(40.dp))
    }
}

// ─── Role Picker ─────────────────────────────────────────────────────────────

@Composable
fun RolePickerScreen(
    onRoleSelected: (String) -> Unit,
    onNavigateToLogin: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberBgPrimary)
            .statusBarsPadding()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(60.dp))

        Text("Join ProCoach India", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = CyberTextPrimary)
        Spacer(Modifier.height(8.dp))
        Text("Who are you?", fontSize = 15.sp, color = CyberTextMuted)

        Spacer(Modifier.height(48.dp))

        RoleCard(
            emoji = "🏋️",
            title = "I'm a Coach",
            subtitle = "Manage members, programs and grow your fitness business",
            onClick = { onRoleSelected("coach") }
        )

        Spacer(Modifier.height(16.dp))

        RoleCard(
            emoji = "🏢",
            title = "I Own a Gym",
            subtitle = "Manage members, fees, attendance & billing — 30-day free trial",
            onClick = { onRoleSelected("gym_owner") }
        )

        Spacer(Modifier.height(16.dp))

        RoleCard(
            emoji = "🔍",
            title = "I'm Looking for a Trainer",
            subtitle = "Find certified coaches near you and book a session",
            onClick = { onRoleSelected("client") }
        )

        Spacer(Modifier.weight(1f))

        Row(modifier = Modifier.padding(bottom = 32.dp)) {
            Text("Already have an account? ", fontSize = 14.sp, color = CyberTextMuted)
            Text("Sign In", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = CyberAccent,
                modifier = Modifier.clickable { onNavigateToLogin() })
        }
    }
}

@Composable
fun RoleCard(emoji: String, title: String, subtitle: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(CyberBgCard)
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(24.dp))
            .clickable { onClick() }
            .padding(24.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
                    .background(CyberAccent.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Text(emoji, fontSize = 26.sp)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = CyberTextPrimary)
                Spacer(Modifier.height(4.dp))
                Text(subtitle, fontSize = 13.sp, color = CyberTextMuted, lineHeight = 18.sp)
            }
            Text("›", fontSize = 22.sp, color = CyberAccent, fontWeight = FontWeight.Bold)
        }
    }
}

package com.example.ui

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.PersonSearch
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.example.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AuthRepository
import com.example.data.AuthResult
import com.example.data.ProfileSync
import com.example.data.UserPreferences
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardActions
import com.example.ui.theme.CyberAccent
import com.example.ui.theme.CyberAccentDark
import com.example.ui.theme.CyberBgCard
import com.example.ui.theme.CyberBgCardElevated
import com.example.ui.theme.CyberBgPrimary
import com.example.ui.theme.CyberDanger
import com.example.ui.theme.CyberSuccess
import com.example.ui.theme.CyberTextMuted
import com.example.ui.theme.CyberTextPrimary
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    userPreferences: UserPreferences,
    viewModel: MainViewModel,
    onLoginSuccess: () -> Unit,
    onNavigateToRegister: () -> Unit
) {
    val context = LocalContext.current
    var selectedRole by remember { mutableStateOf(userPreferences.userRole.ifEmpty { "coach" }) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var isGoogleLoading by remember { mutableStateOf(false) }
    var showForgotPassword by remember { mutableStateOf(false) }
    var resetEmail by remember { mutableStateOf("") }
    var resetSent by remember { mutableStateOf(false) }
    var resetError by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    val googleLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            isGoogleLoading = true
            scope.launch {
                try {
                    val account = AuthRepository.handleGoogleSignInIntent(result.data).getResult(Exception::class.java)
                    when (val authResult = AuthRepository.signInWithGoogle(account.idToken!!)) {
                        is AuthResult.Success -> {
                            val uid = authResult.user.uid
                            // Bind first — clears prefs if different user on same device
                            userPreferences.bindToUser(uid)
                            // Restore onboarding state from Firestore (handles new device logins)
                            com.example.data.FirestoreSync.restoreOnboardingIfNeeded(uid, userPreferences)
                            // Validate stored role
                            val storedRole = com.example.data.FirestoreSync.getUserRole(uid)
                            if (storedRole != null && storedRole != selectedRole) {
                                com.google.firebase.auth.FirebaseAuth.getInstance().signOut()
                                errorMessage = if (storedRole == "client")
                                    "This is a Member account. Please select \"I'm a Member\" to sign in."
                                else
                                    "This is a Coach account. Please select \"I'm a Coach\" to sign in."
                                isGoogleLoading = false
                            } else {
                                val role = storedRole ?: selectedRole
                                userPreferences.userRole = role
                                if (userPreferences.coachName.isEmpty()) {
                                    userPreferences.coachName = authResult.user.displayName ?: ""
                                }
                                userPreferences.coachEmail = authResult.user.email ?: ""
                                if (storedRole != null || !authResult.isNewUser) {
                                    userPreferences.onboardingComplete = true
                                }
                                // Restore full profile from Firestore so any device gets all data
                                ProfileSync.restoreProfile(uid, role, userPreferences)
                                viewModel.refreshProfileFromPrefs()
                                isGoogleLoading = false
                                onLoginSuccess()
                            }
                        }
                        is AuthResult.Error -> {
                            errorMessage = authResult.message
                            isGoogleLoading = false
                        }
                    }
                } catch (e: Exception) {
                    errorMessage = "Google sign-in failed. Try again."
                    isGoogleLoading = false
                }
            }
        }
    }

    if (showForgotPassword) {
        ForgotPasswordDialog(
            email = resetEmail,
            onEmailChange = { resetEmail = it; resetSent = false; resetError = "" },
            sent = resetSent,
            resetError = resetError,
            onSend = {
                scope.launch {
                    try {
                        AuthRepository.sendPasswordReset(resetEmail)
                        resetSent = true
                        resetError = ""
                    } catch (e: Exception) {
                        resetError = e.message ?: "Failed to send reset link. Try again."
                    }
                }
            },
            onDismiss = { showForgotPassword = false; resetEmail = ""; resetSent = false; resetError = "" }
        )
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
        Spacer(Modifier.height(32.dp))

        Box(
            modifier = Modifier
                .size(88.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color.Black)
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(R.mipmap.ic_launcher_foreground),
                contentDescription = "ProCoach India",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        }

        Spacer(Modifier.height(20.dp))
        Row {
            Text("ProCoach ", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = CyberTextPrimary)
            Text("India", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = CyberAccent)
        }
        Spacer(Modifier.height(4.dp))
        Text("Sign in to your account", fontSize = 14.sp, color = CyberTextMuted)

        Spacer(Modifier.height(28.dp))

        // Role selector — pill toggle
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(CyberBgCard)
                .border(1.dp, Color.White.copy(0.08f), RoundedCornerShape(16.dp))
                .padding(6.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth()) {
                RoleToggleOption(
                    icon = Icons.Filled.FitnessCenter,
                    title = "Coach",
                    subtitle = "Manage members",
                    selected = selectedRole == "coach",
                    modifier = Modifier.weight(1f),
                    onClick = { selectedRole = "coach" }
                )
                RoleToggleOption(
                    icon = Icons.Filled.PersonSearch,
                    title = "Member",
                    subtitle = "Find a trainer",
                    selected = selectedRole == "client",
                    modifier = Modifier.weight(1f),
                    onClick = { selectedRole = "client" }
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        // Google Sign-In button
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
            Text("  or sign in with email  ", fontSize = 12.sp, color = CyberTextMuted)
            Box(Modifier.weight(1f).height(1.dp).background(Color.White.copy(alpha = 0.08f)))
        }

        Spacer(Modifier.height(20.dp))

        AuthTextField(
            value = email,
            onValueChange = { email = it; errorMessage = "" },
            label = "Email address",
            keyboardType = KeyboardType.Email,
            imeAction = ImeAction.Next,
            enabled = !isLoading && !isGoogleLoading
        )

        Spacer(Modifier.height(12.dp))

        AuthTextField(
            value = password,
            onValueChange = { password = it; errorMessage = "" },
            label = "Password",
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Done,
            keyboardActions = KeyboardActions(onDone = {
                if (email.isNotBlank() && password.isNotBlank() && !isLoading && !isGoogleLoading) {
                    isLoading = true
                    scope.launch {
                        when (val result = AuthRepository.signIn(email, password)) {
                            is AuthResult.Success -> {
                                val uid = result.user.uid
                                userPreferences.bindToUser(uid)
                                com.example.data.FirestoreSync.restoreOnboardingIfNeeded(uid, userPreferences)
                                val storedRole = com.example.data.FirestoreSync.getUserRole(uid)
                                if (storedRole != null && storedRole != selectedRole) {
                                    com.google.firebase.auth.FirebaseAuth.getInstance().signOut()
                                    errorMessage = if (storedRole == "client")
                                        "This is a Member account. Please select \"I'm a Member\" to sign in."
                                    else
                                        "This is a Coach account. Please select \"I'm a Coach\" to sign in."
                                    isLoading = false
                                } else {
                                    val role = storedRole ?: selectedRole
                                    userPreferences.coachEmail = result.user.email ?: email
                                    userPreferences.userRole = role
                                    userPreferences.onboardingComplete = storedRole != null
                                    ProfileSync.restoreProfile(uid, role, userPreferences)
                                    isLoading = false
                                    onLoginSuccess()
                                }
                            }
                            is AuthResult.Error -> { errorMessage = result.message; isLoading = false }
                        }
                    }
                }
            }),
            enabled = !isLoading && !isGoogleLoading,
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                        contentDescription = null, tint = CyberTextMuted, modifier = Modifier.size(18.dp)
                    )
                }
            }
        )

        Spacer(Modifier.height(8.dp))
        Text(
            "Forgot password?",
            fontSize = 13.sp, color = CyberAccent, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.align(Alignment.End).padding(vertical = 8.dp).clickable { showForgotPassword = true }
        )

        if (errorMessage.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            Text(errorMessage, fontSize = 13.sp, color = CyberDanger, modifier = Modifier.align(Alignment.Start))
        }

        Spacer(Modifier.height(24.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(if (isLoading) CyberAccent.copy(alpha = 0.6f) else CyberAccent)
                .clickable(enabled = !isLoading && !isGoogleLoading) {
                    if (email.isBlank() || password.isBlank()) { errorMessage = "Please fill in all fields"; return@clickable }
                    isLoading = true
                    scope.launch {
                        when (val result = AuthRepository.signIn(email, password)) {
                            is AuthResult.Success -> {
                                val uid = result.user.uid
                                // 1. Bind first (clears prefs if different user on same device)
                                userPreferences.bindToUser(uid)
                                // 2. Restore onboarding state from Firestore (handles new device logins)
                                com.example.data.FirestoreSync.restoreOnboardingIfNeeded(uid, userPreferences)
                                // 3. Validate role — fetch stored role from Firestore
                                val storedRole = com.example.data.FirestoreSync.getUserRole(uid)
                                if (storedRole != null && storedRole != selectedRole) {
                                    // Wrong role selected — sign out and show a clear error
                                    com.google.firebase.auth.FirebaseAuth.getInstance().signOut()
                                    errorMessage = if (storedRole == "client")
                                        "This is a Member account. Please select \"I'm a Member\" to sign in."
                                    else
                                        "This is a Coach account. Please select \"I'm a Coach\" to sign in."
                                    isLoading = false
                                } else {
                                    // 3. Role is valid — restore full profile from Firestore
                                    val role = storedRole ?: selectedRole
                                    userPreferences.coachEmail = result.user.email ?: email
                                    userPreferences.userRole = role
                                    userPreferences.onboardingComplete = storedRole != null
                                    // Pull all profile data (name, specialty, health etc.) from cloud
                                    ProfileSync.restoreProfile(uid, role, userPreferences)
                                    isLoading = false
                                    onLoginSuccess()
                                }
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
                Text("Sign In", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = CyberAccentDark)
            }
        }

        Spacer(Modifier.height(28.dp))

        // Divider before sign-up section
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f).height(1.dp).background(Color.White.copy(alpha = 0.08f)))
            Text("  New here? Create an account  ", fontSize = 12.sp, color = CyberTextMuted)
            Box(Modifier.weight(1f).height(1.dp).background(Color.White.copy(alpha = 0.08f)))
        }

        Spacer(Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SignUpCard(
                icon = Icons.Filled.FitnessCenter,
                label = "Coach",
                sublabel = "Manage members",
                modifier = Modifier.weight(1f),
                enabled = !isLoading && !isGoogleLoading,
                isCoach = true,
                onClick = {
                    userPreferences.userRole = "coach"
                    onNavigateToRegister()
                }
            )
            SignUpCard(
                icon = Icons.Filled.PersonSearch,
                label = "Member",
                sublabel = "Find a trainer",
                modifier = Modifier.weight(1f),
                enabled = !isLoading && !isGoogleLoading,
                isCoach = false,
                onClick = {
                    userPreferences.userRole = "client"
                    onNavigateToRegister()
                }
            )
        }

        Spacer(Modifier.height(24.dp))
        Text("v1.0 · Built for coaches", fontSize = 11.sp, color = CyberTextMuted, modifier = Modifier.padding(bottom = 32.dp))
    }
}

@Composable
fun GoogleLogo() {
    Image(
        painter = painterResource(R.drawable.ic_google),
        contentDescription = null,
        modifier = Modifier.size(20.dp)
    )
}

@Composable
private fun ForgotPasswordDialog(
    email: String,
    onEmailChange: (String) -> Unit,
    sent: Boolean,
    resetError: String = "",
    onSend: () -> Unit,
    onDismiss: () -> Unit
) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(24.dp))
                .background(CyberBgCard)
                .padding(24.dp)
        ) {
            Text("Reset Password", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = CyberTextPrimary)
            Spacer(Modifier.height(8.dp))
            Text("Enter your email to receive a reset link.", fontSize = 13.sp, color = CyberTextMuted)
            Spacer(Modifier.height(16.dp))
            AuthTextField(value = email, onValueChange = onEmailChange, label = "Email address", keyboardType = KeyboardType.Email)
            if (sent) {
                Spacer(Modifier.height(10.dp))
                Text("Reset link sent! Check your inbox.", fontSize = 13.sp, color = CyberSuccess)
            }
            if (resetError.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                Text(resetError, fontSize = 13.sp, color = CyberDanger)
            }
            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier.weight(1f).height(48.dp).clip(RoundedCornerShape(12.dp))
                        .background(CyberAccent.copy(alpha = 0.12f)).clickable { onDismiss() },
                    contentAlignment = Alignment.Center
                ) { Text("Cancel", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = CyberAccent) }
                Box(
                    modifier = Modifier.weight(1f).height(48.dp).clip(RoundedCornerShape(12.dp))
                        .background(CyberAccent).clickable(enabled = email.isNotBlank() && !sent) { onSend() },
                    contentAlignment = Alignment.Center
                ) { Text(if (sent) "Sent!" else "Send Link", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = CyberAccentDark) }
            }
        }
    }
}

@Composable
private fun SignUpCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    sublabel: String,
    modifier: Modifier,
    enabled: Boolean,
    isCoach: Boolean,
    onClick: () -> Unit
) {
    val accent = if (isCoach) Color(0xFF6366F1) else CyberAccent
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(accent.copy(alpha = 0.10f))
            .border(1.5.dp, accent.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
            .clickable(enabled = enabled) { onClick() }
            .padding(vertical = 18.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .background(accent.copy(0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = accent, modifier = Modifier.size(18.dp))
            }
            Text(label, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = CyberTextPrimary)
            Text(sublabel, fontSize = 10.sp, color = CyberTextMuted)
        }
    }
}

@Composable
private fun RoleToggleOption(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    selected: Boolean,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) CyberAccent else Color.Transparent)
            .clickable { onClick() }
            .padding(vertical = 12.dp, horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                icon, null,
                tint = if (selected) CyberAccentDark else CyberTextMuted,
                modifier = Modifier.size(18.dp)
            )
            Column {
                Text(
                    title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (selected) CyberAccentDark else CyberTextPrimary
                )
                Text(
                    subtitle,
                    fontSize = 10.sp,
                    color = if (selected) CyberAccentDark.copy(0.7f) else CyberTextMuted
                )
            }
        }
    }
}

@Composable
fun AuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    enabled: Boolean = true,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingIcon: (@Composable () -> Unit)? = null
) {
    OutlinedTextField(
        value = value, onValueChange = onValueChange,
        label = { Text(label, fontSize = 14.sp) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true, enabled = enabled,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
        keyboardActions = keyboardActions,
        visualTransformation = visualTransformation,
        trailingIcon = trailingIcon,
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = CyberAccent,
            unfocusedBorderColor = Color.White.copy(alpha = 0.12f),
            focusedTextColor = CyberTextPrimary, unfocusedTextColor = CyberTextPrimary,
            disabledTextColor = CyberTextMuted, cursorColor = CyberAccent,
            focusedLabelColor = CyberAccent, unfocusedLabelColor = CyberTextMuted,
            focusedContainerColor = CyberBgCard, unfocusedContainerColor = CyberBgCard,
            disabledContainerColor = CyberBgCard,
        )
    )
}

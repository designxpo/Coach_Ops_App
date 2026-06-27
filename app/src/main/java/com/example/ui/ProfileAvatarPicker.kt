package com.example.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.util.Log
import coil.compose.AsyncImage
import com.example.data.SupabaseStorage
import com.example.data.UserPreferences
import com.example.ui.theme.CyberAccent
import com.example.ui.theme.CyberAccentDark
import com.example.ui.theme.CyberBgCard
import com.example.ui.theme.CyberDanger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ProfileAvatarPicker(
    photoUrl: String,
    initials: String,
    size: Dp = 90.dp,
    borderColor: Color = CyberAccent,
    initialsColor: Color = CyberAccent,
    initialsBg: Color = CyberAccent.copy(0.12f),
    userPreferences: UserPreferences,
    onPhotoUploaded: (url: String) -> Unit
) {
    var isUploading by remember { mutableStateOf(false) }
    var errorMsg    by remember { mutableStateOf("") }
    val scope       = rememberCoroutineScope()
    val context     = LocalContext.current

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        errorMsg = ""
        scope.launch {
            isUploading = true
            try {
                val url = withContext(Dispatchers.IO) { SupabaseStorage.uploadProfilePhoto(context, uri) }
                userPreferences.profilePhotoUrl = url
                onPhotoUploaded(url)
            } catch (e: Exception) {
                Log.e("ProCoach", "Profile photo upload failed: ${e.message}", e)
                errorMsg = e.message ?: "Upload failed — please try again"
            } finally {
                isUploading = false
            }
        }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(contentAlignment = Alignment.BottomEnd) {
            // Main avatar circle
            Box(
                modifier = Modifier
                    .size(size)
                    .clip(CircleShape)
                    .border(2.dp, if (errorMsg.isNotEmpty()) CyberDanger else borderColor.copy(0.35f), CircleShape)
                    .background(initialsBg)
                    .clickable(enabled = !isUploading) {
                        errorMsg = ""
                        launcher.launch("image/*")
                    },
                contentAlignment = Alignment.Center
            ) {
                when {
                    isUploading -> CircularProgressIndicator(
                        color = borderColor,
                        modifier = Modifier.size(size * 0.4f),
                        strokeWidth = 2.5.dp
                    )
                    photoUrl.isNotBlank() -> AsyncImage(
                        model = photoUrl,
                        contentDescription = "Profile photo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(size).clip(CircleShape)
                    )
                    else -> Text(
                        initials.take(1).uppercase().ifEmpty { "?" },
                        fontSize = (size.value * 0.38f).sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = initialsColor
                    )
                }
            }

            // Camera badge
            if (!isUploading) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .offset(x = 2.dp, y = 2.dp)
                        .clip(CircleShape)
                        .background(if (errorMsg.isNotEmpty()) CyberDanger else CyberAccent)
                        .border(2.dp, CyberBgCard, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.CameraAlt,
                        contentDescription = "Change photo",
                        tint = CyberAccentDark,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }

        // Error message below avatar — no fixed height so it never gets clipped
        if (errorMsg.isNotEmpty()) {
            Text(
                text = errorMsg,
                fontSize = 11.sp,
                color = CyberDanger,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(top = 6.dp, start = 16.dp, end = 16.dp)
            )
        }

        // Upload status
        if (isUploading) {
            Text(
                "Uploading…",
                fontSize = 11.sp,
                color = CyberAccent,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
    }
}

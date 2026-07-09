package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.UserPreferences
import com.example.ui.theme.CyberAccent

/**
 * The one profile avatar used across every screen (coach + member).
 *
 * UX contract:
 *  - Shows the user's photo when set, else their initial — identical ring
 *    styling everywhere so the avatar is instantly recognizable.
 *  - Live: reads UserPreferences.profilePhotoFlow, so changing the photo on
 *    the Profile screen updates every header immediately, no restart.
 *  - Tapping it always leads to the user's Profile (pass [onClick]); screens
 *    must not attach any other action to it.
 *  - No status/online dots — the app has no presence system, so a green dot
 *    would be a false signal.
 */
@Composable
fun ProfileAvatar(
    name: String,
    size: Dp = 44.dp,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val prefs = remember { UserPreferences.getInstance(context) }
    val photoUrl by prefs.profilePhotoFlow.collectAsState()

    val base = modifier
        .size(size)
        .clip(CircleShape)
        .background(CyberAccent.copy(0.15f))
        .border(2.dp, CyberAccent.copy(0.4f), CircleShape)
    Box(
        modifier = if (onClick != null) base.clickable { onClick() } else base,
        contentAlignment = Alignment.Center
    ) {
        if (photoUrl.isNotBlank()) {
            AsyncImage(
                model = photoUrl,
                contentDescription = "Profile photo",
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(size).clip(CircleShape)
            )
        } else {
            Text(
                (name.firstOrNull()?.uppercaseChar() ?: 'U').toString(),
                fontSize = (size.value * 0.38f).sp,
                fontWeight = FontWeight.ExtraBold,
                color = CyberAccent
            )
        }
    }
}

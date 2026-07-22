package com.example.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.CyberAccent
import com.example.ui.theme.CyberBgPrimary
import com.example.ui.theme.CyberTextMuted
import com.example.ui.theme.CyberTextPrimary
import com.example.ui.theme.rememberReducedMotion
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SplashScreen(onComplete: () -> Unit) {
    val reduceMotion = rememberReducedMotion()
    val logoScale = remember { Animatable(0.4f) }
    val logoAlpha = remember { Animatable(0f) }
    val textAlpha = remember { Animatable(0f) }
    val taglineAlpha = remember { Animatable(0f) }

    val infiniteTransition = rememberInfiniteTransition(label = "loading_dots")
    val dot1 by infiniteTransition.animateFloat(
        initialValue = 0.2f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(500, delayMillis = 0, easing = FastOutSlowInEasing), RepeatMode.Reverse
        ), label = "d1"
    )
    val dot2 by infiniteTransition.animateFloat(
        initialValue = 0.2f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(500, delayMillis = 170, easing = FastOutSlowInEasing), RepeatMode.Reverse
        ), label = "d2"
    )
    val dot3 by infiniteTransition.animateFloat(
        initialValue = 0.2f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(500, delayMillis = 340, easing = FastOutSlowInEasing), RepeatMode.Reverse
        ), label = "d3"
    )

    LaunchedEffect(Unit) {
        if (reduceMotion) {
            // No bouncy scale under reduced-motion — reveal instantly, hold, continue.
            logoScale.snapTo(1f); logoAlpha.snapTo(1f); textAlpha.snapTo(1f); taglineAlpha.snapTo(1f)
            delay(1600)
            onComplete()
            return@LaunchedEffect
        }
        launch {
            logoScale.animateTo(
                1f,
                spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow)
            )
        }
        launch { logoAlpha.animateTo(1f, tween(380)) }
        delay(320)
        textAlpha.animateTo(1f, tween(420, easing = FastOutSlowInEasing))
        delay(120)
        taglineAlpha.animateTo(1f, tween(380, easing = FastOutSlowInEasing))
        delay(1300)
        onComplete()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberBgPrimary),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // App icon with bouncy entrance
            Box(
                modifier = Modifier
                    .scale(logoScale.value)
                    .alpha(logoAlpha.value)
                    .size(100.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(Color.Black)
                    .padding(10.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(R.mipmap.ic_launcher_foreground),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            }

            Spacer(Modifier.height(30.dp))

            // App name fade-in
            Row(modifier = Modifier.alpha(textAlpha.value)) {
                Text("ProCoach ", fontSize = 36.sp, fontWeight = FontWeight.Black, color = CyberTextPrimary)
                Text("India", fontSize = 36.sp, fontWeight = FontWeight.Black, color = CyberAccent)
            }

            Spacer(Modifier.height(8.dp))

            Text(
                "India's fitness coaching platform",
                fontSize = 14.sp,
                color = CyberTextMuted,
                modifier = Modifier.alpha(taglineAlpha.value)
            )

            Spacer(Modifier.height(72.dp))

            // Staggered loading dots
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.alpha(taglineAlpha.value)
            ) {
                listOf(dot1, dot2, dot3).forEach { alpha ->
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(RoundedCornerShape(50.dp))
                            .background(CyberAccent.copy(alpha = alpha))
                    )
                }
            }
        }
    }
}

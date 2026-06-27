package com.example.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.example.data.FoodNutrition
import com.example.data.GeminiNutrition
import com.example.ui.theme.CyberAccent
import com.example.ui.theme.CyberAccentDark
import com.example.ui.theme.CyberBgCard
import com.example.ui.theme.CyberBgCardElevated
import com.example.ui.theme.CyberBgPrimary
import com.example.ui.theme.CyberDanger
import com.example.ui.theme.CyberSuccess
import com.example.ui.theme.CyberTextMuted
import com.example.ui.theme.CyberTextPrimary
import com.example.ui.theme.CyberTextSecondary
import com.example.ui.theme.CyberWarning
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun FoodScannerScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope   = rememberCoroutineScope()

    var imageUri     by remember { mutableStateOf<Uri?>(null) }
    var imageBitmap  by remember { mutableStateOf<Bitmap?>(null) }
    var isAnalyzing  by remember { mutableStateOf(false) }
    var nutrition    by remember { mutableStateOf<FoodNutrition?>(null) }
    var errorMsg     by remember { mutableStateOf("") }

    // Temp file URI for camera capture
    val tempFile = remember {
        File(context.cacheDir.resolve("food_scans").also { it.mkdirs() }, "scan_${System.currentTimeMillis()}.jpg")
    }
    val cameraUri = remember {
        FileProvider.getUriForFile(context, "${context.packageName}.provider", tempFile)
    }

    // Camera launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            imageUri    = cameraUri
            imageBitmap = null
            isAnalyzing = false
            nutrition   = null
            errorMsg    = ""
            scope.launch {
                val bmp = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(cameraUri)
                        ?.use { BitmapFactory.decodeStream(it) }
                }
                imageBitmap = bmp
            }
        }
    }

    // Gallery launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            imageUri    = uri
            imageBitmap = null
            isAnalyzing = false
            nutrition   = null
            errorMsg    = ""
            scope.launch {
                val bmp = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)
                        ?.use { BitmapFactory.decodeStream(it) }
                }
                imageBitmap = bmp
            }
        }
    }

    // Camera permission launcher — requests CAMERA at runtime before launching camera
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) cameraLauncher.launch(cameraUri)
        // if denied, do nothing — button will still be visible to try again
    }

    fun analyzeImage() {
        val bmp = imageBitmap ?: return
        scope.launch {
            isAnalyzing = true
            errorMsg    = ""
            nutrition   = null
            val result  = GeminiNutrition.analyze(bmp)
            isAnalyzing = false
            result.fold(
                onSuccess = { nutrition = it },
                onFailure = { e ->
                    errorMsg = e.message?.takeIf { it.length < 200 && !it.contains("http") }
                        ?: "Analysis failed. Check internet and try again."
                }
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberBgPrimary)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // ── Top bar ───────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
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
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null,
                    tint = CyberTextPrimary, modifier = Modifier.size(20.dp))
            }
            Column(Modifier.weight(1f)) {
                Text("Food Scanner", fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold, color = CyberTextPrimary)
                Text("Snap food → get nutrition", fontSize = 12.sp, color = CyberTextMuted)
            }
            // Gemini badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Brush.horizontalGradient(listOf(Color(0xFF4285F4), Color(0xFF0F9D58))))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text("AI", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {

            // ── Image preview area ────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(4f / 3f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(CyberBgCard)
                    .border(1.dp, Color.White.copy(0.08f), RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (imageUri != null) {
                    AsyncImage(
                        model          = imageUri,
                        contentDescription = "Food photo",
                        contentScale   = ContentScale.Crop,
                        modifier       = Modifier.fillMaxSize().clip(RoundedCornerShape(20.dp))
                    )
                    // Dim overlay while analyzing
                    if (isAnalyzing) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(0.6f))
                                .clip(RoundedCornerShape(20.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                CircularProgressIndicator(color = CyberAccent, modifier = Modifier.size(36.dp))
                                Text("Analyzing food…", fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold, color = Color.White)
                                Text("Powered by Gemini AI", fontSize = 11.sp, color = Color.White.copy(0.6f))
                            }
                        }
                    }
                } else {
                    // Empty state
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(CyberAccent.copy(0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🍽️", fontSize = 28.sp)
                        }
                        Text("Take or choose a food photo",
                            fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                            color = CyberTextSecondary, textAlign = TextAlign.Center)
                        Text("Works with any food, meal or snack",
                            fontSize = 12.sp, color = CyberTextMuted, textAlign = TextAlign.Center)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Camera / Gallery buttons ──────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Camera button
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(CyberBgCard)
                        .border(1.dp, Color.White.copy(0.1f), RoundedCornerShape(16.dp))
                        .clickable {
                            if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.CAMERA)
                                == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                                cameraLauncher.launch(cameraUri)
                            } else {
                                cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                            }
                        }
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Filled.CameraAlt, null,
                            tint = CyberAccent, modifier = Modifier.size(20.dp))
                        Text("Camera", fontSize = 14.sp,
                            fontWeight = FontWeight.Bold, color = CyberTextPrimary)
                    }
                }
                // Gallery button
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(CyberBgCard)
                        .border(1.dp, Color.White.copy(0.1f), RoundedCornerShape(16.dp))
                        .clickable { galleryLauncher.launch("image/*") }
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Filled.Image, null,
                            tint = CyberTextSecondary, modifier = Modifier.size(20.dp))
                        Text("Gallery", fontSize = 14.sp,
                            fontWeight = FontWeight.Bold, color = CyberTextPrimary)
                    }
                }
            }

            // ── Analyze button ────────────────────────────────────────────────
            if (imageBitmap != null && !isAnalyzing) {
                Spacer(Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.horizontalGradient(listOf(CyberAccent, Color(0xFF00D4AA)))
                        )
                        .clickable { analyzeImage() }
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("🔍", fontSize = 16.sp)
                        Text(
                            if (nutrition != null) "Scan Again" else "Analyze Nutrition",
                            fontSize = 15.sp, fontWeight = FontWeight.ExtraBold,
                            color = CyberAccentDark
                        )
                    }
                }
            }

            // ── Error state ───────────────────────────────────────────────────
            if (errorMsg.isNotBlank()) {
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(CyberDanger.copy(0.1f))
                        .border(1.dp, CyberDanger.copy(0.3f), RoundedCornerShape(12.dp))
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("⚠", fontSize = 14.sp)
                    Text(errorMsg, fontSize = 12.sp, color = CyberDanger, modifier = Modifier.weight(1f))
                    Icon(Icons.Filled.Refresh, null,
                        tint = CyberDanger,
                        modifier = Modifier.size(18.dp).clickable(enabled = !isAnalyzing) { analyzeImage() })
                }
            }

            // ── Nutrition results card ────────────────────────────────────────
            AnimatedVisibility(
                visible = nutrition != null,
                enter   = fadeIn() + slideInVertically { it / 2 },
                exit    = fadeOut()
            ) {
                nutrition?.let { n ->
                    Spacer(Modifier.height(20.dp))
                    NutritionResultCard(n)
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun NutritionResultCard(n: FoodNutrition) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(CyberBgCard)
            .border(1.dp, Color.White.copy(0.08f), RoundedCornerShape(20.dp))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ── Header ────────────────────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(Modifier.weight(1f)) {
                Text(n.foodName, fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold, color = CyberTextPrimary)
                Spacer(Modifier.height(2.dp))
                Text(n.servingSize, fontSize = 12.sp, color = CyberTextMuted)
            }
            // Confidence badge
            val (badgeColor, badgeText) = when (n.confidence) {
                "high"   -> CyberSuccess to "High accuracy"
                "medium" -> CyberWarning to "Medium accuracy"
                else     -> CyberDanger  to "Low accuracy"
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(badgeColor.copy(0.15f))
                    .border(1.dp, badgeColor.copy(0.4f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text(badgeText, fontSize = 11.sp,
                    fontWeight = FontWeight.Bold, color = badgeColor)
            }
        }

        // ── Calories hero ─────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(CyberAccent.copy(0.1f))
                .border(1.dp, CyberAccent.copy(0.25f), RoundedCornerShape(14.dp))
                .padding(vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("🔥", fontSize = 28.sp)
                Spacer(Modifier.height(4.dp))
                Text("${n.calories}", fontSize = 36.sp,
                    fontWeight = FontWeight.ExtraBold, color = CyberAccent)
                Text("calories", fontSize = 13.sp, color = CyberTextMuted)
            }
        }

        // ── Macros grid ───────────────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MacroChip("Protein",  "${n.proteinG}g", Color(0xFF3B82F6), "💪", Modifier.weight(1f))
            MacroChip("Carbs",    "${n.carbsG}g",   Color(0xFFF59E0B), "⚡", Modifier.weight(1f))
            MacroChip("Fat",      "${n.fatG}g",     Color(0xFFEF4444), "🥑", Modifier.weight(1f))
        }

        // ── Fiber bar ─────────────────────────────────────────────────────────
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("🌾", fontSize = 13.sp)
                    Text("Fiber", fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold, color = CyberTextSecondary)
                }
                Text("${n.fiberG}g / 25g daily", fontSize = 12.sp, color = CyberTextMuted)
            }
            LinearProgressIndicator(
                progress      = { (n.fiberG / 25f).coerceIn(0f, 1f) },
                modifier      = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(99.dp)),
                color         = CyberSuccess,
                trackColor    = CyberSuccess.copy(0.15f)
            )
        }

        // ── Macro bars ────────────────────────────────────────────────────────
        val totalMacros = n.proteinG + n.carbsG + n.fatG
        if (totalMacros > 0f) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Macro breakdown", fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold, color = CyberTextMuted)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(99.dp))
                ) {
                    Box(Modifier.weight((n.proteinG / totalMacros).coerceAtLeast(0.001f)).fillMaxSize()
                        .background(Color(0xFF3B82F6)))
                    Box(Modifier.weight((n.carbsG / totalMacros).coerceAtLeast(0.001f)).fillMaxSize()
                        .background(Color(0xFFF59E0B)))
                    Box(Modifier.weight((n.fatG / totalMacros).coerceAtLeast(0.001f)).fillMaxSize()
                        .background(Color(0xFFEF4444)))
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    MacroLegend("Protein", Color(0xFF3B82F6))
                    MacroLegend("Carbs",   Color(0xFFF59E0B))
                    MacroLegend("Fat",     Color(0xFFEF4444))
                }
            }
        }

        // ── Notes ─────────────────────────────────────────────────────────────
        if (n.notes.isNotBlank()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(CyberBgCardElevated)
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("💡", fontSize = 13.sp)
                Text(n.notes, fontSize = 12.sp, color = CyberTextSecondary,
                    modifier = Modifier.weight(1f))
            }
        }

        // ── Disclaimer ────────────────────────────────────────────────────────
        Text(
            "* Values are AI estimates. Actual nutrition may vary based on preparation and ingredients.",
            fontSize = 10.sp, color = CyberTextMuted, textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun MacroChip(
    label: String, value: String, color: Color, emoji: String, modifier: Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(0.1f))
            .border(1.dp, color.copy(0.25f), RoundedCornerShape(12.dp))
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(emoji, fontSize = 18.sp)
        Text(value, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = color)
        Text(label, fontSize = 11.sp, color = CyberTextMuted)
    }
}

@Composable
private fun MacroLegend(label: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
        Text(label, fontSize = 10.sp, color = CyberTextMuted)
    }
}

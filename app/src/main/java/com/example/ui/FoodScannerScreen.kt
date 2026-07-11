package com.example.ui

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.speech.RecognizerIntent
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
import androidx.compose.material.icons.filled.Mic
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
import com.example.data.BarcodeScanner
import com.example.data.FoodNutrition
import com.example.data.LocalFoodParser
import com.example.data.MlKitFoodScanner
import com.example.data.OpenFoodFactsService
import com.example.data.UsdaFoodService
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

private enum class ScanMode { AI, BARCODE, VOICE }

@Composable
fun FoodScannerScreen(onBack: () -> Unit, onOpenDiary: () -> Unit = {}) {
    val context = LocalContext.current
    val scope   = rememberCoroutineScope()

    var mode         by remember { mutableStateOf(ScanMode.AI) }
    var imageUri     by remember { mutableStateOf<Uri?>(null) }
    var imageBitmap  by remember { mutableStateOf<Bitmap?>(null) }
    var isAnalyzing  by remember { mutableStateOf(false) }
    var results      by remember { mutableStateOf<List<FoodNutrition>>(emptyList()) }
    var errorMsg     by remember { mutableStateOf("") }
    var voiceQuery   by remember { mutableStateOf("") }

    fun resetResult() { results = emptyList(); errorMsg = "" }

    // Temp file URI for camera capture
    val tempFile = remember {
        File(context.cacheDir.resolve("food_scans").also { it.mkdirs() }, "scan_current.jpg")
    }
    val cameraUri = remember {
        FileProvider.getUriForFile(context, "${context.packageName}.provider", tempFile)
    }

    // ── Analyze helpers ────────────────────────────────────────────────────────

    // AI Scan — ML Kit on-device image labeling, no API key needed
    fun analyzeAI(bmp: Bitmap) {
        scope.launch {
            isAnalyzing = true; resetResult()
            val result = MlKitFoodScanner.analyze(bmp)
            isAnalyzing = false
            result.fold(
                onSuccess = { results = listOf(it) },
                onFailure = { e ->
                    errorMsg = e.message?.take(200)
                        ?: "Could not identify food. Try a clearer photo or use Voice mode."
                }
            )
        }
    }

    // Barcode — Open Food Facts first (free, no key), USDA branded foods as fallback
    fun analyzeBarcode(bmp: Bitmap) {
        scope.launch {
            isAnalyzing = true; resetResult()
            val barcode = BarcodeScanner.scan(bmp)
            if (barcode == null) {
                isAnalyzing = false
                errorMsg = "No barcode detected. Point camera directly at the barcode or switch to AI Scan."
                return@launch
            }
            val offResult = OpenFoodFactsService.lookup(barcode)
            if (offResult.isSuccess) {
                isAnalyzing = false
                results = listOfNotNull(offResult.getOrNull())
                return@launch
            }
            // Open Food Facts missed — try USDA Branded Foods
            val usdaResult = UsdaFoodService.lookupBarcode(barcode)
            isAnalyzing = false
            usdaResult.fold(
                onSuccess = { results = listOf(it) },
                onFailure = { e ->
                    errorMsg = e.message?.take(200) ?: "Product not found. Try AI Scan or Voice mode."
                }
            )
        }
    }

    // Voice — splits multiple foods, applies quantity per food; offline DB +
    // supplements first, USDA fallback for anything unknown.
    fun analyzeVoice(query: String) {
        scope.launch {
            isAnalyzing = true; resetResult()
            val analysis = com.example.data.FoodAnalyzer.analyze(query)
            isAnalyzing = false
            results = analysis.items
            errorMsg = when {
                analysis.items.isEmpty() ->
                    "Food not recognised. Try: \"2 rotis and dal\", \"a bowl of curd rice\", \"200g chicken\""
                analysis.unresolved.isNotEmpty() ->
                    "Couldn't find: ${analysis.unresolved.joinToString(", ")}"
                else -> ""
            }
        }
    }

    // ── Camera launcher ────────────────────────────────────────────────────────
    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (!success) return@rememberLauncherForActivityResult
        imageUri = cameraUri; voiceQuery = ""
        scope.launch {
            val bmp = withContext(Dispatchers.IO) { decodeDownsampled(context, cameraUri) }
            imageBitmap = bmp
            if (bmp == null) errorMsg = "Couldn't read that photo — try again."
            else if (mode == ScanMode.BARCODE) analyzeBarcode(bmp)
        }
    }

    // ── Gallery launcher ───────────────────────────────────────────────────────
    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        imageUri = uri; imageBitmap = null; voiceQuery = ""; resetResult()
        scope.launch {
            val bmp = withContext(Dispatchers.IO) { decodeDownsampled(context, uri) }
            imageBitmap = bmp
            if (bmp == null) errorMsg = "Couldn't read that photo — try again."
            else if (mode == ScanMode.BARCODE) analyzeBarcode(bmp)
        }
    }

    // ── Camera permission launcher ─────────────────────────────────────────────
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> if (granted) cameraLauncher.launch(cameraUri) }

    // ── Speech recognizer launcher ─────────────────────────────────────────────
    val speechLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val text = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
        if (!text.isNullOrBlank()) {
            voiceQuery  = text
            imageUri    = null
            imageBitmap = null
            resetResult()
            analyzeVoice(text)
        }
    }

    fun launchCamera() {
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.CAMERA)
            == android.content.pm.PackageManager.PERMISSION_GRANTED
        ) cameraLauncher.launch(cameraUri)
        else cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
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
                    .size(40.dp).clip(CircleShape)
                    .background(CyberBgCard)
                    .clickable { onBack() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = CyberTextPrimary, modifier = Modifier.size(20.dp))
            }
            Column(Modifier.weight(1f)) {
                Text("Food Scanner", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = CyberTextPrimary)
                Text(when (mode) {
                    ScanMode.AI      -> "Snap food → AI nutrition analysis"
                    ScanMode.BARCODE -> "Point camera at product barcode"
                    ScanMode.VOICE   -> "Say a food name or meal"
                }, fontSize = 12.sp, color = CyberTextMuted)
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Brush.horizontalGradient(listOf(Color(0xFF4285F4), Color(0xFF0F9D58))))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text("AI", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
            }
        }

        // ── Mode selector ─────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(CyberBgCard)
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            listOf(ScanMode.AI to "🤖 AI Scan", ScanMode.BARCODE to "📦 Barcode", ScanMode.VOICE to "🎙 Voice")
                .forEach { (m, label) ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (mode == m) CyberAccent else Color.Transparent)
                            .clickable {
                                if (mode != m) {
                                    mode = m
                                    imageUri   = null; imageBitmap = null
                                    voiceQuery = ""; resetResult()
                                }
                            }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            label,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (mode == m) CyberAccentDark else CyberTextSecondary
                        )
                    }
                }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(16.dp))

            if (mode == ScanMode.VOICE) {
                // ── Voice mode ─────────────────────────────────────────────────
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(CyberBgCard)
                        .border(1.dp, Color.White.copy(0.08f), RoundedCornerShape(20.dp))
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        if (voiceQuery.isNotBlank()) {
                            Text("\"$voiceQuery\"", fontSize = 15.sp, fontWeight = FontWeight.Bold,
                                color = CyberTextPrimary, textAlign = TextAlign.Center)
                        } else {
                            Text("🎙️", fontSize = 48.sp)
                            Text("Tap the mic and say a food or meal", fontSize = 14.sp,
                                color = CyberTextSecondary, textAlign = TextAlign.Center)
                            Text("e.g. \"2 chapatis with dal\" or \"100g paneer\"",
                                fontSize = 12.sp, color = CyberTextMuted, textAlign = TextAlign.Center)
                        }
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        if (isAnalyzing) listOf(CyberBgCardElevated, CyberBgCardElevated)
                                        else listOf(CyberAccent, Color(0xFF00D4AA))
                                    )
                                )
                                .clickable(enabled = !isAnalyzing) {
                                    // Some devices (no Google app / de-Googled OEMs) have no speech
                                    // recognizer — launching blindly crashes with ActivityNotFound
                                    if (!android.speech.SpeechRecognizer.isRecognitionAvailable(context)) {
                                        android.widget.Toast.makeText(context,
                                            "Voice input isn't available on this device — install/enable the Google app",
                                            android.widget.Toast.LENGTH_LONG).show()
                                    } else {
                                        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                            putExtra(RecognizerIntent.EXTRA_PROMPT, "Say a food name or meal")
                                            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                                        }
                                        try {
                                            speechLauncher.launch(intent)
                                        } catch (_: Exception) {
                                            android.widget.Toast.makeText(context,
                                                "Couldn't open voice input on this device",
                                                android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isAnalyzing) {
                                CircularProgressIndicator(color = CyberAccent, modifier = Modifier.size(28.dp), strokeWidth = 2.5.dp)
                            } else {
                                Icon(Icons.Filled.Mic, null, tint = CyberAccentDark, modifier = Modifier.size(32.dp))
                            }
                        }
                    }
                }
            } else {
                // ── AI Scan / Barcode image area ───────────────────────────────
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
                            model = imageUri, contentDescription = "Food photo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(20.dp))
                        )
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
                                    Text(
                                        if (mode == ScanMode.BARCODE) "Scanning barcode…" else "Analyzing food…",
                                        fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.White
                                    )
                                    Text(
                                        if (mode == ScanMode.BARCODE) "Open Food Facts + USDA" else "ML Kit · on-device",
                                        fontSize = 11.sp, color = Color.White.copy(0.6f)
                                    )
                                }
                            }
                        }
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier.size(64.dp).clip(CircleShape).background(CyberAccent.copy(0.1f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(if (mode == ScanMode.BARCODE) "📦" else "🍽️", fontSize = 28.sp)
                            }
                            Text(
                                if (mode == ScanMode.BARCODE) "Point at a product barcode" else "Take or choose a food photo",
                                fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                                color = CyberTextSecondary, textAlign = TextAlign.Center
                            )
                            Text(
                                if (mode == ScanMode.BARCODE) "Works with packaged food barcodes" else "Works with any food, meal or snack",
                                fontSize = 12.sp, color = CyberTextMuted, textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // ── Camera / Gallery buttons ──────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(CyberBgCard)
                            .border(1.dp, Color.White.copy(0.1f), RoundedCornerShape(16.dp))
                            .clickable { launchCamera() }
                            .padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Filled.CameraAlt, null, tint = CyberAccent, modifier = Modifier.size(20.dp))
                            Text("Camera", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = CyberTextPrimary)
                        }
                    }
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
                            Icon(Icons.Filled.Image, null, tint = CyberTextSecondary, modifier = Modifier.size(20.dp))
                            Text("Gallery", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = CyberTextPrimary)
                        }
                    }
                }

                // ── Analyze button (AI Scan mode only) ────────────────────────
                if (mode == ScanMode.AI && imageBitmap != null && !isAnalyzing) {
                    Spacer(Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Brush.horizontalGradient(listOf(CyberAccent, Color(0xFF00D4AA))))
                            .clickable { imageBitmap?.let { analyzeAI(it) } }
                            .padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("🔍", fontSize = 16.sp)
                            Text(
                                if (results.isNotEmpty()) "Scan Again" else "Analyze Nutrition",
                                fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = CyberAccentDark
                            )
                        }
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
                    if (mode == ScanMode.AI && imageBitmap != null) {
                        Icon(Icons.Filled.Refresh, null,
                            tint = CyberDanger,
                            modifier = Modifier.size(18.dp).clickable(enabled = !isAnalyzing) {
                                imageBitmap?.let { analyzeAI(it) }
                            })
                    }
                }
            }

            // ── Nutrition result cards (one per food) ──────────────────────────
            AnimatedVisibility(
                visible = results.isNotEmpty(),
                enter   = fadeIn() + slideInVertically { it / 2 },
                exit    = fadeOut()
            ) {
                // AnimatedVisibility stacks children like a Box — a Column is
                // required or the card and buttons render on top of each other
                Column {
                    Spacer(Modifier.height(20.dp))
                    results.forEach { n ->
                        NutritionResultCard(n)
                        Spacer(Modifier.height(12.dp))
                    }

                    // ── Log every recognised food to the daily diary ────────────
                    var logged by remember(results) { mutableStateOf(false) }
                    val diaryScope = rememberCoroutineScope()
                    val totalCal = results.sumOf { it.calories }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (logged) CyberSuccess.copy(0.15f) else CyberAccent)
                            .clickable(enabled = !logged) {
                                diaryScope.launch {
                                    val src = when (mode) {
                                        ScanMode.BARCODE -> "BARCODE"
                                        ScanMode.VOICE   -> "VOICE"
                                        else             -> "AI"
                                    }
                                    val dao = com.example.data.AppDatabase.getInstance(context).foodDiaryDao()
                                    results.forEach { n ->
                                        val entry = com.example.data.FoodDiary.entryFrom(n, source = src)
                                        dao.insert(entry)
                                        com.example.data.FoodDiary.syncSave(entry)
                                    }
                                    logged = true
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            when {
                                logged             -> "✓ Added to today's diary"
                                results.size == 1  -> "🍽  Add to Food Diary"
                                else               -> "🍽  Add ${results.size} items · $totalCal kcal"
                            },
                            fontSize = 15.sp, fontWeight = FontWeight.Bold,
                            color = if (logged) CyberSuccess else CyberAccentDark
                        )
                    }

                    // After logging, take the user straight to their diary
                    if (logged) {
                        Spacer(Modifier.height(10.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(CyberBgCard)
                                .border(1.dp, CyberAccent.copy(0.35f), RoundedCornerShape(16.dp))
                                .clickable { onOpenDiary() },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("📖  View Food Diary →", fontSize = 14.sp,
                                fontWeight = FontWeight.Bold, color = CyberAccent)
                        }
                    }
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(Modifier.weight(1f)) {
                Text(n.foodName, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = CyberTextPrimary)
                Spacer(Modifier.height(2.dp))
                Text(n.servingSize, fontSize = 12.sp, color = CyberTextMuted)
            }
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
                Text(badgeText, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = badgeColor)
            }
        }

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
                Text("${n.calories}", fontSize = 36.sp, fontWeight = FontWeight.ExtraBold, color = CyberAccent)
                Text("calories", fontSize = 13.sp, color = CyberTextMuted)
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MacroChip("Protein", "%.1fg".format(n.proteinG), Color(0xFF3B82F6), "💪", Modifier.weight(1f))
            MacroChip("Carbs",   "%.1fg".format(n.carbsG),   Color(0xFFF59E0B), "⚡", Modifier.weight(1f))
            MacroChip("Fat",     "%.1fg".format(n.fatG),     Color(0xFFEF4444), "🥑", Modifier.weight(1f))
        }

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("🌾", fontSize = 13.sp)
                    Text("Fiber", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = CyberTextSecondary)
                }
                Text("%.1fg / 25g daily".format(n.fiberG), fontSize = 12.sp, color = CyberTextMuted)
            }
            LinearProgressIndicator(
                progress      = { (n.fiberG / 25f).coerceIn(0f, 1f) },
                modifier      = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(99.dp)),
                color         = CyberSuccess,
                trackColor    = CyberSuccess.copy(0.15f)
            )
        }

        val totalMacros = n.proteinG + n.carbsG + n.fatG
        if (totalMacros > 0f) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Macro breakdown", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = CyberTextMuted)
                Row(modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(99.dp))) {
                    Box(Modifier.weight((n.proteinG / totalMacros).coerceAtLeast(0.001f)).fillMaxSize().background(Color(0xFF3B82F6)))
                    Box(Modifier.weight((n.carbsG   / totalMacros).coerceAtLeast(0.001f)).fillMaxSize().background(Color(0xFFF59E0B)))
                    Box(Modifier.weight((n.fatG     / totalMacros).coerceAtLeast(0.001f)).fillMaxSize().background(Color(0xFFEF4444)))
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    MacroLegend("Protein", Color(0xFF3B82F6))
                    MacroLegend("Carbs",   Color(0xFFF59E0B))
                    MacroLegend("Fat",     Color(0xFFEF4444))
                }
            }
        }

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
                Text(n.notes, fontSize = 12.sp, color = CyberTextSecondary, modifier = Modifier.weight(1f))
            }
        }

        Text(
            "* Values are AI estimates. Actual nutrition may vary based on preparation and ingredients.",
            fontSize = 10.sp, color = CyberTextMuted, textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun MacroChip(label: String, value: String, color: Color, emoji: String, modifier: Modifier) {
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
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
        Text(label, fontSize = 10.sp, color = CyberTextMuted)
    }
}

/**
 * Decode a photo with downsampling so a 12MP camera image (~48MB as ARGB_8888)
 * can't OutOfMemory-crash a 2GB device — the exact low-RAM phone this feature
 * targets. Reads bounds first, then samples to a ~1600px longest side.
 */
private fun decodeDownsampled(context: android.content.Context, uri: Uri, maxSide: Int = 1600): android.graphics.Bitmap? {
    return try {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        val longest = maxOf(bounds.outWidth, bounds.outHeight)
        while (longest / sample > maxSide) sample *= 2
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
    } catch (_: OutOfMemoryError) {
        null
    } catch (_: Exception) {
        null
    }
}

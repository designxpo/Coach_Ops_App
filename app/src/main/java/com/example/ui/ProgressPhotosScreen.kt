package com.example.ui

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.example.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ProgressPhotosScreen(viewModel: HealthViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope   = rememberCoroutineScope()
    val photos  by viewModel.photos.collectAsState()
    var previewUri by remember { mutableStateOf<String?>(null) }

    val tempFile = remember {
        File(context.cacheDir.resolve("progress_photos").also { it.mkdirs() },
            "progress_${System.currentTimeMillis()}.jpg")
    }
    val cameraUri = remember {
        FileProvider.getUriForFile(context, "${context.packageName}.provider", tempFile)
    }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        if (ok) {
            scope.launch {
                val dest = withContext(Dispatchers.IO) {
                    val dir = context.filesDir.resolve("progress_photos").also { it.mkdirs() }
                    val name = "progress_${System.currentTimeMillis()}.jpg"
                    val destFile = File(dir, name)
                    context.contentResolver.openInputStream(cameraUri)?.use { inp ->
                        destFile.outputStream().use { out -> inp.copyTo(out) }
                    }
                    destFile.absolutePath
                }
                viewModel.saveProgressPhoto(dest)
            }
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            scope.launch {
                val dest = withContext(Dispatchers.IO) {
                    val dir = context.filesDir.resolve("progress_photos").also { it.mkdirs() }
                    val name = "progress_${System.currentTimeMillis()}.jpg"
                    val destFile = File(dir, name)
                    context.contentResolver.openInputStream(uri)?.use { inp ->
                        destFile.outputStream().use { out -> inp.copyTo(out) }
                    }
                    destFile.absolutePath
                }
                viewModel.saveProgressPhoto(dest)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberBgPrimary)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape)
                    .background(CyberBgCard).clickable { onBack() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = CyberTextPrimary, modifier = Modifier.size(20.dp))
            }
            Column(Modifier.weight(1f)) {
                Text("Progress Photos", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = CyberTextPrimary)
                Text("Visual tracking of your transformation", fontSize = 12.sp, color = CyberTextMuted)
            }
        }

        // Add buttons row
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp))
                    .background(CyberBgCard).border(1.dp, Color.White.copy(0.1f), RoundedCornerShape(12.dp))
                    .clickable { cameraLauncher.launch(cameraUri) }.padding(14.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Filled.CameraAlt, null, tint = CyberAccent, modifier = Modifier.size(18.dp))
                    Text("Camera", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = CyberTextPrimary)
                }
            }
            Box(
                modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp))
                    .background(CyberBgCard).border(1.dp, Color.White.copy(0.1f), RoundedCornerShape(12.dp))
                    .clickable { galleryLauncher.launch("image/*") }.padding(14.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Filled.Image, null, tint = CyberTextSecondary, modifier = Modifier.size(18.dp))
                    Text("Gallery", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = CyberTextPrimary)
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        if (photos.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("📸", fontSize = 48.sp)
                    Text("No progress photos yet", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = CyberTextSecondary)
                    Text("Take your first photo to start tracking", fontSize = 13.sp, color = CyberTextMuted)
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(photos) { photo ->
                    Box(
                        modifier = Modifier.aspectRatio(1f).clip(RoundedCornerShape(10.dp))
                            .background(CyberBgCard).clickable { previewUri = photo.localPath }
                    ) {
                        AsyncImage(
                            model = File(photo.localPath),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier.align(Alignment.BottomStart)
                                .fillMaxWidth().background(Color.Black.copy(0.5f))
                                .padding(4.dp)
                        ) {
                            Text(photo.date, fontSize = 9.sp, color = Color.White)
                        }
                        Box(
                            modifier = Modifier.align(Alignment.TopEnd).padding(4.dp)
                                .size(20.dp).clip(CircleShape).background(Color.Black.copy(0.6f))
                                .clickable { viewModel.deleteProgressPhoto(photo.id) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.Close, null, tint = Color.White, modifier = Modifier.size(12.dp))
                        }
                    }
                }
            }
        }
    }

    if (previewUri != null) {
        Dialog(onDismissRequest = { previewUri = null }) {
            Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color.Black)) {
                AsyncImage(
                    model = File(previewUri!!),
                    contentDescription = null,
                    modifier = Modifier.fillMaxWidth(),
                    contentScale = ContentScale.FillWidth
                )
                Box(
                    modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
                        .size(32.dp).clip(CircleShape).background(Color.Black.copy(0.6f))
                        .clickable { previewUri = null },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Close, null, tint = Color.White, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

package com.example.data

import android.graphics.Bitmap
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

object BarcodeScanner {
    suspend fun scan(bitmap: Bitmap): String? = suspendCancellableCoroutine { cont ->
        val image = InputImage.fromBitmap(bitmap, 0)
        BarcodeScanning.getClient().process(image)
            .addOnSuccessListener { barcodes ->
                cont.resume(barcodes.firstOrNull()?.rawValue)
            }
            .addOnFailureListener { cont.resume(null) }
    }
}

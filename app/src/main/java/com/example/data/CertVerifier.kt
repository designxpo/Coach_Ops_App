package com.example.data

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.tasks.await

/**
 * On-device auto-review of an uploaded certificate photo (ML Kit OCR, free).
 *
 * A document auto-verifies when its text contains BOTH the coach's name and a
 * recognised fitness-certification issuer/keyword. Anything else goes to the
 * manual review queue in the admin portal. Auto-verified documents are still
 * listed there for audit, so a spoofed image can be revoked by an admin.
 */
object CertVerifier {

    data class CertCheck(
        val status: String,     // "verified_auto" | "pending"
        val summary: String     // what OCR found — stored for the admin reviewer
    )

    private val ISSUER_KEYWORDS = listOf(
        // International certifying bodies
        "ace", "acsm", "nasm", "issa", "nsca", "reps", "ifpa", "nesta", "afaa",
        // Indian institutes & common bodies
        "k11", "gffi", "iaft", "bfy", "cbt", "gold's gym", "golds gym",
        "talwalkars", "anytime fitness", "cult", "sports authority of india",
        "b.p.ed", "m.p.ed", "bped", "mped",
        // Generic certificate language
        "certified", "certificate", "certification", "diploma",
        "personal trainer", "fitness trainer", "successfully completed"
    )

    suspend fun autoReview(context: Context, uri: Uri, coachName: String): CertCheck {
        val text = try {
            val image = InputImage.fromFilePath(context, uri)
            TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                .process(image).await().text.lowercase()
        } catch (e: Exception) {
            return CertCheck("pending", "OCR failed: ${e.message?.take(60)}")
        }

        if (text.isBlank()) return CertCheck("pending", "No readable text found in the document")

        val nameWords = coachName.lowercase().split(" ").filter { it.length >= 3 }
        val nameHit   = nameWords.any { text.contains(it) }
        val issuerHit = ISSUER_KEYWORDS.firstOrNull { text.contains(it) }

        return when {
            nameHit && issuerHit != null ->
                CertCheck("verified_auto", "OCR matched coach name + \"$issuerHit\"")
            issuerHit != null ->
                CertCheck("pending", "Certificate language found (\"$issuerHit\") but coach name not detected")
            nameHit ->
                CertCheck("pending", "Coach name found but no recognised certification wording")
            else ->
                CertCheck("pending", "Neither coach name nor certification wording detected")
        }
    }
}

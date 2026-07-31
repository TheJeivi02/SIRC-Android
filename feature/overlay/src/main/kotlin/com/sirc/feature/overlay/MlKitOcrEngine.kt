package com.sirc.feature.overlay

import android.graphics.BitmapFactory
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.sirc.capture.log.SircLogger
import com.sirc.capture.ocr.OcrEngine
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Motor OCR basado en ML Kit (reconocimiento de texto latino). Expone una API
 * suspendida para encajar en el pipeline de captura.
 */
@Singleton
class MlKitOcrEngine @Inject constructor(
    private val logger: SircLogger,
) : OcrEngine {
    private val recognizer by lazy { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }

    override suspend fun recognize(imageData: ByteArray): List<String> {
        val bitmap = BitmapFactory.decodeByteArray(imageData, 0, imageData.size) ?: return emptyList()
        val image = InputImage.fromBitmap(bitmap, 0)
        return suspendCancellableCoroutine { continuation ->
            recognizer.process(image)
                .addOnSuccessListener { result ->
                    continuation.resume(result.text.lines())
                }
                .addOnFailureListener { error ->
                    logger.error(TAG, "OCR falló: ${error.message}")
                    continuation.resume(emptyList())
                }
        }
    }

    companion object {
        private const val TAG = "MlKitOcr"
    }
}

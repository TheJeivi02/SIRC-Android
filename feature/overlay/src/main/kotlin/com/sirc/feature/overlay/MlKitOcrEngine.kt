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
 *
 * El bitmap decodificado se recicla al terminar para evitar presión de memoria,
 * y la corrutina se cancela correctamente si el consumidor la abandona.
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
            val task = recognizer.process(image)
            task.addOnSuccessListener { result ->
                bitmap.recycle()
                continuation.resume(result.text.lines())
            }
            task.addOnFailureListener { error ->
                bitmap.recycle()
                logger.error(TAG, "OCR falló: ${error.message}")
                continuation.resume(emptyList())
            }
            continuation.invokeOnCancellation {
                runCatching { bitmap.recycle() }
            }
        }
    }

    companion object {
        private const val TAG = "MlKitOcr"
    }
}

package com.sirc.app.debug

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.sirc.capture.model.CaptureRequest
import com.sirc.capture.ocr.OcrEngine
import com.sirc.capture.pipeline.CapturePipeline
import com.sirc.core.platform.CaptureInputType
import com.sirc.core.platform.DetectionResolution
import com.sirc.core.platform.PlatformDetectionEngine
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.Collections
import javax.inject.Inject

/**
 * Receptor de depuración exclusivo de builds DEBUG (source set `debug`).
 *
 * Prima SOLO en `assembleDebug`; no existe en Release ni se declara en el
 * manifest de producción. Inyecta las imágenes reales empaquetadas como
 * assets (`assets/sirc_test`, imágenes JPG) en el pipeline de captura de
 * producción
 * para validar OCR + detección + parsing con capturas reales de Uber e
 * InDriver (Sprint 12 / E1a, validación física).
 *
 * Flujo (no toca producción, reutiliza sus singletons):
 *  1. Lee cada imagen empaquetada.
 *  2. Ejecuta OCR con el mismo motor de producción ([OcrEngine], ML Kit).
 *  3. Construye un [CaptureRequest] con los textos OCR reales.
 *  4. Lo inyecta en el [CapturePipeline] existente (misma ruta que usan los
 *     requests de accesibilidad/MediaProjection).
 *  5. Registra en logcat el resultado por imagen con el tag `SIRC-OCR-TEST`:
 *     latencias OCR/detección/parseo/total, plataforma, pantalla y campos.
 *
 * No añade permisos, no abre rutas de galería y no sube ningún dato (100 %
 * local). Disparo: `adb shell am broadcast -a com.sirc.debug.OCR_TEST`.
 */
@AndroidEntryPoint
class DebugImageOcrReceiver : BroadcastReceiver() {
    @Inject lateinit var pipeline: CapturePipeline

    @Inject lateinit var ocrEngine: OcrEngine

    @Inject lateinit var detectionEngine: PlatformDetectionEngine

    override fun onReceive(
        context: Context?,
        intent: Intent?,
    ) {
        if (intent?.action != ACTION_OCR_TEST) return
        val appContext = context?.applicationContext ?: return
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            try {
                runAll(appContext)
            } finally {
                pending.finish()
            }
        }
    }

    private suspend fun runAll(context: Context) {
        val names = context.assets.list(ASSET_DIR).orEmpty().filter { it.matches(IMAGE_PATTERN) }
        if (names.isEmpty()) {
            Log.w(TAG, "no hay imágenes de prueba en assets/$ASSET_DIR")
            return
        }
        Collections.sort(names)
        Log.i(TAG, "==== INICIO PRUEBA OCR (${names.size} imágenes) ====")
        names.forEach { name ->
            val bytes = context.assets.open("$ASSET_DIR/$name").use { it.readBytes() }
            runOne(name, bytes)
        }
        Log.i(TAG, "==== FIN PRUEBA OCR ====")
    }

    private suspend fun runOne(
        name: String,
        bytes: ByteArray,
    ) {
        val result = StringBuilder("IMG=$name ")
        val ocrStart = System.nanoTime()
        val texts = ocrEngine.recognize(bytes)
        val ocrMillis = elapsedMillis(ocrStart)
        result.append("ocr=${format(ocrMillis)}ms ")

        val body = name.substringBeforeLast(".")
        val pack = packageFor(body)
        val detection = detectionEngine.detect(texts, packageName = pack)
        result.append(
            "package=$pack detector=${detection.resolution.name} screen=${detection.screenDetection.type.name} ",
        )

        val request =
            CaptureRequest(
                id = System.nanoTime(),
                packageName = pack,
                timestampMillis = System.currentTimeMillis(),
                texts = texts,
                imageData = bytes,
                origin = CaptureInputType.OCR,
            )
        Log.i(TAG, "IMG=$name -> inyectando ${texts.size} líneas OCR al pipeline")
        val snapshot = pipeline.process(request)
        val metrics = pipeline.lastMetrics.value

        metrics.ocrMillis?.let { result.append("pipelineOcr=${format(it)}ms ") }
        metrics.detectionMillis?.let { result.append("detection=${format(it)}ms ") }
        metrics.parseMillis?.let { result.append("parse=${format(it)}ms ") }
        metrics.totalMillis?.let { result.append("total=${format(it)}ms ") }

        if (snapshot != null) {
            result.append(
                "snapshot=OK platform=${snapshot.platform.name} " +
                    "total=${snapshot.estimatedTotal} dist=${snapshot.distanceKm}km " +
                    "dur=${snapshot.durationMin}min texts=${snapshot.texts.size}",
            )
        } else {
            result.append("snapshot=NULL reason=${describeFailure(texts, pack)} state=${pipeline.state.value}")
        }
        Log.i(TAG, result.toString())
        logOcrTexts(name, texts)
    }

    /** Detalla por qué no hubo snapshot sin inventar datos (solo diagnóstico). */
    private fun describeFailure(
        texts: List<String>,
        packageName: String?,
    ): String {
        if (texts.isEmpty()) return "OCR_VACIO"
        val detection = detectionEngine.detect(texts, packageName = packageName)
        if (detection.resolution != DetectionResolution.KEYWORD_CANDIDATE &&
            detection.resolution != DetectionResolution.PACKAGE_MATCH
        ) {
            return "NO_PLATAFORMA (${detection.resolution.name})"
        }
        if (!detection.screenDetection.isRequest) {
            return "PANTALLA_NO_REQUEST (${detection.screenDetection.type.name})"
        }
        return "PARSE_FALLIDO (sin monto/plataforma extraíbles)"
    }

    /** Paquete real de la app según el nombre de la muestra (Uber/InDriver). */
    private fun packageFor(fileName: String): String =
        if (fileName.startsWith("uber")) PACKAGE_UBER else PACKAGE_INDRIVER

    private fun logOcrTexts(
        name: String,
        texts: List<String>,
    ) {
        Log.i(TAG, "IMG=$name OCR(textos=${texts.size}):")
        texts.forEach { Log.i(TAG, "  | $it") }
    }

    private fun elapsedMillis(startNanos: Long): Double = (System.nanoTime() - startNanos) / NANOS_PER_MILLI

    private fun format(value: Double): String = "%.1f".format(value)

    private companion object {
        const val TAG = "SIRC-OCR-TEST"
        const val ACTION_OCR_TEST = "com.sirc.debug.OCR_TEST"
        const val ASSET_DIR = "sirc_test"
        val IMAGE_PATTERN = Regex(".*\\.(jpg|jpeg|png)$", RegexOption.IGNORE_CASE)
        const val PACKAGE_UBER = "com.ubercab"
        const val PACKAGE_INDRIVER = "com.leadingsoft.ride.driver"
        const val NANOS_PER_MILLI = 1_000_000.0
    }
}

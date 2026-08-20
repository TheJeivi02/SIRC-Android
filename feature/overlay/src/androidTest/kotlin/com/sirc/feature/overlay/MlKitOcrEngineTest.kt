package com.sirc.feature.overlay

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.sirc.capture.log.SircLogger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.system.measureTimeMillis

/**
 * Instrumented test (device) de [MlKitOcrEngine] sobre ML Kit REAL.
 *
 * Demuestra el contrato real del motor con Bitmap real decodificado de las
 * imágenes del dataset existente (marcadores del pipeline; README del dataset
 * declara que NO sirven para validar precisión). La precisión OCR queda
 * NOT_VALIDATED: solo se valida que ML Kit inicializa, procesa una imagen
 * válida, devuelve resultado y maneja el decode nulo.
 */
@RunWith(AndroidJUnit4::class)
class MlKitOcrEngineTest {
    private val logger = NoOpSircLogger
    private val engine = MlKitOcrEngine(logger)
    private val assets = InstrumentationRegistry.getInstrumentation().context.assets

    private val images =
        listOf(
            "offer_uberx_1.png",
            "offer_comfort_1.png",
            "offer_moto_1.png",
            "offer_xl_1.png",
            "offer_reservation_1.png",
            "offer_radar_1.png",
            "offer_bonus_1.png",
            "offer_night_1.png",
            "offer_invalid_1.png",
            "offer_cabify_1.png",
            "offer_didi_1.png",
            "offer_indrive_1.png",
            "offer_uber_1.png",
        )

    @Test
    fun imagenesReales_seProcesanSinExcepcionYDevuelvenResultado() {
        val results = mutableListOf<Pair<String, List<String>>>()
        var total = 0L

        for (name in images) {
            val bytes = assets.open(name).readBytes()
            var result: List<String>? = null
            val elapsed = measureTimeMillis { result = runBlockingRecognize(bytes) }
            total += elapsed
            assertNotNull("OCR lanzó null para $name", result)
            results += name to result.orEmpty()
            Log.i(TAG, "OCR[$name]: ${result.orEmpty().size} líneas en ${elapsed}ms")
        }

        Log.i(TAG, "OCR total dataset: ${total}ms para ${images.size} imágenes")
        // Evidencia de la matriz por imagen (se documenta en G10_INSTRUMENTED_VALIDATION).
        results.forEach { (name, lines) -> Log.i(TAG, "OCR_MATRIZ[$name]=${lines.size}") }
        assertTrue(results.isNotEmpty())
    }

    @Test
    fun decodeInvalido_devuelveListaVaciaSinLanzar() {
        // Camino real: BitmapFactory.decodeByteArray devuelve null → contrato vacío.
        val invalid = "esto-no-es-una-imagen".toByteArray()
        val result = runBlockingRecognize(invalid)

        assertEquals(emptyList<String>(), result)
    }

    private fun runBlockingRecognize(bytes: ByteArray): List<String> {
        var result: List<String> = emptyList()
        kotlinx.coroutines.runBlocking { result = engine.recognize(bytes) }
        return result
    }

    private object NoOpSircLogger : SircLogger {
        override fun debug(
            tag: String,
            message: String,
        ) = Unit

        override fun info(
            tag: String,
            message: String,
        ) = Unit

        override fun warn(
            tag: String,
            message: String,
        ) = Unit

        override fun error(
            tag: String,
            message: String,
        ) = Unit
    }

    private companion object {
        const val TAG = "G10OcrTest"
    }
}

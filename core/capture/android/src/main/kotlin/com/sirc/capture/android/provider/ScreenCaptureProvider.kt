package com.sirc.capture.android.provider

import android.content.Intent
import android.graphics.Bitmap
import kotlinx.coroutines.flow.StateFlow

/**
 * Gestor de la sesión de captura de pantalla (MediaProjection).
 *
 * Independiente de la UI: posee el token de proyección, el virtual display y
 * el último frame capturado. La UI solo le entrega el resultado del permiso y
 * lee [isProjecting].
 */
interface ScreenCaptureProvider {
    /** `true` mientras la proyección de pantalla esté activa. */
    val isProjecting: StateFlow<Boolean>

    /**
     * Entrega el resultado del consentimiento de captura de pantalla.
     *
     * En Android 14+ esto arranca el Foreground Service de tipo
     * `mediaProjection` antes de pedir el token; el servicio lo completa.
     */
    fun onProjectionPermissionGranted(
        resultCode: Int,
        data: Intent?,
    )

    /** Detiene la proyección, el virtual display y el servicio asociado. */
    fun stopProjection()

    /** Devuelve el último frame de pantalla como [Bitmap], o null si no hay. */
    suspend fun captureFrame(): Bitmap?
}

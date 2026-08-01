package com.sirc.feature.overlay

import android.content.Intent
import com.sirc.capture.android.provider.ScreenCaptureProvider
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Fachada de alto nivel del overlay para las ViewModels (Home, Diagnóstico).
 *
 * Combina el estado de ejecución del servicio ([OverlayController]) con los
 * permisos y ajustes ([PermissionManager]) y el control de la captura de
 * pantalla ([ScreenCaptureProvider]). La UI nunca toca Android directo.
 */
interface OverlayManager {
    val isRunning: StateFlow<Boolean>

    val projectionActive: StateFlow<Boolean>

    fun start()

    fun stop()

    fun isOverlayPermissionGranted(): Boolean

    fun isAccessibilityEnabled(): Boolean

    fun openOverlayPermissionSettings()

    fun openAccessibilitySettings()

    /** Intent de consentimiento del sistema para capturar la pantalla. */
    fun createScreenCaptureIntent(): Intent

    /** Entrega el resultado del consentimiento de captura de pantalla. */
    fun startProjection(
        resultCode: Int,
        data: Intent?,
    )

    fun stopProjection()
}

@Singleton
class AndroidOverlayManager @Inject constructor(
    private val controller: OverlayController,
    private val permissions: PermissionManager,
    private val screenCaptureProvider: ScreenCaptureProvider,
    private val mediaProjectionManagerFactory: MediaProjectionManagerFactory,
) : OverlayManager {
    override val isRunning: StateFlow<Boolean> = controller.isRunning

    override val projectionActive: StateFlow<Boolean> = screenCaptureProvider.isProjecting

    override fun start() = controller.start()

    override fun stop() = controller.stop()

    override fun isOverlayPermissionGranted(): Boolean = permissions.hasOverlayPermission()

    override fun isAccessibilityEnabled(): Boolean = permissions.hasAccessibilityPermission()

    override fun openOverlayPermissionSettings() = permissions.openOverlaySettings()

    override fun openAccessibilitySettings() = permissions.openAccessibilitySettings()

    override fun createScreenCaptureIntent(): Intent =
        mediaProjectionManagerFactory.create().createScreenCaptureIntent()

    override fun startProjection(
        resultCode: Int,
        data: Intent?,
    ) {
        screenCaptureProvider.onProjectionPermissionGranted(resultCode, data)
    }

    override fun stopProjection() = screenCaptureProvider.stopProjection()
}

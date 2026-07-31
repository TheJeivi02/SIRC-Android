package com.sirc.feature.overlay

import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Fachada de alto nivel del overlay para las ViewModels (Home, Diagnóstico).
 *
 * Combina el estado de ejecución del servicio ([OverlayController]) con los
 * permisos y ajustes ([PermissionManager]). La UI nunca toca Android directo.
 */
interface OverlayManager {
    val isRunning: StateFlow<Boolean>

    fun start()

    fun stop()

    fun isOverlayPermissionGranted(): Boolean

    fun isAccessibilityEnabled(): Boolean

    fun openOverlayPermissionSettings()

    fun openAccessibilitySettings()
}

@Singleton
class AndroidOverlayManager @Inject constructor(
    private val controller: OverlayController,
    private val permissions: PermissionManager,
) : OverlayManager {
    override val isRunning: StateFlow<Boolean> = controller.isRunning

    override fun start() = controller.start()

    override fun stop() = controller.stop()

    override fun isOverlayPermissionGranted(): Boolean = permissions.hasOverlayPermission()

    override fun isAccessibilityEnabled(): Boolean = permissions.hasAccessibilityPermission()

    override fun openOverlayPermissionSettings() = permissions.openOverlaySettings()

    override fun openAccessibilitySettings() = permissions.openAccessibilitySettings()
}

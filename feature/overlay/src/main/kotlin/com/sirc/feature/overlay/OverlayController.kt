package com.sirc.feature.overlay

import com.sirc.capture.log.SircLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Controlador de bajo nivel del Overlay: arranca/para el [OverlayService] (vía
 * [OverlayServiceLauncher]) y expone el estado de ejecución.
 *
 * La detección de permisos y la apertura de ajustes viven en
 * [PermissionManager]; este controlador solo orquesta el servicio.
 *
 * `isRunning` se actualiza al arrancar/parar y se corrige con el estado real
 * que reporta el servicio ([onServiceRunning]): si el sistema (p. ej. XOS) o
 * un error de ventana terminan el FGS, la UI deja de decir "en ejecución".
 */
@Singleton
class OverlayController @Inject constructor(
    private val launcher: OverlayServiceLauncher,
    private val permissions: PermissionManager,
    private val logger: SircLogger,
) {
    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    fun start() {
        if (!permissions.hasOverlayPermission()) return
        _isRunning.value = true
        try {
            launcher.start()
        } catch (error: Exception) {
            logger.error(TAG, "no se pudo arrancar el overlay: ${error.message}")
            _isRunning.value = false
        }
    }

    fun stop() {
        _isRunning.value = false
        launcher.stop()
    }

    /** Estado real del servicio (arrancó o murió) para corregir `isRunning`. */
    fun onServiceRunning(running: Boolean) {
        _isRunning.value = running
    }

    companion object {
        private const val TAG = "OverlayController"
    }
}

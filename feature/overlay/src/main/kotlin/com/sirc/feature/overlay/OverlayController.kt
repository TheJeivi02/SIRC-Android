package com.sirc.feature.overlay

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Controlador de bajo nivel del Overlay: arranca/para el [OverlayService] y
 * expone el estado de ejecución.
 *
 * La detección de permisos y la apertura de ajustes viven en
 * [PermissionManager]; este controlador solo orquesta el servicio.
 */
@Singleton
class OverlayController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val permissions: PermissionManager,
) {
    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    fun start() {
        if (!permissions.hasOverlayPermission()) return
        _isRunning.value = true
        OverlayService.start(context)
    }

    fun stop() {
        _isRunning.value = false
        OverlayService.stop(context)
    }
}

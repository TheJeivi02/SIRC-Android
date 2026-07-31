package com.sirc.app

import androidx.lifecycle.ViewModel
import com.sirc.feature.overlay.OverlayManager
import com.sirc.feature.overlay.PermissionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/** Estado de los 5 requisitos del overlay para la pantalla de Diagnóstico. */
@HiltViewModel
class DiagnosisViewModel @Inject constructor(
    private val overlayManager: OverlayManager,
    private val permissions: PermissionManager,
) : ViewModel() {
    data class UiState(
        val overlayPermissionGranted: Boolean = false,
        val accessibilityEnabled: Boolean = false,
        val overlayRunning: Boolean = false,
        val notificationsGranted: Boolean = false,
        val batteryOptimizationIgnored: Boolean = false,
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    fun refresh() {
        _state.value =
            UiState(
                overlayPermissionGranted = overlayManager.isOverlayPermissionGranted(),
                accessibilityEnabled = overlayManager.isAccessibilityEnabled(),
                overlayRunning = overlayManager.isRunning.value,
                notificationsGranted = permissions.hasNotificationPermission(),
                batteryOptimizationIgnored = permissions.isIgnoringBatteryOptimizations(),
            )
    }

    fun startOverlay() {
        overlayManager.start()
        refresh()
    }

    fun stopOverlay() {
        overlayManager.stop()
        refresh()
    }

    fun openOverlaySettings() = overlayManager.openOverlayPermissionSettings()

    fun openAccessibilitySettings() = overlayManager.openAccessibilitySettings()

    fun openNotificationSettings() = permissions.openNotificationSettings()

    fun openBatterySettings() = permissions.openBatteryOptimizationSettings()
}

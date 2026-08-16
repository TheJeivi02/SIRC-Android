package com.sirc.feature.overlay

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import com.sirc.capture.di.CaptureRequests
import com.sirc.capture.model.CaptureRequest
import com.sirc.capture.pipeline.CapturePipeline
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Accessibility Service dedicado a la captura de ofertas.
 *
 * Único servicio de accesibilidad registrado en producción (WP-E1-03).
 *
 * Adaptador Android delgado: delega la lógica de observación en
 * [AccessibilityCaptureInput] y colecciona la corriente compuesta de requests
 * ([@CaptureRequests], accesibilidad + MediaProjection) encadenándolos al
 * [CapturePipeline]. No conoce la UI ni el overlay.
 */
@AndroidEntryPoint
class CaptureAccessibilityService : AccessibilityService() {
    @Inject lateinit var captureInput: AccessibilityCaptureInput

    @Inject lateinit var pipeline: CapturePipeline

    @Inject
    @CaptureRequests
    lateinit var captureRequests: Flow<CaptureRequest>

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        scope.launch {
            captureRequests.collect { request -> pipeline.process(request) }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        captureInput.onAccessibilityEvent(event, rootInActiveWindow)
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}

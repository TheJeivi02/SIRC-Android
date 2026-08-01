package com.sirc.feature.overlay

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.ViewGroup
import android.view.WindowManager
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.content.ContextCompat
import com.sirc.core.ui.theme.SircTheme
import com.sirc.domain.engine.ProfitEngine
import com.sirc.domain.model.OverlayConfig
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Foreground Service que dibuja el Overlay (TYPE_APPLICATION_OVERLAY).
 *
 * El overlay es una ventana Compose liviana: un solo [ComposeView] que se
 * agrega/retira de WindowManager según el estado de [OverlayDataSource]. No
 * mantiene Views de más, minimizando memoria.
 */
@AndroidEntryPoint
class OverlayService : Service() {
    @Inject lateinit var dataSource: OverlayDataSource

    @Inject lateinit var engine: ProfitEngine

    /**
     * Mantiene la persistencia del historial a partir del flujo real de
     * accesibilidad; el overlay consume [OverlayDataSource] (estado real del
     * pipeline + evaluación).
     */
    @Inject lateinit var evaluator: OfferEvaluator

    private var windowManager: WindowManager? = null
    private var overlayView: ComposeView? = null
    private var windowParams: WindowManager.LayoutParams? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        startForeground(NOTIFICATION_ID, buildNotification())
        if (!Settings.canDrawOverlays(this)) {
            stopSelf()
            return START_NOT_STICKY
        }
        ensureOverlay()
        dataSource.start()
        return START_STICKY
    }

    override fun onDestroy() {
        overlayView?.let { view ->
            runCatching { windowManager?.removeView(view) }
        }
        overlayView = null
        windowParams = null
        dataSource.stop()
        scope.cancel()
        super.onDestroy()
    }

    private fun ensureOverlay() {
        if (overlayView != null) return

        val wm = getSystemService(WINDOW_SERVICE) as WindowManager
        windowManager = wm

        val config = dataSource.uiState.value.config
        val params = buildWindowParams(config)
        windowParams = params

        val view =
            ComposeView(this).apply {
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
                setContent {
                    SircTheme {
                        Box {
                            val state by dataSource.uiState.collectAsState()
                            OverlayContent(
                                state = state,
                                engine = engine,
                                onDismiss = { stopSelf() },
                                onDrag = { dx, dy -> moveOverlay(dx, dy) },
                            )
                        }
                    }
                }
            }
        overlayView = view

        scope.launch {
            dataSource.uiState.collect { state ->
                val attached = view.parent != null
                val shouldShow = state.visible
                when {
                    shouldShow && !attached -> wm.addView(view, params)
                    !shouldShow && attached -> wm.removeView(view)
                    else -> Unit
                }
            }
        }
    }

    private fun moveOverlay(
        deltaX: Int,
        deltaY: Int,
    ) {
        val wm = windowManager ?: return
        val params = windowParams ?: return
        val view = overlayView ?: return
        val display = wm.defaultDisplay
        val metrics = android.util.DisplayMetrics().also { display.getRealMetrics(it) }
        params.x = (params.x + deltaX).coerceIn(0, metrics.widthPixels - params.width)
        params.y = (params.y + deltaY).coerceIn(0, metrics.heightPixels - params.height)
        runCatching { wm.updateViewLayout(view, params) }
    }

    private fun buildWindowParams(config: OverlayConfig): WindowManager.LayoutParams {
        val display = (getSystemService(WINDOW_SERVICE) as WindowManager).defaultDisplay
        val metrics = android.util.DisplayMetrics().also { display.getRealMetrics(it) }
        val width = (metrics.widthPixels * 0.82f).toInt()

        return WindowManager.LayoutParams(
            width,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            val maxX = metrics.widthPixels - width
            x = ((metrics.widthPixels - width) * (config.positionXPercent / 100f)).toInt().coerceIn(0, maxX)
            y = (metrics.heightPixels * (config.positionYPercent / 100f)).toInt()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel =
                NotificationChannel(
                    CHANNEL_ID,
                    getString(R.string.overlay_channel_name),
                    NotificationManager.IMPORTANCE_LOW,
                ).apply {
                    description = getString(R.string.overlay_channel_description)
                }
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val builder =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Notification.Builder(this, CHANNEL_ID)
            } else {
                @Suppress("DEPRECATION")
                Notification.Builder(this)
            }
        return builder
            .setSmallIcon(R.drawable.ic_stat_sirc)
            .setContentTitle(getString(R.string.overlay_notification_title))
            .setContentText(getString(R.string.overlay_notification_text))
            .setOngoing(true)
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "sirc_overlay"
        private const val NOTIFICATION_ID = 9001

        fun start(context: Context) {
            ContextCompat.startForegroundService(context, Intent(context, OverlayService::class.java))
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, OverlayService::class.java))
        }
    }
}

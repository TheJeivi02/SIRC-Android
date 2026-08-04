package com.sirc.feature.overlay

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
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
import java.lang.reflect.Method

/**
 * Foreground Service que dibuja el Overlay (TYPE_APPLICATION_OVERLAY).
 *
 * El overlay es una ventana Compose liviana que se agrega una sola vez al
 * [WindowManager]. Mostrar/ocultar se hace animando la opacidad desde Compose
 * y marcando la ventana como [WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE]
 * cuando está oculta (para no bloquear los toques sobre la app de la
 * plataforma). Así se evita el parpadeo de agregar/quitar la vista en cada
 * oferta.
 */
@AndroidEntryPoint
class OverlayService : Service() {
    @Inject lateinit var dataSource: OverlayDataSource

    @Inject lateinit var engine: ProfitEngine

    private var windowManager: WindowManager? = null
    private var overlayView: ComposeView? = null
    private var windowParams: WindowManager.LayoutParams? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private lateinit var lifecycleRegistry: LifecycleRegistry
    private val viewModelStore = ViewModelStore()

    init {
        lifecycleRegistry = LifecycleRegistry.createUnsafe(
            object : LifecycleOwner {
                override val lifecycle: Lifecycle
                    get() = lifecycleRegistry
            }
        )
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        createNotificationChannel()
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        startForeground(NOTIFICATION_ID, buildNotification())
        if (!Settings.canDrawOverlays(this)) {
            stopSelf()
            return START_NOT_STICKY
        }
        ensureOverlay()
        dataSource.start()
        return START_STICKY
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        reclampOverlay()
    }

    override fun onDestroy() {
        overlayView?.let { view ->
            runCatching { windowManager?.removeView(view) }
        }
        overlayView = null
        windowParams = null
        dataSource.stop()
        scope.cancel()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
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
                // Set ViewTree owners via reflection to avoid KMP metadata compile-time issues
                try {
                    val vtlClass = Class.forName("androidx.lifecycle.ViewTreeLifecycleOwner")
                    val setMethod = vtlClass.getMethod("set", View::class.java, LifecycleOwner::class.java)
                    setMethod.invoke(null, this, lifecycleRegistry)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                try {
                    val vtvsClass = Class.forName("androidx.lifecycle.ViewTreeViewModelStoreOwner")
                    val setMethod = vtvsClass.getMethod("set", View::class.java, ViewModelStoreOwner::class.java)
                    val vmOwner = object : ViewModelStoreOwner {
                        override val viewModelStore: ViewModelStore = this@OverlayService.viewModelStore
                    }
                    setMethod.invoke(null, this, vmOwner)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
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
        runCatching { wm.addView(view, params) }

        scope.launch {
            dataSource.uiState.collect { state ->
                applyVisibility(state.visible)
            }
        }
    }

    private fun applyVisibility(visible: Boolean) {
        val view = overlayView ?: return
        val params = windowParams ?: return
        if (view.parent == null) return
        params.flags =
            if (visible) {
                BASE_FLAGS
            } else {
                BASE_FLAGS or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
            }
        runCatching { windowManager?.updateViewLayout(view, params) }
    }

    /** Reajusta tamaño y posición tras rotación / cambio de resolución / split. */
    private fun reclampOverlay() {
        val wm = windowManager ?: return
        val params = windowParams ?: return
        val view = overlayView ?: return
        val bounds = screenBounds()
        params.width = (bounds.width() * OVERLAY_WIDTH_RATIO).toInt()
        val maxX = (bounds.width() - params.width).coerceAtLeast(0)
        params.x = params.x.coerceIn(0, maxX)
        params.y = params.y.coerceIn(0, bounds.height() - params.height)
        runCatching { wm.updateViewLayout(view, params) }
    }

    private fun moveOverlay(
        deltaX: Int,
        deltaY: Int,
    ) {
        val wm = windowManager ?: return
        val params = windowParams ?: return
        val view = overlayView ?: return
        val bounds = screenBounds()
        params.x = (params.x + deltaX).coerceIn(0, (bounds.width() - params.width).coerceAtLeast(0))
        params.y = (params.y + deltaY).coerceIn(0, (bounds.height() - params.height).coerceAtLeast(0))
        runCatching { wm.updateViewLayout(view, params) }
    }

    private fun buildWindowParams(config: OverlayConfig): WindowManager.LayoutParams {
        val bounds = screenBounds()
        val width = (bounds.width() * OVERLAY_WIDTH_RATIO).toInt()

        return WindowManager.LayoutParams(
            width,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            BASE_FLAGS,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            val maxX = (bounds.width() - width).coerceAtLeast(0)
            x = ((bounds.width() - width) * (config.positionXPercent / 100f)).toInt().coerceIn(0, maxX)
            y = (bounds.height() * (config.positionYPercent / 100f)).toInt()
        }
    }

    /**
     * Bounds de la pantalla usando `WindowMetrics` (API 30+) y el fallback
     * clásico de [android.view.Display] para API 24-29. Evita las APIs
     * deprecadas en Android 15.
     */
    private fun screenBounds(): Rect {
        val wm = getSystemService(WINDOW_SERVICE) as WindowManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return wm.currentWindowMetrics.bounds
        }
        @Suppress("DEPRECATION")
        val metrics = android.util.DisplayMetrics().also { wm.defaultDisplay.getRealMetrics(it) }
        return Rect(0, 0, metrics.widthPixels, metrics.heightPixels)
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
        private const val OVERLAY_WIDTH_RATIO = 0.82f

        private val BASE_FLAGS =
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS

        fun start(context: Context) {
            ContextCompat.startForegroundService(context, Intent(context, OverlayService::class.java))
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, OverlayService::class.java))
        }
    }
}

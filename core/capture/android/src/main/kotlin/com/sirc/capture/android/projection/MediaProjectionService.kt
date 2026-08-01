package com.sirc.capture.android.projection

import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.content.ContextCompat
import com.sirc.capture.android.R
import com.sirc.capture.android.provider.MediaProjectionScreenCaptureProvider
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Foreground Service que mantiene viva la proyección de pantalla.
 *
 * En Android 14+ la app debe correr un FGS de tipo `mediaProjection` antes de
 * llamar a `getMediaProjection()`. Este servicio se inicia al conceder el
 * permiso y delega la creación de la proyección al
 * [MediaProjectionScreenCaptureProvider].
 */
@AndroidEntryPoint
class MediaProjectionService : Service() {
    @Inject lateinit var provider: MediaProjectionScreenCaptureProvider

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
        when (intent?.action) {
            ACTION_START -> {
                val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, Activity.RESULT_CANCELED)
                val data = intent.parcelableExtraCompat(EXTRA_RESULT_DATA)
                if (resultCode != Activity.RESULT_OK || data == null) {
                    stopSelf()
                    return START_NOT_STICKY
                }
                startForeground(NOTIFICATION_ID, buildNotification())
                provider.initializeProjection(resultCode, data)
                return START_NOT_STICKY
            }
        }
        return START_NOT_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel =
                NotificationChannel(
                    CHANNEL_ID,
                    getString(R.string.capture_channel_name),
                    NotificationManager.IMPORTANCE_LOW,
                ).apply {
                    description = getString(R.string.capture_channel_description)
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
            .setSmallIcon(R.drawable.ic_stat_capture)
            .setContentTitle(getString(R.string.capture_notification_title))
            .setContentText(getString(R.string.capture_notification_text))
            .setOngoing(true)
            .build()
    }

    private fun Intent?.parcelableExtraCompat(key: String): Intent? {
        if (this == null) return null
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getParcelableExtra(key, Intent::class.java)
        } else {
            @Suppress("DEPRECATION")
            getParcelableExtra(key)
        }
    }

    companion object {
        private const val TAG = "MediaProjectionService"
        private const val CHANNEL_ID = "sirc_capture"
        private const val NOTIFICATION_ID = 9002

        const val ACTION_START = "com.sirc.capture.START"
        const val EXTRA_RESULT_CODE = "result_code"
        const val EXTRA_RESULT_DATA = "result_data"

        fun start(
            context: Context,
            resultCode: Int,
            data: Intent,
        ) {
            val intent =
                Intent(context, MediaProjectionService::class.java)
                    .setAction(ACTION_START)
                    .putExtra(EXTRA_RESULT_CODE, resultCode)
                    .putExtra(EXTRA_RESULT_DATA, data)
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, MediaProjectionService::class.java))
        }
    }
}

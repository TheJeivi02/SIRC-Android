package com.sirc.feature.overlay

import android.Manifest
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Detección y apertura de ajustes para todos los permisos que SIRC necesita.
 *
 * Fuente única de verdad de permisos: lo consumen [OverlayManager],
 * `HomeViewModel` y `DiagnosisViewModel`. Evita lógica duplicada.
 */
interface PermissionManager {
    fun hasOverlayPermission(): Boolean

    fun hasAccessibilityPermission(): Boolean

    /** En Android < 13 siempre se considera concedido. */
    fun hasNotificationPermission(): Boolean

    /** `true` si la app está exenta de la optimización de batería. */
    fun isIgnoringBatteryOptimizations(): Boolean

    fun openOverlaySettings()

    fun openAccessibilitySettings()

    fun openNotificationSettings()

    fun openBatteryOptimizationSettings()
}

@Singleton
class AndroidPermissionManager @Inject constructor(
    @ApplicationContext private val context: Context,
) : PermissionManager {
    override fun hasOverlayPermission(): Boolean = Settings.canDrawOverlays(context)

    override fun hasAccessibilityPermission(): Boolean {
        val manager = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        return manager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
            .any { info ->
                info.resolveInfo.serviceInfo.packageName == context.packageName &&
                    info.resolveInfo.serviceInfo.name == CaptureAccessibilityService::class.java.name
            }
    }

    override fun hasNotificationPermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
    }

    override fun isIgnoringBatteryOptimizations(): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return powerManager.isIgnoringBatteryOptimizations(context.packageName)
    }

    override fun openOverlaySettings() {
        val intent =
            Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${context.packageName}"),
            )
        startSettings(intent)
    }

    override fun openAccessibilitySettings() {
        startSettings(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    }

    override fun openNotificationSettings() {
        val intent =
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        startSettings(intent)
    }

    override fun openBatteryOptimizationSettings() {
        startSettings(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
    }

    private fun startSettings(intent: Intent) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
    }
}

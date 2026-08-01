package com.sirc.feature.overlay

import android.content.Context
import android.content.pm.ApplicationInfo
import android.util.Log
import com.sirc.capture.flag.FeatureFlag
import com.sirc.capture.flag.FeatureFlags
import com.sirc.capture.log.SircLogger
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Logger centralizado por niveles.
 *
 * - ERROR/WARNING: siempre disponibles (también en Release) para diagnosticar
 *   incidencias de campo en logcat sin afectar al usuario.
 * - INFO: solo en builds de desarrollo (debbugables).
 * - DEBUG: solo en builds de desarrollo y con el flag beta
 *   [FeatureFlag.DETAILED_LOGS] activo.
 */
@Singleton
class AndroidSircLogger @Inject constructor(
    @ApplicationContext context: Context,
    private val featureFlags: FeatureFlags,
) : SircLogger {
    private val debuggable = (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0

    override fun debug(
        tag: String,
        message: String,
    ) {
        if (debuggable && featureFlags.isEnabled(FeatureFlag.DETAILED_LOGS)) Log.d(tag, message)
    }

    override fun info(
        tag: String,
        message: String,
    ) {
        if (debuggable) Log.i(tag, message)
    }

    override fun warn(
        tag: String,
        message: String,
    ) {
        Log.w(tag, message)
    }

    override fun error(
        tag: String,
        message: String,
    ) {
        Log.e(tag, message)
    }
}

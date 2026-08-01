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
 * Logger centralizado que solo emite en builds de desarrollo, listo para
 * deshabilitarse por completo en producción.
 *
 * El flag beta [FeatureFlag.DETAILED_LOGS] controla los logs de depuración:
 * con él desactivado se reduce la escritura de logs y el consumo asociado.
 */
@Singleton
class AndroidSircLogger @Inject constructor(
    @ApplicationContext context: Context,
    private val featureFlags: FeatureFlags,
) : SircLogger {
    private val enabled = (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0

    override fun debug(
        tag: String,
        message: String,
    ) {
        if (enabled && featureFlags.isEnabled(FeatureFlag.DETAILED_LOGS)) Log.d(tag, message)
    }

    override fun info(
        tag: String,
        message: String,
    ) {
        if (enabled) Log.i(tag, message)
    }

    override fun warn(
        tag: String,
        message: String,
    ) {
        if (enabled) Log.w(tag, message)
    }

    override fun error(
        tag: String,
        message: String,
    ) {
        if (enabled) Log.e(tag, message)
    }
}

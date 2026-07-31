package com.sirc.feature.overlay

import android.content.Context
import android.content.pm.ApplicationInfo
import android.util.Log
import com.sirc.capture.log.SircLogger
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Logger centralizado que solo emite en builds de desarrollo, listo para
 * deshabilitarse por completo en producción.
 */
@Singleton
class AndroidSircLogger @Inject constructor(
    @ApplicationContext context: Context,
) : SircLogger {
    private val enabled = (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0

    override fun debug(
        tag: String,
        message: String,
    ) {
        if (enabled) Log.d(tag, message)
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

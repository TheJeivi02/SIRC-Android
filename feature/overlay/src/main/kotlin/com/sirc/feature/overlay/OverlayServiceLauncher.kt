package com.sirc.feature.overlay

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Indirección sobre el arranque/parada del [OverlayService] para que
 * [OverlayController] sea testeable sin Android.
 */
interface OverlayServiceLauncher {
    fun start()

    fun stop()
}

@Singleton
class AndroidOverlayServiceLauncher @Inject constructor(
    @ApplicationContext private val context: Context,
) : OverlayServiceLauncher {
    override fun start() = OverlayService.start(context)

    override fun stop() = OverlayService.stop(context)
}

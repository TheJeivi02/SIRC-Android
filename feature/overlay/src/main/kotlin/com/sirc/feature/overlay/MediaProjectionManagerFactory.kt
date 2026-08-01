package com.sirc.feature.overlay

import android.content.Context
import android.media.projection.MediaProjectionManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** Crea el [MediaProjectionManager] del sistema sin acoplar la fachada al servicio Android. */
@Singleton
class MediaProjectionManagerFactory @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun create(): MediaProjectionManager =
        context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
}

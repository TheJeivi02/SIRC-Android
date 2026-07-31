package com.sirc.app

import android.app.Application
import com.sirc.capture.coordinator.OfferCaptureCoordinator
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class SircApplication : Application() {
    @Inject lateinit var captureCoordinator: OfferCaptureCoordinator

    override fun onCreate() {
        super.onCreate()
        captureCoordinator.start()
    }
}

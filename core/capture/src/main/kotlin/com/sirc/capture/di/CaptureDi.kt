package com.sirc.capture.di

import javax.inject.Qualifier

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AccessibilityRequests

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class CaptureRequests

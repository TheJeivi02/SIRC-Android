package com.sirc.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sirc.feature.onboarding.OnboardingScreen

/**
 * Raíz de la aplicación: muestra el onboarding la primera vez y la app
 * principal cuando el conductor ya completó su configuración inicial.
 */
@Composable
fun SircRoot(viewModel: RootViewModel = hiltViewModel()) {
    val isConfigured by viewModel.isConfigured.collectAsStateWithLifecycle()

    when (isConfigured) {
        null -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        }
        false -> OnboardingScreen()
        true -> SircApp()
    }
}

package com.sirc.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sirc.core.ui.components.SectionCard
import com.sirc.core.ui.components.StatusDot

@Composable
fun HomeScreen(viewModel: HomeViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Refrescar el estado al volver de las pantallas de permisos.
    DisposableEffect(lifecycleOwner) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) viewModel.refresh()
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Solicitar permiso de notificaciones en Android 13+ (notificación del FGS).
    val notifLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission(),
        ) {}
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // Consentimiento del sistema para capturar la pantalla (MediaProjection).
    val projectionLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.StartActivityForResult(),
        ) { result ->
            viewModel.onProjectionPermissionGranted(result.resultCode, result.data)
        }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SectionCard(title = "Estado del sistema") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(text = "Permiso de overlay", style = MaterialTheme.typography.bodyMedium)
                StatusDot(active = state.overlayPermissionGranted)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(text = "Servicio de accesibilidad", style = MaterialTheme.typography.bodyMedium)
                StatusDot(active = state.accessibilityEnabled)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(text = "Notificaciones (FGS)", style = MaterialTheme.typography.bodyMedium)
                StatusDot(active = state.notificationsGranted)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(text = "Optimización de batería", style = MaterialTheme.typography.bodyMedium)
                StatusDot(active = state.batteryOptimizationIgnored)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(text = "Overlay en ejecución", style = MaterialTheme.typography.bodyMedium)
                StatusDot(active = state.overlayRunning)
            }
        }

        if (!state.batteryOptimizationIgnored) {
            SectionCard(title = "Batería") {
                Text(
                    text =
                        "Eximir a SIRC de la optimización de batería evita que el " +
                            "sistema cierre el overlay en segundo plano.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = viewModel::openBatterySettings,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Eximir de optimización")
                }
            }
        }

        SectionCard(title = "Overlay") {
            if (!state.overlayPermissionGranted) {
                Button(
                    onClick = viewModel::requestOverlayPermission,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Permitir dibujar sobre otras apps")
                }
            } else {
                Button(
                    onClick = viewModel::startOverlay,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Iniciar overlay")
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = viewModel::stopOverlay,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Detener overlay")
                }
            }
        }

        SectionCard(title = "Accesibilidad") {
            if (!state.accessibilityEnabled) {
                Text(
                    text = "Necesario para leer las ofertas visibles de Uber, DiDi, Cabify e InDrive.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = viewModel::openAccessibilitySettings,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Activar accesibilidad")
                }
            } else {
                Text(
                    text = "Servicio activo. SIRC leerá las ofertas visibles y calculará la rentabilidad.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        SectionCard(title = "Captura de pantalla") {
            if (!state.projectionActive) {
                Text(
                    text =
                        "Al conceder el permiso, SIRC analiza el contenido visible " +
                            "de las ofertas con OCR para calcular la rentabilidad. " +
                            "Todo el análisis es local, nada sale del dispositivo.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { projectionLauncher.launch(viewModel.createScreenCaptureIntent()) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Permitir captura de pantalla")
                }
            } else {
                Text(
                    text = "Captura activa. SIRC analiza las ofertas visibles en menos de 3 segundos.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = viewModel::stopProjection,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Detener captura")
                }
            }
        }

        SectionCard(title = "Cómo funciona") {
            Text(
                text =
                    "1. Activa accesibilidad y overlay.\n" +
                        "2. Abre tu app de transporte.\n" +
                        "3. Cuando llegue una oferta, SIRC muestra el indicador " +
                        "de rentabilidad en menos de 3 segundos.\n" +
                        "4. SIRC solo lee la pantalla: nunca toca botones ni decide por ti.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

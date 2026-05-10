package com.clsoft.furbyconnect.presentation

import android.Manifest
import android.content.Context
import android.location.LocationManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import com.clsoft.furbyconnect.domain.model.BleCharacteristic
import com.clsoft.furbyconnect.domain.model.BleDevice
import com.clsoft.furbyconnect.domain.model.BleService

@Composable
fun FurbyBleScreenWithPermissions(
    viewModel: FurbyBleViewModel
) {
    val context = LocalContext.current

    val devices by viewModel.devices.collectAsState()
    val services by viewModel.services.collectAsState()
    val status by viewModel.status.collectAsState()
    val selectedCharacteristic by viewModel.selectedCharacteristic.collectAsState()

    // 1️⃣ Launcher para permisos
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val granted = result.values.all { it }
        if (granted) {
            // Validar ubicación del sistema en Android < 12
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                val locationManager =
                    context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
                val isLocationEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                        locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

                if (!isLocationEnabled) {
                    Toast.makeText(context, "Activa la ubicación para escanear BLE", Toast.LENGTH_LONG).show()
                    return@rememberLauncherForActivityResult
                }
            }
            // Iniciar escaneo
            viewModel.scanDevices()
        } else {
            Toast.makeText(context, "Permisos de Bluetooth y ubicación requeridos", Toast.LENGTH_LONG).show()
        }
    }

    // 2️⃣ Pedir permisos en LaunchedEffect
    LaunchedEffect(Unit) {
        val permissions = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.add(Manifest.permission.BLUETOOTH_SCAN)
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION) // para compatibilidad con MAC aleatoria
        } else {
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
            permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION) // algunos dispositivos lo requieren
        }

        launcher.launch(permissions.toTypedArray())
    }

    // 3️⃣ UI principal
    FurbyBleScreen(
        devices = devices,
        services = services,
        statusMessage = status,
        selectedCharacteristic = selectedCharacteristic,
        onStartScan = { viewModel.scanDevices() },
        onStopScan = { viewModel.stopScan() },
        onConnect = { device -> viewModel.connect(device) },
        onSelectCharacteristic = { char -> viewModel.selectCharacteristic(char) },
        onSendHexCommand = { hex ->
            selectedCharacteristic?.let { char ->
                viewModel.sendCommand(char.serviceUuid, char.characteristicUuid, hex)
            }
        }
    )
}
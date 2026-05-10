package com.clsoft.furbyconnect.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clsoft.furbyconnect.domain.model.BleCharacteristic
import com.clsoft.furbyconnect.domain.model.BleDevice
import com.clsoft.furbyconnect.domain.model.BleService
import com.clsoft.furbyconnect.domain.usecase.ConnectToDeviceUseCase
import com.clsoft.furbyconnect.domain.usecase.DiscoverServicesUseCase
import com.clsoft.furbyconnect.domain.usecase.ScanBleDevicesUseCase
import com.clsoft.furbyconnect.domain.usecase.SendCommandUseCase
import com.clsoft.furbyconnect.domain.usecase.StopScanBleDevicesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FurbyBleViewModel @Inject constructor(
    private val scanUseCase: ScanBleDevicesUseCase,
    private val stopScanUseCase: StopScanBleDevicesUseCase,
    private val connectUseCase: ConnectToDeviceUseCase,
    private val discoverServicesUseCase: DiscoverServicesUseCase,
    private val sendCommandUseCase: SendCommandUseCase
) : ViewModel() {

    val devices = MutableStateFlow<List<BleDevice>>(emptyList())
    val services = MutableStateFlow<List<BleService>>(emptyList())
    val status = MutableStateFlow("Idle")
    val selectedCharacteristic = MutableStateFlow<BleCharacteristic?>(null)

    fun scanDevices() = viewModelScope.launch {
        runCatching {
            status.value = "Buscando..."
            devices.value = scanUseCase()
            status.value = if (devices.value.isEmpty()) "Scan completado: sin dispositivos" else "Scan completado"
        }.onFailure {
            status.value = "Error durante el escaneo"
        }
    }

    fun stopScan() {
        stopScanUseCase()
        status.value = "Escaneo detenido"
    }

    fun connect(device: BleDevice) = viewModelScope.launch {
        runCatching {
            status.value = "Conectando a ${device.name}..."
            connectUseCase(device)
            status.value = "Conexión BLE exitosa"
            discoverServices()
        }.onFailure {
            status.value = it.message ?: "No se pudo conectar"
        }
    }

    fun discoverServices() = viewModelScope.launch {
        runCatching {
            status.value = "Descubriendo servicios..."
            services.value = discoverServicesUseCase()
            status.value = if (services.value.isEmpty()) "Conectado, pero sin servicios" else "Servicios listos"
        }.onFailure {
            status.value = "Error al descubrir servicios"
        }
    }

    fun sendCommand(serviceUuid: String, characteristicUuid: String, hex: String) = viewModelScope.launch {
        if (hex.isBlank()) {
            status.value = "Ingresa un comando HEX"
            return@launch
        }
        status.value = "Enviando comando..."
        val success = sendCommandUseCase(serviceUuid, characteristicUuid, hex)
        status.value = if (success) "Comando enviado" else "Error al enviar"
    }

    fun selectCharacteristic(characteristic: BleCharacteristic) {
        selectedCharacteristic.value = characteristic
    }
}

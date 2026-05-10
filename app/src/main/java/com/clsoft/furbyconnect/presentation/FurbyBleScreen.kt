package com.clsoft.furbyconnect.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.clickable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.clsoft.furbyconnect.domain.model.BleCharacteristic
import com.clsoft.furbyconnect.domain.model.BleDevice
import com.clsoft.furbyconnect.domain.model.BleService

@Composable
fun FurbyBleScreen(
    devices: List<BleDevice>,
    services: List<BleService>,
    statusMessage: String,
    selectedCharacteristic: BleCharacteristic?,
    onStartScan: () -> Unit,
    onStopScan: () -> Unit,
    onConnect: (BleDevice) -> Unit,
    onSelectCharacteristic: (BleCharacteristic) -> Unit,
    onSendHexCommand: (String) -> Unit
) {
    val hexCommand = remember { mutableStateOf("") }
    val normalizedHex = hexCommand.value.replace(" ", "")
    val hasValidHex = normalizedHex.isNotBlank() && normalizedHex.length % 2 == 0 && normalizedHex.all { it.isDigit() || it.lowercaseChar() in 'a'..'f' }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Furby BLE Controller",
            style = MaterialTheme.typography.headlineSmall
        )

        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "Estado: $statusMessage")
        Spacer(modifier = Modifier.height(12.dp))

        Row {
            Button(onClick = onStartScan) {
                Text("Buscar")
            }

            Spacer(modifier = Modifier.width(8.dp))

            OutlinedButton(onClick = onStopScan) {
                Text("Detener")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(text = "Dispositivos encontrados", style = MaterialTheme.typography.titleMedium)
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            items(devices) { device ->
                DeviceCard(device, onConnect)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(text = "Servicios y Characteristics", style = MaterialTheme.typography.titleMedium)
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            items(services) { service ->
                ServiceCard(service, selectedCharacteristic, onSelectCharacteristic)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(text = "Enviar comando HEX", style = MaterialTheme.typography.titleMedium)
        Text(
            text = selectedCharacteristic?.let {
                "Destino: ${it.serviceUuid} / ${it.characteristicUuid}"
            } ?: "Destino: selecciona un servicio/característica",
            style = MaterialTheme.typography.bodySmall,
            color = if (selectedCharacteristic != null) Color(0xFF2E7D32) else Color(0xFFB00020)
        )
        OutlinedTextField(
            value = hexCommand.value,
            onValueChange = { hexCommand.value = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Ejemplo: 01 02 03") }
        )

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = { onSendHexCommand(hexCommand.value) },
            enabled = selectedCharacteristic != null && hasValidHex,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Enviar")
        }
    }
}

@Composable
private fun DeviceCard(
    device: BleDevice,
    onConnect: (BleDevice) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("Nombre: ${device.name}")
            Text("MAC: ${device.address}")
            Text("RSSI: ${device.rssi}")

            Spacer(modifier = Modifier.height(8.dp))

            Button(onClick = { onConnect(device) }) {
                Text("Conectar")
            }
        }
    }
}

@Composable
private fun ServiceCard(
    service: BleService,
    selectedCharacteristic: BleCharacteristic?,
    onSelectCharacteristic: (BleCharacteristic) -> Unit
) {
    val firstWritable = service.characteristics.firstOrNull {
        it.canWrite || it.canWriteWithoutResponse
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = firstWritable != null) {
                    firstWritable?.let(onSelectCharacteristic)
                }
                .padding(12.dp)
        ) {
            Text("Service UUID: ${service.uuid}")
            if (firstWritable != null) {
                Text("(Toca el servicio para seleccionar una característica writable)")
            }

            Spacer(modifier = Modifier.height(8.dp))

            service.characteristics.forEach { characteristic ->
                val isSelected =
                    selectedCharacteristic?.characteristicUuid == characteristic.characteristicUuid

                Text(
                    "Characteristic UUID: ${characteristic.characteristicUuid}",
                    modifier = Modifier.clickable { onSelectCharacteristic(characteristic) }
                )
                Text("READ: ${characteristic.canRead}")
                Text("WRITE: ${characteristic.canWrite}")
                Text("WRITE_NO_RESPONSE: ${characteristic.canWriteWithoutResponse}")
                Text("NOTIFY: ${characteristic.canNotify}")

                Spacer(modifier = Modifier.height(4.dp))

                if (characteristic.canWrite || characteristic.canWriteWithoutResponse) {
                    Button(onClick = { onSelectCharacteristic(characteristic) }) {
                        Text(if (isSelected) "Seleccionada" else "Seleccionar para enviar")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

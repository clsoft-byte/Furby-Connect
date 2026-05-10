package com.clsoft.furbyconnect.data.repository

import com.clsoft.furbyconnect.data.ble.BleConnectionManager
import com.clsoft.furbyconnect.data.ble.BleScanner
import com.clsoft.furbyconnect.data.ble.models.BleDeviceItem
import com.clsoft.furbyconnect.data.ble.models.BleServiceItem
import com.clsoft.furbyconnect.domain.model.BleCharacteristic
import com.clsoft.furbyconnect.domain.model.BleDevice
import com.clsoft.furbyconnect.domain.model.BleService
import com.clsoft.furbyconnect.domain.repository.FurbyBleRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import java.util.UUID
import kotlin.coroutines.resume

class FurbyBleRepositoryImpl(
    private val scanner: BleScanner,
    private val connection: BleConnectionManager
) : FurbyBleRepository {

    override suspend fun scanDevices(): List<BleDevice>  {
        val devices = mutableListOf<BleDevice>()
        val lock = CompletableDeferred<Unit>()

        scanner.startScan { item ->
            devices.add(item.toDomain())
        }

        // Esperar hasta timeout
        try {
            withTimeout(5000) {
                lock.await() // No bloquea la UI, solo espera
            }
        } catch (e: TimeoutCancellationException) {
            // Timeout alcanzado, stop scan
            scanner.stopScan()
        }

        scanner.stopScan()
        return devices
    }

    override fun stopScan() {
        scanner.stopScan()
    }

    override suspend fun connect(device: BleDevice) {
        val item = scanner.getDeviceByAddress(device.address) ?: return
        connection.connect(item.device)
    }

    override suspend fun disconnect() {
        connection.disconnect()
    }

    override suspend fun discoverServices(): List<BleService> = suspendCancellableCoroutine { cont ->
        connection.onServicesDiscovered = { services ->
            cont.resume(services.map { it.toDomain() })
        }
    }

    override suspend fun sendCommand(serviceUuid: String, characteristicUuid: String, hex: String): Boolean {
        return connection.writeHexCommand(
            UUID.fromString(serviceUuid),
            UUID.fromString(characteristicUuid),
            hex
        )
    }

    private fun BleDeviceItem.toDomain(): BleDevice =
        BleDevice(name, address, rssi)

    private fun BleServiceItem.toDomain(): BleService =
        BleService(
            uuid.toString(),
            characteristics.map {
                BleCharacteristic(
                    serviceUuid = it.serviceUuid.toString(),
                    characteristicUuid = it.characteristicUuid.toString(),
                    canRead = it.canRead,
                    canWrite = it.canWrite,
                    canWriteWithoutResponse = it.canWriteWithoutResponse,
                    canNotify = it.canNotify
                )
            }
        )
}

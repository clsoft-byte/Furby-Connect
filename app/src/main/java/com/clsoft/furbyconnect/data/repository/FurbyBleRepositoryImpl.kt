package com.clsoft.furbyconnect.data.repository

import com.clsoft.furbyconnect.data.ble.BleConnectionManager
import com.clsoft.furbyconnect.data.ble.BleScanner
import com.clsoft.furbyconnect.data.ble.models.BleDeviceItem
import com.clsoft.furbyconnect.data.ble.models.BleServiceItem
import com.clsoft.furbyconnect.domain.model.BleCharacteristic
import com.clsoft.furbyconnect.domain.model.BleDevice
import com.clsoft.furbyconnect.domain.model.BleService
import com.clsoft.furbyconnect.domain.repository.FurbyBleRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.util.UUID
import kotlin.coroutines.resume

class FurbyBleRepositoryImpl(
    private val scanner: BleScanner,
    private val connection: BleConnectionManager
) : FurbyBleRepository {

    companion object {
        private const val MAX_SCAN_DURATION_MS = 12_000L
        private const val MIN_SCAN_DURATION_MS = 3_000L
        private const val IDLE_STOP_WINDOW_MS = 2_500L
        private const val POLL_INTERVAL_MS = 250L
    }

    override suspend fun scanDevices(): List<BleDevice>  {
        var lastDiscoveryAt = System.currentTimeMillis()
        val scanStartedAt = lastDiscoveryAt

        scanner.startScan {
            lastDiscoveryAt = System.currentTimeMillis()
        }

        try {
            withTimeoutOrNull(MAX_SCAN_DURATION_MS) {
                while (isActive) {
                    delay(POLL_INTERVAL_MS)
                    val now = System.currentTimeMillis()
                    val reachedMinDuration = now - scanStartedAt >= MIN_SCAN_DURATION_MS
                    val idleForTooLong = now - lastDiscoveryAt >= IDLE_STOP_WINDOW_MS

                    if (reachedMinDuration && idleForTooLong) {
                        break
                    }
                }
            }
        } finally {
            scanner.stopScan()
        }

        return scanner.getAllDevices().map { it.toDomain() }
    }

    override fun stopScan() {
        scanner.stopScan()
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

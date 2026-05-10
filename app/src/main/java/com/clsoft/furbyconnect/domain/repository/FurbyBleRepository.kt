package com.clsoft.furbyconnect.domain.repository

import com.clsoft.furbyconnect.domain.model.BleDevice
import com.clsoft.furbyconnect.domain.model.BleService

interface FurbyBleRepository {
    suspend fun scanDevices(): List<BleDevice>
    fun stopScan()
    suspend fun connect(device: BleDevice)
    suspend fun disconnect()
    suspend fun discoverServices(): List<BleService>
    suspend fun sendCommand(serviceUuid: String, characteristicUuid: String, hex: String): Boolean
}

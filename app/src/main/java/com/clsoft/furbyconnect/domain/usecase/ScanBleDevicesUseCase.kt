package com.clsoft.furbyconnect.domain.usecase

import com.clsoft.furbyconnect.domain.model.BleDevice
import com.clsoft.furbyconnect.domain.repository.FurbyBleRepository
import javax.inject.Inject

class ScanBleDevicesUseCase @Inject constructor(private val repository: FurbyBleRepository) {
    suspend operator fun invoke(): List<BleDevice> = repository.scanDevices()
}
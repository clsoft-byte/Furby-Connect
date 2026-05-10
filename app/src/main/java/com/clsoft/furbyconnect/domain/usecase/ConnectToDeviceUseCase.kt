package com.clsoft.furbyconnect.domain.usecase

import com.clsoft.furbyconnect.domain.model.BleDevice
import com.clsoft.furbyconnect.domain.repository.FurbyBleRepository
import javax.inject.Inject

class ConnectToDeviceUseCase @Inject constructor(private val repository: FurbyBleRepository) {
    suspend operator fun invoke(device: BleDevice) = repository.connect(device)
}
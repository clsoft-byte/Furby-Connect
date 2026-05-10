package com.clsoft.furbyconnect.domain.usecase

import com.clsoft.furbyconnect.domain.repository.FurbyBleRepository
import javax.inject.Inject

class SendCommandUseCase @Inject constructor(private val repository: FurbyBleRepository) {
    suspend operator fun invoke(serviceUuid: String, characteristicUuid: String, hex: String) =
        repository.sendCommand(serviceUuid, characteristicUuid, hex)
}
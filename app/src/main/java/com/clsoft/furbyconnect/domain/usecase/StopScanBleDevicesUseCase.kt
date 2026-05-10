package com.clsoft.furbyconnect.domain.usecase

import com.clsoft.furbyconnect.domain.repository.FurbyBleRepository
import javax.inject.Inject

class StopScanBleDevicesUseCase @Inject constructor(private val repository: FurbyBleRepository) {
    operator fun invoke() = repository.stopScan()
}

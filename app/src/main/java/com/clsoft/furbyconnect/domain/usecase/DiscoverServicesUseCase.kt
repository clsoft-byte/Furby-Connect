package com.clsoft.furbyconnect.domain.usecase

import com.clsoft.furbyconnect.domain.model.BleService
import com.clsoft.furbyconnect.domain.repository.FurbyBleRepository
import javax.inject.Inject

class DiscoverServicesUseCase @Inject constructor(private val repository: FurbyBleRepository) {
    suspend operator fun invoke(): List<BleService> = repository.discoverServices()
}
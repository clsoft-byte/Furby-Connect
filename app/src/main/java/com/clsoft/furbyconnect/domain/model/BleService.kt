package com.clsoft.furbyconnect.domain.model

data class BleService(
    val uuid: String,
    val characteristics: List<BleCharacteristic>
)
package com.clsoft.furbyconnect.domain.model

data class BleCharacteristic(
    val serviceUuid: String,
    val characteristicUuid: String,
    val canRead: Boolean,
    val canWrite: Boolean,
    val canWriteWithoutResponse: Boolean,
    val canNotify: Boolean
)
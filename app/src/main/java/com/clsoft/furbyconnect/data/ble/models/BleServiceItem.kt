package com.clsoft.furbyconnect.data.ble.models

import java.util.UUID

data class BleServiceItem(
    val uuid: UUID,
    val characteristics: List<BleCharacteristicItem>
)

data class BleCharacteristicItem(
    val serviceUuid: UUID,
    val characteristicUuid: UUID,
    val properties: Int,
    val canRead: Boolean,
    val canWrite: Boolean,
    val canWriteWithoutResponse: Boolean,
    val canNotify: Boolean
)
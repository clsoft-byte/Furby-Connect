package com.clsoft.furbyconnect.domain.model

data class BleDevice(
    val name: String,
    val address: String,
    val rssi: Int
)
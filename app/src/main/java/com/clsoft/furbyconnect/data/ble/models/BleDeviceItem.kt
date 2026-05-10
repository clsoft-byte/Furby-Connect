package com.clsoft.furbyconnect.data.ble.models

import android.bluetooth.BluetoothDevice

data class BleDeviceItem(
    val name: String,
    val address: String,
    val rssi: Int,
    val device: BluetoothDevice
)
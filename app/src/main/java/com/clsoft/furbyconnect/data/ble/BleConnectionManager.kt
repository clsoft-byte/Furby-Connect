package com.clsoft.furbyconnect.data.ble

import android.annotation.SuppressLint
import android.bluetooth.*
import android.content.Context
import android.os.Build
import com.clsoft.furbyconnect.data.ble.models.BleCharacteristicItem
import com.clsoft.furbyconnect.data.ble.models.BleServiceItem
import java.util.*

class BleConnectionManager(private val context: Context) {
    private var bluetoothGatt: BluetoothGatt? = null
    private var isConnected = false
    private var lastServices: List<BleServiceItem> = emptyList()

    var onConnected: (() -> Unit)? = null
    var onConnectionFailed: ((Int) -> Unit)? = null
    var onDisconnected: (() -> Unit)? = null
    var onServicesDiscovered: ((List<BleServiceItem>) -> Unit)? = null
    var onWriteResult: ((Boolean, String) -> Unit)? = null

    private val callback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                isConnected = false
                onConnectionFailed?.invoke(status)
                gatt.close()
                bluetoothGatt = null
                return
            }

            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    isConnected = true
                    onConnected?.invoke()
                    gatt.discoverServices()
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    isConnected = false
                    lastServices = emptyList()
                    onDisconnected?.invoke()
                    close()
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                val services = gatt.services.map { it.toBleServiceItem() }
                lastServices = services
                onServicesDiscovered?.invoke(services)
            }
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            val success = status == BluetoothGatt.GATT_SUCCESS
            val message = if (success) "Comando enviado" else "Error al enviar ($status)"
            onWriteResult?.invoke(success, message)
        }
    }

    @SuppressLint("MissingPermission")
    fun connect(device: BluetoothDevice) {
        close()
        isConnected = false
        lastServices = emptyList()
        bluetoothGatt = device.connectGatt(context, false, callback, BluetoothDevice.TRANSPORT_LE)
    }

    fun isConnected(): Boolean = isConnected

    fun getLastDiscoveredServices(): List<BleServiceItem> = lastServices

    @SuppressLint("MissingPermission")
    fun writeHexCommand(serviceUuid: UUID, characteristicUuid: UUID, hex: String): Boolean {
        val bytes = hexToByteArray(hex)
        return writeBytes(serviceUuid, characteristicUuid, bytes)
    }

    @SuppressLint("MissingPermission")
    fun writeBytes(serviceUuid: UUID, characteristicUuid: UUID, bytes: ByteArray): Boolean {
        val gatt = bluetoothGatt ?: return false
        val service = gatt.getService(serviceUuid) ?: return false
        val characteristic = service.getCharacteristic(characteristicUuid) ?: return false

        val writeType = when {
            characteristic.hasProperty(BluetoothGattCharacteristic.PROPERTY_WRITE) -> BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
            characteristic.hasProperty(BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) -> BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
            else -> return false
        }

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            gatt.writeCharacteristic(characteristic, bytes, writeType) == BluetoothStatusCodes.SUCCESS
        } else {
            characteristic.writeType = writeType
            characteristic.value = bytes
            gatt.writeCharacteristic(characteristic)
        }
    }

    @SuppressLint("MissingPermission")
    fun disconnect() { bluetoothGatt?.disconnect(); close() }

    @SuppressLint("MissingPermission")
    fun close() {
        bluetoothGatt?.close()
        bluetoothGatt = null
        isConnected = false
    }

    private fun BluetoothGattCharacteristic.hasProperty(property: Int) = properties and property != 0

    private fun hexToByteArray(input: String): ByteArray =
        input.replace(" ", "").chunked(2).map { it.toInt(16).toByte() }.toByteArray()

    private fun BluetoothGattService.toBleServiceItem(): BleServiceItem {
        return BleServiceItem(
            uuid = uuid,
            characteristics = characteristics.map {
                BleCharacteristicItem(
                    serviceUuid = uuid,
                    characteristicUuid = it.uuid,
                    properties = it.properties,
                    canRead = it.hasProperty(BluetoothGattCharacteristic.PROPERTY_READ),
                    canWrite = it.hasProperty(BluetoothGattCharacteristic.PROPERTY_WRITE),
                    canWriteWithoutResponse = it.hasProperty(BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE),
                    canNotify = it.hasProperty(BluetoothGattCharacteristic.PROPERTY_NOTIFY)
                )
            }
        )
    }
}

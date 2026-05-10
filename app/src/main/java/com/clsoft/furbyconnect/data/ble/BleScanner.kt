package com.clsoft.furbyconnect.data.ble

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.clsoft.furbyconnect.data.ble.models.BleDeviceItem
class BleScanner(
    private val context: Context,
    private val bluetoothAdapter: BluetoothAdapter
) {

    private var scanCallback: ScanCallback? = null
    private val discoveredDevices = mutableListOf<BleDeviceItem>()

    @SuppressLint("MissingPermission")
    fun startScan(onDeviceFound: (BleDeviceItem) -> Unit): Boolean {
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY) // rápido y confiable
            .build()


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                Log.e("BLE_SCAN", "No hay permiso BLUETOOTH_SCAN")
                return false
            }
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                Log.e("BLE_SCAN", "No hay permiso BLUETOOTH_CONNECT")
                return false
            }
        } else {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                Log.e("BLE_SCAN", "No hay permiso ACCESS_FINE_LOCATION")
                return false
            }
        }

        val scanner = bluetoothAdapter.bluetoothLeScanner
        if(scanner == null) {
            Log.e("BLE_SCAN", "bluetoothLeScanner es null")
            return false
        }
        discoveredDevices.clear()

        scanCallback = object : ScanCallback() {

            private val seenAddresses = mutableSetOf<String>()

            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val device = result.device
                val name = device.name ?: "Sin nombre"
                val address = device.address

                // Evitar duplicados
                if (seenAddresses.add(address)) {
                    val item = BleDeviceItem(name, address, result.rssi, device)
                    discoveredDevices.add(item)
                    onDeviceFound(item)
                    Log.d("BLE_SCAN", "Dispositivo encontrado: $name [$address], RSSI=${result.rssi}")
                }
            }

            override fun onBatchScanResults(results: MutableList<ScanResult>) {
                results.forEach { result ->
                    onScanResult(ScanSettings.CALLBACK_TYPE_ALL_MATCHES, result)
                }
            }

            override fun onScanFailed(errorCode: Int) {
                val message = when (errorCode) {
                    SCAN_FAILED_ALREADY_STARTED -> "Scan fallido: ya iniciado"
                    SCAN_FAILED_APPLICATION_REGISTRATION_FAILED -> "Scan fallido: registro de app"
                    SCAN_FAILED_INTERNAL_ERROR -> "Scan fallido: error interno"
                    SCAN_FAILED_FEATURE_UNSUPPORTED -> "Scan fallido: feature no soportada"
                    else -> "Scan fallido: código $errorCode"
                }
                Log.e("BLE_SCAN", message)
                super.onScanFailed(errorCode)
            }
        }
// Iniciar scan
        scanner.startScan(null, settings, scanCallback)
        return true
    }

    @SuppressLint("MissingPermission")
    fun stopScan() {
        bluetoothAdapter.bluetoothLeScanner?.let { scanner ->
            scanCallback?.let { scanner.stopScan(it) }
        }
        scanCallback = null
    }

    fun getDeviceByAddress(address: String): BleDeviceItem? {
        return discoveredDevices.firstOrNull { it.address == address }
    }

    fun getAllDevices(): List<BleDeviceItem> = discoveredDevices.toList()
}

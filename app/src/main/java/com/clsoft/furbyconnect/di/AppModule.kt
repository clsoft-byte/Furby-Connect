package com.clsoft.furbyconnect.di

import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import com.clsoft.furbyconnect.data.ble.BleConnectionManager
import com.clsoft.furbyconnect.data.ble.BleScanner
import com.clsoft.furbyconnect.data.repository.FurbyBleRepositoryImpl
import com.clsoft.furbyconnect.domain.repository.FurbyBleRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAppContext(app: Application): Context = app.applicationContext

    @Provides
    @Singleton
    fun provideBluetoothAdapter(app: Application): BluetoothAdapter {
        val manager = app.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        return manager.adapter ?: throw IllegalStateException("Bluetooth no disponible en este dispositivo")
    }

    @Provides
    @Singleton
    fun provideBleScanner(context: Context, adapter: BluetoothAdapter): BleScanner =
        BleScanner(context, adapter)
    @Provides
    @Singleton
    fun provideBleConnectionManager(app: Application): BleConnectionManager = BleConnectionManager(app)

    @Provides
    @Singleton
    fun provideFurbyBleRepository(scanner: BleScanner, connection: BleConnectionManager): FurbyBleRepository =
        FurbyBleRepositoryImpl(scanner, connection)

}
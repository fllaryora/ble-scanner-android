package com.santansarah.scan.presentation

import android.annotation.SuppressLint
import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothProfile
import android.os.Build
import com.santansarah.scan.domain.bleparsables.CCCD
import com.santansarah.scan.domain.models.ConnectionState
import com.santansarah.scan.domain.models.DeviceService
import com.santansarah.scan.domain.usecases.ParseDescriptor
import com.santansarah.scan.domain.usecases.ParseNotification
import com.santansarah.scan.domain.usecases.ParseRead
import com.santansarah.scan.domain.usecases.ParseService
import com.santansarah.scan.utils.print
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import timber.log.Timber

@SuppressLint("MissingPermission")
class BleGatt(
    private val app: Application,
    private val scope: CoroutineScope,
    private val parseService: ParseService,
    private val parseRead: ParseRead,
    private val parseNotification: ParseNotification,
    private val parseDescriptor: ParseDescriptor
) : KoinComponent {

    private var btGatt: BluetoothGatt? = null
    private val btAdapter: BluetoothAdapter = get()

    val connectMessage = MutableStateFlow(ConnectionState.DISCONNECTED)
    val deviceDetails = MutableStateFlow<List<DeviceService>>(emptyList())

    private val bluetoothGattCallback = object : BluetoothGattCallback() {

        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            super.onConnectionStateChange(gatt, status, newState)

            btGatt = gatt
            Timber.d("status: $status")

            when (newState) {
                BluetoothProfile.STATE_CONNECTING -> connectMessage.value =
                    ConnectionState.CONNECTING

                BluetoothProfile.STATE_CONNECTED -> {
                    connectMessage.value = ConnectionState.CONNECTED
                    btGatt?.discoverServices()
                }

                BluetoothProfile.STATE_DISCONNECTING -> connectMessage.value =
                    ConnectionState.DISCONNECTING

                BluetoothProfile.STATE_DISCONNECTED -> connectMessage.value =
                    ConnectionState.DISCONNECTED

                else -> connectMessage.value = ConnectionState.DISCONNECTED
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            super.onServicesDiscovered(gatt, status)

            scope.launch {
                deviceDetails.value = emptyList()
                gatt?.let { bluetoothGatt ->
                    deviceDetails.value = parseService(bluetoothGatt, status)
                    enableNotificationsAndIndications()
                }
            }
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray,
            status: Int
        ) {
            super.onCharacteristicRead(gatt, characteristic, value, status)
            deviceDetails.value = parseRead(value, deviceDetails.value, characteristic, status)
        }

        @Deprecated("Deprecated in Java")
        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            super.onCharacteristicRead(gatt, characteristic, status)
            deviceDetails.value = parseRead(null, deviceDetails.value, characteristic, status)
        }

        @Deprecated("Deprecated in Java")
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic
        ) {
            super.onCharacteristicChanged(gatt, characteristic)
            Timber.d("characteristic changed: ${characteristic.value.print()}")
            deviceDetails.value = parseNotification(deviceDetails.value, characteristic, characteristic.value)
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            super.onCharacteristicChanged(gatt, characteristic, value)
            Timber.d("characteristic changed: ${value.print()}")
            deviceDetails.value = parseNotification(deviceDetails.value, characteristic, value)
        }

        @Deprecated("Deprecated in Java")
        override fun onDescriptorRead(
            gatt: BluetoothGatt?,
            descriptor: BluetoothGattDescriptor,
            status: Int
        ) {
            super.onDescriptorRead(gatt, descriptor, status)

            Timber.d(
                "descriptor read: ${descriptor.uuid}, " +
                        "${descriptor.characteristic.uuid}, $status, ${descriptor.value.print()}"
            )

            deviceDetails.value = parseDescriptor(deviceDetails.value, descriptor, status, descriptor.value)
        }

        override fun onDescriptorRead(
            gatt: BluetoothGatt,
            descriptor: BluetoothGattDescriptor,
            status: Int,
            value: ByteArray
        ) {
            super.onDescriptorRead(gatt, descriptor, status, value)

            Timber.d(
                "descriptor read: ${descriptor.uuid}, " +
                        "${descriptor.characteristic.uuid}, $status, ${value.print()}"
            )

            deviceDetails.value = parseDescriptor(deviceDetails.value, descriptor, status, value)
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            super.onCharacteristicWrite(gatt, characteristic, status)
            btGatt?.readCharacteristic(characteristic)
        }

        override fun onDescriptorWrite(
            gatt: BluetoothGatt?,
            descriptor: BluetoothGattDescriptor,
            status: Int
        ) {
            super.onDescriptorWrite(gatt, descriptor, status)
            Timber.d("descriptor write: ${descriptor.uuid}, ${descriptor.characteristic.uuid}, $status")
        }

    }

    suspend fun enableNotificationsAndIndications() {// Post parse services and characteristics

        btGatt?.services?.forEach { gattSvcForNotify: BluetoothGattService ->
            gattSvcForNotify.characteristics?.forEach { gattCharacteristic: BluetoothGattCharacteristic ->

                gattCharacteristic.descriptors.find { desc : BluetoothGattDescriptor->
                    desc.uuid.toString() == CCCD.uuid //0x2902 the characteristic is notificable or indicable ?
                }?.also { cccd: BluetoothGattDescriptor ->
                    val notifyRegistered = btGatt?.setCharacteristicNotification(gattCharacteristic, true)

                    if (gattCharacteristic.properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY > 0) {
                        cccd.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
                        btGatt?.writeDescriptor(cccd)
                    }

                    if (gattCharacteristic.properties and BluetoothGattCharacteristic.PROPERTY_INDICATE > 0) {
                        cccd.setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE)
                        btGatt?.writeDescriptor(cccd)
                    }

                    // give gatt a little breathing room for writes
                    delay(300L)

                }

            }
        }
    }

    fun connect(address: String) {
        if (btAdapter.isEnabled) {
            btAdapter.let { adapter: BluetoothAdapter ->
                try {
                    connectMessage.value = ConnectionState.CONNECTING
                    val device = adapter.getRemoteDevice(address)
                    device.connectGatt(app, false, bluetoothGattCallback)
                } catch (exception: Exception) {
                    connectMessage.value = ConnectionState.DISCONNECTED
                    Timber.tag("BTGATT_CONNECT").e(exception)
                }
            }
        }
    }

    fun readCharacteristic(uuid: String) {
        btGatt?.services?.flatMap { gattService: BluetoothGattService ->
            gattService.characteristics
        }?.find { gattCharacteristic: BluetoothGattCharacteristic ->
            gattCharacteristic.uuid.toString() == uuid
        }?.also { foundChar: BluetoothGattCharacteristic ->
            Timber.d("Found Char: " + foundChar.uuid.toString())
            btGatt?.readCharacteristic(foundChar)
        }
    }

    fun readDescriptor(charUuid: String, descUuid: String) {

        val currentCharacteristic = btGatt?.services?.flatMap { gattService: BluetoothGattService ->
            gattService.characteristics
        }?.find { gattCharacteristic: BluetoothGattCharacteristic ->
            gattCharacteristic.uuid.toString() == charUuid
        }

        currentCharacteristic?.let { gattCharacteristic: BluetoothGattCharacteristic ->
            gattCharacteristic.descriptors.find { gattDescriptor: BluetoothGattDescriptor ->
                gattDescriptor.uuid.toString() == descUuid
            }?.also { foundDesc: BluetoothGattDescriptor ->
                Timber.d("Found Char: $charUuid; " + foundDesc.uuid.toString())
                btGatt?.readDescriptor(foundDesc)
            }
        }

    }

    fun writeBytes(uuid: String, bytes: ByteArray) {
        btGatt?.services?.flatMap { gattService : BluetoothGattService ->
            gattService.characteristics
        }?.find { gattCharacteristic : BluetoothGattCharacteristic ->
            gattCharacteristic.uuid.toString() == uuid
        }?.also { foundChar:BluetoothGattCharacteristic ->
            Timber.d("Found Char: " + foundChar.uuid.toString())
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                btGatt?.writeCharacteristic(
                    foundChar,
                    bytes,
                    BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                )
            } else {
                foundChar.setValue(bytes)
                btGatt?.writeCharacteristic(foundChar)
            }
        }

    }

    fun writeDescriptor(charUuid: String, uuid: String, bytes: ByteArray) {
        btGatt?.services?.flatMap { gattService : BluetoothGattService ->
            gattService.characteristics
        }?.flatMap { gattCharacteristic : BluetoothGattCharacteristic ->
            gattCharacteristic.descriptors
        }
            ?.find { gattDescriptor: BluetoothGattDescriptor ->
                gattDescriptor.characteristic.uuid.toString() == charUuid &&
                        gattDescriptor.uuid.toString() == uuid
            }?.also { foundDescriptor: BluetoothGattDescriptor ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    btGatt?.writeDescriptor(foundDescriptor, bytes)
                } else {
                    foundDescriptor.setValue(bytes)
                    btGatt?.writeDescriptor(foundDescriptor)
                }
            }
    }

    fun close() {
        connectMessage.value = ConnectionState.DISCONNECTED
        deviceDetails.value = emptyList()
        try {
            btGatt?.let { gatt: BluetoothGatt ->
                gatt.disconnect()
                gatt.close()
                btGatt = null
            }
        } catch (exception: Exception) {
            Timber.tag("BTGATT_CLOSE").e(exception)
        } finally {
            btGatt = null
        }
    }

}

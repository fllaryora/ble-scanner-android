package com.santansarah.scan.domain.usecases

import android.annotation.SuppressLint
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import com.santansarah.scan.domain.models.DeviceCharacteristics
import com.santansarah.scan.domain.models.DeviceService
import com.santansarah.scan.domain.models.updateBytes
import timber.log.Timber

/**
 * It handles onCharacteristicRead
 */
class ParseRead() {

    @SuppressLint("MissingPermission")
    operator fun invoke(
        value: ByteArray?,
        deviceDetails: List<DeviceService>,
        characteristic: BluetoothGattCharacteristic,
        status: Int
    ): List<DeviceService> {
        if (status == BluetoothGatt.GATT_SUCCESS) {
            val newList = deviceDetails.map { service: DeviceService ->
                service.copy(characteristics =
                service.characteristics.map { characteristics : DeviceCharacteristics ->
                    if (characteristics.uuid == characteristic.uuid.toString()) {
                        if(value != null) {
                            characteristics.updateBytes(value)
                        } else {
                            characteristics.updateBytes(characteristic.value)
                        }

                    } else
                        characteristics
                })
            }

            Timber.d("newList: $newList")

            return newList
        } else {
            return deviceDetails
        }

    }
}

package com.santansarah.scan.domain.usecases

import android.bluetooth.BluetoothGattCharacteristic
import com.santansarah.scan.domain.models.DeviceCharacteristics
import com.santansarah.scan.domain.models.DeviceService
import com.santansarah.scan.domain.models.updateNotification
import timber.log.Timber

/**
 * It handles onCharacteristicChanged
 */
class ParseNotification() {

    operator fun invoke(
        deviceDetails: List<DeviceService>,
        characteristic: BluetoothGattCharacteristic,
        value: ByteArray
    ): List<DeviceService> {

        val newList = deviceDetails.map { service :DeviceService ->
            service.copy(characteristics =
            service.characteristics.map { characteristics:DeviceCharacteristics ->
                if (characteristics.uuid == characteristic.uuid.toString()) {
                    characteristics.updateNotification(value)
                } else
                    characteristics
            })
        }

        Timber.d("newList: $newList")

        return newList
    }

}

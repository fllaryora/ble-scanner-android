package com.santansarah.scan.domain.usecases

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattDescriptor
import com.santansarah.scan.domain.models.DeviceCharacteristics
import com.santansarah.scan.domain.models.DeviceService
import com.santansarah.scan.domain.models.updateDescriptors
import timber.log.Timber

/**
 * It handles onDescriptorRead
 */
class ParseDescriptor() {

    operator fun invoke(
        deviceDetails: List<DeviceService>,
        descriptor: BluetoothGattDescriptor,
        status: Int,
        value: ByteArray
    ): List<DeviceService> {

        if (status == BluetoothGatt.GATT_SUCCESS) {

            Timber.d(value.toString())

            val newList = deviceDetails.map { deviceService :DeviceService ->
                deviceService.copy(characteristics =
                deviceService.characteristics.map {
                        characteristics :DeviceCharacteristics ->
                    if (descriptor.characteristic.uuid.toString() == characteristics.uuid) {
                        characteristics.copy(
                            descriptors =
                            characteristics.updateDescriptors(descriptor.uuid.toString(), value)
                        )
                    }
                    else
                        characteristics
                })
            }

            Timber.d("newDescriptorList: $newList")

            return newList
        } else {
            return deviceDetails
        }

    }
}

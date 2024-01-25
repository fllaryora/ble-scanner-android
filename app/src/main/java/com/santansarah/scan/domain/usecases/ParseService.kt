package com.santansarah.scan.domain.usecases

import android.annotation.SuppressLint
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattService
import com.santansarah.scan.domain.Chemistry
import com.santansarah.scan.local.entities.Service
import com.santansarah.scan.domain.interfaces.IBleRepository
import com.santansarah.scan.domain.models.BlePermissions
import com.santansarah.scan.domain.models.BleProperties
import com.santansarah.scan.domain.models.BleWriteTypes
import com.santansarah.scan.domain.models.DeviceCharacteristics
import com.santansarah.scan.domain.models.DeviceDescriptor
import com.santansarah.scan.domain.models.DeviceService
import com.santansarah.scan.domain.models.canRead
import com.santansarah.scan.domain.models.canWriteProperties
import com.santansarah.scan.utils.toGss
import timber.log.Timber

/**
 * It handles on Services Discovered
 */
class ParseService
    (
    private val bleRepository: IBleRepository
) {

    @SuppressLint("MissingPermission")
    suspend operator fun invoke(gatt: BluetoothGatt, status: Int):
            List<DeviceService> {

        val services = mutableListOf<DeviceService>()

        if (status == BluetoothGatt.GATT_SUCCESS) {
            gatt.services?.forEach { gattService : BluetoothGattService ->
                val serviceName = bleRepository.getServiceById(gattService.uuid.toGss())?.name ?:
                    Chemistry.getNameByServiceUUID(gattService.uuid.toString())

                val service = Service(
                    "",
                    serviceName,
                    "",
                    gattService.uuid.toString().uppercase()
                )

                val characteristics = mutableListOf<DeviceCharacteristics>()

                gattService.characteristics.forEach { characteristic : BluetoothGattCharacteristic ->
                    val deviceCharacteristic = bleRepository
                        .getCharacteristicById(characteristic.uuid.toGss())

                    val permissions = characteristic.permissions
                    val properties = BleProperties.getAllProperties(characteristic.properties)
                    val writeTypes = BleWriteTypes.getAllTypes(characteristic.writeType)

                    val descriptors = mutableListOf<DeviceDescriptor>()
                    characteristic.descriptors?.forEach { gattDescriptor: BluetoothGattDescriptor ->

                        Timber.d(characteristic.uuid.toString())
                        Timber.d(gattDescriptor.uuid.toString() + "; " + gattDescriptor.characteristic.uuid.toString())

                        val deviceDescriptor = bleRepository.getDescriptorById(
                            gattDescriptor.uuid.toGss()
                        )

                        descriptors.add(
                            DeviceDescriptor(
                                uuid = gattDescriptor.uuid.toString(),
                                name = deviceDescriptor?.name ?:
                                Chemistry.getNameByDescriptorUUID(gattDescriptor.uuid.toString()),
                                charUuid = gattDescriptor.characteristic.uuid.toString(),
                                permissions = BlePermissions.getAllPermissions(gattDescriptor.permissions),
                                notificationProperty = if (properties.contains(BleProperties.PROPERTY_NOTIFY))
                                    BleProperties.PROPERTY_NOTIFY else if (properties.contains(
                                        BleProperties.PROPERTY_INDICATE
                                    )
                                )
                                    BleProperties.PROPERTY_INDICATE else null,
                                readBytes = null
                            )
                        )
                    }

                    characteristics.add(
                        DeviceCharacteristics(
                            uuid = characteristic.uuid.toString(),
                            //Bluetooth LE Characteristic
                            name = deviceCharacteristic?.name ?:
                            Chemistry.getNameByCharacteristicUUID(characteristic.uuid.toString()),
                            descriptor = null,
                            permissions = permissions,
                            properties = properties,
                            writeTypes = writeTypes,
                            descriptors = descriptors,
                            canRead = properties.canRead(),
                            canWrite = properties.canWriteProperties(),
                            readBytes = null,
                            notificationBytes = null
                        )
                    )
                } //end of for each characteristic

                val deviceService = DeviceService(
                    service.uuid,
                    service.name,
                    characteristics
                )

                services.add(deviceService)
                Timber.d(services.toString())

            } // end of for each service
        } //end of if success

        return services.toList()

    }

}

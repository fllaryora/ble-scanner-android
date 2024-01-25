package com.santansarah.scan.local

import android.os.ParcelUuid
import com.santansarah.scan.local.entities.BleCharacteristic
import com.santansarah.scan.local.entities.Company
import com.santansarah.scan.local.entities.Descriptor
import com.santansarah.scan.local.entities.MicrosoftDevice
import com.santansarah.scan.local.entities.ScannedDevice
import com.santansarah.scan.local.entities.Service
import com.santansarah.scan.domain.interfaces.IBleRepository
import com.santansarah.scan.domain.models.ScanFilterOption
import com.santansarah.scan.utils.toGss
import com.santansarah.scan.utils.toHex
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import timber.log.Timber

class BleRepository(
    private val dao: BleDao
): IBleRepository {

    override suspend fun getCompanyById(id: Int): Company? = dao.getCompanyById(id)

    override suspend fun getServiceById(uuid: String): Service? = dao.getServiceByUuid(uuid)

    override suspend fun getCharacteristicById(uuid: String): BleCharacteristic? {
        Timber.d(uuid)
        return dao.getCharacteristicsByUuid(uuid)
    }

    override suspend fun getMicrosoftDeviceById(id: Int): MicrosoftDevice? = dao.getMicrosoftDevice(id)

    override suspend fun insertDevice(device: ScannedDevice): Long {
        val existingDevice = dao.getDeviceByAddress(device.address)

        val deviceToUpsert = ScannedDevice(
            deviceId = existingDevice?.deviceId,
            deviceName = existingDevice?.deviceName ?: device.deviceName,
            address = device.address,
            rssi = device.rssi,
            manufacturer = existingDevice?.manufacturer ?: device.manufacturer,
            services = device.services,
            extra = device.extra,
            lastSeen = device.lastSeen,
            customName = existingDevice?.customName,
            baseRssi = existingDevice?.let { scannedDevice ->
                val lowRssi = scannedDevice.baseRssi - 20
                val highRssi = scannedDevice.baseRssi + 20
                if (device.rssi in lowRssi..highRssi)
                    scannedDevice.baseRssi
                else
                    device.rssi
            } ?: device.rssi,
            favorite = existingDevice?.favorite ?: false,
            forget = existingDevice?.forget ?: false
        )

        return dao.insertDevice(deviceToUpsert)
    }

    override suspend fun getDeviceByAddress(address: String) = dao.getDeviceByAddress(address)

    override suspend fun deleteScans() = dao.deleteScans()

    override fun getScannedDevices(scanFilter: ScanFilterOption?): Flow<List<ScannedDevice>> {

        val devices = dao.getScannedDevices()
        Timber.d("got devices..")

        return scanFilter?.let { scanFilterNotNull : ScanFilterOption ->
            when (scanFilterNotNull) {
                ScanFilterOption.RSSI -> devices.map { deviceList : List<ScannedDevice> ->
                    deviceList.filter { device ->
                        !device.forget
                    }.sortedByDescending { device -> device.baseRssi }
                }

                ScanFilterOption.NAME -> devices.map { deviceList : List<ScannedDevice> ->
                    deviceList.filter { device ->
                        (device.deviceName != null || device.customName != null) && !device.forget
                    }.sortedBy { device ->
                        device.customName ?: device.deviceName
                    }
                }

                ScanFilterOption.FAVORITES -> devices.map { deviceList: List<ScannedDevice> ->
                    deviceList.filter { device -> device.favorite && !device.forget }
                }
                ScanFilterOption.FORGET -> devices.map { deviceList: List<ScannedDevice> ->
                    deviceList.filter { device -> device.forget }
                }
            }
        } ?: devices.map { deviceList: List<ScannedDevice> ->
            deviceList.filter { device -> !device.forget }
        }

    }

    override suspend fun getMicrosoftDevice(
        byteArray: ByteArray
    ): String? {
        val msDeviceType = byteArray[1].toHex().toInt()
        return getMicrosoftDeviceById(msDeviceType)?.name
    }

    override suspend fun getServices(
        serviceIdRecord: List<ParcelUuid>
    ): List<String>? {
        var serviceNames: MutableList<String>? = null

        serviceIdRecord.forEach { serviceId: ParcelUuid ->
            val formattedId = serviceId.uuid.toGss()
            getServiceById(formattedId)?.name?.let { serviceName: String ->
                if (serviceNames == null)
                    serviceNames = mutableListOf()
                serviceNames?.add(serviceName)
            }
        }

        return serviceNames?.toList()

    }

    override suspend fun getDescriptorById(uuid: String): Descriptor? = dao.getDescriptorByUuid(uuid)

    override suspend fun updateDevice(scannedDevice: ScannedDevice) = dao.updateDevice(scannedDevice)

    override suspend fun deleteNotSeen() = dao.deleteNotSeen()

}

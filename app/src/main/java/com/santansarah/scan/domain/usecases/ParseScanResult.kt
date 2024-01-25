package com.santansarah.scan.domain.usecases

import android.annotation.SuppressLint
import android.bluetooth.le.ScanResult
import android.util.SparseArray
import com.santansarah.scan.domain.MICROSOFT
import com.santansarah.scan.domain.interfaces.IBleRepository
import com.santansarah.scan.local.entities.ScannedDevice
import com.santansarah.scan.utils.toMillis
import timber.log.Timber

class ParseScanResult
    (
    private val bleRepository: IBleRepository
) {

    @SuppressLint("MissingPermission")
    suspend operator fun invoke(result: ScanResult) {

        try {
            var mfName: String? = null
            var services: List<String>? = null
            var extra: List<String>? = null

            result.scanRecord?.manufacturerSpecificData?.let { manufacturedData : SparseArray<ByteArray> ->
                getManufacturedId(manufacturedData)?.let { manufacturedId : Int ->
                    mfName = bleRepository.getCompanyById(manufacturedId)?.name

                    result.scanRecord?.getManufacturerSpecificData(manufacturedId)?.let {
                            manufacturedBytes : ByteArray ->
                        if (manufacturedId == MICROSOFT) {
                            bleRepository.getMicrosoftDevice(manufacturedBytes)?.let {
                                    microsoftDevice : String ->
                                extra = listOf(microsoftDevice)
                            }
                        }
                    }
                }
            }

            result.scanRecord?.serviceUuids?.let { parcelUuidsList ->
                services = bleRepository.getServices(parcelUuidsList)
            }

            val device = ScannedDevice(
                deviceId = 0,
                deviceName = result.device.name,
                address = result.device.address,
                rssi = result.rssi,
                manufacturer = mfName,
                services = services,
                extra = extra,
                lastSeen = result.timestampNanos.toMillis(),
                customName = null,
                baseRssi = 0,
                favorite = false,
                forget = false
            )

            // Only show my Device
            if(result.device.name == "Air Quality Detector") {
                val recNum = bleRepository.insertDevice(device)
                Timber.d("Insert at: $recNum")
            }
        } catch (exception: Exception) {
            Timber.e(exception, "Insert Device")
        }

    }

    private fun getManufacturedId(
        manufacturedData: SparseArray<ByteArray>
    ): Int? {

        var manufacturedId: Int? = null
        for (i in 0 until manufacturedData.size()) {
            manufacturedId = manufacturedData.keyAt(i)
        }
        return manufacturedId
    }

}

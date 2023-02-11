package com.santansarah.blescanner.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.santansarah.blescanner.data.local.entities.BleCharacteristic
import com.santansarah.blescanner.data.local.entities.Company
import com.santansarah.blescanner.data.local.entities.Descriptor
import com.santansarah.blescanner.data.local.entities.MicrosoftDevice
import com.santansarah.blescanner.data.local.entities.ScannedDevice
import com.santansarah.blescanner.data.local.entities.Service
import kotlinx.coroutines.flow.Flow

@Dao
interface BleDao {

    @Query("SELECT * FROM companies WHERE code = :companyId")
    suspend fun getCompanyById(companyId: Int): Company?

    @Query("SELECT * FROM services where uuid = :uuid")
    suspend fun getServiceByUuid(uuid: String): Service?

    @Query("SELECT * FROM characteristics where uuid = :uuid")
    suspend fun getCharacteristicsByUuid(uuid: String): BleCharacteristic?

    @Query("SELECT * FROM MicrosoftDevices where id = :id")
    suspend fun getMicrosoftDevice(id: Int): MicrosoftDevice?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDevice(device: ScannedDevice): Long

    @Query("SELECT * from scanned")
    fun getScannedDevices(): Flow<List<ScannedDevice>>

    @Query("SELECT * from scanned where address = :address")
    suspend fun getDeviceByAddress(address: String): ScannedDevice?

    @Query("DELETE from scanned")
    suspend fun deleteScans()

    @Query("SELECT * FROM descriptors where uuid = :uuid")
    suspend fun getDescriptorByUuid(uuid: String): Descriptor?


}


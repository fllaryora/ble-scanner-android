package com.santansarah.scan.presentation.control

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.santansarah.scan.local.entities.ScannedDevice
import com.santansarah.scan.domain.bleparsables.ELKBLEDOM
import com.santansarah.scan.domain.interfaces.IBleRepository
import com.santansarah.scan.domain.models.AppRouteArgs.ADDRESS
import com.santansarah.scan.domain.models.ConnectionState
import com.santansarah.scan.domain.models.ControlState
import com.santansarah.scan.domain.models.DeviceService
import com.santansarah.scan.presentation.BleGatt
import com.santansarah.scan.utils.decodeHex
import com.santansarah.scan.utils.toHex2
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber

class ControlViewModel(
    private val bleGatt: BleGatt,
    private val bleRepository: IBleRepository,
    private val dispatcher: CoroutineDispatcher,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val address: String = checkNotNull(savedStateHandle[ADDRESS])

    private val _selectedDevice = MutableStateFlow<ScannedDevice?>(null)
    private val _bleMessage = bleGatt.connectMessage
    private val _services = bleGatt.deviceDetails
    private val _userMessage = MutableStateFlow<String?>(null)

    val controlState = combine(
        _services,
        _bleMessage,
        _userMessage,
        _selectedDevice
    ) { services, bleMessage, userMessage, selectedDevice ->

        services.let { serviceList:  List<DeviceService> ->
            serviceList.flatMap { service ->
                service.characteristics
            }.find { characteristics ->
                characteristics.uuid == ELKBLEDOM.uuid
            }?.also { characteristics ->
                readCharacteristic(characteristics.uuid)
            }
        }

        ControlState(
            device = selectedDevice,
            services = services,
            bleMessage = bleMessage,
            userMessage = userMessage
        )

    }.flowOn(dispatcher)
        .stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(),
            ControlState(null, emptyList(), ConnectionState.UNKNOWN, null)
        )

    init {
        getDevice(address)
        //onConnect(address)
    }

    private fun getDevice(address: String) {
        viewModelScope.launch(dispatcher) {
            _selectedDevice.value = bleRepository.getDeviceByAddress(address)
        }
    }

    fun getReadBytes(): ByteArray? {
        return _services.value.let { serviceList : List<DeviceService> ->
            serviceList.flatMap { service ->
                service.characteristics
            }.find { characteristics ->
                characteristics.uuid == ELKBLEDOM.uuid
            }?.readBytes
        }
    }

    fun getOnOffState(): Boolean {
        val bytes = getReadBytes()
        return bytes?.let {
            ELKBLEDOM.onOffState(it)
        } ?: false
    }

    fun toggleOnOff() {
        val isOn = getOnOffState()
        if (isOn)
            onWriteCharacteristic(ELKBLEDOM.uuid, ELKBLEDOM.OFF)
        else
            onWriteCharacteristic(ELKBLEDOM.uuid, ELKBLEDOM.ON)
    }

    fun deviceOn() {
        onWriteCharacteristic(ELKBLEDOM.uuid, ELKBLEDOM.ON)
    }

    fun deviceOff() {
        onWriteCharacteristic(ELKBLEDOM.uuid, ELKBLEDOM.OFF)
    }

    fun changeColor(hexColor: String) {
        val colorCmd = ELKBLEDOM.COLOR.substringAfter("0x")
            .replace("xxxxxx", hexColor)
        onWriteCharacteristic(ELKBLEDOM.uuid, colorCmd)
    }

    fun changeBrightness(value: Int) {
        val brightnessCmd = ELKBLEDOM.BRIGHTNESS.substringAfter("0x")
            .replace("xx", value.toHex2())
        Timber.d("$value, $brightnessCmd")
        onWriteCharacteristic(ELKBLEDOM.uuid, brightnessCmd)
    }

    fun onConnect(address: String) {
        bleGatt.connect(address)
    }

    fun onDisconnect() {
        Timber.d("calling disconnect...")
        bleGatt.close()
    }

    fun readCharacteristic(uuid: String) {
        bleGatt.readCharacteristic(uuid)
        //showUserMessage("Request sent.")
    }

    fun onWriteCharacteristic(uuid: String, bytes: String) {
        try {
            if (bytes.isNotEmpty()) {
                bleGatt.writeBytes(uuid, bytes.decodeHex())
                //showUserMessage("Data sent.")
            } else
                showUserMessage("Hex can't be null.")
        } catch (badHex: Exception) {
            showUserMessage("Invalid Hex String. Must be an even count.")
        }
    }

    fun showUserMessage(message: String) {
        Timber.d("show message...")
        _userMessage.value = message
    }

    fun userMessageShown() {
        Timber.tag("debug").d("user message set to null.")
        _userMessage.value = null
    }

}

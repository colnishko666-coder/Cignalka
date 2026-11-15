package com.example.data.ble

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.example.data.model.BleConnectionState
import com.example.data.model.CarCommand
import com.example.data.model.ConnectionStatus
import com.example.data.model.ScannedBleDevice
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

class IbtBleController(private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val bluetoothManager: BluetoothManager? =
        context.getSystemService(BluetoothManager::class.java)
    private val bluetoothAdapter: BluetoothAdapter?
        get() = bluetoothManager?.adapter

    private val _connectionState = MutableStateFlow(BleConnectionState())
    val connectionState: StateFlow<BleConnectionState> = _connectionState.asStateFlow()

    private val _discoveredDevices = MutableStateFlow<List<ScannedBleDevice>>(emptyList())
    val discoveredDevices: StateFlow<List<ScannedBleDevice>> = _discoveredDevices.asStateFlow()

    private var activeGatt: BluetoothGatt? = null
    private var commandCharacteristic: BluetoothGattCharacteristic? = null
    private var scanJob: Job? = null
    private var rssiPollJob: Job? = null
    private var activeScanner: BluetoothLeScanner? = null

    private val knownServiceUuids = setOf(
        UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb"),
        UUID.fromString("0000fff0-0000-1000-8000-00805f9b34fb"),
        UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e")
    )

    private val knownWriteCharUuids = setOf(
        UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb"),
        UUID.fromString("0000fff1-0000-1000-8000-00805f9b34fb"),
        UUID.fromString("0000fff2-0000-1000-8000-00805f9b34fb"),
        UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e")
    )

    fun requiredPermissions(): Array<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_ADVERTISE
            )
        } else {
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        }
    }

    fun hasRequiredPermissions(): Boolean {
        return requiredPermissions().all { perm ->
            ContextCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun refreshAdapterState(simulationModeEnabled: Boolean) {
        val hasPerm = hasRequiredPermissions()
        val isBtOn = bluetoothAdapter?.isEnabled == true
        _connectionState.update { current ->
            val newStatus = when {
                activeGatt != null && current.status == ConnectionStatus.CONNECTED_BLE -> ConnectionStatus.CONNECTED_BLE
                current.status == ConnectionStatus.CONNECTING -> ConnectionStatus.CONNECTING
                simulationModeEnabled -> ConnectionStatus.SIMULATION
                else -> ConnectionStatus.DISCONNECTED
            }
            current.copy(
                hasBlePermission = hasPerm,
                isBluetoothEnabled = isBtOn,
                status = newStatus
            )
        }
    }

    @SuppressLint("MissingPermission")
    fun startScan() {
        if (!hasRequiredPermissions()) {
            _connectionState.update {
                it.copy(
                    hasBlePermission = false,
                    lastError = "Не выданы разрешения Bluetooth"
                )
            }
            return
        }
        val adapter = bluetoothAdapter
        if (adapter == null || !adapter.isEnabled) {
            _connectionState.update {
                it.copy(
                    isBluetoothEnabled = false,
                    lastError = "Включите Bluetooth на смартфоне"
                )
            }
            return
        }

        stopScan()
        _discoveredDevices.value = emptyList()
        val scanner = adapter.bluetoothLeScanner
        if (scanner == null) {
            _connectionState.update {
                it.copy(lastError = "BLE-сканер недоступен на этом устройстве")
            }
            return
        }

        activeScanner = scanner
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        _connectionState.update { it.copy(isScanning = true, lastError = null) }
        try {
            scanner.startScan(null, settings, scanCallback)
            scanJob = scope.launch {
                delay(12_000L)
                stopScan()
            }
        } catch (e: SecurityException) {
            _connectionState.update {
                it.copy(isScanning = false, lastError = "Ошибка доступа к Bluetooth: ${e.message}")
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun stopScan() {
        scanJob?.cancel()
        scanJob = null
        if (hasRequiredPermissions()) {
            try {
                activeScanner?.stopScan(scanCallback)
            } catch (_: Exception) {
            }
        }
        _connectionState.update { it.copy(isScanning = false) }
    }

    @SuppressLint("MissingPermission")
    fun connectToDevice(macAddress: String, deviceName: String) {
        if (!hasRequiredPermissions()) {
            _connectionState.update { it.copy(lastError = "Нет разрешения BLUETOOTH_CONNECT") }
            return
        }
        val adapter = bluetoothAdapter ?: run {
            _connectionState.update { it.copy(lastError = "Bluetooth адаптер не найден") }
            return
        }

        stopScan()
        disconnect(keepSimulationFallback = false)

        try {
            val device = adapter.getRemoteDevice(macAddress)
            _connectionState.update {
                it.copy(
                    status = ConnectionStatus.CONNECTING,
                    connectedDeviceName = deviceName,
                    connectedDeviceMac = macAddress,
                    lastError = null
                )
            }
            activeGatt = device.connectGatt(context, false, gattCallback, BluetoothDeviceTransportCompat.LE)
        } catch (e: Exception) {
            _connectionState.update {
                it.copy(
                    status = ConnectionStatus.DISCONNECTED,
                    lastError = "Не удалось подключиться: ${e.localizedMessage}"
                )
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun disconnect(keepSimulationFallback: Boolean = true) {
        rssiPollJob?.cancel()
        rssiPollJob = null
        commandCharacteristic = null
        if (hasRequiredPermissions()) {
            try {
                activeGatt?.disconnect()
                activeGatt?.close()
            } catch (_: Exception) {
            }
        }
        activeGatt = null
        _connectionState.update {
            it.copy(
                status = if (keepSimulationFallback) ConnectionStatus.SIMULATION else ConnectionStatus.DISCONNECTED,
                connectedDeviceName = null,
                connectedDeviceMac = null
            )
        }
    }

    @SuppressLint("MissingPermission")
    fun sendCommand(command: CarCommand, pinCode: String): Boolean {
        val gatt = activeGatt ?: return false
        val char = commandCharacteristic ?: return false
        if (!hasRequiredPermissions()) return false

        val payload = buildCommandFrame(command, pinCode)
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val status = gatt.writeCharacteristic(
                    char,
                    payload,
                    BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                )
                status == BluetoothGatt.GATT_SUCCESS || status == 0
            } else {
                @Suppress("DEPRECATION")
                char.value = payload
                char.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                @Suppress("DEPRECATION")
                gatt.writeCharacteristic(char)
            }
        } catch (_: Exception) {
            false
        }
    }

    private fun buildCommandFrame(command: CarCommand, pinCode: String): ByteArray {
        val pinDigits = pinCode.padEnd(4, '0').take(4).map { it.code.toByte() }
        val header0 = 0xAA.toByte()
        val header1 = 0x55.toByte()
        val cmd = command.codeHex
        val checksum = (cmd.toInt() xor pinDigits.fold(0) { acc, b -> acc xor b.toInt() }).toByte()
        return byteArrayOf(header0, header1, cmd) + pinDigits.toByteArray() + byteArrayOf(checksum)
    }

    private val scanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device ?: return
            val rawName = try {
                device.name ?: result.scanRecord?.deviceName
            } catch (_: SecurityException) {
                null
            }
            val displayName = rawName?.takeIf { it.isNotBlank() } ?: "BLE Устройство"
            val mac = device.address ?: return
            val isLikelyIbt = displayName.contains("IBT", ignoreCase = true) ||
                displayName.contains("ALARM", ignoreCase = true) ||
                displayName.contains("PKE", ignoreCase = true) ||
                displayName.contains("CAR", ignoreCase = true) ||
                displayName.contains("SPY", ignoreCase = true)

            _discoveredDevices.update { current ->
                val updated = current.filterNot { it.macAddress == mac } + ScannedBleDevice(
                    name = displayName,
                    macAddress = mac,
                    rssi = result.rssi,
                    isLikelyIbtModule = isLikelyIbt
                )
                updated.sortedWith(
                    compareByDescending<ScannedBleDevice> { it.isLikelyIbtModule }
                        .thenByDescending { it.rssi }
                )
            }
        }

        override fun onScanFailed(errorCode: Int) {
            _connectionState.update {
                it.copy(
                    isScanning = false,
                    lastError = "Ошибка сканирования BLE (код $errorCode)"
                )
            }
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                _connectionState.update {
                    it.copy(
                        status = ConnectionStatus.CONNECTED_BLE,
                        lastError = null
                    )
                }
                if (hasRequiredPermissions()) {
                    try {
                        gatt.discoverServices()
                        startRssiPolling(gatt)
                    } catch (_: SecurityException) {
                    }
                }
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                rssiPollJob?.cancel()
                commandCharacteristic = null
                activeGatt = null
                try {
                    gatt.close()
                } catch (_: Exception) {
                }
                _connectionState.update {
                    it.copy(
                        status = ConnectionStatus.DISCONNECTED,
                        lastError = if (status != BluetoothGatt.GATT_SUCCESS) {
                            "Соединение разорвано (статус $status)"
                        } else null
                    )
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) return
            val services = gatt.services ?: return

            // 1. Prefer known IBT/serial-over-BLE characteristics
            val preferred = services
                .filter { it.uuid in knownServiceUuids }
                .flatMap { it.characteristics.orEmpty() }
                .firstOrNull { it.uuid in knownWriteCharUuids && it.isWritable() }

            // 2. Fallback to any writable characteristic in known services
            val fallbackInKnown = services
                .filter { it.uuid in knownServiceUuids }
                .flatMap { it.characteristics.orEmpty() }
                .firstOrNull { it.isWritable() }

            // 3. Fallback to any writable characteristic
            val anyWritable = services
                .flatMap { it.characteristics.orEmpty() }
                .firstOrNull { it.isWritable() }

            commandCharacteristic = preferred ?: fallbackInKnown ?: anyWritable
        }

        override fun onReadRemoteRssi(gatt: BluetoothGatt, rssi: Int, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                _connectionState.update { it.copy(liveRssi = rssi) }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun startRssiPolling(gatt: BluetoothGatt) {
        rssiPollJob?.cancel()
        rssiPollJob = scope.launch {
            while (activeGatt == gatt) {
                if (hasRequiredPermissions()) {
                    try {
                        gatt.readRemoteRssi()
                    } catch (_: Exception) {
                    }
                }
                delay(1_500L)
            }
        }
    }

    private fun BluetoothGattCharacteristic.isWritable(): Boolean {
        return (properties and BluetoothGattCharacteristic.PROPERTY_WRITE) != 0 ||
            (properties and BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) != 0
    }
}

private object BluetoothDeviceTransportCompat {
    const val LE = 2 // BluetoothDevice.TRANSPORT_LE
}

package com.example.data.model

enum class ThemeMode(val label: String) {
    DARK("Тёмная (Кокпит)"),
    LIGHT("Светлая"),
    SYSTEM("Системная")
}

enum class EngineState {
    STOPPED,
    CRANKING,
    RUNNING
}

enum class SecurityState {
    ARMED,
    DISARMED,
    PANIC
}

enum class ConnectionStatus {
    CONNECTED_BLE,
    CONNECTING,
    SIMULATION,
    DISCONNECTED
}

enum class CarCommand(val title: String, val codeHex: Byte) {
    LOCK("Постановка на охрану", 0x01),
    UNLOCK("Снятие с охраны", 0x02),
    ENGINE_START("Автозапуск двигателя", 0x03),
    ENGINE_STOP("Остановка двигателя", 0x04),
    TRUNK_OPEN("Открытие багажника", 0x05),
    FIND_CAR("Поиск автомобиля", 0x06),
    PKE_ENABLE("Включение PKE", 0x07),
    PKE_DISABLE("Отключение PKE", 0x08)
}

data class ScannedBleDevice(
    val name: String,
    val macAddress: String,
    val rssi: Int,
    val isLikelyIbtModule: Boolean
)

data class VehicleStatus(
    val securityState: SecurityState = SecurityState.ARMED,
    val engineState: EngineState = EngineState.STOPPED,
    val isTrunkOpen: Boolean = false,
    val engineRemainingSeconds: Int = 0,
    val engineTotalSeconds: Int = 15 * 60,
    val batteryVoltage: Float = 12.6f,
    val cabinTempCelsius: Int = 18,
    val currentRssi: Int = -62,
    val isSimulatedTelemetry: Boolean = true
)

data class VehicleSettings(
    val carName: String = "Мой автомобиль",
    val pairedDeviceName: String = "",
    val pairedDeviceMac: String = "",
    val defaultWarmupMinutes: Int = 15,
    val pkeEnabled: Boolean = true,
    val pkeUnlockRssi: Int = -65,
    val pkeLockRssi: Int = -82,
    val shakeControlEnabled: Boolean = false,
    val holdToConfirmEnabled: Boolean = true,
    val hapticEnabled: Boolean = true,
    val pinCode: String = "1234",
    val simulationModeEnabled: Boolean = true,
    val themeMode: ThemeMode = ThemeMode.DARK
)

data class BleConnectionState(
    val status: ConnectionStatus = ConnectionStatus.SIMULATION,
    val connectedDeviceName: String? = null,
    val connectedDeviceMac: String? = null,
    val liveRssi: Int = -62,
    val isScanning: Boolean = false,
    val isBluetoothEnabled: Boolean = false,
    val hasBlePermission: Boolean = false,
    val lastError: String? = null
)

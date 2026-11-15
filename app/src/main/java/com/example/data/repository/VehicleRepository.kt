package com.example.data.repository

import android.content.Context
import com.example.data.ble.IbtBleController
import com.example.data.local.AppDatabase
import com.example.data.local.AutostartScheduleEntity
import com.example.data.local.EventLogEntity
import com.example.data.model.BleConnectionState
import com.example.data.model.CarCommand
import com.example.data.model.ConnectionStatus
import com.example.data.model.EngineState
import com.example.data.model.ScannedBleDevice
import com.example.data.model.SecurityState
import com.example.data.model.ThemeMode
import com.example.data.model.VehicleSettings
import com.example.data.model.VehicleStatus
import com.example.data.prefs.VehiclePreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

class VehicleRepository(context: Context) {

    private val database = AppDatabase.getInstance(context)
    private val eventLogDao = database.eventLogDao()
    private val scheduleDao = database.autostartScheduleDao()
    private val prefsRepo = VehiclePreferencesRepository(context)
    val bleController = IbtBleController(context)

    val settingsFlow: Flow<VehicleSettings> = prefsRepo.settingsFlow
    val recentLogsFlow: Flow<List<EventLogEntity>> = eventLogDao.observeRecentLogs()
    val schedulesFlow: Flow<List<AutostartScheduleEntity>> = scheduleDao.observeSchedules()

    val bleConnectionState: StateFlow<BleConnectionState> = bleController.connectionState
    val discoveredBleDevices: StateFlow<List<ScannedBleDevice>> = bleController.discoveredDevices

    suspend fun logEvent(
        title: String,
        detail: String,
        transport: String,
        isAlert: Boolean = false
    ) {
        eventLogDao.insertLog(
            EventLogEntity(
                title = title,
                detail = detail,
                transport = transport,
                isAlert = isAlert
            )
        )
    }

    suspend fun clearLogs() = eventLogDao.clearAll()

    suspend fun addSchedule(
        label: String,
        hour: Int,
        minute: Int,
        warmupMinutes: Int,
        daysMask: Int
    ) {
        scheduleDao.insert(
            AutostartScheduleEntity(
                label = label.trim().ifEmpty { "Утренний прогрев" },
                hour = hour,
                minute = minute,
                warmupMinutes = warmupMinutes,
                daysMask = daysMask,
                isEnabled = true
            )
        )
    }

    suspend fun toggleSchedule(id: Long, enabled: Boolean) = scheduleDao.setEnabled(id, enabled)

    suspend fun markScheduleTriggered(id: Long, dayKey: String) =
        scheduleDao.markTriggered(id, dayKey)

    suspend fun deleteSchedule(id: Long) = scheduleDao.deleteById(id)

    suspend fun updateCarName(name: String) = prefsRepo.updateCarName(name)
    suspend fun savePairedDevice(name: String, mac: String) = prefsRepo.savePairedDevice(name, mac)
    suspend fun setWarmupMinutes(minutes: Int) = prefsRepo.setWarmupMinutes(minutes)
    suspend fun setPkeEnabled(enabled: Boolean) = prefsRepo.setPkeEnabled(enabled)
    suspend fun setPkeThresholds(unlockRssi: Int, lockRssi: Int) =
        prefsRepo.setPkeThresholds(unlockRssi, lockRssi)
    suspend fun setShakeControlEnabled(enabled: Boolean) = prefsRepo.setShakeControlEnabled(enabled)
    suspend fun setHoldToConfirmEnabled(enabled: Boolean) = prefsRepo.setHoldToConfirmEnabled(enabled)
    suspend fun setHapticEnabled(enabled: Boolean) = prefsRepo.setHapticEnabled(enabled)
    suspend fun setPinCode(pin: String) = prefsRepo.setPinCode(pin)
    suspend fun setSimulationModeEnabled(enabled: Boolean) = prefsRepo.setSimulationModeEnabled(enabled)
    suspend fun setThemeMode(mode: ThemeMode) = prefsRepo.setThemeMode(mode)

    fun sendBleCommand(command: CarCommand, pinCode: String): Boolean {
        return bleController.sendCommand(command, pinCode)
    }

    fun applyCommandToStatus(
        current: VehicleStatus,
        command: CarCommand,
        warmupMinutes: Int,
        connectionStatus: ConnectionStatus
    ): VehicleStatus {
        val isSim = connectionStatus != ConnectionStatus.CONNECTED_BLE
        return when (command) {
            CarCommand.LOCK -> current.copy(
                securityState = SecurityState.ARMED,
                isSimulatedTelemetry = isSim
            )
            CarCommand.UNLOCK -> current.copy(
                securityState = SecurityState.DISARMED,
                isSimulatedTelemetry = isSim
            )
            CarCommand.ENGINE_START -> current.copy(
                engineState = EngineState.CRANKING,
                engineTotalSeconds = warmupMinutes * 60,
                engineRemainingSeconds = warmupMinutes * 60,
                securityState = SecurityState.ARMED,
                isSimulatedTelemetry = isSim
            )
            CarCommand.ENGINE_STOP -> current.copy(
                engineState = EngineState.STOPPED,
                engineRemainingSeconds = 0,
                batteryVoltage = 12.6f,
                isSimulatedTelemetry = isSim
            )
            CarCommand.TRUNK_OPEN -> current.copy(
                isTrunkOpen = !current.isTrunkOpen,
                isSimulatedTelemetry = isSim
            )
            CarCommand.FIND_CAR -> current.copy(
                securityState = SecurityState.PANIC,
                isSimulatedTelemetry = isSim
            )
            CarCommand.PKE_ENABLE, CarCommand.PKE_DISABLE -> current.copy(
                isSimulatedTelemetry = isSim
            )
        }
    }
}

package com.example.ui

import android.app.Application
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
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
import com.example.data.repository.VehicleRepository
import com.example.data.sensor.ShakeDetector
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar

enum class AppTab(val label: String) {
    CONTROL("Пульт"),
    AUTOMATION("Авто и PKE"),
    SETTINGS("Устройство")
}

data class VehicleUiState(
    val currentTab: AppTab = AppTab.CONTROL,
    val status: VehicleStatus = VehicleStatus(),
    val settings: VehicleSettings = VehicleSettings(),
    val bleState: BleConnectionState = BleConnectionState(),
    val discoveredDevices: List<ScannedBleDevice> = emptyList(),
    val eventLogs: List<EventLogEntity> = emptyList(),
    val schedules: List<AutostartScheduleEntity> = emptyList(),
    val shakeSupported: Boolean = true,
    val liveGForce: Float = 1.0f,
    val simulatedPkeRssi: Int = -62,
    val feedbackMessage: String? = null
)

class VehicleViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = VehicleRepository(application.applicationContext)

    private val _currentTab = MutableStateFlow(AppTab.CONTROL)
    private val _status = MutableStateFlow(VehicleStatus())
    private val _simulatedPkeRssi = MutableStateFlow(-62)
    private val _feedbackMessage = MutableStateFlow<String?>(null)

    private var engineJob: Job? = null
    private var panicJob: Job? = null
    private var lastPkeZone: Boolean? = null // true = inside unlock zone, false = outside lock zone

    val shakeDetector = ShakeDetector(application.applicationContext) {
        onShakeGesture()
    }

    private data class LocalRuntimeState(
        val tab: AppTab,
        val status: VehicleStatus,
        val simRssi: Int,
        val gForce: Float,
        val feedback: String?
    )

    private val runtimeFlow = combine(
        _currentTab,
        _status,
        _simulatedPkeRssi,
        shakeDetector.currentGForce,
        _feedbackMessage
    ) { tab, status, simRssi, gForce, feedback ->
        LocalRuntimeState(tab, status, simRssi, gForce, feedback)
    }

    private val dataFlow = combine(
        repository.settingsFlow,
        repository.bleConnectionState,
        repository.discoveredBleDevices,
        repository.recentLogsFlow,
        repository.schedulesFlow
    ) { settings, ble, devices, logs, schedules ->
        DataBundle(settings, ble, devices, logs, schedules)
    }

    private data class DataBundle(
        val settings: VehicleSettings,
        val ble: BleConnectionState,
        val devices: List<ScannedBleDevice>,
        val logs: List<EventLogEntity>,
        val schedules: List<AutostartScheduleEntity>
    )

    val uiState: StateFlow<VehicleUiState> = combine(
        runtimeFlow,
        dataFlow,
        shakeDetector.isSupported
    ) { runtime, data, shakeSupported ->
        val effectiveStatus = runtime.status.copy(
            currentRssi = if (data.ble.status == ConnectionStatus.CONNECTED_BLE) {
                data.ble.liveRssi
            } else {
                runtime.simRssi
            },
            isSimulatedTelemetry = data.ble.status != ConnectionStatus.CONNECTED_BLE
        )
        VehicleUiState(
            currentTab = runtime.tab,
            status = effectiveStatus,
            settings = data.settings,
            bleState = data.ble,
            discoveredDevices = data.devices,
            eventLogs = data.logs,
            schedules = data.schedules,
            shakeSupported = shakeSupported,
            liveGForce = runtime.gForce,
            simulatedPkeRssi = runtime.simRssi,
            feedbackMessage = runtime.feedback
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = VehicleUiState()
    )

    init {
        refreshBlePermissionsAndAdapter()
        startScheduleWatcher()
        observeBleRssiForPke()
    }

    fun selectTab(tab: AppTab) {
        _currentTab.value = tab
    }

    fun consumeFeedback() {
        _feedbackMessage.value = null
    }

    fun refreshBlePermissionsAndAdapter() {
        val simEnabled = uiState.value.settings.simulationModeEnabled
        repository.bleController.refreshAdapterState(simEnabled)
    }

    fun blePermissionsArray(): Array<String> = repository.bleController.requiredPermissions()

    fun startBleScan() {
        repository.bleController.startScan()
    }

    fun stopBleScan() {
        repository.bleController.stopScan()
    }

    fun connectToBleDevice(device: ScannedBleDevice) {
        repository.bleController.connectToDevice(device.macAddress, device.name)
        viewModelScope.launch {
            repository.savePairedDevice(device.name, device.macAddress)
            repository.setSimulationModeEnabled(false)
            repository.logEvent(
                title = "Подключение к модулю",
                detail = "${device.name} (${device.macAddress})",
                transport = "BLE"
            )
        }
    }

    fun disconnectBle() {
        val simEnabled = uiState.value.settings.simulationModeEnabled
        repository.bleController.disconnect(keepSimulationFallback = simEnabled)
        viewModelScope.launch {
            repository.logEvent(
                title = "Отключение Bluetooth",
                detail = "Связь с модулем завершена",
                transport = "BLE"
            )
        }
    }

    fun setSimulationMode(enabled: Boolean) {
        viewModelScope.launch {
            repository.setSimulationModeEnabled(enabled)
            repository.bleController.refreshAdapterState(enabled)
            _feedbackMessage.value = if (enabled) {
                "Режим симуляции включён"
            } else {
                "Режим симуляции отключён (только реальный BLE)"
            }
        }
    }

    fun sendCommand(command: CarCommand, customWarmupMinutes: Int? = null, sourceLabel: String? = null) {
        val state = uiState.value
        val connStatus = state.bleState.status
        val warmupMins = customWarmupMinutes ?: state.settings.defaultWarmupMinutes

        if (connStatus != ConnectionStatus.CONNECTED_BLE && !state.settings.simulationModeEnabled) {
            _feedbackMessage.value = "Нет подключения к модулю BLE. Подключите блок или включите симуляцию."
            return
        }

        val transportLabel = sourceLabel ?: if (connStatus == ConnectionStatus.CONNECTED_BLE) {
            val sent = repository.sendBleCommand(command, state.settings.pinCode)
            if (!sent) {
                _feedbackMessage.value = "Команда поставлена в очередь BLE (нет GATT-характеристики записи)"
            }
            "BLE"
        } else {
            "Симуляция"
        }

        triggerHaptic(state.settings.hapticEnabled)

        when (command) {
            CarCommand.ENGINE_START -> startEngineSequence(warmupMins, connStatus, transportLabel)
            CarCommand.ENGINE_STOP -> stopEngineSequence(connStatus, transportLabel)
            CarCommand.FIND_CAR -> triggerFindCar(connStatus, transportLabel)
            else -> {
                _status.update {
                    repository.applyCommandToStatus(it, command, warmupMins, connStatus)
                }
                val detail = when (command) {
                    CarCommand.LOCK -> "Двери заблокированы, охрана активна"
                    CarCommand.UNLOCK -> "Двери разблокированы, охрана снята"
                    CarCommand.TRUNK_OPEN -> if (!_status.value.isTrunkOpen) "Крышка багажника закрыта" else "Привод багажника активирован"
                    CarCommand.PKE_ENABLE -> "Бесключевой доступ включён"
                    CarCommand.PKE_DISABLE -> "Бесключевой доступ отключён"
                    else -> command.title
                }
                _feedbackMessage.value = command.title
                viewModelScope.launch {
                    repository.logEvent(command.title, detail, transportLabel)
                }
            }
        }
    }

    private fun startEngineSequence(warmupMinutes: Int, connStatus: ConnectionStatus, transport: String) {
        engineJob?.cancel()
        _status.update {
            repository.applyCommandToStatus(it, CarCommand.ENGINE_START, warmupMinutes, connStatus)
        }
        _feedbackMessage.value = "Запуск двигателя ($warmupMinutes мин)…"
        viewModelScope.launch {
            repository.logEvent(
                title = "Автозапуск двигателя",
                detail = "Запрос запуска на $warmupMinutes мин",
                transport = transport
            )
        }

        engineJob = viewModelScope.launch {
            delay(1_400L)
            _status.update {
                it.copy(
                    engineState = EngineState.RUNNING,
                    batteryVoltage = 14.2f
                )
            }
            _feedbackMessage.value = "Двигатель запущен • Прогрев $warmupMinutes мин"
            var tickCounter = 0
            while (_status.value.engineRemainingSeconds > 0 && _status.value.engineState == EngineState.RUNNING) {
                delay(1_000L)
                tickCounter++
                _status.update { current ->
                    val nextRem = (current.engineRemainingSeconds - 1).coerceAtLeast(0)
                    val nextTemp = if (tickCounter % 18 == 0 && current.cabinTempCelsius < 24) {
                        current.cabinTempCelsius + 1
                    } else {
                        current.cabinTempCelsius
                    }
                    if (nextRem == 0) {
                        current.copy(
                            engineState = EngineState.STOPPED,
                            engineRemainingSeconds = 0,
                            batteryVoltage = 12.6f,
                            cabinTempCelsius = nextTemp
                        )
                    } else {
                        current.copy(
                            engineRemainingSeconds = nextRem,
                            cabinTempCelsius = nextTemp
                        )
                    }
                }
            }
            if (_status.value.engineState == EngineState.STOPPED && _status.value.engineRemainingSeconds == 0) {
                repository.logEvent(
                    title = "Прогрев завершён",
                    detail = "Двигатель остановлен по таймеру",
                    transport = transport
                )
            }
        }
    }

    private fun stopEngineSequence(connStatus: ConnectionStatus, transport: String) {
        engineJob?.cancel()
        _status.update {
            repository.applyCommandToStatus(it, CarCommand.ENGINE_STOP, 0, connStatus)
        }
        _feedbackMessage.value = "Двигатель заглушен"
        viewModelScope.launch {
            repository.logEvent(
                title = "Остановка двигателя",
                detail = "Зажигание выключено пользователем",
                transport = transport
            )
        }
    }

    private fun triggerFindCar(connStatus: ConnectionStatus, transport: String) {
        val previousSecurity = if (_status.value.securityState == SecurityState.PANIC) {
            SecurityState.ARMED
        } else {
            _status.value.securityState
        }
        panicJob?.cancel()
        _status.update {
            repository.applyCommandToStatus(it, CarCommand.FIND_CAR, 0, connStatus)
        }
        _feedbackMessage.value = "Поиск авто: подан звуковой и световой сигнал"
        viewModelScope.launch {
            repository.logEvent(
                title = "Поиск автомобиля",
                detail = "Мигание поворотников и сигнал сирены",
                transport = transport,
                isAlert = true
            )
        }
        panicJob = viewModelScope.launch {
            delay(4_500L)
            _status.update { it.copy(securityState = previousSecurity) }
        }
    }

    fun updateSimulatedPkeRssi(rssi: Int) {
        _simulatedPkeRssi.value = rssi
        evaluatePkeZone(rssi, isSimulated = true)
    }

    private fun observeBleRssiForPke() {
        viewModelScope.launch {
            repository.bleConnectionState.collect { ble ->
                if (ble.status == ConnectionStatus.CONNECTED_BLE) {
                    evaluatePkeZone(ble.liveRssi, isSimulated = false)
                }
            }
        }
    }

    private fun evaluatePkeZone(rssi: Int, isSimulated: Boolean) {
        val settings = uiState.value.settings
        if (!settings.pkeEnabled) return
        if (isSimulated && !settings.simulationModeEnabled) return

        val currentSecurity = _status.value.securityState
        if (rssi >= settings.pkeUnlockRssi && lastPkeZone != true) {
            lastPkeZone = true
            if (currentSecurity == SecurityState.ARMED) {
                sendCommand(CarCommand.UNLOCK, sourceLabel = if (isSimulated) "PKE (Симуляция)" else "PKE (BLE)")
            }
        } else if (rssi <= settings.pkeLockRssi && lastPkeZone != false) {
            lastPkeZone = false
            if (currentSecurity == SecurityState.DISARMED) {
                sendCommand(CarCommand.LOCK, sourceLabel = if (isSimulated) "PKE (Симуляция)" else "PKE (BLE)")
            }
        }
    }

    private fun onShakeGesture() {
        val state = uiState.value
        if (!state.settings.shakeControlEnabled) return
        val nextCmd = if (state.status.securityState == SecurityState.ARMED) {
            CarCommand.UNLOCK
        } else {
            CarCommand.LOCK
        }
        sendCommand(nextCmd, sourceLabel = "Встряхивание")
    }

    private fun startScheduleWatcher() {
        viewModelScope.launch {
            while (true) {
                delay(15_000L)
                val now = Calendar.getInstance()
                val hour = now.get(Calendar.HOUR_OF_DAY)
                val minute = now.get(Calendar.MINUTE)
                // Calendar.MONDAY=2..SUNDAY=1 -> map to bit 0..6 (Mon..Sun)
                val dayIndex = when (now.get(Calendar.DAY_OF_WEEK)) {
                    Calendar.MONDAY -> 0
                    Calendar.TUESDAY -> 1
                    Calendar.WEDNESDAY -> 2
                    Calendar.THURSDAY -> 3
                    Calendar.FRIDAY -> 4
                    Calendar.SATURDAY -> 5
                    Calendar.SUNDAY -> 6
                    else -> 0
                }
                val dayKey = "${now.get(Calendar.YEAR)}-${now.get(Calendar.DAY_OF_YEAR)}-$hour:$minute"

                val activeSchedules = uiState.value.schedules
                for (schedule in activeSchedules) {
                    if (!schedule.isEnabled) continue
                    if (schedule.hour != hour || schedule.minute != minute) continue
                    if (schedule.lastTriggeredDayKey == dayKey) continue
                    val matchesDay = schedule.daysMask == 0 || ((schedule.daysMask and (1 shl dayIndex)) != 0)
                    if (matchesDay) {
                        repository.markScheduleTriggered(schedule.id, dayKey)
                        if (schedule.daysMask == 0) {
                            repository.toggleSchedule(schedule.id, false)
                        }
                        if (uiState.value.status.engineState == EngineState.STOPPED) {
                            sendCommand(
                                command = CarCommand.ENGINE_START,
                                customWarmupMinutes = schedule.warmupMinutes,
                                sourceLabel = "Таймер «${schedule.label}»"
                            )
                        }
                    }
                }
            }
        }
    }

    // Settings mutations
    fun updateCarName(name: String) = viewModelScope.launch { repository.updateCarName(name) }
    fun setDefaultWarmupMinutes(mins: Int) = viewModelScope.launch {
        repository.setWarmupMinutes(mins)
        if (_status.value.engineState == EngineState.STOPPED) {
            _status.update { it.copy(engineTotalSeconds = mins * 60) }
        }
    }
    fun setPkeEnabled(enabled: Boolean) = viewModelScope.launch {
        repository.setPkeEnabled(enabled)
        sendCommand(if (enabled) CarCommand.PKE_ENABLE else CarCommand.PKE_DISABLE)
    }
    fun setPkeThresholds(unlockRssi: Int, lockRssi: Int) = viewModelScope.launch {
        repository.setPkeThresholds(unlockRssi, lockRssi)
    }
    fun setShakeControlEnabled(enabled: Boolean) = viewModelScope.launch {
        repository.setShakeControlEnabled(enabled)
    }
    fun setHoldToConfirmEnabled(enabled: Boolean) = viewModelScope.launch {
        repository.setHoldToConfirmEnabled(enabled)
    }
    fun setHapticEnabled(enabled: Boolean) = viewModelScope.launch {
        repository.setHapticEnabled(enabled)
    }
    fun setPinCode(pin: String) = viewModelScope.launch { repository.setPinCode(pin) }
    fun setThemeMode(mode: ThemeMode) = viewModelScope.launch { repository.setThemeMode(mode) }

    fun addSchedule(label: String, hour: Int, minute: Int, warmupMinutes: Int, daysMask: Int) {
        viewModelScope.launch {
            repository.addSchedule(label, hour, minute, warmupMinutes, daysMask)
            _feedbackMessage.value = "Таймер прогрева сохранён на %02d:%02d".format(hour, minute)
        }
    }

    fun toggleSchedule(id: Long, enabled: Boolean) = viewModelScope.launch {
        repository.toggleSchedule(id, enabled)
    }

    fun deleteSchedule(id: Long) = viewModelScope.launch {
        repository.deleteSchedule(id)
    }

    fun clearLogs() = viewModelScope.launch {
        repository.clearLogs()
    }

    private fun triggerHaptic(enabled: Boolean) {
        if (!enabled) return
        val app = getApplication<Application>()
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = app.getSystemService(VibratorManager::class.java)
                vm?.defaultVibrator?.vibrate(
                    VibrationEffect.createOneShot(45L, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = app.getSystemService(Vibrator::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(
                        VibrationEffect.createOneShot(45L, VibrationEffect.DEFAULT_AMPLITUDE)
                    )
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(45L)
                }
            }
        } catch (_: Exception) {
        }
    }
}

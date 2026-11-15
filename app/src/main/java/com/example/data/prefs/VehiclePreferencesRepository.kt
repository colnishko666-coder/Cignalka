package com.example.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.ThemeMode
import com.example.data.model.VehicleSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.vehicleDataStore: DataStore<Preferences> by preferencesDataStore(name = "ibt_vehicle_prefs")

class VehiclePreferencesRepository(private val context: Context) {

    private object Keys {
        val CAR_NAME = stringPreferencesKey("car_name")
        val PAIRED_NAME = stringPreferencesKey("paired_device_name")
        val PAIRED_MAC = stringPreferencesKey("paired_device_mac")
        val WARMUP_MINUTES = intPreferencesKey("default_warmup_minutes")
        val PKE_ENABLED = booleanPreferencesKey("pke_enabled")
        val PKE_UNLOCK_RSSI = intPreferencesKey("pke_unlock_rssi")
        val PKE_LOCK_RSSI = intPreferencesKey("pke_lock_rssi")
        val SHAKE_ENABLED = booleanPreferencesKey("shake_control_enabled")
        val HOLD_CONFIRM = booleanPreferencesKey("hold_to_confirm_enabled")
        val HAPTIC_ENABLED = booleanPreferencesKey("haptic_enabled")
        val PIN_CODE = stringPreferencesKey("pin_code")
        val SIMULATION_MODE = booleanPreferencesKey("simulation_mode_enabled")
        val THEME_MODE = stringPreferencesKey("theme_mode")
    }

    val settingsFlow: Flow<VehicleSettings> = context.vehicleDataStore.data.map { prefs ->
        val themeStr = prefs[Keys.THEME_MODE] ?: ThemeMode.DARK.name
        val theme = runCatching { ThemeMode.valueOf(themeStr) }.getOrDefault(ThemeMode.DARK)
        VehicleSettings(
            carName = prefs[Keys.CAR_NAME] ?: "Мой автомобиль",
            pairedDeviceName = prefs[Keys.PAIRED_NAME] ?: "",
            pairedDeviceMac = prefs[Keys.PAIRED_MAC] ?: "",
            defaultWarmupMinutes = prefs[Keys.WARMUP_MINUTES] ?: 15,
            pkeEnabled = prefs[Keys.PKE_ENABLED] ?: true,
            pkeUnlockRssi = prefs[Keys.PKE_UNLOCK_RSSI] ?: -65,
            pkeLockRssi = prefs[Keys.PKE_LOCK_RSSI] ?: -82,
            shakeControlEnabled = prefs[Keys.SHAKE_ENABLED] ?: false,
            holdToConfirmEnabled = prefs[Keys.HOLD_CONFIRM] ?: true,
            hapticEnabled = prefs[Keys.HAPTIC_ENABLED] ?: true,
            pinCode = prefs[Keys.PIN_CODE] ?: "1234",
            simulationModeEnabled = prefs[Keys.SIMULATION_MODE] ?: true,
            themeMode = theme
        )
    }

    suspend fun updateCarName(name: String) {
        context.vehicleDataStore.edit { it[Keys.CAR_NAME] = name.trim().ifEmpty { "Мой автомобиль" } }
    }

    suspend fun savePairedDevice(name: String, mac: String) {
        context.vehicleDataStore.edit {
            it[Keys.PAIRED_NAME] = name
            it[Keys.PAIRED_MAC] = mac
        }
    }

    suspend fun setWarmupMinutes(minutes: Int) {
        context.vehicleDataStore.edit { it[Keys.WARMUP_MINUTES] = minutes.coerceIn(5, 45) }
    }

    suspend fun setPkeEnabled(enabled: Boolean) {
        context.vehicleDataStore.edit { it[Keys.PKE_ENABLED] = enabled }
    }

    suspend fun setPkeThresholds(unlockRssi: Int, lockRssi: Int) {
        context.vehicleDataStore.edit {
            it[Keys.PKE_UNLOCK_RSSI] = unlockRssi
            it[Keys.PKE_LOCK_RSSI] = lockRssi
        }
    }

    suspend fun setShakeControlEnabled(enabled: Boolean) {
        context.vehicleDataStore.edit { it[Keys.SHAKE_ENABLED] = enabled }
    }

    suspend fun setHoldToConfirmEnabled(enabled: Boolean) {
        context.vehicleDataStore.edit { it[Keys.HOLD_CONFIRM] = enabled }
    }

    suspend fun setHapticEnabled(enabled: Boolean) {
        context.vehicleDataStore.edit { it[Keys.HAPTIC_ENABLED] = enabled }
    }

    suspend fun setPinCode(pin: String) {
        val sanitized = pin.filter { it.isDigit() }.take(6).ifEmpty { "1234" }
        context.vehicleDataStore.edit { it[Keys.PIN_CODE] = sanitized }
    }

    suspend fun setSimulationModeEnabled(enabled: Boolean) {
        context.vehicleDataStore.edit { it[Keys.SIMULATION_MODE] = enabled }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.vehicleDataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }
}

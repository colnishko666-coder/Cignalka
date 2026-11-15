package com.example.ui.screens

import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatterySaver
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.BluetoothConnected
import androidx.compose.material.icons.rounded.BluetoothSearching
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Pin
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.ConnectionStatus
import com.example.data.model.ScannedBleDevice
import com.example.data.model.ThemeMode
import com.example.ui.VehicleUiState
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.IgnitionAmber
import com.example.ui.theme.MonoFontFamily
import com.example.ui.theme.SafeEmerald

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    uiState: VehicleUiState,
    blePermissions: Array<String>,
    onRefreshBleState: () -> Unit,
    onStartScan: () -> Unit,
    onStopScan: () -> Unit,
    onConnectDevice: (ScannedBleDevice) -> Unit,
    onDisconnectBle: () -> Unit,
    onSetSimulationMode: (Boolean) -> Unit,
    onUpdateCarName: (String) -> Unit,
    onUpdatePin: (String) -> Unit,
    onSetHoldToConfirm: (Boolean) -> Unit,
    onSetHaptic: (Boolean) -> Unit,
    onSetThemeMode: (ThemeMode) -> Unit,
    onNavigateBackToControl: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onNavigateBackToControl)

    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        onRefreshBleState()
        onStartScan()
    }

    val enableBtLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        onRefreshBleState()
    }

    LaunchedEffect(Unit) {
        onRefreshBleState()
    }

    var carNameInput by remember(uiState.settings.carName) {
        mutableStateOf(uiState.settings.carName)
    }
    var pinInput by remember(uiState.settings.pinCode) {
        mutableStateOf(uiState.settings.pinCode)
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 760.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // BLE Module Section
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.Bluetooth,
                                contentDescription = null,
                                tint = ElectricCyan,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Bluetooth-модуль IBT Alarm",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        if (uiState.bleState.status == ConnectionStatus.CONNECTED_BLE) {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = SafeEmerald.copy(alpha = 0.14f),
                                border = BorderStroke(1.dp, SafeEmerald.copy(alpha = 0.5f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.BluetoothConnected,
                                        contentDescription = null,
                                        tint = SafeEmerald
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = uiState.bleState.connectedDeviceName ?: "IBT Module",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "${uiState.bleState.connectedDeviceMac.orEmpty()} • ${uiState.bleState.liveRssi} dBm",
                                            fontFamily = MonoFontFamily,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = SafeEmerald
                                        )
                                    }
                                    OutlinedButton(onClick = onDisconnectBle) {
                                        Text("Откл.")
                                    }
                                }
                            }
                        }

                        if (!uiState.bleState.hasBlePermission) {
                            Text(
                                text = "Для поиска и управления блоком автозапуска по Bluetooth LE выдайте разрешение на устройства рядом.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Button(
                                onClick = { permissionLauncher.launch(blePermissions) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("btn_grant_ble_permission")
                            ) {
                                Text("Выдать разрешение Bluetooth")
                            }
                        } else if (!uiState.bleState.isBluetoothEnabled) {
                            Text(
                                text = "Bluetooth на смартфоне выключен.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = IgnitionAmber
                            )
                            Button(
                                onClick = {
                                    enableBtLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Включить Bluetooth")
                            }
                        } else {
                            Button(
                                onClick = {
                                    if (uiState.bleState.isScanning) onStopScan() else onStartScan()
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("btn_ble_scan")
                            ) {
                                if (uiState.bleState.isScanning) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Поиск модулей… Нажмите для остановки")
                                } else {
                                    Icon(
                                        imageVector = Icons.Rounded.BluetoothSearching,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Найти модуль по BLE")
                                }
                            }
                        }

                        uiState.bleState.lastError?.let { err ->
                            Text(
                                text = err,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                        }

                        // Simulation Mode Toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Science,
                                contentDescription = null,
                                tint = IgnitionAmber,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Режим симуляции (тест без авто)",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Позволяет проверить все кнопки, таймер прогрева и PKE без реального блока в машине",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = uiState.settings.simulationModeEnabled,
                                onCheckedChange = onSetSimulationMode,
                                modifier = Modifier.testTag("switch_simulation_mode")
                            )
                        }
                    }
                }
            }

            if (uiState.discoveredDevices.isNotEmpty()) {
                item {
                    Text(
                        text = "Найденные устройства BLE (${uiState.discoveredDevices.size})",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
                items(uiState.discoveredDevices, key = { it.macAddress }) { dev ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(
                            1.dp,
                            if (dev.isLikelyIbtModule) ElectricCyan.copy(alpha = 0.55f)
                            else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = dev.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${dev.macAddress} • ${dev.rssi} dBm",
                                    fontFamily = MonoFontFamily,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Button(onClick = { onConnectDevice(dev) }) {
                                Text("Подкл.")
                            }
                        }
                    }
                }
            }

            // Vehicle & Security Settings
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.Security,
                                contentDescription = null,
                                tint = ElectricCyan,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Автомобиль и безопасность",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = carNameInput,
                                onValueChange = { carNameInput = it },
                                label = { Text("Название автомобиля") },
                                leadingIcon = {
                                    Icon(Icons.Rounded.DirectionsCar, contentDescription = null)
                                },
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("input_car_name")
                            )
                            Button(
                                onClick = { onUpdateCarName(carNameInput) },
                                colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan),
                                modifier = Modifier.testTag("btn_save_car_name")
                            ) {
                                Text("ОК", color = MaterialTheme.colorScheme.onPrimary)
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = pinInput,
                                onValueChange = { pinInput = it.filter { c -> c.isDigit() }.take(6) },
                                label = { Text("ПИН-код блока (4–6 цифр)") },
                                leadingIcon = {
                                    Icon(Icons.Rounded.Pin, contentDescription = null)
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("input_pin_code")
                            )
                            Button(
                                onClick = { onUpdatePin(pinInput) },
                                modifier = Modifier.testTag("btn_save_pin")
                            ) {
                                Text("ОК")
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.TouchApp,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Защита от случайного запуска",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = "Требовать удержание кнопки 1 сек для старта/стопа двигателя",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = uiState.settings.holdToConfirmEnabled,
                                onCheckedChange = onSetHoldToConfirm,
                                modifier = Modifier.testTag("switch_hold_confirm")
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Vibration,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Виброотклик при командах",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = "Тактильное подтверждение отправки команды",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = uiState.settings.hapticEnabled,
                                onCheckedChange = onSetHaptic,
                                modifier = Modifier.testTag("switch_haptic")
                            )
                        }
                    }
                }
            }

            // Theme Picker
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.Palette,
                                contentDescription = null,
                                tint = IgnitionAmber
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Оформление интерфейса",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ThemeMode.entries.forEach { mode ->
                                FilterChip(
                                    selected = uiState.settings.themeMode == mode,
                                    onClick = { onSetThemeMode(mode) },
                                    label = { Text(mode.label) },
                                    modifier = Modifier.testTag("theme_chip_${mode.name}")
                                )
                            }
                        }
                    }
                }
            }

            // Background operation tips (IBT Alarm guide)
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.BatterySaver,
                                contentDescription = null,
                                tint = SafeEmerald
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Стабильная работа PKE в фоне",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        Text(
                            text = "Чтобы бесключевой доступ срабатывал при заблокированном экране на Xiaomi, Samsung и Huawei, выберите для приложения режим батареи «Без ограничений» и разрешите автозапуск.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedButton(
                            onClick = {
                                val intent = Intent(
                                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                    Uri.fromParts("package", context.packageName, null)
                                )
                                context.startActivity(intent)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("btn_open_app_settings")
                        ) {
                            Text("Открыть настройки приложения")
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(12.dp)) }
        }
    }
}

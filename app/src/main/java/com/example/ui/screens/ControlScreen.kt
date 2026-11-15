package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.BluetoothSearching
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.CarCommand
import com.example.data.model.ConnectionStatus
import com.example.data.model.EngineState
import com.example.ui.VehicleUiState
import com.example.ui.components.CommandGrid
import com.example.ui.components.EngineStartButton
import com.example.ui.components.VehicleHeroCard
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.IgnitionAmber
import com.example.ui.theme.SafeEmerald

@Composable
fun ControlScreen(
    uiState: VehicleUiState,
    onCommand: (CarCommand) -> Unit,
    onSelectWarmupMinutes: (Int) -> Unit,
    onTogglePke: (Boolean) -> Unit,
    onToggleShake: (Boolean) -> Unit,
    onOpenDeviceSettings: () -> Unit,
    onRequestBlePermissions: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    BoxWithConstraints(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        val isWide = maxWidth >= 720.dp

        if (isWide) {
            Row(
                modifier = Modifier
                    .widthIn(max = 1040.dp)
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    BluetoothPermissionBanner(
                        visible = !uiState.bleState.hasBlePermission,
                        onRequestPermission = onRequestBlePermissions
                    )
                    SimulationBanner(
                        visible = uiState.bleState.status == ConnectionStatus.SIMULATION,
                        onOpenSettings = onOpenDeviceSettings
                    )
                    VehicleHeroCard(
                        carName = uiState.settings.carName,
                        status = uiState.status,
                        bleState = uiState.bleState,
                        onConnectionChipClick = onOpenDeviceSettings
                    )
                    QuickSmartTogglesCard(
                        pkeEnabled = uiState.settings.pkeEnabled,
                        shakeEnabled = uiState.settings.shakeControlEnabled,
                        shakeSupported = uiState.shakeSupported,
                        onTogglePke = onTogglePke,
                        onToggleShake = onToggleShake
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    EngineControlPanel(
                        uiState = uiState,
                        onToggleEngine = {
                            val cmd = if (uiState.status.engineState == EngineState.STOPPED) {
                                CarCommand.ENGINE_START
                            } else {
                                CarCommand.ENGINE_STOP
                            }
                            onCommand(cmd)
                        },
                        onSelectWarmupMinutes = onSelectWarmupMinutes
                    )
                    CommandGrid(
                        status = uiState.status,
                        onCommand = onCommand
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .widthIn(max = 560.dp)
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                BluetoothPermissionBanner(
                    visible = !uiState.bleState.hasBlePermission,
                    onRequestPermission = onRequestBlePermissions
                )

                SimulationBanner(
                    visible = uiState.bleState.status == ConnectionStatus.SIMULATION,
                    onOpenSettings = onOpenDeviceSettings
                )

                VehicleHeroCard(
                    carName = uiState.settings.carName,
                    status = uiState.status,
                    bleState = uiState.bleState,
                    onConnectionChipClick = onOpenDeviceSettings
                )

                EngineControlPanel(
                    uiState = uiState,
                    onToggleEngine = {
                        val cmd = if (uiState.status.engineState == EngineState.STOPPED) {
                            CarCommand.ENGINE_START
                        } else {
                            CarCommand.ENGINE_STOP
                        }
                        onCommand(cmd)
                    },
                    onSelectWarmupMinutes = onSelectWarmupMinutes
                )

                CommandGrid(
                    status = uiState.status,
                    onCommand = onCommand
                )

                QuickSmartTogglesCard(
                    pkeEnabled = uiState.settings.pkeEnabled,
                    shakeEnabled = uiState.settings.shakeControlEnabled,
                    shakeSupported = uiState.shakeSupported,
                    onTogglePke = onTogglePke,
                    onToggleShake = onToggleShake
                )

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun BluetoothPermissionBanner(
    visible: Boolean,
    onRequestPermission: () -> Unit
) {
    AnimatedVisibility(visible = visible) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = ElectricCyan.copy(alpha = 0.12f),
            border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.45f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.Bluetooth,
                    contentDescription = null,
                    tint = ElectricCyan,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Нужен доступ к Bluetooth",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Разрешите поиск и подключение к блоку автозапуска.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = onRequestPermission,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("banner_grant_ble_btn")
                ) {
                    Text("Разрешить")
                }
            }
        }
    }
}

@Composable
private fun SimulationBanner(
    visible: Boolean,
    onOpenSettings: () -> Unit
) {
    AnimatedVisibility(visible = visible) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = IgnitionAmber.copy(alpha = 0.12f),
            border = BorderStroke(1.dp, IgnitionAmber.copy(alpha = 0.4f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
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
                        text = "Режим симуляции модуля",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Для работы с реальным авто подключите блок IBT по BLE.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                TextButton(
                    onClick = onOpenSettings,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("banner_connect_ble_btn")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.BluetoothSearching,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("BLE")
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EngineControlPanel(
    uiState: VehicleUiState,
    onToggleEngine: () -> Unit,
    onSelectWarmupMinutes: (Int) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            EngineStartButton(
                engineState = uiState.status.engineState,
                remainingSeconds = uiState.status.engineRemainingSeconds,
                totalSeconds = uiState.status.engineTotalSeconds,
                holdToConfirm = uiState.settings.holdToConfirmEnabled,
                onToggleEngine = onToggleEngine
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Timer,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "Время прогрева двигателя",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            val presets = listOf(10, 15, 20, 30)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                presets.forEach { mins ->
                    val selected = uiState.settings.defaultWarmupMinutes == mins
                    FilterChip(
                        selected = selected,
                        onClick = { onSelectWarmupMinutes(mins) },
                        enabled = uiState.status.engineState == EngineState.STOPPED,
                        label = {
                            Text(
                                text = "$mins мин",
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ElectricCyan.copy(alpha = 0.18f),
                            selectedLabelColor = ElectricCyan
                        ),
                        modifier = Modifier.testTag("warmup_chip_$mins")
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickSmartTogglesCard(
    pkeEnabled: Boolean,
    shakeEnabled: Boolean,
    shakeSupported: Boolean,
    onTogglePke: (Boolean) -> Unit,
    onToggleShake: (Boolean) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Умный доступ IBT",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { onTogglePke(!pkeEnabled) }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.Key,
                    contentDescription = null,
                    tint = if (pkeEnabled) SafeEmerald else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "PKE — свободные руки",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Автооткрытие при приближении и охрана при уходе",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = pkeEnabled,
                    onCheckedChange = onTogglePke,
                    modifier = Modifier.testTag("switch_pke_quick")
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable(enabled = shakeSupported) { onToggleShake(!shakeEnabled) }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.Vibration,
                    contentDescription = null,
                    tint = if (shakeEnabled) ElectricCyan else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Управление встряхиванием",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (shakeSupported) {
                            "Двойное встряхивание телефона открывает или закрывает авто"
                        } else {
                            "Акселерометр не найден на этом устройстве"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = shakeEnabled,
                    onCheckedChange = onToggleShake,
                    enabled = shakeSupported,
                    modifier = Modifier.testTag("switch_shake_quick")
                )
            }
        }
    }
}

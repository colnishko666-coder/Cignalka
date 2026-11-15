package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Radar
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.AutostartScheduleEntity
import com.example.data.local.EventLogEntity
import com.example.data.model.ConnectionStatus
import com.example.ui.VehicleUiState
import com.example.ui.theme.AlertCrimson
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.IgnitionAmber
import com.example.ui.theme.MonoFontFamily
import com.example.ui.theme.SafeEmerald
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun AutomationScreen(
    uiState: VehicleUiState,
    onAddSchedule: (String, Int, Int, Int, Int) -> Unit,
    onToggleSchedule: (Long, Boolean) -> Unit,
    onDeleteSchedule: (Long) -> Unit,
    onTogglePke: (Boolean) -> Unit,
    onUpdatePkeThresholds: (Int, Int) -> Unit,
    onSimulateRssi: (Int) -> Unit,
    onToggleShake: (Boolean) -> Unit,
    onClearLogs: () -> Unit,
    onNavigateBackToControl: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onNavigateBackToControl)

    var showAddScheduleDialog by remember { mutableStateOf(false) }

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
            // Section 1: Scheduled Autostart
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Schedule,
                            contentDescription = null,
                            tint = IgnitionAmber,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Расписание автозапуска",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    FilledTonalButton(
                        onClick = { showAddScheduleDialog = true },
                        modifier = Modifier.testTag("btn_add_schedule")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Добавить")
                    }
                }
            }

            if (uiState.schedules.isEmpty()) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Alarm,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(34.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Нет активных таймеров прогрева",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Настройте утренний автозапуск двигателя по дням недели или на один раз.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(uiState.schedules, key = { it.id }) { schedule ->
                    ScheduleItemCard(
                        schedule = schedule,
                        onToggle = { onToggleSchedule(schedule.id, it) },
                        onDelete = { onDeleteSchedule(schedule.id) }
                    )
                }
            }

            // Section 2: PKE Proximity Radar
            item {
                PkeRadarCard(
                    pkeEnabled = uiState.settings.pkeEnabled,
                    currentRssi = uiState.status.currentRssi,
                    unlockThreshold = uiState.settings.pkeUnlockRssi,
                    lockThreshold = uiState.settings.pkeLockRssi,
                    isSimulation = uiState.bleState.status != ConnectionStatus.CONNECTED_BLE &&
                        uiState.settings.simulationModeEnabled,
                    onTogglePke = onTogglePke,
                    onUpdateThresholds = onUpdatePkeThresholds,
                    onSimulateRssi = onSimulateRssi
                )
            }

            // Section 3: Accelerometer Shake Sensor
            item {
                ShakeSensorCard(
                    enabled = uiState.settings.shakeControlEnabled,
                    supported = uiState.shakeSupported,
                    liveGForce = uiState.liveGForce,
                    onToggle = onToggleShake
                )
            }

            // Section 4: Event Log Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.History,
                            contentDescription = null,
                            tint = ElectricCyan,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Журнал событий",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    if (uiState.eventLogs.isNotEmpty()) {
                        TextButton(
                            onClick = onClearLogs,
                            modifier = Modifier.testTag("btn_clear_logs")
                        ) {
                            Text("Очистить")
                        }
                    }
                }
            }

            if (uiState.eventLogs.isEmpty()) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Text(
                            text = "История команд пуста. Нажмите любую кнопку на пульте, чтобы увидеть запись.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(18.dp)
                        )
                    }
                }
            } else {
                items(uiState.eventLogs, key = { it.id }) { log ->
                    EventLogRow(log = log)
                }
            }

            item { Spacer(modifier = Modifier.height(12.dp)) }
        }
    }

    if (showAddScheduleDialog) {
        AddScheduleDialog(
            defaultWarmup = uiState.settings.defaultWarmupMinutes,
            onDismiss = { showAddScheduleDialog = false },
            onSave = { label, h, m, warmup, mask ->
                onAddSchedule(label, h, m, warmup, mask)
                showAddScheduleDialog = false
            }
        )
    }
}

@Composable
private fun ScheduleItemCard(
    schedule: AutostartScheduleEntity,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            1.dp,
            if (schedule.isEnabled) IgnitionAmber.copy(alpha = 0.45f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = String.format(Locale.US, "%02d:%02d", schedule.hour, schedule.minute),
                    fontFamily = MonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.headlineMedium,
                    color = if (schedule.isEnabled) IgnitionAmber else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = schedule.label,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${formatDaysMask(schedule.daysMask)} • Прогрев ${schedule.warmupMinutes} мин",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = schedule.isEnabled,
                onCheckedChange = onToggle,
                modifier = Modifier.testTag("switch_schedule_${schedule.id}")
            )
            IconButton(
                onClick = onDelete,
                modifier = Modifier.testTag("delete_schedule_${schedule.id}")
            ) {
                Icon(
                    imageVector = Icons.Rounded.DeleteOutline,
                    contentDescription = "Удалить таймер",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun PkeRadarCard(
    pkeEnabled: Boolean,
    currentRssi: Int,
    unlockThreshold: Int,
    lockThreshold: Int,
    isSimulation: Boolean,
    onTogglePke: (Boolean) -> Unit,
    onUpdateThresholds: (Int, Int) -> Unit,
    onSimulateRssi: (Int) -> Unit
) {
    val zoneLabel = when {
        !pkeEnabled -> "PKE отключён"
        currentRssi >= unlockThreshold -> "В зоне открытия (рядом с авто)"
        currentRssi <= lockThreshold -> "Вне зоны (авто на охране)"
        else -> "Переходная зона"
    }
    val zoneColor = when {
        !pkeEnabled -> MaterialTheme.colorScheme.onSurfaceVariant
        currentRssi >= unlockThreshold -> SafeEmerald
        currentRssi <= lockThreshold -> ElectricCyan
        else -> IgnitionAmber
    }

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
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.Radar,
                    contentDescription = null,
                    tint = ElectricCyan,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Радар PKE (Свободные руки)",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = zoneLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = zoneColor
                    )
                }
                Switch(
                    checked = pkeEnabled,
                    onCheckedChange = onTogglePke,
                    modifier = Modifier.testTag("switch_pke_radar")
                )
            }

            // Visual radar strength ring + RSSI
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                val normalized = ((currentRssi + 100f) / 60f).coerceIn(0.1f, 1f)
                Box(
                    modifier = Modifier.size(64.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawCircle(
                            color = zoneColor.copy(alpha = 0.2f),
                            style = Stroke(width = 4.dp.toPx())
                        )
                        drawCircle(
                            color = zoneColor.copy(alpha = 0.35f),
                            radius = (size.minDimension / 2f) * normalized
                        )
                    }
                    Text(
                        text = "$currentRssi",
                        fontFamily = MonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Порог открытия: $unlockThreshold dBm",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Slider(
                        value = unlockThreshold.toFloat(),
                        onValueChange = {
                            val v = it.roundToInt().coerceAtLeast(lockThreshold + 6)
                            onUpdateThresholds(v, lockThreshold)
                        },
                        valueRange = -76f..-45f,
                        enabled = pkeEnabled,
                        colors = SliderDefaults.colors(
                            thumbColor = SafeEmerald,
                            activeTrackColor = SafeEmerald
                        )
                    )
                    Text(
                        text = "Порог закрытия (уход): $lockThreshold dBm",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Slider(
                        value = lockThreshold.toFloat(),
                        onValueChange = {
                            val v = it.roundToInt().coerceAtMost(unlockThreshold - 6)
                            onUpdateThresholds(unlockThreshold, v)
                        },
                        valueRange = -98f..-70f,
                        enabled = pkeEnabled,
                        colors = SliderDefaults.colors(
                            thumbColor = ElectricCyan,
                            activeTrackColor = ElectricCyan
                        )
                    )
                }
            }

            if (isSimulation) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = "Тест дальности PKE (симуляция): $currentRssi dBm",
                        style = MaterialTheme.typography.labelMedium,
                        color = IgnitionAmber
                    )
                    Slider(
                        value = currentRssi.toFloat(),
                        onValueChange = { onSimulateRssi(it.roundToInt()) },
                        valueRange = -98f..-45f,
                        enabled = pkeEnabled,
                        modifier = Modifier.testTag("slider_pke_sim")
                    )
                }
            }
        }
    }
}

@Composable
private fun ShakeSensorCard(
    enabled: Boolean,
    supported: Boolean,
    liveGForce: Float,
    onToggle: (Boolean) -> Unit
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.Vibration,
                    contentDescription = null,
                    tint = ElectricCyan,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Датчик встряхивания",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (supported) {
                            "Акселерометр активен: ${String.format(Locale.US, "%.2f G", liveGForce)}"
                        } else {
                            "Датчик не обнаружен"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = enabled,
                    onCheckedChange = onToggle,
                    enabled = supported,
                    modifier = Modifier.testTag("switch_shake_card")
                )
            }
            if (supported) {
                LinearProgressIndicator(
                    progress = { ((liveGForce - 0.8f) / 2.0f).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(CircleShape),
                    color = if (liveGForce > 2.0f) IgnitionAmber else ElectricCyan
                )
            }
        }
    }
}

@Composable
private fun EventLogRow(log: EventLogEntity) {
    val formatter = remember { SimpleDateFormat("HH:mm:ss • dd.MM", Locale.getDefault()) }
    val dotColor = if (log.isAlert) AlertCrimson else ElectricCyan

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = log.title,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = log.detail,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = formatter.format(Date(log.timestampMillis)),
                    fontFamily = MonoFontFamily,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = log.transport,
                    style = MaterialTheme.typography.labelSmall,
                    color = dotColor
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun AddScheduleDialog(
    defaultWarmup: Int,
    onDismiss: () -> Unit,
    onSave: (String, Int, Int, Int, Int) -> Unit
) {
    val timeState = rememberTimePickerState(initialHour = 7, initialMinute = 45, is24Hour = true)
    var label by remember { mutableStateOf("Утренний прогрев") }
    var warmup by remember { mutableIntStateOf(defaultWarmup) }
    var daysMask by remember { mutableIntStateOf(0b0011111) } // Mon-Fri

    val dayLabels = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Новый таймер автозапуска") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                TimePicker(state = timeState)

                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Название") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "Дни недели (пусто = один раз)",
                    style = MaterialTheme.typography.labelMedium
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    dayLabels.forEachIndexed { idx, day ->
                        val bit = 1 shl idx
                        val selected = (daysMask and bit) != 0
                        FilterChip(
                            selected = selected,
                            onClick = { daysMask = daysMask xor bit },
                            label = { Text(day) }
                        )
                    }
                }

                Text(
                    text = "Прогрев: $warmup мин",
                    style = MaterialTheme.typography.labelMedium
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(10, 15, 20, 30).forEach { m ->
                        FilterChip(
                            selected = warmup == m,
                            onClick = { warmup = m },
                            label = { Text("$m мин") }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(label, timeState.hour, timeState.minute, warmup, daysMask)
                },
                colors = ButtonDefaults.buttonColors(containerColor = IgnitionAmber),
                modifier = Modifier.testTag("btn_save_schedule")
            ) {
                Text("Сохранить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}

private fun formatDaysMask(mask: Int): String {
    if (mask == 0) return "Однократно"
    if (mask == 0b0011111) return "Пн–Пт"
    if (mask == 0b1111111) return "Ежедневно"
    val names = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")
    return names.indices
        .filter { (mask and (1 shl it)) != 0 }
        .joinToString(", ") { names[it] }
}

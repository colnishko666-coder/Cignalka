package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.BluetoothConnected
import androidx.compose.material.icons.rounded.BluetoothDisabled
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.SignalCellularAlt
import androidx.compose.material.icons.rounded.Thermostat
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.BleConnectionState
import com.example.data.model.ConnectionStatus
import com.example.data.model.EngineState
import com.example.data.model.SecurityState
import com.example.data.model.VehicleStatus
import com.example.ui.theme.AlertCrimson
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.IgnitionAmber
import com.example.ui.theme.MonoFontFamily
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.SafeEmerald
import kotlinx.coroutines.delay
import java.util.Locale
import kotlin.math.PI
import kotlin.math.sin

@Composable
fun VehicleHeroCard(
    carName: String,
    status: VehicleStatus,
    bleState: BleConnectionState,
    onConnectionChipClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val targetAuraColor = when {
        status.securityState == SecurityState.PANIC -> AlertCrimson
        status.engineState != EngineState.STOPPED -> IgnitionAmber
        status.securityState == SecurityState.ARMED -> ElectricCyan
        else -> SafeEmerald
    }
    val auraColor by animateColorAsState(
        targetValue = targetAuraColor,
        animationSpec = tween(450),
        label = "auraColor"
    )

    // Brief hazard flash & suspension bounce when lock/unlock/trunk changes
    var commandFlashActive by remember { mutableStateOf(false) }
    var bounceTrigger by remember { mutableStateOf(false) }
    LaunchedEffect(status.securityState, status.isTrunkOpen) {
        commandFlashActive = true
        bounceTrigger = true
        delay(220L)
        bounceTrigger = false
        delay(450L)
        commandFlashActive = false
    }

    val carBounceScale by animateFloatAsState(
        targetValue = if (bounceTrigger) 1.025f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "carBounce"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "car2dAnim")

    val radarWave by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (status.securityState == SecurityState.PANIC) 700 else 2600,
                easing = LinearEasing
            )
        ),
        label = "radarWave"
    )

    val engineCycle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (status.engineState == EngineState.CRANKING) 220 else 420,
                easing = LinearEasing
            )
        ),
        label = "engineCycle"
    )

    val exhaustPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600, easing = LinearEasing)
        ),
        label = "exhaustPhase"
    )

    val panicFlash by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 320, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "panicFlash"
    )

    val vibrationX = when (status.engineState) {
        EngineState.CRANKING -> sin(engineCycle * 3f) * 4.5f
        EngineState.RUNNING -> sin(engineCycle * 2f) * 0.9f
        EngineState.STOPPED -> 0f
    }
    val vibrationY = when (status.engineState) {
        EngineState.CRANKING -> sin(engineCycle * 4f) * 3.0f
        EngineState.RUNNING -> sin(engineCycle * 3f) * 1.4f
        EngineState.STOPPED -> 0f
    }
    val vibrationTilt = when (status.engineState) {
        EngineState.CRANKING -> sin(engineCycle * 2f) * 0.8f
        EngineState.RUNNING -> sin(engineCycle) * 0.18f
        EngineState.STOPPED -> 0f
    }

    val flashIntensity = when {
        status.securityState == SecurityState.PANIC -> panicFlash
        commandFlashActive -> 0.85f
        else -> 0f
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = ObsidianBg,
        border = BorderStroke(1.dp, auraColor.copy(alpha = 0.38f)),
        tonalElevation = 6.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = carName,
                        style = MaterialTheme.typography.titleLarge,
                        color = Color(0xFFF1F5F9),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    val engineText = when (status.engineState) {
                        EngineState.STOPPED -> "Двигатель заглушен"
                        EngineState.CRANKING -> "Прокрутка стартера…"
                        EngineState.RUNNING -> "Прогрев • ${formatMmSs(status.engineRemainingSeconds)}"
                    }
                    Text(
                        text = engineText,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (status.engineState == EngineState.STOPPED) {
                            Color(0xFF94A3B8)
                        } else {
                            IgnitionAmber
                        }
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                ConnectionBadge(
                    bleState = bleState,
                    onClick = onConnectionChipClick
                )
            }

            // 2D Cutout Car Stage with live animations
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(222.dp),
                contentAlignment = Alignment.Center
            ) {
                // Stage background glow & expanding 2D sonar waves
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val center = Offset(size.width * 0.5f, size.height * 0.62f)
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                auraColor.copy(alpha = 0.28f),
                                auraColor.copy(alpha = 0.06f),
                                Color.Transparent
                            ),
                            center = center,
                            radius = size.width * 0.48f
                        ),
                        radius = size.width * 0.48f,
                        center = center
                    )

                    // Two staggered elliptical PKE/Security sonar rings on the floor
                    for (i in 0..1) {
                        val p = (radarWave + i * 0.5f) % 1f
                        val ringW = size.width * (0.42f + p * 0.52f)
                        val ringH = size.height * (0.18f + p * 0.24f)
                        drawOval(
                            color = auraColor.copy(alpha = (1f - p) * 0.42f),
                            topLeft = Offset(center.x - ringW / 2f, center.y - ringH / 2f + 16.dp.toPx()),
                            size = Size(ringW, ringH),
                            style = Stroke(width = 2.dp.toPx())
                        )
                    }
                }

                // Exact-aspect container for the 2D cutout + aligned overlays (headlights, exhaust, trunk)
                Box(
                    modifier = Modifier
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                        .padding(bottom = 26.dp)
                        .aspectRatio(1200f / 633f)
                        .graphicsLayer {
                            translationX = vibrationX
                            translationY = vibrationY
                            rotationZ = vibrationTilt
                            scaleX = carBounceScale
                            scaleY = carBounceScale
                        },
                    contentAlignment = Alignment.Center
                ) {
                    // Exhaust smoke puffs behind rear bumper when engine is running/cranking
                    if (status.engineState != EngineState.STOPPED) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val tailX = size.width * 0.89f
                            val tailY = size.height * 0.66f
                            for (i in 0..3) {
                                val p = (exhaustPhase + i * 0.25f) % 1f
                                val puffX = tailX + p * size.width * 0.14f
                                val puffY = tailY - p * size.height * 0.22f
                                val puffR = (5.dp.toPx()) + p * 14.dp.toPx()
                                drawCircle(
                                    color = Color(0xFFCBD5E1).copy(alpha = (1f - p) * 0.42f),
                                    radius = puffR,
                                    center = Offset(puffX, puffY)
                                )
                            }
                        }
                    }

                    // 2D illustrated car cutout
                    Image(
                        painter = painterResource(id = R.drawable.img_car_2d),
                        contentDescription = "2D модель автомобиля $carName",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )

                    // Headlight DRL / Turn-signal flash / Trunk glow aligned to 2D cutout
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val leftHeadlight = Offset(size.width * 0.048f, size.height * 0.455f)
                        val rightHeadlight = Offset(size.width * 0.32f, size.height * 0.485f)
                        val fenderSignal = Offset(size.width * 0.435f, size.height * 0.435f)
                        val trunkEdge = Offset(size.width * 0.88f, size.height * 0.42f)

                        // Warm DRL glow when engine is running or car is unlocked
                        val drlAlpha = when {
                            status.engineState == EngineState.RUNNING -> 0.55f
                            status.securityState == SecurityState.DISARMED -> 0.25f
                            else -> 0f
                        }
                        if (drlAlpha > 0f) {
                            listOf(leftHeadlight, rightHeadlight).forEach { pt ->
                                drawCircle(
                                    brush = Brush.radialGradient(
                                        colors = listOf(
                                            Color(0xFFE0F7FA).copy(alpha = drlAlpha),
                                            ElectricCyan.copy(alpha = drlAlpha * 0.35f),
                                            Color.Transparent
                                        ),
                                        center = pt,
                                        radius = 28.dp.toPx()
                                    ),
                                    radius = 28.dp.toPx(),
                                    center = pt
                                )
                            }
                        }

                        // Amber hazard / lock / panic flash
                        if (flashIntensity > 0f) {
                            listOf(leftHeadlight, rightHeadlight, fenderSignal).forEachIndexed { idx, pt ->
                                val r = if (idx == 2) 18.dp.toPx() else 34.dp.toPx()
                                drawCircle(
                                    brush = Brush.radialGradient(
                                        colors = listOf(
                                            IgnitionAmber.copy(alpha = flashIntensity * 0.85f),
                                            IgnitionAmber.copy(alpha = flashIntensity * 0.25f),
                                            Color.Transparent
                                        ),
                                        center = pt,
                                        radius = r
                                    ),
                                    radius = r,
                                    center = pt
                                )
                            }
                        }

                        // Trunk open indicator glow at rear
                        if (status.isTrunkOpen) {
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        IgnitionAmber.copy(alpha = 0.75f),
                                        Color.Transparent
                                    ),
                                    center = trunkEdge,
                                    radius = 26.dp.toPx()
                                ),
                                radius = 26.dp.toPx(),
                                center = trunkEdge
                            )
                        }
                    }
                }

                // Bottom status overlay pills
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, ObsidianBg.copy(alpha = 0.94f))
                            )
                        )
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val (secIcon, secLabel, secColor) = when (status.securityState) {
                        SecurityState.ARMED -> Triple(Icons.Rounded.Lock, "На охране", ElectricCyan)
                        SecurityState.DISARMED -> Triple(Icons.Rounded.LockOpen, "Открыта", SafeEmerald)
                        SecurityState.PANIC -> Triple(Icons.Rounded.NotificationsActive, "Сигнал", AlertCrimson)
                    }
                    StatusMiniChip(
                        icon = secIcon,
                        label = secLabel,
                        accentColor = secColor,
                        modifier = Modifier.weight(1f)
                    )
                    StatusMiniChip(
                        icon = Icons.Rounded.DirectionsCar,
                        label = if (status.isTrunkOpen) "Багажник откр." else "Багажник закр.",
                        accentColor = if (status.isTrunkOpen) IgnitionAmber else Color(0xFF94A3B8),
                        modifier = Modifier.weight(1.15f)
                    )
                }
            }

            // Telemetry bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF101622))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TelemetryMetric(
                    icon = Icons.Rounded.BatteryChargingFull,
                    value = String.format(Locale.US, "%.1f В", status.batteryVoltage),
                    label = "АКБ",
                    accentColor = if (status.engineState == EngineState.RUNNING) SafeEmerald else ElectricCyan
                )
                TelemetryMetric(
                    icon = Icons.Rounded.Thermostat,
                    value = "${if (status.cabinTempCelsius > 0) "+" else ""}${status.cabinTempCelsius}°C",
                    label = "Салон",
                    accentColor = if (status.cabinTempCelsius >= 20) IgnitionAmber else ElectricCyan
                )
                TelemetryMetric(
                    icon = Icons.Rounded.SignalCellularAlt,
                    value = "${status.currentRssi} dBm",
                    label = if (status.isSimulatedTelemetry) "Симуляция" else "BLE RSSI",
                    accentColor = if (status.currentRssi >= -70) SafeEmerald else IgnitionAmber
                )
            }
        }
    }
}

@Composable
private fun ConnectionBadge(
    bleState: BleConnectionState,
    onClick: () -> Unit
) {
    val (bg, fg, icon, text) = when (bleState.status) {
        ConnectionStatus.CONNECTED_BLE -> Quadruple(
            SafeEmerald.copy(alpha = 0.16f),
            SafeEmerald,
            Icons.Rounded.BluetoothConnected,
            bleState.connectedDeviceName ?: "BLE"
        )
        ConnectionStatus.CONNECTING -> Quadruple(
            ElectricCyan.copy(alpha = 0.16f),
            ElectricCyan,
            Icons.Rounded.BluetoothConnected,
            "Подкл…"
        )
        ConnectionStatus.SIMULATION -> Quadruple(
            IgnitionAmber.copy(alpha = 0.16f),
            IgnitionAmber,
            Icons.Rounded.Science,
            "Симуляция"
        )
        ConnectionStatus.DISCONNECTED -> Quadruple(
            AlertCrimson.copy(alpha = 0.16f),
            AlertCrimson,
            Icons.Rounded.BluetoothDisabled,
            "Нет связи"
        )
    }

    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(bg)
            .border(1.dp, fg.copy(alpha = 0.45f), CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp)
            .testTag("connection_status_chip"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = text,
            tint = fg,
            modifier = Modifier.size(15.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = fg,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun StatusMiniChip(
    icon: ImageVector,
    label: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF141C2B).copy(alpha = 0.88f))
            .border(1.dp, accentColor.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = accentColor,
            modifier = Modifier.size(15.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = Color(0xFFE2E8F0),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun TelemetryMetric(
    icon: ImageVector,
    value: String,
    label: String,
    accentColor: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = accentColor,
            modifier = Modifier.size(17.dp)
        )
        Column {
            Text(
                text = value,
                fontFamily = MonoFontFamily,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.labelLarge,
                color = Color(0xFFF1F5F9)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF94A3B8)
            )
        }
    }
}

private data class Quadruple<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)

fun formatMmSs(totalSeconds: Int): String {
    val safe = totalSeconds.coerceAtLeast(0)
    val m = safe / 60
    val s = safe % 60
    return String.format(Locale.US, "%02d:%02d", m, s)
}

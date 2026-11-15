package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.EngineState
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.IgnitionAmber
import com.example.ui.theme.IgnitionAmberGlow
import com.example.ui.theme.MonoFontFamily
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun EngineStartButton(
    engineState: EngineState,
    remainingSeconds: Int,
    totalSeconds: Int,
    holdToConfirm: Boolean,
    onToggleEngine: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    var isPressed by remember { mutableStateOf(false) }
    var holdProgress by remember { mutableFloatStateOf(0f) }
    var holdJob by remember { mutableStateOf<Job?>(null) }

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 400f),
        label = "buttonScale"
    )

    val isRunning = engineState == EngineState.RUNNING
    val isCranking = engineState == EngineState.CRANKING

    val ringColor by animateColorAsState(
        targetValue = when (engineState) {
            EngineState.STOPPED -> ElectricCyan
            EngineState.CRANKING -> IgnitionAmberGlow
            EngineState.RUNNING -> IgnitionAmber
        },
        animationSpec = tween(350),
        label = "ringColor"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "engineIdle")
    val crankRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing)
        ),
        label = "crankRotation"
    )
    val runningGlow by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "runningGlow"
    )

    val timerFraction = if (isRunning && totalSeconds > 0) {
        (remainingSeconds.toFloat() / totalSeconds.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }

    val cdText = when (engineState) {
        EngineState.STOPPED -> if (holdToConfirm) "Удерживайте для запуска двигателя" else "Нажмите для запуска двигателя"
        EngineState.CRANKING -> "Запуск двигателя"
        EngineState.RUNNING -> "Остановить двигатель, осталось ${formatMmSs(remainingSeconds)}"
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(178.dp)
                .scale(scale)
                .clip(CircleShape)
                .semantics { contentDescription = cdText }
                .testTag("engine_start_button")
                .pointerInput(holdToConfirm, engineState) {
                    detectTapGestures(
                        onPress = {
                            if (isCranking) return@detectTapGestures
                            isPressed = true
                            if (!holdToConfirm) {
                                tryAwaitRelease()
                                isPressed = false
                                onToggleEngine()
                            } else {
                                var completed = false
                                holdJob?.cancel()
                                holdJob = scope.launch {
                                    val steps = 36
                                    val stepMs = 28L // ~1.0s smooth hold
                                    for (i in 1..steps) {
                                        delay(stepMs)
                                        holdProgress = i / steps.toFloat()
                                    }
                                    completed = true
                                    holdProgress = 0f
                                    isPressed = false
                                    onToggleEngine()
                                }
                                tryAwaitRelease()
                                if (!completed) {
                                    holdJob?.cancel()
                                    holdProgress = 0f
                                }
                                isPressed = false
                            }
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = 10.dp.toPx()
                val inset = strokeWidth / 2f + 4.dp.toPx()
                val arcSize = Size(size.width - inset * 2f, size.height - inset * 2f)
                val topLeft = Offset(inset, inset)

                // Ambient glow when running
                if (isRunning || isPressed) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                ringColor.copy(alpha = if (isRunning) runningGlow * 0.32f else 0.25f),
                                Color.Transparent
                            )
                        ),
                        radius = size.minDimension / 2f
                    )
                }

                // Base track ring
                drawArc(
                    color = ringColor.copy(alpha = 0.18f),
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth)
                )

                // Active ring: hold progress, cranking spinner, or countdown progress
                when {
                    holdProgress > 0f -> {
                        drawArc(
                            color = ringColor,
                            startAngle = -90f,
                            sweepAngle = 360f * holdProgress,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                    }
                    isCranking -> {
                        drawArc(
                            color = IgnitionAmberGlow,
                            startAngle = crankRotation,
                            sweepAngle = 110f,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                    }
                    isRunning -> {
                        drawArc(
                            brush = Brush.sweepGradient(
                                colors = listOf(IgnitionAmber, IgnitionAmberGlow, IgnitionAmber)
                            ),
                            startAngle = -90f,
                            sweepAngle = 360f * timerFraction,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                    }
                }
            }

            // Inner metallic button core
            Box(
                modifier = Modifier
                    .size(136.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.verticalGradient(
                            colors = if (isRunning || isCranking) {
                                listOf(Color(0xFF2B1810), Color(0xFF170D09))
                            } else {
                                listOf(Color(0xFF1D283A), Color(0xFF101723))
                            }
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = if (isRunning) Icons.Rounded.Stop else Icons.Rounded.PowerSettingsNew,
                        contentDescription = null,
                        tint = ringColor,
                        modifier = Modifier.size(34.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = when (engineState) {
                            EngineState.STOPPED -> "ЗАПУСК"
                            EngineState.CRANKING -> "СТАРТ…"
                            EngineState.RUNNING -> formatMmSs(remainingSeconds)
                        },
                        fontFamily = if (isRunning) MonoFontFamily else MaterialTheme.typography.titleMedium.fontFamily,
                        fontWeight = FontWeight.Bold,
                        style = if (isRunning) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleMedium,
                        color = Color(0xFFF8FAFC)
                    )
                    Text(
                        text = when (engineState) {
                            EngineState.STOPPED -> "ДВИГАТЕЛЯ"
                            EngineState.CRANKING -> "ПРОКРУТКА"
                            EngineState.RUNNING -> "СТОП"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = ringColor
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = when {
                isCranking -> "Выполняется автозапуск…"
                isRunning && holdToConfirm -> "Удерживайте для остановки"
                isRunning -> "Нажмите для остановки двигателя"
                holdToConfirm -> "Удерживайте 1 сек для запуска"
                else -> "Нажмите для запуска двигателя"
            },
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

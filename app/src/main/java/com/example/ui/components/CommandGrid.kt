package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.CarCommand
import com.example.data.model.SecurityState
import com.example.data.model.VehicleStatus
import com.example.ui.theme.AlertCrimson
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.IgnitionAmber
import com.example.ui.theme.SafeEmerald

@Composable
fun CommandGrid(
    status: VehicleStatus,
    onCommand: (CarCommand) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CommandTile(
                title = "Закрыть",
                subtitle = "На охрану",
                icon = Icons.Rounded.Lock,
                accentColor = ElectricCyan,
                isActive = status.securityState == SecurityState.ARMED,
                testTagName = "btn_lock",
                onClick = { onCommand(CarCommand.LOCK) },
                modifier = Modifier.weight(1f)
            )
            CommandTile(
                title = "Открыть",
                subtitle = "Снять охрану",
                icon = Icons.Rounded.LockOpen,
                accentColor = SafeEmerald,
                isActive = status.securityState == SecurityState.DISARMED,
                testTagName = "btn_unlock",
                onClick = { onCommand(CarCommand.UNLOCK) },
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CommandTile(
                title = "Багажник",
                subtitle = if (status.isTrunkOpen) "Открыт" else "Открыть",
                icon = Icons.Rounded.DirectionsCar,
                accentColor = IgnitionAmber,
                isActive = status.isTrunkOpen,
                testTagName = "btn_trunk",
                onClick = { onCommand(CarCommand.TRUNK_OPEN) },
                modifier = Modifier.weight(1f)
            )
            CommandTile(
                title = "Поиск авто",
                subtitle = "Сигнал и свет",
                icon = Icons.Rounded.NotificationsActive,
                accentColor = AlertCrimson,
                isActive = status.securityState == SecurityState.PANIC,
                testTagName = "btn_find_car",
                onClick = { onCommand(CarCommand.FIND_CAR) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun CommandTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    isActive: Boolean,
    testTagName: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor = if (isActive) {
        accentColor.copy(alpha = 0.14f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val borderColor = if (isActive) {
        accentColor.copy(alpha = 0.65f)
    } else {
        MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)
    }

    Surface(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .testTag(testTagName),
        shape = RoundedCornerShape(20.dp),
        color = containerColor,
        border = BorderStroke(1.dp, borderColor),
        tonalElevation = if (isActive) 4.dp else 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 15.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(accentColor.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = accentColor,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isActive) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

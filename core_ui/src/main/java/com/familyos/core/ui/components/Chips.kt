package com.familyos.core.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import com.familyos.core.domain.model.FamilyRole
import com.familyos.core.domain.model.ShoppingStatus
import com.familyos.core.domain.model.TaskPriority
import com.familyos.core.domain.model.TaskStatus
import com.familyos.core.ui.theme.FamilyOsDanger
import com.familyos.core.ui.theme.FamilyOsSuccess
import com.familyos.core.ui.theme.FamilyOsWarning

@Composable
fun RoleBadge(role: FamilyRole) {
    val fg = when (role) {
        FamilyRole.OWNER -> FamilyOsDanger
        FamilyRole.ADMIN -> MaterialTheme.colorScheme.primary
        FamilyRole.MEMBER -> MaterialTheme.colorScheme.secondary
        FamilyRole.GUEST -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Box(contentAlignment = Alignment.Center) {
        Text(role.name, color = fg, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun PriorityChip(priority: TaskPriority) {
    val color = when (priority) {
        TaskPriority.LOW -> Color(0xFF64748B)
        TaskPriority.MEDIUM -> MaterialTheme.colorScheme.primary
        TaskPriority.HIGH -> FamilyOsWarning
        TaskPriority.URGENT -> FamilyOsDanger
    }
    AssistChip(
        onClick = {},
        enabled = false,
        label = { Text(priority.name) },
        colors = AssistChipDefaults.assistChipColors(
            disabledContainerColor = color.copy(alpha = 0.15f),
            disabledLabelColor = color,
        ),
    )
}

enum class StatusTone { Neutral, Info, Success, Warning, Danger }

@Composable
fun StatusChip(label: String, tone: StatusTone = StatusTone.Neutral) {
    val color = when (tone) {
        StatusTone.Success -> FamilyOsSuccess
        StatusTone.Warning -> FamilyOsWarning
        StatusTone.Danger -> FamilyOsDanger
        StatusTone.Neutral -> MaterialTheme.colorScheme.onSurfaceVariant
        StatusTone.Info -> MaterialTheme.colorScheme.primary
    }
    AssistChip(
        onClick = {},
        enabled = false,
        label = { Text(label) },
        colors = AssistChipDefaults.assistChipColors(
            disabledContainerColor = color.copy(alpha = 0.15f),
            disabledLabelColor = color,
        ),
    )
}

@Composable
fun TaskStatusChip(status: TaskStatus) {
    val tone = when (status) {
        TaskStatus.NEW -> StatusTone.Neutral
        TaskStatus.IN_PROGRESS -> StatusTone.Info
        TaskStatus.WAITING -> StatusTone.Warning
        TaskStatus.DONE -> StatusTone.Success
        TaskStatus.CANCELLED -> StatusTone.Danger
        TaskStatus.OVERDUE -> StatusTone.Danger
    }
    StatusChip(label = status.name.replace('_', ' '), tone = tone)
}

@Composable
fun ShoppingStatusChip(status: ShoppingStatus) {
    val tone = when (status) {
        ShoppingStatus.ACTIVE -> StatusTone.Info
        ShoppingStatus.PURCHASED -> StatusTone.Success
        ShoppingStatus.ARCHIVED -> StatusTone.Neutral
    }
    StatusChip(label = status.name, tone = tone)
}

from pathlib import Path

UI = Path(r"d:/Projects/Develop/FamilyOS/core_ui/src/main/java/com/familyos/core/ui")
alias = UI / "theme" / "ModifierAliases.kt"
if alias.exists():
    alias.unlink()
    print("deleted alias")

(UI / "components" / "Chips.kt").write_text(
    r'''package com.familyos.core.ui.components

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
        TaskStatus.TODO -> StatusTone.Neutral
        TaskStatus.IN_PROGRESS -> StatusTone.Info
        TaskStatus.DONE -> StatusTone.Success
        TaskStatus.CANCELLED -> StatusTone.Danger
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
''',
    encoding="utf-8",
    newline="\n",
)

(UI / "components" / "AsyncImageWithPlaceholder.kt").write_text(
    r'''package com.familyos.core.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BrokenImage
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.request.ImageRequest

@Composable
fun AsyncImageWithPlaceholder(
    model: Any?,
    contentDescription: String?,
    contentScale: ContentScale = ContentScale.Crop,
) {
    SubcomposeAsyncImage(
        model = ImageRequest.Builder(LocalContext.current).data(model).crossfade(true).build(),
        contentDescription = contentDescription,
        contentScale = contentScale,
    ) {
        when (painter.state) {
            is AsyncImagePainter.State.Loading -> Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            is AsyncImagePainter.State.Error -> Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.BrokenImage, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            is AsyncImagePainter.State.Empty -> Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.Image, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            else -> SubcomposeAsyncImageContent()
        }
    }
}
''',
    encoding="utf-8",
    newline="\n",
)

print("fixed chips async")

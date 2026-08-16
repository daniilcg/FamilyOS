package com.familyos.feature.shopping.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.familyos.core.domain.model.ShoppingItem
import com.familyos.core.domain.model.ShoppingStatus
import com.familyos.feature.shopping.util.formatEpoch
import com.familyos.feature.shopping.util.formatPrice
import com.familyos.feature.shopping.util.label
import com.familyos.core.ui.locale.rememberUiStrings

/**
 * Row card for a shopping list entry with contextual actions.
 */
@Composable
fun ShoppingItemRow(
    item: ShoppingItem,
    onClick: () -> Unit,
    onPurchase: (() -> Unit)? = null,
    onArchive: (() -> Unit)? = null,
    onRestore: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val s = rememberUiStrings()

    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (!item.photoUri.isNullOrBlank()) {
                AsyncImage(
                    model = item.photoUri,
                    contentDescription = item.title,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop,
                )
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = item.title, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = buildString {
                        append(item.quantity.stripTrailingZeros())
                        if (!item.unit.isNullOrBlank()) append(" ").append(item.unit)
                        append(" · ").append(item.category.label())
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AssistChip(onClick = {}, label = { Text(item.status.label()) }, enabled = false)
                    Text(
                        text = formatPrice(item.estimatedPrice, item.currency),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.align(Alignment.CenterVertically),
                    )
                }
                if (!item.notes.isNullOrBlank()) {
                    Text(
                        text = item.notes.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    )
                }
                Text(
                    text = formatEpoch(item.createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                )
            }
            Column {
                onPurchase?.let {
                    IconButton(onClick = it) {
                        Icon(Icons.Outlined.CheckCircle, contentDescription = s.purchase)
                    }
                }
                onArchive?.let {
                    IconButton(onClick = it) {
                        Icon(Icons.Outlined.Archive, contentDescription = s.archive)
                    }
                }
                onRestore?.let {
                    IconButton(onClick = it) {
                        Icon(Icons.Outlined.Restore, contentDescription = s.restore)
                    }
                }
                onDelete?.let {
                    IconButton(onClick = it) {
                        Icon(Icons.Outlined.Delete, contentDescription = s.delete)
                    }
                }
            }
        }
    }
}

private fun Double.stripTrailingZeros(): String {
    return if (this % 1.0 == 0.0) toInt().toString() else toString()
}

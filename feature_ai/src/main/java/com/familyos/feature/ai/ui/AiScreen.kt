package com.familyos.feature.ai.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.familyos.core.domain.model.AiMessage
import com.familyos.feature.ai.provider.AiProviderId
import com.familyos.feature.ai.viewmodel.AiUiMessage
import com.familyos.feature.ai.viewmodel.AiUiState

/**
 * Family AI chat UI with provider switching and apply-action CTA.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiScreen(
    state: AiUiState,
    onDraftChange: (String) -> Unit,
    onSend: () -> Unit,
    onProviderChange: (AiProviderId) -> Unit,
    onApplyAction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text("Family AI") }) },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AiProviderId.entries.forEach { provider ->
                    FilterChip(
                        selected = state.providerId == provider,
                        onClick = { onProviderChange(provider) },
                        label = { Text(provider.displayName) },
                    )
                }
            }
            if (!state.isPremium) {
                Text(
                    "Premium required for Family AI",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            }
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(state.messages, key = { it.id }) { message ->
                    AiBubble(message)
                }
            }
            if (state.pendingAction != null) {
                Button(onClick = onApplyAction, modifier = Modifier.fillMaxWidth()) {
                    Text("Apply to family (shopping / tasks)")
                }
            }
            state.appliedMessage?.let {
                Text(it, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(4.dp))
            }
            state.errorMessage?.let {
                Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(4.dp))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = state.draft,
                    onValueChange = onDraftChange,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("e.g. Borscht for 6") },
                    enabled = !state.isSending,
                )
                IconButton(onClick = onSend, enabled = state.draft.isNotBlank() && !state.isSending) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
                }
            }
            Text(
                "Try: birthday prep · budget 1200 EUR · trip checklist",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.padding(bottom = 8.dp, top = 4.dp),
            )
        }
    }
}

@Composable
private fun AiBubble(message: AiUiMessage) {
    val mine = message.role == AiMessage.Role.USER
    val bg = if (mine) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    val fg = if (mine) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (mine) Alignment.End else Alignment.Start,
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 340.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(bg)
                .padding(12.dp),
        ) {
            Text(message.content, color = fg)
        }
    }
}

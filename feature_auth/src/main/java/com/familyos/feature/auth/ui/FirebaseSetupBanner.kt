package com.familyos.feature.auth.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.familyos.core.ui.locale.rememberUiStrings

/**
 * Info banner for local offline auth mode (not an error).
 */
@Composable
fun FirebaseSetupBanner(
    visible: Boolean,
    modifier: Modifier = Modifier,
) {
    if (!visible) return
    val s = rememberUiStrings()
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .padding(12.dp),
    ) {
        Text(
            text = s.localModeTitle,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
        )
        Text(
            text = s.localModeBody,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
        )
    }
}

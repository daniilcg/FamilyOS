package com.familyos.feature.budget.ui.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.familyos.core.domain.model.BudgetCategory
import com.familyos.core.ui.theme.FamilyDanger
import com.familyos.core.ui.theme.FamilyPrimary
import com.familyos.core.ui.theme.FamilySecondary
import com.familyos.core.ui.theme.FamilySuccess
import com.familyos.core.ui.theme.FamilyWarning
import com.familyos.feature.budget.util.formatMoney
import com.familyos.feature.budget.util.label
import kotlin.math.min
import com.familyos.core.ui.locale.rememberUiStrings

private val chartPalette = listOf(
    FamilyPrimary,
    FamilySecondary,
    FamilySuccess,
    FamilyWarning,
    FamilyDanger,
    Color(0xFF0EA5E9),
    Color(0xFF14B8A6),
    Color(0xFFA855F7),
)

/**
 * Simple Canvas bar chart for category spend totals.
 */
@Composable
fun CategoryBarChart(
    values: Map<BudgetCategory, Double>,
    currency: String,
    modifier: Modifier = Modifier,
) {
    val s = rememberUiStrings()
    val entries = values.filter { it.value > 0.0 }.entries.toList()
    if (entries.isEmpty()) {
        Text(s.noExpenseData, style = MaterialTheme.typography.bodyMedium)
        return
    }
    val max = entries.maxOf { it.value }
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp),
        ) {
            val barWidth = size.width / (entries.size * 1.6f)
            val gap = barWidth * 0.6f
            entries.forEachIndexed { index, entry ->
                val height = (entry.value / max).toFloat() * size.height * 0.9f
                val left = index * (barWidth + gap) + gap
                drawRect(
                    color = chartPalette[index % chartPalette.size],
                    topLeft = Offset(left, size.height - height),
                    size = Size(barWidth, height),
                )
            }
        }
        entries.forEachIndexed { index, entry ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Canvas(modifier = Modifier.size(12.dp)) {
                    drawRect(chartPalette[index % chartPalette.size])
                }
                Text("${entry.key.label()}: ${formatMoney(entry.value, currency)}")
            }
        }
    }
}

/**
 * Simple Canvas pie chart for category distribution.
 */
@Composable
fun CategoryPieChart(
    values: Map<BudgetCategory, Double>,
    currency: String,
    modifier: Modifier = Modifier,
) {
    val s = rememberUiStrings()
    val entries = values.filter { it.value > 0.0 }.entries.toList()
    if (entries.isEmpty()) {
        Text(s.noExpenseData, style = MaterialTheme.typography.bodyMedium)
        return
    }
    val total = entries.sumOf { it.value }
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Canvas(modifier = Modifier.size(200.dp)) {
            val diameter = min(size.width, size.height)
            var start = -90f
            entries.forEachIndexed { index, entry ->
                val sweep = ((entry.value / total) * 360.0).toFloat()
                drawArc(
                    color = chartPalette[index % chartPalette.size],
                    startAngle = start,
                    sweepAngle = sweep,
                    useCenter = true,
                    size = Size(diameter, diameter),
                    topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f),
                )
                start += sweep
            }
            drawCircle(
                color = Color.White.copy(alpha = 0.92f),
                radius = diameter * 0.28f,
            )
        }
        entries.forEachIndexed { index, entry ->
            val percent = (entry.value / total) * 100.0
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Canvas(modifier = Modifier.size(10.dp)) {
                        drawCircle(chartPalette[index % chartPalette.size])
                    }
                    Text(entry.key.label())
                }
                Text("${"%.1f".format(percent)}% · ${formatMoney(entry.value, currency)}")
            }
        }
    }
}


package com.reelmonster.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun SectionCard(
    title: String,
    modifier: Modifier = Modifier,
    container: Color = MaterialTheme.colorScheme.surfaceVariant,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = container)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            content()
        }
    }
}

@Composable
fun SwitchRow(title: String, subtitle: String?, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, fontWeight = FontWeight.Medium)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
fun SliderSetting(
    title: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    steps: Int = 0,
    label: (Float) -> String,
    onCommit: (Float) -> Unit
) {
    var v by remember(value) { mutableFloatStateOf(value) }
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(title, fontWeight = FontWeight.Medium)
            Text(label(v), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        }
        Slider(
            value = v,
            onValueChange = { v = it },
            onValueChangeFinished = { onCommit(v) },
            valueRange = range,
            steps = steps
        )
    }
}

@Composable
fun InfoCard(title: String, body: String, actionLabel: String? = null, onAction: (() -> Unit)? = null, status: String? = null) {
    SectionCard(title) {
        if (status != null) Text(status, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        Text(body, style = MaterialTheme.typography.bodyMedium)
        if (actionLabel != null && onAction != null) Button(onClick = onAction) { Text(actionLabel) }
    }
}

package com.najmi.sciuro.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.najmi.sciuro.core.ui.theme.BrandPrimaryLight

@Composable
fun PillToggle(
    options: List<String>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    isOnDarkSurface: Boolean = false,
    fillWidth: Boolean = false
) {
    val containerColor = if (isOnDarkSurface) Color.White.copy(alpha = 0.1f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
    val activeColor = if (isOnDarkSurface) Color.White else MaterialTheme.colorScheme.primary
    val activeTextColor = if (isOnDarkSurface) Color.Black else MaterialTheme.colorScheme.onPrimary
    val inactiveTextColor = if (isOnDarkSurface) Color.White.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(containerColor)
            .padding(4.dp)
    ) {
        Row(
            modifier = if (fillWidth) Modifier.fillMaxWidth() else Modifier.wrapContentSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            options.forEach { option ->
                val isSelected = option == selectedOption
                Box(
                    modifier = Modifier
                        .then(if (fillWidth) Modifier.weight(1f) else Modifier)
                        .defaultMinSize(minHeight = 44.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isSelected) activeColor else Color.Transparent)
                        .clickable { onOptionSelected(option) }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = option,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                        color = if (isSelected) activeTextColor else inactiveTextColor
                    )
                }
            }
        }
    }
}


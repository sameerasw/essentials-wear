package com.sameerasw.essentials.presentation.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalView
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipBorder
import androidx.wear.compose.material.ChipColors
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.Text
import com.sameerasw.essentials.utils.HapticUtil

@Composable
fun EssentialsChip(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    secondaryLabel: String? = null,
    icon: Painter? = null,
    colors: ChipColors = ChipDefaults.secondaryChipColors(),
    enabled: Boolean = true,
    border: ChipBorder
) {
    val view = LocalView.current
    Chip(
        onClick = {
            HapticUtil.performUIHaptic(view)
            onClick()
        },
        label = { Text(label) },
        secondaryLabel = secondaryLabel?.let { { Text(it) } },
        icon = icon?.let {
            {
                Icon(
                    painter = it,
                    contentDescription = null,
                    modifier = Modifier
                        .size(ChipDefaults.IconSize)
                        .wrapContentSize(align = Alignment.Center)
                )
            }
        },
        modifier = modifier.fillMaxWidth(),
        colors = colors,
        enabled = enabled,
        border = border
    )
}

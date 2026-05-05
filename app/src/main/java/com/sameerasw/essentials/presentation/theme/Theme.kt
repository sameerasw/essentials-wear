package com.sameerasw.essentials.presentation.theme

import androidx.compose.runtime.Composable
import androidx.wear.compose.material.MaterialTheme

@Composable
fun EssentialsTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        typography = WearTypography,
        content = content
    )
}
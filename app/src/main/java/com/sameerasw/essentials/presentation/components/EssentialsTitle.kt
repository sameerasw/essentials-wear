package com.sameerasw.essentials.presentation.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.sameerasw.essentials.presentation.theme.GoogleSansFlexRoundedWide

@Composable
fun EssentialsTitle(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified
) {
    Text(
        text = text,
        style = MaterialTheme.typography.title1.copy(
            fontFamily = GoogleSansFlexRoundedWide
        ),
        modifier = modifier.fillMaxWidth(),
        textAlign = TextAlign.Center,
        color = color
    )
}

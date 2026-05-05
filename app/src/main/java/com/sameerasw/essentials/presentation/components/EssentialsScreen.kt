package com.sameerasw.essentials.presentation.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.wear.compose.foundation.lazy.AutoCenteringParams
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.ScalingLazyListScope
import androidx.wear.compose.foundation.lazy.ScalingLazyListState
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.TimeText

@Composable
fun EssentialsScreen(
    modifier: Modifier = Modifier,
    state: ScalingLazyListState = rememberScalingLazyListState(),
    userScrollEnabled: Boolean = true,
    content: ScalingLazyListScope.() -> Unit
) {
    Scaffold(
        timeText = { TimeText() }
    ) {
        ScalingLazyColumn(
            modifier = modifier.fillMaxSize(),
            state = state,
            autoCentering = AutoCenteringParams(itemIndex = 0),
            userScrollEnabled = userScrollEnabled,
            content = content
        )
    }
}

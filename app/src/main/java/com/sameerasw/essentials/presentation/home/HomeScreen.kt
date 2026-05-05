package com.sameerasw.essentials.presentation.home

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.wear.compose.material.ChipDefaults
import com.sameerasw.essentials.R
import com.sameerasw.essentials.presentation.components.EssentialsChip
import com.sameerasw.essentials.presentation.components.EssentialsScreen
import com.sameerasw.essentials.presentation.components.EssentialsTitle
import com.sameerasw.essentials.presentation.navigation.NavRoutes
import com.sameerasw.essentials.utils.ThemeUtil

@Composable
fun HomeScreen(onNavigate: (String) -> Unit) {
    val context = LocalContext.current
    val themeColor = ThemeUtil.getThemeColor(context)

    val tonedThemeColor = themeColor?.let {
        Color(ThemeUtil.getTonedColor(it))
    } ?: Color.DarkGray

    val lightAccentColor = themeColor?.let {
        Color(ThemeUtil.getLightAccentColor(it))
    } ?: Color(0xFFB39DDB.toInt())

    EssentialsScreen {
        item {
            EssentialsTitle(
                text = stringResource(R.string.app_name),
                color = lightAccentColor
            )
        }

        // Schedule
        item {
            EssentialsChip(
                label = stringResource(R.string.feature_schedule),
                onClick = { onNavigate(NavRoutes.SCHEDULE) },
                icon = painterResource(R.drawable.rounded_schedule_24),
                colors = ChipDefaults.secondaryChipColors(
                    backgroundColor = tonedThemeColor,
                    contentColor = Color.White
                ),
                border = ChipDefaults.chipBorder(
                    borderStroke = null
                )
            )
        }

        // Your Android
        item {
            EssentialsChip(
                label = stringResource(R.string.feature_your_android),
                onClick = { onNavigate(NavRoutes.YOUR_ANDROID) },
                icon = painterResource(R.drawable.rounded_mobile_24),
                colors = ChipDefaults.secondaryChipColors(
                    backgroundColor = tonedThemeColor,
                    contentColor = Color.White
                ),
                border = ChipDefaults.chipBorder(
                    borderStroke = null
                )
            )
        }

        // Tools
        item {
            EssentialsChip(
                label = stringResource(R.string.feature_tools),
                onClick = { },
                secondaryLabel = stringResource(R.string.coming_soon),
                icon = painterResource(R.drawable.rounded_settings_heart_24),
                colors = ChipDefaults.secondaryChipColors(
                    backgroundColor = tonedThemeColor,
                    contentColor = Color.White.copy(alpha = 0.5f),
                    secondaryContentColor = Color.White.copy(alpha = 0.3f)
                ),
                enabled = false,
                border = ChipDefaults.chipBorder(
                    borderStroke = null
                )
            )
        }
    }
}

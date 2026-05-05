package com.sameerasw.essentials.presentation.components

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.wear.compose.foundation.CurvedTextStyle
import androidx.wear.compose.foundation.curvedComposable
import androidx.wear.compose.foundation.lazy.ScalingLazyListState
import androidx.wear.compose.material.*
import com.sameerasw.essentials.R
import com.sameerasw.essentials.utils.ThemeUtil

@Composable
fun EssentialsTimeText(
    modifier: Modifier = Modifier,
    scrollState: ScalingLazyListState? = null
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("schedule_prefs", Context.MODE_PRIVATE) }
    
    var batteryLevel by remember { mutableStateOf(prefs.getInt("phone_battery_level", -1)) }
    var isCharging by remember { mutableStateOf(prefs.getBoolean("phone_is_charging", false)) }
    var deviceName by remember { mutableStateOf(prefs.getString("phone_device_name", "")) }

    val showDetails by remember(scrollState) {
        derivedStateOf {
            scrollState == null || (scrollState.centerItemIndex <= 1 && scrollState.centerItemScrollOffset <= 500)
        }
    }

    DisposableEffect(Unit) {
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { p, key ->
            when (key) {
                "phone_battery_level" -> batteryLevel = p.getInt(key, -1)
                "phone_is_charging" -> isCharging = p.getBoolean(key, false)
                "phone_device_name" -> deviceName = p.getString(key, "")
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose {
            prefs.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }

    val themeColor = remember { ThemeUtil.getThemeColor(context) }
    val lightAccentColor = themeColor?.let {
        Color(ThemeUtil.getLightAccentColor(it))
    } ?: Color(0xFFB39DDB.toInt())

    val typography = MaterialTheme.typography
    val colors = MaterialTheme.colors

    val batteryIcon = if (isCharging) {
        R.drawable.rounded_battery_android_frame_bolt_24
    } else {
        when {
            batteryLevel >= 75 -> R.drawable.rounded_battery_android_frame_full_24
            batteryLevel >= 50 -> R.drawable.rounded_battery_android_frame_5_24
            batteryLevel > 20 -> R.drawable.rounded_battery_android_frame_2_24
            else -> R.drawable.rounded_battery_android_alert_24
        }
    }

    val isAnyDetailVisible = showDetails && (!deviceName.isNullOrBlank() || batteryLevel != -1)

    if (!isAnyDetailVisible) {
        TimeText(modifier = modifier)
    } else {
        TimeText(
            modifier = modifier,
            textLinearSeparator = {},
            textCurvedSeparator = {},
            startLinearContent = if (!deviceName.isNullOrBlank()) {
                {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = deviceName!!,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = typography.caption1,
                            color = lightAccentColor
                        )
                        Icon(
                            painter = painterResource(id = R.drawable.rounded_mobile_24),
                            contentDescription = null,
                            modifier = Modifier.size(12.dp).padding(start = 2.dp),
                            tint = lightAccentColor
                        )
                        TimeTextDefaults.TextSeparator(
                            textStyle = typography.caption1.copy(color = lightAccentColor)
                        )
                    }
                }
            } else null,
            startCurvedContent = if (!deviceName.isNullOrBlank()) {
                {
                    curvedText(
                        text = deviceName!!,
                        style = CurvedTextStyle(typography.caption1.copy(color = lightAccentColor))
                    )
                    curvedComposable {
                        Icon(
                            painter = painterResource(id = R.drawable.rounded_mobile_24),
                            contentDescription = null,
                            modifier = Modifier.size(12.dp).padding(start = 2.dp),
                            tint = lightAccentColor
                        )
                    }
                    with(TimeTextDefaults) {
                        CurvedTextSeparator(
                            curvedTextStyle = CurvedTextStyle(typography.caption1.copy(color = lightAccentColor))
                        )
                    }
                }
            } else null,
            endLinearContent = if (batteryLevel != -1) {
                {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TimeTextDefaults.TextSeparator(
                            textStyle = typography.caption1.copy(color = lightAccentColor)
                        )
                        Icon(
                            painter = painterResource(id = batteryIcon),
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = lightAccentColor
                        )
                        Text(
                            text = "$batteryLevel%",
                            style = typography.caption1,
                            modifier = Modifier.padding(start = 2.dp),
                            color = lightAccentColor
                        )
                    }
                }
            } else null,
            endCurvedContent = if (batteryLevel != -1) {
                {
                    with(TimeTextDefaults) {
                        CurvedTextSeparator(
                            curvedTextStyle = CurvedTextStyle(typography.caption1.copy(color = lightAccentColor))
                        )
                    }
                    curvedComposable {
                        Icon(
                            painter = painterResource(id = batteryIcon),
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = lightAccentColor
                        )
                    }
                    curvedText(
                        text = " $batteryLevel%",
                        style = CurvedTextStyle(typography.caption1.copy(color = lightAccentColor))
                    )
                }
            } else null
        )
    }
}

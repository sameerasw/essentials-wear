package com.sameerasw.essentials.presentation

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.AutoCenteringParams
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.*
import com.google.android.gms.wearable.Wearable
import com.sameerasw.essentials.R
import com.sameerasw.essentials.utils.HapticUtil

@Composable
fun YourAndroidScreen() {
    val context = LocalContext.current
    val view = LocalView.current
    val listState = rememberScalingLazyListState()
    val prefs = context.getSharedPreferences("schedule_prefs", Context.MODE_PRIVATE)
    
    // Reactive state for battery info
    val batteryLevelState = remember { mutableStateOf(prefs.getInt("phone_battery_level", -1)) }
    val isChargingState = remember { mutableStateOf(prefs.getBoolean("phone_is_charging", false)) }

    // Observe preference changes
    DisposableEffect(Unit) {
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { p, key ->
            if (key == "phone_battery_level") {
                batteryLevelState.value = p.getInt(key, -1)
            } else if (key == "phone_is_charging") {
                isChargingState.value = p.getBoolean(key, false)
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose {
            prefs.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }

    val batteryLevel = batteryLevelState.value
    val isCharging = isChargingState.value

    val themeColor = androidx.compose.runtime.remember {
        com.sameerasw.essentials.utils.ThemeUtil.getThemeColor(context)
    }
    val lightAccentColor = themeColor?.let {
        androidx.compose.ui.graphics.Color(com.sameerasw.essentials.utils.ThemeUtil.getLightAccentColor(it))
    } ?: androidx.compose.ui.graphics.Color(0xFFB39DDB.toInt())

    val tonedThemeColor = themeColor?.let {
        androidx.compose.ui.graphics.Color(com.sameerasw.essentials.utils.ThemeUtil.getTonedColor(it))
    } ?: androidx.compose.ui.graphics.Color.DarkGray

    val bubbleColors = ButtonDefaults.buttonColors(
        backgroundColor = lightAccentColor,
        contentColor = androidx.compose.ui.graphics.Color.Black
    )

    // Request sync from phone on entry
    LaunchedEffect(Unit) {
        val nodeClient = Wearable.getNodeClient(context)
        nodeClient.connectedNodes.addOnSuccessListener { nodes ->
            val messageClient = Wearable.getMessageClient(context)
            for (node in nodes) {
                messageClient.sendMessage(node.id, "/request_device_info_sync", byteArrayOf())
            }
        }
    }

    Scaffold(
        timeText = { TimeText() }
    ) {
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState,
            autoCentering = AutoCenteringParams(itemIndex = 0)
        ) {
            item {
                Text(
                    text = stringResource(R.string.your_android_title),
                    style = MaterialTheme.typography.title1.copy(
                        fontFamily = com.sameerasw.essentials.presentation.theme.GoogleSansFlexRoundedWide
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    color = lightAccentColor
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Battery Bubble
                    Button(
                        onClick = { 
                            HapticUtil.performUIHaptic(view)
                            val nodeClient = Wearable.getNodeClient(context)
                            nodeClient.connectedNodes.addOnSuccessListener { nodes ->
                                val messageClient = Wearable.getMessageClient(context)
                                for (node in nodes) {
                                    messageClient.sendMessage(node.id, "/request_device_info_sync", byteArrayOf())
                                }
                            }
                        },
                        modifier = Modifier.size(ButtonDefaults.LargeButtonSize),
                        colors = bubbleColors
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
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
                            Icon(
                                painter = painterResource(id = batteryIcon),
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                            if (batteryLevel != -1) {
                                Text(
                                    text = "$batteryLevel%",
                                    style = MaterialTheme.typography.caption3
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Placeholder Bubble 1
                    Button(
                        onClick = { HapticUtil.performUIHaptic(view) },
                        enabled = false,
                        modifier = Modifier.size(ButtonDefaults.LargeButtonSize),
                        colors = bubbleColors
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.rounded_shapes_24),
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Placeholder Bubble 2
                    Button(
                        onClick = { HapticUtil.performUIHaptic(view) },
                        enabled = false,
                        modifier = Modifier.size(ButtonDefaults.LargeButtonSize),
                        colors = bubbleColors
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.rounded_shapes_24),
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}

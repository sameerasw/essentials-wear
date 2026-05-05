package com.sameerasw.essentials.presentation.yourandroid

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.google.android.gms.wearable.Wearable
import com.sameerasw.essentials.R
import com.sameerasw.essentials.presentation.components.EssentialsScreen
import com.sameerasw.essentials.presentation.components.EssentialsTitle
import com.sameerasw.essentials.utils.HapticUtil
import com.sameerasw.essentials.utils.ThemeUtil

@Composable
fun YourAndroidScreen() {
    val context = LocalContext.current
    val view = LocalView.current
    val prefs = context.getSharedPreferences("schedule_prefs", Context.MODE_PRIVATE)

    // Reactive state for battery info
    val batteryLevelState = remember { mutableStateOf(prefs.getInt("phone_battery_level", -1)) }
    val isChargingState = remember { mutableStateOf(prefs.getBoolean("phone_is_charging", false)) }

    // Observe preference changes
    DisposableEffect(Unit) {
        val listener =
            android.content.SharedPreferences.OnSharedPreferenceChangeListener { p, key ->
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

    val themeColor = remember { ThemeUtil.getThemeColor(context) }
    val lightAccentColor = themeColor?.let {
        Color(ThemeUtil.getLightAccentColor(it))
    } ?: Color(0xFFB39DDB.toInt())

    val bubbleColors = ButtonDefaults.buttonColors(
        backgroundColor = lightAccentColor,
        contentColor = Color.Black
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

    EssentialsScreen {
        item {
            EssentialsTitle(
                text = stringResource(R.string.your_android_title),
                color = lightAccentColor
            )
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
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
                                messageClient.sendMessage(
                                    node.id,
                                    "/request_device_info_sync",
                                    byteArrayOf()
                                )
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

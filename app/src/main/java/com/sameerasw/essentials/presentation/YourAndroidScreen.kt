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
    
    val batteryLevel = prefs.getInt("phone_battery_level", -1)
    val isCharging = prefs.getBoolean("phone_is_charging", false)
    val themeColor = androidx.compose.runtime.remember {
        com.sameerasw.essentials.utils.ThemeUtil.getThemeColor(context)
    }
    val lightAccentColor = themeColor?.let {
        androidx.compose.ui.graphics.Color(com.sameerasw.essentials.utils.ThemeUtil.getLightAccentColor(it))
    } ?: androidx.compose.ui.graphics.Color(0xFFB39DDB.toInt())

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
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Battery Bubble
                    Button(
                        onClick = { 
                            HapticUtil.performUIHaptic(view)
                            // Could trigger another sync request here
                            val nodeClient = Wearable.getNodeClient(context)
                            nodeClient.connectedNodes.addOnSuccessListener { nodes ->
                                val messageClient = Wearable.getMessageClient(context)
                                for (node in nodes) {
                                    messageClient.sendMessage(node.id, "/request_device_info_sync", byteArrayOf())
                                }
                            }
                        },
                        modifier = Modifier.size(ButtonDefaults.LargeButtonSize)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                painter = painterResource(id = if (isCharging) R.drawable.rounded_battery_android_frame_bolt_24 else R.drawable.rounded_battery_android_frame_full_24),
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
                        modifier = Modifier.size(ButtonDefaults.LargeButtonSize)
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
                        modifier = Modifier.size(ButtonDefaults.LargeButtonSize)
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

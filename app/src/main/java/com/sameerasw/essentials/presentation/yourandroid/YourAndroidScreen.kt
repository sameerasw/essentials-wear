package com.sameerasw.essentials.presentation.yourandroid

import android.content.Context
import kotlinx.coroutines.delay
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.basicMarquee
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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.border
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.compose.ui.input.rotary.onPreRotaryScrollEvent
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.google.android.gms.wearable.CapabilityClient
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
    val focusRequester = remember { FocusRequester() }
    
    // Reactive state for battery info
    val batteryLevelState = remember { mutableStateOf(prefs.getInt("phone_battery_level", -1)) }
    val isChargingState = remember { mutableStateOf(prefs.getBoolean("phone_is_charging", false)) }

    // Reactive state for flashlight info
    val flashlightOnState = remember { mutableStateOf(prefs.getBoolean("phone_flashlight_on", false)) }
    val flashlightLevelState = remember { mutableStateOf(prefs.getInt("phone_flashlight_level", 1)) }
    val flashlightMaxLevelState = remember { mutableStateOf(prefs.getInt("phone_flashlight_max_level", 1)) }
    val flashlightIntensitySupportedState = remember { mutableStateOf(prefs.getBoolean("phone_flashlight_intensity_supported", false)) }
    val ringerModeState = remember { mutableStateOf(prefs.getInt("phone_ringer_mode", 2)) }
    val deviceNameState = remember { mutableStateOf(prefs.getString("phone_device_name", "")) }

    // Local brightness for smooth crown adjustment
    var localFlashlightLevel by remember { mutableStateOf(flashlightLevelState.value.toFloat()) }
    var lastUserAdjustmentTime by remember { mutableStateOf(0L) }

    fun sendMessage(path: String, data: ByteArray = byteArrayOf()) {
        val nodeClient = Wearable.getNodeClient(context)
        nodeClient.connectedNodes.addOnSuccessListener { nodes ->
            val messageClient = Wearable.getMessageClient(context)
            for (node in nodes) {
                messageClient.sendMessage(node.id, path, data)
            }
        }
    }

    // Sync local level when state changes from phone
    LaunchedEffect(flashlightLevelState.value) {
        if (System.currentTimeMillis() - lastUserAdjustmentTime > 1500) {
            localFlashlightLevel = flashlightLevelState.value.toFloat()
        }
    }

    // Observe preference changes
    DisposableEffect(Unit) {
        val listener =
            android.content.SharedPreferences.OnSharedPreferenceChangeListener { p, key ->
                when (key) {
                    "phone_battery_level" -> batteryLevelState.value = p.getInt(key, -1)
                    "phone_is_charging" -> isChargingState.value = p.getBoolean(key, false)
                    "phone_flashlight_on" -> flashlightOnState.value = p.getBoolean(key, false)
                    "phone_flashlight_level" -> flashlightLevelState.value = p.getInt(key, 1)
                    "phone_flashlight_max_level" -> flashlightMaxLevelState.value = p.getInt(key, 1)
                    "phone_flashlight_intensity_supported" -> flashlightIntensitySupportedState.value = p.getBoolean(key, false)
                    "phone_ringer_mode" -> ringerModeState.value = p.getInt(key, 2)
                    "phone_device_name" -> deviceNameState.value = p.getString(key, "")
                }
            }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose {
            prefs.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }

    // Sync brightness to phone with debouncing
    LaunchedEffect(localFlashlightLevel) {
        if (flashlightOnState.value && flashlightIntensitySupportedState.value) {
            // Debounce to avoid spamming the wearable message layer
            delay(100)
            val roundedLevel = localFlashlightLevel.toInt().coerceIn(1, flashlightMaxLevelState.value)
            if (roundedLevel != flashlightLevelState.value) {
                sendMessage("/set_flashlight_intensity", roundedLevel.toString().toByteArray())
            }
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

    // Request focus for rotary events
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    EssentialsScreen(
        userScrollEnabled = !flashlightOnState.value,
        modifier = Modifier
            .onPreRotaryScrollEvent {
                if (flashlightOnState.value && flashlightIntensitySupportedState.value) {
                    val delta = it.verticalScrollPixels
                    val maxLevel = flashlightMaxLevelState.value
                    // Adjust sensitivity (delta / 10f)
                    val newLevel = (localFlashlightLevel + delta / 10f).coerceIn(1f, maxLevel.toFloat())
                    if (newLevel.toInt() != localFlashlightLevel.toInt()) {
                        HapticUtil.performUIHaptic(view)
                    }
                    lastUserAdjustmentTime = System.currentTimeMillis()
                    localFlashlightLevel = newLevel
                    true
                } else {
                    false
                }
            }
            .focusRequester(focusRequester)
            .focusable()
    ) {
        item {
            val title = if (!deviceNameState.value.isNullOrBlank()) deviceNameState.value!! else stringResource(R.string.your_android_title)
            EssentialsTitle(
                text = title,
                color = lightAccentColor,
                modifier = Modifier.basicMarquee()
            )
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Battery Bubble
                Button(
                    onClick = {
                        HapticUtil.performUIHaptic(view)
                        sendMessage("/request_device_info_sync")
                    },
                    modifier = Modifier.size(52.dp),
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
                                text = "$batteryLevel",
                                style = MaterialTheme.typography.caption3
                            )
                        }
                    }
                }

                // Flashlight Bubble
                val flashlightOn = flashlightOnState.value
                val flashlightLevel = localFlashlightLevel
                val maxLevel = flashlightMaxLevelState.value
                val intensitySupported = flashlightIntensitySupportedState.value
                
                val buttonColors = if (flashlightOn) {
                    bubbleColors // Filled accent
                } else {
                    ButtonDefaults.buttonColors(
                        backgroundColor = Color.Transparent,
                        contentColor = Color.White
                    )
                }

                Button(
                    onClick = {
                        HapticUtil.performUIHaptic(view)
                        sendMessage("/toggle_flashlight")
                    },
                    modifier = Modifier
                        .size(52.dp)
                        .then(
                            if (!flashlightOn) Modifier.border(
                                BorderStroke(1.dp, lightAccentColor.copy(alpha = 0.5f)),
                                CircleShape
                            ) else Modifier
                        ),
                    colors = buttonColors,
                    shape = CircleShape
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        // Inset black ring inside button
                        if (flashlightOn && intensitySupported) {
                            CircularProgressIndicator(
                                progress = flashlightLevel / maxOf(1f, maxLevel.toFloat()),
                                modifier = Modifier.size(52.dp),
                                strokeWidth = 3.dp,
                                indicatorColor = Color.Black.copy(alpha = 0.35f),
                                trackColor = Color.Transparent
                            )
                        }
                        
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                painter = painterResource(id = if (flashlightOn) R.drawable.round_flashlight_on_24 else R.drawable.rounded_flashlight_on_24),
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

                // Sound Mode Bubble
                val ringerMode = ringerModeState.value
                val isNormal = ringerMode == 2 // AudioManager.RINGER_MODE_NORMAL
                
                val soundModeColors = if (!isNormal) {
                    bubbleColors // Filled for Vibrate/Silent
                } else {
                    ButtonDefaults.buttonColors(
                        backgroundColor = Color.Transparent,
                        contentColor = Color.White
                    )
                }

                Button(
                    onClick = {
                        HapticUtil.performUIHaptic(view)
                        sendMessage("/toggle_sound_mode")
                    },
                    modifier = Modifier
                        .size(52.dp)
                        .then(
                            if (isNormal) Modifier.border(
                                BorderStroke(1.dp, lightAccentColor.copy(alpha = 0.5f)),
                                CircleShape
                            ) else Modifier
                        ),
                    colors = soundModeColors,
                    shape = CircleShape
                ) {
                    val soundIcon = when (ringerMode) {
                        1 -> R.drawable.rounded_mobile_vibrate_24
                        0 -> R.drawable.rounded_volume_off_24
                        else -> R.drawable.rounded_volume_up_24
                    }
                    Icon(
                        painter = painterResource(id = soundIcon),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

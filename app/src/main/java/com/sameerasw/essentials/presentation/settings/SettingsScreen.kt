package com.sameerasw.essentials.presentation.settings

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.ToggleChip
import androidx.wear.compose.material.ToggleChipDefaults
import androidx.wear.compose.material.Switch
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.Text
import androidx.compose.ui.Alignment
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.DisposableEffect
import com.google.android.gms.wearable.Wearable
import com.sameerasw.essentials.R
import com.sameerasw.essentials.presentation.components.EssentialsChip
import com.sameerasw.essentials.presentation.components.EssentialsScreen
import com.sameerasw.essentials.presentation.components.EssentialsTitle
import com.sameerasw.essentials.utils.HapticUtil
import com.sameerasw.essentials.utils.ThemeUtil

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val view = LocalView.current
    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val prefs = context.getSharedPreferences("schedule_prefs", Context.MODE_PRIVATE)

    val hasDndPermission = remember { mutableStateOf(notificationManager.isNotificationPolicyAccessGranted) }
    val hasWriteSecureSettings = remember {
        mutableStateOf(
            context.checkCallingOrSelfPermission(android.Manifest.permission.WRITE_SECURE_SETTINGS) ==
                    android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }

    val isSyncEnabled = remember { mutableStateOf(prefs.getBoolean("phone_watch_sync_sound_mode_enabled", false)) }

    DisposableEffect(Unit) {
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { p, key ->
            if (key == "phone_watch_sync_sound_mode_enabled") {
                isSyncEnabled.value = p.getBoolean(key, false)
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose {
            prefs.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }

    // Refresh permissions when entering/returning
    LaunchedEffect(Unit) {
        while (true) {
            hasDndPermission.value = notificationManager.isNotificationPolicyAccessGranted
            hasWriteSecureSettings.value =
                context.checkCallingOrSelfPermission(android.Manifest.permission.WRITE_SECURE_SETTINGS) ==
                        android.content.pm.PackageManager.PERMISSION_GRANTED
            kotlinx.coroutines.delay(1000)
        }
    }

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
                text = stringResource(R.string.feature_settings),
                color = lightAccentColor
            )
        }

        // Sync Sound Mode Toggle
        item {
            ToggleChip(
                checked = isSyncEnabled.value,
                onCheckedChange = { checked ->
                    HapticUtil.performUIHaptic(view)
                    isSyncEnabled.value = checked
                    prefs.edit().putBoolean("phone_watch_sync_sound_mode_enabled", checked).apply()
                    // Send to phone
                    val nodeClient = Wearable.getNodeClient(context)
                    nodeClient.connectedNodes.addOnSuccessListener { nodes ->
                        val messageClient = Wearable.getMessageClient(context)
                        for (node in nodes) {
                            messageClient.sendMessage(
                                node.id, 
                                "/set_sync_sound_mode", 
                                byteArrayOf(if (checked) 1 else 0)
                            )
                        }
                    }
                },
                label = { Text("Sync Sound Mode", maxLines = 1) },
                appIcon = {
                    Icon(
                        painter = painterResource(id = R.drawable.rounded_volume_up_24),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                },
                toggleControl = {
                    Switch(
                        checked = isSyncEnabled.value,
                        enabled = true
                    )
                },
                colors = ToggleChipDefaults.toggleChipColors(
                    checkedStartBackgroundColor = tonedThemeColor,
                    checkedEndBackgroundColor = tonedThemeColor,
                    uncheckedStartBackgroundColor = tonedThemeColor.copy(alpha = 0.5f),
                    uncheckedEndBackgroundColor = tonedThemeColor.copy(alpha = 0.5f)
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }

        // DND Permission Chip
        item {
            val granted = hasDndPermission.value
            EssentialsChip(
                label = stringResource(R.string.perm_dnd_access_title),
                onClick = {
                    HapticUtil.performUIHaptic(view)
                    if (!granted) {
                        try {
                            val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            // Fallback
                        }
                    }
                },
                icon = painterResource(R.drawable.rounded_volume_off_24),
                colors = ChipDefaults.secondaryChipColors(
                    backgroundColor = if (granted) tonedThemeColor else tonedThemeColor.copy(alpha = 0.5f),
                    contentColor = if (granted) Color.White else Color.White.copy(alpha = 0.6f),
                    secondaryContentColor = if (granted) lightAccentColor else Color.White.copy(alpha = 0.4f)
                ),
                border = ChipDefaults.chipBorder(
                    borderStroke = null
                )
            )
        }

        // Write Secure Settings Permission Chip
        item {
            val granted = hasWriteSecureSettings.value
            EssentialsChip(
                label = stringResource(R.string.perm_write_secure_title),
                onClick = {
                    HapticUtil.performUIHaptic(view)
                    if (!granted) {
                        android.widget.Toast.makeText(context, "Grant manually via ADB", android.widget.Toast.LENGTH_LONG).show()
                    }
                },
                icon = painterResource(R.drawable.rounded_lock_24),
                colors = ChipDefaults.secondaryChipColors(
                    backgroundColor = if (granted) tonedThemeColor else tonedThemeColor.copy(alpha = 0.5f),
                    contentColor = if (granted) Color.White else Color.White.copy(alpha = 0.6f),
                    secondaryContentColor = if (granted) lightAccentColor else Color.White.copy(alpha = 0.4f)
                ),
                border = ChipDefaults.chipBorder(
                    borderStroke = null
                )
            )
        }
    }
}

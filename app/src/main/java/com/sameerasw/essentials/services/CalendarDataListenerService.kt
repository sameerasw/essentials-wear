package com.sameerasw.essentials.services

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.wear.tiles.TileService
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.provider.Settings
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.WearableListenerService
import com.google.gson.Gson
import com.sameerasw.essentials.R
import com.sameerasw.essentials.tile.MainTileService
import com.sameerasw.essentials.tile.PhoneBatteryTileService
import com.sameerasw.essentials.utils.HapticUtil
import com.sameerasw.essentials.utils.SoundUtil
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceUpdateRequester

class CalendarDataListenerService : WearableListenerService() {
    companion object {
        private const val TAG = "CalendarDataListener"
        private const val SYNC_PATH = "/calendar_events"
        private const val DEVICE_INFO_PATH = "/device_info"
    }

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        Log.d(TAG, "onDataChanged: ${dataEvents.count}")
        for (event in dataEvents) {
            if (event.type == DataEvent.TYPE_CHANGED && event.dataItem.uri.path == SYNC_PATH) {
                Log.d(TAG, "Received calendar data update")
                val dataMap = DataMapItem.fromDataItem(event.dataItem).dataMap
                val eventList = dataMap.getDataMapArrayList("events")
                if (eventList != null) {
                    val primaryColor = dataMap.getInt("theme_primary_color", -1)
                    val secondaryColor = dataMap.getInt("theme_secondary_color", -1)
                    val tertiaryColor = dataMap.getInt("theme_tertiary_color", -1)

                    saveData(
                        eventList.map { it.toBundle() },
                        if (primaryColor != -1) primaryColor else null,
                        if (secondaryColor != -1) secondaryColor else null,
                        if (tertiaryColor != -1) tertiaryColor else null
                    )
                    Log.d(
                        TAG,
                        "Saved ${eventList.size} events and colors: P=$primaryColor, S=$secondaryColor, T=$tertiaryColor"
                    )

                    // Trigger Tile Update
                    TileService.getUpdater(this).apply {
                        requestUpdate(MainTileService::class.java)
                        requestUpdate(PhoneBatteryTileService::class.java)
                    }

                    // Trigger Complication Update
                    val componentName = ComponentName(
                        this,
                        "com.sameerasw.essentials.complication.MainComplicationService"
                    )
                    ComplicationDataSourceUpdateRequester
                        .create(this, componentName)
                        .requestUpdateAll()

                    // Trigger Watch Face Redraw
                    sendBroadcast(Intent("com.sameerasw.essentials.WATCHFACE_PREFS_CHANGED").apply {
                        setPackage(packageName)
                    })
                }
            } else if (event.type == DataEvent.TYPE_CHANGED && event.dataItem.uri.path == DEVICE_INFO_PATH) {
                Log.d(TAG, "Received device info update")
                val dataMap = DataMapItem.fromDataItem(event.dataItem).dataMap
                val batteryLevel = dataMap.getInt("battery_level", -1)
                val isCharging = dataMap.getBoolean("is_charging", false)
                val flashlightOn = dataMap.getBoolean("flashlight_on", false)
                val flashlightLevel = dataMap.getInt("flashlight_level", 1)
                val flashlightMaxLevel = dataMap.getInt("flashlight_max_level", 1)
                val flashlightIntensitySupported = dataMap.getBoolean("flashlight_intensity_supported", false)
                val ringerMode = dataMap.getInt("ringer_mode", 2)
                val deviceName = dataMap.getString("device_name", "")

                val travelActive = dataMap.getBoolean("travel_active", false)
                val travelName = dataMap.getString("travel_name", "")
                val travelProgress = dataMap.getFloat("travel_progress", 0f)
                val travelRemainingTime = dataMap.getString("travel_remaining_time", "")
                val travelRemainingDistance = dataMap.getString("travel_remaining_distance", "")
                val travelIconName = dataMap.getString("travel_icon_name", "")
                val travelIsPaused = dataMap.getBoolean("travel_is_paused", false)
                val flashlightPulseEnabled = dataMap.getBoolean("flashlight_pulse_enabled", false)
                val aodState = dataMap.getInt("aod_state", 0)
                val watchControlsLayout = dataMap.getString("watch_controls_layout", "")
                val tapToWakeEnabled = dataMap.getBoolean("tap_to_wake_enabled", true)
                val watchSyncSoundModeEnabled = dataMap.getBoolean("watch_sync_sound_mode_enabled", false)

                if (watchSyncSoundModeEnabled) {
                    syncWatchRingerMode(ringerMode)
                }

                val watchfaceHideBattery = dataMap.getBoolean("watchface_hide_battery", false)
                val watchfaceHideDeviceIcons = dataMap.getBoolean("watchface_hide_device_icons", false)
                val watchfaceShowComplications = dataMap.getBoolean("watchface_show_complications", true)
                val watchfaceComplicationsOnAod = dataMap.getBoolean("watchface_complications_on_aod", true)
                val watchfaceComplicationOutline = dataMap.getBoolean("watchface_complication_outline", true)
                val watchfaceLeftComplication = dataMap.getString("watchface_left_complication", "HEART_RATE")
                val watchfaceRightComplication = dataMap.getString("watchface_right_complication", "STEPS")
                val watchfaceShowUpcomingEvents = dataMap.getBoolean("watchface_show_upcoming_events", true)
                val watchfaceShowGlance = dataMap.getBoolean("watchface_show_glance", watchfaceShowUpcomingEvents)
                val watchfaceGlanceBatteryAlerts = dataMap.getBoolean("watchface_glance_battery_alerts", true)
                val watchfaceGlanceTravel = dataMap.getBoolean("watchface_glance_travel", true)
                val watchfaceGlanceEvents = dataMap.getBoolean("watchface_glance_events", true)
                val watchfaceGlanceAlarm = dataMap.getBoolean("watchface_glance_alarm", true)
                val watchfaceShowGlow = dataMap.getBoolean("watchface_show_glow", true)

                val watchfaceClockFont = dataMap.getString("watchface_clock_font", "FLEX") ?: "FLEX"
                val watchfaceClockFontSize = dataMap.getInt("watchface_clock_font_size", 100)
                val watchfaceClockDualTone = dataMap.getBoolean("watchface_clock_dual_tone", false)
                val watchfaceClockOutline = dataMap.getBoolean("watchface_clock_outline", false)
                val watchfaceClockOutlineThickness = dataMap.getInt("watchface_clock_outline_thickness", 2)

                saveDeviceInfo(
                    batteryLevel, 
                    isCharging, 
                    flashlightOn, 
                    flashlightLevel, 
                    flashlightMaxLevel, 
                    flashlightIntensitySupported,
                    ringerMode,
                    deviceName,
                    travelActive,
                    travelName,
                    travelProgress,
                    travelRemainingTime,
                    travelRemainingDistance,
                    travelIconName,
                    travelIsPaused,
                    flashlightPulseEnabled,
                    aodState,
                    watchControlsLayout,
                    tapToWakeEnabled,
                    watchSyncSoundModeEnabled,
                    watchfaceHideBattery,
                    watchfaceHideDeviceIcons,
                    watchfaceShowComplications,
                    watchfaceComplicationsOnAod,
                    watchfaceComplicationOutline,
                    watchfaceLeftComplication,
                    watchfaceRightComplication,
                    watchfaceShowUpcomingEvents,
                    watchfaceShowGlow,
                    watchfaceShowGlance,
                    watchfaceGlanceBatteryAlerts,
                    watchfaceGlanceTravel,
                    watchfaceGlanceEvents,
                    watchfaceGlanceAlarm,
                    watchfaceClockFont,
                    watchfaceClockFontSize,
                    watchfaceClockDualTone,
                    watchfaceClockOutline,
                    watchfaceClockOutlineThickness
                )
                Log.d(TAG, "Saved device info: Level=$batteryLevel, Charging=$isCharging, TravelActive=$travelActive, TravelName=$travelName")

                // Trigger Battery Complication Update
                val batteryCompName = ComponentName(
                    this,
                    "com.sameerasw.essentials.complication.BatteryComplicationService"
                )
                ComplicationDataSourceUpdateRequester
                    .create(this, batteryCompName)
                    .requestUpdateAll()

                // Trigger Phone Battery Tile Update
                TileService.getUpdater(this)
                    .requestUpdate(PhoneBatteryTileService::class.java)

                // Trigger Watch Face Redraw
                sendBroadcast(Intent("com.sameerasw.essentials.WATCHFACE_PREFS_CHANGED").apply {
                    setPackage(packageName)
                })
            }
        }
    }

    private fun saveDeviceInfo(
        batteryLevel: Int, 
        isCharging: Boolean,
        flashlightOn: Boolean,
        flashlightLevel: Int,
        flashlightMaxLevel: Int,
        flashlightIntensitySupported: Boolean,
        ringerMode: Int,
        deviceName: String,
        travelActive: Boolean,
        travelName: String,
        travelProgress: Float,
        travelRemainingTime: String,
        travelRemainingDistance: String,
        travelIconName: String,
        travelIsPaused: Boolean,
        flashlightPulseEnabled: Boolean,
        aodState: Int,
        watchControlsLayout: String,
        tapToWakeEnabled: Boolean,
        watchSyncSoundModeEnabled: Boolean,
        watchfaceHideBattery: Boolean = false,
        watchfaceHideDeviceIcons: Boolean = false,
        watchfaceShowComplications: Boolean = true,
        watchfaceComplicationsOnAod: Boolean = true,
        watchfaceComplicationOutline: Boolean = true,
        watchfaceLeftComplication: String = "HEART_RATE",
        watchfaceRightComplication: String = "STEPS",
        watchfaceShowUpcomingEvents: Boolean = true,
        watchfaceShowGlow: Boolean = true,
        watchfaceShowGlance: Boolean = true,
        watchfaceGlanceBatteryAlerts: Boolean = true,
        watchfaceGlanceTravel: Boolean = true,
        watchfaceGlanceEvents: Boolean = true,
        watchfaceGlanceAlarm: Boolean = true,
        watchfaceClockFont: String = "FLEX",
        watchfaceClockFontSize: Int = 100,
        watchfaceClockDualTone: Boolean = false,
        watchfaceClockOutline: Boolean = false,
        watchfaceClockOutlineThickness: Int = 2
    ) {
        val prefs = getSharedPreferences("schedule_prefs", MODE_PRIVATE)
        prefs.edit()
            .putInt("phone_battery_level", batteryLevel)
            .putBoolean("phone_is_charging", isCharging)
            .putBoolean("phone_flashlight_on", flashlightOn)
            .putInt("phone_flashlight_level", flashlightLevel)
            .putInt("phone_flashlight_max_level", flashlightMaxLevel)
            .putBoolean("phone_flashlight_intensity_supported", flashlightIntensitySupported)
            .putInt("phone_ringer_mode", ringerMode)
            .putString("phone_device_name", deviceName)
            .putBoolean("phone_travel_active", travelActive)
            .putString("phone_travel_name", travelName)
            .putFloat("phone_travel_progress", travelProgress)
            .putString("phone_travel_remaining_time", travelRemainingTime)
            .putString("phone_travel_remaining_distance", travelRemainingDistance)
            .putString("phone_travel_icon_name", travelIconName)
            .putBoolean("phone_travel_is_paused", travelIsPaused)
            .putBoolean("phone_flashlight_pulse_enabled", flashlightPulseEnabled)
            .putInt("phone_aod_state", aodState)
            .putString("phone_watch_controls_layout", watchControlsLayout)
            .putBoolean("phone_tap_to_wake_enabled", tapToWakeEnabled)
            .putBoolean("phone_watch_sync_sound_mode_enabled", watchSyncSoundModeEnabled)
            .putBoolean("watchface_hide_battery", watchfaceHideBattery)
            .putBoolean("watchface_hide_device_icons", watchfaceHideDeviceIcons)
            .putBoolean("watchface_show_complications", watchfaceShowComplications)
            .putBoolean("watchface_complications_on_aod", watchfaceComplicationsOnAod)
            .putBoolean("watchface_complication_outline", watchfaceComplicationOutline)
            .putString("watchface_left_complication", watchfaceLeftComplication)
            .putString("watchface_right_complication", watchfaceRightComplication)
            .putBoolean("watchface_show_upcoming_events", watchfaceShowUpcomingEvents)
            .putBoolean("watchface_show_glance", watchfaceShowGlance)
            .putBoolean("watchface_glance_battery_alerts", watchfaceGlanceBatteryAlerts)
            .putBoolean("watchface_glance_travel", watchfaceGlanceTravel)
            .putBoolean("watchface_glance_events", watchfaceGlanceEvents)
            .putBoolean("watchface_glance_alarm", watchfaceGlanceAlarm)
            .putBoolean("watchface_show_glow", watchfaceShowGlow)
            .putString("watchface_clock_font", watchfaceClockFont)
            .putInt("watchface_clock_font_size", watchfaceClockFontSize)
            .putBoolean("watchface_clock_dual_tone", watchfaceClockDualTone)
            .putBoolean("watchface_clock_outline", watchfaceClockOutline)
            .putInt("watchface_clock_outline_thickness", watchfaceClockOutlineThickness)
            .putLong("phone_battery_timestamp", System.currentTimeMillis())
            .apply()
    }

    private fun syncWatchRingerMode(phoneRingerMode: Int) {
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as? android.media.AudioManager ?: return

        val targetMode = when (phoneRingerMode) {
            2 -> android.media.AudioManager.RINGER_MODE_NORMAL
            1, 0 -> android.media.AudioManager.RINGER_MODE_VIBRATE
            else -> android.media.AudioManager.RINGER_MODE_NORMAL
        }

        if (audioManager.ringerMode != targetMode) {
            try {
                audioManager.ringerMode = targetMode
            } catch (e: Exception) {
                Log.e(TAG, "Failed to set watch ringer mode", e)
            }
        }
    }

    private fun saveData(
        events: List<android.os.Bundle>,
        primaryColor: Int?,
        secondaryColor: Int?,
        tertiaryColor: Int?
    ) {
        val prefs = getSharedPreferences("schedule_prefs", MODE_PRIVATE)
        val json = Gson().toJson(events.map { bundle ->
            mapOf(
                "id" to bundle.getLong("id"),
                "title" to bundle.getString("title"),
                "begin" to bundle.getLong("begin"),
                "end" to bundle.getLong("end"),
                "allDay" to bundle.getBoolean("allDay"),
                "location" to bundle.getString("location")
            )
        })
        val editor = prefs.edit()
        editor.putString("synced_calendar_events", json)
        primaryColor?.let { editor.putInt("theme_primary_color", it) }
        secondaryColor?.let { editor.putInt("theme_secondary_color", it) }
        tertiaryColor?.let { editor.putInt("theme_tertiary_color", it) }
        editor.apply()
    }

    override fun onMessageReceived(messageEvent: com.google.android.gms.wearable.MessageEvent) {
        super.onMessageReceived(messageEvent)
        if (messageEvent.path == "/toggle_watch_adb_wifi") {
            val hasPermission = checkCallingOrSelfPermission(android.Manifest.permission.WRITE_SECURE_SETTINGS) == android.content.pm.PackageManager.PERMISSION_GRANTED
            if (hasPermission) {
                val isAdbWifiEnabled = android.provider.Settings.Global.getInt(contentResolver, "adb_wifi_enabled", 0) == 1
                val newValue = if (isAdbWifiEnabled) 0 else 1
                try {
                    android.provider.Settings.Global.putInt(contentResolver, "adb_wifi_enabled", newValue)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to write adb_wifi_enabled setting", e)
                }
            }
            sendStatusUpdateToPhone(this)
        } else if (messageEvent.path == "/request_watch_status") {
            sendStatusUpdateToPhone(this)
        } else if (messageEvent.path == "/set_notification_sound") {
            val soundName = String(messageEvent.data ?: byteArrayOf())
            Log.d(TAG, "Received set notification sound message: $soundName")
            if (soundName.isNotBlank()) {
                val prefs = getSharedPreferences("schedule_prefs", MODE_PRIVATE)
                prefs.edit().putString("selected_notification_sound", soundName).apply()
                sendBroadcast(Intent("com.sameerasw.essentials.NOTIFICATIONS_UPDATED").apply {
                    setPackage(packageName)
                })
            }
        } else if (messageEvent.path == "/watch_notification" || messageEvent.path == "/notification_sync") {
            val jsonStr = String(messageEvent.data ?: byteArrayOf())
            Log.d(TAG, "Received watch notification message: $jsonStr")
            if (jsonStr.isNotBlank()) {
                handleNotificationPosted(jsonStr)
            }
        } else if (messageEvent.path == "/watch_notification_removed") {
            val key = String(messageEvent.data ?: byteArrayOf())
            Log.d(TAG, "Received watch notification removed: $key")
            if (key.isNotBlank()) {
                handleNotificationRemoved(key)
            }
        } else if (messageEvent.path == "/watch_app_icons") {
            val jsonStr = String(messageEvent.data ?: byteArrayOf())
            Log.d(TAG, "Received watch app icons message")
            if (jsonStr.isNotBlank()) {
                handleAppIconsReceived(jsonStr)
            }
        } else if (messageEvent.path == "/watch_active_notifications_sync") {
            val jsonStr = String(messageEvent.data ?: byteArrayOf())
            Log.d(TAG, "Received watch active notifications sync message")
            if (jsonStr.isNotBlank()) {
                handleActiveNotificationsSync(jsonStr)
            }
        } else if (messageEvent.path == "/watch_call_state") {
            val jsonStr = String(messageEvent.data ?: byteArrayOf())
            Log.d(TAG, "Received watch call state message: $jsonStr")
            if (jsonStr.isNotBlank()) {
                handleCallStateReceived(jsonStr)
            }
        }
    }

    private fun handleCallStateReceived(jsonStr: String) {
        try {
            val obj = org.json.JSONObject(jsonStr)
            val state = obj.optString("state", "IDLE")
            val number = obj.optString("number", "")
            val name = obj.optString("contactName", "")
            val photo = obj.optString("contactPhoto", "")
            val isIncoming = obj.optBoolean("isIncoming", false)
            val timestamp = obj.optLong("timestamp", System.currentTimeMillis())

            val prefs = getSharedPreferences("schedule_prefs", MODE_PRIVATE)
            prefs.edit().putString("watch_call_state_json", jsonStr).apply()

            if (state == "RINGING" || state == "OFFHOOK") {
                val overlayIntent = Intent("com.sameerasw.essentials.SHOW_CALL_OVERLAY").apply {
                    setPackage(packageName)
                    putExtra("state", state)
                    putExtra("number", number)
                    putExtra("contactName", name)
                    putExtra("contactPhoto", photo)
                    putExtra("isIncoming", isIncoming)
                    putExtra("timestamp", timestamp)
                }
                sendBroadcast(overlayIntent)
            } else {
                val hideIntent = Intent("com.sameerasw.essentials.HIDE_CALL_OVERLAY").apply {
                    setPackage(packageName)
                }
                sendBroadcast(hideIntent)
            }

            val updateIntent = Intent("com.sameerasw.essentials.CALL_STATE_UPDATED").apply {
                setPackage(packageName)
            }
            sendBroadcast(updateIntent)
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing call state JSON", e)
        }
    }

    private fun handleActiveNotificationsSync(activeArrayJson: String) {
        try {
            val activeArray = org.json.JSONArray(activeArrayJson)
            val prefs = getSharedPreferences("schedule_prefs", MODE_PRIVATE)
            prefs.edit().putString("watch_notifications_json", activeArray.toString()).apply()
            val intent = Intent("com.sameerasw.essentials.NOTIFICATIONS_UPDATED").apply {
                setPackage(packageName)
            }
            sendBroadcast(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error saving active notifications sync", e)
        }
    }

    private fun handleNotificationPosted(newNotifJson: String) {
        try {
            val prefs = getSharedPreferences("schedule_prefs", MODE_PRIVATE)
            val existingJson = prefs.getString("watch_notifications_json", "[]") ?: "[]"
            val jsonArray = org.json.JSONArray(existingJson)
            val newObj = org.json.JSONObject(newNotifJson)
            val newKey = newObj.optString("key")

            val updatedArray = org.json.JSONArray()
            updatedArray.put(newObj) // Insert new notification at top

            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                if (obj.optString("key") != newKey && updatedArray.length() < 30) {
                    updatedArray.put(obj)
                }
            }

            prefs.edit().putString("watch_notifications_json", updatedArray.toString()).apply()
            
            val isMedia = newObj.optBoolean("isMedia", false)
            if (!isMedia) {
                SoundUtil.playNotificationSound(this)
                HapticUtil.performStrongDoubleTap(this)

                val overlayEnabled = prefs.getBoolean("prefs_notification_overlay_enabled", true)
                if (overlayEnabled) {
                    if (isDefaultLauncher()) {
                        Log.d(TAG, "isDefaultLauncher: true - sending broadcast")
                        val intent = Intent("com.sameerasw.essentials.NOTIFICATIONS_UPDATED").apply {
                            setPackage(packageName)
                            putExtra("new_notification_json", newNotifJson)
                        }
                        sendBroadcast(intent)
                    } else {
                        Log.d(TAG, "isDefaultLauncher: false - showing accessibility overlay")
                        autoEnableAccessibilityService()
                        val intent = Intent("com.sameerasw.essentials.SHOW_OVERLAY").apply {
                            setPackage(packageName)
                            putExtra("notification_json", newNotifJson)
                        }
                        sendBroadcast(intent)
                    }
                } else {
                    // Even if overlay is disabled, we still need to refresh the notification list if launcher is active
                    if (isDefaultLauncher()) {
                        val intent = Intent("com.sameerasw.essentials.NOTIFICATIONS_UPDATED").apply {
                            setPackage(packageName)
                        }
                        sendBroadcast(intent)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error saving synced watch notification", e)
        }
    }

    private fun handleNotificationRemoved(keyToRemove: String) {
        try {
            val prefs = getSharedPreferences("schedule_prefs", MODE_PRIVATE)
            val existingJson = prefs.getString("watch_notifications_json", "[]") ?: "[]"
            val jsonArray = org.json.JSONArray(existingJson)
            val updatedArray = org.json.JSONArray()

            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                if (obj.optString("key") != keyToRemove) {
                    updatedArray.put(obj)
                }
            }

            prefs.edit().putString("watch_notifications_json", updatedArray.toString()).apply()
            val intent = Intent("com.sameerasw.essentials.NOTIFICATIONS_UPDATED").apply {
                setPackage(packageName)
            }
            sendBroadcast(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error removing synced watch notification", e)
        }
    }

    private fun handleAppIconsReceived(jsonStr: String) {
        try {
            val iconsObj = org.json.JSONObject(jsonStr)
            val prefs = getSharedPreferences("schedule_prefs", MODE_PRIVATE)
            val existingIconsJson = prefs.getString("watch_app_icons_json", "{}") ?: "{}"
            val existingObj = org.json.JSONObject(existingIconsJson)

            val keys = iconsObj.keys()
            while (keys.hasNext()) {
                val pkg = keys.next()
                existingObj.put(pkg, iconsObj.getString(pkg))
            }

            prefs.edit().putString("watch_app_icons_json", existingObj.toString()).apply()
            Log.d(TAG, "Saved synced app icons to schedule_prefs")
            val intent = Intent("com.sameerasw.essentials.NOTIFICATIONS_UPDATED").apply {
                setPackage(packageName)
            }
            sendBroadcast(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error saving synced app icons", e)
        }
    }

    private fun sendStatusUpdateToPhone(context: Context) {
        val hasPermission = context.checkCallingOrSelfPermission(android.Manifest.permission.WRITE_SECURE_SETTINGS) == android.content.pm.PackageManager.PERMISSION_GRANTED
        val isAdbWifiEnabled = android.provider.Settings.Global.getInt(context.contentResolver, "adb_wifi_enabled", 0) == 1
        
        val data = byteArrayOf(
            if (isAdbWifiEnabled) 1 else 0,
            if (hasPermission) 1 else 0,
            com.sameerasw.essentials.BuildConfig.VERSION_CODE.toByte()
        )
        
        val nodeClient = com.google.android.gms.wearable.Wearable.getNodeClient(context)
        nodeClient.connectedNodes.addOnSuccessListener { nodes ->
            val messageClient = com.google.android.gms.wearable.Wearable.getMessageClient(context)
            for (node in nodes) {
                messageClient.sendMessage(node.id, "/watch_status_update", data)
            }
        }
    }

    private fun isDefaultLauncher(): Boolean {
        val intent = Intent(Intent.ACTION_MAIN)
        intent.addCategory(Intent.CATEGORY_HOME)
        val resolveInfo = packageManager.resolveActivity(intent, android.content.pm.PackageManager.MATCH_DEFAULT_ONLY)
        val isDefault = resolveInfo?.activityInfo?.packageName == packageName
        Log.d(TAG, "isDefaultLauncher check: $isDefault (Default pkg: ${resolveInfo?.activityInfo?.packageName})")
        return isDefault
    }

    private fun autoEnableAccessibilityService() {
        val serviceName = "$packageName/${OverlayAccessibilityService::class.java.name}"
        try {
            val enabledServices = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: ""
            if (!enabledServices.contains(serviceName)) {
                val newEnabledServices = if (enabledServices.isEmpty()) serviceName else "$enabledServices:$serviceName"
                Settings.Secure.putString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES, newEnabledServices)
                Settings.Secure.putInt(contentResolver, Settings.Secure.ACCESSIBILITY_ENABLED, 1)
                Log.d(TAG, "Successfully enabled accessibility service: $serviceName")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to enable accessibility service", e)
        }
    }

    data class CalendarEvent(
        val id: Long,
        val title: String?,
        val begin: Long,
        val end: Long,
        val allDay: Boolean,
        val location: String?
    )
}

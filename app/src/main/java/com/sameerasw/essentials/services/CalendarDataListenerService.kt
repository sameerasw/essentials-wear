package com.sameerasw.essentials.services

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.wear.tiles.TileService
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.WearableListenerService
import com.google.gson.Gson
import com.sameerasw.essentials.tile.MainTileService
import com.sameerasw.essentials.tile.PhoneBatteryTileService

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
                    val componentName = android.content.ComponentName(
                        this,
                        "com.sameerasw.essentials.complication.MainComplicationService"
                    )
                    androidx.wear.watchface.complications.datasource.ComplicationDataSourceUpdateRequester
                        .create(this, componentName)
                        .requestUpdateAll()
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
                val travelName = dataMap.getString("travel_name", "") ?: ""
                val travelProgress = dataMap.getFloat("travel_progress", 0f)
                val travelRemainingTime = dataMap.getString("travel_remaining_time", "") ?: ""
                val travelRemainingDistance = dataMap.getString("travel_remaining_distance", "") ?: ""
                val travelIconName = dataMap.getString("travel_icon_name", "") ?: ""
                val travelIsPaused = dataMap.getBoolean("travel_is_paused", false)
                val flashlightPulseEnabled = dataMap.getBoolean("flashlight_pulse_enabled", false)
                val aodState = dataMap.getInt("aod_state", 0)
                val watchControlsLayout = dataMap.getString("watch_controls_layout", "") ?: ""
                val tapToWakeEnabled = dataMap.getBoolean("tap_to_wake_enabled", true)
                val watchSyncSoundModeEnabled = dataMap.getBoolean("watch_sync_sound_mode_enabled", false)

                if (watchSyncSoundModeEnabled) {
                    syncWatchRingerMode(ringerMode)
                }

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
                    watchSyncSoundModeEnabled
                )
                Log.d(TAG, "Saved device info: Level=$batteryLevel, Charging=$isCharging, TravelActive=$travelActive, TravelName=$travelName")

                // Trigger Battery Complication Update
                val batteryCompName = android.content.ComponentName(
                    this,
                    "com.sameerasw.essentials.complication.BatteryComplicationService"
                )
                androidx.wear.watchface.complications.datasource.ComplicationDataSourceUpdateRequester
                    .create(this, batteryCompName)
                    .requestUpdateAll()

                // Trigger Phone Battery Tile Update
                TileService.getUpdater(this)
                    .requestUpdate(PhoneBatteryTileService::class.java)
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
        watchSyncSoundModeEnabled: Boolean
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
        }
    }

    private fun sendStatusUpdateToPhone(context: Context) {
        val hasPermission = context.checkCallingOrSelfPermission(android.Manifest.permission.WRITE_SECURE_SETTINGS) == android.content.pm.PackageManager.PERMISSION_GRANTED
        val isAdbWifiEnabled = android.provider.Settings.Global.getInt(context.contentResolver, "adb_wifi_enabled", 0) == 1
        
        val data = byteArrayOf(
            if (isAdbWifiEnabled) 1 else 0,
            if (hasPermission) 1 else 0
        )
        
        val nodeClient = com.google.android.gms.wearable.Wearable.getNodeClient(context)
        nodeClient.connectedNodes.addOnSuccessListener { nodes ->
            val messageClient = com.google.android.gms.wearable.Wearable.getMessageClient(context)
            for (node in nodes) {
                messageClient.sendMessage(node.id, "/watch_status_update", data)
            }
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

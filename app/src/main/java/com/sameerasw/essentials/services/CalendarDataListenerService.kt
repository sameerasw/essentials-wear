package com.sameerasw.essentials.services

import android.content.Context
import android.util.Log
import androidx.wear.tiles.TileService
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.WearableListenerService
import com.google.gson.Gson
import com.sameerasw.essentials.tile.MainTileService

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
                    TileService.getUpdater(this)
                        .requestUpdate(MainTileService::class.java)

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

                if (batteryLevel != -1) {
                    val flashlightOn = dataMap.getBoolean("flashlight_on", false)
                    val flashlightLevel = dataMap.getInt("flashlight_level", 1)
                    val flashlightMaxLevel = dataMap.getInt("flashlight_max_level", 1)
                    val flashlightIntensitySupported = dataMap.getBoolean("flashlight_intensity_supported", false)
                    val ringerMode = dataMap.getInt("ringer_mode", 2)
                    val deviceName = dataMap.getString("device_name", "")

                    saveDeviceInfo(
                        batteryLevel, 
                        isCharging, 
                        flashlightOn, 
                        flashlightLevel, 
                        flashlightMaxLevel, 
                        flashlightIntensitySupported,
                        ringerMode,
                        deviceName
                    )
                    Log.d(TAG, "Saved device info: Level=$batteryLevel, Charging=$isCharging, Flashlight=$flashlightOn, RingerMode=$ringerMode, Device=$deviceName")
                }
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
        deviceName: String
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
            .putLong("phone_battery_timestamp", System.currentTimeMillis())
            .apply()
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

    data class CalendarEvent(
        val id: Long,
        val title: String?,
        val begin: Long,
        val end: Long,
        val allDay: Boolean,
        val location: String?
    )
}

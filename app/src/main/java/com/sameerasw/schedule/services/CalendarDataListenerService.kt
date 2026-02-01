package com.sameerasw.schedule.services

import android.content.Context
import android.util.Log
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.WearableListenerService
import androidx.wear.tiles.TileService
import com.sameerasw.schedule.tile.MainTileService

class CalendarDataListenerService : WearableListenerService() {
    private const val TAG = "CalendarDataListener"
    private const val SYNC_PATH = "/calendar_events"

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        Log.d(TAG, "onDataChanged: ${dataEvents.count}")
        for (event in dataEvents) {
            if (event.type == DataEvent.TYPE_CHANGED && event.dataItem.uri.path == SYNC_PATH) {
                val dataMap = DataMapItem.fromDataItem(event.dataItem).dataMap
                val eventList = dataMap.getDataMapArrayList("events")
                
                if (eventList != null) {
                    saveEvents(eventList.map { it.toBundle() })
                    Log.d(TAG, "Saved ${eventList.size} events")
                    
                    // Trigger Tile Update
                    TileService.getUpdater(this)
                        .requestUpdate(MainTileService::class.java)
                }
            }
        }
    }

    private fun saveEvents(events: List<android.os.Bundle>) {
        val prefs = getSharedPreferences("schedule_prefs", Context.MODE_PRIVATE)
        val json = com.google.gson.Gson().toJson(events.map { bundle ->
            mapOf(
                "id" to bundle.getLong("id"),
                "title" to bundle.getString("title"),
                "begin" to bundle.getLong("begin"),
                "end" to bundle.getLong("end"),
                "all_day" to bundle.getBoolean("all_day"),
                "location" to bundle.getString("location")
            )
        })
        prefs.edit().putString("synced_calendar_events", json).apply()
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

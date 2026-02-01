package com.sameerasw.schedule.tile

import android.content.Context
import androidx.wear.protolayout.ColorBuilders.argb
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.ResourceBuilders
import androidx.wear.protolayout.TimelineBuilders
import androidx.wear.protolayout.material.Colors
import androidx.wear.protolayout.material.Text
import androidx.wear.protolayout.material.Typography
import androidx.wear.protolayout.material.layouts.PrimaryLayout
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.TileBuilders
import androidx.wear.tiles.tooling.preview.Preview
import androidx.wear.tiles.tooling.preview.TilePreviewData
import androidx.wear.tooling.preview.devices.WearDevices
import com.google.android.horologist.annotations.ExperimentalHorologistApi
import com.google.android.horologist.tiles.SuspendingTileService

private const val RESOURCES_VERSION = "0"

/**
 * Skeleton for a tile with no images.
 */
@OptIn(ExperimentalHorologistApi::class)
class MainTileService : SuspendingTileService() {

    override suspend fun resourcesRequest(
        requestParams: RequestBuilders.ResourcesRequest
    ) = resources(requestParams)

    override suspend fun tileRequest(
        requestParams: RequestBuilders.TileRequest
    ) = tile(requestParams, this)

    companion object {
        fun getSyncedEvents(context: Context): List<com.sameerasw.schedule.services.CalendarDataListenerService.CalendarEvent> {
            val prefs = context.getSharedPreferences("schedule_prefs", Context.MODE_PRIVATE)
            val json = prefs.getString("synced_calendar_events", null) ?: return emptyList()
            val type = object : com.google.gson.reflect.TypeToken<List<com.sameerasw.schedule.services.CalendarDataListenerService.CalendarEvent>>() {}.type
            return try {
                com.google.gson.Gson().fromJson(json, type) ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }
        }
    }
}

private fun resources(
    requestParams: RequestBuilders.ResourcesRequest
): ResourceBuilders.Resources {
    return ResourceBuilders.Resources.Builder()
        .setVersion(RESOURCES_VERSION)
        .build()
}

private fun tile(
    requestParams: RequestBuilders.TileRequest,
    context: Context,
): TileBuilders.Tile {
    val singleTileTimeline = TimelineBuilders.Timeline.Builder()
        .addTimelineEntry(
            TimelineBuilders.TimelineEntry.Builder()
                .setLayout(
                    LayoutElementBuilders.Layout.Builder()
                        .setRoot(tileLayout(requestParams, context))
                        .build()
                )
                .build()
        )
        .build()

    return TileBuilders.Tile.Builder()
        .setResourcesVersion(RESOURCES_VERSION)
        .setTileTimeline(singleTileTimeline)
        .build()
}

private fun tileLayout(
    requestParams: RequestBuilders.TileRequest,
    context: Context,
): LayoutElementBuilders.LayoutElement {
    val events = MainTileService.getSyncedEvents(context)
    
    val columnBuilder = LayoutElementBuilders.Column.Builder()
    
    if (events.isEmpty()) {
        columnBuilder.addContent(
            Text.Builder(context, "No upcoming events")
                .setColor(argb(Colors.DEFAULT.onSurface))
                .setTypography(Typography.TYPOGRAPHY_CAPTION1)
                .build()
        )
    } else {
        events.take(3).forEach { event ->
            columnBuilder.addContent(
                LayoutElementBuilders.Column.Builder()
                    .addContent(
                        Text.Builder(context, event.title ?: "No Title")
                            .setColor(argb(Colors.DEFAULT.onSurface))
                            .setTypography(Typography.TYPOGRAPHY_BODY1)
                            .build()
                    )
                    .addContent(
                        Text.Builder(context, formatTime(event.begin))
                            .setColor(argb(Colors.DEFAULT.onSurfaceVariant))
                            .setTypography(Typography.TYPOGRAPHY_CAPTION1)
                            .build()
                    )
                    .setModifiers(
                        LayoutElementBuilders.Modifiers.Builder()
                            .setPadding(LayoutElementBuilders.Padding.Builder().setBottom(androidx.wear.protolayout.DimensionBuilders.dp(4f)).build())
                            .build()
                    )
                    .build()
            )
        }
    }

    return PrimaryLayout.Builder(requestParams.deviceConfiguration)
        .setResponsiveContentInsetEnabled(true)
        .setContent(columnBuilder.build())
        .build()
}

private fun formatTime(timestamp: Long): String {
    val date = java.util.Date(timestamp)
    return java.text.SimpleDateFormat("MMM d, HH:mm", java.util.Locale.getDefault()).format(date)
}

@Preview(device = WearDevices.SMALL_ROUND)
@Preview(device = WearDevices.LARGE_ROUND)
fun tilePreview(context: Context) = TilePreviewData(::resources) {
    tile(it, context)
}
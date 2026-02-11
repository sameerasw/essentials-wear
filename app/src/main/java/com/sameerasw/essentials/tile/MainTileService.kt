package com.sameerasw.essentials.tile

import android.content.Context
import androidx.wear.protolayout.ColorBuilders.argb
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.ResourceBuilders
import androidx.wear.protolayout.TimelineBuilders
import androidx.wear.protolayout.material.Colors
import androidx.wear.protolayout.material.Text
import androidx.wear.protolayout.material.Typography
import androidx.wear.protolayout.material.layouts.PrimaryLayout
import androidx.wear.protolayout.material.layouts.EdgeContentLayout
import androidx.wear.protolayout.ModifiersBuilders
import androidx.wear.protolayout.DimensionBuilders
import androidx.wear.protolayout.material.CircularProgressIndicator
import androidx.wear.protolayout.material.ProgressIndicatorColors
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.TileBuilders
import androidx.wear.tiles.tooling.preview.Preview
import androidx.wear.tiles.tooling.preview.TilePreviewData
import androidx.wear.tooling.preview.devices.WearDevices
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import androidx.wear.protolayout.ActionBuilders
import androidx.wear.protolayout.material.Button
import androidx.wear.protolayout.material.ButtonColors
import androidx.wear.protolayout.material.CompactChip
import com.sameerasw.essentials.R
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
        fun getSyncedEvents(context: Context): List<com.sameerasw.essentials.services.CalendarDataListenerService.CalendarEvent> {
            val prefs = context.getSharedPreferences("schedule_prefs", Context.MODE_PRIVATE)
            val json = prefs.getString("synced_calendar_events", null) ?: return emptyList()
            
            val type = object : TypeToken<List<com.sameerasw.essentials.services.CalendarDataListenerService.CalendarEvent>>() {}.type
            return try {
                Gson().fromJson(json, type) ?: emptyList()
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
    val events = MainTileService.getSyncedEvents(context).filter { !it.allDay }
    val themeColor = com.sameerasw.essentials.utils.ThemeUtil.getThemeColor(context)
    val lightAccent = themeColor?.let { com.sameerasw.essentials.utils.ThemeUtil.getLightAccentColor(it) }
    val tonedColor = themeColor?.let { com.sameerasw.essentials.utils.ThemeUtil.getTonedColor(it) }
    
    val columnBuilder = LayoutElementBuilders.Column.Builder()
        .setWidth(DimensionBuilders.expand())
        .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
    
    val calendar = java.util.Calendar.getInstance()
    val totalMinutesInDay = 24 * 60
    val currentMinutes = calendar.get(java.util.Calendar.HOUR_OF_DAY) * 60 + calendar.get(java.util.Calendar.MINUTE)
    val progress = currentMinutes.toFloat() / totalMinutesInDay
    val progressDegrees = progress * 360f

    val indicatorBuilder = CircularProgressIndicator.Builder()
    indicatorBuilder.setProgress(progress)
    indicatorBuilder.setCircularProgressIndicatorColors(
        ProgressIndicatorColors(
            argb(lightAccent ?: 0xFFEEEEEE.toInt()),
            argb(0x33FFFFFF.toInt())
        )
    )
    val progressIndicator = indicatorBuilder.build()
    
    if (events.isEmpty()) {
        columnBuilder.addContent(
            Text.Builder(context, context.getString(R.string.no_events))
                .setColor(argb(Colors.DEFAULT.onSurface))
                .setTypography(Typography.TYPOGRAPHY_CAPTION1)
                .build()
        )
    } else {
        events.take(2).forEachIndexed { index, event ->
            // Two big cards
            columnBuilder.addContent(
                LayoutElementBuilders.Column.Builder()
                    .setWidth(DimensionBuilders.expand())
                    .addContent(
                        Text.Builder(context, event.title ?: "No Title")
                            .setColor(argb(lightAccent ?: Colors.DEFAULT.primary))
                            .setTypography(Typography.TYPOGRAPHY_BODY1)
                            .build()
                    )
                    .addContent(
                        Text.Builder(context, com.sameerasw.essentials.utils.ThemeUtil.getTimeCountdown(event.begin))
                            .setColor(argb(0xFFFFFFFF.toInt()))
                            .setTypography(Typography.TYPOGRAPHY_CAPTION1)
                            .build()
                    )
                    .setModifiers(
                        ModifiersBuilders.Modifiers.Builder()
                            .setPadding(ModifiersBuilders.Padding.Builder()
                                .setBottom(DimensionBuilders.dp(8f))
                                .setStart(DimensionBuilders.dp(8f))
                                .setEnd(DimensionBuilders.dp(8f))
                                .setTop(DimensionBuilders.dp(8f))
                                .build())
                            .setBackground(
                                ModifiersBuilders.Background.Builder()
                                    .setColor(argb(tonedColor ?: 0xFF333333.toInt()))
                                    .setCorner(ModifiersBuilders.Corner.Builder().setRadius(DimensionBuilders.dp(20f)).build())
                                    .build()
                            )
                            .build()
                    )
                    .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
                    .build()
            )
            
            if (index < 1) {
                columnBuilder.addContent(
                    LayoutElementBuilders.Spacer.Builder().setHeight(DimensionBuilders.dp(6f)).build()
                )
            }
        }
    }

    val openAppIntent = ActionBuilders.LaunchAction.Builder()
        .setAndroidActivity(
            ActionBuilders.AndroidActivity.Builder()
                .setPackageName(context.packageName)
                .setClassName("com.sameerasw.essentials.presentation.MainActivity")
                .build()
        )
        .build()

    val openAppClickable = ModifiersBuilders.Clickable.Builder()
        .setOnClick(openAppIntent)
        .setId("open_app")
        .build()

    val mainContent = columnBuilder.build()

    val edgeContentLayout = EdgeContentLayout.Builder(requestParams.deviceConfiguration)
        .setResponsiveContentInsetEnabled(true)
        .setPrimaryLabelTextContent(
            Text.Builder(context, context.getString(R.string.upcoming_header))
                .setColor(argb(lightAccent ?: 0xFFEEEEEE.toInt()))
                .setTypography(Typography.TYPOGRAPHY_CAPTION1)
                .build()
        )
        .setContent(mainContent)
        .setEdgeContent(progressIndicator)
        .build()

    return LayoutElementBuilders.Box.Builder()
        .setWidth(DimensionBuilders.expand())
        .setHeight(DimensionBuilders.expand())
        .addContent(edgeContentLayout)
        .setModifiers(
            ModifiersBuilders.Modifiers.Builder()
                .setClickable(openAppClickable)
                .build()
        )
        .build()
}


@Preview(device = WearDevices.SMALL_ROUND)
@Preview(device = WearDevices.LARGE_ROUND)
fun tilePreview(context: Context) = TilePreviewData(::resources) {
    tile(it, context)
}
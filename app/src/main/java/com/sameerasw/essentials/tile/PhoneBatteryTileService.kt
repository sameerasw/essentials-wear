package com.sameerasw.essentials.tile

import android.content.Context
import androidx.wear.protolayout.ActionBuilders
import androidx.wear.protolayout.ColorBuilders.argb
import androidx.wear.protolayout.DimensionBuilders
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.ModifiersBuilders
import androidx.wear.protolayout.ResourceBuilders
import androidx.wear.protolayout.TimelineBuilders
import androidx.wear.protolayout.material.CircularProgressIndicator
import androidx.wear.protolayout.material.Colors
import androidx.wear.protolayout.material.ProgressIndicatorColors
import androidx.wear.protolayout.material.Text
import androidx.wear.protolayout.material.Typography
import androidx.wear.protolayout.material.layouts.EdgeContentLayout
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.TileBuilders
import com.google.android.horologist.annotations.ExperimentalHorologistApi
import com.google.android.horologist.tiles.SuspendingTileService
import com.sameerasw.essentials.R
import com.sameerasw.essentials.presentation.MainActivity

private const val RESOURCES_VERSION = "0"
private const val ID_ICON_MOBILE = "ic_mobile"
private const val ID_ICON_BATTERY = "ic_battery"

@OptIn(ExperimentalHorologistApi::class)
class PhoneBatteryTileService : SuspendingTileService() {

    override suspend fun resourcesRequest(
        requestParams: RequestBuilders.ResourcesRequest
    ) = resources(this)

    override suspend fun tileRequest(
        requestParams: RequestBuilders.TileRequest
    ) = tile(requestParams, this)
}

private fun resources(context: Context): ResourceBuilders.Resources {
    return ResourceBuilders.Resources.Builder()
        .setVersion(RESOURCES_VERSION)
        .addIdToImageMapping(
            ID_ICON_MOBILE,
            ResourceBuilders.ImageResource.Builder()
                .setAndroidResourceByResId(
                    ResourceBuilders.AndroidImageResourceByResId.Builder()
                        .setResourceId(R.drawable.rounded_mobile_24)
                        .build()
                )
                .build()
        )
        .addIdToImageMapping(
            ID_ICON_BATTERY,
            ResourceBuilders.ImageResource.Builder()
                .setAndroidResourceByResId(
                    ResourceBuilders.AndroidImageResourceByResId.Builder()
                        .setResourceId(R.drawable.rounded_battery_android_frame_5_24)
                        .build()
                )
                .build()
        )
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
    val prefs = context.getSharedPreferences("schedule_prefs", Context.MODE_PRIVATE)
    val batteryLevel = prefs.getInt("phone_battery_level", -1)
    val deviceName = prefs.getString("phone_device_name", context.getString(R.string.your_android_title)) ?: context.getString(R.string.your_android_title)

    val themeColor = com.sameerasw.essentials.utils.ThemeUtil.getThemeColor(context)
    val lightAccent = themeColor?.let { com.sameerasw.essentials.utils.ThemeUtil.getLightAccentColor(it) }

    val progress = if (batteryLevel >= 0) batteryLevel.toFloat() / 100f else 0f

    val progressIndicator = CircularProgressIndicator.Builder()
        .setProgress(progress)
        .setCircularProgressIndicatorColors(
            ProgressIndicatorColors(
                argb(lightAccent ?: 0xFFEEEEEE.toInt()),
                argb(0x33FFFFFF.toInt())
            )
        )
        .build()

    val columnBuilder = LayoutElementBuilders.Column.Builder()
        .setWidth(DimensionBuilders.expand())
        .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)

    // Phone icon
    columnBuilder.addContent(
        LayoutElementBuilders.Image.Builder()
            .setResourceId(ID_ICON_MOBILE)
            .setWidth(DimensionBuilders.dp(32f))
            .setHeight(DimensionBuilders.dp(32f))
            .setColorFilter(
                LayoutElementBuilders.ColorFilter.Builder()
                    .setTint(argb(lightAccent ?: Colors.DEFAULT.primary))
                    .build()
            )
            .build()
    )

    // Spacer
    columnBuilder.addContent(
        LayoutElementBuilders.Spacer.Builder()
            .setHeight(DimensionBuilders.dp(4f))
            .build()
    )

    // Device Name
    columnBuilder.addContent(
        Text.Builder(context, deviceName)
            .setColor(argb(0xFFFFFFFF.toInt()))
            .setTypography(Typography.TYPOGRAPHY_CAPTION1)
            .setMaxLines(2)
            .setMultilineAlignment(LayoutElementBuilders.TEXT_ALIGN_CENTER)
            .build()
    )

    // Battery row: icon + percentage
    if (batteryLevel >= 0) {
        columnBuilder.addContent(
            LayoutElementBuilders.Row.Builder()
                .setVerticalAlignment(LayoutElementBuilders.VERTICAL_ALIGN_CENTER)
                .addContent(
                    LayoutElementBuilders.Image.Builder()
                        .setResourceId(ID_ICON_BATTERY)
                        .setWidth(DimensionBuilders.dp(16f))
                        .setHeight(DimensionBuilders.dp(16f))
                        .setColorFilter(
                            LayoutElementBuilders.ColorFilter.Builder()
                                .setTint(argb(lightAccent ?: Colors.DEFAULT.primary))
                                .build()
                        )
                        .build()
                )
                .addContent(
                    LayoutElementBuilders.Spacer.Builder()
                        .setWidth(DimensionBuilders.dp(4f))
                        .build()
                )
                .addContent(
                    Text.Builder(context, "$batteryLevel%")
                        .setColor(argb(lightAccent ?: Colors.DEFAULT.primary))
                        .setTypography(Typography.TYPOGRAPHY_CAPTION1)
                        .build()
                )
                .build()
        )
    }

    val openAppIntent = ActionBuilders.LaunchAction.Builder()
        .setAndroidActivity(
            ActionBuilders.AndroidActivity.Builder()
                .setPackageName(context.packageName)
                .setClassName("com.sameerasw.essentials.presentation.MainActivity")
                .addKeyToExtraMapping(
                    MainActivity.EXTRA_NAVIGATE_TO,
                    ActionBuilders.AndroidStringExtra.Builder()
                        .setValue(MainActivity.NAV_YOUR_ANDROID)
                        .build()
                )
                .build()
        )
        .build()

    val openAppClickable = ModifiersBuilders.Clickable.Builder()
        .setOnClick(openAppIntent)
        .setId("open_your_android")
        .build()

    val edgeContentLayout = EdgeContentLayout.Builder(requestParams.deviceConfiguration)
        .setResponsiveContentInsetEnabled(true)
        .setContent(columnBuilder.build())
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

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

private const val ID_ICON_MOBILE = "ic_mobile"
private const val ID_ICON_BATTERY_BOLT = "ic_battery_bolt"
private const val ID_ICON_BATTERY_FULL = "ic_battery_full"
private const val ID_ICON_BATTERY_5 = "ic_battery_5"
private const val ID_ICON_BATTERY_2 = "ic_battery_2"
private const val ID_ICON_BATTERY_ALERT = "ic_battery_alert"

private fun getResourcesVersion(context: Context): String {
    val prefs = context.getSharedPreferences("schedule_prefs", Context.MODE_PRIVATE)
    val travelActive = prefs.getBoolean("phone_travel_active", false)
    val travelIconName = prefs.getString("phone_travel_icon_name", "round_navigation_24") ?: "round_navigation_24"
    val themeColor = com.sameerasw.essentials.utils.ThemeUtil.getThemeColor(context) ?: 0
    return "v_${travelActive}_${travelIconName}_${themeColor}"
}

@OptIn(ExperimentalHorologistApi::class)
class PhoneBatteryTileService : SuspendingTileService() {

    override suspend fun resourcesRequest(
        requestParams: RequestBuilders.ResourcesRequest
    ) = resources(this, getResourcesVersion(this))

    override suspend fun tileRequest(
        requestParams: RequestBuilders.TileRequest
    ) = tile(requestParams, this)
}

private fun resources(context: Context, version: String): ResourceBuilders.Resources {
    return ResourceBuilders.Resources.Builder()
        .setVersion(version)
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
            ID_ICON_BATTERY_BOLT,
            ResourceBuilders.ImageResource.Builder()
                .setAndroidResourceByResId(
                    ResourceBuilders.AndroidImageResourceByResId.Builder()
                        .setResourceId(R.drawable.rounded_battery_android_frame_bolt_24)
                        .build()
                )
                .build()
        )
        .addIdToImageMapping(
            ID_ICON_BATTERY_FULL,
            ResourceBuilders.ImageResource.Builder()
                .setAndroidResourceByResId(
                    ResourceBuilders.AndroidImageResourceByResId.Builder()
                        .setResourceId(R.drawable.rounded_battery_android_frame_full_24)
                        .build()
                )
                .build()
        )
        .addIdToImageMapping(
            ID_ICON_BATTERY_5,
            ResourceBuilders.ImageResource.Builder()
                .setAndroidResourceByResId(
                    ResourceBuilders.AndroidImageResourceByResId.Builder()
                        .setResourceId(R.drawable.rounded_battery_android_frame_5_24)
                        .build()
                )
                .build()
        )
        .addIdToImageMapping(
            ID_ICON_BATTERY_2,
            ResourceBuilders.ImageResource.Builder()
                .setAndroidResourceByResId(
                    ResourceBuilders.AndroidImageResourceByResId.Builder()
                        .setResourceId(R.drawable.rounded_battery_android_frame_2_24)
                        .build()
                )
                .build()
        )
        .addIdToImageMapping(
            ID_ICON_BATTERY_ALERT,
            ResourceBuilders.ImageResource.Builder()
                .setAndroidResourceByResId(
                    ResourceBuilders.AndroidImageResourceByResId.Builder()
                        .setResourceId(R.drawable.rounded_battery_android_alert_24)
                        .build()
                )
                .build()
        )
        .apply {
            val travelIcons = listOf(
                "round_navigation_24",
                "rounded_home_24",
                "rounded_work_24",
                "rounded_apartment_24",
                "rounded_shopping_cart_24",
                "rounded_school_24",
                "rounded_storefront_24",
                "rounded_fork_spoon_24",
                "rounded_favorite_24",
                "rounded_account_balance_24",
                "rounded_garage_home_24",
                "rounded_beach_access_24",
                "rounded_local_pizza_24",
                "rounded_train_24",
                "rounded_directions_bus_24",
                "rounded_flight_24",
                "rounded_directions_boat_24"
            )
            for (iconName in travelIcons) {
                val resId = context.resources.getIdentifier(iconName, "drawable", context.packageName)
                val finalResId = if (resId != 0) resId else R.drawable.rounded_mobile_24
                addIdToImageMapping(
                    iconName,
                    ResourceBuilders.ImageResource.Builder()
                        .setAndroidResourceByResId(
                            ResourceBuilders.AndroidImageResourceByResId.Builder()
                                .setResourceId(finalResId)
                                .build()
                        )
                        .build()
                )
            }
        }
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
        .setResourcesVersion(getResourcesVersion(context))
        .setTileTimeline(singleTileTimeline)
        .build()
}

private fun tileLayout(
    requestParams: RequestBuilders.TileRequest,
    context: Context,
): LayoutElementBuilders.LayoutElement {
    val prefs = context.getSharedPreferences("schedule_prefs", Context.MODE_PRIVATE)
    val travelActive = prefs.getBoolean("phone_travel_active", false)
    val themeColor = com.sameerasw.essentials.utils.ThemeUtil.getThemeColor(context)
    val lightAccent = themeColor?.let { com.sameerasw.essentials.utils.ThemeUtil.getLightAccentColor(it) }

    val progressIndicator = if (travelActive) {
        val travelProgress = prefs.getFloat("phone_travel_progress", 0f)
        CircularProgressIndicator.Builder()
            .setProgress(travelProgress)
            .setCircularProgressIndicatorColors(
                ProgressIndicatorColors(
                    argb(lightAccent ?: 0xFFEEEEEE.toInt()),
                    argb(0x33FFFFFF.toInt())
                )
            )
            .build()
    } else {
        val batteryLevel = prefs.getInt("phone_battery_level", -1)
        val progress = if (batteryLevel >= 0) batteryLevel.toFloat() / 100f else 0f
        CircularProgressIndicator.Builder()
            .setProgress(progress)
            .setCircularProgressIndicatorColors(
                ProgressIndicatorColors(
                    argb(lightAccent ?: 0xFFEEEEEE.toInt()),
                    argb(0x33FFFFFF.toInt())
                )
            )
            .build()
    }

    val columnBuilder = LayoutElementBuilders.Column.Builder()
        .setWidth(DimensionBuilders.expand())
        .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)

    if (travelActive) {
        val travelName = prefs.getString("phone_travel_name", "") ?: ""
        val travelRemainingTime = prefs.getString("phone_travel_remaining_time", "") ?: ""
        val travelIconName = prefs.getString("phone_travel_icon_name", "round_navigation_24") ?: "round_navigation_24"
        val travelIsPaused = prefs.getBoolean("phone_travel_is_paused", false)

        val displayName = if (travelIsPaused) "$travelName (Paused)" else travelName

        // Travel Icon
        columnBuilder.addContent(
            LayoutElementBuilders.Image.Builder()
                .setResourceId(travelIconName)
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

        // Travel Name
        columnBuilder.addContent(
            Text.Builder(context, displayName)
                .setColor(argb(0xFFFFFFFF.toInt()))
                .setTypography(Typography.TYPOGRAPHY_CAPTION1)
                .setMaxLines(2)
                .setMultilineAlignment(LayoutElementBuilders.TEXT_ALIGN_CENTER)
                .build()
        )

        // Travel row: icon + remaining time
        columnBuilder.addContent(
            LayoutElementBuilders.Row.Builder()
                .setVerticalAlignment(LayoutElementBuilders.VERTICAL_ALIGN_CENTER)
                .addContent(
                    LayoutElementBuilders.Image.Builder()
                        .setResourceId("round_navigation_24")
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
                    Text.Builder(context, travelRemainingTime)
                        .setColor(argb(lightAccent ?: Colors.DEFAULT.primary))
                        .setTypography(Typography.TYPOGRAPHY_CAPTION1)
                        .build()
                )
                .build()
        )

        val travelRemainingDistance = prefs.getString("phone_travel_remaining_distance", "") ?: ""
        if (travelRemainingDistance.isNotEmpty()) {
            columnBuilder.addContent(
                LayoutElementBuilders.Spacer.Builder()
                    .setHeight(DimensionBuilders.dp(2f))
                    .build()
            )
            columnBuilder.addContent(
                Text.Builder(context, travelRemainingDistance)
                    .setColor(argb(0xFFFFFFFF.toInt()))
                    .setTypography(Typography.TYPOGRAPHY_CAPTION2)
                    .build()
            )
        }
    } else {
        val batteryLevel = prefs.getInt("phone_battery_level", -1)
        val isCharging = prefs.getBoolean("phone_is_charging", false)
        val deviceName = prefs.getString("phone_device_name", context.getString(R.string.your_android_title)) ?: context.getString(R.string.your_android_title)

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

        val batteryIconId = if (isCharging) {
            ID_ICON_BATTERY_BOLT
        } else {
            when {
                batteryLevel >= 75 -> ID_ICON_BATTERY_FULL
                batteryLevel >= 50 -> ID_ICON_BATTERY_5
                batteryLevel > 20 -> ID_ICON_BATTERY_2
                else -> ID_ICON_BATTERY_ALERT
            }
        }

        // Battery row: icon + percentage
        if (batteryLevel >= 0) {
            columnBuilder.addContent(
                LayoutElementBuilders.Row.Builder()
                    .setVerticalAlignment(LayoutElementBuilders.VERTICAL_ALIGN_CENTER)
                    .addContent(
                        LayoutElementBuilders.Image.Builder()
                            .setResourceId(batteryIconId)
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

package com.sameerasw.essentials.complication

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.GoalProgressComplicationData
import androidx.wear.watchface.complications.data.MonochromaticImage
import androidx.wear.watchface.complications.data.MonochromaticImageComplicationData
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.RangedValueComplicationData
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.data.SmallImage
import androidx.wear.watchface.complications.data.SmallImageComplicationData
import androidx.wear.watchface.complications.data.SmallImageType
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.SuspendingComplicationDataSourceService
import com.sameerasw.essentials.R
import com.sameerasw.essentials.presentation.MainActivity

class PhoneBatteryComplicationService : SuspendingComplicationDataSourceService() {

    override fun getPreviewData(type: ComplicationType): ComplicationData? {
        return createComplicationData(85, isCharging = false, type)
    }

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData {
        val prefs = getSharedPreferences("schedule_prefs", Context.MODE_PRIVATE)
        val phoneBattery = prefs.getInt("phone_battery_level", -1)
        val isCharging = prefs.getBoolean("phone_is_charging", false)

        return createComplicationData(phoneBattery, isCharging, request.complicationType)
    }

    private fun createComplicationData(
        phoneLevel: Int,
        isCharging: Boolean,
        type: ComplicationType
    ): ComplicationData {
        val phoneLevelSafe = if (phoneLevel < 0) 0 else phoneLevel
        val textValue = if (phoneLevel < 0) "--" else "$phoneLevelSafe%"
        val contentDescription = PlainComplicationText.Builder(
            if (isCharging) "Phone Battery $phoneLevelSafe% Charging" else "Phone Battery $phoneLevelSafe%"
        ).build()

        val iconRes = getBatteryIconRes(phoneLevel, isCharging)
        val icon = Icon.createWithResource(this, iconRes)
        val monochromaticImage = MonochromaticImage.Builder(icon).build()
        val tapAction = getTapAction()

        return when (type) {
            ComplicationType.RANGED_VALUE -> {
                RangedValueComplicationData.Builder(
                    value = phoneLevelSafe.toFloat(),
                    min = 0f,
                    max = 100f,
                    contentDescription = contentDescription
                )
                .setText(PlainComplicationText.Builder(textValue).build())
                .setTitle(PlainComplicationText.Builder(getString(R.string.phone_battery_short)).build())
                .setMonochromaticImage(monochromaticImage)
                .setTapAction(tapAction)
                .build()
            }
            ComplicationType.GOAL_PROGRESS -> {
                GoalProgressComplicationData.Builder(
                    value = phoneLevelSafe.toFloat(),
                    targetValue = 100f,
                    contentDescription = contentDescription
                )
                .setText(PlainComplicationText.Builder(textValue).build())
                .setTitle(PlainComplicationText.Builder(getString(R.string.phone_battery_short)).build())
                .setMonochromaticImage(monochromaticImage)
                .setTapAction(tapAction)
                .build()
            }
            ComplicationType.SHORT_TEXT -> {
                ShortTextComplicationData.Builder(
                    text = PlainComplicationText.Builder(textValue).build(),
                    contentDescription = contentDescription
                )
                .setTitle(PlainComplicationText.Builder(getString(R.string.phone_battery_short)).build())
                .setMonochromaticImage(monochromaticImage)
                .setTapAction(tapAction)
                .build()
            }
            ComplicationType.SMALL_IMAGE -> {
                SmallImageComplicationData.Builder(
                    smallImage = SmallImage.Builder(
                        image = icon,
                        type = SmallImageType.ICON
                    ).build(),
                    contentDescription = contentDescription
                )
                .setTapAction(tapAction)
                .build()
            }
            ComplicationType.MONOCHROMATIC_IMAGE -> {
                MonochromaticImageComplicationData.Builder(
                    monochromaticImage = monochromaticImage,
                    contentDescription = contentDescription
                )
                .setTapAction(tapAction)
                .build()
            }
            else -> {
                ShortTextComplicationData.Builder(
                    text = PlainComplicationText.Builder(textValue).build(),
                    contentDescription = contentDescription
                )
                .setMonochromaticImage(monochromaticImage)
                .setTapAction(tapAction)
                .build()
            }
        }
    }

    private fun getBatteryIconRes(level: Int, isCharging: Boolean): Int {
        if (isCharging) return R.drawable.rounded_battery_android_frame_bolt_24
        return when {
            level >= 75 -> R.drawable.rounded_battery_android_frame_full_24
            level >= 50 -> R.drawable.rounded_battery_android_frame_5_24
            level > 20 -> R.drawable.rounded_battery_android_frame_2_24
            level >= 0 -> R.drawable.rounded_battery_android_alert_24
            else -> R.drawable.rounded_mobile_24
        }
    }

    private fun getTapAction(): PendingIntent {
        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra(MainActivity.EXTRA_NAVIGATE_TO, MainActivity.NAV_YOUR_ANDROID)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        return PendingIntent.getActivity(
            this,
            5,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}

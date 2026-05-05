package com.sameerasw.essentials.complication

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.graphics.drawable.Icon
import android.os.BatteryManager
import androidx.wear.watchface.complications.data.*
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.SuspendingComplicationDataSourceService
import com.sameerasw.essentials.R
import com.sameerasw.essentials.presentation.MainActivity

class BatteryComplicationService : SuspendingComplicationDataSourceService() {

    override fun getPreviewData(type: ComplicationType): ComplicationData? {
        return createComplicationData(80, 70, type)
    }

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData {
        val prefs = getSharedPreferences("schedule_prefs", Context.MODE_PRIVATE)
        val phoneBattery = prefs.getInt("phone_battery_level", -1)
        
        val batteryManager = getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val watchBattery = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)

        return createComplicationData(watchBattery, phoneBattery, request.complicationType)
    }

    private fun createComplicationData(watchLevel: Int, phoneLevel: Int, type: ComplicationType): ComplicationData {
        val phoneLevelSafe = if (phoneLevel < 0) 0 else phoneLevel
        val text = "W:$watchLevel% P:$phoneLevelSafe%"
        val contentDescription = PlainComplicationText.Builder("Watch $watchLevel%, Phone $phoneLevelSafe%").build()
        
        val tapAction = getTapAction()
        val dualIcon = drawDualBatteryIcon(phoneLevelSafe, watchLevel)
        val monochromaticImage = MonochromaticImage.Builder(dualIcon).build()

        return when (type) {
            ComplicationType.SHORT_TEXT -> {
                ShortTextComplicationData.Builder(
                    text = PlainComplicationText.Builder("$watchLevel%").build(),
                    contentDescription = contentDescription
                )
                .setTitle(PlainComplicationText.Builder("P:$phoneLevelSafe%").build())
                .setMonochromaticImage(monochromaticImage)
                .setTapAction(tapAction)
                .build()
            }
            ComplicationType.RANGED_VALUE -> {
                RangedValueComplicationData.Builder(
                    value = watchLevel.toFloat(),
                    min = 0f,
                    max = 100f,
                    contentDescription = contentDescription
                )
                .setText(PlainComplicationText.Builder("$watchLevel%").build())
                .setTitle(PlainComplicationText.Builder("P:$phoneLevelSafe%").build())
                .setMonochromaticImage(monochromaticImage)
                .setTapAction(tapAction)
                .build()
            }
            ComplicationType.SMALL_IMAGE -> {
                SmallImageComplicationData.Builder(
                    smallImage = SmallImage.Builder(
                        image = dualIcon,
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
                    text = PlainComplicationText.Builder(text).build(),
                    contentDescription = contentDescription
                )
                .setTapAction(tapAction)
                .build()
            }
        }
    }

    private fun drawDualBatteryIcon(phoneLevel: Int, watchLevel: Int): Icon {
        val size = 128
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint().apply {
            isAntiAlias = true
            strokeWidth = 12f
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
        }

        val padding = 12f
        
        // Outer Circle (Watch Battery)
        paint.color = Color.WHITE
        paint.alpha = 40
        val outerRect = RectF(padding, padding, size - padding, size - padding)
        canvas.drawArc(outerRect, 0f, 360f, false, paint)
        
        paint.alpha = 255
        canvas.drawArc(outerRect, -90f, (watchLevel * 3.6f), false, paint)

        // Inner Circle (Phone Battery)
        val innerPadding = padding + 24f
        val innerRect = RectF(innerPadding, innerPadding, size - innerPadding, size - innerPadding)
        
        paint.alpha = 40
        canvas.drawArc(innerRect, 0f, 360f, false, paint)
        
        paint.alpha = 255
        canvas.drawArc(innerRect, -90f, (phoneLevel * 3.6f), false, paint)

        return Icon.createWithBitmap(bitmap)
    }

    private fun getTapAction(): PendingIntent {
        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra(MainActivity.EXTRA_NAVIGATE_TO, MainActivity.NAV_YOUR_ANDROID)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        return PendingIntent.getActivity(
            this,
            2, // Unique ID for this complication
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}

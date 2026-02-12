package com.sameerasw.essentials.complication

import android.content.Context
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.SuspendingComplicationDataSourceService
import java.util.Calendar

/**
 * Skeleton for complication data source that returns short text.
 */
class MainComplicationService : SuspendingComplicationDataSourceService() {

    override fun getPreviewData(type: ComplicationType): ComplicationData? {
        return when (type) {
            ComplicationType.SHORT_TEXT -> createComplicationData("#FFFFFFFF", "Theme Color")
            ComplicationType.RANGED_VALUE -> createRangedComplicationData(0xFFFFFFFF.toLong(), "Theme Color")
            else -> null
        }
    }

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData {
        val prefs = getSharedPreferences("schedule_prefs", Context.MODE_PRIVATE)
        val primaryColor = prefs.getInt("theme_primary_color", -1)
        val targetColor = if (primaryColor != -1) primaryColor.toLong() and 0xFFFFFFFFL else 0xFFFFFFFFL

        return when (request.complicationType) {
            ComplicationType.RANGED_VALUE -> createRangedComplicationData(targetColor, "Theme Color")
            ComplicationType.SHORT_TEXT -> {
                val hexColor = String.format("#%08X", targetColor)
                createComplicationData(hexColor, "Theme Color")
            }
            else -> {
                // Fallback for other types or if data is missing
                when (Calendar.getInstance().get(Calendar.DAY_OF_WEEK)) {
                    Calendar.SUNDAY -> createComplicationData("Sun", "Sunday")
                    Calendar.MONDAY -> createComplicationData("Mon", "Monday")
                    else -> createComplicationData("Today", "Today")
                }
            }
        }
    }

    private fun createRangedComplicationData(colorValue: Long, contentDescription: String) =
        androidx.wear.watchface.complications.data.RangedValueComplicationData.Builder(
            value = colorValue.toFloat(),
            min = 0f,
            max = 4294967295f,
            contentDescription = PlainComplicationText.Builder(contentDescription).build()
        ).build()

    private fun createComplicationData(text: String, contentDescription: String) =
        ShortTextComplicationData.Builder(
            text = PlainComplicationText.Builder(text).build(),
            contentDescription = PlainComplicationText.Builder(contentDescription).build()
        ).build()
}
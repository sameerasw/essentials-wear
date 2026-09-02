package com.sameerasw.essentials.complication

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.BatteryManager
import android.provider.AlarmClock
import android.text.format.DateFormat
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.GoalProgressComplicationData
import androidx.wear.watchface.complications.data.LongTextComplicationData
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
import com.sameerasw.essentials.services.FlashlightToggleReceiver
import com.sameerasw.essentials.tile.MainTileService
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class GlanceComplicationService : SuspendingComplicationDataSourceService() {

    private enum class GlanceType {
        FLASHLIGHT,
        EVENT,
        BATTERY_ALERT,
        TRAVEL,
        ALARM,
        DEFAULT
    }

    private data class GlanceInfo(
        val type: GlanceType,
        val title: String,
        val text: String,
        val iconRes: Int,
        val remainingMinutes: Long? = null,
        val glowMaxMinutes: Long = 120L,
        val glowPeakMinutes: Long = 15L,
        val tapAction: PendingIntent
    )

    override fun getPreviewData(type: ComplicationType): ComplicationData? {
        val icon = Icon.createWithResource(this, R.drawable.rounded_calendar_today_24)
        val monochromaticImage = MonochromaticImage.Builder(icon).build()
        val titleText = "Meeting"
        val descText = "in 15m"
        val contentDesc = PlainComplicationText.Builder("Meeting in 15m").build()

        return when (type) {
            ComplicationType.SHORT_TEXT -> {
                ShortTextComplicationData.Builder(
                    text = PlainComplicationText.Builder(descText).build(),
                    contentDescription = contentDesc
                )
                .setTitle(PlainComplicationText.Builder(titleText).build())
                .setMonochromaticImage(monochromaticImage)
                .build()
            }
            ComplicationType.LONG_TEXT -> {
                LongTextComplicationData.Builder(
                    text = PlainComplicationText.Builder(descText).build(),
                    contentDescription = contentDesc
                )
                .setTitle(PlainComplicationText.Builder(titleText).build())
                .setMonochromaticImage(monochromaticImage)
                .build()
            }
            ComplicationType.RANGED_VALUE -> {
                RangedValueComplicationData.Builder(
                    value = 75f,
                    min = 0f,
                    max = 100f,
                    contentDescription = contentDesc
                )
                .setText(PlainComplicationText.Builder(descText).build())
                .setTitle(PlainComplicationText.Builder(titleText).build())
                .setMonochromaticImage(monochromaticImage)
                .build()
            }
            ComplicationType.GOAL_PROGRESS -> {
                GoalProgressComplicationData.Builder(
                    value = 75f,
                    targetValue = 100f,
                    contentDescription = contentDesc
                )
                .setText(PlainComplicationText.Builder(descText).build())
                .setTitle(PlainComplicationText.Builder(titleText).build())
                .setMonochromaticImage(monochromaticImage)
                .build()
            }
            ComplicationType.SMALL_IMAGE -> {
                SmallImageComplicationData.Builder(
                    smallImage = SmallImage.Builder(icon, SmallImageType.ICON).build(),
                    contentDescription = contentDesc
                ).build()
            }
            ComplicationType.MONOCHROMATIC_IMAGE -> {
                MonochromaticImageComplicationData.Builder(
                    monochromaticImage = monochromaticImage,
                    contentDescription = contentDesc
                ).build()
            }
            else -> null
        }
    }

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData {
        val info = calculateGlanceInfo()
        val icon = Icon.createWithResource(this, info.iconRes)
        val monochromaticImage = MonochromaticImage.Builder(icon).build()
        val contentDesc = PlainComplicationText.Builder("${info.title} ${info.text}").build()

        val prefs = getSharedPreferences("schedule_prefs", Context.MODE_PRIVATE)
        val showGlow = prefs.getBoolean("watchface_show_glow", true)

        // Calculate Glow proximity factor (0.0 to 1.0)
        val proximityFactor = if (showGlow && info.remainingMinutes != null && info.remainingMinutes <= info.glowMaxMinutes) {
            val mins = info.remainingMinutes.coerceAtLeast(0L)
            val peak = info.glowPeakMinutes
            val maxM = info.glowMaxMinutes
            if (maxM > peak) {
                ((maxM - mins).toFloat() / (maxM - peak).toFloat()).coerceIn(0f, 1f)
            } else {
                1f
            }
        } else {
            0f
        }
        val glowProgressPercent = (proximityFactor * 100f).coerceIn(0f, 100f)

        return when (request.complicationType) {
            ComplicationType.SHORT_TEXT -> {
                ShortTextComplicationData.Builder(
                    text = PlainComplicationText.Builder(info.text).build(),
                    contentDescription = contentDesc
                )
                .setTitle(PlainComplicationText.Builder(info.title).build())
                .setMonochromaticImage(monochromaticImage)
                .setTapAction(info.tapAction)
                .build()
            }
            ComplicationType.LONG_TEXT -> {
                LongTextComplicationData.Builder(
                    text = PlainComplicationText.Builder(info.text).build(),
                    contentDescription = contentDesc
                )
                .setTitle(PlainComplicationText.Builder(info.title).build())
                .setMonochromaticImage(monochromaticImage)
                .setTapAction(info.tapAction)
                .build()
            }
            ComplicationType.RANGED_VALUE -> {
                RangedValueComplicationData.Builder(
                    value = glowProgressPercent,
                    min = 0f,
                    max = 100f,
                    contentDescription = contentDesc
                )
                .setText(PlainComplicationText.Builder(info.text).build())
                .setTitle(PlainComplicationText.Builder(info.title).build())
                .setMonochromaticImage(monochromaticImage)
                .setTapAction(info.tapAction)
                .build()
            }
            ComplicationType.GOAL_PROGRESS -> {
                GoalProgressComplicationData.Builder(
                    value = glowProgressPercent,
                    targetValue = 100f,
                    contentDescription = contentDesc
                )
                .setText(PlainComplicationText.Builder(info.text).build())
                .setTitle(PlainComplicationText.Builder(info.title).build())
                .setMonochromaticImage(monochromaticImage)
                .setTapAction(info.tapAction)
                .build()
            }
            ComplicationType.SMALL_IMAGE -> {
                SmallImageComplicationData.Builder(
                    smallImage = SmallImage.Builder(icon, SmallImageType.ICON).build(),
                    contentDescription = contentDesc
                )
                .setTapAction(info.tapAction)
                .build()
            }
            ComplicationType.MONOCHROMATIC_IMAGE -> {
                MonochromaticImageComplicationData.Builder(
                    monochromaticImage = monochromaticImage,
                    contentDescription = contentDesc
                )
                .setTapAction(info.tapAction)
                .build()
            }
            else -> {
                ShortTextComplicationData.Builder(
                    text = PlainComplicationText.Builder(info.text).build(),
                    contentDescription = contentDesc
                )
                .setMonochromaticImage(monochromaticImage)
                .setTapAction(info.tapAction)
                .build()
            }
        }
    }

    private fun calculateGlanceInfo(): GlanceInfo {
        val now = System.currentTimeMillis()
        val prefs = getSharedPreferences("schedule_prefs", Context.MODE_PRIVATE)

        val showFlashlight = prefs.getBoolean("watchface_glance_flashlight", true)
        val showBatteryAlerts = prefs.getBoolean("watchface_glance_battery_alerts", true)
        val showTravel = prefs.getBoolean("watchface_glance_travel", true)
        val showEvents = prefs.getBoolean("watchface_glance_events", true)
        val showAlarm = prefs.getBoolean("watchface_glance_alarm", true)

        // 0. Flashlight on phone
        if (showFlashlight) {
            val flashlightOn = prefs.getBoolean("phone_flashlight_on", false)
            if (flashlightOn) {
                return GlanceInfo(
                    type = GlanceType.FLASHLIGHT,
                    title = "Flashlight",
                    text = "On",
                    iconRes = R.drawable.round_flashlight_on_24,
                    remainingMinutes = 0L,
                    glowMaxMinutes = 60L,
                    glowPeakMinutes = 0L,
                    tapAction = getFlashlightTapAction()
                )
            }
        }

        // 1. Calendar Events (support overlapping events stack & glow up to 30 mins of ongoing)
        var earlyUpcomingMeeting: GlanceInfo? = null
        if (showEvents) {
            try {
                val cal = Calendar.getInstance().apply { timeInMillis = now }
                cal.set(Calendar.HOUR_OF_DAY, 23)
                cal.set(Calendar.MINUTE, 59)
                cal.set(Calendar.SECOND, 59)
                cal.set(Calendar.MILLISECOND, 999)
                val endOfToday = cal.timeInMillis

                val eventList = MainTileService.getSyncedEvents(this)
                val todayEvents = eventList.filter { !it.allDay && it.end > now && it.begin <= endOfToday }
                val ongoingEvents = todayEvents.filter { now in it.begin..it.end }.sortedByDescending { it.begin }
                val upcomingEvents = todayEvents.filter { it.begin > now }.sortedBy { it.begin }

                val nextUpcoming = upcomingEvents.firstOrNull()
                val selectedEvent = if (nextUpcoming != null && (nextUpcoming.begin - now) <= 15 * 60 * 1000L) {
                    nextUpcoming
                } else {
                    ongoingEvents.firstOrNull() ?: nextUpcoming
                }

                if (selectedEvent != null) {
                    val isOngoing = now in selectedEvent.begin..selectedEvent.end
                    val diffMs = selectedEvent.begin - now
                    val diffMinutes = if (diffMs <= 0) 0L else (diffMs / 60000L).coerceAtLeast(1L)
                    val timeStr = if (isOngoing) {
                        "Now"
                    } else {
                        if (diffMinutes < 60) {
                            "in ${diffMinutes}m"
                        } else {
                            val hours = diffMinutes / 60
                            val remainingMin = diffMinutes % 60
                            if (remainingMin > 0) "in ${hours}h ${remainingMin}m" else "in ${hours}h"
                        }
                    }
                    val rawTitle = selectedEvent.title ?: ""
                    val cleanTitle = if (rawTitle.length > 20) rawTitle.take(19) + "…" else rawTitle

                    val (remainingMinsForGlow, glowMax, glowPeak) = if (isOngoing) {
                        val elapsedMinutes = ((now - selectedEvent.begin) / 60000L).coerceAtLeast(0L)
                        if (elapsedMinutes < 30L) {
                            Triple(0L, 30L, 0L)
                        } else {
                            Triple(null, 30L, 0L)
                        }
                    } else {
                        Triple(diffMinutes, 120L, 15L)
                    }

                    earlyUpcomingMeeting = GlanceInfo(
                        type = GlanceType.EVENT,
                        title = cleanTitle,
                        text = timeStr,
                        iconRes = R.drawable.rounded_calendar_today_24,
                        remainingMinutes = remainingMinsForGlow,
                        glowMaxMinutes = glowMax,
                        glowPeakMinutes = glowPeak,
                        tapAction = getScheduleTapAction()
                    )
                }
            } catch (_: Exception) { }
        }

        if (earlyUpcomingMeeting != null && (earlyUpcomingMeeting.text == "Now" || (earlyUpcomingMeeting.remainingMinutes != null && earlyUpcomingMeeting.remainingMinutes in 1..30))) {
            return earlyUpcomingMeeting
        }

        // 2. Battery Alerts
        if (showBatteryAlerts) {
            val watchBat = getWatchBatteryLevel()
            val phoneBat = prefs.getInt("phone_battery_level", -1).let { if (it >= 0) it else 100 }
            val isPhoneCharging = prefs.getBoolean("phone_is_charging", false)
            val isWatchCharging = try {
                val bm = getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
                val status = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_STATUS) ?: -1
                status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
            } catch (_: Exception) { false }

            val watchLow = watchBat in 1..19 && !isWatchCharging
            val phoneLow = phoneBat in 1..19 && !isPhoneCharging

            if (watchLow && phoneLow) {
                return GlanceInfo(
                    type = GlanceType.BATTERY_ALERT,
                    title = "Battery",
                    text = "Watch & Phone Low",
                    iconRes = R.drawable.rounded_battery_android_alert_24,
                    remainingMinutes = 0L,
                    glowMaxMinutes = 60L,
                    glowPeakMinutes = 0L,
                    tapAction = getYourAndroidTapAction()
                )
            } else if (watchLow) {
                return GlanceInfo(
                    type = GlanceType.BATTERY_ALERT,
                    title = "Battery",
                    text = "Watch Low ($watchBat%)",
                    iconRes = R.drawable.rounded_battery_android_alert_24,
                    remainingMinutes = 0L,
                    glowMaxMinutes = 60L,
                    glowPeakMinutes = 0L,
                    tapAction = getYourAndroidTapAction()
                )
            } else if (phoneLow) {
                return GlanceInfo(
                    type = GlanceType.BATTERY_ALERT,
                    title = "Battery",
                    text = "Phone Low ($phoneBat%)",
                    iconRes = R.drawable.rounded_battery_android_alert_24,
                    remainingMinutes = 0L,
                    glowMaxMinutes = 60L,
                    glowPeakMinutes = 0L,
                    tapAction = getYourAndroidTapAction()
                )
            }
        }

        // 3. Travel / Navigation
        if (showTravel) {
            val travelActive = prefs.getBoolean("phone_travel_active", false)
            val syncLocationReachedEnabled = prefs.getBoolean("phone_watch_sync_location_reached_enabled", true)
            if (travelActive && syncLocationReachedEnabled) {
                val travelName = prefs.getString("phone_travel_name", "") ?: ""
                val travelRemainingTime = prefs.getString("phone_travel_remaining_time", "") ?: ""
                val travelRemainingDistance = prefs.getString("phone_travel_remaining_distance", "") ?: ""
                val travelIconName = prefs.getString("phone_travel_icon_name", "round_navigation_24") ?: "round_navigation_24"
                val travelIconRes = resources.getIdentifier(travelIconName, "drawable", packageName).let {
                    if (it != 0) it else R.drawable.round_navigation_24
                }

                var travelMinutes: Long? = null
                if (travelRemainingTime.isNotBlank()) {
                    try {
                        val timePart = travelRemainingTime.lowercase().trim()
                        if (timePart.contains("h")) {
                            val parts = timePart.split("h")
                            val h = parts[0].filter { it.isDigit() }.toLongOrNull() ?: 0L
                            val m = parts.getOrNull(1)?.filter { it.isDigit() }?.toLongOrNull() ?: 0L
                            travelMinutes = (h * 60L) + m
                        } else if (timePart.contains("m")) {
                            travelMinutes = timePart.filter { it.isDigit() }.toLongOrNull()
                        }
                    } catch (_: Exception) { }
                }

                val etaPrefix = when {
                    travelRemainingTime.isNotBlank() -> travelRemainingTime
                    travelRemainingDistance.isNotBlank() -> travelRemainingDistance
                    else -> "ETA"
                }

                val travelText = if (travelName.isNotBlank()) "$etaPrefix till $travelName" else etaPrefix

                return GlanceInfo(
                    type = GlanceType.TRAVEL,
                    title = if (travelName.isNotBlank()) travelName else "Travel",
                    text = travelText,
                    iconRes = travelIconRes,
                    remainingMinutes = travelMinutes ?: 30L,
                    glowMaxMinutes = 60L,
                    glowPeakMinutes = 20L,
                    tapAction = getYourAndroidTapAction()
                )
            }
        }

        // 4. Calendar Event later today
        if (earlyUpcomingMeeting != null) {
            return earlyUpcomingMeeting
        }

        // 5. Next Alarm
        if (showAlarm) {
            try {
                val am = getSystemService(Context.ALARM_SERVICE) as? AlarmManager
                val nextAlarm = am?.nextAlarmClock
                if (nextAlarm != null && nextAlarm.triggerTime > now) {
                    val alarmCal = Calendar.getInstance().apply { timeInMillis = nextAlarm.triggerTime }
                    val diffMs = nextAlarm.triggerTime - now
                    val diffMinutes = (diffMs / 60000L).coerceAtLeast(1L)
                    val is24 = DateFormat.is24HourFormat(this)
                    val timeStr = if (is24) {
                        SimpleDateFormat("HH:mm", Locale.getDefault()).format(alarmCal.time)
                    } else {
                        val h = alarmCal.get(Calendar.HOUR).let { if (it == 0) 12 else it }
                        val m = alarmCal.get(Calendar.MINUTE)
                        val amPm = if (alarmCal.get(Calendar.AM_PM) == Calendar.AM) "AM" else "PM"
                        String.format(Locale.getDefault(), "%d:%02d %s", h, m, amPm)
                    }
                    return GlanceInfo(
                        type = GlanceType.ALARM,
                        title = "Alarm",
                        text = timeStr,
                        iconRes = R.drawable.rounded_alarm_24,
                        remainingMinutes = diffMinutes,
                        glowMaxMinutes = 120L,
                        glowPeakMinutes = 15L,
                        tapAction = nextAlarm.showIntent ?: getAlarmTapAction()
                    )
                }
            } catch (_: Exception) { }
        }

        // 6. Default Fallback (Date / Schedule)
        val today = Calendar.getInstance().time
        val dateText = SimpleDateFormat("EEE, MMM d", Locale.getDefault()).format(today)
        return GlanceInfo(
            type = GlanceType.DEFAULT,
            title = dateText,
            text = getString(R.string.no_events),
            iconRes = R.drawable.rounded_calendar_today_24,
            remainingMinutes = null,
            tapAction = getScheduleTapAction()
        )
    }

    private fun getWatchBatteryLevel(): Int {
        return try {
            val bm = getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
            bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: 100
        } catch (_: Exception) {
            100
        }
    }

    private fun getFlashlightTapAction(): PendingIntent {
        val intent = Intent(this, FlashlightToggleReceiver::class.java)
        return PendingIntent.getBroadcast(
            this,
            10,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun getScheduleTapAction(): PendingIntent {
        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra(MainActivity.EXTRA_NAVIGATE_TO, MainActivity.NAV_SCHEDULE)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        return PendingIntent.getActivity(
            this,
            11,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun getYourAndroidTapAction(): PendingIntent {
        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra(MainActivity.EXTRA_NAVIGATE_TO, MainActivity.NAV_YOUR_ANDROID)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        return PendingIntent.getActivity(
            this,
            12,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun getAlarmTapAction(): PendingIntent {
        val intent = Intent(AlarmClock.ACTION_SHOW_ALARMS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        return PendingIntent.getActivity(
            this,
            13,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}

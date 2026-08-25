package com.sameerasw.essentials.watchface

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.BatteryManager
import android.util.Log
import android.view.SurfaceHolder
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.wear.watchface.CanvasType
import androidx.wear.watchface.ComplicationSlot
import androidx.wear.watchface.ComplicationSlotsManager
import androidx.wear.watchface.DrawMode
import androidx.wear.watchface.Renderer
import androidx.wear.watchface.TapEvent
import androidx.wear.watchface.TapType
import androidx.wear.watchface.WatchFace
import androidx.wear.watchface.WatchFaceService
import androidx.wear.watchface.WatchFaceType
import androidx.wear.watchface.WatchState
import androidx.wear.watchface.complications.ComplicationSlotBounds
import androidx.wear.watchface.complications.DefaultComplicationDataSourcePolicy
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.rendering.CanvasComplicationDrawable
import androidx.wear.watchface.complications.rendering.ComplicationDrawable
import androidx.wear.watchface.style.CurrentUserStyleRepository
import com.google.android.gms.wearable.Wearable
import com.sameerasw.essentials.R
import com.sameerasw.essentials.utils.HapticUtil
import com.sameerasw.essentials.utils.ThemeUtil
import java.text.SimpleDateFormat
import java.time.ZonedDateTime
import java.util.Calendar
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class EssentialsWatchFaceService : WatchFaceService() {

    companion object {
        const val LEFT_COMPLICATION_ID = 100
        const val RIGHT_COMPLICATION_ID = 101
    }

    private fun getClockColor(): Int {
        val themeColor = ThemeUtil.getThemeColor(applicationContext)
        return themeColor?.let { ThemeUtil.getLightAccentColor(it) } ?: 0xFFB39DDB.toInt()
    }

    private fun getTrackColor(): Int {
        val themeColor = ThemeUtil.getThemeColor(applicationContext) ?: 0xFF6750A4.toInt()
        return ThemeUtil.getTonedColor(themeColor)
    }

    override fun createComplicationSlotsManager(
        currentUserStyleRepository: CurrentUserStyleRepository
    ): ComplicationSlotsManager {
        val canvasComplicationFactory = androidx.wear.watchface.CanvasComplicationFactory { watchState, listener ->
            val complicationDrawable = ComplicationDrawable(applicationContext).apply {
                val accentColor = getClockColor()
                val trackColor = getTrackColor()
                activeStyle.textColor = Color.WHITE
                activeStyle.titleColor = Color.WHITE
                activeStyle.iconColor = Color.WHITE
                activeStyle.rangedValuePrimaryColor = accentColor
                activeStyle.rangedValueSecondaryColor = trackColor
                activeStyle.highlightColor = accentColor
                ambientStyle.textColor = Color.WHITE
                ambientStyle.titleColor = Color.WHITE
                ambientStyle.iconColor = Color.WHITE
                ambientStyle.rangedValuePrimaryColor = Color.WHITE
                ambientStyle.rangedValueSecondaryColor = 0x66FFFFFF.toInt()
                ambientStyle.highlightColor = Color.WHITE
            }
            CanvasComplicationDrawable(
                complicationDrawable,
                watchState,
                listener
            )
        }

        val supportedTypes = listOf(
            ComplicationType.GOAL_PROGRESS,
            ComplicationType.WEIGHTED_ELEMENTS,
            ComplicationType.RANGED_VALUE,
            ComplicationType.SHORT_TEXT,
            ComplicationType.MONOCHROMATIC_IMAGE,
            ComplicationType.SMALL_IMAGE
        )

        // Left slot: centered at (0.13, 0.50) with size 0.26 x 0.26 (bounds: 0.00f, 0.37f, 0.26f, 0.63f)
        val leftComplicationSlot = ComplicationSlot.createRoundRectComplicationSlotBuilder(
            id = LEFT_COMPLICATION_ID,
            canvasComplicationFactory = canvasComplicationFactory,
            supportedTypes = supportedTypes,
            defaultDataSourcePolicy = DefaultComplicationDataSourcePolicy(),
            bounds = ComplicationSlotBounds(
                RectF(0.00f, 0.37f, 0.26f, 0.63f)
            )
        ).setNameResourceId(R.string.left_complication_label)
            .build()

        // Right slot: centered at (0.87, 0.50) with size 0.26 x 0.26 (bounds: 0.74f, 0.37f, 1.00f, 0.63f)
        val rightComplicationSlot = ComplicationSlot.createRoundRectComplicationSlotBuilder(
            id = RIGHT_COMPLICATION_ID,
            canvasComplicationFactory = canvasComplicationFactory,
            supportedTypes = supportedTypes,
            defaultDataSourcePolicy = DefaultComplicationDataSourcePolicy(),
            bounds = ComplicationSlotBounds(
                RectF(0.74f, 0.37f, 1.00f, 0.63f)
            )
        ).setNameResourceId(R.string.right_complication_label)
            .build()

        return ComplicationSlotsManager(
            listOf(leftComplicationSlot, rightComplicationSlot),
            currentUserStyleRepository
        )
    }

    override suspend fun createWatchFace(
        surfaceHolder: SurfaceHolder,
        watchState: WatchState,
        complicationSlotsManager: ComplicationSlotsManager,
        currentUserStyleRepository: CurrentUserStyleRepository
    ): WatchFace {
        val renderer = EssentialsCanvasRenderer(
            context = applicationContext,
            surfaceHolder = surfaceHolder,
            watchState = watchState,
            complicationSlotsManager = complicationSlotsManager,
            currentUserStyleRepository = currentUserStyleRepository,
            canvasType = CanvasType.HARDWARE
        )

        val watchFace = WatchFace(
            watchFaceType = WatchFaceType.DIGITAL,
            renderer = renderer
        )

        watchFace.setTapListener(renderer)

        return watchFace
    }
}

private enum class GlanceType {
    FLASHLIGHT,
    BATTERY_ALERT,
    TRAVEL,
    EVENT,
    ALARM
}

private data class TopScheduleInfo(
    val type: GlanceType,
    val text: String,
    val iconDrawable: Drawable?,
    val remainingMinutes: Long? = null,
    val glowMaxMinutes: Long = 120L,
    val glowPeakMinutes: Long = 15L
)

class EssentialsCanvasRenderer(
    private val context: Context,
    surfaceHolder: SurfaceHolder,
    private val watchState: WatchState,
    private val complicationSlotsManager: ComplicationSlotsManager,
    currentUserStyleRepository: CurrentUserStyleRepository,
    canvasType: Int
) : Renderer.CanvasRenderer2<EssentialsCanvasRenderer.EssentialsSharedAssets>(
    surfaceHolder = surfaceHolder,
    currentUserStyleRepository = currentUserStyleRepository,
    watchState = watchState,
    canvasType = canvasType,
    interactiveDrawModeUpdateDelayMillis = 1000L,
    clearWithBackgroundTintBeforeRenderingHighlightLayer = false
), SensorEventListener, WatchFace.TapListener {

    class EssentialsSharedAssets : SharedAssets {
        override fun onDestroy() {}
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private var sharedPrefs: SharedPreferences? = null
    private val prefListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == "theme_primary_color" ||
            key == "phone_battery_level" ||
            key == "phone_flashlight_on" ||
            key == "phone_ringer_mode" ||
            key == "synced_calendar_events" ||
            key?.startsWith("watchface_") == true
        ) {
            updateThemeColor()
            invalidate()
        }
    }

    private val calendar = Calendar.getInstance()
    private val textPaintFlex = Paint()
    private val textPaintGrowth = Paint()
    private val textPaintInflate = Paint()
    private val textPaintRotation = Paint()
    private val datePaint = Paint()
    private val topEventPaint = Paint()
    private val sideValuePaint = Paint()
    private val circleOutlinePaint = Paint()
    private val trackPaint = Paint()
    private val progressPaint = Paint()
    private val bgGradientPaint = Paint()
    private var typefaceFlex: Typeface? = null
    private var typefaceGrowth: Typeface? = null
    private var typefaceInflate: Typeface? = null
    private var typefaceRotation: Typeface? = null

    private var watchIconDrawable: Drawable? = null
    private var mobileIconDrawable: Drawable? = null
    private var heartIconDrawable: Drawable? = null
    private var stepsIconDrawable: Drawable? = null
    private var distanceIconDrawable: Drawable? = null
    private var fireIconDrawable: Drawable? = null
    private var notifIconDrawable: Drawable? = null
    private var musicIconDrawable: Drawable? = null
    private var travelIconDrawable: Drawable? = null
    private var soundIconDrawable: Drawable? = null
    private var flashlightIconDrawable: Drawable? = null
    private var vibrateIconDrawable: Drawable? = null
    private var muteIconDrawable: Drawable? = null
    private var calendarIconDrawable: Drawable? = null
    private var alarmIconDrawable: Drawable? = null
    private var batteryAlertIconDrawable: Drawable? = null

    private var sensorManager: SensorManager? = null
    private var heartRateSensor: Sensor? = null
    private var stepSensor: Sensor? = null
    private var currentHeartRate: Int = 0
    private var currentSteps: Int = 0
    private var lastHeartRateUpdateMs: Long = 0L

    init {
        try {
            typefaceFlex = ResourcesCompat.getFont(context, R.font.google_sans_flex)
        } catch (_: Exception) {
            typefaceFlex = Typeface.DEFAULT
        }
        try {
            typefaceGrowth = ResourcesCompat.getFont(context, R.font.growth)
        } catch (_: Exception) {
            typefaceGrowth = Typeface.DEFAULT
        }
        try {
            typefaceInflate = ResourcesCompat.getFont(context, R.font.inflate_vf)
        } catch (_: Exception) {
            typefaceInflate = Typeface.DEFAULT
        }
        try {
            typefaceRotation = ResourcesCompat.getFont(context, R.font.rotation_gx)
        } catch (_: Exception) {
            typefaceRotation = Typeface.DEFAULT
        }

        textPaintFlex.apply {
            color = getClockColor()
            typeface = typefaceFlex ?: Typeface.DEFAULT
            fontVariationSettings = "'ROND' 100.0, 'wdth' 150.0, 'wght' 200.0"
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }

        textPaintGrowth.apply {
            color = getClockColor()
            typeface = typefaceGrowth ?: Typeface.DEFAULT
            fontVariationSettings = "'wght' 600.0"
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }

        textPaintInflate.apply {
            color = getClockColor()
            typeface = typefaceInflate ?: Typeface.DEFAULT
            fontVariationSettings = "'wght' 600.0"
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }

        textPaintRotation.apply {
            color = getClockColor()
            typeface = typefaceRotation ?: Typeface.DEFAULT
            fontVariationSettings = "'wght' 600.0"
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }

        datePaint.apply {
            color = Color.WHITE
            typeface = typefaceFlex ?: Typeface.DEFAULT
            fontVariationSettings = "'ROND' 100.0, 'wdth' 100.0, 'wght' 400.0"
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }

        topEventPaint.apply {
            color = Color.WHITE
            typeface = typefaceFlex ?: Typeface.DEFAULT
            fontVariationSettings = "'ROND' 100.0, 'wdth' 100.0, 'wght' 400.0"
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }

        sideValuePaint.apply {
            color = Color.WHITE
            typeface = typefaceFlex ?: Typeface.DEFAULT
            fontVariationSettings = "'ROND' 100.0, 'wdth' 100.0, 'wght' 400.0"
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }

        circleOutlinePaint.apply {
            color = getTrackColor()
            style = Paint.Style.STROKE
            isAntiAlias = true
        }

        bgGradientPaint.apply {
            isAntiAlias = true
        }

        trackPaint.apply {
            color = getTrackColor()
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            isAntiAlias = true
        }

        progressPaint.apply {
            color = getClockColor()
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            isAntiAlias = true
        }

        watchIconDrawable = ContextCompat.getDrawable(context, R.drawable.rounded_watch_24)?.mutate()
        mobileIconDrawable = ContextCompat.getDrawable(context, R.drawable.rounded_mobile_24)?.mutate()
        heartIconDrawable = ContextCompat.getDrawable(context, R.drawable.rounded_favorite_24)?.mutate()
        stepsIconDrawable = ContextCompat.getDrawable(context, R.drawable.rounded_steps_24)?.mutate()
        distanceIconDrawable = ContextCompat.getDrawable(context, R.drawable.rounded_distance_24)?.mutate()
        fireIconDrawable = ContextCompat.getDrawable(context, R.drawable.rounded_local_fire_department_24)?.mutate()
        notifIconDrawable = ContextCompat.getDrawable(context, R.drawable.rounded_mobile_text_2_24)?.mutate()
        musicIconDrawable = ContextCompat.getDrawable(context, R.drawable.rounded_music_note_24)?.mutate()
        travelIconDrawable = ContextCompat.getDrawable(context, R.drawable.rounded_directions_bus_24)?.mutate()
        soundIconDrawable = ContextCompat.getDrawable(context, R.drawable.rounded_volume_up_24)?.mutate()
        flashlightIconDrawable = ContextCompat.getDrawable(context, R.drawable.rounded_flashlight_on_24)?.mutate()
        vibrateIconDrawable = ContextCompat.getDrawable(context, R.drawable.rounded_mobile_vibrate_24)?.mutate()
        muteIconDrawable = ContextCompat.getDrawable(context, R.drawable.rounded_volume_off_24)?.mutate()
        calendarIconDrawable = ContextCompat.getDrawable(context, R.drawable.rounded_calendar_today_24)?.mutate()
        alarmIconDrawable = ContextCompat.getDrawable(context, R.drawable.rounded_alarm_24)?.mutate()
        batteryAlertIconDrawable = ContextCompat.getDrawable(context, R.drawable.rounded_battery_alert_24)?.mutate()

        sharedPrefs = context.getSharedPreferences("schedule_prefs", Context.MODE_PRIVATE)
        sharedPrefs?.registerOnSharedPreferenceChangeListener(prefListener)

        sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        heartRateSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_HEART_RATE)
        stepSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
            ?: sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)

        // Observe ambient mode and visibility changes through standard StateFlows
        scope.launch {
            watchState.isAmbient.collect { isAmbient ->
                if (isAmbient == true) {
                    unregisterSensors()
                } else if (watchState.isVisible.value == true) {
                    registerSensors()
                }
                invalidate()
            }
        }

        scope.launch {
            watchState.isVisible.collect { isVisible ->
                if (isVisible == true && watchState.isAmbient.value == false) {
                    registerSensors()
                } else {
                    unregisterSensors()
                }
                invalidate()
            }
        }
    }

    override suspend fun createSharedAssets(): EssentialsSharedAssets {
        return EssentialsSharedAssets()
    }

    override fun onDestroy() {
        scope.cancel()
        unregisterSensors()
        sharedPrefs?.unregisterOnSharedPreferenceChangeListener(prefListener)
        super.onDestroy()
    }

    private fun getClockColor(): Int {
        val themeColor = ThemeUtil.getThemeColor(context)
        return themeColor?.let { ThemeUtil.getLightAccentColor(it) } ?: 0xFFB39DDB.toInt()
    }

    private fun getTrackColor(): Int {
        val themeColor = ThemeUtil.getThemeColor(context) ?: 0xFF6750A4.toInt()
        return ThemeUtil.getTonedColor(themeColor)
    }

    private fun updateThemeColor() {
        val color = getClockColor()
        textPaintFlex.color = color
        textPaintGrowth.color = color
        textPaintInflate.color = color
        textPaintRotation.color = color
        progressPaint.color = color
        val trackColor = getTrackColor()
        trackPaint.color = trackColor
        circleOutlinePaint.color = trackColor
    }

    private fun registerSensors() {
        if (watchState.isAmbient.value == true || watchState.isVisible.value != true) return
        heartRateSensor?.let {
            sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
        stepSensor?.let {
            sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    private fun unregisterSensors() {
        sensorManager?.unregisterListener(this)
    }

    private var stepCountPrefs: SharedPreferences? = null

    private fun getStepPrefs(): SharedPreferences {
        return stepCountPrefs ?: context.getSharedPreferences("watchface_steps_prefs", Context.MODE_PRIVATE).also {
            stepCountPrefs = it
        }
    }

    private fun getSavedDailySteps(): Int {
        val prefs = getStepPrefs()
        val savedDay = prefs.getInt("step_day_of_year", -1)
        val currentDay = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
        if (savedDay != currentDay) {
            return 0
        }
        return prefs.getInt("daily_step_count", 0)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        val sensor = event?.sensor ?: return
        val now = System.currentTimeMillis()

        if (sensor.type == Sensor.TYPE_HEART_RATE && event.values.isNotEmpty()) {
            val hr = event.values[0].toInt()
            if (hr > 0 && (now - lastHeartRateUpdateMs >= 4000L || currentHeartRate == 0)) {
                currentHeartRate = hr
                lastHeartRateUpdateMs = now
                invalidate()
            }
        } else if (sensor.type == Sensor.TYPE_STEP_COUNTER && event.values.isNotEmpty()) {
            val prefs = getStepPrefs()
            val currentDay = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
            val savedDay = prefs.getInt("step_day_of_year", -1)
            if (savedDay != currentDay) {
                currentSteps = 0
                prefs.edit()
                    .putInt("step_day_of_year", currentDay)
                    .putInt("daily_step_count", 0)
                    .apply()
            }
            currentSteps++
            prefs.edit().putInt("daily_step_count", currentSteps).apply()
            invalidate()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onTapEvent(tapType: Int, tapEvent: TapEvent, complicationSlot: androidx.wear.watchface.ComplicationSlot?) {
        if (watchState.isAmbient.value == true) return
        if (tapType == TapType.UP) {
            val x = tapEvent.xPos.toFloat()
            val y = tapEvent.yPos.toFloat()
            handleTap(x, y)
        }
    }

    private var currentBounds: Rect = Rect(0, 0, 454, 454)

    private fun handleTap(x: Float, y: Float) {
        val prefs = sharedPrefs
        val height = currentBounds.height().toFloat()
        val width = currentBounds.width().toFloat()
        val centerY = height / 2f

        val showUpcomingEvents = prefs?.getBoolean("watchface_show_glance", prefs?.getBoolean("watchface_show_upcoming_events", true) ?: true) ?: true
        if (showUpcomingEvents && y < centerY - (height * 0.15f)) {
            val topInfo = getUpcomingMeetingOrAlarm()
            if (topInfo != null) {
                HapticUtil.performClick(context)
                when (topInfo.type) {
                    GlanceType.FLASHLIGHT -> togglePhoneFlashlight()
                    GlanceType.EVENT -> openScheduleScreen()
                    GlanceType.ALARM -> openAlarmApp()
                    GlanceType.TRAVEL, GlanceType.BATTERY_ALERT -> openYourAndroidScreen()
                }
                return
            }
        }

        val showComplications = prefs?.getBoolean("watchface_show_complications", true) ?: true
        if (showComplications) {
            val circleRadius = height * 0.14f
            val leftCenterX = width * 0.12f
            val rightCenterX = width * 0.88f

            val distLeft = Math.hypot((x - leftCenterX).toDouble(), (y - centerY).toDouble()).toFloat()
            val distRight = Math.hypot((x - rightCenterX).toDouble(), (y - centerY).toDouble()).toFloat()

            val leftComplicationType = prefs?.getString("watchface_left_complication", "DYNAMIC") ?: "DYNAMIC"
            val rightComplicationType = prefs?.getString("watchface_right_complication", "STEPS") ?: "STEPS"

            if (distLeft <= circleRadius) {
                handleComplicationTap(leftComplicationType)
            } else if (distRight <= circleRadius) {
                handleComplicationTap(rightComplicationType)
            }
        }
    }

    private fun handleComplicationTap(type: String) {
        when (type) {
            "HEART_RATE", "STEPS", "DISTANCE", "CALORIES" -> {
                HapticUtil.performClick(context)
                openHealthApp()
            }
            "TRAVEL", "SOUND_MODE", "NOTIFICATIONS", "NOW_PLAYING", "PHONE_BATTERY" -> {
                HapticUtil.performClick(context)
                openYourAndroidScreen()
            }
            else -> {
                HapticUtil.performClick(context)
            }
        }
    }

    private fun togglePhoneFlashlight() {
        try {
            val nodeClient = Wearable.getNodeClient(context)
            nodeClient.connectedNodes.addOnSuccessListener { nodes ->
                val messageClient = Wearable.getMessageClient(context)
                for (node in nodes) {
                    messageClient.sendMessage(node.id, "/toggle_flashlight", byteArrayOf())
                }
            }
        } catch (_: Exception) { }
    }

    private fun openHealthApp() {
        try {
            val intent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
            }
            val pm = context.packageManager
            val packages = listOf(
                "com.google.android.apps.fitness",
                "com.samsung.android.app.shealth",
                "com.fitbit.FitbitMobile"
            )
            for (pkg in packages) {
                val launchIntent = pm.getLaunchIntentForPackage(pkg)
                if (launchIntent != null) {
                    context.startActivity(launchIntent)
                    return
                }
            }
            openYourAndroidScreen()
        } catch (_: Exception) {
            openYourAndroidScreen()
        }
    }

    private fun openAlarmApp() {
        try {
            val intent = Intent(android.provider.AlarmClock.ACTION_SHOW_ALARMS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            openYourAndroidScreen()
        }
    }

    private fun openScheduleScreen() {
        try {
            val intent = Intent(context, com.sameerasw.essentials.presentation.MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(com.sameerasw.essentials.presentation.MainActivity.EXTRA_NAVIGATE_TO, com.sameerasw.essentials.presentation.MainActivity.NAV_SCHEDULE)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            openYourAndroidScreen()
        }
    }

    private fun openYourAndroidScreen() {
        try {
            val intent = Intent(context, com.sameerasw.essentials.presentation.MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(com.sameerasw.essentials.presentation.MainActivity.EXTRA_NAVIGATE_TO, com.sameerasw.essentials.presentation.MainActivity.NAV_YOUR_ANDROID)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e("EssentialsCanvasRenderer", "Failed to launch screen", e)
        }
    }

    override fun renderHighlightLayer(canvas: Canvas, bounds: Rect, zonedDateTime: ZonedDateTime, sharedAssets: EssentialsSharedAssets) {}

    override fun render(
        canvas: Canvas,
        bounds: Rect,
        zonedDateTime: ZonedDateTime,
        sharedAssets: EssentialsSharedAssets
    ) {
        currentBounds = bounds
        val isAmbient = renderParameters.drawMode == DrawMode.AMBIENT

        val now = System.currentTimeMillis()
        calendar.timeInMillis = now

        val rawHour = calendar.get(Calendar.HOUR)
        val hourInt = if (rawHour == 0) 12 else rawHour
        val minuteInt = calendar.get(Calendar.MINUTE)

        val hourText = String.format(Locale.getDefault(), "%02d", hourInt)
        val minuteText = String.format(Locale.getDefault(), "%02d", minuteInt)

        canvas.drawColor(Color.BLACK)

        val width = bounds.width().toFloat()
        val height = bounds.height().toFloat()
        val centerX = bounds.exactCenterX()
        val centerY = bounds.exactCenterY()

        val prefs = sharedPrefs
        val hideBattery = prefs?.getBoolean("watchface_hide_battery", false) ?: false
        val hideDeviceIcons = prefs?.getBoolean("watchface_hide_device_icons", false) ?: false
        val showComplications = prefs?.getBoolean("watchface_show_complications", true) ?: true
        val complicationOutline = prefs?.getBoolean("watchface_complication_outline", true) ?: true
        val leftComplicationType = prefs?.getString("watchface_left_complication", "HEART_RATE") ?: "HEART_RATE"
        val rightComplicationType = prefs?.getString("watchface_right_complication", "STEPS") ?: "STEPS"
        val showUpcomingEvents = prefs?.getBoolean("watchface_show_glance", prefs?.getBoolean("watchface_show_upcoming_events", true) ?: true) ?: true
        val showGlow = prefs?.getBoolean("watchface_show_glow", true) ?: true

        val clockFont = prefs?.getString("watchface_clock_font", "FLEX") ?: "FLEX"
        val clockFontSize = prefs?.getInt("watchface_clock_font_size", 100) ?: 100
        val clockDualTone = prefs?.getBoolean("watchface_clock_dual_tone", false) ?: false
        val clockOutline = prefs?.getBoolean("watchface_clock_outline", false) ?: false
        val clockOutlineThickness = prefs?.getInt("watchface_clock_outline_thickness", 2) ?: 2

        val topInfo = if (!isAmbient && showUpcomingEvents) getUpcomingMeetingOrAlarm() else null

        if (!isAmbient && showGlow && topInfo != null && topInfo.remainingMinutes != null && topInfo.remainingMinutes <= topInfo.glowMaxMinutes) {
            val mins = topInfo.remainingMinutes.coerceAtLeast(0L)
            val peak = topInfo.glowPeakMinutes
            val maxM = topInfo.glowMaxMinutes
            val proximityFactor = if (maxM > peak) {
                ((maxM - mins).toFloat() / (maxM - peak).toFloat()).coerceIn(0f, 1f)
            } else {
                1f
            }

            if (proximityFactor > 0f) {
                val accent = getClockColor()
                val darkFactor = 0.60f
                val darkRed = (Color.red(accent) * darkFactor).toInt()
                val darkGreen = (Color.green(accent) * darkFactor).toInt()
                val darkBlue = (Color.blue(accent) * darkFactor).toInt()

                val topAlpha = (40 + (190 * proximityFactor)).toInt().coerceIn(0, 255)
                val midAlpha = (20 + (100 * proximityFactor)).toInt().coerceIn(0, 255)
                val gradientHeight = height * (0.35f + (0.50f * proximityFactor))

                val topAlphaColor = Color.argb(topAlpha, darkRed, darkGreen, darkBlue)
                val midAlphaColor = Color.argb(midAlpha, darkRed, darkGreen, darkBlue)

                bgGradientPaint.shader = LinearGradient(
                    centerX,
                    0f,
                    centerX,
                    gradientHeight,
                    intArrayOf(topAlphaColor, midAlphaColor, Color.TRANSPARENT),
                    floatArrayOf(0f, 0.40f, 1f),
                    Shader.TileMode.CLAMP
                )
                canvas.drawRect(0f, 0f, width, height, bgGradientPaint)
            }
        }

        val sizeScale = (clockFontSize.toFloat() / 100f).coerceIn(0.5f, 1.5f)
        val activePaint = when (clockFont) {
            "GROWTH" -> textPaintGrowth
            "INFLATE" -> textPaintInflate
            "ROTATION" -> textPaintRotation
            else -> textPaintFlex
        }
        val baseTextSize = when (clockFont) {
            "GROWTH" -> height * 0.22f * sizeScale
            "INFLATE" -> height * 0.23f * sizeScale
            "ROTATION" -> height * 0.23f * sizeScale
            else -> height * 0.25f * sizeScale
        }
        activePaint.textSize = baseTextSize

        // Clock font variation settings for Interactive vs AOD (Ambient) modes
        when (clockFont) {
            "FLEX" -> {
                // Flex font axes: ROND (0..100), wdth (25..151), wght (1..1000)
                activePaint.fontVariationSettings = if (isAmbient) {
                    "'ROND' 100.0, 'wdth' 125.0, 'wght' 25.0"
                } else {
                    "'ROND' 100.0, 'wdth' 150.0, 'wght' 200.0"
                }
            }
            "GROWTH" -> {
                // Growth font axis: wght (1..1000)
                activePaint.fontVariationSettings = if (isAmbient) {
                    "'wght' 25.0"
                } else {
                    "'wght' 600.0"
                }
            }
            "INFLATE" -> {
                // Inflate font axis: wght (1..1000)
                activePaint.fontVariationSettings = if (isAmbient) {
                    "'wght' 25.0"
                } else {
                    "'wght' 600.0"
                }
            }
            "ROTATION" -> {
                // Rotation font axis: wght (1..1000)
                activePaint.fontVariationSettings = if (isAmbient) {
                    "'wght' 25.0"
                } else {
                    "'wght' 600.0"
                }
            }
            else -> {
                activePaint.fontVariationSettings = if (isAmbient) {
                    "'ROND' 100.0, 'wdth' 150.0, 'wght' 50.0"
                } else {
                    "'ROND' 100.0, 'wdth' 150.0, 'wght' 200.0"
                }
            }
        }

        if (clockOutline) {
            activePaint.style = Paint.Style.STROKE
            activePaint.strokeWidth = clockOutlineThickness * height * 0.003f
        } else {
            activePaint.style = Paint.Style.FILL
            activePaint.strokeWidth = 0f
        }

        if (!isAmbient) {
            val strokeW = height * 0.015f
            trackPaint.strokeWidth = strokeW
            progressPaint.strokeWidth = strokeW

            val dialPadding = strokeW * 1.5f
            val dialRect = RectF(dialPadding, dialPadding, width - dialPadding, height - dialPadding)
            val radius = (width - 2f * dialPadding) / 2f

            if (!hideBattery) {
                val arcSpan = if (hideDeviceIcons) 42f else 30f
                val leftStartAngle = if (hideDeviceIcons) 108f else 120f
                val rightStartAngle = if (hideDeviceIcons) 72f else 60f

                if (!hideDeviceIcons) {
                    val iconSize = (height * 0.085f).toInt()
                    val iconInsetRadius = radius - (iconSize * 0.45f)

                    val leftIconAngleRad = Math.toRadians(111.0)
                    val leftIconCenterX = centerX + iconInsetRadius * cos(leftIconAngleRad).toFloat()
                    val leftIconCenterY = centerY + iconInsetRadius * sin(leftIconAngleRad).toFloat()
                    drawTintedDrawable(canvas, watchIconDrawable, leftIconCenterX, leftIconCenterY, iconSize, Color.WHITE, 111f - 90f)

                    val rightIconAngleRad = Math.toRadians(69.0)
                    val rightIconCenterX = centerX + iconInsetRadius * cos(rightIconAngleRad).toFloat()
                    val rightIconCenterY = centerY + iconInsetRadius * sin(rightIconAngleRad).toFloat()
                    drawTintedDrawable(canvas, mobileIconDrawable, rightIconCenterX, rightIconCenterY, iconSize, Color.WHITE, 69f - 90f)
                }

                // Left Battery Arc (Watch)
                canvas.drawArc(dialRect, leftStartAngle, arcSpan, false, trackPaint)
                val watchBattery = getWatchBatteryLevel()
                val leftProgressSweep = (watchBattery / 100f) * arcSpan
                canvas.drawArc(dialRect, leftStartAngle, leftProgressSweep, false, progressPaint)

                // Right Battery Arc (Phone)
                canvas.drawArc(dialRect, rightStartAngle, -arcSpan, false, trackPaint)
                val phoneBattery = getPhoneBatteryLevel()
                val rightProgressSweep = (phoneBattery / 100f) * arcSpan
                canvas.drawArc(dialRect, rightStartAngle, -rightProgressSweep, false, progressPaint)
            }

            // Draw Curved Date Text at bottom center
            val dateText = SimpleDateFormat("EEE, MMM d", Locale.getDefault()).format(calendar.time)
            datePaint.color = Color.WHITE
            datePaint.textSize = height * 0.070f
            val dateRadius = height * 0.37f
            val datePathRect = RectF(
                centerX - dateRadius,
                centerY - dateRadius,
                centerX + dateRadius,
                centerY + dateRadius
            )
            val datePath = Path().apply {
                addArc(datePathRect, 140f, -100f)
            }
            canvas.drawTextOnPath(dateText, datePath, 0f, 0f, datePaint)

            // Side Complications
            if (showComplications) {
                val sideIconSize = (height * 0.075f).toInt()
                sideValuePaint.textSize = height * 0.055f
                val accentColor = getClockColor()

                circleOutlinePaint.strokeWidth = height * 0.01f
                val circleRadius = height * 0.11f
                val sideIconY = centerY - (height * 0.045f)
                val sideTextY = centerY + (height * 0.065f)

                fun drawComplication(type: String, compCenterX: Float) {
                    if (type == "NONE") return
                    if (complicationOutline) {
                        canvas.drawCircle(compCenterX, centerY, circleRadius, circleOutlinePaint)
                    }
                    when (type) {
                        "DYNAMIC" -> {
                            val flashlightOn = prefs?.getBoolean("phone_flashlight_on", false) ?: false
                            val ringerMode = prefs?.getInt("phone_ringer_mode", 2) ?: 2

                            if (flashlightOn) {
                                drawTintedDrawable(canvas, flashlightIconDrawable, compCenterX, sideIconY, sideIconSize, accentColor)
                                canvas.drawText("On", compCenterX, sideTextY, sideValuePaint)
                            } else if (ringerMode != 2) {
                                val icon = if (ringerMode == 1) vibrateIconDrawable else muteIconDrawable
                                drawTintedDrawable(canvas, icon ?: soundIconDrawable, compCenterX, sideIconY, sideIconSize, accentColor)
                                val modeText = if (ringerMode == 1) "Vib" else "Mute"
                                canvas.drawText(modeText, compCenterX, sideTextY, sideValuePaint)
                            } else {
                                // Default state: show steps info while keeping the tap action intact
                                drawTintedDrawable(canvas, stepsIconDrawable, compCenterX, sideIconY, sideIconSize, accentColor)
                                canvas.drawText(currentSteps.toString(), compCenterX, sideTextY, sideValuePaint)
                            }
                        }
                        "HEART_RATE" -> {
                            drawTintedDrawable(canvas, heartIconDrawable, compCenterX, sideIconY, sideIconSize, accentColor)
                            val hrText = if (currentHeartRate > 0) currentHeartRate.toString() else "--"
                            canvas.drawText(hrText, compCenterX, sideTextY, sideValuePaint)
                        }
                        "STEPS" -> {
                            drawTintedDrawable(canvas, stepsIconDrawable, compCenterX, sideIconY, sideIconSize, accentColor)
                            val stepsText = currentSteps.toString()
                            canvas.drawText(stepsText, compCenterX, sideTextY, sideValuePaint)
                        }
                        "DISTANCE" -> {
                            drawTintedDrawable(canvas, distanceIconDrawable, compCenterX, sideIconY, sideIconSize, accentColor)
                            val km = (currentSteps * 0.000762f)
                            val distText = if (km >= 10f) String.format(Locale.getDefault(), "%.0fkm", km) else String.format(Locale.getDefault(), "%.1fkm", km)
                            canvas.drawText(distText, compCenterX, sideTextY, sideValuePaint)
                        }
                        "CALORIES" -> {
                            drawTintedDrawable(canvas, fireIconDrawable, compCenterX, sideIconY, sideIconSize, accentColor)
                            val kcal = (currentSteps * 0.04f).toInt()
                            val calText = if (kcal > 0) "${kcal}k" else "--"
                            canvas.drawText(calText, compCenterX, sideTextY, sideValuePaint)
                        }
                        "NOTIFICATIONS" -> {
                            drawTintedDrawable(canvas, notifIconDrawable, compCenterX, sideIconY, sideIconSize, accentColor)
                            val count = try {
                                val notifJson = prefs?.getString("watch_notifications_json", "[]") ?: "[]"
                                org.json.JSONArray(notifJson).length()
                            } catch (_: Exception) { 0 }
                            val notifText = if (count > 0) count.toString() else "0"
                            canvas.drawText(notifText, compCenterX, sideTextY, sideValuePaint)
                        }
                        "NOW_PLAYING" -> {
                            drawTintedDrawable(canvas, musicIconDrawable, compCenterX, sideIconY, sideIconSize, accentColor)
                            canvas.drawText("▶", compCenterX, sideTextY, sideValuePaint)
                        }
                        "TRAVEL" -> {
                            drawTintedDrawable(canvas, travelIconDrawable, compCenterX, sideIconY, sideIconSize, accentColor)
                            val travelActive = prefs?.getBoolean("phone_travel_active", false) ?: false
                            val travelTime = prefs?.getString("phone_travel_remaining_time", "") ?: ""
                            val travelDist = prefs?.getString("phone_travel_remaining_distance", "") ?: ""
                            val travelText = when {
                                !travelActive -> "--"
                                travelTime.isNotBlank() -> travelTime
                                travelDist.isNotBlank() -> travelDist
                                else -> "ETA"
                            }
                            canvas.drawText(travelText, compCenterX, sideTextY, sideValuePaint)
                        }
                        "SOUND_MODE" -> {
                            drawTintedDrawable(canvas, soundIconDrawable, compCenterX, sideIconY, sideIconSize, accentColor)
                            val ringerMode = prefs?.getInt("phone_ringer_mode", 2) ?: 2
                            val modeText = when (ringerMode) {
                                0 -> "Mute"
                                1 -> "Vib"
                                else -> "Ring"
                            }
                            canvas.drawText(modeText, compCenterX, sideTextY, sideValuePaint)
                        }
                        "WATCH_BATTERY" -> {
                            drawTintedDrawable(canvas, watchIconDrawable, compCenterX, sideIconY, sideIconSize, accentColor)
                            val watchBat = getWatchBatteryLevel()
                            val batText = if (watchBat >= 0) "${watchBat}%" else "--"
                            canvas.drawText(batText, compCenterX, sideTextY, sideValuePaint)
                        }
                        "PHONE_BATTERY" -> {
                            drawTintedDrawable(canvas, mobileIconDrawable, compCenterX, sideIconY, sideIconSize, accentColor)
                            val phoneBat = getPhoneBatteryLevel()
                            val batText = if (phoneBat >= 0) "${phoneBat}%" else "--"
                            canvas.drawText(batText, compCenterX, sideTextY, sideValuePaint)
                        }
                    }
                }

                val leftSideCenterX = width * 0.12f
                drawComplication(leftComplicationType, leftSideCenterX)

                val rightSideCenterX = width * 0.88f
                drawComplication(rightComplicationType, rightSideCenterX)
            }

            // Draw Top Curved Text: Next upcoming meeting for today, travel ETA, battery alert, or next alarm
            if (topInfo != null) {
                val accentColor = getClockColor()
                topEventPaint.textSize = height * 0.065f
                val topRadius = height * 0.43f
                val topPathRect = RectF(
                    centerX - topRadius,
                    centerY - topRadius,
                    centerX + topRadius,
                    centerY + topRadius
                )
                val topPath = Path().apply {
                    addArc(topPathRect, 210f, 120f)
                }
                canvas.drawTextOnPath(topInfo.text, topPath, 0f, 0f, topEventPaint)

                val topIcon = topInfo.iconDrawable ?: calendarIconDrawable
                val topIconSize = (height * 0.065f).toInt()
                val topIconY = centerY - (height * 0.35f)
                drawTintedDrawable(canvas, topIcon, centerX, topIconY, topIconSize, accentColor)
            }
        }

        // Draw Centered Clock Text
        val hourBounds = Rect()
        val minuteBounds = Rect()
        activePaint.getTextBounds(hourText, 0, hourText.length, hourBounds)
        activePaint.getTextBounds(minuteText, 0, minuteText.length, minuteBounds)

        val hourCapHeight = -hourBounds.top.toFloat()
        val minuteCapHeight = -minuteBounds.top.toFloat()
        val lineGap = height * 0.015f

        val totalClockHeight = hourCapHeight + lineGap + minuteCapHeight
        val clockTop = centerY - (totalClockHeight / 2f)

        val hourY = clockTop + hourCapHeight
        val minuteY = hourY + lineGap + minuteCapHeight

        val defaultClockColor = getClockColor()
        if (clockDualTone) {
            activePaint.color = Color.WHITE
            canvas.drawText(hourText, centerX, hourY, activePaint)
            activePaint.color = defaultClockColor
            canvas.drawText(minuteText, centerX, minuteY, activePaint)
        } else {
            activePaint.color = defaultClockColor
            canvas.drawText(hourText, centerX, hourY, activePaint)
            canvas.drawText(minuteText, centerX, minuteY, activePaint)
        }

        // Draw system complication slots (rendered in ambient only if enabled)
        val showComplicationsOnAod = sharedPrefs?.getBoolean("watchface_complications_on_aod", true) ?: true
        if (!isAmbient || showComplicationsOnAod) {
            for ((_, complication) in complicationSlotsManager.complicationSlots) {
                if (complication.enabled) {
                    complication.render(canvas, zonedDateTime, renderParameters)
                }
            }
        }
    }

    private fun getUpcomingMeetingOrAlarm(): TopScheduleInfo? {
        val now = System.currentTimeMillis()
        val prefs = context.getSharedPreferences("schedule_prefs", Context.MODE_PRIVATE)

        // Individual At a Glance complication toggles
        val showFlashlight = prefs.getBoolean("watchface_glance_flashlight", true)
        val showBatteryAlerts = prefs.getBoolean("watchface_glance_battery_alerts", true)
        val showTravel = prefs.getBoolean("watchface_glance_travel", true)
        val showEvents = prefs.getBoolean("watchface_glance_events", true)
        val showAlarm = prefs.getBoolean("watchface_glance_alarm", true)

        // 0. Flashlight on phone takes highest priority
        if (showFlashlight) {
            val flashlightOn = prefs.getBoolean("phone_flashlight_on", false)
            if (flashlightOn) {
                return TopScheduleInfo(
                    GlanceType.FLASHLIGHT,
                    "Flashlight on",
                    flashlightIconDrawable,
                    remainingMinutes = 0L,
                    glowMaxMinutes = 60L,
                    glowPeakMinutes = 0L
                )
            }
        }

        // 1. Check for upcoming calendar event starting within 30 minutes
        var earlyUpcomingMeeting: TopScheduleInfo? = null
        if (showEvents) {
            try {
                val cal = Calendar.getInstance().apply { timeInMillis = now }
                cal.set(Calendar.HOUR_OF_DAY, 23)
                cal.set(Calendar.MINUTE, 59)
                cal.set(Calendar.SECOND, 59)
                cal.set(Calendar.MILLISECOND, 999)
                val endOfToday = cal.timeInMillis

                val eventList = com.sameerasw.essentials.tile.MainTileService.getSyncedEvents(context)
                var earliestEventTitle: String? = null
                var earliestBegin = Long.MAX_VALUE

                for (event in eventList) {
                    val begin = event.begin
                    val end = event.end
                    val title = event.title ?: ""
                    val allDay = event.allDay

                    if (!allDay && begin > now && begin <= endOfToday && begin < earliestBegin) {
                        earliestBegin = begin
                        earliestEventTitle = title
                    } else if (!allDay && now in begin..end && begin < earliestBegin) {
                        earliestBegin = begin
                        earliestEventTitle = title
                    }
                }

                if (earliestEventTitle != null) {
                    val diffMs = earliestBegin - now
                    val diffMinutes = if (diffMs <= 0) 0L else (diffMs / 60000L).coerceAtLeast(1L)
                    val timeStr = if (diffMs <= 0) {
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
                    val cleanTitle = if (earliestEventTitle.length > 20) earliestEventTitle.take(19) + "…" else earliestEventTitle
                    earlyUpcomingMeeting = TopScheduleInfo(
                        GlanceType.EVENT,
                        "$cleanTitle $timeStr",
                        calendarIconDrawable,
                        diffMinutes,
                        glowMaxMinutes = 120L,
                        glowPeakMinutes = 15L
                    )
                }
            } catch (_: Exception) { }
        }

        if (earlyUpcomingMeeting != null && earlyUpcomingMeeting.remainingMinutes != null && earlyUpcomingMeeting.remainingMinutes in 1..30) {
            return earlyUpcomingMeeting
        }

        if (showBatteryAlerts) {
            val watchBat = getWatchBatteryLevel()
            val phoneBat = getPhoneBatteryLevel()
            val isPhoneCharging = prefs.getBoolean("phone_is_charging", false)
            val isWatchCharging = try {
                val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
                val status = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_STATUS) ?: -1
                status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
            } catch (_: Exception) { false }

            val watchLow = watchBat in 1..19 && !isWatchCharging
            val phoneLow = phoneBat in 1..19 && !isPhoneCharging

            if (watchLow && phoneLow) {
                return TopScheduleInfo(
                    GlanceType.BATTERY_ALERT,
                    "Watch & phone battery low",
                    batteryAlertIconDrawable,
                    remainingMinutes = 0L,
                    glowMaxMinutes = 60L,
                    glowPeakMinutes = 0L
                )
            } else if (watchLow) {
                return TopScheduleInfo(
                    GlanceType.BATTERY_ALERT,
                    "Watch battery low",
                    batteryAlertIconDrawable,
                    remainingMinutes = 0L,
                    glowMaxMinutes = 60L,
                    glowPeakMinutes = 0L
                )
            } else if (phoneLow) {
                return TopScheduleInfo(
                    GlanceType.BATTERY_ALERT,
                    "Phone battery low",
                    batteryAlertIconDrawable,
                    remainingMinutes = 0L,
                    glowMaxMinutes = 60L,
                    glowPeakMinutes = 0L
                )
            }
        }

        if (showTravel) {
            val travelActive = prefs.getBoolean("phone_travel_active", false)
            val syncLocationReachedEnabled = prefs.getBoolean("phone_watch_sync_location_reached_enabled", true)
            if (travelActive && syncLocationReachedEnabled) {
                val travelName = prefs.getString("phone_travel_name", "") ?: ""
                val travelRemainingTime = prefs.getString("phone_travel_remaining_time", "") ?: ""
                val travelRemainingDistance = prefs.getString("phone_travel_remaining_distance", "") ?: ""

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

                return TopScheduleInfo(
                    GlanceType.TRAVEL,
                    travelText,
                    distanceIconDrawable,
                    remainingMinutes = travelMinutes ?: 30L,
                    glowMaxMinutes = 60L,
                    glowPeakMinutes = 20L
                )
            }
        }

        if (earlyUpcomingMeeting != null) {
            return earlyUpcomingMeeting
        }

        if (showAlarm) {
            try {
                val am = context.getSystemService(Context.ALARM_SERVICE) as? android.app.AlarmManager
                val nextAlarm = am?.nextAlarmClock
                if (nextAlarm != null && nextAlarm.triggerTime > now) {
                    val alarmCal = Calendar.getInstance().apply { timeInMillis = nextAlarm.triggerTime }
                    val diffMs = nextAlarm.triggerTime - now
                    val diffMinutes = (diffMs / 60000L).coerceAtLeast(1L)
                    val is24 = android.text.format.DateFormat.is24HourFormat(context)
                    val timeStr = if (is24) {
                        SimpleDateFormat("HH:mm", Locale.getDefault()).format(alarmCal.time)
                    } else {
                        val h = alarmCal.get(Calendar.HOUR).let { if (it == 0) 12 else it }
                        val m = alarmCal.get(Calendar.MINUTE)
                        val amPm = if (alarmCal.get(Calendar.AM_PM) == Calendar.AM) "AM" else "PM"
                        String.format(Locale.getDefault(), "%d:%02d %s", h, m, amPm)
                    }
                    return TopScheduleInfo(
                        GlanceType.ALARM,
                        timeStr,
                        alarmIconDrawable,
                        diffMinutes,
                        glowMaxMinutes = 120L,
                        glowPeakMinutes = 15L
                    )
                }
            } catch (_: Exception) { }
        }

        return null
    }

    private fun drawTintedDrawable(
        canvas: Canvas,
        drawable: Drawable?,
        cx: Float,
        cy: Float,
        size: Int,
        color: Int,
        rotationDegrees: Float = 0f
    ) {
        drawable ?: return
        drawable.colorFilter = PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN)
        val half = size / 2
        drawable.setBounds(-half, -half, half, half)
        canvas.save()
        canvas.translate(cx, cy)
        if (rotationDegrees != 0f) {
            canvas.rotate(rotationDegrees)
        }
        drawable.draw(canvas)
        canvas.restore()
    }

    private fun getWatchBatteryLevel(): Int {
        return try {
            val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
            bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: 100
        } catch (_: Exception) {
            100
        }
    }

    private fun getPhoneBatteryLevel(): Int {
        val prefs = context.getSharedPreferences("schedule_prefs", Context.MODE_PRIVATE)
        val level = prefs.getInt("phone_battery_level", -1)
        return if (level >= 0) level else 0
    }
}

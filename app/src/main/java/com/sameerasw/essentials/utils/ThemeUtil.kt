package com.sameerasw.essentials.utils

import android.content.Context
import androidx.core.graphics.ColorUtils

object ThemeUtil {
    fun getThemeColor(context: Context): Int? {
        val prefs = context.getSharedPreferences("schedule_prefs", Context.MODE_PRIVATE)
        val color = prefs.getInt("theme_primary_color", -1)
        return if (color != -1) color else null
    }

    fun getTonedColor(baseColor: Int): Int {
        val hsl = FloatArray(3)
        ColorUtils.colorToHSL(baseColor, hsl)
        
        // Darken for card background: Lightness around 25% for a nice visible toned look
        hsl[2] = 0.25f 
        // Also slightly desaturate for a more premium "dark mode" feel
        hsl[1] = (hsl[1] * 0.8f).coerceIn(0f, 1f)
        
        return ColorUtils.HSLToColor(hsl)
    }

    fun getLightAccentColor(baseColor: Int): Int {
        val hsl = FloatArray(3)
        ColorUtils.colorToHSL(baseColor, hsl)
        
        // Increase lightness for better visibility on dark backgrounds
        hsl[2] = (hsl[2] + 0.3f).coerceIn(0f, 1f)
        // Ensure some saturation remains
        hsl[1] = (hsl[1] + 0.2f).coerceIn(0f, 1f)
        
        return ColorUtils.HSLToColor(hsl)
    }

    fun getTimeCountdown(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = timestamp - now
        
        if (diff <= 0) return "now"
        
        val minutes = diff / (1000 * 60)
        val hours = minutes / 60
        val days = hours / 24
        
        return when {
            days > 0 -> "in ${days}d"
            hours > 0 -> "in ${hours}h"
            else -> "in ${minutes}m"
        }
    }
}

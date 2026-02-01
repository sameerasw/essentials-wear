package com.sameerasw.essentials.utils

import android.view.HapticFeedbackConstants
import android.view.View

object HapticUtil {
    fun performUIHaptic(view: View) {
        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
    }

    fun performLightHaptic(view: View) {
        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
    }
}

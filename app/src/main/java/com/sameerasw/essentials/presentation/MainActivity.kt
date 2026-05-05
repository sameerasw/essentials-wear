package com.sameerasw.essentials.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import com.sameerasw.essentials.presentation.home.HomeScreen
import com.sameerasw.essentials.presentation.navigation.NavRoutes
import com.sameerasw.essentials.presentation.schedule.ScheduleScreen
import com.sameerasw.essentials.presentation.theme.EssentialsTheme
import com.sameerasw.essentials.presentation.yourandroid.YourAndroidScreen

class MainActivity : ComponentActivity() {

    companion object {
        const val EXTRA_NAVIGATE_TO = "navigate_to"
        const val NAV_SCHEDULE = "schedule"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setTheme(android.R.style.Theme_DeviceDefault)

        val navigateTo = intent?.getStringExtra(EXTRA_NAVIGATE_TO)

        setContent {
            WearApp(initialScreen = navigateTo)
        }
    }
}

@Composable
fun WearApp(initialScreen: String? = null) {
    val startDestination =
        if (initialScreen == MainActivity.NAV_SCHEDULE) NavRoutes.SCHEDULE else NavRoutes.HOME
    val navController = rememberSwipeDismissableNavController()

    EssentialsTheme {
        SwipeDismissableNavHost(
            navController = navController,
            startDestination = startDestination
        ) {
            composable(NavRoutes.HOME) {
                HomeScreen(
                    onNavigate = { route -> navController.navigate(route) }
                )
            }
            composable(NavRoutes.SCHEDULE) {
                ScheduleScreen()
            }
            composable(NavRoutes.YOUR_ANDROID) {
                YourAndroidScreen()
            }
        }
    }
}
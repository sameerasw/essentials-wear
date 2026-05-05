package com.sameerasw.essentials.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.Icon
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import androidx.wear.tooling.preview.devices.WearDevices
import com.sameerasw.essentials.R
import com.sameerasw.essentials.presentation.theme.EssentialsTheme
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.foundation.lazy.AutoCenteringParams
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val timeFormatter = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())

object NavRoutes {
    const val HOME = "home"
    const val SCHEDULE = "schedule"
}

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
    val startDestination = if (initialScreen == MainActivity.NAV_SCHEDULE) NavRoutes.SCHEDULE else NavRoutes.HOME
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
        }
    }
}

@Composable
fun HomeScreen(onNavigate: (String) -> Unit) {
    val view = androidx.compose.ui.platform.LocalView.current
    val context = androidx.compose.ui.platform.LocalContext.current
    val themeColor = androidx.compose.runtime.remember {
        com.sameerasw.essentials.utils.ThemeUtil.getThemeColor(context)
    }
    val tonedThemeColor = themeColor?.let {
        androidx.compose.ui.graphics.Color(com.sameerasw.essentials.utils.ThemeUtil.getTonedColor(it))
    } ?: androidx.compose.ui.graphics.Color.DarkGray
    val lightAccentColor = themeColor?.let {
        androidx.compose.ui.graphics.Color(com.sameerasw.essentials.utils.ThemeUtil.getLightAccentColor(it))
    } ?: androidx.compose.ui.graphics.Color(0xFFB39DDB.toInt())

    val listState = rememberScalingLazyListState()

    Scaffold(
        timeText = { TimeText() }
    ) {
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState,
            autoCentering = AutoCenteringParams(itemIndex = 0)
        ) {
        item {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.title1,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                color = lightAccentColor
            )
        }

        // Schedule
        item {
            Chip(
                onClick = {
                    com.sameerasw.essentials.utils.HapticUtil.performUIHaptic(view)
                    onNavigate(NavRoutes.SCHEDULE)
                },
                label = { Text(stringResource(R.string.feature_schedule)) },
                icon = {
                    Icon(
                        painter = painterResource(R.drawable.rounded_schedule_24),
                        contentDescription = null,
                        modifier = Modifier.size(ChipDefaults.IconSize).wrapContentSize(align = Alignment.Center)
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ChipDefaults.secondaryChipColors(
                    backgroundColor = tonedThemeColor,
                    contentColor = androidx.compose.ui.graphics.Color.White
                )
            )
        }

        // Placeholder: Your Android
        item {
            Chip(
                onClick = {
                    com.sameerasw.essentials.utils.HapticUtil.performUIHaptic(view)
                },
                label = { Text(stringResource(R.string.feature_your_android)) },
                secondaryLabel = { Text(stringResource(R.string.coming_soon)) },
                icon = {
                    Icon(
                        painter = painterResource(R.drawable.rounded_mobile_24),
                        contentDescription = null,
                        modifier = Modifier.size(ChipDefaults.IconSize).wrapContentSize(align = Alignment.Center)
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ChipDefaults.secondaryChipColors(
                    contentColor = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.5f),
                    secondaryContentColor = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.3f)
                ),
                enabled = false
            )
        }

        // Placeholder: Tools
        item {
            Chip(
                onClick = {
                    com.sameerasw.essentials.utils.HapticUtil.performUIHaptic(view)
                },
                label = { Text(stringResource(R.string.feature_tools)) },
                secondaryLabel = { Text(stringResource(R.string.coming_soon)) },
                icon = {
                    Icon(
                        painter = painterResource(R.drawable.rounded_settings_heart_24),
                        contentDescription = null,
                        modifier = Modifier.size(ChipDefaults.IconSize).wrapContentSize(align = Alignment.Center)
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ChipDefaults.secondaryChipColors(
                    contentColor = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.5f),
                    secondaryContentColor = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.3f)
                ),
                enabled = false
            )
        }
        }
    }
}

@Composable
fun ScheduleScreen() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val allEvents = androidx.compose.runtime.remember {
        com.sameerasw.essentials.tile.MainTileService.getSyncedEvents(context)
    }
    val themeColor = androidx.compose.runtime.remember {
        com.sameerasw.essentials.utils.ThemeUtil.getThemeColor(context)
    }

    val groupedEvents = androidx.compose.runtime.remember(allEvents) {
        val dateFormatter = SimpleDateFormat("EEE, d MMM", Locale.getDefault())
        allEvents.groupBy { dateFormatter.format(Date(it.begin)) }
    }

    val listState = rememberScalingLazyListState()

    Scaffold(
        timeText = { TimeText() }
    ) {
        if (groupedEvents.isEmpty()) {
            Greeting("No upcoming events")
        } else {
            ScalingLazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState,
            autoCentering = AutoCenteringParams(itemIndex = 0)
        ) {
            item {
                Text(
                    text = stringResource(R.string.upcoming_agenda),
                    style = MaterialTheme.typography.title3,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }

            groupedEvents.forEach { (date, eventsInDate) ->
                item {
                    Text(
                        text = date,
                        style = MaterialTheme.typography.caption1,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp, bottom = 4.dp),
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colors.onSurfaceVariant
                    )
                }

                items(eventsInDate) { event ->
                    val view = androidx.compose.ui.platform.LocalView.current
                    val tonedThemeColor = themeColor?.let { androidx.compose.ui.graphics.Color(com.sameerasw.essentials.utils.ThemeUtil.getTonedColor(it)) } ?: androidx.compose.ui.graphics.Color.DarkGray

                    Chip(
                        onClick = {
                            com.sameerasw.essentials.utils.HapticUtil.performUIHaptic(view)
                        },
                        label = { Text(event.title ?: "No Title") },
                        secondaryLabel = {
                            Text(
                                if (event.allDay) "All day"
                                else com.sameerasw.essentials.utils.ThemeUtil.getTimeCountdown(event.begin)
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(
                                if (event.allDay) {
                                    Modifier.border(
                                        width = 1.dp,
                                        color = tonedThemeColor,
                                        shape = MaterialTheme.shapes.large
                                    )
                                } else Modifier
                            ),
                        colors = if (event.allDay) {
                            ChipDefaults.secondaryChipColors(
                                backgroundColor = androidx.compose.ui.graphics.Color.Transparent,
                                contentColor = androidx.compose.ui.graphics.Color.White,
                                secondaryContentColor = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.7f)
                            )
                        } else {
                            ChipDefaults.secondaryChipColors(
                                backgroundColor = tonedThemeColor,
                                contentColor = androidx.compose.ui.graphics.Color.White,
                                secondaryContentColor = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.7f)
                            )
                        }
                    )
                }
            }
            }
        }
    }
}

private fun formatTime(timestamp: Long): String {
    return timeFormatter.format(Date(timestamp))
}

@Composable
fun Greeting(greetingName: String) {
    Text(
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Center,
        color = androidx.compose.ui.graphics.Color(com.sameerasw.essentials.utils.ThemeUtil.getThemeColor(androidx.compose.ui.platform.LocalContext.current) ?: 0xFFB39DDB.toInt()),
        text = stringResource(R.string.hello_world, greetingName)
    )
}

@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true)
@Composable
fun DefaultPreview() {
    WearApp()
}
/* While this template provides a good starting point for using Wear Compose, you can always
 * take a look at https://github.com/android/wear-os-samples/tree/main/ComposeStarter to find the
 * most up to date changes to the libraries and their usages.
 */

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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.tooling.preview.devices.WearDevices
import com.sameerasw.essentials.R
import com.sameerasw.essentials.presentation.theme.EssentialsTheme
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.foundation.lazy.AutoCenteringParams
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val timeFormatter = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()

        super.onCreate(savedInstanceState)

        setTheme(android.R.style.Theme_DeviceDefault)

        setContent {
            WearApp()
        }
    }
}

@Composable
fun WearApp() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val allEvents = androidx.compose.runtime.remember { 
        com.sameerasw.essentials.tile.MainTileService.getSyncedEvents(context)
    }
    val themeColor = androidx.compose.runtime.remember {
        com.sameerasw.essentials.utils.ThemeUtil.getThemeColor(context)
    }
    
    // Group events by date
    val groupedEvents = androidx.compose.runtime.remember(allEvents) {
        val dateFormatter = SimpleDateFormat("EEE, d MMM", Locale.getDefault())
        allEvents.groupBy { dateFormatter.format(Date(it.begin)) }
    }
    
    val listState = rememberScalingLazyListState()

    EssentialsTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colors.background),
            contentAlignment = Alignment.Center
        ) {
            TimeText()
            
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
package com.sameerasw.essentials.watchface

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Switch
import androidx.wear.compose.material.SwitchDefaults
import androidx.wear.compose.material.Text
import androidx.wear.watchface.editor.EditorSession
import com.sameerasw.essentials.R
import com.sameerasw.essentials.utils.HapticUtil
import kotlinx.coroutines.launch

class EssentialsWatchFaceConfigActivity : ComponentActivity() {

    private var editorSession: EditorSession? by mutableStateOf(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            try {
                editorSession = EditorSession.createOnWatchEditorSession(this@EssentialsWatchFaceConfigActivity)
            } catch (e: Exception) {
                Log.e("WatchFaceConfig", "Failed to create EditorSession", e)
            }
        }

        setContent {
            WatchFaceEditorScreen(
                editorSession = editorSession,
                onClose = {
                    finish()
                }
            )
        }
    }
}

@Composable
fun WatchFaceEditorScreen(
    editorSession: EditorSession?,
    onClose: () -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { 2 })

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            when (page) {
                0 -> ComplicationsPage(
                    editorSession = editorSession,
                    onClose = onClose
                )
                1 -> AodSettingsPage(
                    onClose = onClose
                )
            }
        }

        // Page Indicator dots
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            repeat(2) { index ->
                Box(
                    modifier = Modifier
                        .size(if (pagerState.currentPage == index) 5.dp else 3.dp)
                        .background(
                            color = if (pagerState.currentPage == index) Color.White else Color(0x66FFFFFF),
                            shape = CircleShape
                        )
                )
            }
        }
    }
}

@Composable
private fun ComplicationsPage(
    editorSession: EditorSession?,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = stringResource(R.string.editor_tab_complications),
            style = MaterialTheme.typography.title3.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            ),
            color = Color.White,
            modifier = Modifier.padding(top = 10.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Complication Slot Button
            ComplicationSlotButton(
                contentDescription = stringResource(R.string.left_complication_label),
                onClick = {
                    HapticUtil.performClick(context)
                    editorSession?.let { session ->
                        coroutineScope.launch {
                            session.openComplicationDataSourceChooser(EssentialsWatchFaceService.LEFT_COMPLICATION_ID)
                        }
                    }
                }
            )

            // Right Complication Slot Button
            ComplicationSlotButton(
                contentDescription = stringResource(R.string.right_complication_label),
                onClick = {
                    HapticUtil.performClick(context)
                    editorSession?.let { session ->
                        coroutineScope.launch {
                            session.openComplicationDataSourceChooser(EssentialsWatchFaceService.RIGHT_COMPLICATION_ID)
                        }
                    }
                }
            )
        }

        // Done Button
        Box(
            modifier = Modifier
                .padding(bottom = 12.dp)
                .background(
                    color = Color(0xFF2C2C2C),
                    shape = RoundedCornerShape(20.dp)
                )
                .clickable {
                    HapticUtil.performClick(context)
                    onClose()
                }
                .padding(horizontal = 20.dp, vertical = 5.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.action_done),
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun AodSettingsPage(
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("schedule_prefs", Context.MODE_PRIVATE) }
    var complicationsOnAod by remember {
        mutableStateOf(prefs.getBoolean("watchface_complications_on_aod", true))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = stringResource(R.string.editor_tab_aod),
            style = MaterialTheme.typography.title3.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            ),
            color = Color.White,
            modifier = Modifier.padding(top = 10.dp)
        )

        // Toggle Container
        Box(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .background(
                    color = Color(0xFF1E1E1E),
                    shape = RoundedCornerShape(16.dp)
                )
                .clickable {
                    HapticUtil.performClick(context)
                    val newValue = !complicationsOnAod
                    complicationsOnAod = newValue
                    prefs.edit().putBoolean("watchface_complications_on_aod", newValue).apply()
                    context.sendBroadcast(Intent("com.sameerasw.essentials.WATCHFACE_PREFS_CHANGED").apply {
                        setPackage(context.packageName)
                    })
                }
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.watchface_complications_on_aod_title),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )
                    Text(
                        text = stringResource(R.string.watchface_complications_on_aod_desc),
                        fontSize = 10.sp,
                        color = Color(0xFFAAAAAA),
                        lineHeight = 12.sp
                    )
                }
                Switch(
                    checked = complicationsOnAod,
                    onCheckedChange = { newValue ->
                        HapticUtil.performClick(context)
                        complicationsOnAod = newValue
                        prefs.edit().putBoolean("watchface_complications_on_aod", newValue).apply()
                        context.sendBroadcast(Intent("com.sameerasw.essentials.WATCHFACE_PREFS_CHANGED").apply {
                            setPackage(context.packageName)
                        })
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = MaterialTheme.colors.primary
                    )
                )
            }
        }

        // Done Button
        Box(
            modifier = Modifier
                .padding(bottom = 12.dp)
                .background(
                    color = Color(0xFF2C2C2C),
                    shape = RoundedCornerShape(20.dp)
                )
                .clickable {
                    HapticUtil.performClick(context)
                    onClose()
                }
                .padding(horizontal = 20.dp, vertical = 5.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.action_done),
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun ComplicationSlotButton(
    contentDescription: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(56.dp)
            .background(
                color = Color(0xFF222222),
                shape = CircleShape
            )
            .clickable {
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(id = R.drawable.rounded_blur_circular_24),
            contentDescription = contentDescription,
            tint = Color.White,
            modifier = Modifier.size(26.dp)
        )
    }
}

package com.sameerasw.essentials.presentation.launcher

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Scaffold
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import com.sameerasw.essentials.presentation.theme.EssentialsTheme
import com.sameerasw.essentials.presentation.theme.GoogleSansFlexRounded
import com.sameerasw.essentials.utils.HapticUtil
import com.sameerasw.essentials.utils.ThemeUtil
import kotlinx.coroutines.flow.distinctUntilChanged

class AppLauncherActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setTheme(android.R.style.Theme_DeviceDefault)
        setContent {
            EssentialsTheme {
                AppLauncherScreen()
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_STEM_1 || keyCode == KeyEvent.KEYCODE_HOME) {
            // Crown press returns to Home (LauncherActivity)
            finish()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }
}

data class AppInfo(
    val packageName: String,
    val icon: Drawable,
    val launchIntent: Intent?
)

@Composable
fun AppLauncherScreen() {
    val context = LocalContext.current
    val view = LocalView.current
    val pm = context.packageManager

    // Theme integration
    val themeColor = remember { ThemeUtil.getThemeColor(context) }
    val tonedColor = themeColor?.let { Color(ThemeUtil.getTonedColor(it)) } ?: Color(0xFF1E1E1E)
    
    // Bumped on install/uninstall so the list below is re-queried
    val refresh = remember { mutableIntStateOf(0) }
    DisposableEffect(Unit) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context, i: Intent) { refresh.intValue++ }
        }
        context.registerReceiver(receiver, IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addDataScheme("package")
        })
        onDispose { context.unregisterReceiver(receiver) }
    }

    val apps = remember(refresh.intValue) {
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        pm.queryIntentActivities(intent, 0).mapNotNull { resolveInfo ->
            val packageName = resolveInfo.activityInfo.packageName
            val icon = resolveInfo.loadIcon(pm)
            val launchIntent = pm.getLaunchIntentForPackage(packageName)
            if (launchIntent != null) AppInfo(packageName, icon, launchIntent) else null
        }.sortedBy { it.packageName }
    }

    val listState = rememberScalingLazyListState()

    // Haptic feedback for scrolling
    LaunchedEffect(listState) {
        snapshotFlow { listState.centerItemIndex }
            .distinctUntilChanged()
            .collect {
                HapticUtil.performSubtleTick(view)
            }
    }

    Scaffold {
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState,
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 40.dp)
        ) {
            // Group into rows of 3 to create a grid effect
            val rows = apps.chunked(3)
            items(rows.size) { rowIndex ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp), // Reduced vertical spacing
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally) // Reduced horizontal spacing
                ) {
                    rows[rowIndex].forEach { app ->
                        AppIconItem(app, tonedColor)
                    }
                }
            }
        }
    }
}

@Composable
fun AppIconItem(app: AppInfo, backgroundColor: Color) {
    val context = LocalContext.current
    val view = LocalView.current
    
    Box(
        modifier = Modifier
            .size(52.dp)
            .clip(CircleShape)
            .background(backgroundColor)
            .clickable {
                HapticUtil.performUIHaptic(view)
                try {
                    app.launchIntent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(app.launchIntent)
                } catch (e: Exception) {}
            },
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = rememberDrawablePainter(drawable = app.icon),
            contentDescription = null,
            modifier = Modifier.size(36.dp)
        )
    }
}

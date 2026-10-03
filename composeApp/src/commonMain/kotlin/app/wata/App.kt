package app.wata

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import app.wata.data.WaterRepository
import app.wata.ui.AppPlatform
import app.wata.ui.HomeScreen
import app.wata.ui.SettingsSheet
import app.wata.ui.WataTheme
import kotlinx.coroutines.delay
import kotlin.time.Clock

@Composable
fun App(repository: WaterRepository, platform: AppPlatform) {
    WataTheme {
        val state by repository.state.collectAsState()
        var now by remember { mutableStateOf(Clock.System.now()) }
        var showSettings by rememberSaveable { mutableStateOf(false) }

        // Keep time-based labels fresh and roll over to a new day while the app stays open.
        LaunchedEffect(Unit) {
            while (true) {
                delay(60_000)
                now = Clock.System.now()
                repository.refresh()
            }
        }

        HomeScreen(
            state = state,
            now = now,
            platform = platform,
            onAdd = repository::addDrink,
            onUndo = repository::undoLastDrink,
            onOpenSettings = { showSettings = true },
        )
        if (showSettings) {
            SettingsSheet(
                settings = state.settings,
                platform = platform,
                onChange = repository::updateSettings,
                onDismiss = { showSettings = false },
            )
        }
    }
}

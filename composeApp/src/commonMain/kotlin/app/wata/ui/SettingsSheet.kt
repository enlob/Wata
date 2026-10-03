package app.wata.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderColors
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.wata.data.WaterSettings
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val Intervals = listOf(30, 45, 60, 90, 120)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(
    settings: WaterSettings,
    platform: AppPlatform,
    onChange: ((WaterSettings) -> WaterSettings) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = LocalWataColors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.card,
        contentColor = colors.ink,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Settings", fontSize = 24.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                TextButton(onClick = { scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() } }) {
                    Text("Done", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(Modifier.height(20.dp))
            var goal by remember(settings.dailyGoalMl) { mutableFloatStateOf(settings.dailyGoalMl.toFloat()) }
            LabelRow("Daily goal", "${goal.roundToInt()} ml")
            Slider(
                value = goal,
                onValueChange = { goal = it },
                onValueChangeFinished = { onChange { it.copy(dailyGoalMl = goal.roundToInt()) } },
                valueRange = 1000f..4000f,
                steps = 29,
                colors = sliderColors(),
            )
            Hint("About ${(goal / 250f).roundToInt()} glasses of 250 ml")

            Spacer(Modifier.height(28.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Reminders", fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                    Hint("A gentle nudge when it's time to drink")
                }
                Switch(
                    checked = settings.remindersOn,
                    onCheckedChange = { on ->
                        onChange { it.copy(remindersOn = on) }
                        if (on && !platform.notificationsAllowed) platform.requestNotificationAccess()
                    },
                )
            }

            AnimatedVisibility(settings.remindersOn) {
                Column {
                    if (!platform.notificationsAllowed) {
                        Spacer(Modifier.height(16.dp))
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(colors.warning.copy(alpha = 0.12f))
                                .clickable(onClick = platform::requestNotificationAccess)
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(WataIcons.Bell, null, tint = colors.warning, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(10.dp))
                            Text(
                                "Notifications are blocked. Tap to allow them.",
                                color = colors.warning,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    }

                    Spacer(Modifier.height(24.dp))
                    LabelRow("Remind me every", null)
                    Spacer(Modifier.height(12.dp))
                    IntervalPicker(settings.intervalMinutes) { minutes -> onChange { it.copy(intervalMinutes = minutes) } }

                    Spacer(Modifier.height(28.dp))
                    var hours by remember(settings.startHour, settings.endHour) {
                        mutableStateOf(settings.startHour.toFloat()..settings.endHour.toFloat())
                    }
                    val use24h = platform.use24HourClock
                    LabelRow(
                        "Active hours",
                        "${formatHour(hours.start.roundToInt(), use24h)} – ${formatHour(hours.endInclusive.roundToInt(), use24h)}",
                    )
                    RangeSlider(
                        value = hours,
                        onValueChange = { if (it.endInclusive - it.start >= 1f) hours = it },
                        onValueChangeFinished = {
                            onChange { it.copy(startHour = hours.start.roundToInt(), endHour = hours.endInclusive.roundToInt()) }
                        },
                        valueRange = 0f..24f,
                        steps = 23,
                        colors = sliderColors(),
                    )
                    Hint("No reminders outside these hours")
                }
            }
        }
    }
}

@Composable
private fun LabelRow(label: String, value: String?) {
    val colors = LocalWataColors.current
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        if (value != null) Text(value, color = colors.accent, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun Hint(text: String) {
    Text(text, color = LocalWataColors.current.inkMuted, fontSize = 13.sp)
}

@Composable
private fun IntervalPicker(selected: Int, onSelect: (Int) -> Unit) {
    val colors = LocalWataColors.current
    Row(
        Modifier
            .fillMaxWidth()
            .clip(CircleShape)
            .background(colors.ink.copy(alpha = 0.05f))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Intervals.forEach { minutes ->
            val isSelected = minutes == selected
            val background by animateColorAsState(if (isSelected) colors.accent else Color.Transparent)
            val content by animateColorAsState(if (isSelected) MaterialTheme.colorScheme.onPrimary else colors.ink)
            Box(
                Modifier
                    .weight(1f)
                    .clip(CircleShape)
                    .background(background)
                    .clickable { onSelect(minutes) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    formatInterval(minutes),
                    color = content,
                    fontSize = 14.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                )
            }
        }
    }
}

@Composable
private fun sliderColors(): SliderColors {
    val accent = LocalWataColors.current.accent
    return SliderDefaults.colors(
        thumbColor = accent,
        activeTrackColor = accent,
        inactiveTrackColor = accent.copy(alpha = 0.18f),
        activeTickColor = Color.Transparent,
        inactiveTickColor = Color.Transparent,
    )
}

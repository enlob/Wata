package app.wata.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import app.wata.data.Drink
import app.wata.data.WaterState
import app.wata.resources.*
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt
import kotlin.time.Instant

private class Portion(val label: StringResource, val ml: Int, val iconScale: Float)

private val Portions = listOf(
    Portion(Res.string.portion_sip, 100, 0.62f),
    Portion(Res.string.portion_glass, 250, 0.82f),
    Portion(Res.string.portion_bottle, 500, 1f),
)

@Composable
fun HomeScreen(
    state: WaterState,
    now: Instant,
    platform: AppPlatform,
    onAdd: (Int) -> Unit,
    onUndo: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val colors = LocalWataColors.current
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val splash = remember { Animatable(0f) }
    val add: (Int) -> Unit = { ml ->
        haptics.performHapticFeedback(HapticFeedbackType.Confirm)
        onAdd(ml)
        scope.launch {
            splash.snapTo(1f)
            splash.animateTo(0f, tween(1800, easing = LinearOutSlowInEasing))
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(colors.backgroundTop, colors.backgroundBottom))),
    ) {
        BoxWithConstraints(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 24.dp),
        ) {
            val bubble = @Composable { size: Dp ->
                Bubble(state, size, splash = { splash.value })
            }
            val width = maxWidth
            val height = maxHeight
            if (width > height) {
                Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        bubble(min(height * 0.78f, width * 0.4f))
                    }
                    Spacer(Modifier.width(24.dp))
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Header(now, platform, onOpenSettings)
                        Spacer(Modifier.height(16.dp))
                        ProgressMessage(state)
                        Spacer(Modifier.height(12.dp))
                        ReminderStatus(state, now, platform, onOpenSettings)
                        Spacer(Modifier.height(16.dp))
                        QuickAddRow(add)
                        UndoRow(state.lastDrink, platform.use24HourClock, onUndo)
                    }
                }
            } else {
                Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Header(now, platform, onOpenSettings)
                    Spacer(Modifier.weight(1f))
                    bubble(min(width * 0.8f, height * 0.42f))
                    Spacer(Modifier.height(32.dp))
                    ProgressMessage(state)
                    Spacer(Modifier.height(16.dp))
                    ReminderStatus(state, now, platform, onOpenSettings)
                    Spacer(Modifier.weight(1f))
                    QuickAddRow(add)
                    UndoRow(state.lastDrink, platform.use24HourClock, onUndo)
                }
            }
        }
    }
}

@Composable
private fun Header(now: Instant, platform: AppPlatform, onOpenSettings: () -> Unit) {
    val colors = LocalWataColors.current
    val local = now.toLocalDateTime(TimeZone.currentSystemDefault())
    val greeting = when (local.hour) {
        in 5..11 -> Res.string.greeting_morning
        in 12..17 -> Res.string.greeting_afternoon
        in 18..22 -> Res.string.greeting_evening
        else -> Res.string.greeting_night
    }
    Row(Modifier.fillMaxWidth().padding(top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(
                platform.formatDate(local.date).uppercase(),
                color = colors.inkMuted,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.4.sp,
            )
            Text(
                stringResource(greeting),
                color = colors.ink,
                fontSize = 28.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.5).sp,
            )
        }
        CircleIconButton(WataIcons.Settings, stringResource(Res.string.settings), onOpenSettings)
    }
}

@Composable
private fun CircleIconButton(icon: ImageVector, description: String, onClick: () -> Unit) {
    val colors = LocalWataColors.current
    Box(
        Modifier
            .size(46.dp)
            .clip(CircleShape)
            .background(colors.card)
            .border(1.dp, colors.cardBorder, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, description, tint = colors.ink, modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun Bubble(state: WaterState, size: Dp, splash: () -> Float) {
    val shown = remember { Animatable(0f) }
    LaunchedEffect(state.totalMl) {
        shown.animateTo(state.totalMl.toFloat(), tween(900, easing = FastOutSlowInEasing))
    }
    val total = shown.value.roundToInt()
    val colors = LocalWataColors.current
    WaterBubble(state.progress, splash, Modifier.size(size)) { wet ->
        // White text on bright water needs a soft shadow to stay legible.
        val style = if (wet) {
            TextStyle(color = Color.White, shadow = Shadow(colors.waterDeep.copy(alpha = 0.45f), Offset(0f, 2f), 14f))
        } else {
            TextStyle(color = colors.ink)
        }
        val secondary = style.color.copy(alpha = if (wet) 0.9f else 0.7f)
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row {
                Text(
                    "$total",
                    style = style,
                    fontSize = 60.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = (-2).sp,
                    modifier = Modifier.alignByBaseline(),
                )
                Text(
                    "ml",
                    style = style,
                    color = secondary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.alignByBaseline().padding(start = 4.dp),
                )
            }
            Text(stringResource(Res.string.of_goal, state.settings.dailyGoalMl), style = style, color = secondary, fontSize = 15.sp)
        }
    }
}

@Composable
private fun ProgressMessage(state: WaterState) {
    val colors = LocalWataColors.current
    val goal = state.settings.dailyGoalMl
    val remaining = (goal - state.totalMl).coerceAtLeast(0)
    val percent = (state.progress * 100).roundToInt()
    val title = stringResource(
        when {
            state.totalMl == 0 -> Res.string.title_start
            state.goalReached -> Res.string.title_reached
            state.progress < 0.25f -> Res.string.title_good_start
            state.progress < 0.5f -> Res.string.title_keep_going
            state.progress < 0.75f -> Res.string.title_over_halfway
            else -> Res.string.title_almost
        },
    )
    val subtitle = when {
        state.totalMl == 0 -> stringResource(Res.string.subtitle_goal_today, goal)
        state.goalReached -> stringResource(Res.string.subtitle_reached, percent)
        else -> stringResource(Res.string.subtitle_progress, remaining, percent)
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        AnimatedContent(
            targetState = title,
            transitionSpec = { (fadeIn() + slideInVertically { it / 3 }) togetherWith fadeOut() },
            label = "title",
        ) {
            Text(it, color = colors.ink, fontSize = 21.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
        }
        Text(subtitle, color = colors.inkMuted, fontSize = 15.sp, lineHeight = 20.sp, textAlign = TextAlign.Center)
    }
}

@Composable
private fun ReminderStatus(state: WaterState, now: Instant, platform: AppPlatform, onOpenSettings: () -> Unit) {
    val colors = LocalWataColors.current
    val next = state.nextReminder
    val blocked = state.settings.remindersOn && !platform.notificationsAllowed
    val text = when {
        !state.settings.remindersOn || next == null -> stringResource(Res.string.reminders_off)
        blocked -> stringResource(Res.string.allow_notifications)
        else -> {
            val tz = TimeZone.currentSystemDefault()
            val time = formatTime(next, platform.use24HourClock, tz)
            // The planner never schedules further ahead than tomorrow morning.
            val today = next.toLocalDateTime(tz).date == now.toLocalDateTime(tz).date
            stringResource(if (today) Res.string.next_reminder_today else Res.string.next_reminder_tomorrow, time)
        }
    }
    val tint = when {
        blocked -> colors.warning
        state.settings.remindersOn -> colors.accent
        else -> colors.inkMuted
    }
    Row(
        Modifier
            .clip(CircleShape)
            .background(colors.card.copy(alpha = 0.7f))
            .border(1.dp, colors.cardBorder, CircleShape)
            .clickable(onClick = if (blocked) platform::requestNotificationAccess else onOpenSettings)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(WataIcons.Bell, null, tint = tint, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, color = if (blocked) colors.warning else colors.ink, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun QuickAddRow(onAdd: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Portions.forEachIndexed { i, portion ->
            AddButton(portion, primary = i == 1, onClick = { onAdd(portion.ml) }, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun AddButton(portion: Portion, primary: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val colors = LocalWataColors.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.93f else 1f, spring(dampingRatio = 0.45f, stiffness = 700f))
    val shape = RoundedCornerShape(26.dp)
    val content = if (primary) Color.White else colors.ink
    Column(
        modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .then(
                if (primary) {
                    Modifier.shadow(14.dp, shape, ambientColor = colors.waterDeep, spotColor = colors.waterDeep)
                } else {
                    Modifier
                },
            )
            .clip(shape)
            .background(
                if (primary) Brush.verticalGradient(listOf(colors.waterLight, colors.waterDeep)) else SolidColor(colors.card),
            )
            .border(1.dp, if (primary) Color.Transparent else colors.cardBorder, shape)
            .clickable(interactionSource = interaction, indication = ripple(color = content), onClick = onClick)
            .padding(vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(if (primary) Color.White.copy(alpha = 0.22f) else colors.waterLight.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                WataIcons.Drop,
                null,
                tint = if (primary) Color.White else colors.waterDeep,
                modifier = Modifier.size(26.dp * portion.iconScale),
            )
        }
        Spacer(Modifier.height(10.dp))
        Text("+${portion.ml} ml", color = content, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        Text(stringResource(portion.label), color = content.copy(alpha = 0.7f), fontSize = 13.sp)
    }
}

@Composable
private fun UndoRow(last: Drink?, use24h: Boolean, onUndo: () -> Unit) {
    val colors = LocalWataColors.current
    Box(Modifier.fillMaxWidth().height(60.dp), contentAlignment = Alignment.Center) {
        AnimatedContent(targetState = last, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "undo") { drink ->
            if (drink == null) {
                Spacer(Modifier.height(1.dp))
            } else {
                Row(
                    Modifier
                        .clip(CircleShape)
                        .clickable(onClick = onUndo)
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        stringResource(Res.string.last_drink, drink.ml, formatTime(drink.at, use24h)),
                        color = colors.inkMuted,
                        fontSize = 13.sp,
                    )
                    Text("   ·   ", color = colors.inkMuted, fontSize = 13.sp)
                    Icon(WataIcons.Undo, null, tint = colors.accent, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(Res.string.undo), color = colors.accent, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

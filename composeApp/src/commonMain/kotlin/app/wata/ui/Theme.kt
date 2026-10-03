package app.wata.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class WataColors(
    val backgroundTop: Color,
    val backgroundBottom: Color,
    val ink: Color,
    val inkMuted: Color,
    val card: Color,
    val cardBorder: Color,
    val waterLight: Color,
    val waterDeep: Color,
    val waterBack: Color,
    val bubbleFill: Color,
    val bubbleRing: Color,
    val accent: Color,
    val warning: Color,
    val isDark: Boolean,
)

private val Light = WataColors(
    backgroundTop = Color(0xFFF6FBFF),
    backgroundBottom = Color(0xFFDCEDFB),
    ink = Color(0xFF0E2A47),
    inkMuted = Color(0xFF627C95),
    card = Color(0xFFFFFFFF),
    cardBorder = Color(0x0F0E2A47),
    waterLight = Color(0xFF6CD2FF),
    waterDeep = Color(0xFF2F7FEA),
    waterBack = Color(0xFFB4E7FF),
    bubbleFill = Color(0xCCFFFFFF),
    bubbleRing = Color(0x242F7FEA),
    accent = Color(0xFF2F7FEA),
    warning = Color(0xFFD9733B),
    isDark = false,
)

private val Dark = WataColors(
    backgroundTop = Color(0xFF0C1B2C),
    backgroundBottom = Color(0xFF050C16),
    ink = Color(0xFFEAF4FF),
    inkMuted = Color(0xFF8DA5BE),
    card = Color(0xFF13263B),
    cardBorder = Color(0x14FFFFFF),
    waterLight = Color(0xFF55C6FF),
    waterDeep = Color(0xFF2361D3),
    waterBack = Color(0xFF1D527F),
    bubbleFill = Color(0xFF0F2236),
    bubbleRing = Color(0x3355C6FF),
    accent = Color(0xFF6CCFFF),
    warning = Color(0xFFF0A066),
    isDark = true,
)

val LocalWataColors = staticCompositionLocalOf { Light }

@Composable
fun WataTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val c = if (dark) Dark else Light
    val scheme = if (dark) {
        darkColorScheme(
            primary = c.accent, onPrimary = Color(0xFF04233F),
            background = c.backgroundTop, onBackground = c.ink,
            surface = c.card, onSurface = c.ink, onSurfaceVariant = c.inkMuted,
            surfaceContainerLow = c.card, surfaceContainerHigh = c.card,
        )
    } else {
        lightColorScheme(
            primary = c.accent, onPrimary = Color.White,
            background = c.backgroundTop, onBackground = c.ink,
            surface = c.card, onSurface = c.ink, onSurfaceVariant = c.inkMuted,
            surfaceContainerLow = c.card, surfaceContainerHigh = c.card,
        )
    }
    MaterialTheme(colorScheme = scheme) {
        CompositionLocalProvider(LocalWataColors provides c, content = content)
    }
}

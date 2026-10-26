package it.elia.gestionespese.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle

/** Colori dell'app oltre a quelli di Material (palette di SPEC.md §5 e del prototipo). */
@Immutable
data class AppColors(
    val accent: Color,
    val onAccent: Color,
    val accentSoft: Color,
    val spend: Color,
    val spendSoft: Color,
    val goal: Color,
    val goalSoft: Color,
    val goalTint: Color,
    val neg: Color,
    val negSoft: Color,
    val ok: Color,
    val onOk: Color,
    val quick: Color,
    val onQuick: Color,
    val chartIn: Color,
    val chartOut: Color,
    val line: Color,
    val muted: Color,
    val sunk: Color,
)

private val LightAppColors = AppColors(
    accent = Color(0xFF0F6B5C),
    onAccent = Color.White,
    accentSoft = Color(0xFFD8EBE6),
    spend = Color(0xFF2D5EA3),
    spendSoft = Color(0xFFE1E9F6),
    goal = Color(0xFF8A5A00),
    goalSoft = Color(0xFFF5E8CC),
    goalTint = Color(0xFFFBF5E8),
    neg = Color(0xFFB3261E),
    negSoft = Color(0xFFF8DEDB),
    ok = Color(0xFF25804F),
    onOk = Color.White,
    quick = Color(0xFFB4531A),
    onQuick = Color.White,
    chartIn = Color(0xFF1F8A70),
    chartOut = Color(0xFFC46F1C),
    line = Color(0xFFD3DAD7),
    muted = Color(0xFF5B6965),
    sunk = Color(0xFFE7EBE9),
)

private val DarkAppColors = AppColors(
    accent = Color(0xFF4FBFA6),
    onAccent = Color(0xFF0B1714),
    accentSoft = Color(0xFF1D3A33),
    spend = Color(0xFF8DB4EC),
    spendSoft = Color(0xFF1E2C42),
    goal = Color(0xFFE3B55A),
    goalSoft = Color(0xFF3A2F17),
    goalTint = Color(0xFF221F16),
    neg = Color(0xFFF08A80),
    negSoft = Color(0xFF3D1F1C),
    ok = Color(0xFF4CB87E),
    onOk = Color(0xFF08170F),
    quick = Color(0xFFE08A4A),
    onQuick = Color(0xFF1A0F06),
    chartIn = Color(0xFF2FA583),
    chartOut = Color(0xFFCF7F30),
    line = Color(0xFF33403C),
    muted = Color(0xFF9FB0AA),
    sunk = Color(0xFF232D2A),
)

private val LocalAppColors = staticCompositionLocalOf { LightAppColors }

private val LightScheme = lightColorScheme(
    primary = LightAppColors.accent,
    onPrimary = LightAppColors.onAccent,
    primaryContainer = LightAppColors.accentSoft,
    onPrimaryContainer = LightAppColors.accent,
    secondary = LightAppColors.spend,
    tertiary = LightAppColors.goal,
    error = LightAppColors.neg,
    background = Color(0xFFF2F4F3),
    onBackground = Color(0xFF17211E),
    surface = Color(0xFFFCFCFB),
    onSurface = Color(0xFF17211E),
    surfaceVariant = LightAppColors.sunk,
    onSurfaceVariant = LightAppColors.muted,
    surfaceContainerLow = Color(0xFFFCFCFB),
    surfaceContainer = Color(0xFFF7F8F7),
    surfaceContainerHigh = Color(0xFFFCFCFB),
    outline = Color(0xFF8A9692),
    outlineVariant = LightAppColors.line,
)

private val DarkScheme = darkColorScheme(
    primary = DarkAppColors.accent,
    onPrimary = DarkAppColors.onAccent,
    primaryContainer = DarkAppColors.accentSoft,
    onPrimaryContainer = DarkAppColors.accent,
    secondary = DarkAppColors.spend,
    tertiary = DarkAppColors.goal,
    error = DarkAppColors.neg,
    background = Color(0xFF121816),
    onBackground = Color(0xFFE6ECE9),
    surface = Color(0xFF1A2220),
    onSurface = Color(0xFFE6ECE9),
    surfaceVariant = DarkAppColors.sunk,
    onSurfaceVariant = DarkAppColors.muted,
    surfaceContainerLow = Color(0xFF1A2220),
    surfaceContainer = Color(0xFF1D2624),
    surfaceContainerHigh = Color(0xFF1F2927),
    outline = Color(0xFF71817B),
    outlineVariant = DarkAppColors.line,
)

@Composable
fun GestioneSpeseTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val appColors = if (darkTheme) DarkAppColors else LightAppColors
    CompositionLocalProvider(LocalAppColors provides appColors) {
        MaterialTheme(colorScheme = if (darkTheme) DarkScheme else LightScheme, content = content)
    }
}

/** Accesso ai colori dell'app: `AppTheme.colors.goal`. */
object AppTheme {
    val colors: AppColors
        @Composable @ReadOnlyComposable get() = LocalAppColors.current
}

/** Cifre a larghezza fissa, così gli importi restano allineati. */
fun TextStyle.tabular(): TextStyle = copy(fontFeatureSettings = "tnum")

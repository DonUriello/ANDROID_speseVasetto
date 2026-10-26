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

/** Colori dell'app oltre a quelli di Material (palette di SPEC.md §5). */
@Immutable
data class AppColors(
    val accent: Color,
    val spend: Color,
    val goal: Color,
    val neg: Color,
    val ok: Color,
    val quick: Color,
    val chartIn: Color,
    val chartOut: Color,
)

private val LightAppColors = AppColors(
    accent = Color(0xFF0F6B5C),
    spend = Color(0xFF2D5EA3),
    goal = Color(0xFF8A5A00),
    neg = Color(0xFFB3261E),
    ok = Color(0xFF25804F),
    quick = Color(0xFFB4531A),
    chartIn = Color(0xFF1F8A70),
    chartOut = Color(0xFFC46F1C),
)

private val DarkAppColors = AppColors(
    accent = Color(0xFF4FBFA6),
    spend = Color(0xFF8DB4EC),
    goal = Color(0xFFE3B55A),
    neg = Color(0xFFF08A80),
    ok = Color(0xFF4CB87E),
    quick = Color(0xFFE08A4A),
    chartIn = Color(0xFF2FA583),
    chartOut = Color(0xFFCF7F30),
)

private val LocalAppColors = staticCompositionLocalOf { LightAppColors }

private val LightScheme = lightColorScheme(
    primary = LightAppColors.accent,
    onPrimary = Color.White,
    secondary = LightAppColors.spend,
    tertiary = LightAppColors.goal,
    error = LightAppColors.neg,
)

private val DarkScheme = darkColorScheme(
    primary = DarkAppColors.accent,
    onPrimary = Color(0xFF00201A),
    secondary = DarkAppColors.spend,
    tertiary = DarkAppColors.goal,
    error = DarkAppColors.neg,
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

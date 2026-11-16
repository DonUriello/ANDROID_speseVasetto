package it.elia.gestionespese.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import it.elia.gestionespese.R

/**
 * Colori dell'app: card pastello (pesca, teal, ambra, azzurro), testata teal, card blu notte per gli
 * obiettivi, accento pesca. Ogni colore con testo sopra ha il suo "on…" con contrasto ≥ 4,5:1.
 */
@Immutable
data class AppColors(
    /** Testo principale. */
    val ink: Color,
    val muted: Color,
    val line: Color,
    val sunk: Color,
    /** Testata della schermata Budget. */
    val header: Color,
    val onHeader: Color,
    /** Accento caldo: pulsante −, Conferma, Spese rapide. */
    val peach: Color,
    val onPeach: Color,
    /** Ambra: "+ Stipendio", monete, barre di avanzamento. */
    val amber: Color,
    val onAmber: Color,
    /** Fondi pastello delle card, con testo [ink]. */
    val peachSoft: Color,
    val tealSoft: Color,
    val amberSoft: Color,
    val blueSoft: Color,
    /** Card degli obiettivi. */
    val navy: Color,
    val onNavy: Color,
    val onNavyMuted: Color,
    val warnOnNavy: Color,
    /** Importi in entrata. */
    val income: Color,
    val neg: Color,
    val negSoft: Color,
    val ok: Color,
    val onOk: Color,
    val chartIn: Color,
    val chartInStrong: Color,
    val chartOut: Color,
    // Nomi storici, usati da Impostazioni e moduli
    val accent: Color,
    val onAccent: Color,
    val accentSoft: Color,
    val spend: Color,
    val spendSoft: Color,
    val goal: Color,
    val goalSoft: Color,
    val goalTint: Color,
    val quick: Color,
    val onQuick: Color,
)

private val LightAppColors = AppColors(
    ink = Color(0xFF2E3E5C),
    muted = Color(0xFF4A5A70),
    line = Color(0xFFE2E5EA),
    sunk = Color(0xFFF1EFEC),
    header = Color(0xFF2F7F72),
    onHeader = Color.White,
    peach = Color(0xFFF2A07B),
    onPeach = Color(0xFF2E3E5C),
    amber = Color(0xFFF2B552),
    onAmber = Color(0xFF2E3E5C),
    peachSoft = Color(0xFFF9D3BE),
    tealSoft = Color(0xFFBFE0DC),
    amberSoft = Color(0xFFF7D78E),
    blueSoft = Color(0xFFC9D6EC),
    navy = Color(0xFF3B5280),
    onNavy = Color.White,
    onNavyMuted = Color(0xFFD5DCEA),
    warnOnNavy = Color(0xFFFFB4A9),
    income = Color(0xFF1F6B5E),
    neg = Color(0xFFB3261E),
    negSoft = Color(0xFFF8DEDB),
    ok = Color(0xFF25804F),
    onOk = Color.White,
    chartIn = Color(0xFF5BAA9B),
    chartInStrong = Color(0xFF2F7F72),
    chartOut = Color(0xFFF2B552),
    accent = Color(0xFF2F7F72),
    onAccent = Color.White,
    accentSoft = Color(0xFFDCEFEC),
    spend = Color(0xFF3B5280),
    spendSoft = Color(0xFFC9D6EC),
    goal = Color(0xFF8A5A00),
    goalSoft = Color(0xFFF7D78E),
    goalTint = Color(0xFFFBF5E8),
    quick = Color(0xFFF2A07B),
    onQuick = Color(0xFF2E3E5C),
)

private val DarkAppColors = AppColors(
    ink = Color(0xFFE8ECF2),
    muted = Color(0xFFA9B4C4),
    line = Color(0xFF334052),
    sunk = Color(0xFF252E3B),
    header = Color(0xFF1F5F55),
    onHeader = Color.White,
    peach = Color(0xFFF2A07B),
    onPeach = Color(0xFF1E2633),
    amber = Color(0xFFF2B552),
    onAmber = Color(0xFF1E2633),
    peachSoft = Color(0xFF55372A),
    tealSoft = Color(0xFF22463F),
    amberSoft = Color(0xFF524221),
    blueSoft = Color(0xFF2A3A55),
    navy = Color(0xFF2C3D63),
    onNavy = Color.White,
    onNavyMuted = Color(0xFFC6CFDF),
    warnOnNavy = Color(0xFFFFB4A9),
    income = Color(0xFF7FD3C2),
    neg = Color(0xFFF08A80),
    negSoft = Color(0xFF4A2522),
    ok = Color(0xFF4CB87E),
    onOk = Color(0xFF08170F),
    chartIn = Color(0xFF5BAA9B),
    chartInStrong = Color(0xFF8FD6C8),
    chartOut = Color(0xFFF2B552),
    accent = Color(0xFF7FD3C2),
    onAccent = Color(0xFF0B1714),
    accentSoft = Color(0xFF22463F),
    spend = Color(0xFFA9C1EA),
    spendSoft = Color(0xFF2A3A55),
    goal = Color(0xFFF2C46B),
    goalSoft = Color(0xFF524221),
    goalTint = Color(0xFF2A2619),
    quick = Color(0xFFF2A07B),
    onQuick = Color(0xFF1E2633),
)

private val LocalAppColors = staticCompositionLocalOf { LightAppColors }

private val LightScheme = lightColorScheme(
    primary = LightAppColors.header,
    onPrimary = Color.White,
    primaryContainer = LightAppColors.tealSoft,
    onPrimaryContainer = LightAppColors.ink,
    secondary = LightAppColors.navy,
    tertiary = LightAppColors.amber,
    error = LightAppColors.neg,
    background = Color(0xFFF6F4F1),
    onBackground = LightAppColors.ink,
    surface = Color.White,
    onSurface = LightAppColors.ink,
    surfaceVariant = LightAppColors.sunk,
    onSurfaceVariant = LightAppColors.muted,
    surfaceContainerLow = Color.White,
    surfaceContainer = Color.White,
    surfaceContainerHigh = Color.White,
    surfaceContainerHighest = LightAppColors.sunk,
    outline = Color(0xFF8A96A6),
    outlineVariant = LightAppColors.line,
)

private val DarkScheme = darkColorScheme(
    primary = DarkAppColors.accent,
    onPrimary = DarkAppColors.onAccent,
    primaryContainer = DarkAppColors.tealSoft,
    onPrimaryContainer = DarkAppColors.ink,
    secondary = DarkAppColors.spend,
    tertiary = DarkAppColors.amber,
    error = DarkAppColors.neg,
    background = Color(0xFF151B24),
    onBackground = DarkAppColors.ink,
    surface = Color(0xFF1E2631),
    onSurface = DarkAppColors.ink,
    surfaceVariant = DarkAppColors.sunk,
    onSurfaceVariant = DarkAppColors.muted,
    surfaceContainerLow = Color(0xFF1E2631),
    surfaceContainer = Color(0xFF212A36),
    surfaceContainerHigh = Color(0xFF242D3A),
    surfaceContainerHighest = DarkAppColors.sunk,
    outline = Color(0xFF6F7C8E),
    outlineVariant = DarkAppColors.line,
)

/** Rubik, incluso nell'app (licenza SIL OFL in assets/licenses). */
val Rubik = FontFamily(
    Font(R.font.rubik_regular, FontWeight.Normal),
    Font(R.font.rubik_medium, FontWeight.Medium),
    Font(R.font.rubik_semibold, FontWeight.SemiBold),
    Font(R.font.rubik_bold, FontWeight.Bold),
)

private val AppTypography: Typography = Typography().let { t ->
    fun TextStyle.rubik() = copy(fontFamily = Rubik)
    Typography(
        displayLarge = t.displayLarge.rubik(),
        displayMedium = t.displayMedium.rubik(),
        displaySmall = t.displaySmall.rubik(),
        headlineLarge = t.headlineLarge.rubik(),
        headlineMedium = t.headlineMedium.rubik(),
        headlineSmall = t.headlineSmall.rubik(),
        titleLarge = t.titleLarge.rubik(),
        titleMedium = t.titleMedium.rubik(),
        titleSmall = t.titleSmall.rubik(),
        bodyLarge = t.bodyLarge.rubik(),
        bodyMedium = t.bodyMedium.rubik(),
        bodySmall = t.bodySmall.rubik(),
        labelLarge = t.labelLarge.rubik(),
        labelMedium = t.labelMedium.rubik(),
        labelSmall = t.labelSmall.rubik(),
    )
}

@Composable
fun GestioneSpeseTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val appColors = if (darkTheme) DarkAppColors else LightAppColors
    CompositionLocalProvider(LocalAppColors provides appColors) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkScheme else LightScheme,
            typography = AppTypography,
            content = content,
        )
    }
}

/** Accesso ai colori dell'app: `AppTheme.colors.peach`. */
object AppTheme {
    val colors: AppColors
        @Composable @ReadOnlyComposable get() = LocalAppColors.current
}

/** Cifre a larghezza fissa, così gli importi restano allineati. */
fun TextStyle.tabular(): TextStyle = copy(fontFeatureSettings = "tnum")

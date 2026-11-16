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
 * Colori dell'app: fondo grigio chiaro, card bianche, nero per la card del saldo e le azioni principali,
 * verde/arancio/blu/viola per le barre dei budget. Ogni colore con testo sopra ha il suo "on…" (≥ 4,5:1).
 */
@Immutable
data class AppColors(
    /** Testo principale. */
    val ink: Color,
    val muted: Color,
    val line: Color,
    /** Fondo dei campi e delle tracce delle barre. */
    val sunk: Color,
    /** Card del saldo (nera) e selezioni forti. */
    val header: Color,
    val onHeader: Color,
    val onHeaderMuted: Color,
    /** Azione principale: pulsanti pieni, Conferma, pillola di navigazione. */
    val peach: Color,
    val onPeach: Color,
    /** Verde: progressi, entrate, "Da assegnare". */
    val amber: Color,
    val onAmber: Color,
    /** Tinte chiare per icone e fondi secondari. */
    val peachSoft: Color,
    val tealSoft: Color,
    val amberSoft: Color,
    val blueSoft: Color,
    /** Riquadri forti (es. "Resta in Da assegnare"). */
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
    /** Colori delle barre dei budget, a rotazione. */
    val bars: List<Color>,
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
    ink = Color(0xFF111113),
    muted = Color(0xFF6B6B70),
    line = Color(0xFFE5E5EA),
    sunk = Color(0xFFF2F2F4),
    header = Color(0xFF1C1C1E),
    onHeader = Color.White,
    onHeaderMuted = Color(0xFFB8B8BD),
    peach = Color(0xFF111113),
    onPeach = Color.White,
    amber = Color(0xFF16A34A),
    onAmber = Color.White,
    peachSoft = Color(0xFFFFF1E6),
    tealSoft = Color(0xFFE7F7EC),
    amberSoft = Color(0xFFFFF6DA),
    blueSoft = Color(0xFFEAF0FF),
    navy = Color(0xFF1C1C1E),
    onNavy = Color.White,
    onNavyMuted = Color(0xFFB8B8BD),
    warnOnNavy = Color(0xFFFF9F94),
    income = Color(0xFF15803D),
    neg = Color(0xFFC62828),
    negSoft = Color(0xFFFDE7E7),
    ok = Color(0xFF15803D),
    onOk = Color.White,
    chartIn = Color(0xFF22C55E),
    chartInStrong = Color(0xFF15803D),
    chartOut = Color(0xFF1C1C1E),
    bars = listOf(Color(0xFF22C55E), Color(0xFFF59E0B), Color(0xFF3B82F6), Color(0xFF8B5CF6)),
    accent = Color(0xFF111113),
    onAccent = Color.White,
    accentSoft = Color(0xFFE7F7EC),
    spend = Color(0xFF111113),
    spendSoft = Color(0xFFF2F2F4),
    goal = Color(0xFF9A5B00),
    goalSoft = Color(0xFFFFF6DA),
    goalTint = Color(0xFFFFFFFF),
    quick = Color(0xFF111113),
    onQuick = Color.White,
)

private val DarkAppColors = AppColors(
    ink = Color(0xFFF5F5F7),
    muted = Color(0xFFA1A1A6),
    line = Color(0xFF38383A),
    sunk = Color(0xFF2C2C2E),
    header = Color(0xFF2C2C2E),
    onHeader = Color.White,
    onHeaderMuted = Color(0xFFB8B8BD),
    peach = Color(0xFFF5F5F7),
    onPeach = Color(0xFF111113),
    amber = Color(0xFF30D158),
    onAmber = Color(0xFF0B1A10),
    peachSoft = Color(0xFF3A2A1E),
    tealSoft = Color(0xFF1E3326),
    amberSoft = Color(0xFF3A3218),
    blueSoft = Color(0xFF1E2840),
    navy = Color(0xFF2C2C2E),
    onNavy = Color.White,
    onNavyMuted = Color(0xFFB8B8BD),
    warnOnNavy = Color(0xFFFF9F94),
    income = Color(0xFF30D158),
    neg = Color(0xFFFF6B60),
    negSoft = Color(0xFF3D1E1C),
    ok = Color(0xFF30D158),
    onOk = Color(0xFF0B1A10),
    chartIn = Color(0xFF30D158),
    chartInStrong = Color(0xFF7BE495),
    chartOut = Color(0xFFF5F5F7),
    bars = listOf(Color(0xFF30D158), Color(0xFFFFB340), Color(0xFF64A0FF), Color(0xFFB08CFF)),
    accent = Color(0xFFF5F5F7),
    onAccent = Color(0xFF111113),
    accentSoft = Color(0xFF1E3326),
    spend = Color(0xFFF5F5F7),
    spendSoft = Color(0xFF2C2C2E),
    goal = Color(0xFFFFB340),
    goalSoft = Color(0xFF3A3218),
    goalTint = Color(0xFF1C1C1E),
    quick = Color(0xFFF5F5F7),
    onQuick = Color(0xFF111113),
)

private val LocalAppColors = staticCompositionLocalOf { LightAppColors }

private val LightScheme = lightColorScheme(
    primary = LightAppColors.peach,
    onPrimary = LightAppColors.onPeach,
    primaryContainer = LightAppColors.tealSoft,
    onPrimaryContainer = LightAppColors.ink,
    secondary = LightAppColors.ink,
    tertiary = LightAppColors.amber,
    error = LightAppColors.neg,
    background = Color(0xFFF2F2F4),
    onBackground = LightAppColors.ink,
    surface = Color.White,
    onSurface = LightAppColors.ink,
    surfaceVariant = LightAppColors.sunk,
    onSurfaceVariant = LightAppColors.muted,
    surfaceContainerLow = Color.White,
    surfaceContainer = Color.White,
    surfaceContainerHigh = Color.White,
    surfaceContainerHighest = LightAppColors.sunk,
    outline = Color(0xFF8E8E93),
    outlineVariant = LightAppColors.line,
)

private val DarkScheme = darkColorScheme(
    primary = DarkAppColors.peach,
    onPrimary = DarkAppColors.onPeach,
    primaryContainer = DarkAppColors.tealSoft,
    onPrimaryContainer = DarkAppColors.ink,
    secondary = DarkAppColors.ink,
    tertiary = DarkAppColors.amber,
    error = DarkAppColors.neg,
    background = Color(0xFF000000),
    onBackground = DarkAppColors.ink,
    surface = Color(0xFF1C1C1E),
    onSurface = DarkAppColors.ink,
    surfaceVariant = DarkAppColors.sunk,
    onSurfaceVariant = DarkAppColors.muted,
    surfaceContainerLow = Color(0xFF1C1C1E),
    surfaceContainer = Color(0xFF1C1C1E),
    surfaceContainerHigh = Color(0xFF242426),
    surfaceContainerHighest = DarkAppColors.sunk,
    outline = Color(0xFF8E8E93),
    outlineVariant = DarkAppColors.line,
)

/** Inter, incluso nell'app (licenza SIL OFL in assets/licenses). */
val AppFont = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_bold, FontWeight.Bold),
)

private val AppTypography: Typography = Typography().let { t ->
    fun TextStyle.withAppFont() = copy(fontFamily = AppFont)
    Typography(
        displayLarge = t.displayLarge.withAppFont(),
        displayMedium = t.displayMedium.withAppFont(),
        displaySmall = t.displaySmall.withAppFont(),
        headlineLarge = t.headlineLarge.withAppFont(),
        headlineMedium = t.headlineMedium.withAppFont(),
        headlineSmall = t.headlineSmall.withAppFont(),
        titleLarge = t.titleLarge.withAppFont(),
        titleMedium = t.titleMedium.withAppFont(),
        titleSmall = t.titleSmall.withAppFont(),
        bodyLarge = t.bodyLarge.withAppFont(),
        bodyMedium = t.bodyMedium.withAppFont(),
        bodySmall = t.bodySmall.withAppFont(),
        labelLarge = t.labelLarge.withAppFont(),
        labelMedium = t.labelMedium.withAppFont(),
        labelSmall = t.labelSmall.withAppFont(),
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

/** Accesso ai colori dell'app: `AppTheme.colors.ink`. */
object AppTheme {
    val colors: AppColors
        @Composable @ReadOnlyComposable get() = LocalAppColors.current
}

/** Cifre a larghezza fissa, così gli importi restano allineati. */
fun TextStyle.tabular(): TextStyle = copy(fontFeatureSettings = "tnum")

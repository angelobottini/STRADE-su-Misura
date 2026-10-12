package it.stradesumisura.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightColors = lightColorScheme(
    primary = Ink,
    onPrimary = Color.White,
    primaryContainer = Mauve,
    onPrimaryContainer = Ink,
    inversePrimary = Mauve,
    secondary = MauveDeep,
    onSecondary = Color.White,
    secondaryContainer = MauveSoft,
    onSecondaryContainer = MauveInk,
    tertiary = Moss,
    onTertiary = Color.White,
    tertiaryContainer = MossSoft,
    onTertiaryContainer = MossInk,
    background = Paper,
    onBackground = Ink,
    surface = Color.White,
    onSurface = Ink,
    surfaceVariant = Mist,
    onSurfaceVariant = Graphite,
    surfaceTint = MauveDeep,
    inverseSurface = Ink,
    inverseOnSurface = Paper,
    outline = OutlineStrong,
    outlineVariant = OutlineSoft,
    error = Danger,
    onError = Color.White,
    errorContainer = DangerSoft,
    onErrorContainer = DangerInk,
    surfaceBright = Color.White,
    surfaceDim = MistHighest,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Paper,
    surfaceContainer = Mist,
    surfaceContainerHigh = MistHigh,
    surfaceContainerHighest = MistHighest,
)

private val DarkColors = darkColorScheme(
    primary = Mauve,
    onPrimary = Ink,
    primaryContainer = MauveSoftDark,
    onPrimaryContainer = MauveSoft,
    inversePrimary = MauveDeep,
    secondary = Mauve,
    onSecondary = Ink,
    secondaryContainer = MauveSoftDark,
    onSecondaryContainer = MauveSoft,
    tertiary = Color(0xFF9DCBB0),
    onTertiary = MossInk,
    tertiaryContainer = Color(0xFF2B4D3C),
    onTertiaryContainer = MossSoft,
    background = Night,
    onBackground = NightInk,
    surface = NightSurface,
    onSurface = NightInk,
    surfaceVariant = NightContainer,
    onSurfaceVariant = NightGraphite,
    surfaceTint = Mauve,
    inverseSurface = NightInk,
    inverseOnSurface = Night,
    outline = NightOutline,
    outlineVariant = NightOutlineSoft,
    error = Color(0xFFF2B8B5),
    onError = Color(0xFF601410),
    errorContainer = Color(0xFF8C1D18),
    onErrorContainer = DangerSoft,
    surfaceBright = NightHighest,
    surfaceDim = Night,
    surfaceContainerLowest = Color(0xFF0F0D0F),
    surfaceContainerLow = NightLow,
    surfaceContainer = NightContainer,
    surfaceContainerHigh = NightHigh,
    surfaceContainerHighest = NightHighest,
)

val StradeShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun StradeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = StradeTypography,
        shapes = StradeShapes,
        content = content,
    )
}

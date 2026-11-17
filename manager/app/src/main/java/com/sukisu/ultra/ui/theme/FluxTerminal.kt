package com.sukisu.ultra.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import top.yukonga.miuix.kmp.theme.Colors

/**
 * FluxSU "terminal" design system: phosphor green on near-black (or dark green on paper),
 * amber for notices, red for errors. Used whenever the user has not picked a custom key colour,
 * so a picked key colour still wins.
 */
object FluxTerminalPalette {
    // Dark
    val Phosphor = Color(0xFF39FF88)
    val PhosphorDim = Color(0xFF0F3A22)
    val Cyan = Color(0xFF5CE1E6)
    val Amber = Color(0xFFFFB454)
    val Danger = Color(0xFFFF5C57)
    val Ink = Color(0xFFD7F5DE)
    val InkDim = Color(0xFF8FB59B)
    val Line = Color(0xFF2C4B38)
    val LineSoft = Color(0xFF1B2D22)
}

fun fluxTerminalColorScheme(isDark: Boolean, isAmoled: Boolean): ColorScheme =
    if (isDark) {
        val bg = if (isAmoled) Color.Black else Color(0xFF0A0E0A)
        darkColorScheme(
            primary = FluxTerminalPalette.Phosphor,
            onPrimary = Color(0xFF00210E),
            primaryContainer = FluxTerminalPalette.PhosphorDim,
            onPrimaryContainer = Color(0xFFB6FFD3),
            inversePrimary = Color(0xFF00873F),
            secondary = FluxTerminalPalette.Cyan,
            onSecondary = Color(0xFF00363A),
            secondaryContainer = Color(0xFF0B3A3D),
            onSecondaryContainer = Color(0xFFC2F8FA),
            tertiary = FluxTerminalPalette.Amber,
            onTertiary = Color(0xFF3A2400),
            tertiaryContainer = Color(0xFF4A3000),
            onTertiaryContainer = Color(0xFFFFDDAE),
            background = bg,
            onBackground = FluxTerminalPalette.Ink,
            surface = bg,
            onSurface = FluxTerminalPalette.Ink,
            surfaceVariant = Color(0xFF1B241B),
            onSurfaceVariant = FluxTerminalPalette.InkDim,
            surfaceTint = FluxTerminalPalette.Phosphor,
            inverseSurface = FluxTerminalPalette.Ink,
            inverseOnSurface = Color(0xFF0A0E0A),
            error = FluxTerminalPalette.Danger,
            onError = Color(0xFF3B0000),
            errorContainer = Color(0xFF5A1411),
            onErrorContainer = Color(0xFFFFDAD6),
            outline = Color(0xFF3E6B4F),
            outlineVariant = FluxTerminalPalette.LineSoft,
            scrim = Color.Black,
            surfaceBright = if (isAmoled) Color(0xFF0B100B) else Color(0xFF131A13),
            surfaceDim = bg,
            surfaceContainerLowest = bg,
            surfaceContainerLow = if (isAmoled) Color.Black else Color(0xFF0D120D),
            surfaceContainer = if (isAmoled) Color(0xFF050805) else Color(0xFF111711),
            surfaceContainerHigh = if (isAmoled) Color(0xFF0A0F0A) else Color(0xFF161D16),
            surfaceContainerHighest = if (isAmoled) Color(0xFF0F150F) else Color(0xFF1B241B),
        )
    } else {
        lightColorScheme(
            primary = Color(0xFF007A3D),
            onPrimary = Color.White,
            primaryContainer = Color(0xFFBDF5D2),
            onPrimaryContainer = Color(0xFF00210E),
            inversePrimary = FluxTerminalPalette.Phosphor,
            secondary = Color(0xFF006B70),
            onSecondary = Color.White,
            secondaryContainer = Color(0xFFBFF1F3),
            onSecondaryContainer = Color(0xFF002022),
            tertiary = Color(0xFF8A5100),
            onTertiary = Color.White,
            tertiaryContainer = Color(0xFFFFDDAE),
            onTertiaryContainer = Color(0xFF2C1700),
            background = Color(0xFFF3F7F1),
            onBackground = Color(0xFF0E1A12),
            surface = Color(0xFFF3F7F1),
            onSurface = Color(0xFF0E1A12),
            surfaceVariant = Color(0xFFDDE8DE),
            onSurfaceVariant = Color(0xFF445A4A),
            surfaceTint = Color(0xFF007A3D),
            inverseSurface = Color(0xFF0E1A12),
            inverseOnSurface = Color(0xFFD7F5DE),
            error = Color(0xFFB3261E),
            onError = Color.White,
            errorContainer = Color(0xFFF9DEDC),
            onErrorContainer = Color(0xFF410E0B),
            outline = Color(0xFF6F8A76),
            outlineVariant = Color(0xFFC4D3C7),
            scrim = Color.Black,
            surfaceBright = Color(0xFFFFFFFF),
            surfaceDim = Color(0xFFDDE5DC),
            surfaceContainerLowest = Color(0xFFFFFFFF),
            surfaceContainerLow = Color(0xFFEDF3EB),
            surfaceContainer = Color(0xFFE7EFE5),
            surfaceContainerHigh = Color(0xFFE0E9DE),
            surfaceContainerHighest = Color(0xFFDAE4D8),
        )
    }

/** Same palette for the Miuix shell. Only roles whose names are used by the app's overlay code. */
fun Colors.withFluxTerminal(isDark: Boolean, accentChoice: FluxAccent = FluxAccent.Phosphor): Colors {
    val bg = if (isDark) Color(0xFF0A0E0A) else Color(0xFFF3F7F1)
    val card = if (isDark) Color(0xFF111711) else Color(0xFFE7EFE5)
    val cardHigh = if (isDark) Color(0xFF161D16) else Color(0xFFE0E9DE)
    val cardHighest = if (isDark) Color(0xFF1B241B) else Color(0xFFDAE4D8)
    val accent = if (isDark) accentChoice.dark else accentChoice.light
    val onAccent = if (isDark) Color(0xFF06130B) else Color.White
    val text = if (isDark) FluxTerminalPalette.Ink else Color(0xFF0E1A12)
    val textSecondary = if (isDark) FluxTerminalPalette.InkDim else Color(0xFF445A4A)
    val line = if (accentChoice == FluxAccent.Phosphor) {
        if (isDark) Color(0xFF3E6B4F) else Color(0xFF6F8A76)
    } else {
        lerp(bg, accent, if (isDark) 0.4f else 0.5f)
    }
    val danger = if (isDark) FluxTerminalPalette.Danger else Color(0xFFB3261E)

    return copy(
        primary = accent,
        primaryVariant = accent,
        onPrimary = onAccent,
        onPrimaryVariant = onAccent,
        error = danger,
        onBackground = text,
        onBackgroundVariant = textSecondary,
        onSurface = text,
        onSurfaceSecondary = text,
        onSurfaceContainer = text,
        onSurfaceContainerVariant = text,
        onSurfaceContainerHigh = text,
        onSurfaceContainerHighest = text,
        onSurfaceVariantSummary = textSecondary,
        onSurfaceVariantActions = textSecondary,
        surface = bg,
        background = bg,
        surfaceContainer = card,
        surfaceContainerHigh = cardHigh,
        surfaceContainerHighest = cardHighest,
        outline = line,
        dividerLine = line.copy(alpha = 0.5f),
    )
}

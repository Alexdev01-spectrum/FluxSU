package com.sukisu.ultra.ui.theme

import android.content.Context
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import com.sukisu.ultra.ksuApp

/** Selectable accent colours for the FluxSU terminal theme. */
enum class FluxAccent(val id: String, val dark: Color, val light: Color) {
    Phosphor("phosphor", Color(0xFF39FF88), Color(0xFF007A3D)),
    Cyan("cyan", Color(0xFF5CE1E6), Color(0xFF006B70)),
    Amber("amber", Color(0xFFFFB454), Color(0xFF8A5100)),
    Magenta("magenta", Color(0xFFFF6AD5), Color(0xFFA8007F)),
    Ice("ice", Color(0xFF8AB4FF), Color(0xFF1A4FBF));

    companion object {
        fun fromId(id: String?): FluxAccent = values().firstOrNull { it.id == id } ?: Phosphor
    }
}

/** Remembers the chosen accent; reading [accent] inside composition recomposes the theme on change. */
object FluxAccentStore {
    private const val PREFS = "flux_ui"
    private const val KEY = "accent"

    var accent by mutableStateOf(load())
        private set

    private fun load(): FluxAccent = runCatching {
        FluxAccent.fromId(ksuApp.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null))
    }.getOrDefault(FluxAccent.Phosphor)

    fun set(value: FluxAccent) {
        accent = value
        runCatching {
            ksuApp.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, value.id).apply()
        }
    }
}

/** Re-tints the primary roles of a terminal colour scheme. The default accent keeps the hand-tuned palette. */
fun ColorScheme.withFluxAccent(accent: FluxAccent, isDark: Boolean): ColorScheme {
    if (accent == FluxAccent.Phosphor) return this
    val a = if (isDark) accent.dark else accent.light
    return copy(
        primary = a,
        onPrimary = if (isDark) Color(0xFF06130B) else Color.White,
        primaryContainer = lerp(surface, a, if (isDark) 0.24f else 0.28f),
        onPrimaryContainer = if (isDark) lerp(a, Color.White, 0.7f) else lerp(a, Color.Black, 0.65f),
        inversePrimary = if (isDark) accent.light else accent.dark,
        surfaceTint = a,
        outline = lerp(surface, a, if (isDark) 0.4f else 0.5f),
        outlineVariant = lerp(surface, a, if (isDark) 0.13f else 0.22f),
    )
}

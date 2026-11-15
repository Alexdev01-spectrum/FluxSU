package com.sukisu.ultra.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily

// FluxSU terminal look: every text style uses the monospace family.
private val Mono = FontFamily.Monospace
private val Base = Typography()

private fun TextStyle.mono(): TextStyle = copy(fontFamily = Mono)

val Typography = Typography(
    displayLarge = Base.displayLarge.mono(),
    displayMedium = Base.displayMedium.mono(),
    displaySmall = Base.displaySmall.mono(),
    headlineLarge = Base.headlineLarge.mono(),
    headlineMedium = Base.headlineMedium.mono(),
    headlineSmall = Base.headlineSmall.mono(),
    titleLarge = Base.titleLarge.mono(),
    titleMedium = Base.titleMedium.mono(),
    titleSmall = Base.titleSmall.mono(),
    bodyLarge = Base.bodyLarge.mono(),
    bodyMedium = Base.bodyMedium.mono(),
    bodySmall = Base.bodySmall.mono(),
    labelLarge = Base.labelLarge.mono(),
    labelMedium = Base.labelMedium.mono(),
    labelSmall = Base.labelSmall.mono(),
)

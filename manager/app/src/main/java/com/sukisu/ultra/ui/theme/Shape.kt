package com.sukisu.ultra.ui.theme

import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// FluxSU terminal look: sharp edges with a single cut corner pair on larger surfaces.
val Shapes = Shapes(
    extraSmall = CutCornerShape(0.dp),
    small = CutCornerShape(0.dp),
    medium = CutCornerShape(topEnd = 10.dp, bottomStart = 10.dp),
    large = CutCornerShape(topEnd = 14.dp, bottomStart = 14.dp),
    extraLarge = CutCornerShape(topEnd = 18.dp, bottomStart = 18.dp),
)

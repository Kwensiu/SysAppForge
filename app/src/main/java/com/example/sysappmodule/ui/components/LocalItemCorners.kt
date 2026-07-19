package com.example.sysappmodule.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocal
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
data class ItemCorners(
    val topStart: Dp,
    val topEnd: Dp,
    val bottomStart: Dp,
    val bottomEnd: Dp
) {
    companion object {
        val Rounded16 = ItemCorners(16.dp, 16.dp, 16.dp, 16.dp)
    }
}

val LocalItemCorners = compositionLocalOf<ItemCorners> { ItemCorners.Rounded16 }

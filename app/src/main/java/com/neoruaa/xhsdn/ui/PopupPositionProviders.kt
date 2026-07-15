package com.neoruaa.xhsdn.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.ListPopupDefaults
import top.yukonga.miuix.kmp.basic.PopupPositionProvider

@Composable
fun rememberOffsetPopupPositionProvider(
    base: PopupPositionProvider = ListPopupDefaults.DropdownPositionProvider,
    x: Dp = 0.dp,
    y: Dp = 0.dp,
): PopupPositionProvider {
    val density = LocalDensity.current

    return remember(base, density, x, y) {
        val xOffsetPx = with(density) { x.roundToPx() }
        val yOffsetPx = with(density) { y.roundToPx() }

        object : PopupPositionProvider {
            override fun calculatePosition(
                anchorBounds: IntRect,
                windowBounds: IntRect,
                layoutDirection: LayoutDirection,
                popupContentSize: IntSize,
                popupMargin: IntRect,
                alignment: PopupPositionProvider.Align,
            ): IntOffset {
                val position = base.calculatePosition(
                    anchorBounds = anchorBounds,
                    windowBounds = windowBounds,
                    layoutDirection = layoutDirection,
                    popupContentSize = popupContentSize,
                    popupMargin = popupMargin,
                    alignment = alignment,
                )

                val minX = windowBounds.left
                val maxX = (windowBounds.right - popupContentSize.width - popupMargin.right)
                    .coerceAtLeast(minX)
                val minY = (windowBounds.top + popupMargin.top)
                    .coerceAtMost(windowBounds.bottom - popupContentSize.height - popupMargin.bottom)
                val maxY = windowBounds.bottom - popupContentSize.height - popupMargin.bottom

                return IntOffset(
                    x = (position.x + xOffsetPx).coerceIn(minX, maxX),
                    y = (position.y + yOffsetPx).coerceIn(minY, maxY),
                )
            }

            override fun getMargins() = base.getMargins()
        }
    }
}

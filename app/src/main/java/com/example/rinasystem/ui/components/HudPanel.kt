package com.example.rinasystem.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import com.example.rinasystem.ui.theme.AriaCyan

@Composable
fun HudPanel(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(modifier = modifier.fillMaxWidth()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val cornerLen = 20.dp.toPx()
            val color = AriaCyan.copy(alpha = 0.5f)
            val strokeWidth = 1.5f

            // Top-left corner
            drawLine(color, Offset(0f, cornerLen), Offset(0f, 0f), strokeWidth)
            drawLine(color, Offset(0f, 0f), Offset(cornerLen, 0f), strokeWidth)

            // Top-right corner
            drawLine(color, Offset(w - cornerLen, 0f), Offset(w, 0f), strokeWidth)
            drawLine(color, Offset(w, 0f), Offset(w, cornerLen), strokeWidth)

            // Bottom-left corner
            drawLine(color, Offset(0f, h - cornerLen), Offset(0f, h), strokeWidth)
            drawLine(color, Offset(0f, h), Offset(cornerLen, h), strokeWidth)

            // Bottom-right corner
            drawLine(color, Offset(w - cornerLen, h), Offset(w, h), strokeWidth)
            drawLine(color, Offset(w, h - cornerLen), Offset(w, h), strokeWidth)
        }

        Column(
            modifier = Modifier.padding(16.dp),
            content = content
        )
    }
}

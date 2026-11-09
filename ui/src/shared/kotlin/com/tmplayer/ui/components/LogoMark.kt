package com.tmplayer.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import com.tmplayer.ui.theme.Tone

/** Original Veylora V/play mark. The play facet follows the loader animation. */
@Composable
fun LogoMark(size: Dp, modifier: Modifier = Modifier, progress: () -> Float = { RESTING }) {
    Canvas(modifier.size(size).semantics { contentDescription = BRAND_NAME }) {
        val u = this.size.width / VIEWPORT

        drawRoundRect(Color(0xFF080713), Offset.Zero, this.size, CornerRadius(this.size.width * TILE_CORNER))
        scale(u, u, pivot = Offset.Zero) {
            drawPath(leftWing,
                Brush.linearGradient(listOf(Color(0xFFC5A3FF), Color(0xFF963EFF), Color(0xFF32106B)),
                    start = Offset.Zero, end = Offset(1024f, 1024f)))
            drawPath(rightWing,
                Brush.linearGradient(listOf(Color(0xFF557BFF), Color(0xFF963EFF)),
                    start = Offset(710f, 220f), end = Offset(445f, 820f)))
            drawPath(playFacet,
                Color(0xFFC5A3FF), alpha = 0.85f + progress().coerceIn(0f, 1f) * 0.15f)
        }
    }
}

/** Where the playhead rests when nothing moves it: where the launcher icon has it. */
const val RESTING = (640f - 128f) / (896f - 128f)

private const val VIEWPORT = 1024f

/** The tile's corner as a share of its side: the lockup's (`MARK_CORNER`) and [AppMark]'s. */
const val TILE_CORNER = 0.22f

private val leftWing = PathParser().parsePathString("M170 220H332L519 645L445 820Z").toPath()
private val rightWing = PathParser().parsePathString("M710 220H872L570 820H445Z").toPath()
private val playFacet = PathParser().parsePathString("M470 325L679 448L470 571Z").toPath()

package com.reelmonster.app.monster

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

private val Ink = Color(0xFF2B2B3A)
private val Gold = Color(0xFFFFD54F)
private val Plum = Color(0xFF3B1E54)

/** Animated monster drawn entirely with Canvas (no image assets). [bump] increments trigger a squish. */
@Composable
fun MonsterView(count: Int, modifier: Modifier = Modifier, bump: Int = 0, showCount: Boolean = true) {
    val stage = STAGES[stageIndex(count)]
    val inf = rememberInfiniteTransition(label = "monster")
    val phase by inf.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(2600, easing = LinearEasing)),
        label = "phase"
    )
    val squish = remember { Animatable(0f) }
    LaunchedEffect(bump) {
        if (bump > 0) {
            squish.animateTo(1f, tween(70))
            squish.animateTo(0f, spring(dampingRatio = 0.35f, stiffness = 400f))
        }
    }
    var blink by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(2200L + Random.nextLong(2000L))
            blink = true
            delay(130)
            blink = false
        }
    }
    BoxWithConstraints(modifier) {
        val s = minOf(maxWidth, maxHeight)
        Canvas(Modifier.fillMaxSize()) { drawMonster(stage, phase, blink, squish.value) }
        if (showCount) {
            Text(
                text = count.toString(),
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = s * 0.2f),
                style = TextStyle(fontSize = (s.value * 0.15f).sp, fontWeight = FontWeight.Black, color = Ink)
            )
        }
    }
}

private fun DrawScope.tri(c: Color, a: Offset, b: Offset, d: Offset) {
    val p = Path().apply {
        moveTo(a.x, a.y); lineTo(b.x, b.y); lineTo(d.x, d.y); close()
    }
    drawPath(p, c)
}

private fun DrawScope.drawMonster(st: Stage, phase: Float, blink: Boolean, squish: Float) {
    val w = size.width
    val h = size.height
    val idx = st.index
    val bob = sin(phase) * h * 0.025f
    val bw = w * 0.62f * st.scale * (1f + 0.12f * squish)
    val bh = h * 0.58f * st.scale * (1f - 0.15f * squish)
    val cx = w / 2f
    val bottom = h * 0.84f + bob
    val top = bottom - bh
    val left = cx - bw / 2f
    val cy = top + bh / 2f

    // shadow
    drawOval(Color(0x2A000000), Offset(cx - bw * 0.4f, h * 0.86f), Size(bw * 0.8f * (1f - 0.05f * sin(phase)), h * 0.05f))

    // arms (waving)
    val aw = sin(phase * 2f) * bh * 0.05f
    drawOval(st.body, Offset(left - bw * 0.09f, cy - bh * 0.02f + aw), Size(bw * 0.2f, bh * 0.3f))
    drawOval(st.body, Offset(left + bw * 0.89f, cy - bh * 0.02f - aw), Size(bw * 0.2f, bh * 0.3f))

    // ears / horns / antenna / spikes (differ by stage)
    if (idx == 0) {
        drawCircle(st.body, bw * 0.10f, Offset(left + bw * 0.20f, top + bh * 0.04f))
        drawCircle(st.body, bw * 0.10f, Offset(left + bw * 0.80f, top + bh * 0.04f))
    } else {
        val hh = bh * (0.10f + 0.035f * idx)
        val hw = bw * 0.16f
        val horn = if (idx >= 4) Plum else Color(0xFFFFE082)
        tri(horn, Offset(left + bw * 0.14f, top + bh * 0.14f), Offset(left + bw * 0.14f + hw, top + bh * 0.06f), Offset(left + bw * 0.10f, top - hh))
        tri(horn, Offset(left + bw * 0.86f, top + bh * 0.14f), Offset(left + bw * 0.86f - hw, top + bh * 0.06f), Offset(left + bw * 0.90f, top - hh))
    }
    if (idx == 2) {
        drawLine(Ink, Offset(cx, top + bh * 0.02f), Offset(cx + bw * 0.05f, top - bh * 0.16f), strokeWidth = bw * 0.02f, cap = StrokeCap.Round)
        drawCircle(Color(0xFFFFEB3B), bw * 0.05f, Offset(cx + bw * 0.05f, top - bh * 0.16f))
    }
    if (idx >= 4) {
        val c = if (idx == 5) Gold else Plum
        val tall = if (idx == 5) 0.2f else 0.12f
        for (i in -1..1) {
            val x = cx + i * bw * 0.16f
            tri(c, Offset(x - bw * 0.06f, top + bh * 0.03f), Offset(x + bw * 0.06f, top + bh * 0.03f), Offset(x, top - bh * tall))
        }
    }

    // body, highlight, belly
    drawRoundRect(st.body, Offset(left, top), Size(bw, bh), CornerRadius(bw * 0.42f, bh * 0.42f))
    drawOval(Color(0x33FFFFFF), Offset(left + bw * 0.12f, top + bh * 0.08f), Size(bw * 0.3f, bh * 0.12f))
    drawOval(st.belly, Offset(cx - bw * 0.3f, top + bh * 0.52f), Size(bw * 0.6f, bh * 0.42f))
    if (idx >= 3) {
        drawCircle(Color(0x22000000), bw * 0.04f, Offset(left + bw * 0.15f, top + bh * 0.66f))
        drawCircle(Color(0x22000000), bw * 0.03f, Offset(left + bw * 0.86f, top + bh * 0.58f))
        drawCircle(Color(0x22000000), bw * 0.035f, Offset(left + bw * 0.80f, top + bh * 0.78f))
    }

    // eyes
    val ey = top + bh * 0.30f
    val ex = bw * 0.20f
    val er = bw * (if (idx == 0) 0.13f else 0.11f)
    eye(st, cx - ex, ey, er, blink)
    eye(st, cx + ex, ey, er, blink)
    if (idx == 5) drawCircle(Color(0xFFFFEB3B), er * 0.6f, Offset(cx, top + bh * 0.12f))

    // cheeks
    if (idx <= 1) {
        drawCircle(Color(0x55FF4081), bw * 0.06f, Offset(cx - bw * 0.3f, ey + er * 1.7f))
        drawCircle(Color(0x55FF4081), bw * 0.06f, Offset(cx + bw * 0.3f, ey + er * 1.7f))
    }

    // mouth
    val my = top + bh * 0.50f
    val mw = bw * 0.34f
    when (idx) {
        0 -> drawArc(Ink, 20f, 140f, false, Offset(cx - mw * 0.4f, my - mw * 0.25f), Size(mw * 0.8f, mw * 0.5f), style = Stroke(bw * 0.025f, cap = StrokeCap.Round))
        1 -> {
            drawArc(Ink, 0f, 180f, true, Offset(cx - mw / 2, my - mw * 0.2f), Size(mw, mw * 0.6f))
            drawOval(Color(0xFFFF8A80), Offset(cx - mw * 0.2f, my + mw * 0.12f), Size(mw * 0.4f, mw * 0.2f))
        }
        2 -> drawLine(Ink, Offset(cx - mw * 0.35f, my), Offset(cx + mw * 0.35f, my), strokeWidth = bw * 0.025f, cap = StrokeCap.Round)
        3 -> drawOval(Ink, Offset(cx - mw * 0.15f, my - mw * 0.15f), Size(mw * 0.3f, mw * 0.4f))
        4 -> {
            drawArc(Ink, 0f, 180f, true, Offset(cx - mw * 0.6f, my - mw * 0.25f), Size(mw * 1.2f, mw * 0.8f))
            for (i in 0..4) drawRect(Color.White, Offset(cx - mw * 0.5f + i * mw * 0.2f, my + mw * 0.15f), Size(mw * 0.16f, mw * 0.14f))
        }
        else -> {
            drawArc(Ink, 0f, 180f, true, Offset(cx - mw * 0.7f, my - mw * 0.3f), Size(mw * 1.4f, mw * 1.0f))
            val fy = my + mw * 0.2f
            tri(Color.White, Offset(cx - mw * 0.5f, fy), Offset(cx - mw * 0.3f, fy), Offset(cx - mw * 0.4f, fy + mw * 0.22f))
            tri(Color.White, Offset(cx + mw * 0.3f, fy), Offset(cx + mw * 0.5f, fy), Offset(cx + mw * 0.4f, fy + mw * 0.22f))
        }
    }
}

private fun DrawScope.eye(st: Stage, x: Float, y: Float, r: Float, blink: Boolean) {
    if (blink) {
        drawLine(Ink, Offset(x - r, y), Offset(x + r, y), strokeWidth = r * 0.3f, cap = StrokeCap.Round)
        return
    }
    when (st.index) {
        5 -> {
            drawCircle(Color(0x55FFEB3B), r * 1.5f, Offset(x, y))
            drawCircle(Color(0xFFFFEB3B), r, Offset(x, y))
            drawOval(Ink, Offset(x - r * 0.15f, y - r * 0.8f), Size(r * 0.3f, r * 1.6f))
        }
        4 -> {
            drawCircle(Color(0xFFFFF1F1), r, Offset(x, y))
            drawLine(Color(0xFFE53935), Offset(x - r, y - r * 0.3f), Offset(x - r * 0.3f, y), strokeWidth = r * 0.08f)
            drawLine(Color(0xFFE53935), Offset(x + r, y + r * 0.3f), Offset(x + r * 0.3f, y), strokeWidth = r * 0.08f)
            drawCircle(Ink, r * 0.45f, Offset(x, y + r * 0.05f))
        }
        3 -> {
            drawCircle(Color.White, r, Offset(x, y))
            val sw = Stroke(r * 0.12f)
            drawCircle(Ink, r * 0.75f, Offset(x, y), style = sw)
            drawCircle(Ink, r * 0.45f, Offset(x, y), style = sw)
            drawCircle(Ink, r * 0.12f, Offset(x, y))
        }
        2 -> {
            drawCircle(Color.White, r, Offset(x, y))
            drawCircle(Ink, r * 0.5f, Offset(x, y + r * 0.2f))
            drawRect(st.body, Offset(x - r * 1.05f, y - r * 1.05f), Size(r * 2.1f, r * 1.0f))
            drawLine(Ink, Offset(x - r, y - r * 0.05f), Offset(x + r, y - r * 0.05f), strokeWidth = r * 0.12f)
        }
        else -> {
            drawCircle(Color.White, r, Offset(x, y))
            drawCircle(Ink, r * 0.55f, Offset(x, y + r * 0.05f))
            drawCircle(Color.White, r * 0.18f, Offset(x - r * 0.18f, y - r * 0.15f))
        }
    }
}

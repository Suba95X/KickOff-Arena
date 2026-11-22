package com.example.ui.components

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.core.content.res.ResourcesCompat
import com.example.R
import com.example.game.ArenaConstants.BR
import com.example.game.ArenaConstants.CX
import com.example.game.ArenaConstants.CY
import com.example.game.ArenaConstants.FX0
import com.example.game.ArenaConstants.FX1
import com.example.game.ArenaConstants.FY0
import com.example.game.ArenaConstants.FY1
import com.example.game.ArenaConstants.GD
import com.example.game.ArenaConstants.GY0
import com.example.game.ArenaConstants.GY1
import com.example.game.ArenaConstants.H
import com.example.game.ArenaConstants.PR
import com.example.game.ArenaConstants.W
import com.example.game.ArenaScreenState
import com.example.game.ArenaViewModel
import com.example.game.PlayerActor
import com.example.ui.theme.ArenaBg
import com.example.ui.theme.ArenaCyan
import com.example.ui.theme.ArenaFire
import com.example.ui.theme.ArenaGold
import com.example.ui.theme.ArenaGreen
import com.example.ui.theme.ArenaRed
import com.example.ui.theme.PitchOuterGrass
import com.example.ui.theme.PitchStripeA
import com.example.ui.theme.PitchStripeB
import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun PitchCanvas(
    viewModel: ArenaViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val orbitronTypeface = remember(context) {
        try {
            ResourcesCompat.getFont(context, R.font.orbitron)?.let {
                Typeface.create(it, Typeface.BOLD)
            } ?: Typeface.DEFAULT_BOLD
        } catch (_: Throwable) {
            Typeface.DEFAULT_BOLD
        }
    }

    val textPaintFill = remember(orbitronTypeface) {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = orbitronTypeface
            textAlign = Paint.Align.CENTER
            style = Paint.Style.FILL
        }
    }
    val textPaintStroke = remember(orbitronTypeface) {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = orbitronTypeface
            textAlign = Paint.Align.CENTER
            style = Paint.Style.STROKE
            strokeJoin = Paint.Join.ROUND
        }
    }

    // 60fps Game Loop
    LaunchedEffect(viewModel) {
        var lastTime = withFrameMillis { it }
        while (true) {
            withFrameMillis { now ->
                val dt = (now - lastTime) / 1000f
                lastTime = now
                viewModel.stepFrame(dt, now)
            }
        }
    }

    // Observe frameTick to trigger redraw every frame
    val tick = viewModel.frameTick
    val settings = viewModel.settings
    val kitColor0 = remember(settings.kitColorHex) {
        parseHexColor(settings.kitColorHex, Color(0xFF3AA7FF))
    }
    val kitColor1 = remember(settings.kitColorHex) {
        // Ensure opponent has a high-contrast color if user selected red
        if (settings.kitColorHex.equals("#ff5364", ignoreCase = true)) {
            Color(0xFF3AA7FF)
        } else {
            ArenaRed
        }
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .testTag("game_canvas")
    ) {
        val vw = size.width
        val vh = size.height
        drawRect(color = ArenaBg, size = size)

        val sc = min(vw / W, vh / H)
        val ox = (vw - W * sc) / 2f
        val oy = (vh - H * sc) / 2f
        val shakeAmt = viewModel.shake
        val shx = if (shakeAmt > 0.05f) (Random.nextFloat() * 2f - 1f) * shakeAmt else 0f
        val shy = if (shakeAmt > 0.05f) (Random.nextFloat() * 2f - 1f) * shakeAmt else 0f

        withTransform({
            translate(left = ox + shx * sc, top = oy + shy * sc)
            scale(scaleX = sc, scaleY = sc, pivot = Offset.Zero)
        }) {
            drawPitchField()
            drawPowerupsLayer(viewModel, tick, textPaintFill)
            drawRingsLayer(viewModel)

            // Depth-sort players and ball by y coordinate
            val sortedPlayers = viewModel.players.sortedBy { it.y }
            var ballDrawn = false
            for (p in sortedPlayers) {
                if (!ballDrawn && p.y > viewModel.ball.y) {
                    drawBallActor(viewModel)
                    ballDrawn = true
                }
                val pCol = when {
                    p.isGk -> ArenaGold
                    p.team == 0 -> kitColor0
                    else -> kitColor1
                }
                drawPlayerActor(
                    p = p,
                    color = pCol,
                    isLocal = (p.index == viewModel.localId),
                    teamName = if (p.team == 0) viewModel.teamName0 else viewModel.teamName1,
                    jerseyNumber = if (p.index == viewModel.localId) settings.jerseyNumber else if (p.isGk) "1" else "9",
                    nowMillis = tick,
                    fillPaint = textPaintFill,
                    strokePaint = textPaintStroke
                )
            }
            if (!ballDrawn) {
                drawBallActor(viewModel)
            }

            drawParticlesLayer(viewModel)
            drawFloatingTextsLayer(viewModel, textPaintFill, textPaintStroke)
            drawCountdownLayer(viewModel, textPaintFill, textPaintStroke)
        }

        if (viewModel.flash > 0f) {
            drawRect(
                color = Color.White.copy(alpha = (viewModel.flash * 0.3f).coerceIn(0f, 1f)),
                size = size
            )
        }
    }
}

private fun DrawScope.drawPitchField() {
    val pw = FX1 - FX0
    val ph = FY1 - FY0
    val stripeCount = 10

    // Outer deep grass border
    drawRect(
        color = PitchOuterGrass,
        topLeft = Offset(FX0 - GD - 30f, FY0 - 30f),
        size = Size(pw + 2f * GD + 60f, ph + 60f)
    )

    // 10 alternating vertical pitch stripes
    val stripeW = pw / stripeCount
    for (i in 0 until stripeCount) {
        drawRect(
            color = if (i % 2 == 1) PitchStripeA else PitchStripeB,
            topLeft = Offset(FX0 + i * stripeW, FY0),
            size = Size(stripeW + 0.6f, ph)
        )
    }

    val lineColor = Color.White.copy(alpha = 0.94f)
    val lineStroke = 3f

    // Top and bottom touchlines
    drawLine(lineColor, Offset(FX0, FY0), Offset(FX1, FY0), strokeWidth = lineStroke, cap = StrokeCap.Round)
    drawLine(lineColor, Offset(FX0, FY1), Offset(FX1, FY1), strokeWidth = lineStroke, cap = StrokeCap.Round)

    // Left goal line segments
    drawLine(lineColor, Offset(FX0, FY0), Offset(FX0, GY0), strokeWidth = lineStroke, cap = StrokeCap.Round)
    drawLine(lineColor, Offset(FX0, GY1), Offset(FX0, FY1), strokeWidth = lineStroke, cap = StrokeCap.Round)

    // Right goal line segments
    drawLine(lineColor, Offset(FX1, FY0), Offset(FX1, GY0), strokeWidth = lineStroke, cap = StrokeCap.Round)
    drawLine(lineColor, Offset(FX1, GY1), Offset(FX1, FY1), strokeWidth = lineStroke, cap = StrokeCap.Round)

    // Halfway line & center circle
    drawLine(lineColor, Offset(CX, FY0), Offset(CX, FY1), strokeWidth = lineStroke, cap = StrokeCap.Round)
    drawCircle(lineColor, radius = 70f, center = Offset(CX, CY), style = Stroke(width = lineStroke))
    drawCircle(Color.White, radius = 4f, center = Offset(CX, CY))

    // Penalty boxes
    drawRect(
        color = lineColor,
        topLeft = Offset(FX0, CY - 140f),
        size = Size(130f, 280f),
        style = Stroke(width = lineStroke)
    )
    drawRect(
        color = lineColor,
        topLeft = Offset(FX1 - 130f, CY - 140f),
        size = Size(130f, 280f),
        style = Stroke(width = lineStroke)
    )

    // Goal areas
    drawRect(
        color = lineColor,
        topLeft = Offset(FX0, CY - 75f),
        size = Size(50f, 150f),
        style = Stroke(width = lineStroke)
    )
    drawRect(
        color = lineColor,
        topLeft = Offset(FX1 - 50f, CY - 75f),
        size = Size(50f, 150f),
        style = Stroke(width = lineStroke)
    )

    // Left and Right Goals
    drawGoalNet(FX0 - GD, FX0)
    drawGoalNet(FX1, FX1 + GD)
}

private fun DrawScope.drawGoalNet(x0: Float, x1: Float) {
    val topLeft = Offset(x0, GY0)
    val sz = Size(x1 - x0, GY1 - GY0)
    drawRect(color = Color.White.copy(alpha = 0.14f), topLeft = topLeft, size = sz)
    drawRect(color = Color.White, topLeft = topLeft, size = sz, style = Stroke(width = 4f))
}

private fun DrawScope.drawGroundShadow(x: Float, y: Float, rx: Float, ry: Float) {
    drawOval(
        color = Color.Black.copy(alpha = 0.26f),
        topLeft = Offset(x + 3f - rx, y - ry),
        size = Size(rx * 2f, ry * 2f)
    )
}

private fun DrawScope.drawPowerupsLayer(
    viewModel: ArenaViewModel,
    nowMillis: Long,
    fillPaint: Paint
) {
    for (pu in viewModel.powerups) {
        val floatY = pu.y + sin(nowMillis / 220f + pu.id) * 4f
        val isSpeed = pu.type == "speed"
        drawGroundShadow(pu.x, pu.y + 14f, 14f, 6f)
        drawCircle(
            color = if (isSpeed) ArenaCyan.copy(alpha = 0.6f) else ArenaFire.copy(alpha = 0.6f),
            radius = pu.r + 3f,
            center = Offset(pu.x, floatY),
            style = Stroke(width = 2.5f)
        )
        drawCircle(
            color = if (isSpeed) Color(0xFF00C8E0) else Color(0xFFE65C00),
            radius = pu.r,
            center = Offset(pu.x, floatY)
        )
        drawContext.canvas.nativeCanvas.apply {
            fillPaint.textSize = 14f
            fillPaint.color = android.graphics.Color.WHITE
            drawText(if (isSpeed) "⚡" else "🔥", pu.x, floatY + 5f, fillPaint)
        }
    }
}

private fun DrawScope.drawRingsLayer(viewModel: ArenaViewModel) {
    for (r in viewModel.rings) {
        val k = (1f - r.life / r.maxLife).coerceIn(0f, 1f)
        val alpha = (1f - k).coerceIn(0f, 1f)
        val curR = r.maxRadius * (1f - (1f - k).pow(3)) + 2f
        drawCircle(
            color = r.color.copy(alpha = r.color.alpha * alpha),
            radius = curR,
            center = Offset(r.x, r.y),
            style = Stroke(width = 5f * (1f - k) + 1f)
        )
    }
}

private fun DrawScope.drawPlayerActor(
    p: PlayerActor,
    color: Color,
    isLocal: Boolean,
    teamName: String,
    jerseyNumber: String,
    nowMillis: Long,
    fillPaint: Paint,
    strokePaint: Paint
) {
    drawGroundShadow(p.x, p.y + PR * 0.75f, PR * 0.95f, PR * 0.4f)
    val s = 1f + p.sq * 0.25f

    withTransform({
        translate(left = p.x, top = p.y)
        scale(scaleX = s, scaleY = 1f / s, pivot = Offset.Zero)
    }) {
        if (isLocal) {
            val pulse = 0.5f + 0.5f * sin(nowMillis / 160f)
            drawCircle(
                color = Color.White.copy(alpha = (0.35f + pulse * 0.35f).coerceIn(0f, 1f)),
                radius = PR + 6f + pulse * 2f,
                center = Offset.Zero,
                style = Stroke(width = 3f)
            )
        }
        drawCircle(color = color, radius = PR, center = Offset.Zero)
        drawCircle(color = Color.White, radius = PR, center = Offset.Zero, style = Stroke(width = 3f))
    }

    // Draw jersey number inside player disk
    drawContext.canvas.nativeCanvas.apply {
        fillPaint.textSize = 11f
        fillPaint.color = if (p.isGk) android.graphics.Color.rgb(24, 15, 0) else android.graphics.Color.WHITE
        drawText(jerseyNumber, p.x, p.y + 4f, fillPaint)

        // Draw overhead label or active power-up icons
        if (p.buffSpeed > 0f || p.buffKick > 0f) {
            fillPaint.textSize = 13f
            fillPaint.color = (if (p.buffKick > 0f) ArenaFire else ArenaCyan).toArgb()
            val iconStr = buildString {
                if (p.buffSpeed > 0f) append("⚡ ")
                if (p.buffKick > 0f) append("🔥")
            }
            drawText(iconStr, p.x, p.y - PR - 12f, fillPaint)
        } else if (!p.isGk) {
            val label = if (p.human) teamName.uppercase() else "CPU"
            strokePaint.textSize = 11f
            strokePaint.strokeWidth = 3.2f
            strokePaint.color = android.graphics.Color.argb(210, 5, 8, 20)
            drawText(label, p.x, p.y - PR - 10f, strokePaint)

            fillPaint.textSize = 11f
            fillPaint.color = android.graphics.Color.WHITE
            drawText(label, p.x, p.y - PR - 10f, fillPaint)
        }
    }

    // Stamina bar below local player
    if (isLocal && p.stam < 0.999f) {
        val w = 40f
        val y = p.y + PR + 8f
        drawRect(
            color = Color.Black.copy(alpha = 0.45f),
            topLeft = Offset(p.x - w / 2f, y),
            size = Size(w, 4f)
        )
        val barColor = when {
            p.buffSpeed > 0f -> ArenaCyan
            p.stam > 0.25f -> ArenaGreen
            else -> ArenaRed
        }
        drawRect(
            color = barColor,
            topLeft = Offset(p.x - w / 2f, y),
            size = Size(w * p.stam.coerceIn(0f, 1f), 4f)
        )
    }
}

private fun DrawScope.drawBallActor(viewModel: ArenaViewModel) {
    val b = viewModel.ball
    drawGroundShadow(b.x, b.y + BR * 0.8f, BR * 0.9f, BR * 0.4f)

    val trailSize = b.trail.size
    for (i in 0 until trailSize) {
        val pt = b.trail[i]
        val alpha = (i.toFloat() / trailSize) * 0.35f
        val r = BR * (0.3f + 0.6f * i.toFloat() / trailSize)
        drawCircle(
            color = (if (b.fireT > 0f) ArenaFire else Color.White).copy(alpha = alpha),
            radius = r,
            center = Offset(pt.x, pt.y)
        )
    }

    drawCircle(
        color = if (b.fireT > 0f) Color(0xFFFFE082) else Color(0xFFF5F7FB),
        radius = BR,
        center = Offset(b.x, b.y)
    )

    val spotColor = if (b.fireT > 0f) Color(0xFFD84315) else Color(0xFF1B2236)
    for (k in 0 until 5) {
        val a = b.rot + k * (PI.toFloat() * 2f) / 5f
        val sx = b.x + cos(a) * BR * 0.55f
        val sy = b.y + sin(a) * BR * 0.55f
        drawCircle(
            color = spotColor,
            radius = BR * 0.3f,
            center = Offset(sx, sy)
        )
    }

    drawCircle(
        color = Color.Black.copy(alpha = 0.45f),
        radius = BR,
        center = Offset(b.x, b.y),
        style = Stroke(width = 1.5f)
    )
}

private fun DrawScope.drawParticlesLayer(viewModel: ArenaViewModel) {
    for (p in viewModel.particles) {
        val k = (p.life / p.maxLife).coerceIn(0f, 1f)
        val alpha = (k * 1.5f).coerceIn(0f, 1f)
        val col = p.color.copy(alpha = alpha)
        if (p.isRect) {
            withTransform({
                translate(left = p.x, top = p.y)
                rotate(degrees = p.angle * (180f / PI.toFloat()), pivot = Offset.Zero)
            }) {
                drawRect(
                    color = col,
                    topLeft = Offset(-p.size * 0.5f, -p.size * 0.3f),
                    size = Size(p.size, p.size * 0.6f)
                )
            }
        } else {
            drawCircle(
                color = col,
                radius = max(0.5f, p.size * (0.4f + 0.6f * k)),
                center = Offset(p.x, p.y)
            )
        }
    }
}

private fun DrawScope.drawFloatingTextsLayer(
    viewModel: ArenaViewModel,
    fillPaint: Paint,
    strokePaint: Paint
) {
    for (t in viewModel.floatingTexts) {
        val k = (1f - t.life / t.maxLife).coerceIn(0f, 1f)
        val sc = if (k < 0.18f) easeOutBack(k / 0.18f) else 1f
        val alpha = ((t.life * 2.5f).coerceIn(0f, 1f) * 255).toInt()
        withTransform({
            translate(left = t.x, top = t.y - k * 24f)
            scale(scaleX = sc, scaleY = sc, pivot = Offset.Zero)
        }) {
            drawContext.canvas.nativeCanvas.apply {
                strokePaint.textSize = t.fontSize
                strokePaint.strokeWidth = t.fontSize * 0.12f
                strokePaint.color = android.graphics.Color.argb((alpha * 0.85f).toInt(), 5, 8, 20)

                fillPaint.textSize = t.fontSize
                fillPaint.color = t.color.copy(alpha = alpha / 255f).toArgb()

                val yOffset = t.fontSize * 0.35f
                drawText(t.text, 0f, yOffset, strokePaint)
                drawText(t.text, 0f, yOffset, fillPaint)
            }
        }
    }
}

private fun DrawScope.drawCountdownLayer(
    viewModel: ArenaViewModel,
    fillPaint: Paint,
    strokePaint: Paint
) {
    val cd = viewModel.countdownTime
    if (viewModel.screenState == ArenaScreenState.COUNTDOWN && cd > 0f) {
        val n = ceil(cd).toInt()
        val frac = cd - (n - 1)
        val alpha = ((frac * 3f).coerceIn(0f, 1f) * 255).toInt()
        val sc = 1.3f - 0.3f * frac
        withTransform({
            translate(left = CX, top = CY)
            scale(scaleX = sc, scaleY = sc, pivot = Offset.Zero)
        }) {
            drawContext.canvas.nativeCanvas.apply {
                val fontSize = 130f
                strokePaint.textSize = fontSize
                strokePaint.strokeWidth = 10f
                strokePaint.color = android.graphics.Color.argb((alpha * 0.7f).toInt(), 5, 8, 20)

                fillPaint.textSize = fontSize
                fillPaint.color = ArenaGold.copy(alpha = alpha / 255f).toArgb()

                val yOffset = fontSize * 0.35f
                drawText(n.toString(), 0f, yOffset, strokePaint)
                drawText(n.toString(), 0f, yOffset, fillPaint)
            }
        }
    }
}

private fun easeOutBack(x: Float): Float {
    val c1 = 1.70158f
    val c3 = c1 + 1f
    return 1f + c3 * (x - 1f).pow(3) + c1 * (x - 1f).pow(2)
}

fun parseHexColor(hex: String, fallback: Color): Color {
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (_: Throwable) {
        fallback
    }
}

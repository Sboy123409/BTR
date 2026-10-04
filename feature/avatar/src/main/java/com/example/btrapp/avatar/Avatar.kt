package com.example.btrapp.avatar

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.example.btrapp.core.ui.theme.PosterBlack
import com.example.btrapp.core.ui.theme.PosterCream
import com.example.btrapp.core.ui.theme.PosterRed
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * A comic-style avatar: flat fills with heavy ink outlines on a halftone disc.
 * [animated] adds blinking and a hop when the mood improves; turn it off for small previews.
 */
@Composable
fun Avatar(
    config: AvatarConfig,
    mood: Mood,
    modifier: Modifier = Modifier,
    animated: Boolean = true,
) {
    var blinking by remember { mutableStateOf(false) }
    val hop = remember { Animatable(1f) }
    var lastMood by remember { mutableStateOf(mood) }

    if (animated) {
        LaunchedEffect(Unit) {
            while (true) {
                delay(Random.nextLong(2_500, 5_500))
                blinking = true
                delay(140)
                blinking = false
            }
        }
        LaunchedEffect(mood) {
            if (mood > lastMood) {
                hop.snapTo(0.88f)
                hop.animateTo(1f, spring(dampingRatio = 0.35f, stiffness = 300f))
            }
            lastMood = mood
        }
    }

    Canvas(
        modifier
            .aspectRatio(1f)
            .graphicsLayer {
                scaleX = hop.value
                scaleY = hop.value
            }
            .semantics { contentDescription = "Avatar" },
    ) {
        val k = size.minDimension / 100f
        withTransform({ scale(k, k, pivot = Offset.Zero) }) {
            drawAvatar(config, mood, blinking)
        }
    }
}

// Everything below draws on a 100 × 100 grid.

private val Ink = PosterBlack
private val InkStroke = Stroke(width = 2.2f, join = StrokeJoin.Round, cap = StrokeCap.Round)
private val MouthRed = Color(0xFF8B1E2B)
private val Blush = Color(0x73FF7A8A)
private val Gold = Color(0xFFF2C94C)

/** Where the head sits; the face and hair are placed relative to it. */
private class Head(val path: Path, val l: Float, val t: Float, val r: Float, val b: Float) {
    val eyeY get() = (t + b) / 2 + 2
    val mouthY get() = b - 10
}

private fun headFor(shape: FaceShape): Head = when (shape) {
    FaceShape.Round -> Rect(28f, 22f, 72f, 66f).let { Head(Path().apply { addOval(it) }, 28f, 22f, 72f, 66f) }
    FaceShape.Oval -> Rect(30f, 19f, 70f, 67f).let { Head(Path().apply { addOval(it) }, 30f, 19f, 70f, 67f) }
    FaceShape.Square -> Head(
        Path().apply { addRoundRect(RoundRect(Rect(28f, 22f, 72f, 66f), CornerRadius(13f))) },
        28f, 22f, 72f, 66f,
    )
}

private fun DrawScope.inked(path: Path, fill: Color) {
    drawPath(path, fill)
    drawPath(path, Ink, style = InkStroke)
}

private fun roundRect(l: Float, t: Float, r: Float, b: Float, radius: Float) =
    Path().apply { addRoundRect(RoundRect(Rect(l, t, r, b), CornerRadius(radius))) }

private fun oval(l: Float, t: Float, r: Float, b: Float) = Path().apply { addOval(Rect(l, t, r, b)) }

private fun circle(cx: Float, cy: Float, radius: Float) = oval(cx - radius, cy - radius, cx + radius, cy + radius)

private fun DrawScope.drawAvatar(config: AvatarConfig, mood: Mood, blinking: Boolean) {
    val skin = AvatarPalette.skin[config.skin]
    val hairColor = AvatarPalette.hair[config.hairColor]
    val outfitColor = AvatarPalette.outfit[config.outfitColor]
    val bg = AvatarPalette.background[config.background]
    val head = headFor(config.face)
    val disc = circle(50f, 50f, 49f)

    clipPath(disc) {
        drawRect(bg, Offset.Zero, Size(100f, 100f))
        drawHalftone()
        drawBackHair(config.hair, hairColor, head)
        drawBody(config.outfit, outfitColor, skin, head)
        // Ears, then the head over them.
        inked(circle(head.l, head.eyeY + 1, 5.5f), skin)
        inked(circle(head.r, head.eyeY + 1, 5.5f), skin)
        inked(head.path, skin)
        drawFace(config, mood, blinking, head, hairColor)
        drawFrontHair(config.hair, hairColor, outfitColor, head)
        drawAccessory(config.accessory, outfitColor, head)
        when (mood) {
            Mood.Ecstatic -> drawSparkles()
            Mood.Sleepy -> drawZzz()
            else -> Unit
        }
    }
    drawPath(disc, Ink, style = InkStroke)
}

/** Comic-print dots, fading from top-left to bottom-right. */
private fun DrawScope.drawHalftone() {
    val dot = Ink.copy(alpha = 0.08f)
    for (row in 0..11) {
        for (col in 0..11) {
            val x = col * 9f + if (row % 2 == 0) 2f else 6.5f
            val y = row * 9f + 2f
            val radius = 0.4f + 1.8f * (1f - (x + y) / 200f)
            drawCircle(dot, radius, Offset(x, y))
        }
    }
}

private fun DrawScope.drawBackHair(style: HairStyle, color: Color, h: Head) {
    when (style) {
        HairStyle.Bob -> inked(roundRect(h.l - 4, h.t - 4, h.r + 4, h.t + 44, 18f), color)
        HairStyle.Long -> inked(roundRect(h.l - 5, h.t - 4, h.r + 5, h.b + 24, 20f), color)
        HairStyle.Bun -> inked(circle(50f, h.t - 5, 9f), color)
        else -> Unit
    }
}

private fun DrawScope.drawBody(style: OutfitStyle, color: Color, skin: Color, h: Head) {
    if (style == OutfitStyle.Hoodie) {
        inked(roundRect(31f, h.b - 14, 69f, h.b + 16, 16f), color.darker())
    }
    inked(roundRect(44f, h.b - 8, 56f, 82f, 0f), skin)
    when (style) {
        OutfitStyle.Tank -> {
            inked(roundRect(14f, 77f, 86f, 130f, 22f), skin)
            inked(roundRect(29f, 81f, 71f, 130f, 10f), color)
            drawNeckline(skin, 42f, 76f, 58f, 88f)
        }
        else -> {
            inked(roundRect(14f, 76f, 86f, 130f, 22f), color)
            drawNeckline(skin, 42f, 70f, 58f, 84f)
        }
    }
    when (style) {
        OutfitStyle.Hoodie -> {
            listOf(46f to 45f, 54f to 55f).forEach { (top, bottom) ->
                drawLine(PosterCream, Offset(top, 83f), Offset(bottom, 92f), strokeWidth = 1.6f, cap = StrokeCap.Round)
                drawCircle(PosterCream, 1.4f, Offset(bottom, 92.5f))
            }
        }
        OutfitStyle.Collar -> {
            inked(Path().apply { moveTo(50f, 81f); lineTo(39f, 75f); lineTo(42f, 87f); close() }, Color.White)
            inked(Path().apply { moveTo(50f, 81f); lineTo(61f, 75f); lineTo(58f, 87f); close() }, Color.White)
        }
        else -> Unit
    }
}

/** A skin-coloured U where the neck meets the top. */
private fun DrawScope.drawNeckline(skin: Color, l: Float, t: Float, r: Float, b: Float) {
    drawArc(skin, 0f, 180f, useCenter = true, topLeft = Offset(l, t), size = Size(r - l, b - t))
    drawArc(Ink, 0f, 180f, useCenter = false, topLeft = Offset(l, t), size = Size(r - l, b - t), style = InkStroke)
}

private fun DrawScope.drawFace(config: AvatarConfig, mood: Mood, blinking: Boolean, h: Head, hairColor: Color) {
    val y = h.eyeY
    val browColor = if (config.hair == HairStyle.None) Ink else hairColor

    // Eyes
    for (x in listOf(41f, 59f)) {
        when {
            mood == Mood.Ecstatic -> drawArc(
                Ink, 180f, 180f, useCenter = false,
                topLeft = Offset(x - 4.5f, y - 2f), size = Size(9f, 8f), style = InkStroke,
            )
            blinking -> drawArc(
                Ink, 0f, 180f, useCenter = false,
                topLeft = Offset(x - 4.5f, y - 3f), size = Size(9f, 6f), style = InkStroke,
            )
            mood == Mood.Sleepy -> {
                drawLine(Ink, Offset(x - 4.5f, y + 1f), Offset(x + 4.5f, y + 1f), strokeWidth = 2.2f, cap = StrokeCap.Round)
                drawLine(Ink, Offset(x - 3f, y + 1f), Offset(x - 4f, y + 3.5f), strokeWidth = 1.4f, cap = StrokeCap.Round)
                drawLine(Ink, Offset(x + 3f, y + 1f), Offset(x + 4f, y + 3.5f), strokeWidth = 1.4f, cap = StrokeCap.Round)
            }
            else -> drawEye(config.eyes, x, y)
        }
    }

    // Brows
    val by = y - 8.5f
    for (side in listOf(-1f, 1f)) {
        fun mx(x: Float) = if (side < 0) x else 100f - x
        val stroke = Stroke(
            width = when (config.brows) {
                BrowStyle.Soft -> 1.8f
                BrowStyle.Bold -> 3.6f
                BrowStyle.Flat -> 3f
            },
            cap = StrokeCap.Round,
        )
        val brow = Path().apply {
            moveTo(mx(36.5f), by + 1f)
            if (config.brows == BrowStyle.Flat) lineTo(mx(45.5f), by)
            else quadraticTo(mx(41f), by - 2.5f, mx(45.5f), by + 0.5f)
        }
        drawPath(brow, browColor, style = stroke)
    }

    // Nose
    drawPath(
        Path().apply { moveTo(49f, y + 5f); quadraticTo(47.5f, y + 9f, 50.5f, y + 9.5f) },
        Ink, style = Stroke(width = 1.6f, cap = StrokeCap.Round),
    )

    // Mouth and cheeks
    val my = h.mouthY
    if (mood >= Mood.Happy) {
        drawCircle(Blush, 3.4f, Offset(36f, my - 3f))
        drawCircle(Blush, 3.4f, Offset(64f, my - 3f))
    }
    when (mood) {
        Mood.Sleepy -> inked(oval(48f, my - 1.5f, 52f, my + 3f), MouthRed)
        Mood.Neutral -> drawPath(
            Path().apply { moveTo(45f, my); quadraticTo(50f, my + 3.5f, 55f, my) },
            Ink, style = InkStroke,
        )
        Mood.Happy -> inked(
            Path().apply { moveTo(43.5f, my - 1f); quadraticTo(50f, my + 9f, 56.5f, my - 1f); close() },
            MouthRed,
        )
        Mood.Ecstatic -> {
            val grin = Path().apply { moveTo(41f, my - 2f); quadraticTo(50f, my + 12f, 59f, my - 2f); close() }
            drawPath(grin, MouthRed)
            clipPath(grin) { drawRect(Color.White, Offset(40f, my - 2f), Size(20f, 3.2f)) }
            drawPath(grin, Ink, style = InkStroke)
        }
    }
}

private fun DrawScope.drawEye(style: EyeStyle, x: Float, y: Float) {
    when (style) {
        EyeStyle.Dots -> drawCircle(Ink, 2.6f, Offset(x, y))
        EyeStyle.Round -> {
            inked(circle(x, y, 5f), Color.White)
            drawCircle(Ink, 2.4f, Offset(x + 0.6f, y + 0.6f))
            drawCircle(Color.White, 0.9f, Offset(x - 0.3f, y - 0.5f))
        }
        EyeStyle.Anime -> {
            inked(oval(x - 4.5f, y - 6f, x + 4.5f, y + 6f), Color.White)
            drawPath(oval(x - 3.2f, y - 4f, x + 3.2f, y + 5f), Ink)
            drawCircle(Color.White, 1.5f, Offset(x - 1f, y - 1.8f))
        }
        EyeStyle.Sharp -> {
            inked(
                Path().apply { moveTo(x - 5f, y); quadraticTo(x, y - 6f, x + 5f, y - 1f); quadraticTo(x, y + 4f, x - 5f, y); close() },
                Color.White,
            )
            drawCircle(Ink, 2.2f, Offset(x, y - 0.6f))
        }
    }
}

private fun DrawScope.drawFrontHair(style: HairStyle, color: Color, capColor: Color, h: Head) {
    val l = h.l
    val t = h.t
    val r = h.r
    val shortTop = Path().apply {
        moveTo(l - 1, t + 18)
        cubicTo(l - 2, t - 14, r + 2, t - 14, r + 1, t + 18)
        lineTo(r - 3, t + 16)
        quadraticTo(r - 10, t + 6, 54f, t + 9)
        quadraticTo(l + 12, t + 4, l + 3, t + 17)
        close()
    }
    when (style) {
        HairStyle.None -> Unit
        HairStyle.Short, HairStyle.Bun -> inked(shortTop, color)
        HairStyle.Spiky -> inked(
            Path().apply {
                moveTo(l - 1, t + 18)
                lineTo(l - 3, t + 2)
                lineTo(l + 6, t + 4)
                lineTo(l + 6, t - 8)
                lineTo(l + 16, t - 2)
                lineTo(48f, t - 14)
                lineTo(r - 14, t - 2)
                lineTo(r - 6, t - 8)
                lineTo(r - 6, t + 4)
                lineTo(r + 3, t + 2)
                lineTo(r + 1, t + 18)
                lineTo(r - 3, t + 16)
                quadraticTo(r - 10, t + 6, 54f, t + 9)
                quadraticTo(l + 12, t + 4, l + 3, t + 17)
                close()
            },
            color,
        )
        HairStyle.Bob, HairStyle.Long -> inked(
            Path().apply {
                moveTo(l - 3, t + 34)
                cubicTo(l - 4, t - 14, r + 4, t - 14, r + 3, t + 34)
                lineTo(r - 1, t + 34)
                lineTo(r - 1, t + 20)
                quadraticTo(r - 8, t + 6, 50f, t + 10)
                quadraticTo(l + 8, t + 6, l + 1, t + 20)
                lineTo(l + 1, t + 34)
                close()
            },
            color,
        )
        HairStyle.Cap -> {
            inked(roundRect(l - 1, t + 12, l + 4, t + 24, 2f), color)
            inked(roundRect(r - 4, t + 12, r + 1, t + 24, 2f), color)
            inked(
                Path().apply { moveTo(l - 2, t + 14); cubicTo(l - 2, t - 14, r + 2, t - 14, r + 2, t + 14); close() },
                capColor,
            )
            inked(roundRect(l - 2, t + 10, r + 12, t + 17, 3.5f), capColor.darker())
        }
    }
}

private fun DrawScope.drawAccessory(accessory: Accessory, color: Color, h: Head) {
    val y = h.eyeY
    when (accessory) {
        Accessory.None -> Unit
        Accessory.Glasses -> {
            for (x in listOf(41f, 59f)) {
                drawCircle(Color.White.copy(alpha = 0.18f), 6.5f, Offset(x, y))
                drawCircle(Ink, 6.5f, Offset(x, y), style = InkStroke)
            }
            drawLine(Ink, Offset(47.5f, y), Offset(52.5f, y), strokeWidth = 2.2f)
            drawLine(Ink, Offset(34.5f, y - 1f), Offset(h.l, y - 2f), strokeWidth = 2.2f, cap = StrokeCap.Round)
            drawLine(Ink, Offset(65.5f, y - 1f), Offset(h.r, y - 2f), strokeWidth = 2.2f, cap = StrokeCap.Round)
        }
        Accessory.Sunglasses -> {
            drawPath(roundRect(32.5f, y - 5f, 48f, y + 5f, 4f), Ink)
            drawPath(roundRect(52f, y - 5f, 67.5f, y + 5f, 4f), Ink)
            drawLine(Ink, Offset(47f, y - 2f), Offset(53f, y - 2f), strokeWidth = 2.2f)
            drawLine(Ink, Offset(33f, y - 2f), Offset(h.l, y - 3f), strokeWidth = 2.2f, cap = StrokeCap.Round)
            drawLine(Ink, Offset(67f, y - 2f), Offset(h.r, y - 3f), strokeWidth = 2.2f, cap = StrokeCap.Round)
            for (x in listOf(36f, 55.5f)) {
                drawLine(Color.White.copy(alpha = 0.6f), Offset(x, y - 1.5f), Offset(x + 3f, y - 3.5f), strokeWidth = 1.4f, cap = StrokeCap.Round)
            }
        }
        Accessory.Headphones -> {
            drawArc(
                Ink, 180f, 180f, useCenter = false,
                topLeft = Offset(h.l - 5, h.t - 10), size = Size(h.r - h.l + 10, y + 30 - h.t),
                style = Stroke(width = 4f, cap = StrokeCap.Round),
            )
            inked(roundRect(h.l - 8, y - 6, h.l + 1, y + 9, 4f), color)
            inked(roundRect(h.r - 1, y - 6, h.r + 8, y + 9, 4f), color)
        }
        Accessory.Bandana -> {
            inked(roundRect(h.l - 1, h.t + 7, h.r + 1, h.t + 14, 2f), PosterRed)
            for (x in listOf(h.l + 6, 44f, 56f, h.r - 6)) drawCircle(PosterCream, 1f, Offset(x, h.t + 10.5f))
            inked(
                Path().apply {
                    moveTo(h.r, h.t + 10); lineTo(h.r + 9, h.t + 5); lineTo(h.r + 7, h.t + 13)
                    lineTo(h.r + 10, h.t + 18); lineTo(h.r, h.t + 12); close()
                },
                PosterRed,
            )
        }
    }
}

private fun DrawScope.drawSparkles() {
    listOf(Triple(17f, 24f, 6f), Triple(85f, 32f, 5f), Triple(78f, 12f, 3.5f)).forEach { (x, y, size) ->
        val star = Path().apply {
            for (i in 0 until 8) {
                val radius = if (i % 2 == 0) size else size * 0.35f
                val angle = i * PI / 4 - PI / 2
                val px = x + (radius * cos(angle)).toFloat()
                val py = y + (radius * sin(angle)).toFloat()
                if (i == 0) moveTo(px, py) else lineTo(px, py)
            }
            close()
        }
        inked(star, Gold)
    }
}

private fun DrawScope.drawZzz() {
    listOf(Triple(74f, 20f, 6f), Triple(83f, 11f, 4f)).forEach { (x, y, s) ->
        drawPath(
            Path().apply { moveTo(x, y); lineTo(x + s, y); lineTo(x, y + s); lineTo(x + s, y + s) },
            Ink, style = Stroke(width = 1.8f, cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
    }
}

private fun Color.darker(factor: Float = 0.75f) =
    Color(red * factor, green * factor, blue * factor, alpha)

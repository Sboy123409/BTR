package com.example.btrapp.ui.welcome

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.btrapp.R
import kotlinx.coroutines.delay

private val Crimson = Color(0xFFC1121F)
private val TearBlue = Color(0xFF8FA9C4)
private val Gold = Color(0xFFD4AF37)
private val DeepRed = Color(0xFF3B0A0F)
private val NearBlack = Color(0xFF0D0D0D)
private val Ink = Color(0xFFF5F0E8)
private val InkMuted = Color(0xFFB8AFA6)

private data class Pillar(val word: String, val color: Color, val line: String)

private val pillars = listOf(
    Pillar("Blood", Crimson, "Earn your unlocks with real reps."),
    Pillar("Tears", TearBlue, "Lock the apps that steal your time."),
    Pillar("Respect", Gold, "Build discipline you can be proud of."),
)

/** First-launch brand screen. Always dark: it's a brand moment, not a themed surface. */
@Composable
fun WelcomeScreen(onStart: () -> Unit, modifier: Modifier = Modifier) {
    // Steps reveal the wordmark, each pillar, the slogan, then the button.
    var step by rememberSaveable { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (step < 6) {
            delay(if (step == 0) 200 else 450)
            step++
        }
    }

    Box(
        modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(DeepRed, NearBlack, NearBlack))),
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(48.dp))
            Reveal(step >= 1) {
                Text(
                    "BTR",
                    color = Ink,
                    fontSize = 88.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 12.sp,
                )
            }
            Spacer(Modifier.height(8.dp))
            Reveal(step >= 1) {
                Text(
                    "BLOOD · TEARS · RESPECT",
                    color = InkMuted,
                    style = MaterialTheme.typography.labelLarge,
                    letterSpacing = 4.sp,
                )
            }

            Spacer(Modifier.height(40.dp))
            Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                pillars.forEachIndexed { index, pillar ->
                    Reveal(step >= 2 + index) { PillarRow(pillar) }
                }
            }

            Spacer(Modifier.height(44.dp))
            Reveal(step >= 5) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        Modifier
                            .width(48.dp)
                            .height(2.dp)
                            .background(Gold),
                    )
                    Spacer(Modifier.height(20.dp))
                    Text(
                        "“${stringResource(R.string.btr_slogan)}”",
                        color = Ink,
                        style = MaterialTheme.typography.titleLarge,
                        fontStyle = FontStyle.Italic,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            Spacer(Modifier.height(48.dp))
            Reveal(step >= 6) {
                Button(
                    onClick = onStart,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Crimson,
                        contentColor = Color.White,
                    ),
                ) {
                    Text("Get started", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

@Composable
private fun PillarRow(pillar: Pillar) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(12.dp)
                .background(pillar.color, CircleShape),
        )
        Spacer(Modifier.width(16.dp))
        Column {
            Text(
                pillar.word.uppercase(),
                color = pillar.color,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 3.sp,
            )
            Text(pillar.line, color = InkMuted, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

/** Fades and lifts [content] in. Space is reserved up front so the layout doesn't jump. */
@Composable
private fun Reveal(visible: Boolean, content: @Composable () -> Unit) {
    val progress by animateFloatAsState(
        if (visible) 1f else 0f,
        animationSpec = tween(600),
        label = "reveal",
    )
    Box(
        Modifier.graphicsLayer {
            alpha = progress
            translationY = (1f - progress) * 24.dp.toPx()
        },
    ) { content() }
}

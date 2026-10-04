package com.example.btrapp.ui.welcome

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.btrapp.R
import com.example.btrapp.ui.theme.PosterBlack
import com.example.btrapp.ui.theme.PosterCream
import com.example.btrapp.ui.theme.PosterRed
import kotlinx.coroutines.delay

/** First-launch brand screen, built around the BTR poster art. Always dark. */
@Composable
fun WelcomeScreen(onStart: () -> Unit, modifier: Modifier = Modifier) {
    // 1: poster punches in, 2: slogan, 3: button.
    var step by rememberSaveable { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (step < 3) {
            delay(if (step == 0) 150 else 500)
            step++
        }
    }

    val posterScale by animateFloatAsState(
        if (step >= 1) 1f else 0.85f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "posterScale",
    )

    Column(
        modifier
            .fillMaxSize()
            .background(PosterBlack)
            .safeDrawingPadding()
            .padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            Reveal(step >= 1) {
                Image(
                    painter = painterResource(R.drawable.welcome_hero),
                    contentDescription = "BTR: Blood, Tears, Respect",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            scaleX = posterScale
                            scaleY = posterScale
                        },
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        Reveal(step >= 2) {
            Text(
                "“${stringResource(R.string.btr_slogan)}”",
                color = PosterCream.copy(alpha = 0.85f),
                style = MaterialTheme.typography.titleMedium,
                fontStyle = FontStyle.Italic,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp),
            )
        }

        Spacer(Modifier.height(28.dp))
        Reveal(step >= 3) {
            Button(
                onClick = onStart,
                modifier = Modifier
                    .widthIn(min = 220.dp)
                    .height(60.dp),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PosterRed,
                    contentColor = Color.White,
                ),
            ) {
                Text(
                    "Get started",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        Spacer(Modifier.height(16.dp))
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

package com.hifnawy.alquran.view.player

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import kotlin.random.Random

@Composable
fun AnimatedAudioBars(
        modifier: Modifier = Modifier,
        width: Dp = 40.dp,
        height: Dp = 40.dp,
        barCount: Int = 7,
        barWidth: Dp = 3.dp,
        barSpacing: Dp = 2.dp,
        color: Color = MaterialTheme.colorScheme.tertiary,
        durationRangeMs: IntRange = 100..150
) {
    val barMaxHeight = height - 5.dp
    val barMinHeight = 5.dp

    Row(
            modifier = modifier
                .width(width)
                .height(height)
                .clipToBounds(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
    ) {
        repeat(barCount) { index ->
            RandomHeightBar(
                    barWidth = barWidth,
                    barMinHeight = barMinHeight,
                    barMaxHeight = barMaxHeight,
                    color = color,
                    durationRangeMs = durationRangeMs
            )

            if (index >= barCount - 1) return@repeat
            Spacer(Modifier.width(barSpacing))
        }
    }
}

@Composable
private fun RandomHeightBar(
        barWidth: Dp,
        barMinHeight: Dp,
        barMaxHeight: Dp,
        color: Color,
        durationRangeMs: IntRange
) {
    val anim = remember { Animatable(Random.nextFloat()) }
    val height = lerp(barMinHeight, barMaxHeight, anim.value)

    LaunchedEffect(Unit) {
        while (true) {
            val next = Random.nextFloat()
            anim.animateTo(
                    targetValue = next,
                    animationSpec = tween(durationMillis = Random.nextInt(durationRangeMs.first, durationRangeMs.last), easing = LinearEasing)
            )
        }
    }

    Box(
            modifier = Modifier
                .width(barWidth)
                .height(height)
                .clip(RoundedCornerShape(percent = 50))
                .background(color)
    )
}

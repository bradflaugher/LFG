/*
 * LFE — A low-feature, on-device AI agent for Android.
 * Copyright (C) 2026 Brad Flaugher
 *
 * Licensed under the GNU General Public License v3.0 or later.
 * See LICENSE in the project root for terms.
 *
 * Includes code adapted from Google AI Edge Gallery (Apache 2.0,
 * Copyright 2025 Google LLC) — https://github.com/google-ai-edge/gallery.
 */
package com.bradflaugher.lfe.ui.common

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush.Companion.linearGradient
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.bradflaugher.lfe.R
import com.bradflaugher.lfe.data.Task

/**
 * Composable that displays an icon representing a task. It consists of a background image and a
 * foreground icon, both centered within a square box.
 */
@Composable
fun TaskIcon(
  task: Task,
  modifier: Modifier = Modifier,
  width: Dp = 56.dp,
  animationProgress: Float = 1f,
) {
  val revealingBrush =
    linearGradient(
      colorStops =
        arrayOf(
          (1f + 0.2f) * (1 - animationProgress) - 0.2f to Color.Red,
          (1f + 0.2f) * (1 - animationProgress) to Color.Transparent,
        ),
    )
  Box(modifier = modifier.width(width).aspectRatio(1f), contentAlignment = Alignment.Center) {
    val brush = linearGradient(colors = getTaskBgGradientColors(task = task))
    Image(
      painter = painterResource(R.drawable.circle),
      contentDescription = null,
      modifier =
        Modifier.fillMaxSize()
          .graphicsLayer(
            // This is important to make blending mode work.
            alpha = 0.99f,
            compositingStrategy = CompositingStrategy.Offscreen,
            translationX = 80 * (1 - animationProgress),
            rotationZ = -180 * (1 - animationProgress),
          )
          .drawWithContent {
            drawContent()
            drawRect(brush = brush, blendMode = BlendMode.SrcIn)
            drawRect(brush = revealingBrush, blendMode = BlendMode.DstOut)
          },
      contentScale = ContentScale.FillHeight,
    )
    var iconAnimationProgress = 0f
    if (animationProgress >= 0.8) {
      iconAnimationProgress = (animationProgress - 0.8f) / 0.2f
    }
    Icon(
      ImageVector.vectorResource(R.drawable.agent),
      tint = Color.White,
      modifier =
        Modifier.size(width * 0.55f)
          .graphicsLayer { alpha = iconAnimationProgress }
          .scale(iconAnimationProgress),
      contentDescription = null,
    )
  }
}

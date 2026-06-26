package com.bradflaugher.lfg.ui.common

import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Indeterminate loading spinner used during model load and message generation.
 *
 * Single-color (theme primary). Replaces the four-shape spinner that used Google brand
 * red/green/blue/yellow gradients — kept the same call signature so existing call sites
 * don't need to change.
 */
@Composable
fun RotationalLoader(size: Dp) {
  CircularProgressIndicator(
    color = MaterialTheme.colorScheme.primary,
    trackColor = MaterialTheme.colorScheme.surfaceVariant,
    strokeWidth = (size.value * 0.08f).coerceAtLeast(2f).dp,
    modifier = Modifier.size(size).clearAndSetSemantics {},
  )
}

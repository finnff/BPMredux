package com.bpmredux.ui.components

// BeatPulse is now integrated into BpmDisplay as the confidence ring.
// This file is kept as a no-op for backward compatibility.

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun BeatPulse(
    beatEvent: Long,
    modifier: Modifier = Modifier
) {
    // Beat pulse is now part of BpmDisplay's confidence ring
}

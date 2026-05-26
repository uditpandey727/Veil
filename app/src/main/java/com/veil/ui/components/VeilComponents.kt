package com.veil.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.veil.ui.theme.VeilColors

// ── Privacy Promise Card ───────────────────────────────────────────────
// Shows one thing Veil does NOT collect

@Composable
fun PrivacyBadge(
    icon: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(VeilColors.SurfaceVariant)
            .border(1.dp, VeilColors.Border, RoundedCornerShape(10.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text     = icon,
            fontSize = 16.sp
        )
        Text(
            text       = label,
            fontSize   = 13.sp,
            color      = VeilColors.TextSecondary,
            fontWeight = FontWeight.Normal
        )
    }
}

// ── Pulsing Identity Orb ───────────────────────────────────────────────
// Shown while generating identity — radiating pulse rings

@Composable
fun IdentityOrb(
    isGenerating: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "orb")

    // Ring 1 — fast pulse
    val ring1Scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue  = 1.6f,
        animationSpec = infiniteRepeatable(
            animation  = tween(1200, easing = EaseOut),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring1"
    )
    val ring1Alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue  = 0f,
        animationSpec = infiniteRepeatable(
            animation  = tween(1200, easing = EaseOut),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring1a"
    )

    // Ring 2 — slow pulse, offset
    val ring2Scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue  = 2f,
        animationSpec = infiniteRepeatable(
            animation  = tween(1800, easing = EaseOut, delayMillis = 400),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring2"
    )
    val ring2Alpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue  = 0f,
        animationSpec = infiniteRepeatable(
            animation  = tween(1800, easing = EaseOut, delayMillis = 400),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring2a"
    )

    Box(
        modifier        = modifier.size(96.dp),
        contentAlignment = Alignment.Center
    ) {
        // Pulse rings (only when generating)
        if (isGenerating) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .scale(ring2Scale)
                    .clip(CircleShape)
                    .background(VeilColors.Accent.copy(alpha = ring2Alpha))
            )
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .scale(ring1Scale)
                    .clip(CircleShape)
                    .background(VeilColors.Accent.copy(alpha = ring1Alpha))
            )
        }

        // Core circle
        Box(
            modifier         = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(if (isGenerating) VeilColors.AccentSubtle else VeilColors.SurfaceVariant)
                .border(
                    width = 1.5.dp,
                    color = if (isGenerating) VeilColors.Accent else VeilColors.BorderStrong,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text     = if (isGenerating) "⟳" else "◈",
                fontSize = 28.sp,
                color    = if (isGenerating) VeilColors.Accent else VeilColors.TextSecondary
            )
        }
    }
}

// ── User ID Display ────────────────────────────────────────────────────
// Shows the generated UUID in a monospace pill — looks techy and intentional

@Composable
fun UserIdDisplay(
    userId: String,
    modifier: Modifier = Modifier
) {
    // Show only first 8 chars of UUID — readable but obviously anonymous
    val shortId = userId.take(8).uppercase()

    Column(
        modifier            = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text      = "Your anonymous ID",
            fontSize  = 11.sp,
            color     = VeilColors.TextDisabled,
            letterSpacing = 1.sp,
            fontWeight = FontWeight.Medium
        )
        Row(
            modifier            = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(VeilColors.SurfaceVariant)
                .border(1.dp, VeilColors.BorderStrong, RoundedCornerShape(8.dp))
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text       = "# ",
                fontSize   = 14.sp,
                color      = VeilColors.Accent,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
            Text(
                text       = shortId,
                fontSize   = 14.sp,
                color      = VeilColors.TextPrimary,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Medium,
                letterSpacing = 2.sp
            )
        }
        Text(
            text      = "No name. No number. Just you.",
            fontSize  = 12.sp,
            color     = VeilColors.TextDisabled,
            textAlign = TextAlign.Center
        )
    }
}

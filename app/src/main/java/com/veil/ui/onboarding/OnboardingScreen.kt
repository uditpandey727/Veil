package com.veil.ui.onboarding

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.veil.ui.components.IdentityOrb
import com.veil.ui.components.PrivacyBadge
import com.veil.ui.components.UserIdDisplay
import com.veil.ui.onboarding.OnboardingViewModel
import com.veil.ui.theme.VeilColors

// ── What Veil doesn't collect — shown as badges ───────────────────────
private val privacyPromises = listOf(
    "📵" to "No phone number",
    "✉️" to "No email address",
    "👤" to "No real name",
    "📍" to "No location",
    "🔗" to "No account linking",
)

@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel,
    onIdentityCreated: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Navigate when identity is ready
    LaunchedEffect(uiState) {
        if (uiState is OnboardingViewModel.UiState.Success) {
            // Small delay so user sees the success state
            kotlinx.coroutines.delay(1200)
            onIdentityCreated()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(VeilColors.Background)
    ) {
        // Subtle top glow — accent color bleeds from top
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            VeilColors.Accent.copy(alpha = 0.05f),
                            Color.Transparent
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp)
                .padding(top = 80.dp, bottom = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // ── App name ──────────────────────────────────────────────
            Text(
                text       = "veil",
                fontSize   = 13.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 4.sp,
                color      = VeilColors.Accent
            )

            Spacer(Modifier.height(48.dp))

            // ── Identity orb (animates when generating) ───────────────
            IdentityOrb(
                isGenerating = uiState is OnboardingViewModel.UiState.Loading
            )

            Spacer(Modifier.height(36.dp))

            // ── Headline changes based on state ───────────────────────
            AnimatedContent(
                targetState = uiState,
                transitionSpec = {
                    fadeIn(tween(300)) togetherWith fadeOut(tween(200))
                },
                label = "headline"
            ) { state ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    when (state) {
                        is OnboardingViewModel.UiState.Idle -> {
                            Text(
                                text       = "Private by default.",
                                style      = MaterialTheme.typography.headlineMedium,
                                textAlign  = TextAlign.Center,
                                color      = VeilColors.TextPrimary
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text      = "No signup. No phone number.\nJust an anonymous identity — yours alone.",
                                fontSize  = 15.sp,
                                color     = VeilColors.TextSecondary,
                                textAlign = TextAlign.Center,
                                lineHeight = 24.sp
                            )
                        }

                        is OnboardingViewModel.UiState.Loading -> {
                            Text(
                                text      = "Generating your identity…",
                                fontSize  = 18.sp,
                                fontWeight = FontWeight.Medium,
                                color     = VeilColors.TextPrimary,
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text      = "Creating encryption keys on your device.",
                                fontSize  = 14.sp,
                                color     = VeilColors.TextSecondary,
                                textAlign = TextAlign.Center
                            )
                        }

                        is OnboardingViewModel.UiState.Success -> {
                            Text(
                                text      = "Identity created.",
                                fontSize  = 18.sp,
                                fontWeight = FontWeight.Medium,
                                color     = VeilColors.Success,
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(16.dp))
                            // Show the generated ID
                            UserIdDisplay(userId = state.user.userId)
                        }

                        is OnboardingViewModel.UiState.Error -> {
                            Text(
                                text      = "Something went wrong.",
                                fontSize  = 18.sp,
                                fontWeight = FontWeight.Medium,
                                color     = VeilColors.Error,
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text      = state.message,
                                fontSize  = 14.sp,
                                color     = VeilColors.TextSecondary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(48.dp))

            // ── Privacy promise badges ─────────────────────────────────
            // Only show on idle state — clean once generating starts
            AnimatedVisibility(
                visible = uiState is OnboardingViewModel.UiState.Idle,
                enter   = fadeIn() + expandVertically(),
                exit    = fadeOut() + shrinkVertically()
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text      = "WE NEVER ASK FOR",
                        fontSize  = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.5.sp,
                        color     = VeilColors.TextDisabled
                    )
                    Spacer(Modifier.height(4.dp))

                    // Two badges per row
                    privacyPromises.chunked(2).forEach { row ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            row.forEach { (icon, label) ->
                                PrivacyBadge(
                                    icon     = icon,
                                    label    = label,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            // Pad last row if odd number
                            if (row.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }

            Spacer(Modifier.height(40.dp))

            // ── Optional display name (shown after success) ────────────
            AnimatedVisibility(
                visible = uiState is OnboardingViewModel.UiState.Success,
                enter   = fadeIn(tween(400)) + expandVertically()
            ) {
                DisplayNameInput(
                    onNameSet = { name -> viewModel.setDisplayName(name) }
                )
            }

            Spacer(Modifier.weight(1f))
            Spacer(Modifier.height(32.dp))

            // ── Main CTA button ────────────────────────────────────────
            AnimatedVisibility(
                visible = uiState !is OnboardingViewModel.UiState.Success,
            ) {
                VeilPrimaryButton(
                    text      = when (uiState) {
                        is OnboardingViewModel.UiState.Loading -> "Creating…"
                        is OnboardingViewModel.UiState.Error   -> "Try Again"
                        else                                   -> "Create Anonymous Identity"
                    },
                    enabled   = uiState !is OnboardingViewModel.UiState.Loading,
                    onClick   = { viewModel.createIdentity() }
                )
            }

            Spacer(Modifier.height(16.dp))

            // ── Fine print ─────────────────────────────────────────────
            Text(
                text      = "Your keys are generated locally and never leave your device.",
                fontSize  = 11.sp,
                color     = VeilColors.TextDisabled,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )
        }
    }
}

// ── Display name input (optional, shown after identity is created) ────

@Composable
private fun DisplayNameInput(
    onNameSet: (String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var saved by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(bottom = 24.dp)
    ) {
        Text(
            text      = "Add a local nickname? (optional)",
            fontSize  = 13.sp,
            color     = VeilColors.TextSecondary,
            textAlign = TextAlign.Center
        )
        Text(
            text      = "Only stored on this device — never shared.",
            fontSize  = 11.sp,
            color     = VeilColors.TextDisabled,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(4.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment     = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value         = name,
                onValueChange = { name = it },
                placeholder   = { Text("e.g. Ghost, Anon…", color = VeilColors.TextDisabled) },
                singleLine    = true,
                enabled       = !saved,
                modifier      = Modifier.weight(1f),
                shape         = RoundedCornerShape(12.dp),
                colors        = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor   = VeilColors.Accent,
                    unfocusedBorderColor = VeilColors.BorderStrong,
                    cursorColor          = VeilColors.Accent,
                    focusedTextColor     = VeilColors.TextPrimary,
                    unfocusedTextColor   = VeilColors.TextPrimary,
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = {
                        if (name.isNotBlank()) {
                            onNameSet(name)
                            saved = true
                        }
                        focusManager.clearFocus()
                    }
                )
            )

            if (!saved) {
                TextButton(
                    onClick = {
                        if (name.isNotBlank()) {
                            onNameSet(name)
                            saved = true
                            focusManager.clearFocus()
                        }
                    }
                ) {
                    Text("Save", color = VeilColors.Accent)
                }
            } else {
                Text("✓ Saved", fontSize = 13.sp, color = VeilColors.Success)
            }
        }
    }
}

// ── Primary button ─────────────────────────────────────────────────────

@Composable
fun VeilPrimaryButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    Button(
        onClick  = onClick,
        enabled  = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp),
        shape    = RoundedCornerShape(14.dp),
        colors   = ButtonDefaults.buttonColors(
            containerColor         = VeilColors.Accent,
            contentColor           = Color(0xFF0A0A0B),
            disabledContainerColor = VeilColors.AccentSubtle,
            disabledContentColor   = VeilColors.AccentDim
        )
    ) {
        Text(
            text       = text,
            fontSize   = 15.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.3.sp
        )
    }
}

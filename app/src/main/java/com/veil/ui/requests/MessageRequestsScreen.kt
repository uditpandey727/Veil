package com.veil.ui.requests

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.veil.domain.model.MessageRequest
import com.veil.ui.theme.VeilColors
import java.text.SimpleDateFormat
import java.util.*

private val fmtTs = SimpleDateFormat("HH:mm", Locale.getDefault())
private val fmtDate = SimpleDateFormat("dd/MM", Locale.getDefault())

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageRequestsScreen(
    viewModel      : MessageRequestsViewModel,
    onBackClick    : () -> Unit,
    onOpenChat     : (contactId: String) -> Unit
) {
    val requests by viewModel.requests.collectAsStateWithLifecycle()
    val uiState  by viewModel.uiState.collectAsStateWithLifecycle()

    // Navigate to chat when request is accepted
    LaunchedEffect(uiState.acceptedContactId) {
        uiState.acceptedContactId?.let {
            viewModel.clearNavigation()
            onOpenChat(it)
        }
    }

    Scaffold(
        containerColor = VeilColors.Background,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = VeilColors.Background
                ),
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Outlined.ArrowBackIosNew, null,
                            tint     = VeilColors.TextSecondary,
                            modifier = Modifier.size(18.dp))
                    }
                },
                title = {
                    Column {
                        Text("Message requests",
                            fontSize   = 17.sp,
                            fontWeight = FontWeight.SemiBold,
                            color      = VeilColors.TextPrimary)
                        if (requests.isNotEmpty()) {
                            Text("${requests.size} pending",
                                fontSize = 12.sp,
                                color    = VeilColors.TextSecondary)
                        }
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (requests.isEmpty()) {
                EmptyState()
            } else {
                LazyColumn(
                    modifier       = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    // Privacy notice at top
                    item {
                        PrivacyNotice()
                    }
                    items(
                        items = requests,
                        key   = { it.senderId }
                    ) { request ->
                        RequestCard(
                            request    = request,
                            isLoading  = uiState.isProcessing,
                            onAccept   = { viewModel.accept(request.senderId) },
                            onDecline  = { viewModel.decline(request.senderId) }
                        )
                    }
                }
            }

            // Declined snackbar
            uiState.declinedId?.let {
                Snackbar(
                    modifier       = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    action         = {
                        TextButton(onClick = { viewModel.clearDeclined() }) {
                            Text("OK", color = VeilColors.Accent)
                        }
                    },
                    containerColor = VeilColors.SurfaceVariant,
                    contentColor   = VeilColors.TextPrimary
                ) {
                    Text("Request declined. Messages deleted.")
                }
            }
        }
    }
}

// ── Privacy notice ─────────────────────────────────────────────────────

@Composable
private fun PrivacyNotice() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(VeilColors.SurfaceVariant)
            .border(0.5.dp, VeilColors.Border, RoundedCornerShape(12.dp))
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment     = Alignment.Top
    ) {
        Icon(Icons.Outlined.ShieldMoon, null,
            tint     = VeilColors.Accent,
            modifier = Modifier.size(16.dp).padding(top = 1.dp))
        Text(
            text      = "These people know your anonymous ID but you haven't saved them as a contact. " +
                        "Declining deletes their messages. They won't be notified either way.",
            fontSize  = 12.sp,
            color     = VeilColors.TextSecondary,
            lineHeight = 18.sp
        )
    }
}

// ── Request card ───────────────────────────────────────────────────────

@Composable
private fun RequestCard(
    request  : MessageRequest,
    isLoading: Boolean,
    onAccept : () -> Unit,
    onDecline: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(VeilColors.Surface)
            .border(0.5.dp, VeilColors.Border, RoundedCornerShape(16.dp))
    ) {
        // Header row
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Unknown avatar
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(VeilColors.SurfaceVariant)
                    .border(1.dp, VeilColors.BorderStrong, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.Person, null,
                    tint     = VeilColors.TextDisabled,
                    modifier = Modifier.size(24.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text       = request.senderShortId,
                    fontSize   = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color      = VeilColors.TextPrimary,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 0.5.sp
                )
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text(
                        text    = "${request.messageCount} message${if (request.messageCount != 1) "s" else ""}",
                        fontSize = 12.sp,
                        color   = VeilColors.TextSecondary
                    )
                    Text("·", fontSize = 12.sp, color = VeilColors.TextDisabled)
                    Text(
                        text    = formatTimestamp(request.timestamp),
                        fontSize = 12.sp,
                        color   = VeilColors.TextSecondary
                    )
                }
            }
        }

        // Message preview
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(VeilColors.SurfaceVariant)
                .padding(12.dp)
        ) {
            if (request.previewText.startsWith("🔒")) {
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Outlined.Lock, null,
                        tint     = VeilColors.TextSecondary,
                        modifier = Modifier.size(13.dp))
                    Text(
                        text      = "Encrypted message",
                        fontSize  = 13.sp,
                        color     = VeilColors.TextSecondary,
                        fontStyle = FontStyle.Italic
                    )
                }
            } else {
                Text(
                    text     = request.previewText,
                    fontSize = 13.sp,
                    color    = VeilColors.TextPrimary,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 20.sp
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // Action buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Decline
            OutlinedButton(
                onClick  = onDecline,
                enabled  = !isLoading,
                modifier = Modifier.weight(1f).height(44.dp),
                shape    = RoundedCornerShape(12.dp),
                colors   = ButtonDefaults.outlinedButtonColors(
                    contentColor = VeilColors.Error
                ),
                border   = androidx.compose.foundation.BorderStroke(
                    1.dp, VeilColors.Error.copy(alpha = 0.5f)
                )
            ) {
                Icon(Icons.Outlined.Close, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Decline", fontWeight = FontWeight.Medium)
            }

            // Accept
            Button(
                onClick  = onAccept,
                enabled  = !isLoading,
                modifier = Modifier.weight(1f).height(44.dp),
                shape    = RoundedCornerShape(12.dp),
                colors   = ButtonDefaults.buttonColors(
                    containerColor = VeilColors.Accent,
                    contentColor   = Color(0xFF0A0A0B)
                )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier    = Modifier.size(16.dp),
                        color       = Color(0xFF0A0A0B),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(Icons.Outlined.Check, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Accept", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

// ── Empty state ────────────────────────────────────────────────────────

@Composable
private fun EmptyState() {
    Column(
        modifier            = Modifier.fillMaxSize().padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Outlined.MarkEmailRead, null,
            tint     = VeilColors.BorderStrong,
            modifier = Modifier.size(52.dp))
        Spacer(Modifier.height(16.dp))
        Text("No pending requests",
            fontSize   = 18.sp,
            fontWeight = FontWeight.Medium,
            color      = VeilColors.TextPrimary,
            textAlign  = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text("When someone you haven't saved messages you, their request will appear here.",
            fontSize  = 14.sp,
            color     = VeilColors.TextSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp)
    }
}

private fun formatTimestamp(ts: Long): String {
    val diff = System.currentTimeMillis() - ts
    return when {
        diff < 3_600_000  -> "${diff / 60_000}m ago"
        diff < 86_400_000 -> fmtTs.format(Date(ts))
        else              -> fmtDate.format(Date(ts))
    }
}

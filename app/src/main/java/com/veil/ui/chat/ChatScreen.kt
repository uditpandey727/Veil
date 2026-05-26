package com.veil.ui.chat

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowBackIosNew
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.DoneAll
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.veil.domain.model.Message
import com.veil.domain.model.MessageStatus
import com.veil.ui.theme.VeilColors
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel   : ChatViewModel,
    onBackClick : () -> Unit
) {
    val uiState   by viewModel.uiState.collectAsStateWithLifecycle()
    val listState  = rememberLazyListState()
    var inputText  by remember { mutableStateOf("") }
    val myUserId   = remember { viewModel.myUserId }

    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    Scaffold(
        containerColor = VeilColors.Background,
        topBar = {
            ChatTopBar(
                contactName      = uiState.contactDisplayName,
                isVerified       = uiState.isContactVerified,
                disappearSeconds = uiState.disappearSeconds,
                onBackClick      = onBackClick,
                onTimerChange    = { viewModel.setDisappearTimer(it) }
            )
        },
        bottomBar = {
            MessageInputBar(
                text         = inputText,
                onTextChange = { inputText = it },
                isSending    = uiState.isSending,
                onSend       = {
                    viewModel.sendMessage(inputText)
                    inputText = ""
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {

            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color    = VeilColors.Accent,
                    strokeWidth = 2.dp
                )
            } else {
                LazyColumn(
                    state          = listState,
                    modifier       = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start  = 10.dp,
                        end    = 10.dp,
                        top    = 10.dp,
                        bottom = 10.dp
                    )
                ) {
                    // Group by day
                    val grouped = uiState.messages.groupByDay()
                    grouped.forEach { (dayLabel, messages) ->

                        item(key = "header_$dayLabel") {
                            DateChip(dayLabel)
                        }

                        // Show disappearing messages notice if active
                        if (uiState.disappearSeconds != null) {
                            item(key = "disappear_notice_$dayLabel") {
                                DisappearNotice(uiState.disappearSeconds!!)
                            }
                        }

                        items(
                            items = messages,
                            key   = { it.messageId }
                        ) { message ->
                            val index      = messages.indexOf(message)
                            val isMine     = message.senderId == myUserId
                            val prev       = messages.getOrNull(index - 1)
                            val next       = messages.getOrNull(index + 1)

                            // Show time label if first message or 5+ min gap
                            val showTime = index == 0 ||
                                (message.timestamp - messages[index - 1].timestamp > 5 * 60_000)

                            // Group logic — show avatar only on last bubble in a group
                            val isLastInGroup = next == null ||
                                next.senderId != message.senderId ||
                                (next.timestamp - message.timestamp > 5 * 60_000)

                            val isFirstInGroup = prev == null ||
                                prev.senderId != message.senderId ||
                                (message.timestamp - prev.timestamp > 5 * 60_000)

                            if (showTime) {
                                TimeLabel(message.timestamp)
                            }

                            MessageRow(
                                message        = message,
                                isMine         = isMine,
                                isFirstInGroup = isFirstInGroup,
                                isLastInGroup  = isLastInGroup,
                                onLongPress    = { viewModel.showMessageOptions(message.messageId) }
                            )
                        }
                    }
                }
            }

            // Error snackbar
            uiState.errorMessage?.let { error ->
                Snackbar(
                    modifier       = Modifier.align(Alignment.BottomCenter).padding(12.dp),
                    action         = {
                        TextButton(onClick = { viewModel.clearError() }) {
                            Text("OK", color = VeilColors.Accent)
                        }
                    },
                    containerColor = VeilColors.SurfaceVariant,
                    contentColor   = VeilColors.TextPrimary
                ) { Text(error, fontSize = 13.sp) }
            }
        }
    }

    uiState.showMessageOptions?.let { messageId ->
        MessageOptionsDialog(
            onDismiss = { viewModel.dismissMessageOptions() },
            onDelete  = { viewModel.deleteMessage(messageId) }
        )
    }
}

// ── Top bar ────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatTopBar(
    contactName     : String,
    isVerified      : Boolean,
    disappearSeconds: Int?,
    onBackClick     : () -> Unit,
    onTimerChange   : (Int?) -> Unit
) {
    var showTimerMenu by remember { mutableStateOf(false) }

    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = VeilColors.Surface
        ),
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(Icons.Outlined.ArrowBackIosNew, null,
                    tint = VeilColors.TextSecondary, modifier = Modifier.size(18.dp))
            }
        },
        title = {
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Avatar
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(VeilColors.AccentSubtle)
                        .border(1.dp, VeilColors.Accent.copy(alpha = 0.3f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text       = if (contactName.startsWith("#")) "#"
                                     else contactName.firstOrNull()?.uppercase() ?: "?",
                        fontSize   = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color      = VeilColors.Accent
                    )
                }
                Column {
                    Row(
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text       = contactName,
                            fontSize   = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color      = VeilColors.TextPrimary
                        )
                        if (isVerified) {
                            Icon(Icons.Outlined.VerifiedUser, null,
                                tint     = VeilColors.Accent,
                                modifier = Modifier.size(13.dp))
                        }
                    }
                    Row(
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(Icons.Outlined.Lock, null,
                            tint     = VeilColors.TextSecondary,
                            modifier = Modifier.size(10.dp))
                        Text("End-to-end encrypted", fontSize = 10.sp, color = VeilColors.TextSecondary)
                    }
                }
            }
        },
        actions = {
            Box {
                IconButton(onClick = { showTimerMenu = true }) {
                    if (disappearSeconds != null) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(VeilColors.AccentSubtle)
                                .border(1.dp, VeilColors.Accent.copy(alpha = 0.4f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text      = formatTimerShort(disappearSeconds),
                                fontSize  = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color     = VeilColors.Accent
                            )
                        }
                    } else {
                        Icon(Icons.Outlined.Timer, null, tint = VeilColors.TextSecondary)
                    }
                }
                DropdownMenu(
                    expanded         = showTimerMenu,
                    onDismissRequest = { showTimerMenu = false },
                    containerColor   = VeilColors.Surface
                ) {
                    listOf(null, 30, 60, 300, 3600, 86400).forEach { s ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    if (s == null) "Off" else formatTimerLabel(s),
                                    color = if (disappearSeconds == s) VeilColors.Accent
                                            else VeilColors.TextPrimary
                                )
                            },
                            leadingIcon = {
                                if (disappearSeconds == s) {
                                    Icon(Icons.Outlined.Check, null,
                                        tint     = VeilColors.Accent,
                                        modifier = Modifier.size(16.dp))
                                }
                            },
                            onClick = { onTimerChange(s); showTimerMenu = false }
                        )
                    }
                }
            }
        }
    )
}

// ── Message row — grouped bubbles with avatar ─────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MessageRow(
    message        : Message,
    isMine         : Boolean,
    isFirstInGroup : Boolean,
    isLastInGroup  : Boolean,
    onLongPress    : () -> Unit
) {
    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .padding(bottom = if (isLastInGroup) 4.dp else 1.dp),
        horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start,
        verticalAlignment     = Alignment.Bottom
    ) {
        // Their avatar — only shown on last bubble in group
        if (!isMine) {
            Box(modifier = Modifier.size(24.dp).padding(end = 4.dp)) {
                if (isLastInGroup) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(VeilColors.AccentSubtle)
                            .border(0.5.dp, VeilColors.Accent.copy(alpha = 0.3f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("G", fontSize = 8.sp, color = VeilColors.Accent, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Bubble shape — flat corner on tail side, only on last in group
        val shape = when {
            isMine && isLastInGroup   -> RoundedCornerShape(16.dp, 16.dp, 4.dp, 16.dp)
            isMine                    -> RoundedCornerShape(16.dp, 16.dp, 16.dp, 16.dp)
            !isMine && isLastInGroup  -> RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp)
            else                      -> RoundedCornerShape(16.dp, 16.dp, 16.dp, 16.dp)
        }

        Box(
            modifier = Modifier
                .widthIn(min = 60.dp, max = 240.dp)
                .clip(shape)
                .background(if (isMine) VeilColors.AccentSubtle else VeilColors.SurfaceVariant)
                .border(
                    0.5.dp,
                    if (isMine) VeilColors.Accent.copy(alpha = 0.2f) else VeilColors.BorderStrong,
                    shape
                )
                .combinedClickable(onClick = {}, onLongClick = onLongPress)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Column {
                // Disappearing tag
                if (message.expiresAt != null) {
                    Row(
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        modifier              = Modifier.padding(bottom = 3.dp)
                    ) {
                        Icon(Icons.Outlined.Timer, null,
                            tint     = VeilColors.Accent.copy(alpha = 0.7f),
                            modifier = Modifier.size(10.dp))
                        val remaining = (message.expiresAt - System.currentTimeMillis()) / 1000
                        Text(
                            text    = if (remaining > 0) "Disappears in ${formatTimerShort(remaining.toInt())}"
                                      else "Disappearing",
                            fontSize = 10.sp,
                            color   = VeilColors.Accent.copy(alpha = 0.7f)
                        )
                    }
                }

                // Message content
                if (message.plaintext != null) {
                    Text(
                        text      = message.plaintext,
                        fontSize  = 14.sp,
                        color     = VeilColors.TextPrimary,
                        lineHeight = 21.sp
                    )
                } else {
                    // Decryption failed or pending
                    Row(
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(Icons.Outlined.Lock, null,
                            tint     = VeilColors.TextSecondary,
                            modifier = Modifier.size(12.dp))
                        Text(
                            text      = "Encrypted message",
                            fontSize  = 13.sp,
                            color     = VeilColors.TextSecondary,
                            fontStyle = FontStyle.Italic
                        )
                    }
                }

                // Time + status
                Spacer(Modifier.height(3.dp))
                Row(
                    modifier              = Modifier.align(Alignment.End),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text    = fmtTime.format(Date(message.timestamp)),
                        fontSize = 10.sp,
                        color   = if (isMine) VeilColors.Accent.copy(alpha = 0.5f)
                                  else VeilColors.TextDisabled
                    )
                    if (isMine) StatusIcon(message.status)
                }
            }
        }

        // Spacer for mine side (no avatar)
        if (isMine) {
            Spacer(Modifier.width(4.dp))
        }
    }
}

// ── Status icon ────────────────────────────────────────────────────────

@Composable
private fun StatusIcon(status: MessageStatus) {
    when (status) {
        MessageStatus.SENDING   -> CircularProgressIndicator(
            modifier    = Modifier.size(10.dp),
            color       = VeilColors.Accent.copy(alpha = 0.5f),
            strokeWidth = 1.dp
        )
        MessageStatus.SENT      -> Icon(Icons.Outlined.Check, null,
            tint = VeilColors.TextDisabled, modifier = Modifier.size(12.dp))
        MessageStatus.DELIVERED -> Icon(Icons.Outlined.DoneAll, null,
            tint = VeilColors.TextSecondary, modifier = Modifier.size(12.dp))
        MessageStatus.READ      -> Icon(Icons.Outlined.DoneAll, null,
            tint = VeilColors.Accent, modifier = Modifier.size(12.dp))
        MessageStatus.FAILED    -> Icon(Icons.Outlined.ErrorOutline, null,
            tint = VeilColors.Error, modifier = Modifier.size(12.dp))
    }
}

// ── Date chip ──────────────────────────────────────────────────────────

@Composable
private fun DateChip(label: String) {
    Box(
        modifier            = Modifier.fillMaxWidth().padding(vertical = 10.dp),
        contentAlignment    = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(VeilColors.SurfaceVariant)
                .padding(horizontal = 12.dp, vertical = 4.dp)
        ) {
            Text(label, fontSize = 11.sp, color = VeilColors.TextSecondary)
        }
    }
}

// ── Time label ─────────────────────────────────────────────────────────

@Composable
private fun TimeLabel(timestamp: Long) {
    Text(
        text      = fmtTimeFull.format(Date(timestamp)),
        fontSize  = 10.sp,
        color     = VeilColors.TextDisabled,
        textAlign = TextAlign.Center,
        modifier  = Modifier.fillMaxWidth().padding(vertical = 6.dp)
    )
}

// ── Disappearing messages notice ───────────────────────────────────────

@Composable
private fun DisappearNotice(seconds: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(VeilColors.AccentSubtle)
                .border(0.5.dp, VeilColors.Accent.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp, vertical = 5.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(Icons.Outlined.Timer, null,
                tint = VeilColors.Accent, modifier = Modifier.size(12.dp))
            Text(
                text     = "Disappearing messages: ${formatTimerLabel(seconds)}",
                fontSize = 11.sp,
                color    = VeilColors.Accent
            )
        }
    }
}

// ── Input bar ──────────────────────────────────────────────────────────

@Composable
private fun MessageInputBar(
    text        : String,
    onTextChange: (String) -> Unit,
    isSending   : Boolean,
    onSend      : () -> Unit
) {
    val canSend = text.isNotBlank() && !isSending

    Surface(color = VeilColors.Surface, tonalElevation = 0.dp) {
        Row(
            modifier          = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Attach button
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(VeilColors.SurfaceVariant)
                    .border(0.5.dp, VeilColors.BorderStrong, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.Add, null,
                    tint = VeilColors.TextSecondary, modifier = Modifier.size(18.dp))
            }

            // Text field
            OutlinedTextField(
                value         = text,
                onValueChange = onTextChange,
                placeholder   = {
                    Text("Message", color = VeilColors.TextDisabled, fontSize = 14.sp)
                },
                modifier      = Modifier.weight(1f),
                shape         = RoundedCornerShape(20.dp),
                maxLines      = 5,
                colors        = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor      = VeilColors.BorderStrong,
                    unfocusedBorderColor    = VeilColors.Border,
                    cursorColor             = VeilColors.Accent,
                    focusedTextColor        = VeilColors.TextPrimary,
                    unfocusedTextColor      = VeilColors.TextPrimary,
                    focusedContainerColor   = VeilColors.SurfaceVariant,
                    unfocusedContainerColor = VeilColors.SurfaceVariant,
                ),
                textStyle       = LocalTextStyle.current.copy(fontSize = 14.sp),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction      = ImeAction.Default
                )
            )

            // Send button
            IconButton(
                onClick  = { if (canSend) onSend() },
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(if (canSend) VeilColors.Accent else VeilColors.SurfaceVariant)
            ) {
                if (isSending) {
                    CircularProgressIndicator(
                        modifier    = Modifier.size(16.dp),
                        color       = Color(0xFF0A0A0B),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        Icons.Outlined.Send,
                        contentDescription = "Send",
                        tint     = if (canSend) Color(0xFF0A0A0B) else VeilColors.TextDisabled,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

// ── Long-press dialog ──────────────────────────────────────────────────

@Composable
private fun MessageOptionsDialog(onDismiss: () -> Unit, onDelete: () -> Unit) {
    AlertDialog(
        onDismissRequest  = onDismiss,
        containerColor    = VeilColors.Surface,
        titleContentColor = VeilColors.TextPrimary,
        title = { Text("Message options", fontWeight = FontWeight.SemiBold) },
        text  = { Text("What would you like to do?", fontSize = 14.sp, color = VeilColors.TextSecondary) },
        confirmButton = {
            TextButton(onClick = onDelete) {
                Text("Delete for me", color = VeilColors.Error, fontWeight = FontWeight.Medium)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = VeilColors.TextSecondary)
            }
        }
    )
}

// ── Helpers ────────────────────────────────────────────────────────────

// Top-level formatters — created once, outside composables, avoids Locale lint warning
private val fmtTime    = SimpleDateFormat("HH:mm",          Locale.getDefault())
private val fmtTimeFull= SimpleDateFormat("h:mm a",         Locale.getDefault())
private val fmtDate    = SimpleDateFormat("MMMM d, yyyy",   Locale.getDefault())

private fun List<Message>.groupByDay(): Map<String, List<Message>> {
    val today     = Calendar.getInstance()
    val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
    return groupBy { msg ->
        val cal = Calendar.getInstance().apply { timeInMillis = msg.timestamp }
        when {
            cal.isSameDay(today)     -> "Today"
            cal.isSameDay(yesterday) -> "Yesterday"
            else                     -> fmtDate.format(Date(msg.timestamp))
        }
    }
}

private fun Calendar.isSameDay(other: Calendar) =
    get(Calendar.YEAR) == other.get(Calendar.YEAR) &&
    get(Calendar.DAY_OF_YEAR) == other.get(Calendar.DAY_OF_YEAR)

private fun formatTimerShort(s: Int) = when {
    s < 60   -> "${s}s"
    s < 3600 -> "${s / 60}m"
    else     -> "${s / 3600}h"
}

private fun formatTimerLabel(s: Int) = when (s) {
    30    -> "30 seconds"
    60    -> "1 minute"
    300   -> "5 minutes"
    3600  -> "1 hour"
    86400 -> "24 hours"
    else  -> "${s}s"
}

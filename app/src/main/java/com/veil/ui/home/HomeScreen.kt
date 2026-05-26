package com.veil.ui.home

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.veil.domain.model.Conversation
import com.veil.domain.model.MessageStatus
import com.veil.ui.theme.VeilColors
import java.text.SimpleDateFormat
import java.util.*

private val fmtTs   = SimpleDateFormat("HH:mm",  Locale.getDefault())
private val fmtDay  = SimpleDateFormat("EEE",    Locale.getDefault())
private val fmtDate = SimpleDateFormat("dd/MM",  Locale.getDefault())

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel           : HomeViewModel,
    onConversationClick : (contactId: String) -> Unit,
    onSettingsClick     : () -> Unit,
    onAddContactClick   : () -> Unit,
    onRequestsClick     : () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.addContactSuccess) {
        uiState.addContactSuccess?.let { contactId ->
            viewModel.clearAddContactSuccess()
            onConversationClick(contactId)
        }
    }

    Scaffold(
        containerColor = VeilColors.Background,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = VeilColors.Background
                ),
                title = {
                    Column {
                        Text("veil", fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                            letterSpacing = 3.sp, color = VeilColors.Accent)
                        Text(
                            text       = uiState.myDisplayName ?: uiState.myShortId,
                            fontSize   = 13.sp,
                            color      = VeilColors.TextSecondary,
                            fontFamily = if (uiState.myDisplayName == null) FontFamily.Monospace
                                         else FontFamily.Default
                        )
                    }
                },
                actions = {
                    // Message requests button with badge
                    Box {
                        IconButton(onClick = onRequestsClick) {
                            Icon(Icons.Outlined.MarkEmailUnread,
                                contentDescription = "Message requests",
                                tint = if (uiState.requestCount > 0) VeilColors.Accent
                                       else VeilColors.TextSecondary)
                        }
                        // Badge
                        if (uiState.requestCount > 0) {
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(VeilColors.Error)
                                    .align(Alignment.TopEnd)
                                    .offset(x = (-6).dp, y = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text      = if (uiState.requestCount > 9) "9+"
                                               else uiState.requestCount.toString(),
                                    fontSize  = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color     = Color.White
                                )
                            }
                        }
                    }
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Outlined.Settings,
                            contentDescription = "Settings",
                            tint = VeilColors.TextSecondary)
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick        = { viewModel.showAddContact() },
                containerColor = VeilColors.Accent,
                contentColor   = Color(0xFF0A0A0B),
                shape          = RoundedCornerShape(16.dp),
                modifier       = Modifier.size(56.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "New conversation")
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            when {
                uiState.isLoading -> ConversationSkeleton()
                uiState.conversations.isEmpty() -> EmptyState(
                    hasRequests    = uiState.requestCount > 0,
                    onAddContact   = { viewModel.showAddContact() },
                    onViewRequests = onRequestsClick
                )
                else -> LazyColumn(
                    modifier       = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    // Requests banner — shown inline if there are pending requests
                    if (uiState.requestCount > 0) {
                        item(key = "requests_banner") {
                            RequestsBanner(
                                count   = uiState.requestCount,
                                onClick = onRequestsClick
                            )
                        }
                    }

                    items(
                        items = uiState.conversations,
                        key   = { it.contactId }
                    ) { conversation ->
                        ConversationRow(
                            conversation = conversation,
                            onClick      = { onConversationClick(conversation.contactId) }
                        )
                        HorizontalDivider(
                            color     = VeilColors.Border,
                            thickness = 0.5.dp,
                            modifier  = Modifier.padding(start = 76.dp)
                        )
                    }
                }
            }
        }
    }

    if (uiState.showAddContact) {
        AddContactSheet(
            isLoading    = uiState.isAddingContact,
            errorMessage = uiState.addContactError,
            onDismiss    = { viewModel.dismissAddContact() },
            onAddById    = { id -> viewModel.addContactById(id) },
            onScanQr     = {
                viewModel.dismissAddContact()
                onAddContactClick()
            }
        )
    }
}

// ── Requests banner ────────────────────────────────────────────────────

@Composable
private fun RequestsBanner(count: Int, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(VeilColors.SurfaceVariant)
            .border(0.5.dp, VeilColors.BorderStrong, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(14.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Icon with badge
        Box {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(VeilColors.Error.copy(alpha = 0.12f))
                    .border(1.dp, VeilColors.Error.copy(alpha = 0.3f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.MarkEmailUnread, null,
                    tint     = VeilColors.Error,
                    modifier = Modifier.size(20.dp))
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text       = "$count message request${if (count != 1) "s" else ""}",
                fontSize   = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color      = VeilColors.TextPrimary
            )
            Text(
                text    = "From people you haven't saved",
                fontSize = 12.sp,
                color   = VeilColors.TextSecondary
            )
        }
        Icon(Icons.Outlined.ChevronRight, null,
            tint     = VeilColors.TextDisabled,
            modifier = Modifier.size(20.dp))
    }
}

// ── Conversation Row ───────────────────────────────────────────────────

@Composable
private fun ConversationRow(conversation: Conversation, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        ContactAvatar(displayName = conversation.displayName, isVerified = conversation.isVerified)

        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text       = conversation.displayName,
                    fontSize   = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color      = VeilColors.TextPrimary,
                    fontFamily = if (conversation.displayName.startsWith("#"))
                                     FontFamily.Monospace else FontFamily.Default,
                    maxLines   = 1,
                    overflow   = TextOverflow.Ellipsis,
                    modifier   = Modifier.weight(1f, fill = false)
                )
                if (conversation.isVerified) {
                    Icon(Icons.Outlined.VerifiedUser, null,
                        tint = VeilColors.Accent, modifier = Modifier.size(13.dp))
                }
            }
            Spacer(Modifier.height(3.dp))
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (conversation.lastMessageWasMine) StatusIcon(conversation.lastMessageStatus)
                Text(
                    text     = conversation.lastMessagePreview,
                    fontSize = 13.sp,
                    color    = VeilColors.TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (conversation.lastMessageTimestamp > 0L) {
                Text(
                    text  = formatTimestamp(conversation.lastMessageTimestamp),
                    fontSize = 11.sp,
                    color = if (conversation.unreadCount > 0) VeilColors.Accent
                            else VeilColors.TextSecondary
                )
            }
            if (conversation.unreadCount > 0) {
                Box(
                    modifier         = Modifier.size(20.dp).clip(CircleShape)
                        .background(VeilColors.Accent),
                    contentAlignment = Alignment.Center
                ) {
                    Text(conversation.unreadCount.toString(),
                        fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0A0A0B))
                }
            }
        }
    }
}

@Composable
private fun ContactAvatar(displayName: String, isVerified: Boolean) {
    val colors = listOf(VeilColors.Accent, Color(0xFF9B7FE8), Color(0xFFE87F7F),
        Color(0xFF7FB5E8), Color(0xFFE8C37F))
    val avatarColor = colors[displayName.length % colors.size]
    val initial = when {
        displayName.startsWith("#") -> "#"
        displayName.isNotEmpty()    -> displayName.first().uppercase()
        else                        -> "?"
    }
    Box(
        modifier         = Modifier.size(48.dp).clip(CircleShape)
            .background(avatarColor.copy(alpha = 0.15f))
            .border(1.dp, avatarColor.copy(alpha = 0.3f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(initial, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = avatarColor,
            fontFamily = if (initial == "#") FontFamily.Monospace else FontFamily.Default)
    }
}

@Composable
private fun StatusIcon(status: MessageStatus) {
    val (icon, tint) = when (status) {
        MessageStatus.SENDING   -> Icons.Outlined.Schedule to VeilColors.TextDisabled
        MessageStatus.SENT      -> Icons.Outlined.Check to VeilColors.TextSecondary
        MessageStatus.DELIVERED -> Icons.Outlined.DoneAll to VeilColors.TextSecondary
        MessageStatus.READ      -> Icons.Outlined.DoneAll to VeilColors.Accent
        MessageStatus.FAILED    -> Icons.Outlined.ErrorOutline to VeilColors.Error
    }
    Icon(icon, null, tint = tint, modifier = Modifier.size(14.dp))
}

// ── Empty state ────────────────────────────────────────────────────────

@Composable
private fun EmptyState(
    hasRequests   : Boolean,
    onAddContact  : () -> Unit,
    onViewRequests: () -> Unit
) {
    Column(
        modifier            = Modifier.fillMaxSize().padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("◈", fontSize = 48.sp, color = VeilColors.BorderStrong)
        Spacer(Modifier.height(20.dp))
        Text("No conversations yet.", fontSize = 18.sp, fontWeight = FontWeight.Medium,
            color = VeilColors.TextPrimary, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text("Add a contact using their anonymous ID or scan their QR code.",
            fontSize = 14.sp, color = VeilColors.TextSecondary,
            textAlign = TextAlign.Center, lineHeight = 22.sp)
        Spacer(Modifier.height(28.dp))
        Button(
            onClick = onAddContact,
            shape   = RoundedCornerShape(12.dp),
            colors  = ButtonDefaults.buttonColors(
                containerColor = VeilColors.AccentSubtle,
                contentColor   = VeilColors.Accent
            )
        ) {
            Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Add a contact", fontWeight = FontWeight.Medium)
        }
        // Show requests button if there are pending ones
        if (hasRequests) {
            Spacer(Modifier.height(12.dp))
            TextButton(onClick = onViewRequests) {
                Icon(Icons.Outlined.MarkEmailUnread, null,
                    modifier = Modifier.size(16.dp), tint = VeilColors.Error)
                Spacer(Modifier.width(6.dp))
                Text("View message requests", color = VeilColors.Error)
            }
        }
    }
}

// ── Loading skeleton ───────────────────────────────────────────────────

@Composable
private fun ConversationSkeleton() {
    val shimmerAlpha by rememberInfiniteTransition(label = "shimmer")
        .animateFloat(
            initialValue  = 0.3f, targetValue = 0.6f,
            animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
            label = "alpha"
        )
    Column {
        repeat(5) {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(48.dp).clip(CircleShape)
                    .background(VeilColors.SurfaceVariant.copy(alpha = shimmerAlpha)))
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(modifier = Modifier.fillMaxWidth(0.4f).height(14.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(VeilColors.SurfaceVariant.copy(alpha = shimmerAlpha)))
                    Box(modifier = Modifier.fillMaxWidth(0.65f).height(12.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(VeilColors.SurfaceVariant.copy(alpha = shimmerAlpha)))
                }
            }
            HorizontalDivider(color = VeilColors.Border, thickness = 0.5.dp,
                modifier = Modifier.padding(start = 76.dp))
        }
    }
}

// ── Add Contact Sheet ──────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddContactSheet(
    isLoading   : Boolean,
    errorMessage: String?,
    onDismiss   : () -> Unit,
    onAddById   : (String) -> Unit,
    onScanQr    : () -> Unit
) {
    var inputText      = remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val focusManager   = LocalFocusManager.current
    val sheetState     = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(100)
        focusRequester.requestFocus()
    }

    ModalBottomSheet(
        onDismissRequest = { if (!isLoading) onDismiss() },
        sheetState       = sheetState,
        containerColor   = VeilColors.Surface,
        dragHandle = {
            Box(modifier = Modifier.padding(vertical = 12.dp).width(36.dp).height(4.dp)
                .clip(CircleShape).background(VeilColors.BorderStrong))
        }
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("Add a contact", fontSize = 18.sp, fontWeight = FontWeight.SemiBold,
                color = VeilColors.TextPrimary)
            Text("Enter their full anonymous ID — they can copy it from their Settings screen.",
                fontSize = 13.sp, color = VeilColors.TextSecondary, lineHeight = 20.sp)

            OutlinedTextField(
                value         = inputText.value,
                onValueChange = { inputText.value = it },
                placeholder   = {
                    Text("Paste their full ID here…", color = VeilColors.TextDisabled,
                        fontFamily = FontFamily.Monospace, fontSize = 13.sp)
                },
                singleLine    = true,
                enabled       = !isLoading,
                modifier      = Modifier.fillMaxWidth().focusRequester(focusRequester),
                shape         = RoundedCornerShape(12.dp),
                colors        = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor   = VeilColors.Accent,
                    unfocusedBorderColor = VeilColors.BorderStrong,
                    errorBorderColor     = VeilColors.Error,
                    cursorColor          = VeilColors.Accent,
                    focusedTextColor     = VeilColors.TextPrimary,
                    unfocusedTextColor   = VeilColors.TextPrimary,
                ),
                isError         = errorMessage != null,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    focusManager.clearFocus()
                    if (inputText.value.isNotBlank()) onAddById(inputText.value)
                })
            )

            AnimatedVisibility(visible = errorMessage != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Top) {
                    Icon(Icons.Outlined.ErrorOutline, null, tint = VeilColors.Error,
                        modifier = Modifier.size(16.dp).padding(top = 1.dp))
                    Text(errorMessage ?: "", fontSize = 12.sp, color = VeilColors.Error, lineHeight = 18.sp)
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                HorizontalDivider(modifier = Modifier.weight(1f), color = VeilColors.Border)
                Text("or", fontSize = 12.sp, color = VeilColors.TextDisabled)
                HorizontalDivider(modifier = Modifier.weight(1f), color = VeilColors.Border)
            }

            OutlinedButton(
                onClick = onScanQr, enabled = !isLoading,
                modifier = Modifier.fillMaxWidth(),
                shape    = RoundedCornerShape(12.dp),
                colors   = ButtonDefaults.outlinedButtonColors(contentColor = VeilColors.TextSecondary),
                border   = androidx.compose.foundation.BorderStroke(1.dp, VeilColors.BorderStrong)
            ) {
                Icon(Icons.Outlined.QrCodeScanner, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Scan QR code instead", fontWeight = FontWeight.Medium)
            }

            Button(
                onClick  = { focusManager.clearFocus(); onAddById(inputText.value) },
                enabled  = inputText.value.isNotBlank() && !isLoading,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape    = RoundedCornerShape(12.dp),
                colors   = ButtonDefaults.buttonColors(
                    containerColor         = VeilColors.Accent,
                    contentColor           = Color(0xFF0A0A0B),
                    disabledContainerColor = VeilColors.AccentSubtle,
                    disabledContentColor   = VeilColors.AccentDim
                )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp),
                        color = Color(0xFF0A0A0B), strokeWidth = 2.dp)
                    Spacer(Modifier.width(10.dp))
                    Text("Looking up…", fontWeight = FontWeight.SemiBold)
                } else {
                    Text("Add contact", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    return when {
        diff < 60_000      -> "now"
        diff < 3_600_000   -> "${diff / 60_000}m"
        diff < 86_400_000  -> fmtTs.format(Date(timestamp))
        diff < 604_800_000 -> fmtDay.format(Date(timestamp))
        else               -> fmtDate.format(Date(timestamp))
    }
}

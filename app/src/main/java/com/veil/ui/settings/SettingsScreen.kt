package com.veil.ui.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.veil.ui.theme.VeilColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel         : SettingsViewModel,
    onBackClick       : () -> Unit,
    onAppearanceClick : () -> Unit,
    onQrClick         : () -> Unit,
    onWiped           : () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context  = LocalContext.current

    LaunchedEffect(uiState.wiped) {
        if (uiState.wiped) onWiped()
    }

    Scaffold(
        containerColor = VeilColors.Background,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = VeilColors.Background),
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Outlined.ArrowBackIosNew, null,
                            tint = VeilColors.TextSecondary, modifier = Modifier.size(18.dp))
                    }
                },
                title = {
                    Text("Settings", fontSize = 17.sp, fontWeight = FontWeight.SemiBold,
                        color = VeilColors.TextPrimary)
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 40.dp)
        ) {

            // ── IDENTITY ──────────────────────────────────────────────
            SectionHeader("IDENTITY")
            IdentityCard(
                shortId       = uiState.myShortId,
                displayName   = uiState.displayName,
                isEditingName = uiState.editingName,
                nameInput     = uiState.nameInput,
                copiedToClip  = uiState.copiedToClipboard,
                onEditClick   = { viewModel.startEditingName() },
                onNameChange  = { viewModel.onNameInputChange(it) },
                onNameSave    = { viewModel.saveDisplayName() },
                onNameCancel  = { viewModel.cancelEditingName() },
                onCopyId      = {
                    copyToClipboard(context, uiState.myUserId)
                    viewModel.onIdCopied()
                }
            )
            Spacer(Modifier.height(4.dp))
            SettingsRow(
                icon     = Icons.Outlined.Palette,
                iconTint = VeilColors.Accent,
                label    = "My QR Code  ",
                sublabel = "",
                onClick  = onQrClick,
                trailing = {
                    Icon(Icons.Outlined.ChevronRight, null,
                        tint = VeilColors.TextDisabled, modifier = Modifier.size(20.dp))
                }
            )
            Spacer(Modifier.height(4.dp))

            SettingsRow(
                icon     = Icons.Outlined.Fingerprint,
                iconTint = VeilColors.Accent,
                label    = "Key fingerprint",
                sublabel = uiState.keyFingerprint,
                sublabelMono = true,
                onClick  = { viewModel.showFingerprintInfo() },
                trailing = {
                    Icon(Icons.Outlined.Info, null,
                        tint = VeilColors.TextDisabled, modifier = Modifier.size(16.dp))
                }
            )

            // --- QR Screen -------------------------------------------


            // ── APPEARANCE ────────────────────────────────────────────
            SectionHeader("APPEARANCE")
            SettingsRow(
                icon     = Icons.Outlined.Palette,
                iconTint = VeilColors.Accent,
                label    = "Theme & colors",
                sublabel = "Dark mode, accent color, bubble color, app icon",
                onClick  = onAppearanceClick,
                trailing = {
                    Icon(Icons.Outlined.ChevronRight, null,
                        tint = VeilColors.TextDisabled, modifier = Modifier.size(20.dp))
                }
            )



            // ── PRIVACY ───────────────────────────────────────────────
            SectionHeader("PRIVACY")
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(VeilColors.Surface)
                    .border(0.5.dp, VeilColors.Border, RoundedCornerShape(14.dp))
            ) {
                SettingsToggleRow(
                    icon     = Icons.Outlined.Lock,
                    iconTint = VeilColors.Accent,
                    label    = "App lock",
                    sublabel = "Require biometrics to open Veil",
                    checked  = uiState.appLockEnabled,
                    onCheckedChange = { viewModel.setAppLock(it) }
                )
                HorizontalDivider(color = VeilColors.Border, thickness = 0.5.dp,
                    modifier = Modifier.padding(start = 66.dp))
                SettingsToggleRow(
                    icon     = Icons.Outlined.DoneAll,
                    iconTint = VeilColors.TextSecondary,
                    label    = "Read receipts",
                    sublabel = "Let contacts know you've read their messages",
                    checked  = uiState.sendReadReceipts,
                    onCheckedChange = { viewModel.setReadReceipts(it) }
                )
                HorizontalDivider(color = VeilColors.Border, thickness = 0.5.dp,
                    modifier = Modifier.padding(start = 66.dp))
                SettingsToggleRow(
                    icon     = Icons.Outlined.AccessTime,
                    iconTint = VeilColors.TextSecondary,
                    label    = "Show last seen",
                    sublabel = "Let contacts see when you were last active",
                    checked  = uiState.showLastSeen,
                    onCheckedChange = { viewModel.setShowLastSeen(it) }
                )
            }

            // ── MESSAGES ──────────────────────────────────────────────
            SectionHeader("MESSAGES")
            DisappearTimerRow(
                current  = uiState.defaultDisappear,
                onChange = { viewModel.setDefaultDisappear(it) }
            )

            // ── DANGER ZONE ───────────────────────────────────────────
            SectionHeader("DANGER ZONE")
            SettingsRow(
                icon       = Icons.Outlined.DeleteForever,
                iconTint   = VeilColors.Error,
                label      = "Wipe all data",
                sublabel   = "Delete your identity, keys, and all local data",
                labelColor = VeilColors.Error,
                onClick    = { viewModel.showWipeConfirm() }
            )

            Spacer(Modifier.height(32.dp))
            Text(
                text      = "Veil v0.1.0 · Keys generated locally · Open source",
                fontSize  = 11.sp,
                color     = VeilColors.TextDisabled,
                textAlign = TextAlign.Center,
                modifier  = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                lineHeight = 18.sp
            )
        }
    }

    if (uiState.showFingerprintInfo) {
        FingerprintInfoDialog(
            fingerprint = uiState.keyFingerprint,
            onDismiss   = { viewModel.dismissFingerprintInfo() }
        )
    }

    if (uiState.showWipeConfirm) {
        WipeConfirmDialog(
            onConfirm = { viewModel.wipeAllData() },
            onDismiss = { viewModel.dismissWipeConfirm() }
        )
    }
}

// ── Identity card ──────────────────────────────────────────────────────

@Composable
private fun IdentityCard(
    shortId      : String,
    displayName  : String,
    isEditingName: Boolean,
    nameInput    : String,
    copiedToClip : Boolean,
    onEditClick  : () -> Unit,
    onNameChange : (String) -> Unit,
    onNameSave   : () -> Unit,
    onNameCancel : () -> Unit,
    onCopyId     : () -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    val focusManager   = LocalFocusManager.current

    LaunchedEffect(isEditingName) {
        if (isEditingName) focusRequester.requestFocus()
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(VeilColors.Surface)
            .border(0.5.dp, VeilColors.Border, RoundedCornerShape(14.dp))
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(60.dp).clip(CircleShape)
                .background(VeilColors.AccentSubtle)
                .border(1.5.dp, VeilColors.Accent.copy(alpha = 0.4f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text       = if (displayName.isNotBlank()) displayName.first().uppercase() else "#",
                fontSize   = 24.sp,
                fontWeight = FontWeight.SemiBold,
                color      = VeilColors.Accent,
                fontFamily = if (displayName.isBlank()) FontFamily.Monospace else FontFamily.Default
            )
        }
        Spacer(Modifier.height(14.dp))

        AnimatedContent(
            targetState = isEditingName,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "nameEdit"
        ) { editing ->
            if (editing) {
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = nameInput, onValueChange = onNameChange, singleLine = true,
                        placeholder = { Text("Nickname (optional)", color = VeilColors.TextDisabled) },
                        modifier = Modifier.weight(1f).focusRequester(focusRequester),
                        shape  = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor   = VeilColors.Accent,
                            unfocusedBorderColor = VeilColors.BorderStrong,
                            cursorColor          = VeilColors.Accent,
                            focusedTextColor     = VeilColors.TextPrimary,
                            unfocusedTextColor   = VeilColors.TextPrimary
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { onNameSave(); focusManager.clearFocus() })
                    )
                    TextButton(onClick = onNameSave) { Text("Save", color = VeilColors.Accent, fontWeight = FontWeight.Medium) }
                    TextButton(onClick = onNameCancel) { Text("Cancel", color = VeilColors.TextSecondary) }
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text       = displayName.ifBlank { "No nickname set" },
                        fontSize   = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color      = if (displayName.isBlank()) VeilColors.TextDisabled else VeilColors.TextPrimary
                    )
                    Icon(Icons.Outlined.Edit, "Edit nickname", tint = VeilColors.TextDisabled,
                        modifier = Modifier.size(16.dp).clickable { onEditClick() })
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        Row(
            modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(VeilColors.SurfaceVariant)
                .border(1.dp, VeilColors.BorderStrong, RoundedCornerShape(8.dp))
                .clickable { onCopyId() }.padding(horizontal = 14.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(shortId, fontSize = 14.sp, fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Medium, color = VeilColors.TextPrimary, letterSpacing = 1.sp)
            AnimatedContent(targetState = copiedToClip, label = "copyAnim") { copied ->
                Icon(
                    if (copied) Icons.Outlined.Check else Icons.Outlined.ContentCopy,
                    "Copy ID", tint = if (copied) VeilColors.Success else VeilColors.TextDisabled,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Text("Tap to copy · Share this to receive messages", fontSize = 11.sp,
            color = VeilColors.TextDisabled, textAlign = TextAlign.Center)
    }
}

// ── Disappear timer row ────────────────────────────────────────────────

@Composable
private fun DisappearTimerRow(current: Int?, onChange: (Int?) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val options = listOf(null to "Off", 30 to "30 seconds", 60 to "1 minute",
        300 to "5 minutes", 3600 to "1 hour", 86400 to "24 hours")

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
        .clip(RoundedCornerShape(14.dp)).background(VeilColors.Surface)
        .border(0.5.dp, VeilColors.Border, RoundedCornerShape(14.dp))) {
        Row(modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded }
            .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(modifier = Modifier.size(36.dp).clip(RoundedCornerShape(9.dp))
                .background(VeilColors.AccentSubtle), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.Timer, null, tint = VeilColors.Accent, modifier = Modifier.size(20.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text("Default disappear timer", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = VeilColors.TextPrimary)
                Text(options.firstOrNull { it.first == current }?.second ?: "Off", fontSize = 13.sp,
                    color = if (current != null) VeilColors.Accent else VeilColors.TextSecondary)
            }
            Icon(if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                null, tint = VeilColors.TextDisabled, modifier = Modifier.size(20.dp))
        }
        AnimatedVisibility(visible = expanded, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
            Column {
                HorizontalDivider(color = VeilColors.Border, thickness = 0.5.dp)
                options.forEachIndexed { i, (seconds, label) ->
                    Row(modifier = Modifier.fillMaxWidth().clickable { onChange(seconds); expanded = false }
                        .padding(horizontal = 16.dp, vertical = 13.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(label, fontSize = 15.sp,
                            color = if (current == seconds) VeilColors.Accent else VeilColors.TextPrimary,
                            modifier = Modifier.weight(1f))
                        if (current == seconds) Icon(Icons.Outlined.Check, null, tint = VeilColors.Accent, modifier = Modifier.size(18.dp))
                    }
                    if (i < options.lastIndex) HorizontalDivider(color = VeilColors.Border, thickness = 0.5.dp, modifier = Modifier.padding(start = 16.dp))
                }
            }
        }
    }
}

// ── Fingerprint dialog ─────────────────────────────────────────────────

@Composable
private fun FingerprintInfoDialog(fingerprint: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss, containerColor = VeilColors.Surface,
        titleContentColor = VeilColors.TextPrimary,
        title = { Text("Your key fingerprint", fontWeight = FontWeight.SemiBold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("This is a short representation of your public encryption key.", fontSize = 14.sp, color = VeilColors.TextSecondary, lineHeight = 22.sp)
                Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(VeilColors.SurfaceVariant).padding(14.dp), contentAlignment = Alignment.Center) {
                    Text(fingerprint, fontFamily = FontFamily.Monospace, fontSize = 15.sp, color = VeilColors.Accent, letterSpacing = 1.sp, textAlign = TextAlign.Center, lineHeight = 24.sp)
                }
                Text("Compare this with a contact in person to verify no man-in-the-middle.", fontSize = 13.sp, color = VeilColors.TextSecondary, lineHeight = 20.sp)
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Got it", color = VeilColors.Accent, fontWeight = FontWeight.Medium) } }
    )
}

// ── Wipe confirm dialog ────────────────────────────────────────────────

@Composable
private fun WipeConfirmDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss, containerColor = VeilColors.Surface,
        titleContentColor = VeilColors.TextPrimary,
        icon = { Icon(Icons.Outlined.Warning, null, tint = VeilColors.Error, modifier = Modifier.size(28.dp)) },
        title = { Text("Wipe all data?", fontWeight = FontWeight.SemiBold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("This will permanently delete:", fontSize = 14.sp, color = VeilColors.TextSecondary)
                listOf("Your anonymous identity", "Your encryption keys", "All local contacts", "All message history").forEach {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(4.dp).clip(CircleShape).background(VeilColors.Error))
                        Text(it, fontSize = 13.sp, color = VeilColors.TextSecondary)
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text("This cannot be undone.", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = VeilColors.Error)
            }
        },
        confirmButton = {
            Button(onClick = onConfirm, colors = ButtonDefaults.buttonColors(containerColor = VeilColors.Error, contentColor = Color.White), shape = RoundedCornerShape(10.dp)) {
                Text("Wipe everything", fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = VeilColors.TextSecondary) } }
    )
}

// ── Reusable rows ──────────────────────────────────────────────────────

@Composable
private fun SectionHeader(title: String) {
    Text(title, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp,
        color = VeilColors.TextDisabled, modifier = Modifier.padding(start = 32.dp, top = 24.dp, bottom = 8.dp))
}

@Composable
private fun SettingsRow(
    icon      : ImageVector, iconTint: Color, label: String,
    sublabel  : String? = null, labelColor: Color = VeilColors.TextPrimary,
    sublabelMono: Boolean = false, onClick: (() -> Unit)? = null,
    trailing  : (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(14.dp)).background(VeilColors.Surface)
            .border(0.5.dp, VeilColors.Border, RoundedCornerShape(14.dp))
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(modifier = Modifier.size(36.dp).clip(RoundedCornerShape(9.dp)).background(iconTint.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = iconTint, modifier = Modifier.size(20.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(label, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = labelColor)
            if (sublabel != null) Text(sublabel, fontSize = 12.sp, color = VeilColors.TextSecondary,
                fontFamily = if (sublabelMono) FontFamily.Monospace else FontFamily.Default,
                letterSpacing = if (sublabelMono) 0.5.sp else 0.sp, lineHeight = 18.sp)
        }
        trailing?.invoke()
    }
}

@Composable
private fun SettingsToggleRow(
    icon: ImageVector, iconTint: Color, label: String,
    sublabel: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth().background(VeilColors.Surface)
        .clickable { onCheckedChange(!checked) }.padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Box(modifier = Modifier.size(36.dp).clip(RoundedCornerShape(9.dp)).background(iconTint.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = iconTint, modifier = Modifier.size(20.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(label, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = VeilColors.TextPrimary)
            Text(sublabel, fontSize = 12.sp, color = VeilColors.TextSecondary, lineHeight = 18.sp)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor    = Color.White, checkedTrackColor    = VeilColors.Accent,
                uncheckedThumbColor  = VeilColors.TextDisabled, uncheckedTrackColor  = VeilColors.SurfaceVariant,
                uncheckedBorderColor = VeilColors.BorderStrong
            ))
    }
}

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("Veil ID", text))
}

package com.veil.ui.appearance

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.veil.ui.theme.VeilColors
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceScreen(
    viewModel   : AppearanceViewModel,
    onBackClick : () -> Unit
) {
    val config  by viewModel.config.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Auto-dismiss success snackbar
    LaunchedEffect(uiState.iconChangedSuccess) {
        if (uiState.iconChangedSuccess) {
            delay(2500)
            viewModel.clearIconChangedSuccess()
        }
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
                    Text("Appearance", fontSize = 17.sp, fontWeight = FontWeight.SemiBold,
                        color = VeilColors.TextPrimary)
                },
                actions = {
                    TextButton(onClick = { viewModel.resetToDefaults() }) {
                        Text("Reset", color = VeilColors.TextSecondary, fontSize = 13.sp)
                    }
                }
            )
        },
        snackbarHost = {
            if (uiState.iconChangedSuccess) {
                Box(
                    modifier         = Modifier.fillMaxWidth().padding(16.dp),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Snackbar(
                        containerColor = VeilColors.SurfaceVariant,
                        contentColor   = VeilColors.TextPrimary
                    ) {
                        Row(
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Outlined.Check, null,
                                tint     = VeilColors.Success,
                                modifier = Modifier.size(16.dp))
                            Text("App icon updated! Your launcher will refresh shortly.",
                                fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 40.dp)
        ) {

            // ── Live preview ───────────────────────────────────────────
            ChatPreview(config = config)

            Spacer(Modifier.height(8.dp))

            // ── Theme ──────────────────────────────────────────────────
            SectionHeader("THEME")
            ThemeSelector(selected = config.theme, onSelect = { viewModel.setTheme(it) })

            // ── Accent color ───────────────────────────────────────────
            SectionHeader("ACCENT COLOR")
            Text("Used for buttons, links, and status indicators",
                fontSize = 12.sp, color = VeilColors.TextDisabled,
                modifier = Modifier.padding(horizontal = 20.dp))
            Spacer(Modifier.height(10.dp))
            ColorPicker(
                colors   = AccentColor.entries,
                selected = config.accentColor.hex,
                getHex   = { it.hex },
                getLabel = { it.label },
                onSelect = { viewModel.setAccentColor(it) }
            )

            // ── Bubble color ───────────────────────────────────────────
            SectionHeader("MESSAGE BUBBLE COLOR")
            Text("Color of your sent message bubbles",
                fontSize = 12.sp, color = VeilColors.TextDisabled,
                modifier = Modifier.padding(horizontal = 20.dp))
            Spacer(Modifier.height(10.dp))
            ColorPicker(
                colors   = BubbleColor.entries,
                selected = config.bubbleColor.hex,
                getHex   = { it.hex },
                getLabel = { it.label },
                onSelect = { viewModel.setBubbleColor(it) }
            )

            // ── App icon ───────────────────────────────────────────────
            SectionHeader("APP ICON")
            Text("How Veil appears on your home screen",
                fontSize = 12.sp, color = VeilColors.TextDisabled,
                modifier = Modifier.padding(horizontal = 20.dp))
            Spacer(Modifier.height(10.dp))
            AppIconPicker(
                selected = config.appIcon,
                onSelect = { viewModel.requestIconChange(it) }   // shows confirm dialog
            )
        }
    }

    // ── Icon change confirm dialog ─────────────────────────────────────
    if (uiState.showIconConfirm) {
        val pending = uiState.pendingIcon
        AlertDialog(
            onDismissRequest  = { viewModel.dismissIconConfirm() },
            containerColor    = VeilColors.Surface,
            titleContentColor = VeilColors.TextPrimary,
            icon = {
                if (pending != null) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(VeilColors.SurfaceVariant)
                            .border(1.dp, VeilColors.BorderStrong, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(pending.emoji, fontSize = 24.sp)
                    }
                }
            },
            title = { Text("Change app icon?", fontWeight = FontWeight.SemiBold) },
            text  = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Switch to the ${pending?.label} icon?",
                        fontSize  = 14.sp,
                        color     = VeilColors.TextSecondary
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(VeilColors.SurfaceVariant)
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment     = Alignment.Top
                    ) {
                        Icon(Icons.Outlined.Info, null,
                            tint     = VeilColors.TextDisabled,
                            modifier = Modifier.size(14.dp).padding(top = 1.dp))
                        Text(
                            "The app icon will briefly disappear from your launcher " +
                            "while Android updates it. This is normal.",
                            fontSize  = 12.sp,
                            color     = VeilColors.TextSecondary,
                            lineHeight = 18.sp
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.confirmIconChange() },
                    colors  = ButtonDefaults.buttonColors(
                        containerColor = VeilColors.Accent,
                        contentColor   = Color(0xFF0A0A0B)
                    ),
                    shape   = RoundedCornerShape(10.dp)
                ) {
                    Text("Change icon", fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissIconConfirm() }) {
                    Text("Cancel", color = VeilColors.TextSecondary)
                }
            }
        )
    }
}

// ── Live chat preview ──────────────────────────────────────────────────

@Composable
private fun ChatPreview(config: AppearanceConfig) {
    val accentColor  = Color(android.graphics.Color.parseColor(config.accentColor.hex))
    val bubbleBg     = Color(android.graphics.Color.parseColor(config.bubbleColor.backgroundHex))
    val bubbleBorder = Color(android.graphics.Color.parseColor(config.bubbleColor.hex)).copy(alpha = 0.22f)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(VeilColors.Surface)
            .border(0.5.dp, VeilColors.Border, RoundedCornerShape(16.dp))
    ) {
        // Top bar
        Row(
            modifier = Modifier.fillMaxWidth().background(VeilColors.SurfaceVariant)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier.size(26.dp).clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.2f))
                    .border(1.dp, accentColor.copy(alpha = 0.4f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("G", fontSize = 11.sp, color = accentColor, fontWeight = FontWeight.Bold)
            }
            Column {
                Text("Ghost", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = VeilColors.TextPrimary)
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Lock, null, tint = VeilColors.TextSecondary, modifier = Modifier.size(9.dp))
                    Text("End-to-end encrypted", fontSize = 9.sp, color = VeilColors.TextSecondary)
                }
            }
        }

        // Messages
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)) {
            // Received
            Row(verticalAlignment = Alignment.Bottom) {
                Box(modifier = Modifier.size(18.dp).clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.15f))
                    .border(0.5.dp, accentColor.copy(alpha = 0.3f), CircleShape),
                    contentAlignment = Alignment.Center) {
                    Text("G", fontSize = 7.sp, color = accentColor, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(4.dp))
                Box(modifier = Modifier.clip(RoundedCornerShape(12.dp, 12.dp, 12.dp, 3.dp))
                    .background(VeilColors.SurfaceVariant)
                    .border(0.5.dp, VeilColors.BorderStrong, RoundedCornerShape(12.dp, 12.dp, 12.dp, 3.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp)) {
                    Text("Hey! Love the app 👀", fontSize = 12.sp, color = VeilColors.TextPrimary)
                }
            }
            // Sent 1
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Box(modifier = Modifier.clip(RoundedCornerShape(12.dp, 12.dp, 3.dp, 12.dp))
                    .background(bubbleBg).border(0.5.dp, bubbleBorder, RoundedCornerShape(12.dp, 12.dp, 3.dp, 12.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("Built it myself actually", fontSize = 12.sp, color = VeilColors.TextPrimary)
                        Text("✓✓", fontSize = 9.sp, color = accentColor.copy(alpha = 0.7f))
                    }
                }
            }
            // Sent 2
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Box(modifier = Modifier.clip(RoundedCornerShape(12.dp, 12.dp, 3.dp, 12.dp))
                    .background(bubbleBg).border(0.5.dp, bubbleBorder, RoundedCornerShape(12.dp, 12.dp, 3.dp, 12.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("No accounts. Zero PII.", fontSize = 12.sp, color = VeilColors.TextPrimary)
                        Text("✓✓", fontSize = 9.sp, color = accentColor.copy(alpha = 0.7f))
                    }
                }
            }
        }

        // Input bar
        Row(modifier = Modifier.fillMaxWidth().background(VeilColors.SurfaceVariant)
            .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(modifier = Modifier.weight(1f).height(28.dp).clip(RoundedCornerShape(14.dp))
                .background(VeilColors.Surface).border(0.5.dp, VeilColors.Border, RoundedCornerShape(14.dp))
                .padding(horizontal = 12.dp), contentAlignment = Alignment.CenterStart) {
                Text("Message", fontSize = 11.sp, color = VeilColors.TextDisabled)
            }
            Box(modifier = Modifier.size(28.dp).clip(CircleShape).background(accentColor),
                contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.Send, null, tint = Color(0xFF0A0A0B), modifier = Modifier.size(14.dp))
            }
        }
    }
}

// ── Theme selector ─────────────────────────────────────────────────────

@Composable
private fun ThemeSelector(selected: AppTheme, onSelect: (AppTheme) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
        .clip(RoundedCornerShape(14.dp)).background(VeilColors.Surface)
        .border(0.5.dp, VeilColors.Border, RoundedCornerShape(14.dp))) {
        AppTheme.entries.forEachIndexed { index, theme ->
            Row(modifier = Modifier.fillMaxWidth().clickable { onSelect(theme) }
                .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Box(modifier = Modifier.size(36.dp).clip(RoundedCornerShape(9.dp))
                    .background(when (theme) {
                        AppTheme.DARK   -> Color(0xFF111113)
                        AppTheme.LIGHT  -> Color(0xFFF5F5F7)
                        AppTheme.AMOLED -> Color(0xFF000000)
                        AppTheme.SYSTEM -> Color(0xFF2A2A2E)
                    }).border(1.dp, VeilColors.BorderStrong, RoundedCornerShape(9.dp)))
                Column(modifier = Modifier.weight(1f)) {
                    Text(theme.label, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = VeilColors.TextPrimary)
                    Text(when (theme) {
                        AppTheme.DARK   -> "Near-black background, easy on eyes"
                        AppTheme.LIGHT  -> "Light background, bright surfaces"
                        AppTheme.AMOLED -> "Pure black, saves battery on OLED screens"
                        AppTheme.SYSTEM -> "Follows your device dark/light setting"
                    }, fontSize = 12.sp, color = VeilColors.TextSecondary)
                }
                if (selected == theme) Icon(Icons.Outlined.Check, null, tint = VeilColors.Accent, modifier = Modifier.size(20.dp))
            }
            if (index < AppTheme.entries.lastIndex)
                HorizontalDivider(color = VeilColors.Border, thickness = 0.5.dp, modifier = Modifier.padding(start = 66.dp))
        }
    }
}

// ── Color picker grid ──────────────────────────────────────────────────

@Composable
private fun <T> ColorPicker(
    colors: List<T>, selected: String,
    getHex: (T) -> String, getLabel: (T) -> String, onSelect: (T) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
        .clip(RoundedCornerShape(14.dp)).background(VeilColors.Surface)
        .border(0.5.dp, VeilColors.Border, RoundedCornerShape(14.dp)).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        colors.chunked(4).forEach { row ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { item ->
                    val hex        = getHex(item)
                    val isSelected = hex == selected
                    val color      = Color(android.graphics.Color.parseColor(hex))
                    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Box(modifier = Modifier.size(42.dp).clip(CircleShape)
                            .background(color.copy(alpha = 0.2f))
                            .border(if (isSelected) 2.dp else 1.dp,
                                if (isSelected) color else color.copy(alpha = 0.4f), CircleShape)
                            .clickable { onSelect(item) },
                            contentAlignment = Alignment.Center) {
                            if (isSelected) Icon(Icons.Outlined.Check, null, tint = color, modifier = Modifier.size(18.dp))
                        }
                        Text(getLabel(item), fontSize = 10.sp,
                            color     = if (isSelected) color else VeilColors.TextSecondary,
                            fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                            textAlign = TextAlign.Center)
                    }
                }
                repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

// ── App icon picker ────────────────────────────────────────────────────

@Composable
private fun AppIconPicker(selected: AppIcon, onSelect: (AppIcon) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
        .clip(RoundedCornerShape(14.dp)).background(VeilColors.Surface)
        .border(0.5.dp, VeilColors.Border, RoundedCornerShape(14.dp))) {
        AppIcon.entries.forEachIndexed { index, icon ->
            Row(modifier = Modifier.fillMaxWidth().clickable { onSelect(icon) }
                .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Box(modifier = Modifier.size(42.dp).clip(RoundedCornerShape(10.dp))
                    .background(VeilColors.SurfaceVariant)
                    .border(1.dp, if (selected == icon) VeilColors.Accent.copy(alpha = 0.5f)
                                  else VeilColors.BorderStrong, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center) {
                    Text(icon.emoji, fontSize = 20.sp)
                }
                Text(icon.label, fontSize = 15.sp, fontWeight = FontWeight.Medium,
                    color = VeilColors.TextPrimary, modifier = Modifier.weight(1f))
                if (selected == icon) Icon(Icons.Outlined.Check, null, tint = VeilColors.Accent, modifier = Modifier.size(20.dp))
            }
            if (index < AppIcon.entries.lastIndex)
                HorizontalDivider(color = VeilColors.Border, thickness = 0.5.dp, modifier = Modifier.padding(start = 72.dp))
        }

        // Info note
        Row(modifier = Modifier.fillMaxWidth().background(VeilColors.SurfaceVariant)
            .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
            Icon(Icons.Outlined.Info, null, tint = VeilColors.TextDisabled, modifier = Modifier.size(14.dp).padding(top = 1.dp))
            Text("The icon briefly disappears from your launcher during the switch. This is normal — Android handles it.",
                fontSize = 11.sp, color = VeilColors.TextSecondary, lineHeight = 17.sp)
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(title, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp,
        color = VeilColors.TextDisabled, modifier = Modifier.padding(start = 32.dp, top = 24.dp, bottom = 8.dp))
}

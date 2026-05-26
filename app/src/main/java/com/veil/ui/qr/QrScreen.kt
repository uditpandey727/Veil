package com.veil.ui.qr

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.veil.ui.theme.VeilColors
import java.util.concurrent.Executors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QrScreen(
    viewModel   : QrViewModel,
    onBackClick : () -> Unit,
    onContactAdded: (contactId: String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Navigate when contact is added
    LaunchedEffect(uiState.contactAdded) {
        uiState.contactAdded?.let { onContactAdded(it) }
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
                        Icon(
                            Icons.Outlined.ArrowBackIosNew,
                            contentDescription = "Back",
                            tint = VeilColors.TextSecondary
                        )
                    }
                },
                title = {
                    Text(
                        "Add contact",
                        fontSize   = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color      = VeilColors.TextPrimary
                    )
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // ── Tab switcher ──────────────────────────────────────────
            TabRow(
                activeTab = uiState.activeTab,
                onTabChange = { viewModel.setActiveTab(it) }
            )

            // ── Tab content ───────────────────────────────────────────
            AnimatedContent(
                targetState = uiState.activeTab,
                transitionSpec = {
                    if (targetState == QrViewModel.Tab.SCAN) {
                        slideInHorizontally { it } + fadeIn() togetherWith
                        slideOutHorizontally { -it } + fadeOut()
                    } else {
                        slideInHorizontally { -it } + fadeIn() togetherWith
                        slideOutHorizontally { it } + fadeOut()
                    }
                },
                label = "tabContent"
            ) { tab ->
                when (tab) {
                    QrViewModel.Tab.MY_QR -> MyQrTab(uiState = uiState)
                    QrViewModel.Tab.SCAN  -> ScanTab(
                        uiState      = uiState,
                        onQrScanned  = { viewModel.onQrScanned(it) },
                        onAddContact = { viewModel.addScannedContact(it) },
                        onDismiss    = { viewModel.dismissScanResult() }
                    )
                }
            }
        }
    }
}

// ── Tab switcher ───────────────────────────────────────────────────────

@Composable
private fun TabRow(
    activeTab   : QrViewModel.Tab,
    onTabChange : (QrViewModel.Tab) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(VeilColors.SurfaceVariant)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        listOf(
            QrViewModel.Tab.MY_QR to "My QR code",
            QrViewModel.Tab.SCAN  to "Scan contact"
        ).forEach { (tab, label) ->
            val isActive = activeTab == tab
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(9.dp))
                    .background(
                        animateColorAsState(
                            if (isActive) VeilColors.Surface else Color.Transparent,
                            label = "tabBg"
                        ).value
                    )
                    .then(
                        if (isActive) Modifier.border(
                            0.5.dp, VeilColors.Border, RoundedCornerShape(9.dp)
                        ) else Modifier
                    )
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text       = label,
                    fontSize   = 14.sp,
                    fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
                    color      = if (isActive) VeilColors.TextPrimary else VeilColors.TextSecondary
                )
            }
        }
    }
}

// ── MY QR tab ──────────────────────────────────────────────────────────

@Composable
private fun MyQrTab(uiState: QrViewModel.UiState) {
    Column(
        modifier            = Modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically)
    ) {
        Text(
            text      = "Let someone scan this\nto add you as a contact.",
            fontSize  = 15.sp,
            color     = VeilColors.TextSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 24.sp
        )

        // QR code card
        Box(
            modifier = Modifier
                .size(260.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(VeilColors.Surface)
                .border(1.dp, VeilColors.Border, RoundedCornerShape(20.dp))
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            if (uiState.isLoading || uiState.myQrBitmap == null) {
                // Loading shimmer
                CircularProgressIndicator(
                    color       = VeilColors.Accent,
                    modifier    = Modifier.size(32.dp),
                    strokeWidth = 2.dp
                )
            } else {
                androidx.compose.foundation.Image(
                    bitmap      = uiState.myQrBitmap.asImageBitmap(),
                    contentDescription = "Your QR code",
                    modifier    = Modifier.fillMaxSize()
                )
            }
        }

        // Anonymous ID below the QR
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text      = "YOUR ANONYMOUS ID",
                fontSize  = 10.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp,
                color     = VeilColors.TextDisabled
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(9.dp))
                    .background(VeilColors.SurfaceVariant)
                    .border(1.dp, VeilColors.BorderStrong, RoundedCornerShape(9.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text       = uiState.myShortId,
                    fontSize   = 15.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Medium,
                    color      = VeilColors.TextPrimary,
                    letterSpacing = 1.sp
                )
            }
        }

        // Verified badge explanation
        Row(
            modifier              = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(VeilColors.AccentSubtle)
                .border(1.dp, VeilColors.Accent.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                Icons.Outlined.VerifiedUser,
                contentDescription = null,
                tint     = VeilColors.Accent,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text      = "Contacts added by QR scan are automatically verified — no MITM possible.",
                fontSize  = 12.sp,
                color     = VeilColors.Accent.copy(alpha = 0.85f),
                lineHeight = 18.sp
            )
        }
    }
}

// ── SCAN tab ───────────────────────────────────────────────────────────

@Composable
private fun ScanTab(
    uiState     : QrViewModel.UiState,
    onQrScanned : (String) -> Unit,
    onAddContact: (QrViewModel.ScanResult.VeilContact) -> Unit,
    onDismiss   : () -> Unit
) {
    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasCameraPermission = granted }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (hasCameraPermission) {
            // Camera preview with QR scanner
            CameraQrScanner(
                modifier    = Modifier.fillMaxSize(),
                onQrScanned = onQrScanned
            )

            // Scanning frame overlay
            ScannerOverlay()

        } else {
            // Permission denied state
            PermissionDeniedMessage(
                onRequestPermission = { permissionLauncher.launch(Manifest.permission.CAMERA) }
            )
        }

        // Result sheet — appears when QR is decoded
        uiState.scanResult?.let { result ->
            ScanResultSheet(
                result       = result,
                onAddContact = { if (result is QrViewModel.ScanResult.VeilContact) onAddContact(result) },
                onDismiss    = onDismiss
            )
        }
    }
}

// ── Camera view with ZXing analyzer ───────────────────────────────────

@Composable
private fun CameraQrScanner(
    modifier    : Modifier,
    onQrScanned : (String) -> Unit
) {
    val context        = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val executor       = remember { Executors.newSingleThreadExecutor() }
    val reader         = remember { MultiFormatReader() }
    var lastScan       = remember { 0L }

    AndroidView(
        modifier = modifier,
        factory  = { ctx ->
            val previewView = PreviewView(ctx)
            val cameraFuture = ProcessCameraProvider.getInstance(ctx)

            cameraFuture.addListener({
                val cameraProvider = cameraFuture.get()

                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also { analysis ->
                        analysis.setAnalyzer(executor) { imageProxy ->
                            val now = System.currentTimeMillis()
                            if (now - lastScan > 1000) { // throttle to 1 scan/sec
                                imageProxy.use {
                                    try {
                                        val buffer = imageProxy.planes[0].buffer
                                        val bytes  = ByteArray(buffer.remaining())
                                        buffer.get(bytes)
                                        val source = PlanarYUVLuminanceSource(
                                            bytes,
                                            imageProxy.width, imageProxy.height,
                                            0, 0,
                                            imageProxy.width, imageProxy.height,
                                            false
                                        )
                                        val result = reader.decode(BinaryBitmap(HybridBinarizer(source)))
                                        lastScan = now
                                        onQrScanned(result.text)
                                    } catch (_: Exception) { /* no QR in frame */ }
                                }
                            }
                        }
                    }

                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageAnalysis
                )
            }, ContextCompat.getMainExecutor(ctx))

            previewView
        }
    )
}

// ── Scanning frame UI overlay ──────────────────────────────────────────

@Composable
private fun ScannerOverlay() {
    // Animated scan line
    val infiniteTransition = rememberInfiniteTransition(label = "scanLine")
    val scanLineY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue  = 1f,
        animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing), RepeatMode.Reverse),
        label        = "y"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        // Dimmed overlay with clear center square
        // (simplified — production would use Canvas for precise cutout)

        // Corner brackets
        val cornerSize  = 200.dp
        val cornerStroke = 3.dp
        val cornerLen   = 28.dp

        Box(
            modifier         = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // Outer frame
            Box(modifier = Modifier.size(cornerSize)) {
                // Top-left
                CornerBracket(Alignment.TopStart, cornerLen, cornerStroke)
                // Top-right
                CornerBracket(Alignment.TopEnd, cornerLen, cornerStroke)
                // Bottom-left
                CornerBracket(Alignment.BottomStart, cornerLen, cornerStroke)
                // Bottom-right
                CornerBracket(Alignment.BottomEnd, cornerLen, cornerStroke)

                // Animated scan line inside frame
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.5.dp)
                        .background(VeilColors.Accent.copy(alpha = 0.8f))
                        .align(Alignment.TopStart)
                        .offset(y = (cornerSize * scanLineY))
                )
            }
        }

        // Instruction text at bottom
        Text(
            text     = "Point camera at a Veil QR code",
            fontSize = 14.sp,
            color    = Color.White,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 60.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.Black.copy(alpha = 0.5f))
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun BoxScope.CornerBracket(
    alignment  : Alignment,
    length     : androidx.compose.ui.unit.Dp,
    stroke     : androidx.compose.ui.unit.Dp
) {
    Box(
        modifier = Modifier
            .size(length)
            .align(alignment)
    ) {
        // Horizontal line
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(stroke)
                .background(VeilColors.Accent)
                .align(
                    if (alignment == Alignment.TopStart || alignment == Alignment.TopEnd)
                        Alignment.TopCenter else Alignment.BottomCenter
                )
        )
        // Vertical line
        Box(
            modifier = Modifier
                .width(stroke)
                .fillMaxHeight()
                .background(VeilColors.Accent)
                .align(
                    if (alignment == Alignment.TopStart || alignment == Alignment.BottomStart)
                        Alignment.CenterStart else Alignment.CenterEnd
                )
        )
    }
}

// ── Scan result bottom sheet ───────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScanResultSheet(
    result      : QrViewModel.ScanResult,
    onAddContact: () -> Unit,
    onDismiss   : () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = sheetState,
        containerColor   = VeilColors.Surface,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(VeilColors.BorderStrong)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            when (result) {
                is QrViewModel.ScanResult.VeilContact -> {
                    // Success icon
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(VeilColors.AccentSubtle)
                            .border(1.dp, VeilColors.Accent.copy(alpha = 0.3f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Outlined.PersonAdd,
                            contentDescription = null,
                            tint     = VeilColors.Accent,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    if (result.isAlreadyContact) {
                        Text(
                            "Already a contact",
                            fontSize   = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color      = VeilColors.TextPrimary
                        )
                        Text(
                            "You've already added ${result.shortId}.",
                            fontSize  = 14.sp,
                            color     = VeilColors.TextSecondary,
                            textAlign = TextAlign.Center
                        )
                        Button(
                            onClick = onDismiss,
                            shape   = RoundedCornerShape(12.dp),
                            colors  = ButtonDefaults.buttonColors(
                                containerColor = VeilColors.SurfaceVariant,
                                contentColor   = VeilColors.TextPrimary
                            ),
                            modifier = Modifier.fillMaxWidth().height(50.dp)
                        ) { Text("Close", fontWeight = FontWeight.SemiBold) }

                    } else {
                        Text(
                            "Veil contact found",
                            fontSize   = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color      = VeilColors.TextPrimary
                        )

                        // Contact ID display
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(VeilColors.SurfaceVariant)
                                .border(1.dp, VeilColors.BorderStrong, RoundedCornerShape(12.dp))
                                .padding(16.dp),
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(VeilColors.AccentSubtle)
                                    .border(1.dp, VeilColors.Accent.copy(alpha = 0.3f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "#",
                                    fontSize   = 16.sp,
                                    color      = VeilColors.Accent,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Column {
                                Text(
                                    result.shortId,
                                    fontSize   = 15.sp,
                                    fontWeight = FontWeight.Medium,
                                    color      = VeilColors.TextPrimary,
                                    fontFamily = FontFamily.Monospace,
                                    letterSpacing = 1.sp
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        Icons.Outlined.VerifiedUser,
                                        contentDescription = null,
                                        tint     = VeilColors.Accent,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        "Key verified via QR",
                                        fontSize = 12.sp,
                                        color    = VeilColors.Accent
                                    )
                                }
                            }
                        }

                        // Buttons
                        Button(
                            onClick  = onAddContact,
                            shape    = RoundedCornerShape(12.dp),
                            colors   = ButtonDefaults.buttonColors(
                                containerColor = VeilColors.Accent,
                                contentColor   = Color(0xFF0A0A0B)
                            ),
                            modifier = Modifier.fillMaxWidth().height(50.dp)
                        ) {
                            Icon(
                                Icons.Outlined.PersonAdd,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Add contact", fontWeight = FontWeight.SemiBold)
                        }
                        TextButton(onClick = onDismiss) {
                            Text("Cancel", color = VeilColors.TextSecondary)
                        }
                    }
                }

                is QrViewModel.ScanResult.InvalidQr -> {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(VeilColors.Error.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Outlined.QrCodeScanner,
                            contentDescription = null,
                            tint     = VeilColors.Error,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Text(
                        "Not a Veil QR code",
                        fontSize   = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color      = VeilColors.TextPrimary
                    )
                    Text(
                        "This QR doesn't contain a Veil identity. Make sure you're scanning a Veil contact's code.",
                        fontSize  = 14.sp,
                        color     = VeilColors.TextSecondary,
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp
                    )
                    Button(
                        onClick = onDismiss,
                        shape   = RoundedCornerShape(12.dp),
                        colors  = ButtonDefaults.buttonColors(
                            containerColor = VeilColors.SurfaceVariant,
                            contentColor   = VeilColors.TextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth().height(50.dp)
                    ) { Text("Try again", fontWeight = FontWeight.SemiBold) }
                }
            }
        }
    }
}

// ── Camera permission denied ───────────────────────────────────────────

@Composable
private fun PermissionDeniedMessage(onRequestPermission: () -> Unit) {
    Column(
        modifier            = Modifier.fillMaxSize().padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Outlined.CameraAlt,
            contentDescription = null,
            tint     = VeilColors.TextDisabled,
            modifier = Modifier.size(48.dp)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            "Camera access needed",
            fontSize   = 17.sp,
            fontWeight = FontWeight.SemiBold,
            color      = VeilColors.TextPrimary,
            textAlign  = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Veil needs camera access to scan QR codes. Your camera is never used for anything else.",
            fontSize  = 14.sp,
            color     = VeilColors.TextSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp
        )
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onRequestPermission,
            shape   = RoundedCornerShape(12.dp),
            colors  = ButtonDefaults.buttonColors(
                containerColor = VeilColors.Accent,
                contentColor   = Color(0xFF0A0A0B)
            )
        ) { Text("Allow camera", fontWeight = FontWeight.SemiBold) }
    }
}

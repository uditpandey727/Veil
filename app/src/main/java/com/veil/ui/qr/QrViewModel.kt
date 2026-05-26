package com.veil.ui.qr

import android.graphics.Bitmap
import android.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.veil.data.repository.VeilRepository
import com.veil.domain.model.Contact
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

class QrViewModel(
    private val repository: VeilRepository
) : ViewModel() {

    enum class Tab { MY_QR, SCAN }

    data class UiState(
        val activeTab      : Tab        = Tab.MY_QR,
        val myUserId       : String     = "",
        val myShortId      : String     = "",
        val myQrBitmap     : Bitmap?    = null,
        val isLoading      : Boolean    = true,
        val scanResult     : ScanResult? = null,
        val errorMessage   : String?    = null,

        // Set when a contact is successfully added — triggers navigation in QrScreen
        val contactAdded   : String?    = null
    )

    sealed class ScanResult {
        data class VeilContact(
            val userId          : String,
            val shortId         : String,
            val publicKey       : String,
            val isAlreadyContact: Boolean = false
        ) : ScanResult()

        data class InvalidQr(val raw: String) : ScanResult()
    }

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init { generateMyQr() }

    // ── My QR ─────────────────────────────────────────────────────────

    private fun generateMyQr() {
        viewModelScope.launch {
            val user      = repository.getCurrentUser()
            val publicKey = repository.getMyPublicKey() ?: ""

            val payload = JSONObject().apply {
                put("id", user.userId)
                put("pk", publicKey)
                put("v",  1)
            }.toString()

            val bitmap = withContext(Dispatchers.Default) {
                generateQrBitmap(payload, size = 512)
            }

            _uiState.update { it.copy(
                myUserId   = user.userId,
                myShortId  = "# ${user.userId.take(8).uppercase()}",
                myQrBitmap = bitmap,
                isLoading  = false
            )}
        }
    }

    // ── Scanner ───────────────────────────────────────────────────────

    fun setActiveTab(tab: Tab) {
        _uiState.update { it.copy(activeTab = tab, scanResult = null) }
    }

    fun onQrScanned(raw: String) {
        if (_uiState.value.scanResult != null) return

        viewModelScope.launch {
            try {
                val json      = JSONObject(raw)
                val userId    = json.getString("id")
                val publicKey = json.getString("pk")
                val version   = json.optInt("v", 1)

                if (version != 1 || userId.isBlank() || publicKey.isBlank()) {
                    _uiState.update { it.copy(scanResult = ScanResult.InvalidQr(raw)) }
                    return@launch
                }

                _uiState.update { it.copy(
                    scanResult = ScanResult.VeilContact(
                        userId           = userId,
                        shortId          = "# ${userId.take(8).uppercase()}",
                        publicKey        = publicKey,
                        isAlreadyContact = repository.isContact(userId)
                    )
                )}
            } catch (e: Exception) {
                _uiState.update { it.copy(scanResult = ScanResult.InvalidQr(raw)) }
            }
        }
    }

    fun addScannedContact(result: ScanResult.VeilContact) {
        viewModelScope.launch {
            repository.addContact(
                Contact(
                    contactId  = result.userId,
                    publicKey  = result.publicKey,
                    isVerified = true   // key came from QR in-person = verified ✓
                )
            )
            // Set contactAdded — QrScreen's LaunchedEffect will navigate to chat
            _uiState.update { it.copy(
                scanResult   = null,
                contactAdded = result.userId
            )}
        }
    }

    fun dismissScanResult() {
        _uiState.update { it.copy(scanResult = null) }
    }

    fun clearError() { _uiState.update { it.copy(errorMessage = null) } }

    // ── QR Bitmap generation ──────────────────────────────────────────

    private fun generateQrBitmap(content: String, size: Int): Bitmap {
        val hints = mapOf(
            EncodeHintType.MARGIN          to 1,
            EncodeHintType.ERROR_CORRECTION to com.google.zxing.qrcode.decoder.ErrorCorrectionLevel.H
        )
        val matrix  = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, size, size, hints)
        val bitmap  = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)

        val moduleColor = 0xFF4ECDC4.toInt()   // teal modules
        val bgColor     = 0xFF111113.toInt()   // dark background

        for (x in 0 until size) {
            for (y in 0 until size) {
                bitmap.setPixel(x, y, if (matrix[x, y]) moduleColor else bgColor)
            }
        }
        return bitmap
    }
}

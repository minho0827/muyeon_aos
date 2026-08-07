package com.muyeon.app.ui.qr

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.muyeon.app.webview.ScanQRWebViewInterface
import com.muyeon.app.data.models.qr.ReservationData
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

import kotlinx.serialization.json.Json

data class QrScannerUiState(
    val isCameraPermissionGranted: Boolean = false,
    val showPermissionDeniedDialog: Boolean = false,
    val parsingError: String? = null,
    val showParsingErrorDialog: Boolean = false
)

sealed class QrScannerNavigationEvent {
    data object NavigateBack : QrScannerNavigationEvent()
    data class NavigateToResult(val result: String) : QrScannerNavigationEvent()
}

class QrScannerViewModel(
    private val scanQRInterface: ScanQRWebViewInterface?
) : ViewModel() {

    @Suppress("unused")
    private val jsonParser = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    private val _uiState = MutableStateFlow(QrScannerUiState())
    val uiState: StateFlow<QrScannerUiState> = _uiState

    private val _navigationEvent = Channel<QrScannerNavigationEvent>(Channel.BUFFERED)
    val navigationEvent = _navigationEvent.receiveAsFlow()

    fun onPermissionResult(isGranted: Boolean) {
        _uiState.update { it.copy(isCameraPermissionGranted = isGranted) }
    }

    fun handleBarcodeDetected(qrString: String) {
        val decoded = try {
            URLDecoder.decode(qrString, StandardCharsets.UTF_8.name())
        } catch (e: Exception) {
            qrString
        }

        try {
            val gson = Gson()
            gson.fromJson(decoded, ReservationData::class.java)

            viewModelScope.launch {
                _navigationEvent.send(QrScannerNavigationEvent.NavigateToResult(qrString))
            }
        } catch (e: JsonSyntaxException) {
            _uiState.update {
                it.copy(
                    parsingError = "QR_PARSE_ERROR",
                    showParsingErrorDialog = true
                )
            }
        } catch (e: Exception) {
            _uiState.update {
                it.copy(
                    parsingError = "QR_PARSE_ERROR",
                    showParsingErrorDialog = true
                )
            }
        }
    }

    fun handleParsingErrorConfirmed() {
        scanQRInterface?.sendQRResultToWeb(data = null, success = 400)
        _uiState.update { it.copy(parsingError = null, showParsingErrorDialog = false) }
    }

    @Suppress("unused")
    fun onNavigationIconClick() {
        scanQRInterface?.sendQRResultToWeb(data = null, success = 499)
        viewModelScope.launch {
            _navigationEvent.send(QrScannerNavigationEvent.NavigateBack)
        }
    }
}

class QrScannerViewModelFactory(
    private val scanQRInterface: ScanQRWebViewInterface?
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(QrScannerViewModel::class.java)) {
            return QrScannerViewModel(scanQRInterface) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
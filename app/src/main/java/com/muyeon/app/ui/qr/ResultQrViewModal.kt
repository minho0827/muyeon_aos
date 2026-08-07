package com.muyeon.app.ui.qr

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.muyeon.app.R
import com.muyeon.app.data.api_endpoint.QrService
import com.muyeon.app.data.models.qr.QrResponse
import com.muyeon.app.data.models.qr.ReservationData
import com.muyeon.app.webview.ScanQRWebViewInterface
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

@Suppress("unused")
data class ResultQrUiState(
    val reservationData: ReservationData? = null,
    val decodedResult: String = "",
    val parsingError: String? = null,
    val isLoading: Boolean = false,
    val dialogMessage: String? = null,
    val apiStatus: Int = 404,
    val apiResponse: QrResponse? = null
) {
    val isReady: Boolean = reservationData != null && parsingError == null
}

sealed class ResultQRNavigationEvent {
    data object NavigateBack : ResultQRNavigationEvent()
}

class ResultQRViewModel(
    qrString: String,
    private val scanQRInterface: ScanQRWebViewInterface?
) : ViewModel() {

    private val _uiState = MutableStateFlow(ResultQrUiState())
    val uiState: StateFlow<ResultQrUiState> = _uiState

    private val _navigationEvent = Channel<ResultQRNavigationEvent>(Channel.BUFFERED)
    val navigationEvent = _navigationEvent.receiveAsFlow()

    init {
        parseQrString(qrString)
    }

    private fun parseQrString(qrString: String) {
        val decoded = try {
            URLDecoder.decode(qrString, StandardCharsets.UTF_8.name())
        } catch (e: Exception) {
            qrString
        }

        _uiState.update { it.copy(decodedResult = decoded) }

        try {
            val gson = Gson()
            val data: ReservationData = gson.fromJson(decoded, ReservationData::class.java)
            _uiState.update {
                it.copy(reservationData = data, parsingError = null)
            }
        } catch (e: JsonSyntaxException) {
            _uiState.update {
                it.copy(
                    parsingError = "Error Json: ${e.message}",
                    reservationData = null,
                    dialogMessage = R.string.fail.toString(),
                )
            }
        } catch (e: Exception) {
            _uiState.update {
                it.copy(
                    parsingError = "Unknown error during parsing: ${e.message}",
                    reservationData = null,
                    dialogMessage = R.string.fail.toString(),
                )
            }
        }
    }

    fun callApiAndSendResult() {
        val data = _uiState.value.reservationData

        if (data == null || _uiState.value.isLoading) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, dialogMessage = null) }

            val apiResultCode = QrService.checkQrCode(
                data.rsvSeq,
                data.mbrSeq,
                data.cfFdSeq,
                data.rsvStatusCd,
                data.rsvDate,
                data.rsvCnt
            )

            val message = if (apiResultCode != null) "Success" else "Fail"
            val status = if (apiResultCode != null) 200 else 404

            _uiState.update {
                it.copy(
                    isLoading = false,
                    dialogMessage = message,
                    apiStatus = status,
                    apiResponse = apiResultCode
                )
            }
        }
    }

    fun sendResultAndDismiss() {
        val data = _uiState.value.apiResponse
        val success = _uiState.value.apiStatus

        if (data != null) {
            scanQRInterface?.sendQRResultToWeb(
                data = data,
                success = success,
            )
        }

        dismissDialog()

        viewModelScope.launch {
            _navigationEvent.send(ResultQRNavigationEvent.NavigateBack)
        }
    }

    @Suppress("unused")
    fun handleParsingErrorConfirmation() {
        sendResultAndDismiss()
    }

    fun dismissDialog() {
        _uiState.update { it.copy(dialogMessage = null) }
    }
}

class ResultQRViewModelFactory(
    private val qrString: String,
    private val scanQRInterface: ScanQRWebViewInterface?
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ResultQRViewModel::class.java)) {
            return ResultQRViewModel(qrString, scanQRInterface) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
package com.muyeon.app.ui.ipconfig

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.muyeon.app.data.models.ipconfig.IpHistoryItem
import com.muyeon.app.data.repository.AuthRepositoryImpl
import com.muyeon.app.data.repository.IpConfigRepository
import com.muyeon.app.routers.SplashRouter
import com.muyeon.app.routers.SplashRouterImpl
import com.muyeon.app.utils.NetworkMonitor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

sealed class ConnectEvent {
    data object Success : ConnectEvent()
}

class IpConfigViewModel(application: Application) : AndroidViewModel(application) {

    private val networkMonitor = NetworkMonitor(application)

    private val repo = IpConfigRepository(application)

    private val _currentIp = MutableStateFlow(repo.getCurrentIp())
    val currentIp: StateFlow<String> = _currentIp.asStateFlow()

    private val _history = MutableStateFlow(repo.getIpHistory())
    val history: StateFlow<List<IpHistoryItem>> = _history.asStateFlow()

    private val _ipError = MutableStateFlow<String?>(null)
    val ipError: StateFlow<String?> = _ipError.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val navigator: SplashRouter = SplashRouterImpl(getApplication())
    private val authRepo = AuthRepositoryImpl(getApplication(), Dispatchers.IO)

    private val _connectEvent = MutableSharedFlow<ConnectEvent>()
    val connectEvent: SharedFlow<ConnectEvent> = _connectEvent.asSharedFlow()

    private fun validateIp(rawIp: String): String? {

        if (!networkMonitor.isConnected.value) {
            return "No Internet Connection"
        }
        if (rawIp.isEmpty()) {
            return "Please enter IP address"
        }

        return null
    }

    override fun onCleared() {
        super.onCleared()
        networkMonitor.unregister()
    }

    fun onIpChange(newIp: String) {
        _currentIp.value = newIp
        _ipError.value = validateIp(newIp)
    }

    fun onConnect() {
        viewModelScope.launch {
            _ipError.value = null
            _isLoading.value = true


            val raw = _currentIp.value.trim().trimEnd('/')
            val url = raw.takeIf {
                it.startsWith("http://", true) ||
                        it.startsWith("https://", true)
            } ?: "http://$raw"

            val reachableResult = runCatching {
                withContext(Dispatchers.IO) {
                    OkHttpClient.Builder()
                        .connectTimeout(15, TimeUnit.SECONDS)
                        .readTimeout(15, TimeUnit.SECONDS)
                        .build()
                        .newCall(Request.Builder().url(url).head().build())
                        .execute()
                        .use { it.isSuccessful }
                }
            }

            _isLoading.value = false

            val hasToken = authRepo.hasValidToken()

            if (hasToken) {
                navigator.navigateToHome()
            } else {
                reachableResult.fold(onSuccess = { _ ->
                    repo.saveIpAddress(raw)
                    _connectEvent.emit(ConnectEvent.Success)
                }, onFailure = { throwable ->
                    _ipError.value = when (throwable) {
                        is java.net.SocketTimeoutException -> "Connection timed out"
                        is java.net.UnknownHostException    -> "Domain not found"
                        is java.io.IOException              -> "No Internet Connection"
                        else                                 -> "Unexpected error"
                    }
                })
            }
        }
    }
}
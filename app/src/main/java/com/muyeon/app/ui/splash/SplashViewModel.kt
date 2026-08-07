package com.muyeon.app.ui.splash

import com.muyeon.app.BuildConfig
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.muyeon.app.routers.SplashRouter
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SplashViewModel(
    private val navigator: SplashRouter,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {

    fun checkTokenAndNavigate() {
        viewModelScope.launch(dispatcher) {
            // iOS SplashView: 0.5초 후 라우팅(로그인 상태면 즉시 진입). 동일 감각으로 맞춤.
            delay(500)

            navigator.navigateToWebView()
            return@launch
        }
    }
}

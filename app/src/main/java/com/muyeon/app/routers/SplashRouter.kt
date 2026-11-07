package com.muyeon.app.routers

import android.content.Context
import android.content.Intent
import com.muyeon.app.activity.IpConnectActivity
import com.muyeon.app.activity.LoginActivity
import com.muyeon.app.activity.SplashActivity

interface SplashRouter {
    fun navigateToLogin()
    fun navigateToWebView()
    fun navigateToIpConnect()
}

class SplashRouterImpl(private val context: Context) : SplashRouter {
    override fun navigateToLogin() {
        val intent = Intent(context, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        context.startActivity(intent)
    }

    override fun navigateToWebView() {
        // ★ 2026-11-07 앱 루트는 탭 셸(MainActivity). 웹은 그 위 창으로 열린다.
        com.muyeon.app.ui.main.MainActivity.launch(context, null)
        if (context is SplashActivity) {
            context.finish()
        }
    }

    override fun navigateToIpConnect() {
        val intent = Intent(context, IpConnectActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        context.startActivity(intent)
    }
}

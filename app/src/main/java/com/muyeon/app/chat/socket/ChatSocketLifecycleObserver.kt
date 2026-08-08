package com.muyeon.app.chat.socket

import android.content.Context
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner

/**
 * 앱 전역 포그라운드/백그라운드 전환에 맞춰 채팅 소켓을 pause/resume.
 *  PaceERA `manager/SocketLifecycleObserver.kt` 와 동일 역할.
 *
 *  [ChatSocketManager] 는 reconnectionAttempts=MAX 라 끊기면 무한 재연결한다.
 *  백그라운드에서 끊어주지 않으면 화면이 꺼져도 재연결 시도가 계속돼 로그 폭주 + 배터리/데이터 낭비.
 *  백그라운드 알림은 FCM 이 담당하므로(게이트웨이가 방 미접속자에게 푸시 발송) 단절해도 안전하다.
 *
 *  등록: 채팅 화면 진입 시 [attach] 1회(앱 클래스가 없어 최초 사용처에서 등록).
 */
class ChatSocketLifecycleObserver(private val appContext: Context) : DefaultLifecycleObserver {

    fun attach() {
        if (attached) return
        attached = true
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        Log.i(TAG, "attach — ProcessLifecycleOwner ON_START/ON_STOP 옵저버 등록")
    }

    override fun onStart(owner: LifecycleOwner) {
        ChatSocketManager.resumeFromBackground(appContext)
    }

    override fun onStop(owner: LifecycleOwner) {
        ChatSocketManager.pauseForBackground()
    }

    companion object {
        private const val TAG = "ChatSocketLifecycle"

        @Volatile
        private var attached = false

        /** 어디서 호출해도 1회만 등록된다. */
        fun ensureAttached(context: Context) {
            if (attached) return
            ChatSocketLifecycleObserver(context.applicationContext).attach()
        }
    }
}

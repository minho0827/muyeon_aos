package com.muyeon.app.auth

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner

/** 앱이 앞으로 나올 때마다(콜드 스타트·설정 앱에서 돌아옴 포함) 기기 상태를 맞춘다. MuyeonApplication 에서 1회. */
class DeviceSyncLifecycleObserver(private val deviceSync: DeviceSync) : DefaultLifecycleObserver {
    override fun onStart(owner: LifecycleOwner) = deviceSync.sync()

    companion object {
        fun install(deviceSync: DeviceSync) {
            ProcessLifecycleOwner.get().lifecycle.addObserver(DeviceSyncLifecycleObserver(deviceSync))
        }
    }
}

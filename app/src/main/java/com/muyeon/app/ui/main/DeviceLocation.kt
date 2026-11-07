package com.muyeon.app.ui.main

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.muyeon.domain.model.region.Coordinates
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

/**
 * 대표 지역 처음 정하기용 좌표 — 이미 받은 위치 권한이 있을 때만 마지막 위치를 쓴다(여기서 권한을 묻지 않는다).
 *   웹 RegionProvider 는 같은 일을 앱 브리지 getLocation 으로 한다.
 */
object DeviceLocation {
    suspend fun lastKnown(context: Context): Coordinates? {
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!fine && !coarse) return null
        return withTimeoutOrNull(TIMEOUT_MS) {
            suspendCancellableCoroutine { cont ->
                runCatching {
                    LocationServices.getFusedLocationProviderClient(context).lastLocation
                        .addOnSuccessListener { l -> cont.resume(l?.let { Coordinates(it.latitude, it.longitude) }) }
                        .addOnFailureListener { cont.resume(null) }
                }.onFailure { cont.resume(null) }
            }
        }
    }

    private const val TIMEOUT_MS = 5_000L
}

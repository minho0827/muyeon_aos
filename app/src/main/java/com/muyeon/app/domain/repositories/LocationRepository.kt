package com.muyeon.app.domain.repositories

import com.muyeon.app.domain.models.location.LocationResult

fun interface LocationRepository {
    fun requestLocation(callback: (LocationResult) -> Unit)
}
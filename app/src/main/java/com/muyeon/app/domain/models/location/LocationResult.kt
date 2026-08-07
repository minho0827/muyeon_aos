package com.muyeon.app.domain.models.location

data class LocationResult(
    val isLoading: Boolean = false,
    val latitude: Double? = 0.0,
    val longitude: Double? = 0.0,
    val status: String = "",
    val message: String? = null
)
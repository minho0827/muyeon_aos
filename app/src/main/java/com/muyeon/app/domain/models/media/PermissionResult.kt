package com.muyeon.app.domain.models.media

@Suppress("unused")
data class PermissionResult(
    val type: PermissionType,
    val granted: Boolean
)
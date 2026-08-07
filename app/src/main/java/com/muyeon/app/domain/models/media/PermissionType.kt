package com.muyeon.app.domain.models.media

sealed class PermissionType {
    data object Camera : PermissionType()
    data object Gallery : PermissionType()
}
package com.muyeon.app.domain.models.location

enum class PermissionResult {
    GRANTED_PERMANENTLY,
    GRANTED_ONCE,
    DENIED_WITH_RATIONALE,
    DENIED_PERMANENTLY,
    ERROR
}
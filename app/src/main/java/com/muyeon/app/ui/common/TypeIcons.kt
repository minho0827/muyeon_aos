package com.muyeon.app.ui.common

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.TheaterComedy
import androidx.compose.ui.graphics.vector.ImageVector
import com.muyeon.domain.model.member.ActiveTypeInfos

/** ★ 2026-11-16 유형 아이콘 — 유형 정보 표(ActiveTypeInfos)의 아이콘 열쇠를 그림으로 바꾸는 단 한 곳(탭바 동그라미·유형 시트). */
object TypeIcons {
    @Suppress("DEPRECATION")
    fun of(type: String?): ImageVector = when (ActiveTypeInfos.of(type).iconKey) {
        "school" -> Icons.Filled.School
        "dancer" -> Icons.Filled.DirectionsRun
        "academy" -> Icons.Filled.Apartment
        "team" -> Icons.Filled.TheaterComedy
        else -> Icons.Filled.Person
    }
}

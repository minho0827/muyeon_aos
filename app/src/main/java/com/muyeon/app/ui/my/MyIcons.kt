package com.muyeon.app.ui.my

import androidx.annotation.DrawableRes
import com.muyeon.app.R

/** 웹 아이콘 이름(_icons.scss — 서버 메뉴 icon 값) → 색 그대로 옮긴 그림. 모르는 이름은 myinfo. */
object MyIcons {
    @DrawableRes
    fun of(name: String?): Int = when (name) {
        "filled-note" -> R.drawable.ic_menu_note_filled
        "filled-paper" -> R.drawable.ic_menu_paper_filled
        "filled-building" -> R.drawable.ic_menu_building_filled
        "filled-star" -> R.drawable.ic_menu_star_filled
        "filled-community" -> R.drawable.ic_menu_community_filled
        "event" -> R.drawable.ic_menu_event
        "notice" -> R.drawable.ic_menu_notice
        "faq" -> R.drawable.ic_menu_faq
        "ask" -> R.drawable.ic_menu_ask
        "edit" -> R.drawable.ic_menu_edit
        "camera" -> R.drawable.ic_menu_camera
        else -> R.drawable.ic_menu_myinfo
    }
}

package com.muyeon.app.ui.lessons

import androidx.annotation.StringRes
import com.muyeon.app.R

/** 강사 카드 코드 → 이름(웹 constants/jobOptions careerLabel · subOptions fieldLabel). 모르는 분야 코드는 그대로. */
object LessonLabels {
    @StringRes
    fun career(code: String?): Int? = when (code) {
        "ANY" -> R.string.career_any
        "NEW" -> R.string.career_new
        "Y1_3" -> R.string.career_y1_3
        "Y3_5" -> R.string.career_y3_5
        "Y5_10" -> R.string.career_y5_10
        "Y10" -> R.string.career_y10
        else -> null
    }

    @StringRes
    fun field(code: String?): Int? = when (code) {
        "BASIC" -> R.string.field_basic
        "HOBBY" -> R.string.field_hobby
        "MAJOR" -> R.string.field_major
        "EXAM" -> R.string.field_exam
        "REPERTOIRE" -> R.string.field_repertoire
        "CONCOURS" -> R.string.field_concours
        "AUDITION" -> R.string.field_audition
        "PILATES" -> R.string.field_pilates
        "STRETCH" -> R.string.field_stretch
        "OTHER" -> R.string.field_other
        // 옛 코드(이전에 저장된 프로필) — 웹 LEGACY_FIELD_MAP
        "BALLET_KIDS" -> R.string.field_legacy_kids
        "BALLET_ELEM" -> R.string.field_legacy_elem
        "BALLET_TEEN" -> R.string.field_legacy_teen
        "BALLET_MAJOR" -> R.string.field_major
        "BALLET_EXAM" -> R.string.field_exam
        "BALLET_ADULT" -> R.string.field_legacy_adult
        "KOREAN" -> R.string.field_legacy_korean
        "MODERN" -> R.string.field_legacy_modern
        "PRACTICAL" -> R.string.field_legacy_practical
        "BALLET_FIT" -> R.string.field_legacy_balletfit
        "BARRE" -> R.string.field_legacy_barre
        else -> null
    }
}

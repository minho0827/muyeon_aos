package com.muyeon.app.ui.academyedit

import com.muyeon.app.ui.resume.ResumeOptions
import com.muyeon.app.ui.space.SpaceFacility

/**
 * 학원 정보 관리 고르기 목록 — 웹과 같은 값·같은 순서(서버로 그대로 보내는 값이라 문구 파일이 아니라 여기 둔다).
 *   장르 = 웹 mock/muyeonMock GENRES · 분야 = constants/subOptions TEACHING_FIELDS(ResumeOptions 와 같은 코드) ·
 *   시설 = 웹 FACILITY_KEYS 를 FACILITY_META 순서로(공간 SpaceFacility 와 같은 이름).
 */
object AcademyEditOptions {
    val genres: List<Pair<String, String>> = listOf("발레", "한국무용", "현대무용", "실용무용", "뮤지컬", "바레", "발레핏").map { it to it }
    val fields: List<Pair<String, String>> = ResumeOptions.teachingFields
    private val facilityKeys = setOf("parking", "changingRoom", "fullMirror", "woodFloor", "springFloor", "danceFloor", "sound", "shower", "restroom", "wifi")
    val facilities: List<Pair<String, String>> = SpaceFacility.ALL.filter { it.key in facilityKeys }.map { it.key to it.label }
}

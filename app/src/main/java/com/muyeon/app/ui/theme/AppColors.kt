package com.muyeon.app.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * 새 네이티브 탭 화면의 색 단일 출처(SSOT) — 화면 코드에 `Color(0x…)` 직접 금지.
 *   값은 웹 CSS 와 같다(웹 디자인 그대로 옮기기). 웹에 다크 모드가 없어 라이트 한 벌이다.
 *
 *   ⚠️ 기존 화면이 쓰는 `ui/common/MuyeonColors`(iOS 1:1)와 따로 있다. 새 탭 화면은 이 객체만 쓴다.
 *   ⚠️ 이름이 같은 기존 `com.muyeon.app.theme.AppTypography` 가 있다 — import 는 `com.muyeon.app.ui.theme.*`.
 */
object AppColors {
    // === Brand ===
    val Primary: Color = Palette.Primary500
    val PrimaryDeep: Color = Palette.Primary550
    val PrimarySoft: Color = Palette.Primary50
    /** 웹 $pr-06 — 주황 6% 바탕. */
    val PrimaryTint: Color = Palette.Primary500.copy(alpha = 0.06f)

    // === Text ===
    val TextHead: Color = Palette.Gray900
    val TextBody: Color = Palette.Gray800
    val TextSub: Color = Palette.Gray600
    val TextHint: Color = Palette.Gray500
    val TextDisabled: Color = Palette.Gray400
    val TextOnPrimary: Color = Palette.White

    // === Surface ===
    val Surface: Color = Palette.White
    val SurfaceMuted: Color = Palette.Gray50
    val Placeholder: Color = Palette.Gray100

    // === Line ===
    val Divider: Color = Palette.Gray100
    /** 웹 $divider-dash — 탭바 위 선. */
    val DividerStrong: Color = Palette.Gray150
    val ChipBorder: Color = Palette.Gray200
    /** 웹 .btn-md.outlined 테두리($btn-outlined-border = gray-500). */
    val ButtonOutline: Color = Palette.Gray500

    // === Icon ===
    /** 탭바 꺼진 아이콘(웹 $background-ico-g300). */
    val IconOff: Color = Palette.Gray300
    val HeartOff: Color = Color(0xFFC4C4C4)

    // === Status ===
    val Badge: Color = Palette.BadgeRed
    val Danger: Color = Palette.Red
    val Positive: Color = Palette.Green
    val Warning: Color = Palette.Yellow500
    /** 새 항목 점(받은 지원 '새 지원' — 웹 #0A84FF). */
    val NewDot: Color = Color(0xFF0A84FF)

    /** 칩(선택) — 웹 _tab.scss: 선택 칩은 검정 바탕 흰 글자. */
    val ChipSelectedBg: Color = Palette.Gray900
    val ChipText: Color = Palette.Gray800

    /** 사진 위 덮개(하트 원 92% 흰색). */
    val OverlayWhite: Color = Palette.White.copy(alpha = 0.92f)

    // 탭바(2026-11-08, 토스 방식 — 1계층 붙은 바 · 2계층 떠 있는 캡슐) — 면 · 테두리.
    val TabBarGlass: Color = Palette.White
    val TabBarBorder: Color = Palette.Gray150
    val Scrim: Color = Palette.Black.copy(alpha = 0.5f)
}

/** 배지·알약(pill)의 바탕·글자 한 쌍. */
data class PillColors(val bg: Color, val fg: Color)

/**
 * 웹에서 화면마다 정의한 분류 색(Home.js GENRE_COLOR·TYPE_COLOR, TeacherHome TYPE, _chips.scss level).
 *   한 곳에 모은다 — 웹은 같은 값이 파일마다 복사돼 있다.
 */
object CategoryColors {
    val Default = PillColors(Color(0xFFEFEFEF), Color(0xFF6D6E71))

    private val genre = mapOf(
        "발레" to PillColors(Color(0xFFF4ECE2), Color(0xFFA9793F)),
        "한국무용" to PillColors(Color(0xFFE7F4EA), Color(0xFF2A7A43)),  // ★ 2026-11-09 글자 진하게(대비 3.0→4.7, 잘 안 보인다는 사용자 제보)
        "현대무용" to PillColors(Color(0xFFEEE8FB), Color(0xFF7B5CC4)),
        "실용무용" to PillColors(Color(0xFFFDE9DA), Color(0xFFB04E0E)),  // ★ 2026-11-09 글자 진하게(대비 2.6→4.5)
        "바레" to PillColors(Color(0xFFE6F0FD), Color(0xFF3B7BD4)),
        "발레핏" to PillColors(Color(0xFFFDE6E6), Color(0xFFD8564D)),
    )
    private val lessonType = mapOf(
        "체험" to PillColors(Color(0xFFEEE8FB), Color(0xFF7B5CC4)),
        "원데이" to PillColors(Color(0xFFFDE6E6), Color(0xFFD8564D)),
    )

    /** 장르 알약 — 표에 없는 장르(뮤지컬 등)는 회색(웹과 같음). */
    fun genre(name: String?): PillColors = genre[name] ?: Default

    /** 레슨 카드 배지 — 유형(체험·원데이) 먼저, 아니면 장르(웹 tagColor). */
    fun lessonTag(label: String?, genreName: String?): PillColors = lessonType[label] ?: genre(genreName)

    /** 추천(부스트) 알약 — 웹 #ffe8e6 / #e2362d. */
    val Boosted = PillColors(Color(0xFFFFE8E6), Color(0xFFE2362D))
    /** 홈 구인 카드 종류(채용·대타) — 웹 #FDE9DA / #E0752A. */
    val Recruit = PillColors(Color(0xFFFDE9DA), Color(0xFFE0752A))
    /** 강사 홈 카드 종류(TeacherHome TYPE). */
    val FeedJob = PillColors(Color(0xFFFDE9DA), Color(0xFFE0752A))
    val FeedSub = PillColors(Color(0xFFFFE8E6), Color(0xFFD8564D))
    val FeedCasting = PillColors(Color(0xFFEEE8FB), Color(0xFF7B5CC4))
    /** 강사 카드 태그(CardTags) — 웹 #fdeee9 / #d97a5a. */
    val CardTag = PillColors(Color(0xFFFDEEE9), Color(0xFFD97A5A))
    /** DM(레슨 문의) 동그라미 — 웹 #FDEBDD 바탕 · #F58232 선. */
    val Dm = PillColors(Color(0xFFFDEBDD), Palette.Primary500)
    /** 홈 하단 '커뮤니티' 제목 아이콘(웹 $background-ico-blue 근사). */
    val CommunityIcon: Color = Color(0xFF5A6CD8)
    /** 강사 홈 '신규' 표시. */
    val NewLabel: Color = Palette.Green

    /**
     * 웹 _chips.scss `.tag-group .level` — 흰 글자 배지. high 주황 · very-high 빨강 · middle 노랑 · row 초록 · end 회색.
     */
    enum class Level(val bg: Color) {
        High(Palette.Primary500), VeryHigh(Palette.Red), Middle(Palette.Yellow500), Row(Palette.Green), End(Palette.Gray400)
    }
}

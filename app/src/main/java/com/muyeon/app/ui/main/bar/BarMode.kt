package com.muyeon.app.ui.main.bar

/**
 * ★ 2026-11-08 탭바 모드 — PaceERA BarMode(토스 증권 방식)와 같은 생각. 채용 칸을 누르면 탭바가 채용 2계층으로 바뀐다.
 *   MAIN : [홈][공연][채용][공간][MY]   ★ 2026-11-09 채용을 공연 다음으로(사용자 결정).
 *   JOBS : [←][채용][대타공고]   — 채용 화면 안 위쪽 탭이 구인·구직 · ★ 2026-11-09 캐스팅은 공연 칸 안(공연·캐스팅 통합 탭)
 *   공간(대관·양도양수)은 2계층 없이 화면 안 위쪽 탭. 커뮤니티는 MY 안 섹션(사용자 결정 2026-11-08).
 *   모드는 따로 들고 있지 않고 **지금 탭에서 계산한다**(MainTab.mode) — 링크·뒤로가기가 와도 화면과 어긋나지 않는다.
 */
// ★ 11-10 JOBS 도 5칸 기준 — [←][채용][대타공고] 를 왼쪽부터 붙여 놓는다(3칸 나눔은 너무 떨어져 보였다, 사용자 결정).
enum class BarMode(val slotCount: Int) { MAIN(5), JOBS(5) }

/**
 * 탭바의 모든 칸. [slots] 각 모드에서 앉는 칸 번호(없으면 그 모드에서 안 보인다).
 *   채용 칸(JOBS)은 두 모드에 다 있어 3번째 칸 → 2번째 칸으로 미끄러진다.
 */
enum class BarItem(private val slots: Map<BarMode, Int>) {
    HOME(mapOf(BarMode.MAIN to 0)),
    PERFORMANCE(mapOf(BarMode.MAIN to 1)),
    JOBS(mapOf(BarMode.MAIN to 2, BarMode.JOBS to 1)),
    SPACE(mapOf(BarMode.MAIN to 3)),
    MY(mapOf(BarMode.MAIN to 4)),
    BACK(mapOf(BarMode.JOBS to 0)),
    SUBS(mapOf(BarMode.JOBS to 2));

    fun visibleIn(mode: BarMode) = mode in slots

    /** 칸 가운데 위치(탭바 폭 대비 0~1). 그 모드에 없으면 있던 모드의 자리 — 그 자리에서 사라지고 나타난다. */
    fun centerIn(mode: BarMode): Float {
        val m = if (visibleIn(mode)) mode else slots.keys.first()
        return (slots.getValue(m) + 0.5f) / m.slotCount
    }

    /** 나타날 때 차례(ms) — 왼쪽부터 30ms 간격(PaceERA 60ms 를 반으로 — 사용자 요청 '더 빠르게'). 채용 칸은 바로 출발. */
    fun enterDelayMs(mode: BarMode): Int = if (this == JOBS) 0 else (slots[mode] ?: 0) * 30 + 30
}

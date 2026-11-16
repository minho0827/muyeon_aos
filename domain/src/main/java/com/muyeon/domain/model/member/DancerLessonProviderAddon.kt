package com.muyeon.domain.model.member

/**
 * ★ 2026-11-16 무용수 레슨 제공(추가 기능) — 무용수 유형도 강사와 똑같이 레슨 요청을 받고 레슨을 운영한다.
 *
 * 켜고 끄는 곳: 서버가 `lessonProviderTypes` 에 "DANCER" 를 넣으면 켜지고, 빼면 앱은 예전처럼 무용수를 막는다.
 *   앱 쪽 판정은 모두 [LessonProviders] 를 읽으므로 무용수 전용 `if` 분기는 따로 없다.
 *
 * 앱에서 이 기능 때문에 생긴 곳(빼려면 이 목록을 되돌린다):
 *   1) [LessonProviders] 명부 + ActiveRole(앱) 의 명부 저장·불러오기, /auth/me·/app-gate 에서 명부 갱신
 *   2) 무용수 상세(DancerDetailScreen)의 레슨 버튼 — 서버 `lessonProvider` 값이 true 일 때만 보인다
 *   3) 레슨 탭 강사 목록에서 role=DANCER 카드 → 무용수 상세로 열기
 *   4) 이력서 수정의 레슨 항목 — [LessonProviders.showsLessonFields]
 *   5) 레슨 요청 흐름 공용 함수(LessonRequestLauncher) — 강사 프로필·이력서·무용수 상세가 같이 쓴다(빼도 그대로 둬도 된다)
 *
 * 빼는 방법: 서버에서 DANCER 를 빼는 것만으로 충분하다. 코드까지 지우려면 2)·3)·4) 를 걷어내고
 *   이 파일을 삭제한다. 고객 쪽(레슨 요청·예약·후기)은 2026-11-16부터 어느 유형이든 막지 않는다(이 기능과 무관).
 */
object DancerLessonProviderAddon {
    /** 지금 무용수 레슨 제공이 켜져 있나(서버 명부 기준). */
    val enabled: Boolean get() = LessonProviders.canProvide(ActiveType.DANCER)
}

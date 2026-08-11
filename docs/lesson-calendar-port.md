# 레슨 캘린더 iOS → AOS 1:1 이식

작성 2026-08-12. 원본 `~/Muyeon/Muyeon/Screens/Lesson/LessonCalendarView.swift`(800줄) +
`LessonCalendarViewModel.swift`(268줄).

## 1. 왜 화살표만 얹으면 안 되는가

AOS는 월이동·필터·요일헤더·그리드·아젠다가 **전부 하나의 `verticalScroll` Column** 안에 있다.
iOS는 상단(달력)이 고정이고 하단 아젠다만 독립 스크롤이다. 접기가 의미를 가지려면
**레이아웃 분리가 선행**되어야 한다 — 안 그러면 접어도 빈 공간만 생기고 목록이 늘지 않는다.

## 2. iOS 동작 명세(실측)

### 접기/펼치기 — 진입점 3개
- `chevron`(315행): 그리드 아래 전체 폭 버튼. 아이콘 `chevron.up`/`chevron.down`, `toggleCalendar()`, easeInOut 0.2s
- `chevronPull`(600행): 화살표 줄 드래그. `simultaneousGesture` 로 탭과 병행. **떼기 전 실시간** 판정 — 아래 25pt 초과 → 펼침 / 위 25pt 초과 → 접힘. 스프링 0.35s
- `agendaLift`(613행): 펼침 상태에서 아젠다를 위로 25pt 끌면 접힘

### 접힘 상태가 화면을 바꾼다
- 펼침: 요일헤더+그리드 표시, 아젠다 **스크롤 OFF**(`scrollDisabled`) → 위로 끄는 동작이 접기로 잡힘
- 접힘: 그리드 숨김, **상태 필터 칩**(전체/예정/조율 중/취소) 표시, 아젠다 스크롤 ON
- 다시 펼치면 필터를 '전체'로 초기화(131행) — 필터가 남으면 "일정이 사라졌다" 오해
- `init(startCollapsed:)` — '일정 확정 필요' 진입 시 접힌 채 시작

### 아젠다 구조
- `selectedYmd != nil` → 그 날 제목 + **[전체 보기]** + 그날 카드(취소 포함)
- `selectedYmd == nil` → **조율 중 섹션**(pending) + 월별 섹션(표시월~일정 있는 마지막 달, 빈 달은 "일정이 없습니다")
- 상태 탭 필터: 예정=SCHEDULED / 취소=CANCELED / 조율=pending

### 카드(`lessonCard` 413행)
상대 라인(아바타20+강사/고객명) → 날짜 20pt Bold(+취소 취소선·"취소됨"·"완료"·"신규 예약" 배지)
→ 3pt 컬러바 + `serviceLabel · place` + 시간/상태 문구 → (미니맵) → 액션(일정 수정/정하기 · 채팅).
신규/딥링크 건은 주황 테두리 + 배경 틴트.

### 부가 상태
- `newBookingIds`: 지난 방문 이후 생성된 BOOKING 에 '신규 예약' 배지. 기준선은 세션 내 고정, 저장값은 즉시 전진
- `seenPendingIds`: 상세를 열어본 pending 은 상태 탭의 파란 점에서 제외

## 3. AOS 이식 매핑

- `calendarExpanded` / `toggleCalendar()` / `setCalendarExpanded()` → State 에 동일 추가
- `simultaneousGesture` → `Modifier.pointerInput { detectVerticalDragGestures }` (px → dp 변환 필요)
- `scrollDisabled(expanded)` → `verticalScroll(state, enabled = !expanded)`
- `UserDefaults` → `SharedPreferences`(생성자 주입)
- 애니메이션 → `animateContentSize` / `AnimatedVisibility`

## 4. 이식하지 않는 것(원본에 있으나 AOS 선행 조건 부족)

- **월간/리스트 뷰 전환**(상단 메뉴): 리스트 전용 바디 4종이 딸린 별개 화면 규모. 접힘 상태의 상태 필터로 상당 부분 대체됨
- **날짜 재탭 → 그날 시트**(`LessonCalendarDaySheet`): 개인 일정 편집기가 미이식. AOS는 재탭 = 선택 해제(iOS `selectDay` 토글과 동일)
- **개인 일정 추가(+)** / **캘린더 관리**: 각각 편집기·관리 화면 미이식(관리는 기존대로 웹 폴백)
- **딥링크 펄스/자동 스크롤**: 캘린더 딥링크 라우트가 AOS에 없음
- **미니맵**(LocationMapPreview): AOS 지도 프리뷰 컴포넌트 미이식

## 5. 의도적 차이

- 카드의 **[일정 정하기/수정]** → iOS는 전용 편집 시트, AOS는 **상세 화면**으로 이동.
  AOS `LessonDetailScreen` 이 이미 일정 확정·취소를 담당하므로 죽은 버튼을 만들지 않는 선택
  (CLAUDE.md "무반응 금지").
- 배지 저장 키에 계정 id 를 붙이지 않음 — AOS 는 계정 id 를 로컬에 보관하지 않는다.
  계정 전환 시 배지 상태가 남을 수 있음(단말 1계정 전제).
- `selectedYmd` 기본값을 오늘 → **null** 로 변경(iOS 와 동일). 첫 진입이 월 아젠다가 된다.

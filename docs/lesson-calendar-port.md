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

## 4. 2차 이식(2026-08-12) — 관리 화면 + 리스트 모드

### 내 캘린더(관리) — `CalendarManageScreen.kt`
iOS `CalendarManageView` + `CalendarEditSheet` 1:1.
- 목록: [기본](편집 불가 안내 행) + 내 캘린더(48dp 색 커버 + 프리셋 아이콘, 탭 → 편집) + 점선 카드 [새로운 캘린더 만들기]
- 편집기: 만들기에서만 프리셋 6종(2열) → 이름 → 12색 팔레트(6열, 선택 시 체크) → (편집일 때) 캘린더 삭제
- 삭제 확인 문구까지 동일: "이 캘린더의 일정은 '기본'으로 이동합니다. 일정이 삭제되지는 않아요."
- ⚠️ iOS 는 시트를 겹쳐 띄우지만 AOS 는 **같은 화면의 상태 전환**으로 처리(백 스택 단순화). 뒤로가기는 편집기 → 목록 → 종료.
- ⚠️ 점선 테두리는 Compose 기본 제공이 없어 `drawBehind` + `PathEffect.dashPathEffect` 로 직접 그림.
- ⚠️ SF Symbol → Material 아이콘 매핑(`presetIcon`): figure.dance→SelfImprovement, graduationcap→School,
  figure.and.child→ChildCare, trophy→EmojiEvents, person→Person, building.2→Apartment, 기본 CalendarMonth.
- API(`UserCalendarApi`)는 이미 1:1 이식돼 있어 그대로 사용(`/studio/calendars` CRUD).

### 월간 ↔ 리스트 전환
- iOS 는 상단 드롭다운 메뉴, AOS 는 **상단 우측 아이콘 토글**(현재 모드 아이콘 표시 — iOS 와 동일 규칙)
- 리스트 모드: 달력·월이동 없이 상태 탭 + 목록만(iOS `viewMode == .list` 와 동일 구성)
- 리스트 바디 4종 그대로: 전체(조율 중 최대 2건 + "N건 모두 보기 →" → 조율 탭 전환, 이후 날짜별 예정) /
  예정(오늘 이후) / 조율(전량) / 취소(날짜 내림차순)
- 상태 탭 초기화는 **월간 모드에서만** — 리스트는 탭이 상시 노출이라 유지(iOS 131행 조건 동일)
- 카드는 월간·리스트가 같은 `LessonCard` 를 공유(iOS `lessonCardPublic` 과 동일 구조)

## 5. 여전히 이식하지 않은 것

- **날짜 재탭 → 그날 시트**(`LessonCalendarDaySheet`): 개인 일정 편집기가 미이식. AOS는 재탭 = 선택 해제(iOS `selectDay` 토글과 동일)
- **개인 일정 추가(+)**: 타임트리식 편집기 미이식
- **딥링크 펄스/자동 스크롤**: 캘린더 딥링크 라우트가 AOS에 없음
- **미니맵**(LocationMapPreview): AOS 지도 프리뷰 컴포넌트 미이식
- **캘린더 픽커 시트**(`CalendarPickerSheet`): 일정 확정 폼 전용 — 해당 폼이 미이식

## 6. 의도적 차이

- 카드의 **[일정 정하기/수정]** → iOS는 전용 편집 시트, AOS는 **상세 화면**으로 이동.
  AOS `LessonDetailScreen` 이 이미 일정 확정·취소를 담당하므로 죽은 버튼을 만들지 않는 선택
  (CLAUDE.md "무반응 금지").
- 배지 저장 키에 계정 id 를 붙이지 않음 — AOS 는 계정 id 를 로컬에 보관하지 않는다.
  계정 전환 시 배지 상태가 남을 수 있음(단말 1계정 전제).
- `selectedYmd` 기본값을 오늘 → **null** 로 변경(iOS 와 동일). 첫 진입이 월 아젠다가 된다.

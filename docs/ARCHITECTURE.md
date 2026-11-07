# 무용연 Android — 탭 네이티브 전환 구조 규칙

> 2026-11-07 시작(브랜치 `feature/native-tabs`). 구조는 PaceERA(`/Users/minhoyoo/PaceERA/AGENTS.md`)를 따른다.
> **새 코드(탭 셸·탭 화면·그 아래 상세)는 이 문서 규칙을 지킨다.** 기존 화면(견적·채팅·레슨 …)은 손대지 않고 차차 옮긴다.

---

## 0. 가장 중요한 3가지

1. **경로는 한 곳에만** — 서버 경로 `ApiEndpoints`, 화면 경로 `navigation/Screen.kt`, 탭 종류 `ui/main/MainTab.kt`
2. **하드코딩 금지** — 문구는 `strings.xml`, 색은 `ui/theme/AppColors`·`CategoryColors`, 글자는 `AppTypography`, 크기는 `Dimensions`
3. **새 기능은 9단계 체크리스트(§6)로 빠짐없이**

---

## 1. 앱 구조

```
SplashActivity ─▶ MainActivity (앱 루트: 하단 탭바 + 앱 탭 화면)
                     │  AppNavHost ─ Screen.Main ─ MainScreen(탭바 + 안쪽 NavHost: 홈·강사·커뮤니티·공연·MY)
                     │            └ 탭 위 상세(커뮤니티 글 상세·공연 상세 …, 탭바 없음)
                     ├─▶ WebViewActivity (웹 페이지 창 — 상세·글쓰기·로그인·구인구직/대타 탭 …)
                     └─▶ 기존 네이티브 화면(견적·채팅·프로필 … NativeActionRouter 표)
```

- **웹 창에서 앱 탭 주소**(`/home` `/lessons` `/community` `/performance` `/mypage`)로 가면 그 창을 닫고 앱 탭으로 돌아온다
  (`WebViewActivity.onWebRoute` → `MainActivity.openTab`). 주소가 잠깐(250ms) 머문 뒤에만 — 웹 `__nativeGo` 의 홈 깔기 때문.
- **웹 탭**(구인구직·대타 — 아직 웹)은 웹 창으로 열고, 그 창 아래에 **같은 탭바**(`MainTabBarHost`)를 그린다.
  웹 Footer 는 `__muyeonNativeCapabilities.nativeTabBar` 를 보면 그리지 않는다(muyeon-front Footer.js).
- **네이티브 화면 → 웹 복귀**(`NativeWebRoute.openWebAndFinish`)·푸시·링크·AI 도우미는 모두 `MainActivity.openPath` 로 간다:
  앱 탭 주소면 탭, 아니면 웹 창.
- 네이티브 화면을 띄울 때는 기존과 같이 `NativeActionRouter.open(activity, action, data)` — 액션 이름은 iOS·웹과 같은 계약이다.

## 2. 모듈 (의존: `:app → :data → :network → :domain`)

| 모듈 | 담는 것 | 모르는 것 |
|---|---|---|
| `:domain` | 모델 · 저장소 **인터페이스** · 유스케이스 · `ApiResult` · `UserFacingError` · `CursorPaging` · `TokenStore`/`ActiveTypeStore`/`ForceLogoutTrigger` | Android·Retrofit·Moshi |
| `:network` | `ApiEndpoints`(SSOT) · Retrofit `*Api` · DTO(+`toDomain()`) · 인터셉터(토큰·X-Active-Type·X-Muyeon-Client·강제 로그아웃) | 화면 |
| `:data` | 저장소 구현(`safeApiCall`) · 로컬 캐시 | 화면 |
| `:app` | ViewModel · 화면 · 테마 · 탭 셸 · 기존 화면 | — |

- Koin: `MuyeonApplication` 이 `networkModules() + dataModule + domainModule + appModule + viewModelModule` 을 띄운다.
- `TokenStore`·`ActiveTypeStore`·`ForceLogoutTrigger` 구현은 `:app/di/AppModule` — **기존 `TokenManager`·`ActiveRole`·`ForceLogout` 를 그대로 쓴다**(웹뷰·기존 화면과 같은 토큰·유형).
- DTO 는 Moshi **코드 생성**(`@JsonClass(generateAdapter = true)`) — 릴리스가 R8 을 켜서 리플렉션 어댑터는 운영에서만 깨질 수 있다.
- 서버가 null 을 보내는 칸은 DTO 에서 전부 `?`.

## 3. 서버 계약 (muyeon-backend, NestJS)

- Base URL `https://muyeon.co.kr/` + `api/…` (dev·prod 같음). 응답은 **봉투 없이 객체 그대로**.
- 오류 본문 `{ statusCode, message(문자열|배열), code? }` → `safeApiCall` 이 4xx+문구는 REJECTED(문구 그대로), 그 밖은 공통 문구.
- 커서 목록: 요청 `cursor`(첫 페이지 생략)+`limit`, 응답 `{ items, hasMore, nextCursor }` → `CursorPage`·`CursorState`·`fetchCursorPages`.
- 모든 요청에 `X-Active-Type`(서버 유형 게이트·메뉴·추천의 근거).

## 4. 화면 규칙 (PaceERA 와 같다)

- `XxxScreen(callbacks, viewModel = koinViewModel())` → **상태 없는 하위 컴포넌트**(`*Section` `*Card` `*Row` `*Chips` `*Sheet` `*Labels`).
- **공개 컴포넌트 하나 = 파일 하나**. 기능 폴더 `ui/<feature>/`, 그 안 `components/`. 여러 기능이 쓰면 `ui/components/`.
- 웹(muyeon-front)이 컴포넌트로 나눈 단위는 **그대로 나눈다**(예: 홈 `Section`·`HomeQuoteRequests`·`HomeNoticeCommunityFooter`).
- ViewModel: `MutableStateFlow<XxxUiState>`(평평한 data class), 한 번만 처리할 일은 `SharedFlow` 이벤트.
  실패를 빈 목록으로 보이지 않는다(`errorMessage` 따로).
- 다른 화면이 닫히며 돌려준 결과 키(`result/ResultKeys`) → `AppResults` → `viewModelScope.collectAppResults(KEY) { refresh() }`.
- 탭 다시 누르기 → `TabReselectEffect(MainTab.X) { … }`(웹 useTabReselect: 다음 칩 + 맨 위로).
- 당겨서 새로고침: 앱 탭 목록·홈에 넣는다(웹엔 없음 — 2026-11-07 사용자 결정).

## 5. 문구·색·크기

- 문구 키 `<기능>_<화면>_<요소>`(예: `performance_list_empty`). 공통은 `common_*`. 탭바는 `tab_*`.
- 색은 웹 `_colors.scss` 를 옮긴 `Palette`(internal) → 역할 이름 `AppColors`. 분류 색(장르·유형·레벨 배지)은 `CategoryColors` 한 곳.
- 크기는 웹 rem 그대로(1rem = 10dp/10sp). 컴포넌트 하나만 쓰는 미세값은 그 파일 안 `private val`.
- ⚠️ 기존 `com.muyeon.app.theme.AppTypography`(T1…)·`ui/common/MuyeonColors` 는 기존 화면용. 새 코드는 `com.muyeon.app.ui.theme.*`.

## 6. 새 기능 9단계 체크리스트

1. `:network/api/ApiEndpoints.kt` — 경로 + 출처(컨트롤러 파일) 주석
2. `:network/dto/<feature>/` — DTO + `toDomain()`
3. `:network/api/<Feature>Api.kt` + `NetworkModule.apiModule` 한 줄
4. `:domain/model/<feature>/` · `:domain/repository/<Feature>Repository.kt`
5. `:data/repository/<Feature>RepositoryImpl.kt`(`safeApiCall`) + `DataModule` 한 줄
6. `:domain/usecase/<feature>/` + `DomainModule` 한 줄
7. `:app/ui/<feature>/<Xxx>ViewModel.kt` + `AppModule.viewModelModule` 한 줄
8. `strings.xml` 문구 · 필요하면 `AppColors`/`CategoryColors`
9. `<Xxx>Screen.kt` + 하위 컴포넌트 · `Screen.kt` 경로 · `AppNavHost`(또는 `MainScreen`) 등록 — 단위 테스트(저장소 변환·유스케이스)

## 7. 변경 이력
| 날짜 | 내용 |
|---|---|
| 2026-11-07 | 모듈 분리·Koin·탭 셸(MainActivity)·웹 창 연결 |

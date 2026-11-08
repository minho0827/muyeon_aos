package com.muyeon.app.navigation

import androidx.compose.runtime.Composable
import kotlinx.coroutines.launch
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.muyeon.app.ui.main.MainScreen
import com.muyeon.app.ui.performance.PerformanceDetailScreen
import com.muyeon.app.ui.community.CommunityPostScreen
import com.muyeon.app.result.findActivity
import com.muyeon.app.ui.main.MainViewModel
import com.muyeon.app.ui.castingstage.StageRoutes
import com.muyeon.app.ui.castingstage.castingStageGraph

/**
 * 앱 셸 바깥 NavHost — 탭 셸([Screen.Main]) + 탭 위 상세(탭바 없음). PaceERA AppNavHost 와 같은 자리.
 *   상세 화면은 각 탭 이식 때 여기 한 줄씩 붙는다(커뮤니티 글 상세 · 공연 상세).
 */
@Composable
fun AppNavHost(
    mainViewModel: MainViewModel,
    onMyLongPress: () -> Unit,
    onOpenWeb: (String) -> Unit,
    onOpenPath: (String) -> Unit,
    navController: NavHostController = rememberNavController(),
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    // 푸시·링크·웹 창에서 온 상세 열기 — 같은 화면(같은 인자)이 이미 맨 위면 다시 쌓지 않는다.
    androidx.compose.runtime.LaunchedEffect(navController) {
        mainViewModel.routes.collect { route ->
            val top = navController.currentBackStackEntry
            val topRoute = top?.destination?.route?.let { pattern ->
                top.arguments?.keySet()?.fold(pattern) { acc, k -> acc.replace("{$k}", top.arguments?.get(k)?.toString().orEmpty()) } ?: pattern
            }
            if (topRoute != route) navController.navigate(route)
        }
    }
    // 플로팅(채팅 버튼) 표시는 웹 주소 기준 — 탭 위 상세에 들어가면 상세 주소를 알려 숨기고, 탭으로 돌아오면 탭 주소.
    androidx.compose.runtime.LaunchedEffect(navController) {
        navController.currentBackStackEntryFlow.collect { entry ->
            val route = entry.destination.route
            if (route == Screen.Main.route) {
                com.muyeon.app.ui.main.MainTab.fromKey(mainViewModel.state.value.selectedKey)
                    ?.let { com.muyeon.app.ui.floating.FloatingState.updateRoute(it.webPath) }
            } else {
                com.muyeon.app.ui.floating.FloatingState.updateRoute("/" + (route ?: "detail"))
            }
        }
    }
    // 화면 전환 — PaceERA AppNavHost 와 같다(들어갈 때·나올 때 모두 페이드 + 가로 밀기 300ms).
    val t = com.muyeon.app.ui.common.nav.MuyeonNav
    NavHost(
        navController, startDestination = Screen.Main.route,
        enterTransition = t.enter, exitTransition = t.exit, popEnterTransition = t.shellPopEnter, popExitTransition = t.shellPopExit,
    ) {
        // 캐스팅·무대 상세·목록·등록(첫 화면은 탭 — 탭에 없는 유형이 링크로 열면 첫 화면도 여기).
        castingStageGraph(navController, StageRoutes.SHELL, includeHome = true, onExit = {},
            onCreated = { AppResults.publish(setOf(com.muyeon.app.result.ResultKeys.JOB_POSTINGS)) })
        composable(Screen.Main.route) {
            MainScreen(
                viewModel = mainViewModel,
                onOpenDetail = { route -> navController.navigate(route) },
                onMyLongPress = onMyLongPress,
                onOpenWeb = onOpenWeb,
                onOpenPath = onOpenPath,
                onNativeAction = { action, data ->
                    context.findActivity()?.let { a ->
                        com.muyeon.app.webview.NativeActionRouter.open(a, action, org.json.JSONObject(data as Map<*, *>))
                    }
                },
            )
        }
        // 공고 상세(보는 사람) — 채용·대타.
        listOf(Screen.JobDetail to "JOB", Screen.SubDetail to "SUB").forEach { (screen, kind) ->
            composable(screen.route, arguments = listOf(navArgument("id") { type = NavType.StringType })) { e ->
                val id = e.arguments?.getString("id")?.toIntOrNull() ?: 0
                PostingDetailRoute(kind, id, onBack = { navController.popBackStack() }, onOpenWeb = onOpenWeb)
            }
        }
        // 대타공고 설문 등록 — 올리면 닫고 대타공고 목록을 다시 받는다.
        composable(Screen.SubWizard.route, arguments = listOf(navArgument(Screen.SubWizard.ARG_PREFILL) { type = NavType.StringType; nullable = true; defaultValue = null })) { e ->
            com.muyeon.app.ui.subwizard.SubWizardScreen(
                prefillId = e.arguments?.getString(Screen.SubWizard.ARG_PREFILL),
                onClose = { navController.popBackStack() },
                onCreated = {
                    AppResults.publish(setOf(com.muyeon.app.result.ResultKeys.JOB_POSTINGS))
                    navController.popBackStack()
                },
            )
        }
        // 커뮤니티 목록 — MY 섹션·웹 /community 에서 연다(탭이 아니다).
        composable(Screen.CommunityList.route) {
            val loggedIn = mainViewModel.state.collectAsState().value.isLoggedIn
            val write = { onOpenWeb(if (loggedIn) WebPaths.COMMUNITY_WRITE else WebPaths.login(WebPaths.COMMUNITY_WRITE)) }
            com.muyeon.app.ui.community.CommunityListScreen(
                onOpenPost = { id -> navController.navigate(Screen.CommunityPost.build(id)) },
                onWrite = write,
                header = {
                    com.muyeon.app.ui.components.MainTopBar(
                        title = androidx.compose.ui.res.stringResource(com.muyeon.app.R.string.tab_community),
                        onBack = { navController.popBackStack() },
                    ) {
                        com.muyeon.app.ui.components.TopBarIconButton(
                            com.muyeon.app.R.drawable.ic_web_edit, androidx.compose.ui.res.stringResource(com.muyeon.app.R.string.community_write), onClick = write,
                        )
                    }
                },
            )
        }
        composable(
            Screen.CommunityPost.route,
            arguments = listOf(navArgument(Screen.CommunityPost.ARG_ID) { type = NavType.StringType }),
        ) {
            CommunityPostScreen(
                onBack = { navController.popBackStack() },
                onLogin = { id -> onOpenWeb(WebPaths.login("/community/post/$id")) },
                onEditPost = { id -> onOpenWeb("/community/post/$id/edit") },
                onOpenTeacher = { userId ->
                    context.findActivity()?.let { a ->
                        com.muyeon.app.webview.NativeActionRouter.open(a, "openPublicProfile", org.json.JSONObject().put("userId", userId.toString()))
                    }
                },
            )
        }
        // ★ 2026-11-08 고객센터 — 공지사항·FAQ(웹 /notice · /faq).
        composable(Screen.Notices.route) {
            com.muyeon.app.ui.support.NoticeListScreen(
                onBack = { navController.popBackStack() },
                onOpen = { id -> navController.navigate(Screen.NoticeDetail.build(id)) },
            )
        }
        composable(Screen.NoticeDetail.route, arguments = listOf(navArgument(Screen.NoticeDetail.ARG_ID) { type = NavType.StringType })) { e ->
            val id = e.arguments?.getString(Screen.NoticeDetail.ARG_ID)?.toLongOrNull() ?: 0L
            com.muyeon.app.ui.support.NoticeDetailScreen(
                viewModel = org.koin.androidx.compose.koinViewModel(parameters = { org.koin.core.parameter.parametersOf(id) }),
                onBack = { navController.popBackStack() },
            )
        }
        composable(Screen.Faq.route) {
            com.muyeon.app.ui.support.FaqScreen(onBack = { navController.popBackStack() }, onInquiry = { navController.navigate(Screen.SupportCompose.build(Screen.SupportCompose.INQUIRY)) })
        }
        // ★ 2026-11-08 1:1 문의·의견(웹 /support/inquiries · /support/inquiry · /support/feedback).
        composable(Screen.SupportInquiries.route) {
            com.muyeon.app.ui.support.InquiryListScreen(
                viewModel = org.koin.androidx.compose.koinViewModel(),
                onBack = { navController.popBackStack() },
                onOpen = { id -> navController.navigate(Screen.SupportInquiryDetail.build(id)) },
                onWrite = { navController.navigate(Screen.SupportCompose.build(Screen.SupportCompose.INQUIRY)) },
            )
        }
        composable(Screen.SupportInquiryDetail.route, arguments = listOf(navArgument(Screen.SupportInquiryDetail.ARG_ID) { type = NavType.StringType })) { e ->
            val id = e.arguments?.getString(Screen.SupportInquiryDetail.ARG_ID)?.toLongOrNull() ?: 0L
            com.muyeon.app.ui.support.InquiryDetailScreen(
                viewModel = org.koin.androidx.compose.koinViewModel(parameters = { org.koin.core.parameter.parametersOf(id) }),
                onBack = { navController.popBackStack() },
                onDeleted = { navController.popBackStack() },
            )
        }
        composable(Screen.SupportCompose.route, arguments = listOf(navArgument(Screen.SupportCompose.ARG_TYPE) { type = NavType.StringType })) { e ->
            val type = if (e.arguments?.getString(Screen.SupportCompose.ARG_TYPE) == Screen.SupportCompose.FEEDBACK)
                com.muyeon.domain.model.support.SupportComposeType.FEEDBACK else com.muyeon.domain.model.support.SupportComposeType.INQUIRY
            com.muyeon.app.ui.support.SupportComposeScreen(
                type = type,
                viewModel = org.koin.androidx.compose.koinViewModel(parameters = { org.koin.core.parameter.parametersOf(type) }),
                onBack = { navController.popBackStack() },
                // 문의 접수 뒤 [문의내역 확인] — 아래가 문의내역이면 거기로 돌아가고(다시 읽는다), 아니면 이 화면 자리에 문의내역.
                onDoneInquiry = {
                    if (navController.previousBackStackEntry?.destination?.route == Screen.SupportInquiries.route) navController.popBackStack()
                    else navController.navigate(Screen.SupportInquiries.route) { popUpTo(Screen.SupportCompose.route) { inclusive = true } }
                },
            )
        }
        // ★ 2026-11-08 찜/관심(웹 /likes) — 누르면 그 상세(앱 화면이면 이 위에 쌓여 뒤로 가면 찜으로 돌아온다).
        composable(Screen.Likes.route) {
            com.muyeon.app.ui.likes.LikesScreen(
                onBack = { navController.popBackStack() },
                onOpenPath = onOpenPath,
                onBrowse = {
                    navController.popBackStack(Screen.Main.route, inclusive = false)
                    onOpenPath("/home")
                },
            )
        }
        // ★ 2026-11-08 내가 예약한 내역(웹 /myReservations) — 예약 상세·후기는 기존 앱 화면, 돌려준 결과로 다시 읽는다.
        composable(Screen.MyReservations.route) {
            val native: (String, org.json.JSONObject) -> Unit = { action, data ->
                context.findActivity()?.let { a -> com.muyeon.app.webview.NativeActionRouter.open(a, action, data) }
            }
            com.muyeon.app.ui.reservation.MyReservationsScreen(
                onBack = { navController.popBackStack() },
                onOpenReservation = { id -> native("openLessonReservationDetail", org.json.JSONObject().put("reservationId", id.toString())) },
                onReview = { r ->
                    val tid = r.teacherId?.toString() ?: return@MyReservationsScreen
                    // 후기를 썼으면 내 후기(전체 후기 화면), 아니면 후기 쓰기(웹과 같다).
                    if (r.reviewed) native("openReviewList", org.json.JSONObject().put("teacherId", tid))
                    else native("openReviewWrite", org.json.JSONObject().put("teacherId", tid).put("teacherName", r.teacherName))
                },
                onBrowseLessons = {
                    navController.popBackStack(Screen.Main.route, inclusive = false)
                    com.muyeon.app.ui.home.HomeModeRequests.request(com.muyeon.app.ui.home.HomeMode.LESSONS)
                    onOpenPath("/home")
                },
            )
        }
        // ★ 2026-11-08 지원 내역 — 내가 지원한 공고(웹 /myApplications) · 받은 지원(웹 /receivedApplications).
        composable(Screen.MyApplications.route) {
            com.muyeon.app.ui.application.MyApplicationsScreen(
                onBack = { navController.popBackStack() },
                onOpenPath = onOpenPath,
                onBrowseJobs = {
                    navController.popBackStack(Screen.Main.route, inclusive = false)
                    onOpenPath("/jobs")
                },
            )
        }
        composable(
            Screen.ReceivedApplications.route,
            arguments = listOf(
                navArgument(Screen.ReceivedApplications.ARG_KIND) { type = NavType.StringType; nullable = true; defaultValue = null },
                navArgument(Screen.ReceivedApplications.ARG_POSTING) { type = NavType.StringType; nullable = true; defaultValue = null },
            ),
        ) { e ->
            val kind = e.arguments?.getString(Screen.ReceivedApplications.ARG_KIND)?.takeIf { it.isNotBlank() }
            val postingId = e.arguments?.getString(Screen.ReceivedApplications.ARG_POSTING)?.toLongOrNull()
            com.muyeon.app.ui.application.ReceivedApplicationsScreen(
                viewModel = org.koin.androidx.compose.koinViewModel(parameters = { org.koin.core.parameter.parametersOf(kind, postingId) }),
                onBack = { navController.popBackStack() },
                onOpenResume = { a ->
                    val pid = a.postingId ?: return@ReceivedApplicationsScreen
                    // 채용·대타·캐스팅 모두 앱 지원자 이력서(캐스팅은 열람·채팅만).
                    context.findActivity()?.let { act ->
                        com.muyeon.app.webview.NativeActionRouter.open(
                            act, "openApplicantResume",
                            org.json.JSONObject().put("applicationKind", a.kind)
                                .put(when (a.kind) { "SUB" -> "subId"; "CASTING" -> "callId"; else -> "jobId" }, pid.toString())
                                .put("applicationId", a.id.toString()),
                        )
                    }
                },
                onManagePostings = { onOpenWeb("/myPostings") },
            )
        }
        // ★ 2026-11-08 설정(웹 /setting) — 로그아웃은 MY 와 같은 길(LogoutUseCase → 셸 onLoggedOut), 차단한 사용자는 앱 화면.
        composable(Screen.Settings.route) {
            com.muyeon.app.ui.settings.SettingsScreen(
                onBack = { navController.popBackStack() },
                onOpenPath = onOpenPath,
                onNotificationSettings = {
                    context.findActivity()?.let { a -> com.muyeon.app.webview.NativeActionRouter.open(a, "openNotificationSettings", org.json.JSONObject()) }
                },
                onBlockedUsers = { navController.navigate(Screen.BlockedUsers.route) },
                onLoggedOut = {
                    navController.popBackStack(Screen.Main.route, inclusive = false)
                    mainViewModel.onLoggedOut()
                },
            )
        }
        composable(Screen.BlockedUsers.route) {
            com.muyeon.app.ui.settings.BlockedUsersScreen(onBack = { navController.popBackStack() })
        }
        // ★ 2026-11-08 무용수 프로필 열람(웹 /dancers · /dancers/:id).
        composable(Screen.Dancers.route) {
            com.muyeon.app.ui.dancer.DancerListScreen(
                onBack = { navController.popBackStack() },
                onOpen = { id -> navController.navigate(Screen.DancerDetail.build(id)) },
                onLogin = { onOpenWeb(WebPaths.login("/dancers")) },
            )
        }
        composable(Screen.DancerDetail.route, arguments = listOf(navArgument(Screen.DancerDetail.ARG_ID) { type = NavType.StringType })) { e ->
            val id = e.arguments?.getString(Screen.DancerDetail.ARG_ID)?.toLongOrNull() ?: 0L
            val native: (String, org.json.JSONObject) -> Unit = { action, data ->
                context.findActivity()?.let { a -> com.muyeon.app.webview.NativeActionRouter.open(a, action, data) }
            }
            com.muyeon.app.ui.dancer.DancerDetailScreen(
                viewModel = org.koin.androidx.compose.koinViewModel(parameters = { org.koin.core.parameter.parametersOf(id) }),
                onBack = { navController.popBackStack() },
                onLogin = { onOpenWeb(WebPaths.login("/dancers/$id")) },
                onOpenImage = { urls, i -> native("openImageViewer", org.json.JSONObject().put("urls", org.json.JSONArray(urls).toString()).put("index", i.toString())) },
                onOpenUrl = { url -> runCatching { context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))) } },
                onReviews = { native("openReviewList", org.json.JSONObject().put("teacherId", id.toString())) },
                onOpenChat = { roomId, title -> native("openChatRoom", org.json.JSONObject().put("roomId", roomId.toString()).put("title", title)) },
                onMembership = { native("openMembership", org.json.JSONObject()) },
            )
        }
        // ★ 2026-11-08 개인정보 수정(웹 /profileEdit) — 회원유형은 셸의 유형 화면(MY 머리와 같은 길).
        composable(Screen.ProfileEdit.route) {
            com.muyeon.app.ui.account.ProfileEditScreen(
                onClose = { navController.popBackStack() },
                onOpenPath = onOpenPath,
                onMemberType = { mainViewModel.onTypePicked(com.muyeon.app.ui.main.MainViewModel.TYPE_MANAGE) },
            )
        }
        // ★ 2026-11-08 경력·자격 확인(웹 /my/credentials) — 본인인증 전이면 개인정보 수정(앱 화면)으로.
        composable(Screen.Credentials.route) {
            com.muyeon.app.ui.credential.CredentialsScreen(
                onBack = { navController.popBackStack() },
                onProfileEdit = { navController.navigate(Screen.ProfileEdit.route) },
            )
        }
        // ★ 2026-11-08 비밀번호 변경 · 대타 신뢰도(웹 /changePassword · /subTrust).
        composable(Screen.ChangePassword.route) { com.muyeon.app.ui.account.ChangePasswordScreen(onClose = { navController.popBackStack() }) }
        // ★ 2026-11-08 회원탈퇴(웹 /withdraw) — 완료 뒤 로그아웃은 설정과 같은 길(셸 onLoggedOut).
        composable(Screen.Withdraw.route) {
            com.muyeon.app.ui.account.WithdrawScreen(
                onClose = { navController.popBackStack() },
                onOpenPath = onOpenPath,
                onLoggedOut = {
                    navController.popBackStack(Screen.Main.route, inclusive = false)
                    mainViewModel.onLoggedOut()
                },
            )
        }
        composable(Screen.LessonGenres.route) { com.muyeon.app.ui.account.LessonGenresScreen(onClose = { navController.popBackStack() }) }
        composable(Screen.SubTrust.route) { com.muyeon.app.ui.settings.SubTrustScreen(onBack = { navController.popBackStack() }) }
        // ★ 2026-11-08 상담 채팅(웹 /support/chat) — 방은 운영자 답장 푸시의 착지점.
        composable(Screen.SupportChatList.route) {
            com.muyeon.app.ui.support.SupportChatListScreen(
                onBack = { navController.popBackStack() },
                onOpenRoom = { id -> navController.navigate(Screen.SupportChatRoom.build(id)) },
                onOpenPath = onOpenPath,
            )
        }
        composable(Screen.SupportChatRoom.route, arguments = listOf(navArgument(Screen.SupportChatRoom.ARG_ROOM) { type = NavType.StringType })) { e ->
            val id = e.arguments?.getString(Screen.SupportChatRoom.ARG_ROOM)?.toLongOrNull() ?: 0L
            com.muyeon.app.ui.support.SupportChatRoomScreen(
                viewModel = org.koin.androidx.compose.koinViewModel(parameters = { org.koin.core.parameter.parametersOf(id) }),
                onBack = { navController.popBackStack() },
                onOpenPath = onOpenPath,
                onMy = { navController.popBackStack(Screen.Main.route, inclusive = false); onOpenPath("/mypage") },
            )
        }
        // ★ 2026-11-08 홈 '전체보기' 레슨 목록(웹 /lessonList).
        composable(
            Screen.LessonList.route,
            arguments = listOf(
                navArgument(Screen.LessonList.ARG_TITLE) { type = NavType.StringType; nullable = true; defaultValue = null },
                navArgument(Screen.LessonList.ARG_TYPE) { type = NavType.StringType; nullable = true; defaultValue = null },
            ),
        ) { e ->
            val title = e.arguments?.getString(Screen.LessonList.ARG_TITLE).orEmpty()
            val type = e.arguments?.getString(Screen.LessonList.ARG_TYPE)?.takeIf { it.isNotBlank() }
            com.muyeon.app.ui.lessonlist.LessonListScreen(
                title = title,
                viewModel = org.koin.androidx.compose.koinViewModel(parameters = { org.koin.core.parameter.parametersOf(type) }),
                onBack = { navController.popBackStack() },
                onOpenLesson = { id ->
                    context.findActivity()?.let { a ->
                        com.muyeon.app.webview.NativeActionRouter.open(a, "openLessonDetail", org.json.JSONObject().put("lessonId", id.toString()))
                    }
                },
                onLogin = { onOpenWeb(WebPaths.login(Screen.LessonList.build(title, type).let { "/$it" })) },
            )
        }
        // ★ 2026-11-08 (b4) 약관 — 버전을 오가면 같은 화면을 새로 쌓는다(웹 Link).
        composable(
            Screen.Policy.route,
            arguments = listOf(
                navArgument(Screen.Policy.ARG_DOC) { type = NavType.StringType; nullable = true; defaultValue = null },
                navArgument(Screen.Policy.ARG_VERSION) { type = NavType.StringType; nullable = true; defaultValue = null },
            ),
        ) { e ->
            val doc = e.arguments?.getString(Screen.Policy.ARG_DOC)?.takeIf { it.isNotBlank() } ?: Screen.Policy.DEFAULT_DOC
            val version = e.arguments?.getString(Screen.Policy.ARG_VERSION)?.takeIf { it.isNotBlank() }
            com.muyeon.app.ui.policy.PolicyScreen(
                version = version,
                onBack = { navController.popBackStack() },
                onOpenVersion = { v -> navController.navigate(Screen.Policy.build(doc, v)) },
                viewModel = org.koin.androidx.compose.koinViewModel(key = "policy:$doc:$version", parameters = { org.koin.core.parameter.parametersOf(doc, version) }),
            )
        }
        // ★ 2026-11-08 (b4) 서비스 소개 — '둘러보기'는 홈 탭(쌓인 화면을 내린다).
        composable(Screen.About.route) {
            com.muyeon.app.ui.about.AboutScreen(
                onBack = { navController.popBackStack() },
                onHome = { navController.popBackStack(Screen.Main.route, inclusive = false); onOpenPath("/home") },
                onOpenPath = onOpenPath,
            )
        }
        // ★ 2026-11-08 (b4) 관심 지역 — [확인]은 MY(웹 navigate('/mypage')), 지역 추가는 고르면 목록으로 돌아온다.
        composable(Screen.MyRegions.route) {
            com.muyeon.app.ui.region.MyRegionsScreen(
                onBack = { navController.popBackStack() },
                onAdd = { navController.navigate(Screen.AddRegion.route) },
                onDone = { navController.popBackStack(Screen.Main.route, inclusive = false); onOpenPath("/mypage") },
            )
        }
        composable(Screen.AddRegion.route) {
            com.muyeon.app.ui.region.AddRegionScreen(
                onBack = { navController.popBackStack() },
                onAdded = { if (!navController.popBackStack(Screen.MyRegions.route, inclusive = false)) { navController.popBackStack(); navController.navigate(Screen.MyRegions.route) } },
            )
        }
        // ★ 2026-11-08 (b4) 관심 조건 알림 — 저장 팝업을 닫으면 MY(웹 navigate('/mypage')).
        composable(Screen.Alerts.route) {
            com.muyeon.app.ui.alerts.AlertSettingsScreen(
                onBack = { navController.popBackStack() },
                onSaved = { navController.popBackStack(Screen.Main.route, inclusive = false); onOpenPath("/mypage") },
            )
        }
        composable(
            Screen.PerformanceDetail.route,
            arguments = listOf(navArgument(Screen.PerformanceDetail.ARG_ID) { type = NavType.StringType }),
        ) {
            PerformanceDetailScreen(
                onBack = { navController.popBackStack() },
                onLogin = { onOpenWeb(WebPaths.login("/performance")) },
            )
        }
        // ★ 2026-11-08 묶음 2 — 학원 정보 관리(저장 뒤 웹처럼 MY) · 캐스팅·공연 자료 업로드(등록된 글은 앱 경로표로).
        composable(
            Screen.AcademyEdit.route,
            arguments = listOf(navArgument(Screen.AcademyEdit.ARG_AI_DRAFT) { type = NavType.StringType; nullable = true; defaultValue = null }),
        ) { e ->
            com.muyeon.app.ui.academyedit.AcademyEditScreen(
                aiDraftId = e.arguments?.getString(Screen.AcademyEdit.ARG_AI_DRAFT)?.takeIf { it.isNotBlank() },
                onBack = { navController.popBackStack() },
                onMy = { navController.popBackStack(Screen.Main.route, inclusive = false); onOpenPath("/mypage") },
            )
        }
        composable(Screen.CastingMaterials.route, arguments = listOf(navArgument(Screen.CastingMaterials.ARG_KIND) { type = NavType.StringType })) { e ->
            val kind = if (e.arguments?.getString(Screen.CastingMaterials.ARG_KIND) == "PERFORMANCE")
                com.muyeon.domain.model.castingmaterial.CastingMaterialKind.PERFORMANCE else com.muyeon.domain.model.castingmaterial.CastingMaterialKind.CASTING
            com.muyeon.app.ui.castingmaterial.CastingMaterialsScreen(
                viewModel = org.koin.androidx.compose.koinViewModel(key = kind.code, parameters = { org.koin.core.parameter.parametersOf(kind) }),
                onBack = { navController.popBackStack() },
                onOpenPath = onOpenPath,
            )
        }
        // ★ 2026-11-08 묶음 3 — 대타공고 수정 · 커뮤니티 글쓰기/수정(AppNavGraphB3).
        b3Graph(navController)
    }
}

/** 공고 상세 + 찜(채용·대타) — 찜 상태는 저장소에서 읽고 바꾼다. */
@Composable
private fun PostingDetailRoute(kind: String, id: Int, onBack: () -> Unit, onOpenWeb: (String) -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val target = if (kind == "SUB") com.muyeon.domain.model.scrap.ScrapTarget.SUB else com.muyeon.domain.model.scrap.ScrapTarget.JOB
    val getLiked = org.koin.compose.koinInject<com.muyeon.domain.usecase.scrap.GetLikedIdsUseCase>()
    val toggle = org.koin.compose.koinInject<com.muyeon.domain.usecase.scrap.ToggleScrapUseCase>()
    var liked by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val login = { onOpenWeb(WebPaths.login("/${if (kind == "SUB") "subs" else "jobs"}/$id")) }
    androidx.compose.runtime.LaunchedEffect(id) { liked = id.toLong() in getLiked(target) }
    com.muyeon.app.ui.jobposting.PostingViewScreen(
        kind = kind, id = id, liked = liked,
        onToggleLike = {
            val was = liked
            scope.launch {
                when (val r = toggle(target, id.toLong(), was)) {
                    is com.muyeon.domain.usecase.scrap.ToggleScrapResult.Done -> liked = r.liked
                    com.muyeon.domain.usecase.scrap.ToggleScrapResult.LoginRequired -> login()
                    is com.muyeon.domain.usecase.scrap.ToggleScrapResult.Failed -> android.widget.Toast.makeText(context, r.message, android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        },
        onBack = onBack,
        onLogin = login,
        onOpenWeb = onOpenWeb,
        onWriteResume = {
            val dancer = com.muyeon.app.webview.ActiveRole.current(context) == "DANCER"
            com.muyeon.app.ui.resume.ResumeActivity.startEdit(context, null, if (dancer) "dancer" else null)
        },
    )
}

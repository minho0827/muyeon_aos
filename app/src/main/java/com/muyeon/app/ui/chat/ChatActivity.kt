package com.muyeon.app.ui.chat

import androidx.lifecycle.viewmodel.compose.viewModel
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.muyeon.app.chat.socket.ChatSocketLifecycleObserver
import com.muyeon.app.chat.socket.ChatSocketManager
import com.muyeon.app.result.OnResultKeys
import com.muyeon.app.result.ResultActivity
import com.muyeon.app.result.ResultKeys
import com.muyeon.app.result.ReturnResultKeys
import com.muyeon.app.result.launchScreen
import com.muyeon.app.utils.TokenManager

/**
 * 채팅 컨테이너 — 웹 `openChatList` / `openChatRoom` 브릿지 진입점.
 *  iOS 는 ChatListView(NavigationView) 안에서 ChatRoomView 를 push 한다. 동일 스택을 NavHost 로.
 *
 *  소켓은 앱 전역 단일([ChatSocketManager]) — 화면이 아니라 프로세스 수명에 맞춘다.
 *  진입 시 connect, 백그라운드 전환은 [ChatSocketLifecycleObserver] 가 pause/resume.
 *
 *  결과: 닫히면 CHAT_ROOMS·CHAT_ROOM(목록 배지·방 상태가 바뀌었을 수 있다).
 *  NavHost 안: 방·차단목록 → 목록 복귀 시 CHAT_ROOMS 로 목록 재조회.
 */
class ChatActivity : ResultActivity() {

    override val defaultResultKeys = setOf(ResultKeys.CHAT_ROOMS, ResultKeys.CHAT_ROOM)

    companion object {
        private const val EXTRA_ROOM_ID = "roomId"
        private const val EXTRA_TITLE = "title"
        private const val EXTRA_FILTER = "filter"
        private const val EXTRA_DISPATCH_ID = "dispatchId"   // 설문 응답 푸시 — 대상 설문 카드
        private const val EXTRA_PROPOSAL_ID = "proposalId"   // 약속 제안 푸시·웹 — 대상 제안 카드

        /** 목록부터. filter 는 웹 openChatList 의 세그먼트 문자열(requested|responded|inquiry). */
        fun listIntent(context: Context, filter: String? = null): Intent =
            Intent(context, ChatActivity::class.java).putExtra(EXTRA_FILTER, filter ?: "")

        /**
         * 특정 방 직행(견적 채택·푸시 딥링크). 뒤로가면 목록.
         *  [dispatchId]·[proposalId] 가 있으면 방에 들어간 뒤 그 설문/약속 제안 카드로 스크롤해 강조한다
         *  (iOS ChatRoomView initialSurveyDispatchId·initialProposalId).
         */
        fun roomIntent(
            context: Context,
            roomId: Int,
            title: String? = null,
            dispatchId: Int? = null,
            proposalId: Int? = null,
        ): Intent =
            Intent(context, ChatActivity::class.java)
                .putExtra(EXTRA_ROOM_ID, roomId)
                .putExtra(EXTRA_TITLE, title ?: "")
                .apply {
                    dispatchId?.takeIf { it > 0 }?.let { putExtra(EXTRA_DISPATCH_ID, it) }
                    proposalId?.takeIf { it > 0 }?.let { putExtra(EXTRA_PROPOSAL_ID, it) }
                }

        fun startList(context: Context, filter: String? = null) =
            context.launchScreen(listIntent(context, filter))

        fun startRoom(
            context: Context,
            roomId: Int,
            title: String? = null,
            dispatchId: Int? = null,
            proposalId: Int? = null,
        ) = context.launchScreen(
            // ★ 2026-10-30 채팅방은 한 번에 하나(카톡 방식, PACERA 048da603) — 채팅 → 레슨 상세 → '채팅으로 이동'처럼
            //  같은 방을 또 열면 액티비티가 끝없이 쌓였다. 떠 있는 채팅 화면이 있으면 그 위 화면을 닫고(CLEAR_TOP)
            //  그 화면이 onNewIntent 로 받아, 같은 방이면 그대로 두고 다른 방이면 보던 방을 닫고 연다.
            roomIntent(context, roomId, title, dispatchId, proposalId)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
        )
    }

    // 직행 대상(방·설문 카드·약속 제안 카드) — onNewIntent 로 바뀔 수 있어 상태로 둔다.
    private var deepRoomId by mutableIntStateOf(0)
    private var deepDispatchId by mutableStateOf<Int?>(null)
    private var deepProposalId by mutableStateOf<Int?>(null)
    private var navController: NavHostController? = null

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val rid = intent.getIntExtra(EXTRA_ROOM_ID, 0)
        if (rid <= 0) return
        deepRoomId = rid
        deepDispatchId = intent.getIntExtra(EXTRA_DISPATCH_ID, 0).takeIf { it > 0 }
        deepProposalId = intent.getIntExtra(EXTRA_PROPOSAL_ID, 0).takeIf { it > 0 }
        val nav = navController ?: return
        // 보고 있는 방이면 그대로(위 화면은 CLEAR_TOP 이 이미 닫았다).
        if (nav.currentBackStackEntry?.arguments?.getString("roomId")?.toIntOrNull() == rid) return
        val title = intent.getStringExtra(EXTRA_TITLE).orEmpty()
        // 설문·약속 카드 직행은 직행용 경로(room/{id})로 — 그쪽만 대상 카드로 스크롤한다.
        val route = if (deepDispatchId != null || deepProposalId != null) "room/$rid" else "room/$rid?title=$title"
        nav.navigate(route) {
            // 보던 방은 닫는다 — 목록이 있으면 목록 위로, 방으로 시작했으면 그 방까지 지운다.
            val hasList = runCatching { nav.getBackStackEntry("list") }.isSuccess
            if (hasList) popUpTo("list") else popUpTo(nav.graph.id) { inclusive = true }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        deepRoomId = intent.getIntExtra(EXTRA_ROOM_ID, 0)
        val startRoomId = deepRoomId
        val deepTitle = intent.getStringExtra(EXTRA_TITLE).orEmpty()
        deepDispatchId = intent.getIntExtra(EXTRA_DISPATCH_ID, 0).takeIf { it > 0 }
        deepProposalId = intent.getIntExtra(EXTRA_PROPOSAL_ID, 0).takeIf { it > 0 }
        val filter = ChatRoomFilter.from(intent.getStringExtra(EXTRA_FILTER)?.ifEmpty { null })

        // 소켓 연결 + 백그라운드 pause/resume 옵저버 등록(둘 다 idempotent).
        ChatSocketManager.connect(this)
        ChatSocketLifecycleObserver.ensureAttached(this)

        setContent {
            val nav = rememberNavController()
            navController = nav
            com.muyeon.app.analytics.TrackNavigation("Chat", nav)
            val token = remember { TokenManager.getAccessToken(this) }
            val api = remember { ChatApi(token) }

            // ★ 목록 상태는 NavHost **바깥**에 둔다 — 방에 들어가면 목록 composable 이
            //   composition 에서 빠지므로, 안에 두면 목록·필터가 매번 날아간다.
            val listState = remember { ChatListState(api) }
            val scope = rememberCoroutineScope()
            LaunchedEffect(Unit) {
                listState.setFilterFrom(filter)
                listState.start(scope)
                listState.load()
            }

            // 화면이 다시 보일 때마다 조용히 재조회.
            //  ⚠️ 이게 없으면 백그라운드에 다녀온 사이 온 메시지가 목록에 안 뜬다 —
            //    소켓은 ON_STOP 에서 끊기고 이벤트는 replay=0 이라 통째로 유실되기 때문이다.
            //    (최초 진입의 ON_RESUME 은 위 load() 와 겹치지만 요청이 합쳐져 1회로 끝난다.)
            val lifecycleOwner = LocalLifecycleOwner.current
            DisposableEffect(lifecycleOwner) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_RESUME) listState.requestReload(immediate = true)
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
            }

            val start = if (startRoomId > 0) "room/$startRoomId" else "list"

            NavHost(nav, startDestination = start) {
                composable("list") { entry ->
                    // 방·차단목록에서 돌아오면 목록을 다시 읽는다(ON_RESUME 은 액티비티 복귀만 잡는다).
                    entry.OnResultKeys { keys ->
                        if (ResultKeys.CHAT_ROOMS in keys) listState.requestReload()
                    }
                    ChatListScreen(
                        state = listState,
                        onClose = { finish() },
                        onOpenRoom = { rid, title -> nav.navigate("room/$rid?title=$title") },
                        onOpenBlocked = { nav.navigate("blocked") },
                    )
                }
                composable("blocked") { entry ->
                    nav.ReturnResultKeys(entry, ResultKeys.CHAT_ROOMS)
                    BlockedUsersScreen(api = api, onBack = { nav.popBackStack() })
                }
                composable("room/{roomId}") { entry ->
                    nav.ReturnResultKeys(entry, ResultKeys.CHAT_ROOMS)
                    val rid = entry.arguments?.getString("roomId")?.toIntOrNull() ?: 0
                    // ★ 2026-10-30 remember{} 로 만들면 ViewModel.onCleared 가 영영 안 불려 방 입장(join-room)이 풀리지 않았다
                    //  → 나간 방도 서버는 '보는 중'으로 알아 푸시·안읽음이 빠지고, 남은 VM 이 새 메시지를 읽음 처리했다.
                    //  viewModel(key) 는 이 화면(NavBackStackEntry)에 붙어 방을 떠날 때 onCleared → leaveRoom 이 불린다.
                    val vm: ChatRoomViewModel = viewModel(key = "room-$rid") { ChatRoomViewModel(rid, deepTitle, api, token) }
                    ActiveRoomEffect(listState, rid)
                    // 딥링크 대상 카드는 직행한 방에만 적용한다(목록에서 다시 연 방에는 적용하지 않음).
                    val isDeepRoom = rid == deepRoomId
                    ChatRoomScreen(
                        vm = vm,
                        onBack = { if (!nav.popBackStack()) finish() },
                        initialSurveyDispatchId = deepDispatchId.takeIf { isDeepRoom },
                        initialProposalId = deepProposalId.takeIf { isDeepRoom },
                    )
                }
                composable("room/{roomId}?title={title}") { entry ->
                    nav.ReturnResultKeys(entry, ResultKeys.CHAT_ROOMS)
                    val rid = entry.arguments?.getString("roomId")?.toIntOrNull() ?: 0
                    val t = entry.arguments?.getString("title").orEmpty()
                    val vm: ChatRoomViewModel = viewModel(key = "room-$rid") { ChatRoomViewModel(rid, t, api, token) }
                    ActiveRoomEffect(listState, rid)
                    ChatRoomScreen(vm = vm, onBack = { if (!nav.popBackStack()) finish() })
                }
            }
        }
    }
}

/**
 * 방 화면이 떠 있는 동안 목록 상태에 "지금 보고 있는 방" 을 알린다 — 그 방의 안읽음은 0 으로 표시된다.
 *  방이 겹쳐 열리는 경우(딥링크 → 다른 방) 나중에 열린 방이 우선이며, 내 방이 아닐 때는 지우지 않는다.
 */
@androidx.compose.runtime.Composable
private fun ActiveRoomEffect(listState: ChatListState, roomId: Int) {
    DisposableEffect(roomId) {
        listState.activeRoomId = roomId
        onDispose { if (listState.activeRoomId == roomId) listState.activeRoomId = null }
    }
}

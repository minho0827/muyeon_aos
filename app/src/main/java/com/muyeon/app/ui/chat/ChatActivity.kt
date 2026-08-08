package com.muyeon.app.ui.chat

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.remember
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.muyeon.app.chat.socket.ChatSocketLifecycleObserver
import com.muyeon.app.chat.socket.ChatSocketManager
import com.muyeon.app.utils.TokenManager

/**
 * 채팅 컨테이너 — 웹 `openChatList` / `openChatRoom` 브릿지 진입점.
 *  iOS 는 ChatListView(NavigationView) 안에서 ChatRoomView 를 push 한다. 동일 스택을 NavHost 로.
 *
 *  소켓은 앱 전역 단일([ChatSocketManager]) — 화면이 아니라 프로세스 수명에 맞춘다.
 *  진입 시 connect, 백그라운드 전환은 [ChatSocketLifecycleObserver] 가 pause/resume.
 */
class ChatActivity : ComponentActivity() {

    companion object {
        private const val EXTRA_ROOM_ID = "roomId"
        private const val EXTRA_TITLE = "title"
        private const val EXTRA_FILTER = "filter"

        /** 목록부터. filter 는 웹 openChatList 의 세그먼트 문자열(requested|responded|inquiry). */
        fun startList(context: Context, filter: String? = null) {
            context.launch(Intent(context, ChatActivity::class.java).putExtra(EXTRA_FILTER, filter ?: ""))
        }

        /** 특정 방 직행(견적 채택·푸시 딥링크). 뒤로가면 목록. */
        fun startRoom(context: Context, roomId: Int, title: String? = null) {
            context.launch(
                Intent(context, ChatActivity::class.java)
                    .putExtra(EXTRA_ROOM_ID, roomId)
                    .putExtra(EXTRA_TITLE, title ?: ""),
            )
        }

        private fun Context.launch(i: Intent) {
            if (this !is Activity) i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(i)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val deepRoomId = intent.getIntExtra(EXTRA_ROOM_ID, 0)
        val deepTitle = intent.getStringExtra(EXTRA_TITLE).orEmpty()
        val filter = ChatRoomFilter.from(intent.getStringExtra(EXTRA_FILTER)?.ifEmpty { null })

        // 소켓 연결 + 백그라운드 pause/resume 옵저버 등록(둘 다 idempotent).
        ChatSocketManager.connect(this)
        ChatSocketLifecycleObserver.ensureAttached(this)

        setContent {
            val nav = rememberNavController()
            val token = remember { TokenManager.getAccessToken(this) }
            val api = remember { ChatApi(token) }

            val start = if (deepRoomId > 0) "room/$deepRoomId" else "list"

            NavHost(nav, startDestination = start) {
                composable("list") {
                    ChatListScreen(
                        api = api,
                        initialFilter = filter,
                        onClose = { finish() },
                        onOpenRoom = { rid, title -> nav.navigate("room/$rid?title=$title") },
                    )
                }
                composable("room/{roomId}") { entry ->
                    val rid = entry.arguments?.getString("roomId")?.toIntOrNull() ?: 0
                    val vm = remember(rid) { ChatRoomViewModel(rid, deepTitle, api, token) }
                    ChatRoomScreen(vm = vm, onBack = { if (!nav.popBackStack()) finish() })
                }
                composable("room/{roomId}?title={title}") { entry ->
                    val rid = entry.arguments?.getString("roomId")?.toIntOrNull() ?: 0
                    val t = entry.arguments?.getString("title").orEmpty()
                    val vm = remember(rid) { ChatRoomViewModel(rid, t, api, token) }
                    ChatRoomScreen(vm = vm, onBack = { if (!nav.popBackStack()) finish() })
                }
            }
        }
    }
}

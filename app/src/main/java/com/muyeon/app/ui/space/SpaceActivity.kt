package com.muyeon.app.ui.space

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.runtime.remember
import com.muyeon.app.result.ResultActivity
import com.muyeon.app.result.ResultKeys
import com.muyeon.app.result.launchScreen
import com.muyeon.app.ui.chat.ChatActivity

/**
 * 공간 상세 진입점 — 웹 `openSpaceDetail`(spaceId, date, fromReservation).
 *  iOS `WebViewModel.presentSpaceDetail` 대응.
 *
 *  결과: SPACE·RESERVATIONS(찜·예약 요청). 예약·찜을 실제로 했으면 명시적으로도 싣는다.
 */
class SpaceActivity : ResultActivity() {

    override val defaultResultKeys = setOf(ResultKeys.SPACE, ResultKeys.RESERVATIONS)

    companion object {
        private const val EXTRA_SPACE_ID = "spaceId"
        private const val EXTRA_DATE = "date"
        private const val EXTRA_FROM_RESERVATION = "fromReservation"
        /** ★ 로그인 후 이어서 할 일(reserve·chat·like) — LoginResume "then". */
        private const val EXTRA_THEN = "then"

        fun intent(context: Context, spaceId: Int, date: String?, fromReservation: Boolean): Intent =
            Intent(context, SpaceActivity::class.java)
                .putExtra(EXTRA_SPACE_ID, spaceId)
                .putExtra(EXTRA_DATE, date ?: "")
                .putExtra(EXTRA_FROM_RESERVATION, fromReservation)

        fun start(context: Context, spaceId: Int, date: String?, fromReservation: Boolean, then: String? = null) =
            context.launchScreen(intent(context, spaceId, date, fromReservation).putExtra(EXTRA_THEN, then.orEmpty()))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val spaceId = intent.getIntExtra(EXTRA_SPACE_ID, 0)
        if (spaceId <= 0) { finish(); return }
        val date = intent.getStringExtra(EXTRA_DATE).orEmpty()
        val fromReservation = intent.getBooleanExtra(EXTRA_FROM_RESERVATION, false)

        setContent {
            val api = remember { SpaceApi(this) }
            SpaceDetailScreen(
                api = api,
                spaceId = spaceId,
                initialDate = date,
                // 예약내역에서 들어오면 예약 버튼을 숨긴다(iOS hideReserve).
                hideReserve = fromReservation,
                resumeAction = intent.getStringExtra(EXTRA_THEN)?.takeIf { it.isNotEmpty() },
                onClose = { finish() },
                onChat = { roomId -> ChatActivity.startRoom(this, roomId) },
                onReserved = { addResultKeys(ResultKeys.RESERVATIONS) },
                onScrapChanged = { addResultKeys(ResultKeys.SPACE) },
                onEdit = { SpaceHubActivity.startEditor(this, it) },
                onManage = { SpaceHubActivity.startHosting(this) },
                onOpenSpace = { start(this, it, null, false) },
                // 결제 완료 → 내 예약(새로 읽은 목록)으로. 웹 부모도 예약 목록을 다시 읽게 RESERVATIONS 를 싣는다.
                onPaid = { addResultKeys(ResultKeys.RESERVATIONS); SpaceHubActivity.startBookings(this) },
            )
        }
    }
}

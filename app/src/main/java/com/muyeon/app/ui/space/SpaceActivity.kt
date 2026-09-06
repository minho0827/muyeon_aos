package com.muyeon.app.ui.space

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.remember
import com.muyeon.app.ui.chat.ChatActivity
import com.muyeon.app.utils.TokenManager

/**
 * 공간 상세 진입점 — 웹 `openSpaceDetail`(spaceId, date, fromReservation).
 *  iOS `WebViewModel.presentSpaceDetail` 대응.
 */
class SpaceActivity : ComponentActivity() {

    companion object {
        private const val EXTRA_SPACE_ID = "spaceId"
        private const val EXTRA_DATE = "date"
        private const val EXTRA_FROM_RESERVATION = "fromReservation"

        fun start(context: Context, spaceId: Int, date: String?, fromReservation: Boolean) {
            val i = Intent(context, SpaceActivity::class.java)
                .putExtra(EXTRA_SPACE_ID, spaceId)
                .putExtra(EXTRA_DATE, date ?: "")
                .putExtra(EXTRA_FROM_RESERVATION, fromReservation)
            if (context !is Activity) i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(i)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val spaceId = intent.getIntExtra(EXTRA_SPACE_ID, 0)
        if (spaceId <= 0) { finish(); return }
        val date = intent.getStringExtra(EXTRA_DATE).orEmpty()
        val fromReservation = intent.getBooleanExtra(EXTRA_FROM_RESERVATION, false)

        setContent {
            val api = remember { SpaceApi(TokenManager.getAccessToken(this)) }
            SpaceDetailScreen(
                api = api,
                spaceId = spaceId,
                initialDate = date,
                // 예약내역에서 들어오면 예약 버튼을 숨긴다(iOS hideReserve).
                hideReserve = fromReservation,
                onClose = { finish() },
                onChat = { roomId -> ChatActivity.startRoom(this, roomId) },
            )
        }
    }
}

package com.muyeon.app.ui.chat

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muyeon.app.BuildConfig
import com.muyeon.app.ui.common.MuyeonColors

/**
 * 강사가 견적 상담 채팅을 처음 열었을 때 + 버튼 위에 한 번 뜨는 설문 안내(iOS ChatSurveyHint 와 같은 문구·규칙).
 * 앱 설치 후 1회 — 앱 데이터와 함께 지워지므로 재설치하면 다시 한 번 보인다.
 */
object ChatSurveyHint {
    private const val PREFS = "chat_hints"
    private const val KEY = "surveyHintShown"
    // 2026-10-20 UI 확인용 — 개발 빌드는 견적 상담방을 열 때마다 뜬다. 정식 빌드는 앱 설치 후 1회.
    private val SHOW_EVERY_TIME = BuildConfig.DEBUG

    /** 아직 안 보여 줬으면 true 를 돌려주고 '보여 줌'으로 기록한다(같은 방을 다시 열어도 한 번뿐). */
    fun consume(ctx: Context): Boolean {
        if (SHOW_EVERY_TIME) return true
        val p = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (p.getBoolean(KEY, false)) return false
        p.edit().putBoolean(KEY, true).apply()
        return true
    }
}

/**
 * + 버튼 위 작은 말풍선 — 누르면 설문지 고르기를 바로 연다, X 는 닫기만.
 * ★ 2026-10-20 처음엔 입력바 폭 전체 카드(아이콘·3줄)라 채팅을 너무 가렸다 → 글 2줄짜리 말풍선으로(iOS 동일).
 */
@Composable
fun ChatSurveyHintBubble(onOpen: () -> Unit, onClose: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
        Row(
            Modifier
                .shadow(4.dp, RoundedCornerShape(12.dp))
                .clip(RoundedCornerShape(12.dp))
                .background(MuyeonColors.primary)
                .clickable(onClick = onOpen)
                .padding(start = 12.dp, end = 6.dp, top = 8.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text("레슨 전 설문을 먼저 보내 보세요", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Text("회원 수준·원하는 수업을 미리 알 수 있어요 ›", color = Color.White.copy(alpha = 0.9f), fontSize = 11.sp)
            }
            Icon(
                Icons.Filled.Close, "안내 닫기", tint = Color.White.copy(alpha = 0.85f),
                modifier = Modifier.size(18.dp).clip(RoundedCornerShape(50)).clickable(onClick = onClose).padding(3.dp),
            )
        }
        // 꼬리 — + 버튼(입력바 왼쪽 끝)을 가리킨다.
        Spacer(
            Modifier
                .padding(start = 17.dp)
                .size(width = 12.dp, height = 6.dp)
                .background(MuyeonColors.primary, GenericShape { s, _ ->
                    moveTo(0f, 0f); lineTo(s.width, 0f); lineTo(s.width / 2f, s.height); close()
                }),
        )
    }
}

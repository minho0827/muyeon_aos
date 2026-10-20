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
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muyeon.app.ui.common.MuyeonColors

/**
 * 강사가 견적 상담 채팅을 처음 열었을 때 + 버튼 위에 한 번 뜨는 설문 안내(iOS ChatSurveyHint 와 같은 문구·규칙).
 * 앱 설치 후 1회 — 앱 데이터와 함께 지워지므로 재설치하면 다시 한 번 보인다.
 */
object ChatSurveyHint {
    private const val PREFS = "chat_hints"
    private const val KEY = "surveyHintShown"

    /** 아직 안 보여 줬으면 true 를 돌려주고 '보여 줌'으로 기록한다(같은 방을 다시 열어도 한 번뿐). */
    fun consume(ctx: Context): Boolean {
        val p = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (p.getBoolean(KEY, false)) return false
        p.edit().putBoolean(KEY, true).apply()
        return true
    }
}

/** + 버튼 위 말풍선 — 누르면 설문지 고르기를 바로 연다, X 는 닫기만. */
@Composable
fun ChatSurveyHintBubble(onOpen: () -> Unit, onClose: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
        Row(
            Modifier
                .fillMaxWidth()
                .shadow(6.dp, RoundedCornerShape(14.dp))
                .clip(RoundedCornerShape(14.dp))
                .background(MuyeonColors.primary)
                .clickable(onClick = onOpen)
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(Icons.Filled.Assignment, null, tint = Color.White, modifier = Modifier.size(18.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("상담 전에 레슨 전 설문을 보내 보세요", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text(
                    "회원님의 수준과 원하는 수업을 미리 알면 상담이 훨씬 빨라져요. + 버튼 › 설문지에서 보낼 수 있어요.",
                    color = Color.White, fontSize = 12.sp, lineHeight = 17.sp,
                )
                Text("설문지 고르기 ›", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 4.dp))
            }
            Icon(
                Icons.Filled.Close, "안내 닫기", tint = Color.White.copy(alpha = 0.85f),
                modifier = Modifier.size(20.dp).clip(RoundedCornerShape(50)).clickable(onClick = onClose).padding(2.dp),
            )
        }
        // 꼬리 — + 버튼(입력바 왼쪽 끝)을 가리킨다.
        Spacer(
            Modifier
                .padding(start = 22.dp)
                .size(width = 16.dp, height = 8.dp)
                .background(MuyeonColors.primary, GenericShape { s, _ ->
                    moveTo(0f, 0f); lineTo(s.width, 0f); lineTo(s.width / 2f, s.height); close()
                }),
        )
    }
}

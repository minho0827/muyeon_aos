package com.muyeon.app.ui.floating

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muyeon.app.theme.customFontFamily
import com.muyeon.app.ui.common.MuyeonColors

/**
 * 인증 심사 완료 화면 — iOS `VerificationCompleteView.swift` 1:1.
 *  책갈피(완료·초록) 탭 시 표시.
 *  닫기(뒤로가기·바깥 탭)는 호출부 Dialog 의 onDismissRequest 가 처리한다 —
 *  거기서 '확인 처리'(책갈피 숨김)까지 같이 해야 해서 화면이 따로 콜백을 받지 않는다.
 */
@Composable
fun VerificationCompleteScreen(role: String, onStart: () -> Unit) {
    val roleLabel = when (role) {
        "TEACHER" -> "강사"
        "DANCER" -> "무용수"
        "ACADEMY" -> "학원·원장"
        "SPACE" -> "공간 보유자"
        "TEAM" -> "공연팀·기획자"
        else -> "회원"
    }
    val features = when (role) {
        "TEACHER" -> listOf("강사 이력서 등록", "레슨 프로필 등록", "채용·대타 공고 지원")
        "DANCER" -> listOf("무용수 이력서 등록", "프로필·포트폴리오 등록", "캐스팅 공고 지원")
        "ACADEMY" -> listOf("강사 채용 공고 등록", "지원자 이력서 열람·관리", "대타 공고 등록")
        "SPACE" -> listOf("연습실·공연 공간 등록", "공간 예약 접수·관리")
        "TEAM" -> listOf("공연·오디션 공고 등록", "무용수 프로필 열람·캐스팅 제안")
        else -> listOf("새로운 기능을 이용할 수 있어요.")
    }

    Column(
        Modifier.fillMaxSize().background(MuyeonColors.surface),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(1f))

        Icon(
            Icons.Filled.Verified, null, tint = VERIFY_GREEN,
            modifier = Modifier.size(72.dp).padding(bottom = 0.dp),
        )
        Spacer(Modifier.height(24.dp))
        Text(
            "심사가 완료되었습니다",
            fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 24.sp,
            lineHeight = 29.sp, color = MuyeonColors.textHead,
        )
        Text(
            "${roleLabel} 인증이 승인되었어요.\n이제 아래 기능을 이용할 수 있어요.",
            fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 15.sp,
            lineHeight = 23.sp, color = MuyeonColors.textSub, textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 10.dp),
        )

        Column(
            Modifier.padding(horizontal = 28.dp).padding(top = 32.dp)
                .fillMaxWidth().clip(RoundedCornerShape(16.dp))
                .background(Color(0xFFF4F4F4)).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            features.forEach { f ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Filled.CheckCircle, null, tint = MuyeonColors.primary,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        f,
                        fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 15.sp,
                        lineHeight = 18.sp, color = MuyeonColors.textHead,
                    )
                }
            }
        }

        Spacer(Modifier.weight(1f))

        Text(
            "시작하기",
            fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp,
            lineHeight = 19.sp, color = Color.White, textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 20.dp)
                .fillMaxWidth().clip(RoundedCornerShape(12.dp))
                .background(MuyeonColors.primary)
                .clickable(onClick = onStart)
                .padding(vertical = 16.dp),
        )
    }
}

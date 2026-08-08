package com.muyeon.app.ui.quote

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.MarkUnreadChatAlt
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muyeon.app.theme.customFontFamily
import com.muyeon.app.ui.common.MuyeonColors

/**
 * 레슨 요청 인트로(최초 1회) — iOS `QuoteIntroView.swift` 1:1.
 *  무용 종류 선택으로 바로 떨어지는 갑작스러움 해소.
 *  강사 지정(1:1) 요청은 이 화면을 거치지 않는다(프로필 맥락이 이미 있음).
 *
 * ⚠️ iOS 수치: 제목 26 bold(top 28) / 부제 15 medium(top 12, 줄간격 +5) / 혜택행 v16 아이콘 56 /
 *   안내박스 r12 F7F7F7 p16 / CTA 16 bold v16 r12 / 좌우 20.
 */
@Composable
fun QuoteIntroScreen(onStart: () -> Unit, onClose: () -> Unit) {
    Column(Modifier.fillMaxSize().background(MuyeonColors.surface)) {
        // navBar — 제목 18 bold, X 18 medium, 좌우 20 상하 14
        Box(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "레슨 요청",
                fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 18.sp,
                lineHeight = 21.sp, color = MuyeonColors.textHead,
            )
            Icon(
                Icons.Filled.Close, "닫기", tint = MuyeonColors.body,
                modifier = Modifier.align(Alignment.CenterEnd).size(18.dp).clickable(onClick = onClose),
            )
        }

        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
        ) {
            Text(
                "강사를 찾고 계신가요?",
                fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 26.sp,
                lineHeight = 31.sp, color = MuyeonColors.textHead,
                modifier = Modifier.padding(top = 28.dp),
            )
            Text(
                "원하는 조건을 입력하면\n여러 강사가 견적을 보내드립니다.",
                fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 15.sp,
                lineHeight = 23.sp,   // iOS lineSpacing 5 = 18 + 5
                color = MuyeonColors.textSub,
                modifier = Modifier.padding(top = 12.dp),
            )

            Column(Modifier.padding(top = 24.dp)) {
                BenefitRow(Icons.Outlined.Notifications, "여러 강사의 견적 비교", "최대 5명의 강사에게 견적을 받아\n비교할 수 있어요.")
                HorizontalDivider(color = Color(0xFFF4F4F4))
                BenefitRow(Icons.Outlined.CalendarMonth, "원하는 시간·지역 선택", "날짜와 시간, 지역을 선택하면\n조건에 맞게 보내드려요.")
                HorizontalDivider(color = Color(0xFFF4F4F4))
                BenefitRow(Icons.Outlined.MarkUnreadChatAlt, "마음에 드는 강사와 상담", "궁금한 점은 채팅으로 편하게\n상담할 수 있어요.")
            }

            // 안내사항 박스
            Column(
                Modifier
                    .padding(top = 20.dp, bottom = 24.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF7F7F7))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    "안내사항",
                    fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 14.sp,
                    lineHeight = 17.sp, color = MuyeonColors.textHead,
                )
                BulletLine("요청 내용은 강사에게만 공개됩니다.")
                BulletLine("최대 5명의 강사에게 견적을 받을 수 있습니다.")
                BulletLine("원하지 않을 경우 언제든 요청을 취소할 수 있습니다.")
            }
        }

        // 하단 CTA
        Text(
            "레슨 요청 시작하기",
            fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp,
            lineHeight = 19.sp, color = Color.White, textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .padding(top = 8.dp, bottom = 12.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MuyeonColors.primary)
                .clickable(onClick = onStart)
                .padding(vertical = 16.dp),
        )
    }
}

@Composable
private fun BenefitRow(icon: ImageVector, title: String, desc: String) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(
            Modifier.size(56.dp).clip(CircleShape).background(MuyeonColors.primary.copy(alpha = 0.10f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = MuyeonColors.primary, modifier = Modifier.size(22.dp))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                title,
                fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp,
                lineHeight = 19.sp, color = MuyeonColors.textHead,
            )
            Text(
                desc,
                fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp,
                lineHeight = 21.sp,   // iOS lineSpacing 4 = 17 + 4
                color = MuyeonColors.textSub,
            )
        }
    }
}

@Composable
private fun BulletLine(text: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            "•",
            fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 13.sp,
            lineHeight = 16.sp, color = MuyeonColors.textSub,
        )
        Text(
            text,
            fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 13.sp,
            lineHeight = 16.sp, color = MuyeonColors.textSub,
        )
    }
}

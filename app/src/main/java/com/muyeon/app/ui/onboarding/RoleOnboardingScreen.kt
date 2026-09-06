package com.muyeon.app.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
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
 * 회원가입 후 회원유형 선택 온보딩(풀스크린, 강제) — iOS `RoleOnboardingView.swift` 1:1.
 *  웹 가입완료 → 브릿지 openRoleOnboarding → 이 화면 → 선택 시 웹에 통지.
 *
 * ⚠️ 강제 화면이라 뒤로가기로 닫히면 안 된다(호출부에서 BackHandler 로 막는다).
 */
@Composable
fun RoleOnboardingScreen(onSelect: (String) -> Unit) {
    var selected by remember { mutableStateOf<String?>(null) }

    Column(Modifier.fillMaxSize().background(MuyeonColors.surface)) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 24.dp)
                .padding(top = 32.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                "어떤 목적으로\n무용연을 이용하시나요?",
                fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 24.sp,
                lineHeight = 33.sp, color = MuyeonColors.textHead,
            )
            Text(
                "회원 유형은 언제든 추가하거나 변경할 수 있어요.",
                fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp,
                lineHeight = 17.sp, color = MuyeonColors.textSub,
            )
        }

        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp).padding(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ONBOARDING_ROLE_OPTIONS.forEach { opt -> RoleCard(opt, selected == opt.code) { selected = opt.code } }
        }

        Text(
            "시작하기",
            fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp,
            lineHeight = 19.sp, color = Color.White, textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 20.dp).padding(top = 8.dp, bottom = 16.dp)
                .fillMaxWidth().clip(RoundedCornerShape(12.dp))
                .background(if (selected == null) MuyeonColors.tileLocked else MuyeonColors.primary)
                .clickable(enabled = selected != null) { selected?.let(onSelect) }
                .padding(vertical = 16.dp),
        )
    }
}

internal data class OnboardingRoleOption(val code: String, val emoji: String, val title: String, val desc: String)

/** 공간보유자 유형은 회원 기반 확대 후 이 위치에서 재오픈한다(iOS 주석과 동일). */
internal val ONBOARDING_ROLE_OPTIONS = listOf(
    OnboardingRoleOption("TEACHER", "🩰", "강사", "강사 채용 공고를 보고 지원하고 싶어요."),
    OnboardingRoleOption("ACADEMY", "🏫", "학원·원장", "강사를 채용하거나 학원 공고를 등록하고 싶어요."),
    OnboardingRoleOption("DANCER", "💃", "무용수", "오디션 및 캐스팅 공고를 보고 지원하고 싶어요."),
    OnboardingRoleOption("TEAM", "🎭", "공연팀·기획자", "공연 및 오디션 공고를 등록하고 무용수를 모집하고 싶어요."),
    OnboardingRoleOption("HOBBY", "✨", "일반회원", "커뮤니티와 개인레슨 등 무용 관련 서비스를 이용하고 싶어요."),
)

/** 일반회원(HOBBY)만 인증 없이 바로 시작한다 — 나머지는 서류 인증이 필요하다. */
internal fun roleRequiresVerification(code: String) = code != "HOBBY"

@Composable
private fun RoleCard(opt: OnboardingRoleOption, isSelected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
            .background(
                if (isSelected) Color(0xFFFFE4D2).copy(alpha = 0.4f) else MuyeonColors.surface,
            )
            .border(
                if (isSelected) 2.dp else 1.dp,
                if (isSelected) MuyeonColors.primary else MuyeonColors.border,
                RoundedCornerShape(14.dp),
            )
            .clickable(onClick = onClick)
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(opt.emoji, fontSize = 28.sp, lineHeight = 34.sp)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                opt.title,
                fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 17.sp,
                lineHeight = 20.sp, color = MuyeonColors.textHead,
            )
            Text(
                opt.desc,
                fontFamily = customFontFamily, fontSize = 13.sp, lineHeight = 18.sp,
                color = MuyeonColors.textSub,
            )
        }
        if (roleRequiresVerification(opt.code)) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Icon(Icons.Filled.Lock, null, tint = MuyeonColors.textSub, modifier = Modifier.size(15.dp))
                Text(
                    "인증 필요",
                    fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 11.sp,
                    lineHeight = 14.sp, color = MuyeonColors.textSub,
                )
            }
        }
    }
}

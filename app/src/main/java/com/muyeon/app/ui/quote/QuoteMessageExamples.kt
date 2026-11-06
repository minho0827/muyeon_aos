package com.muyeon.app.ui.quote

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muyeon.app.theme.customFontFamily
import com.muyeon.app.ui.chat.QuoteCardBubble
import com.muyeon.app.ui.common.MuyeonColors
import com.muyeon.app.ui.resume.ResumeActivity
import com.muyeon.app.ui.resume.ResumeApi
import com.muyeon.app.utils.TokenManager
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.json.JSONObject

/**
 * "다른 강사님들은 이렇게 보내요" — 견적 메시지를 쓰는 강사에게 보여 주는 예시.
 *  보내는 사람은 실제 대표 강사 계정(23 무용연 · 6 무연). 시트를 열 때 공개 프로필(이름·사진)을 불러오고,
 *  실패하면 아래 이름·기본 아바타로 그린다.
 */
object QuoteMessageExamples {
    data class Item(
        val userId: Int,
        val fallbackName: String,
        val service: String,
        val region: String,
        val priceAmount: Int,
        val priceUnit: String,
        val text: String,
    )

    val items = listOf(
        Item(
            userId = 23,
            fallbackName = "무용연",
            service = "발레",
            region = "서울 강북구",
            priceAmount = 55000,
            priceUnit = "PER_SESSION",
            text = "안녕하세요! 발레를 가르치는 무용연이에요 🩰\n\n발레를 전공하고 6년 동안 무대와 수업 현장에서 활동했어요. 체형과 목표를 먼저 여쭤보고, 기본 자세부터 차근차근 맞춤 수업을 짜 드려요.\n\n처음이셔도 괜찮아요. 바른 자세와 유연성이 달라지는 걸 직접 느끼실 거예요.\n\n개인레슨 가격\n· 1:1 1회 55,000원\n· 주 1회 (한 달 4회) 20만원\n· 주 2회 (한 달 8회) 38만원\n\n* 연습실 대여비 포함\n* 궁금한 점은 채팅으로 편하게 물어봐 주세요",
        ),
        Item(
            userId = 6,
            fallbackName = "무연",
            service = "한국무용",
            region = "서울 강남구",
            priceAmount = 60000,
            priceUnit = "PER_SESSION",
            text = "안녕하세요, 한국무용 강사 무연입니다 🌿\n\n한국무용을 전공하고 무대와 교육 현장에서 쌓은 경험으로 수업하고 있어요. 기본 자세와 호흡, 춤사위를 탄탄히 익히면서 한국무용의 멋을 자연스럽게 느끼실 수 있게 도와드려요.\n\n취미반·입시반 모두 가능하고, 배우고 싶은 작품이 있으면 그 작품으로 진행해요.\n\n가격\n· 1:1 1회 60,000원\n· 한 달 4회 22만원 / 8회 42만원\n\n* 서울 강남구 연습실 (대여비 별도)",
        ),
    )

    /** 견적 카드(QUOTE_CARD content) 와 같은 모양의 JSON — QuoteCardBubble 에 그대로 넣는다. */
    fun cardJson(item: Item, name: String, image: String?): String = JSONObject()
        .put("id", item.userId)
        .put("name", name)
        .put("image", image ?: "")
        .put("service", item.service)
        .put("region", item.region)
        .put("priceAmount", item.priceAmount)
        .put("priceUnit", item.priceUnit)
        .toString()

    /**
     * ★ 2026-11-06 자동응답 미리보기용 카드 JSON — 금액은 강사가 쓴 문구 그대로("price").
     *  비어 있으면 넣지 않아 카드가 '협의 가능'으로 그린다(QuoteUi.priceText).
     */
    fun previewCardJson(
        userId: Int, name: String, image: String?,
        service: String?, region: String?, career: String?, price: String?,
    ): String = JSONObject()
        .put("id", userId)
        .put("name", name)
        .put("image", image ?: "")
        .put("service", service ?: "")
        .put("region", region ?: "")
        .put("career", career ?: "")
        .apply { price?.trim()?.takeIf { it.isNotEmpty() }?.let { put("price", it) } }
        .toString()
}

/** 메시지 칸 라벨 옆(오른쪽) 작은 글자 버튼 — "ⓘ 다른 강사님들은 이렇게 보내요". 누르면 예시 시트. */
@Composable
fun QuoteMessageExamplesButton(modifier: Modifier = Modifier) {
    var open by remember { mutableStateOf(false) }
    Row(
        modifier.clip(RoundedCornerShape(6.dp)).clickable { open = true }.padding(horizontal = 2.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            Modifier.size(14.dp).border(1.2.dp, MuyeonColors.primary, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "i", fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 9.sp,
                lineHeight = 9.sp, color = MuyeonColors.primary,
            )
        }
        Text(
            "다른 강사님들은 이렇게 보내요",
            fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 13.sp,
            lineHeight = 16.sp, color = MuyeonColors.primary,
        )
    }
    if (open) QuoteMessageExamplesSheet(onDismiss = { open = false })
}

/** 예시 시트 — 읽기 전용. 대표 강사 두 명이 보낸 것처럼 [아바타 + 이름] → 견적 카드 → 인사말 말풍선. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuoteMessageExamplesSheet(onDismiss: () -> Unit) {
    val context = LocalContext.current
    // userId → (이름, 사진). 불러오기 전·실패 시엔 비어 있어 기본 이름·아바타로 그린다.
    var profiles by remember { mutableStateOf<Map<Int, Pair<String?, String?>>>(emptyMap()) }
    LaunchedEffect(Unit) {
        val api = ResumeApi(TokenManager.getAccessToken(context))
        profiles = coroutineScope {
            QuoteMessageExamples.items.map { item ->
                async {
                    api.publicProfile(item.userId, src = "quote").getOrNull()?.let { item.userId to (it.name to it.image) }
                }
            }.awaitAll().filterNotNull().toMap()
        }
    }
    val openProfile: (Int) -> Unit = { id -> ResumeActivity.startProfile(context, id, "quote_example") }

    QuoteThreadSheet(
        title = "다른 강사님들은 이렇게 보내요",
        sub = "무용연 대표 강사 프로필의 견적 예시예요.",
        onDismiss = onDismiss,
    ) {
        QuoteMessageExamples.items.forEach { item ->
            val loaded = profiles[item.userId]
            val name = loaded?.first?.takeIf { it.isNotBlank() } ?: item.fallbackName
            val image = loaded?.second?.takeIf { it.isNotBlank() }
            QuoteChatThread(
                name = name,
                image = image,
                cardJson = QuoteMessageExamples.cardJson(item, name, image),
                text = item.text,
                onOpenProfile = { openProfile(item.userId) },
            )
        }
    }
}

/**
 * ★ 2026-11-06 예시 시트·자동응답 미리보기가 같이 쓰는 시트 틀 — 제목 + 닫기 + 안내 한 줄 + 스레드 목록.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun QuoteThreadSheet(
    title: String,
    sub: String,
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MuyeonColors.surface,
    ) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding()) {
            Row(
                Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    title,
                    fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 17.sp,
                    lineHeight = 20.sp, color = MuyeonColors.textHead, modifier = Modifier.weight(1f),
                )
                Text(
                    "닫기",
                    fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 15.sp,
                    lineHeight = 18.sp, color = MuyeonColors.textSub,
                    modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onDismiss)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                )
            }
            Column(
                Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp).padding(top = 4.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                Text(
                    sub,
                    fontFamily = customFontFamily, fontSize = 13.sp, lineHeight = 16.sp,
                    color = MuyeonColors.textSub,
                )
                content()
            }
        }
    }
}

/** 받은 채팅처럼 — 왼쪽 34dp 아바타, 위에 이름(11sp), 견적 카드, 글 말풍선(ChatRoomScreen MessageBubble 상대 쪽과 같은 모양). */
@Composable
internal fun QuoteChatThread(
    name: String,
    image: String?,
    cardJson: String,
    text: String,
    onOpenProfile: () -> Unit,
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        QuoteAvatar(
            image, name, 34.dp,
            modifier = Modifier.clip(CircleShape).clickable(onClick = onOpenProfile),
        )
        Spacer(Modifier.width(6.dp))
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                name,
                fontFamily = customFontFamily, fontSize = 11.sp, color = MuyeonColors.textSub,
                modifier = Modifier.clip(RoundedCornerShape(4.dp)).clickable(onClick = onOpenProfile)
                    .padding(start = 4.dp, end = 4.dp),
            )
            QuoteCardBubble(
                cardJson,
                showProviderProfile = true,
                onOpenProvider = { _, _ -> onOpenProfile() },
            )
            Box(
                Modifier.widthIn(max = 260.dp).clip(RoundedCornerShape(18.dp))
                    .background(Color(0xFFF2F2F7))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
            ) {
                Text(
                    text,
                    fontFamily = customFontFamily, fontSize = 15.sp, lineHeight = 20.sp,
                    color = MuyeonColors.textHead,
                )
            }
        }
    }
}

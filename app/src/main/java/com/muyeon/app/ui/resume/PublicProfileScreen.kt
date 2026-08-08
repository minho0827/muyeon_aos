package com.muyeon.app.ui.resume

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.muyeon.app.theme.customFontFamily
import com.muyeon.app.ui.common.MuyeonColors
import com.muyeon.app.ui.quote.QuoteDialog
import com.muyeon.app.ui.quote.QuoteUi
import com.muyeon.app.ui.review.ReviewApi
import com.muyeon.app.ui.review.ReviewList
import com.muyeon.app.ui.review.ReviewOptions
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * 공개 프로필(강사 상세) — iOS `PublicProfileView.swift` 이식.
 *  히어로(사진·이름·평점) + 섹션(소개/전문분야/활동지역/레슨시간/경력/학력/자격증/공연/수상/포트폴리오/후기)
 *  + 하단 CTA(문의하기·견적요청) + 찜/신고.
 *
 *  preview=true 는 **본인이 일반회원 시점으로 확인**하는 모드(공개범위 화면의 미리보기).
 */
@Composable
fun PublicProfileScreen(
    api: ResumeApi,
    reviewApi: ReviewApi,
    userId: Int,
    preview: Boolean = false,
    src: String? = null,
    onClose: () -> Unit,
    onOpenChat: (Int) -> Unit,
    onRequestQuote: () -> Unit,
    onOpenReviews: () -> Unit,
) {
    var profile by remember { mutableStateOf<PublicProfile?>(null) }
    var reviews by remember { mutableStateOf<ReviewList?>(null) }
    var loading by remember { mutableStateOf(true) }
    var scrapped by remember { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }
    var reportOpen by remember { mutableStateOf(false) }
    var toast by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(userId, preview) {
        api.publicProfile(userId, preview, src).onSuccess {
            profile = it
            scrapped = it.scrapped == true
        }
        reviewApi.list(userId).onSuccess { reviews = it }
        loading = false
    }

    Box(Modifier.fillMaxSize().background(MuyeonColors.surface)) {
        if (loading) {
            CircularProgressIndicator(color = MuyeonColors.primary, modifier = Modifier.align(Alignment.Center))
            return@Box
        }
        val p = profile
        if (p == null) {
            Text(
                "프로필을 불러오지 못했어요.",
                fontFamily = customFontFamily, fontSize = 14.sp, color = MuyeonColors.textSub,
                modifier = Modifier.align(Alignment.Center),
            )
            return@Box
        }

        Column(Modifier.fillMaxSize()) {
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                // 히어로 — 대표 사진
                Box(Modifier.fillMaxWidth().height(280.dp).background(Color(0xFFF2F2F7))) {
                    QuoteUi.imageUrl(p.image)?.let {
                        AsyncImage(it, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    }
                    // 뒤로 / 더보기
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CircleIconButton(Icons.AutoMirrored.Filled.ArrowBack, "뒤로", onClick = onClose)
                        Spacer(Modifier.weight(1f))
                        if (!preview) {
                            CircleIconButton(
                                if (scrapped) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                "찜",
                                tint = if (scrapped) MuyeonColors.primary else MuyeonColors.textHead,
                            ) {
                                val next = !scrapped
                                scrapped = next
                                scope.launch { api.setScrap(userId, next).onFailure { scrapped = !next } }
                            }
                            Spacer(Modifier.width(6.dp))
                            Box {
                                CircleIconButton(Icons.Filled.MoreVert, "더보기") { menuOpen = true }
                                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                                    DropdownMenuItem(
                                        text = { Text("신고하기", fontFamily = customFontFamily, fontSize = 14.sp) },
                                        onClick = { menuOpen = false; reportOpen = true },
                                    )
                                }
                            }
                        }
                    }
                    // 본인 열람 안내 — 비공개 항목도 보이는 예외 상황을 명시(iOS selfView).
                    if (p.selfView == true || preview) {
                        Text(
                            if (preview) "미리보기 — 일반회원에게 보이는 화면이에요" else "본인 프로필이라 비공개 항목도 보여요",
                            fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 12.sp,
                            lineHeight = 14.sp, color = Color.White,
                            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                                .background(Color.Black.copy(alpha = 0.55f))
                                .padding(vertical = 8.dp),
                            textAlign = TextAlign.Center,
                        )
                    }
                }

                // 이름 + 평점
                Column(Modifier.padding(horizontal = 20.dp).padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        p.name ?: "강사",
                        fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 22.sp,
                        lineHeight = 26.sp, color = MuyeonColors.textHead,
                    )
                    if ((p.ratingCount ?: 0) > 0) {
                        Row(
                            Modifier.clickable(onClick = onOpenReviews),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Filled.Star, null, tint = MuyeonColors.yellow, modifier = Modifier.size(14.dp))
                            Text(
                                String.format(Locale.KOREA, "%.1f (%d)", p.ratingAvg ?: 0.0, p.ratingCount ?: 0),
                                fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
                                lineHeight = 17.sp, color = MuyeonColors.textHead,
                            )
                        }
                    }
                    p.oneLiner?.takeIf { it.isNotEmpty() }?.let {
                        Text(
                            it,
                            fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp,
                            lineHeight = 20.sp, color = MuyeonColors.textSub,
                        )
                    }
                }

                // 채용 뷰어인데 이용권 없음 → 멤버십 유도(iOS recruiterLocked)
                if (p.recruiterLocked == true) {
                    Text(
                        "이용권이 있으면 연락처와 전체 이력서를 볼 수 있어요.",
                        fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 13.sp,
                        lineHeight = 18.sp, color = MuyeonColors.primary,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp).fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MuyeonColors.primary.copy(alpha = 0.08f))
                            .padding(14.dp),
                    )
                }

                Spacer(Modifier.height(8.dp))
                ProfileSections(p, reviews, onOpenReviews)
                Spacer(Modifier.height(20.dp))
            }

            // 하단 CTA — 기존 채팅방이 있을 때만 [문의하기](iOS chatRoomId 규칙)
            if (!preview) {
                Row(
                    Modifier.fillMaxWidth().background(MuyeonColors.surface)
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    val roomId = p.chatRoomId ?: 0
                    if (roomId > 0) {
                        CtaButton("문의하기", filled = false, modifier = Modifier.weight(1f)) { onOpenChat(roomId) }
                    }
                    CtaButton("견적 요청하기", filled = true, modifier = Modifier.weight(1f), onClick = onRequestQuote)
                }
            }
        }
    }

    if (reportOpen) {
        QuoteDialog(
            title = "이 강사를 신고할까요?",
            message = "허위 정보·부적절한 프로필로 신고합니다. 운영팀이 확인 후 조치해요.",
            confirmText = "신고하기",
            onConfirm = {
                reportOpen = false
                scope.launch {
                    api.report(userId, "INAPPROPRIATE")
                        .onSuccess { toast = "신고가 접수되었어요." }
                        .onFailure { toast = it.message }
                }
            },
            onDismiss = { reportOpen = false },
        )
    }
    toast?.let { msg ->
        QuoteDialog("알림", msg, "확인", onConfirm = { toast = null }, onDismiss = { toast = null })
    }
}

@Composable
private fun ProfileSections(p: PublicProfile, reviews: ReviewList?, onOpenReviews: () -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        p.intro?.takeIf { it.isNotEmpty() }?.let {
            ProfileSection("소개글") {
                Text(it, fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 24.sp, color = MuyeonColors.body)
            }
        }
        (p.fields ?: p.genres)?.takeIf { it.isNotEmpty() }?.let { fs ->
            ProfileSection("전문 분야") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    fs.chunked(3).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            row.forEach { f ->
                                Text(
                                    ResumeOptions.fieldLabel(f),
                                    fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 13.sp,
                                    lineHeight = 16.sp, color = MuyeonColors.primary, maxLines = 1,
                                    modifier = Modifier.clip(RoundedCornerShape(50))
                                        .background(MuyeonColors.primary.copy(alpha = 0.10f))
                                        .padding(horizontal = 12.dp, vertical = 7.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
        p.activeRegion?.takeIf { it.isNotEmpty() }?.let { BodySection("활동 지역", it) }
        if (!p.availableDays.isNullOrEmpty() || !p.availableTimeSlots.isNullOrEmpty()) {
            ProfileSection("레슨 가능 시간") { LessonTimeGrid(p) }
        }
        p.careers?.takeIf { it.isNotEmpty() }?.let { cs ->
            ProfileSection("경력 (요약)") {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    ResumeOptions.careerLabel(p.career).takeIf { it.isNotEmpty() }?.let { bucket ->
                        Text(
                            bucket,
                            fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 14.sp,
                            lineHeight = 17.sp, color = MuyeonColors.primary,
                        )
                    }
                    cs.forEach { c ->
                        val line = listOf(c.academy, c.position, c.period).filter { it.isNotEmpty() }.joinToString(" · ")
                        Text(line, fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp, color = MuyeonColors.body)
                    }
                }
            }
        }
        p.educations?.takeIf { it.isNotEmpty() }?.let { edus ->
            ProfileSection("학력") {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    edus.forEach { e ->
                        val text = if (e.period.isEmpty()) e.school else "${e.school} (${e.period})"
                        Text(text, fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp, color = MuyeonColors.body)
                    }
                }
            }
        }
        p.certificates?.takeIf { it.isNotEmpty() }?.let { LineListSection("자격증", it) }
        p.performances?.takeIf { it.isNotEmpty() }?.let { perfs ->
            ProfileSection("공연 이력") {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    perfs.forEach { pf ->
                        val head = listOf(pf.year, pf.title).filter { it.isNotEmpty() }.joinToString(" ")
                        val line = listOf(head, pf.role).filter { it.isNotEmpty() }.joinToString(" — ")
                        Text(line, fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp, color = MuyeonColors.body)
                    }
                }
            }
        }
        p.awards?.takeIf { it.isNotEmpty() }?.let { LineListSection("수상 이력", it) }
        p.images?.takeIf { it.isNotEmpty() }?.let { imgs ->
            ProfileSection("포트폴리오") {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    imgs.chunked(3).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            row.forEach { url ->
                                AsyncImage(
                                    QuoteUi.imageUrl(url), null, contentScale = ContentScale.Crop,
                                    modifier = Modifier.weight(1f).aspectRatio(1f).clip(RoundedCornerShape(10.dp)),
                                )
                            }
                            repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
            }
        }
        // 후기 요약 — 태그 배지 + 상위 3개 + 전체보기
        reviews?.items?.takeIf { it.isNotEmpty() }?.let { items ->
            ProfileSection("레슨 후기 ${reviews.count ?: items.size}개") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    val top = reviews.tagCounts.entries.sortedByDescending { it.value }.take(4)
                    if (top.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            top.chunked(2).forEach { row ->
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    row.forEach { (tag, n) ->
                                        Text(
                                            "${ReviewOptions.tagLabel(tag)} $n",
                                            fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 12.sp,
                                            lineHeight = 14.sp, color = MuyeonColors.primary, maxLines = 1,
                                            modifier = Modifier.clip(RoundedCornerShape(50))
                                                .background(MuyeonColors.primary.copy(alpha = 0.08f))
                                                .padding(horizontal = 10.dp, vertical = 5.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }
                    items.take(3).forEach { r ->
                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Star, null, tint = MuyeonColors.primary, modifier = Modifier.size(12.dp))
                                Text(
                                    "${r.rating ?: 0}",
                                    fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 12.sp,
                                    lineHeight = 14.sp, color = MuyeonColors.textHead,
                                )
                                Text(
                                    r.reviewerName ?: "회원",
                                    fontFamily = customFontFamily, fontSize = 12.sp, lineHeight = 14.sp,
                                    color = MuyeonColors.secondary,
                                )
                            }
                            r.content?.takeIf { it.isNotBlank() }?.let {
                                Text(it, fontFamily = customFontFamily, fontSize = 13.sp, lineHeight = 19.sp, color = MuyeonColors.body, maxLines = 3)
                            }
                        }
                    }
                    if (items.size > 3) {
                        Text(
                            "후기 전체 보기",
                            fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 13.sp,
                            lineHeight = 16.sp, color = MuyeonColors.primary,
                            modifier = Modifier.clickable(onClick = onOpenReviews),
                        )
                    }
                }
            }
        }
    }
}

/** 레슨 가능 시간 그리드 — 6버킷을 오전/오후/저녁 3행으로(iOS gridRows). */
@Composable
private fun LessonTimeGrid(p: PublicProfile) {
    val days = p.availableDays ?: emptyList()
    val slots = p.availableTimeSlots ?: emptyList()
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (days.isNotEmpty()) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                ResumeOptions.weekDays.forEach { d ->
                    val on = days.contains(d)
                    Text(
                        d,
                        fontFamily = customFontFamily,
                        fontWeight = if (on) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 12.sp, lineHeight = 15.sp, textAlign = TextAlign.Center,
                        color = if (on) Color.White else MuyeonColors.secondary,
                        modifier = Modifier.weight(1f).clip(CircleShape)
                            .background(if (on) MuyeonColors.primary else Color(0xFFF2F2F7))
                            .padding(vertical = 7.dp),
                    )
                }
            }
        }
        ResumeOptions.gridRows.forEach { (label, keys) ->
            val on = keys.any { slots.contains(it) }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    label,
                    fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 13.sp,
                    lineHeight = 16.sp, color = MuyeonColors.textSub, modifier = Modifier.width(36.dp),
                )
                Text(
                    if (on) keys.filter { slots.contains(it) }.joinToString(" · ") { ResumeOptions.timeSlotLabel(it) } else "—",
                    fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 13.sp,
                    lineHeight = 16.sp, color = if (on) MuyeonColors.textHead else MuyeonColors.secondary,
                )
            }
        }
    }
}

@Composable
private fun ProfileSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(top = 22.dp)) {
        Text(
            title,
            fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp,
            lineHeight = 19.sp, color = MuyeonColors.textHead,
            modifier = Modifier.padding(bottom = 10.dp),
        )
        content()
        HorizontalDivider(Modifier.padding(top = 20.dp), color = Color(0xFFF4F4F4))
    }
}

@Composable
private fun BodySection(title: String, text: String) {
    ProfileSection(title) {
        Text(text, fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp, color = MuyeonColors.body)
    }
}

/** 여러 줄(줄당 1건) 텍스트 — 자격증·수상. */
@Composable
private fun LineListSection(title: String, text: String) {
    ProfileSection(title) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            text.split("\n").map { it.trim() }.filter { it.isNotEmpty() }.forEach {
                Text("· $it", fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp, color = MuyeonColors.body)
            }
        }
    }
}

@Composable
private fun CircleIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    desc: String,
    tint: Color = MuyeonColors.textHead,
    onClick: () -> Unit,
) {
    Box(
        Modifier.size(36.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.9f)).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, desc, tint = tint, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun CtaButton(text: String, filled: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Text(
        text,
        fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp, lineHeight = 18.sp,
        color = if (filled) Color.White else MuyeonColors.primary, textAlign = TextAlign.Center,
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (filled) MuyeonColors.primary else MuyeonColors.primary.copy(alpha = 0.08f))
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
    )
}

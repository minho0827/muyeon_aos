package com.muyeon.app.ui.resume

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import com.muyeon.app.ui.quote.QuoteDialog
import com.muyeon.app.ui.quote.QuoteNavBar
import kotlinx.coroutines.launch

/**
 * 공개 범위 설정 — iOS `FieldVisibilityView.swift` 1:1.
 *  이력서 기반 공개 항목 토글 + 항목단위 상세 선택(경력/공연) + [미리보기 확인하기].
 *  토글 변경은 **즉시 PATCH**(공개 프로필 미러 반영) — iOS 와 동일하게 저장 버튼이 없다.
 */
private const val INTRO_FLAG_KEY = "fieldVisibilityIntro.v1"
private const val INTRO_LOCAL_KEY = "muyeon.tut.fieldVisibilityIntro.v1"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FieldVisibilityScreen(
    api: ResumeApi,
    mode: ResumeMode,
    prefs: android.content.SharedPreferences,
    onClose: () -> Unit,
    onPreview: () -> Unit,
) {
    var flags by remember { mutableStateOf(FieldVisibilityFlags()) }
    var profileHidden by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(true) }
    var careers by remember { mutableStateOf<List<CareerItem>>(emptyList()) }
    var performances by remember { mutableStateOf<List<PerfItem>>(emptyList()) }
    var introOpen by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var detailSheet by remember { mutableStateOf<String?>(null) }   // "career" | "performance"
    val scope = rememberCoroutineScope()

    fun persist() {
        scope.launch { api.setVisibility(flags, profileHidden).onFailure { errorMessage = it.message } }
    }

    LaunchedEffect(Unit) {
        api.getVisibility().onSuccess { (f, hidden) -> flags = f; profileHidden = hidden }
        // 기본 이력서의 경력/공연 — '상세 >' 선택 시트 데이터
        api.list().getOrNull()?.let { list ->
            val def = list.firstOrNull { it.isDefault } ?: list.firstOrNull()
            def?.let { d ->
                api.getOne(d.id).getOrNull()?.let { dto ->
                    careers = dto.data.careers ?: emptyList()
                    performances = dto.data.performances ?: emptyList()
                }
            }
        }
        // 최초 1회 안내 — 로컬(빠름) 우선, 미확인이면 계정 플래그 확인(재설치·기기변경 대응)
        if (!prefs.getBoolean(INTRO_LOCAL_KEY, false)) {
            if (api.uiFlagSeen(INTRO_FLAG_KEY)) prefs.edit().putBoolean(INTRO_LOCAL_KEY, true).apply()
            else introOpen = true
        }
        loading = false
    }

    Column(Modifier.fillMaxSize().background(MuyeonColors.surface)) {
        QuoteNavBar(title = "공개 범위 설정", onBack = onClose)

        if (loading) {
            Box(Modifier.weight(1f).fillMaxWidth(), Alignment.Center) {
                CircularProgressIndicator(color = MuyeonColors.primary)
            }
        } else {
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
            ) {
                GuideBox()
                Text(
                    "공개 정보 설정",
                    fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp,
                    lineHeight = 19.sp, color = MuyeonColors.textHead,
                    modifier = Modifier.padding(top = 22.dp, bottom = 4.dp),
                )

                // 문서 단위 전체 비공개(강사 목록 숨김) — 개별 토글보다 우선한다.
                VisibilityRow(
                    title = if (mode.isDancer) "무용수 목록에서 숨기기" else "강사 목록에서 숨기기",
                    caption = "켜면 검색·목록·견적 매칭에서 모두 빠져요",
                    on = profileHidden,
                    onChange = { profileHidden = it; persist() },
                )
                HorizontalDivider(Modifier.padding(vertical = 6.dp), color = Color(0xFFF7F7F7))

                @Composable
                fun toggle(title: String, key: String, caption: String? = null, sub: String? = null, detail: (() -> Unit)? = null) {
                    VisibilityRow(
                        title = title, caption = caption, sub = sub,
                        on = flags.isOn(key),
                        enabled = !profileHidden,
                        onDetail = detail,
                        onChange = { flags = flags.copy().also { f -> f.flags.putAll(flags.flags); f.set(key, it) }; persist() },
                    )
                }

                toggle("프로필 사진", "photo")
                toggle("이름", "name")
                toggle("한줄 소개", "oneLiner")
                toggle(
                    if (mode.isDancer) "전공·장르" else "전문 분야", "fields",
                    caption = if (mode.isDancer) "끄면 무용수 검색·필터에서도 제외돼요" else "끄면 강사 검색·필터에서도 제외돼요",
                )
                toggle("활동 지역", "region", caption = "끄면 지역 견적 매칭에서도 제외돼요")
                if (mode.isDancer) {
                    toggle("신체정보 (성별·키)", "body", caption = "끄면 무용수 검색·필터에서도 제외돼요")
                } else {
                    toggle("레슨 가능 시간", "lessonTime")
                    toggle("경력 (요약)", "career", detail = if (careers.isEmpty()) null else ({ detailSheet = "career" }))
                }
                toggle("학력", "education")
                if (!mode.isDancer) toggle("자격증", "certificate")
                toggle("공연 이력", "performance", detail = if (performances.isEmpty()) null else ({ detailSheet = "performance" }))
                toggle("수상 이력", "award")
                toggle("SNS 링크", "sns")
                toggle("연락처", "contact", sub = "직접 문의만 가능")

                WarnBox(Modifier.padding(top = 18.dp))
                Spacer(Modifier.height(12.dp))
            }

            Text(
                "미리보기 확인하기",
                fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp,
                lineHeight = 19.sp, color = Color.White, textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(horizontal = 20.dp).padding(top = 8.dp, bottom = 12.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MuyeonColors.primary)
                    .clickable(onClick = onPreview)
                    .padding(vertical = 16.dp),
            )
        }
    }

    // 항목단위 상세 선택 — 켠 항목만 공개(iOS careerItems / performanceItems).
    detailSheet?.let { kind ->
        val labels: List<Pair<Int, String>> = if (kind == "career") {
            careers.mapIndexed { i, c -> i to listOfNotNull(c.academy.ifEmpty { null }, c.position.ifEmpty { null }).joinToString(" · ") }
        } else {
            performances.mapIndexed { i, p -> i to listOfNotNull(p.year.ifEmpty { null }, p.title.ifEmpty { null }).joinToString(" · ") }
        }
        val selected = (if (kind == "career") flags.careerItems else flags.performanceItems) ?: labels.map { it.first }

        ModalBottomSheet(onDismissRequest = { detailSheet = null }) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 24.dp)) {
                Text(
                    if (kind == "career") "공개할 경력 선택" else "공개할 공연 선택",
                    fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 17.sp,
                    lineHeight = 20.sp, color = MuyeonColors.textHead,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                labels.forEach { (idx, label) ->
                    val on = selected.contains(idx)
                    Row(
                        Modifier.fillMaxWidth().clickable {
                            val next = if (on) selected - idx else selected + idx
                            flags = flags.copy().also { f ->
                                f.flags.putAll(flags.flags)
                                f.careerItems = flags.careerItems
                                f.performanceItems = flags.performanceItems
                                if (kind == "career") f.careerItems = next else f.performanceItems = next
                            }
                            persist()
                        }.padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            label.ifEmpty { "(제목 없음)" },
                            fontFamily = customFontFamily, fontSize = 15.sp, lineHeight = 18.sp,
                            color = MuyeonColors.textHead, modifier = Modifier.weight(1f),
                        )
                        if (on) Icon(Icons.Filled.Check, null, tint = MuyeonColors.primary, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }

    if (introOpen) {
        // 최초 1회 '이렇게 달라져요!' 안내 — 확인 시 로컬 + 계정 플래그 양쪽 기록.
        QuoteDialog(
            title = "이렇게 달라져요!",
            message = "여기서 켠 항목만 일반회원에게 보여요.\n끈 항목은 검색·매칭에서도 빠질 수 있으니 확인해 주세요.",
            confirmText = "확인",
            onConfirm = {
                introOpen = false
                prefs.edit().putBoolean(INTRO_LOCAL_KEY, true).apply()
                scope.launch { api.markUiFlag(INTRO_FLAG_KEY) }
            },
            onDismiss = { introOpen = false },
        )
    }
    errorMessage?.let { msg ->
        QuoteDialog("오류", msg, "확인", onConfirm = { errorMessage = null }, onDismiss = { errorMessage = null })
    }
}

@Composable
private fun VisibilityRow(
    title: String,
    caption: String? = null,
    sub: String? = null,
    on: Boolean,
    enabled: Boolean = true,
    onDetail: (() -> Unit)? = null,
    onChange: (Boolean) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    title,
                    fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 15.sp,
                    lineHeight = 18.sp, color = MuyeonColors.textHead,
                )
                sub?.let {
                    Text(
                        it,
                        fontFamily = customFontFamily, fontSize = 11.sp, lineHeight = 13.sp,
                        color = MuyeonColors.secondary,
                    )
                }
            }
            caption?.let {
                Text(
                    it,
                    fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 12.sp,
                    lineHeight = 14.sp, color = MuyeonColors.textSub,
                )
            }
        }
        if (onDetail != null) {
            Row(
                Modifier.clickable(onClick = onDetail).padding(end = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "상세",
                    fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 13.sp,
                    lineHeight = 16.sp, color = MuyeonColors.textSub,
                )
                Icon(Icons.Filled.KeyboardArrowRight, null, tint = MuyeonColors.chevron, modifier = Modifier.size(14.dp))
            }
        }
        Switch(
            checked = on, onCheckedChange = onChange, enabled = enabled,
            colors = SwitchDefaults.colors(checkedTrackColor = MuyeonColors.primary),
        )
    }
}

@Composable
private fun GuideBox() {
    Column(
        Modifier
            .padding(top = 12.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MuyeonColors.primary.copy(alpha = 0.07f))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            "공개 프로필은 기본 이력서를 따라가요",
            fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 14.sp,
            lineHeight = 17.sp, color = MuyeonColors.textHead,
        )
        Text(
            "여기서 켠 항목만 일반회원에게 보여요. 변경하면 바로 저장돼요.",
            fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 12.sp,
            lineHeight = 16.sp, color = MuyeonColors.textSub,
        )
    }
}

@Composable
private fun WarnBox(modifier: Modifier = Modifier) {
    Text(
        "전공·활동지역을 끄면 검색·필터와 견적 매칭에서도 제외돼요. 문의를 받고 싶다면 켜 두세요.",
        fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 12.sp,
        lineHeight = 17.sp, color = MuyeonColors.textSub,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFF7F7F7))
            .padding(14.dp),
    )
}

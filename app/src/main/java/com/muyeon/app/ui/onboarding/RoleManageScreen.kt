package com.muyeon.app.ui.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.muyeon.app.theme.customFontFamily
import com.muyeon.app.ui.common.MuyeonColors
import com.muyeon.app.ui.quote.QuoteUi
import org.json.JSONObject

/**
 * 회원유형 관리 — iOS `Onboarding/Role/RoleManageView.swift` **1:1 이식**.
 *  계정 1개 = 역할 N개(대표역할 없음). '전환'이 아니라 유형 '관리'.
 *
 * 상호작용 규약(iOS 와 동일 — 임의로 바꾸면 두 앱이 달라진다):
 *  - **탭** = 열린 카드면 활동 유형 전환, 잠긴 카드면 관리 시트
 *  - **길게 누르기** = 관리 시트(추가·인증·해제)
 *  - HOBBY 카드는 활동 유형 `GENERAL` 에 대응한다(코드가 다르므로 매핑 필수)
 *
 * ⚠️ 실제 API 는 **웹 콜백**이 수행한다(`__onRoleManageAdd`/`__onRoleRemove`/
 *   `__onRoleManageVerify`/`__onActiveTypeChanged`). 이 화면은 UI + 낙관적 갱신만 담당한다.
 *   ★ 추가·해제·유형선택은 콜백을 보낸 뒤에도 화면을 닫지 않는다 — 연속으로 조작할 수 있어야 한다.
 *     인증 서류 제출만 예외로, 제출 뒤 이 화면까지 함께 닫는다(OnboardingActivity 참고).
 *
 * @param business 인증(서류 제출 → 관리자 승인) 후에만 부여되는 유형인가.
 *  ★ 일반회원(HOBBY)을 뺀 다섯 유형은 전부 인증 대상이다 — 강사·무용수도 예외가 아니다.
 *    예전엔 이 둘이 false 라 "바로 추가할 수 있는 회원유형입니다" + [회원유형 추가] 가 떠서
 *    서류 없이 보유로 표시됐다(iOS `RoleManageView.options` 와 같은 값이어야 한다).
 */
data class RoleOption(val code: String, val emoji: String, val title: String, val business: Boolean)

private val ROLE_OPTIONS = listOf(
    RoleOption("TEACHER", "🩰", "강사", true),
    RoleOption("DANCER", "💃", "무용수", true),
    RoleOption("ACADEMY", "🏫", "학원·원장", true),
    RoleOption("TEAM", "🎭", "공연팀·기획자", true),
    RoleOption("SPACE", "🏢", "공간보유자", true),
    RoleOption("HOBBY", "✨", "일반회원", false),
)

// iOS 와 동일 수치 — 곡선 패널이 히어로 이미지를 파고드는 깊이/단차.
private val HERO_HEIGHT = 430.dp
private val PANEL_OVERLAP = 140.dp
private val CURVE_DEPTH = 42.dp

/** 웹이 브릿지로 전달하는 역할 데이터. */
data class RoleManagePayload(
    val granted: Set<String>,
    val status: Map<String, String>,
    val activeType: String,
    /** 역할 → 이미 제출한 서류 URL. 재제출 화면에서 미리보기로 다시 채워 준다. */
    val documents: Map<String, List<String>> = emptyMap(),
) {
    companion object {
        /**
         * @param json 웹 `data.roles` — `{"held":[...],"roles":[{role,granted,status,documents}]}` **문자열**
         * @param activeType `data.activeType` — roles JSON 안이 아니라 바깥 평평한 키로 온다
         *
         * 서버가 함께 주는 `needsVerification` 은 읽지 않는다 — 인증 대상을 '추가'만 할 수 있고
         *   해제하지는 못하는데, 일반회원을 뺀 모든 유형이 이미 ROLE_OPTIONS 에서 인증 대상이라
         *   합치면 결과가 같다. (iOS 는 서버가 새 유형을 늘릴 때를 대비해 합집합을 취한다)
         *
         * ★ 예전엔 data 객체 전체를 넘겨받아 여기서 held/roles 를 찾다 실패했다.
         *   그러면 보유·인증상태가 통째로 비어 사업 역할이 전부 자물쇠로 보인다(2026-08-13 수정).
         */
        fun parse(json: String?, activeType: String = "GENERAL"): RoleManagePayload {
            if (json.isNullOrEmpty()) return RoleManagePayload(emptySet(), emptyMap(), activeType)
            return runCatching {
                val o = JSONObject(json)
                val granted = mutableSetOf<String>()
                val status = mutableMapOf<String, String>()
                val documents = mutableMapOf<String, List<String>>()
                o.optJSONArray("held")?.let { arr -> (0 until arr.length()).forEach { granted.add(arr.optString(it)) } }
                o.optJSONArray("roles")?.let { arr ->
                    (0 until arr.length()).forEach { i ->
                        val r = arr.optJSONObject(i) ?: return@forEach
                        val code = r.optString("role")
                        if (r.optBoolean("granted", false)) granted.add(code)
                        r.optString("status").takeIf { it.isNotEmpty() }?.let { status[code] = it }
                        r.optJSONArray("documents")?.let { da ->
                            val urls = (0 until da.length()).mapNotNull {
                                da.optJSONObject(it)?.optString("url")?.ifEmpty { null }
                            }
                            if (urls.isNotEmpty()) documents[code] = urls
                        }
                    }
                }
                RoleManagePayload(granted, status, activeType.ifEmpty { "GENERAL" }, documents)
            }.getOrDefault(RoleManagePayload(emptySet(), emptyMap(), activeType))
        }
    }
}

@Composable
fun RoleManageScreen(
    payload: RoleManagePayload,
    heroImageUrl: String?,
    onClose: () -> Unit,
    onAdd: (String) -> Unit,          // 개인 추가 / 사업 승인완료 → __onRoleManageAdd
    onRemove: (String) -> Unit,       // 해제 → __onRoleRemove
    // 사업 인증 → 인증 화면. 서류를 제출했으면 콜백(true)으로 낙관적 '심사중' 전환(iOS 동일).
    onVerify: (String, (Boolean) -> Unit) -> Unit,
    onSelectActive: (String) -> Unit, // 활동 유형 선택 → __onActiveTypeChanged
) {
    var granted by remember { mutableStateOf(payload.granted) }
    var status by remember { mutableStateOf(payload.status) }
    var activeSel by remember { mutableStateOf(payload.activeType) }
    var pending by remember { mutableStateOf<RoleOption?>(null) }

    Box(Modifier.fillMaxSize().background(MuyeonColors.surface)) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            // 히어로 위에 곡선 패널을 겹친다 — 대비로 굴곡이 보이고 배경 이미지가 크게 남는다.
            Box(Modifier.fillMaxWidth()) {
                HeroHeader(heroImageUrl)
                Column(Modifier.padding(top = HERO_HEIGHT - PANEL_OVERLAP)) {
                    ProfilePanel(
                        granted = granted,
                        status = status,
                        activeSel = activeSel,
                        onTap = { opt ->
                            if (isActiveEligible(opt, granted, status)) {
                                val code = activeCode(opt)
                                activeSel = code
                                onSelectActive(code)
                            } else {
                                pending = opt
                            }
                        },
                        onLongPress = { pending = it },
                    )
                }
            }
        }

        RoleCloseButton(onClose, Modifier.align(Alignment.TopStart).padding(top = 8.dp, start = 14.dp))

        pending?.let { opt ->
            RoleActionSheet(
                opt = opt,
                isGranted = granted.contains(opt.code),
                st = status[opt.code] ?: "NONE",
                isLastHeld = granted.size <= 1,
                onDismiss = { pending = null },
                onRemoveClick = {
                    granted = granted - opt.code
                    pending = null
                    onRemove(opt.code)
                },
                onAddClick = {
                    granted = granted + opt.code
                    pending = null
                    onAdd(opt.code)
                },
                onVerifyClick = {
                    val code = opt.code
                    pending = null
                    // ★ 심사중 표시는 '서류 제출 완료' 콜백을 받은 뒤에만. 인증 화면을 그냥 닫았는데
                    //   심사중으로 보이면 사용자가 제출한 줄 알고 기다리게 된다.
                    onVerify(code) { submitted -> if (submitted) status = status + (code to "PENDING") }
                },
            )
        }
    }
}

// ── 상태 판정 (iOS activeCode / isActiveEligible / isLocked / badge 1:1) ──

/** HOBBY 카드 = 활동 유형 GENERAL(고객). 코드가 달라 매핑하지 않으면 영원히 선택되지 않는다. */
private fun activeCode(opt: RoleOption): String = if (opt.code == "HOBBY") "GENERAL" else opt.code

/** 활동 유형으로 고를 수 있는가 — 일반(항상) / 보유 개인역할 / 승인완료 사업역할. */
private fun isActiveEligible(opt: RoleOption, granted: Set<String>, status: Map<String, String>): Boolean {
    if (opt.code == "HOBBY") return true
    if (!granted.contains(opt.code)) return false
    return !opt.business || status[opt.code] == "APPROVED"
}

/** 잠금 = 사업역할 & 미보유 & 승인 이력 없음. */
private fun isLocked(opt: RoleOption, granted: Set<String>, status: Map<String, String>): Boolean {
    if (!opt.business || granted.contains(opt.code)) return false
    return (status[opt.code] ?: "NONE") != "APPROVED"
}

private fun badge(
    opt: RoleOption, active: Boolean, granted: Set<String>, status: Map<String, String>,
): Pair<String, Color> {
    if (active) return "● 활동중" to MuyeonColors.primary
    if (granted.contains(opt.code)) return "보유" to MuyeonColors.primary
    if (opt.business) return when (status[opt.code]) {
        "PENDING" -> "심사중" to MuyeonColors.primary
        "REJECTED" -> "반려" to MuyeonColors.danger
        "APPROVED" -> "인증완료" to MuyeonColors.textSub
        else -> "인증 필요" to MuyeonColors.textSub
    }
    if (opt.code == "HOBBY") return "" to MuyeonColors.textSub   // 일반(고객)은 미보유 표기 안 함
    return "미보유" to MuyeonColors.tileLocked
}

// ── 구성 뷰 ──

@Composable
private fun HeroHeader(heroImageUrl: String?) {
    Box(
        // ★ 바탕을 항상 어둡게 깐다(iOS AsyncImage 의 default phase = color101116 과 동일).
        //   이미지가 없을 때만이 아니라 **로딩 중·실패에도** 이 색이 보여야 한다.
        //   비워두면 흰 배경이 비쳐 그 위의 흰 문구가 통째로 사라진다.
        Modifier.fillMaxWidth().height(HERO_HEIGHT).background(MuyeonColors.textHead),
        contentAlignment = Alignment.BottomCenter,
    ) {
        QuoteUi.imageUrl(heroImageUrl)?.let {
            AsyncImage(it, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        }
        // 하단으로 갈수록 어두워져 흰 문구가 읽힌다.
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    0.5f to Color.Transparent,
                    1f to Color.Black.copy(alpha = 0.35f),
                ),
            ),
        )
        Text(
            "회원유형을 선택하세요",
            fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp,
            lineHeight = 18.sp, color = Color.White,
            modifier = Modifier
                // 곡선 정점(중앙) 위 10dp — iOS 와 같은 기준.
                .padding(bottom = CURVE_DEPTH + 10.dp)
                .clip(RoundedCornerShape(50))
                .background(Color.Black.copy(alpha = 0.28f))
                .padding(horizontal = 30.dp, vertical = 14.dp),
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ProfilePanel(
    granted: Set<String>,
    status: Map<String, String>,
    activeSel: String,
    onTap: (RoleOption) -> Unit,
    onLongPress: (RoleOption) -> Unit,
) {
    val curve = remember { PanelTopCurveShape(PANEL_OVERLAP, CURVE_DEPTH) }
    Column(
        Modifier.fillMaxWidth()
            .shadow(8.dp, curve, clip = false)
            .background(MuyeonColors.surface, curve),
    ) {
        Column(
            Modifier.padding(horizontal = 20.dp)
                .padding(top = PANEL_OVERLAP - 36.dp),   // 첫 줄이 곡선 모서리에 살짝 걸치도록
            verticalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            ROLE_OPTIONS.chunked(3).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    row.forEach { opt ->
                        RoleTile(
                            opt = opt,
                            granted = granted,
                            status = status,
                            active = activeSel == activeCode(opt),
                            modifier = Modifier.weight(1f).combinedClickable(
                                onClick = { onTap(opt) },
                                onLongClick = { onLongPress(opt) },
                            ),
                        )
                    }
                    repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
        Text(
            "카드를 탭하면 그 유형으로 활동이 전환돼요(하단 탭·홈이 바뀜).\n" +
                "길게 누르면 추가·인증·해제. 일반회원을 제외한 유형은 승인 후 부여됩니다.",
            fontFamily = customFontFamily, fontSize = 12.sp, lineHeight = 17.sp,
            color = MuyeonColors.textSub, textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
                .padding(top = 24.dp, start = 24.dp, end = 24.dp, bottom = 40.dp),
        )
    }
}

@Composable
private fun RoleTile(
    opt: RoleOption,
    granted: Set<String>,
    status: Map<String, String>,
    active: Boolean,
    modifier: Modifier = Modifier,
) {
    val isGranted = granted.contains(opt.code)
    val locked = isLocked(opt, granted, status)
    val (label, labelColor) = badge(opt, active, granted, status)
    val fill = when {
        active -> MuyeonColors.primary
        isGranted -> MuyeonColors.tileHeld.copy(alpha = 0.6f)
        locked -> MuyeonColors.tileLocked   // 회색으로 감싸 '미오픈'
        else -> MuyeonColors.tileIdle
    }
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
            Modifier.fillMaxWidth().aspectRatio(1f)
                .clip(RoundedCornerShape(18.dp))
                .background(fill)
                .then(
                    if (active || isGranted) {
                        Modifier.border(if (active) 3.dp else 2.dp, MuyeonColors.primary, RoundedCornerShape(18.dp))
                    } else Modifier,
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (locked) {
                Icon(Icons.Filled.Lock, "잠김", tint = Color.White, modifier = Modifier.size(26.dp))
            } else {
                Text(opt.emoji, fontSize = 34.sp, lineHeight = 40.sp)
            }
        }
        Text(
            opt.title,
            fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 14.sp, lineHeight = 17.sp,
            color = if (active) MuyeonColors.primary else MuyeonColors.textHead,
            textAlign = TextAlign.Center, maxLines = 1,
        )
        // 빈 값도 자리를 유지해야 타일 높이가 어긋나지 않는다(iOS 동일).
        Text(
            label.ifEmpty { " " },
            fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 14.sp,
            color = labelColor, modifier = Modifier.height(14.dp),
        )
    }
}

@Composable
private fun RoleCloseButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier.size(40.dp).clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.4f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Filled.Close, "닫기", tint = Color.White, modifier = Modifier.size(15.dp))
    }
}

/**
 * 관리 시트 — iOS 커스텀 시트 1:1(Material3 ModalBottomSheet 아님).
 * 딤 40% + 상단 라운드 28 + 드래그 핸들 + 이모지/제목/설명 + 주 액션 + 닫기.
 */
@Composable
private fun RoleActionSheet(
    opt: RoleOption,
    isGranted: Boolean,
    st: String,
    isLastHeld: Boolean,
    onDismiss: () -> Unit,
    onRemoveClick: () -> Unit,
    onAddClick: () -> Unit,
    onVerifyClick: () -> Unit,
) {
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }

    Box(
        Modifier.fillMaxSize()
            .background(Color.Black.copy(alpha = 0.4f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.BottomCenter,
    ) {
        AnimatedVisibility(shown, enter = slideInVertically { it }, exit = slideOutVertically { it }) {
            Column(
                Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                    .background(MuyeonColors.surface)
                    // 시트 본문 탭이 딤(닫기)으로 새지 않도록 흡수.
                    .clickable(enabled = false) {}
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 34.dp)
                    .navigationBarsPadding(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Spacer(Modifier.height(10.dp))
                Box(Modifier.width(40.dp).height(4.dp).clip(RoundedCornerShape(50))
                    .background(MuyeonColors.tileLocked))
                Text(opt.emoji, fontSize = 40.sp, lineHeight = 46.sp)
                Text(
                    opt.title,
                    fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 18.sp,
                    lineHeight = 21.sp, color = MuyeonColors.textHead,
                )
                Text(
                    sheetDesc(opt, isGranted, st),
                    fontFamily = customFontFamily, fontSize = 13.sp, lineHeight = 19.sp,
                    color = MuyeonColors.textSub, textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 8.dp),
                )
                Spacer(Modifier.height(4.dp))
                when {
                    isGranted && isLastHeld ->
                        RoleCtaButton("기본 유형은 해제할 수 없어요", RoleCtaStyle.GHOST, enabled = false) {}
                    isGranted ->
                        RoleCtaButton("회원유형 해제", RoleCtaStyle.DESTRUCTIVE, onClick = onRemoveClick)
                    !opt.business ->
                        RoleCtaButton("회원유형 추가", onClick = onAddClick)
                    st == "APPROVED" ->
                        RoleCtaButton("회원유형 전환", onClick = onAddClick)
                    else -> RoleCtaButton(
                        if (st == "PENDING" || st == "REJECTED") "서류 다시 제출" else "인증하고 추가",
                        onClick = onVerifyClick,
                    )
                }
                RoleCtaButton("닫기", RoleCtaStyle.GHOST, onClick = onDismiss)
            }
        }
    }
}

private fun sheetDesc(opt: RoleOption, isGranted: Boolean, st: String): String {
    if (isGranted) return "현재 보유 중인 회원유형입니다."
    if (!opt.business) return "바로 추가할 수 있는 회원유형입니다."
    return when (st) {
        "APPROVED" -> "이미 인증이 완료되어 재인증 없이 바로 전환할 수 있어요."
        "PENDING" -> "인증 심사 중입니다. 서류를 다시 제출할 수 있어요."
        "REJECTED" -> "인증이 반려되었어요. 서류를 다시 제출해 주세요."
        else -> "서류 제출 후 관리자 승인 시 부여되는 회원유형입니다."
    }
}

private enum class RoleCtaStyle { PRIMARY, DESTRUCTIVE, GHOST }

/** iOS `RoleCtaButton` 이식 — height 56, radius 28, primary 는 주황 그라데이션. */
@Composable
private fun RoleCtaButton(
    title: String,
    style: RoleCtaStyle = RoleCtaStyle.PRIMARY,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(28.dp)
    val fg = when (style) {
        RoleCtaStyle.PRIMARY -> Color.White
        RoleCtaStyle.DESTRUCTIVE -> MuyeonColors.danger
        RoleCtaStyle.GHOST -> MuyeonColors.textSub
    }
    Box(
        Modifier.fillMaxWidth().height(56.dp).alpha(if (enabled) 1f else 0.4f)
            .clip(shape)
            .then(
                when (style) {
                    RoleCtaStyle.PRIMARY -> Modifier.background(
                        Brush.horizontalGradient(listOf(MuyeonColors.primary, MuyeonColors.primaryDeep)),
                    )
                    RoleCtaStyle.DESTRUCTIVE -> Modifier.background(MuyeonColors.danger.copy(alpha = 0.08f))
                    RoleCtaStyle.GHOST -> Modifier.background(MuyeonColors.surface)
                        .border(1.dp, MuyeonColors.border, shape)
                },
            )
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            title,
            fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 17.sp,
            lineHeight = 20.sp, color = fg,
        )
    }
}

/**
 * 프로필 패널의 곡선 상단 — iOS `PanelTopCurve` 이식.
 * 중앙이 위로 볼록(∩). 양쪽 끝은 y=base, 중앙은 base-depth 로 솟는다.
 */
private class PanelTopCurveShape(private val base: Dp, private val depth: Dp) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val b = with(density) { base.toPx() }
        val d = with(density) { depth.toPx() }
        val ctrlY = b - d * 4f / 3f   // 중앙 y 가 base-depth 가 되도록 제어점 보정(3차 베지어)
        val p = Path().apply {
            moveTo(0f, b)
            cubicTo(0f, ctrlY, size.width, ctrlY, size.width, b)
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            close()
        }
        return Outline.Generic(p)
    }
}

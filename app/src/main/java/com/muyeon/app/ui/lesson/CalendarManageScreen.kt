package com.muyeon.app.ui.lesson

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
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
 * 내 캘린더(관리) — iOS `CalendarManageView` + `CalendarEditSheet` 1:1 이식.
 *
 *  목록: [기본](편집 불가 안내) + 내가 만든 캘린더(탭 → 편집) + 점선 카드 [새로운 캘린더 만들기].
 *  편집기: 만들기일 때만 프리셋 6종 → 이름 → 12색 팔레트 → (편집일 때) 삭제.
 *
 *  iOS 는 시트를 겹쳐 띄우지만 AOS 는 같은 화면에서 편집기 상태로 전환한다(백 스택 관리 단순화).
 *  뒤로가기는 편집기 → 목록 → 화면 종료 순으로 한 단계씩 닫힌다.
 */
@Composable
fun CalendarManageScreen(
    api: UserCalendarApi,
    onClose: () -> Unit,
    onChanged: () -> Unit = {},
) {
    val scope = rememberCoroutineScope()
    var calendars by remember { mutableStateOf<List<UserCalendar>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    /** null = 목록, EditTarget.Create = 새 캘린더, EditTarget.Edit = 편집. */
    var editing by remember { mutableStateOf<EditTarget?>(null) }

    suspend fun reload() {
        loading = true
        api.list().onSuccess { calendars = it }
        loading = false
    }

    LaunchedEffect(Unit) { reload() }

    val target = editing
    if (target != null) {
        CalendarEditScreen(
            api = api,
            target = target,
            onCancel = { editing = null },
            onDone = {
                editing = null
                scope.launch { reload(); onChanged() }
            },
        )
        return
    }

    Column(Modifier.fillMaxSize().background(MuyeonColors.surface)) {
        QuoteNavBar(title = "내 캘린더", onClose = onClose)

        if (loading && calendars.isEmpty()) {
            Box(Modifier.fillMaxSize(), Alignment.Center) {
                CircularProgressIndicator(color = MuyeonColors.primary)
            }
            return@Column
        }

        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            // 기본(미배정) — 편집·삭제 불가라 화살표 없이 안내만.
            item {
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CalendarCover(UserCalendar.DEFAULT)
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            "기본",
                            fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp,
                            lineHeight = 19.sp, color = MuyeonColors.textHead,
                        )
                        Text(
                            "캘린더를 지정하지 않은 일정",
                            fontFamily = customFontFamily, fontSize = 12.sp, lineHeight = 15.sp,
                            color = MuyeonColors.textSub,
                        )
                    }
                }
            }

            items(calendars, key = { it.id }) { cal ->
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                        .clickable { editing = EditTarget.Edit(cal) }
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CalendarCover(cal)
                    Text(
                        cal.name,
                        fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp,
                        lineHeight = 19.sp, color = MuyeonColors.textHead, modifier = Modifier.weight(1f),
                    )
                    Icon(
                        Icons.Filled.ChevronRight, null,
                        tint = MuyeonColors.secondary, modifier = Modifier.size(18.dp),
                    )
                }
            }

            // 새로운 캘린더 만들기 — 점선 카드(timetree)
            item {
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                        .clickable { editing = EditTarget.Create }
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier.size(48.dp).dashedBorder(MuyeonColors.border, 12.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Filled.Add, null, tint = MuyeonColors.secondary, modifier = Modifier.size(20.dp))
                    }
                    Text(
                        "새로운 캘린더 만들기",
                        fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 15.sp,
                        lineHeight = 18.sp, color = MuyeonColors.textSub,
                    )
                }
            }
        }
    }
}

/** 편집기 대상 — 만들기 / 기존 캘린더 편집. */
sealed interface EditTarget {
    data object Create : EditTarget
    data class Edit(val calendar: UserCalendar) : EditTarget
}

/**
 * 새 캘린더 / 캘린더 편집 — iOS `CalendarEditSheet` 1:1.
 *  만들기에서만 프리셋 그리드를 보여준다(편집은 이름·색만).
 */
@Composable
private fun CalendarEditScreen(
    api: UserCalendarApi,
    target: EditTarget,
    onCancel: () -> Unit,
    onDone: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val isEdit = target is EditTarget.Edit
    val existing = (target as? EditTarget.Edit)?.calendar

    var name by remember { mutableStateOf(existing?.name ?: "") }
    var color by remember { mutableStateOf(existing?.color ?: UserCalendarCatalog.palette[0]) }
    var preset by remember { mutableStateOf(existing?.preset) }
    var saving by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }

    val canSave = !saving && name.trim().isNotEmpty()

    Column(Modifier.fillMaxSize().background(MuyeonColors.surface)) {
        // 상단바 — 취소 / 제목 / 저장
        Box(Modifier.fillMaxWidth().height(44.dp), contentAlignment = Alignment.Center) {
            Text(
                if (isEdit) "캘린더 편집" else "새 캘린더",
                fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 17.sp,
                lineHeight = 20.sp, color = MuyeonColors.textHead,
            )
            Text(
                "취소",
                fontFamily = customFontFamily, fontSize = 15.sp, lineHeight = 18.sp, color = MuyeonColors.textHead,
                modifier = Modifier.align(Alignment.CenterStart).clickable(onClick = onCancel)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            )
            Text(
                if (saving) "저장 중…" else "저장",
                fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 18.sp,
                color = if (canSave) MuyeonColors.primary else MuyeonColors.chevron,
                modifier = Modifier.align(Alignment.CenterEnd)
                    .clickable(enabled = canSave) {
                        saving = true
                        errorText = null
                        val trimmed = name.trim()
                        scope.launch {
                            val result = if (existing != null) api.update(existing.id, trimmed, color).map { }
                            else api.create(trimmed, color, preset).map { }
                            result
                                .onSuccess { saving = false; onDone() }
                                .onFailure { saving = false; errorText = "저장에 실패했어요. 다시 시도해 주세요." }
                        }
                    }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            )
        }

        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            // 프리셋 — 만들기에서만. 탭하면 이름·색을 채우고 이후 자유 수정.
            if (!isEdit) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "어떤 캘린더를 만들까요?",
                        fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp,
                        lineHeight = 18.sp, color = MuyeonColors.textHead,
                    )
                    UserCalendarCatalog.presets.chunked(2).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            row.forEach { p ->
                                val selected = preset == p.id
                                Row(
                                    Modifier.weight(1f).clip(RoundedCornerShape(12.dp))
                                        .background(
                                            if (selected) UserCalendar.hexToColor(p.color).copy(alpha = 0.12f)
                                            else MuyeonColors.groupedBg
                                        )
                                        .clickable { name = p.name; color = p.color; preset = p.id }
                                        .padding(8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Box(
                                        Modifier.size(32.dp).clip(RoundedCornerShape(8.dp))
                                            .background(UserCalendar.hexToColor(p.color)),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(presetIcon(p.id), null, tint = Color.White, modifier = Modifier.size(16.dp))
                                    }
                                    Text(
                                        p.name,
                                        fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 13.sp,
                                        lineHeight = 16.sp, color = MuyeonColors.textHead,
                                    )
                                }
                            }
                            if (row.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }

            // 이름
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "이름",
                    fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
                    lineHeight = 17.sp, color = MuyeonColors.textHead,
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = {
                        Text(
                            "예: 성인 취미반",
                            fontFamily = customFontFamily, fontSize = 14.sp, color = MuyeonColors.secondary,
                        )
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            // 색상 — 12색 6열
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "색상",
                    fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
                    lineHeight = 17.sp, color = MuyeonColors.textHead,
                )
                UserCalendarCatalog.palette.chunked(6).forEach { row ->
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        row.forEach { hex ->
                            Box(
                                Modifier.weight(1f).aspectRatio(1f).padding(2.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Box(
                                    Modifier.size(36.dp).clip(CircleShape)
                                        .background(UserCalendar.hexToColor(hex))
                                        .clickable { color = hex },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    if (color == hex) {
                                        Icon(Icons.Filled.Check, "선택됨", tint = Color.White, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                        repeat(6 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }

            errorText?.let {
                Text(
                    it,
                    fontFamily = customFontFamily, fontSize = 13.sp, lineHeight = 16.sp, color = MuyeonColors.danger,
                )
            }

            if (isEdit) {
                Text(
                    "캘린더 삭제",
                    fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 15.sp,
                    lineHeight = 18.sp, color = MuyeonColors.danger, textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().clickable { confirmDelete = true }.padding(vertical = 12.dp),
                )
            }
        }
    }

    if (confirmDelete && existing != null) {
        QuoteDialog(
            title = "캘린더를 삭제할까요?",
            message = "이 캘린더의 일정은 '기본'으로 이동합니다. 일정이 삭제되지는 않아요.",
            confirmText = "삭제",
            onConfirm = {
                confirmDelete = false
                saving = true
                scope.launch {
                    api.remove(existing.id)
                        .onSuccess { saving = false; onDone() }
                        .onFailure { saving = false; errorText = "삭제에 실패했어요. 다시 시도해 주세요." }
                }
            },
            onDismiss = { confirmDelete = false },
        )
    }
}

/** 목록의 48dp 색 커버 — 프리셋 아이콘을 흰색으로 얹는다(iOS cover). */
@Composable
private fun CalendarCover(cal: UserCalendar) {
    Box(
        Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(cal.uiColor),
        contentAlignment = Alignment.Center,
    ) {
        Icon(presetIcon(cal.preset), null, tint = Color.White.copy(alpha = 0.95f), modifier = Modifier.size(20.dp))
    }
}

/** iOS SF Symbol → Material 아이콘 매핑(프리셋 id 기준). 없으면 달력. */
private fun presetIcon(preset: String?): ImageVector = when (preset) {
    "ADULT" -> Icons.Filled.SelfImprovement   // figure.dance
    "EXAM" -> Icons.Filled.School             // graduationcap.fill
    "KIDS" -> Icons.Filled.ChildCare          // figure.and.child.holdinghands
    "MAJOR" -> Icons.Filled.EmojiEvents       // trophy.fill
    "PERSONAL" -> Icons.Filled.Person         // person.fill
    "SPACE" -> Icons.Filled.Apartment         // building.2.fill
    else -> Icons.Filled.CalendarMonth        // calendar
}

/** 점선 테두리 — Compose 에 기본 제공이 없어 직접 그린다(iOS StrokeStyle dash:[4]). */
private fun Modifier.dashedBorder(color: Color, radius: androidx.compose.ui.unit.Dp) = drawBehind {
    val r = radius.toPx()
    drawRoundRect(
        color = color,
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(r, r),
        style = Stroke(
            width = 1.5.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f),
        ),
    )
}

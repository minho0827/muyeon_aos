package com.muyeon.app.ui.lesson

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import com.muyeon.app.BuildConfig
import com.muyeon.app.theme.customFontFamily
import com.muyeon.app.ui.common.MuyeonColors
import com.muyeon.app.ui.quote.QuoteDialog
import com.muyeon.app.ui.quote.QuoteNavBar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

/** 견적 수신 조건 — 백엔드 GET/PUT /me/quote-prefs. iOS `QuotePrefs` 1:1. */
data class QuotePrefs(
    val enabled: Boolean = true,
    val classes: List<String> = emptyList(),
    val ageGroups: List<String> = emptyList(),
    val formats: List<String> = emptyList(),
    val studentGenders: List<String> = emptyList(),
    val days: List<String> = emptyList(),
    val times: List<String> = emptyList(),
    val methods: List<String> = emptyList(),
    val regionCodes: List<String> = emptyList(),
) {
    fun toJson(): JSONObject = JSONObject()
        .put("enabled", enabled)
        .put("classes", JSONArray(classes))
        .put("ageGroups", JSONArray(ageGroups))
        .put("formats", JSONArray(formats))
        .put("studentGenders", JSONArray(studentGenders))
        .put("days", JSONArray(days))
        .put("times", JSONArray(times))
        .put("methods", JSONArray(methods))
        .put("regionCodes", JSONArray(regionCodes))

    companion object {
        fun from(o: JSONObject?): QuotePrefs {
            if (o == null) return QuotePrefs()
            // 서버가 미설정 축을 null 로 주므로 null·키없음·타입불일치 전부 빈 배열로(iOS 와 동일).
            fun arr(k: String): List<String> = o.optJSONArray(k)?.let { a ->
                (0 until a.length()).map { a.optString(it) }.filter { it.isNotEmpty() }
            } ?: emptyList()
            return QuotePrefs(
                enabled = if (o.has("enabled") && !o.isNull("enabled")) o.optBoolean("enabled") else true,
                classes = arr("classes"), ageGroups = arr("ageGroups"), formats = arr("formats"),
                studentGenders = arr("studentGenders"), days = arr("days"), times = arr("times"),
                methods = arr("methods"), regionCodes = arr("regionCodes"),
            )
        }
    }
}

/** 조건 옵션 — 웹/iOS 와 **값 계약**. 코드가 다르면 매칭이 조용히 어긋난다. */
object QuotePrefsOptions {
    val ages = listOf(
        "preschool" to "미취학 아동", "elem" to "초등학생", "middle" to "중학생",
        "high" to "고등학생", "20s" to "20대", "30s" to "30대", "40s+" to "40대 이상",
    )
    val formats = listOf("private" to "개인 레슨", "group" to "그룹 레슨", "academy" to "학원")
    val studentGenders = listOf("male" to "남자", "female" to "여자")
    val days = listOf(
        "mon" to "월", "tue" to "화", "wed" to "수", "thu" to "목",
        "fri" to "금", "sat" to "토", "sun" to "일",
    )
    val times = listOf(
        "dawn" to "이른 오전", "morning" to "오전", "noon" to "오후",
        "afternoon" to "늦은 오후", "evening" to "저녁", "night" to "늦은 저녁",
    )
    val methods = listOf("visitMe" to "내가 있는 곳으로", "visitThem" to "강사에게 방문", "any" to "무관")
    val teacherGenders = listOf("male" to "남", "female" to "여")
}

class LessonSettingsApi(private val token: String?) {

    private val client = OkHttpClient()
    private val apiBase = BuildConfig.API_BASE_URL + "/api"
    private val json = "application/json; charset=utf-8".toMediaType()

    /**
     * ⚠️ 미설정(없음)은 **켜짐**이다. 이 값은 토글을 저장한 적 있는 계정에만 생기는데
     *  서버(listTeachers / notifyMatchingPros)는 `!== false` 로 판정해 미설정자를 노출 대상에 넣는다.
     *  여기서 기본값을 false 로 읽으면 실제로는 노출 중인데 화면만 꺼짐으로 보이는 불일치가 생긴다.
     */
    suspend fun getProfile(): JSONObject? = call("/auth/me/profile")

    suspend fun saveProfile(lessonEnabled: Boolean, gender: String?): Result<Unit> = runCatching {
        val body = JSONObject().put("lessonEnabled", lessonEnabled).put("gender", gender ?: "")
        if (call("/auth/me/profile", "PATCH", body) == null) error("저장에 실패했어요.")
    }

    suspend fun getPrefs(): QuotePrefs = QuotePrefs.from(call("/me/quote-prefs"))

    suspend fun savePrefs(prefs: QuotePrefs): Result<Unit> = runCatching {
        if (call("/me/quote-prefs", "PUT", prefs.toJson()) == null) error("저장에 실패했어요.")
    }

    private suspend fun call(path: String, method: String = "GET", body: JSONObject? = null): JSONObject? =
        withContext(Dispatchers.IO) {
            runCatching {
                val payload = body?.toString()?.toRequestBody(json)
                    ?: if (method != "GET") "".toRequestBody(json) else null
                val req = Request.Builder().url(apiBase + path).method(method, payload)
                    .apply { if (!token.isNullOrEmpty()) addHeader("Authorization", "Bearer $token") }
                    .build()
                client.newCall(req).execute().use { res ->
                    if (!res.isSuccessful) return@use null
                    val text = res.body?.string().orEmpty()
                    if (text.isBlank()) JSONObject() else JSONObject(text)
                }
            }.getOrNull()
        }
}

/**
 * 레슨 설정 — iOS `LessonProvideSettingsView.swift` 1:1(웹 /lessonSettings 의 네이티브 이식).
 *  '레슨 제공' 토글 + 강사프로필 관리 진입 + 강사 성별 + 견적 수신 조건.
 */
@Composable
fun LessonProvideSettingsScreen(
    api: LessonSettingsApi,
    onClose: () -> Unit,
    onManageProfile: () -> Unit,
) {
    var enabled by remember { mutableStateOf(true) }   // 미설정 = 켜짐(서버 판정과 동일)
    var gender by remember { mutableStateOf<String?>(null) }
    var prefs by remember { mutableStateOf(QuotePrefs()) }
    var loading by remember { mutableStateOf(true) }
    var saving by remember { mutableStateOf(false) }
    var doneMessage by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        api.getProfile()?.let { p ->
            enabled = if (p.has("lessonEnabled") && !p.isNull("lessonEnabled")) p.optBoolean("lessonEnabled") else true
            gender = p.optString("gender").ifEmpty { null }
        }
        prefs = api.getPrefs()
        loading = false
    }

    Column(Modifier.fillMaxSize().background(MuyeonColors.surface)) {
        QuoteNavBar(title = "레슨 설정", onBack = onClose)

        if (loading) {
            Box(Modifier.weight(1f).fillMaxWidth(), Alignment.Center) {
                CircularProgressIndicator(color = MuyeonColors.primary)
            }
            return@Column
        }

        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp).padding(top = 18.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Text(
                "레슨 제공을 켜면, 일반회원의 강사 탭에 내 강사프로필이 노출되고 레슨 견적요청을 받을 수 있어요. " +
                    "강사 탭·강사 상세에 보이는 강사프로필(소개·사진·영상·경력·학력·지역)은 아래에서 수정합니다.",
                fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp,
                lineHeight = 22.sp, color = MuyeonColors.textSub,
            )

            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                    .border(1.dp, MuyeonColors.border, RoundedCornerShape(12.dp))
                    .clickable(onClick = onManageProfile).padding(14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Outlined.Badge, null, tint = MuyeonColors.primary, modifier = Modifier.size(18.dp))
                Text(
                    "강사프로필 관리 (소개·사진·영상·경력)",
                    fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
                    lineHeight = 17.sp, color = MuyeonColors.textHead, modifier = Modifier.weight(1f),
                )
                Icon(Icons.Filled.KeyboardArrowRight, null, tint = MuyeonColors.chevron, modifier = Modifier.size(16.dp))
            }

            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                    .border(1.dp, MuyeonColors.border, RoundedCornerShape(12.dp))
                    .clickable { enabled = !enabled }.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "레슨 제공 (강사 탭에 노출)",
                    fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 15.sp,
                    lineHeight = 18.sp, color = MuyeonColors.textHead, modifier = Modifier.weight(1f),
                )
                Switch(
                    checked = enabled, onCheckedChange = { enabled = it },
                    colors = SwitchDefaults.colors(checkedTrackColor = MuyeonColors.primary),
                )
            }

            // 강사 성별 — 고객의 '선호 강사 성별' 문진과 매칭(미설정이면 매칭에서 제외되지 않음).
            SettingsCard {
                Text(
                    "강사 성별",
                    fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp,
                    lineHeight = 18.sp, color = MuyeonColors.textHead,
                )
                Text(
                    "고객이 '선호하는 강사 성별'을 고른 견적요청과 매칭돼요. 선택 안 하면 모든 요청을 받아요.",
                    fontFamily = customFontFamily, fontSize = 13.sp, lineHeight = 18.sp,
                    color = MuyeonColors.textSub,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    QuotePrefsOptions.teacherGenders.forEach { (id, label) ->
                        PrefChip(label, gender == id, Modifier.weight(1f)) {
                            gender = if (gender == id) null else id
                        }
                    }
                }
            }

            // 견적 수신 조건(당근식) — 원하는 조건의 요청만 푸시·자동견적·모아보기에 노출.
            SettingsCard {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(
                            "견적 수신 조건 사용",
                            fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp,
                            lineHeight = 18.sp, color = MuyeonColors.textHead,
                        )
                        Text(
                            "끄면 모든 견적요청을 받아요.",
                            fontFamily = customFontFamily, fontSize = 12.sp, lineHeight = 15.sp,
                            color = MuyeonColors.textSub,
                        )
                    }
                    Switch(
                        checked = prefs.enabled, onCheckedChange = { prefs = prefs.copy(enabled = it) },
                        colors = SwitchDefaults.colors(checkedTrackColor = MuyeonColors.primary),
                    )
                }
                if (prefs.enabled) {
                    Text(
                        "선택한 조건에 맞는 견적요청만 알림·자동견적·모아보기에 보여요. 아무것도 선택하지 않으면 전체를 받아요. " +
                            "(내가 지정된 1:1 요청은 조건과 상관없이 항상 받아요)",
                        fontFamily = customFontFamily, fontSize = 13.sp, lineHeight = 19.sp,
                        color = MuyeonColors.textSub,
                    )
                    PrefGroup("레슨생 성별", QuotePrefsOptions.studentGenders, prefs.studentGenders) {
                        prefs = prefs.copy(studentGenders = it)
                    }
                    PrefGroup("연령대", QuotePrefsOptions.ages, prefs.ageGroups) { prefs = prefs.copy(ageGroups = it) }
                    PrefGroup("레슨 형태", QuotePrefsOptions.formats, prefs.formats) { prefs = prefs.copy(formats = it) }
                    PrefGroup("요일", QuotePrefsOptions.days, prefs.days) { prefs = prefs.copy(days = it) }
                    PrefGroup("시간대", QuotePrefsOptions.times, prefs.times) { prefs = prefs.copy(times = it) }
                    PrefGroup("진행 방식", QuotePrefsOptions.methods, prefs.methods) { prefs = prefs.copy(methods = it) }
                }
            }
        }

        Text(
            if (saving) "저장 중…" else "저장",
            fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp,
            lineHeight = 19.sp, color = Color.White, textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 20.dp).padding(top = 8.dp, bottom = 12.dp)
                .fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(MuyeonColors.primary)
                .clickable(enabled = !saving) {
                    saving = true
                    scope.launch {
                        api.saveProfile(enabled, gender)
                            .mapCatching { api.savePrefs(prefs).getOrThrow() }
                            .onSuccess { doneMessage = "레슨 설정을 저장했습니다." }
                            .onFailure { errorMessage = it.message ?: "저장에 실패했어요." }
                        saving = false
                    }
                }
                .padding(vertical = 16.dp),
        )
    }

    doneMessage?.let { msg ->
        QuoteDialog("안내", msg, "확인", onConfirm = { doneMessage = null; onClose() }, onDismiss = { doneMessage = null })
    }
    errorMessage?.let { msg ->
        QuoteDialog("오류", msg, "확인", onConfirm = { errorMessage = null }, onDismiss = { errorMessage = null })
    }
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
            .border(1.dp, MuyeonColors.border, RoundedCornerShape(12.dp)).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        content = content,
    )
}

@Composable
private fun PrefGroup(
    title: String,
    options: List<Pair<String, String>>,
    selected: List<String>,
    onChange: (List<String>) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            title,
            fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
            lineHeight = 17.sp, color = MuyeonColors.textHead,
        )
        // iOS 는 adaptive(minimum: 84) 그리드 — 여기선 3열 고정으로 같은 폭감을 낸다.
        options.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { (id, label) ->
                    PrefChip(label, selected.contains(id), Modifier.weight(1f)) {
                        onChange(if (selected.contains(id)) selected - id else selected + id)
                    }
                }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun PrefChip(label: String, on: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Text(
        label,
        fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 13.sp,
        lineHeight = 16.sp,
        color = if (on) Color.White else MuyeonColors.body,
        textAlign = TextAlign.Center, maxLines = 1,
        modifier = modifier.clip(RoundedCornerShape(50))
            .background(if (on) MuyeonColors.primary else MuyeonColors.surface)
            .then(if (on) Modifier else Modifier.border(1.dp, MuyeonColors.border, RoundedCornerShape(50)))
            .clickable(onClick = onClick)
            .padding(horizontal = 13.dp, vertical = 8.dp),
    )
}

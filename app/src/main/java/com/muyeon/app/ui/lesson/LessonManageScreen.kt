package com.muyeon.app.ui.lesson

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.muyeon.app.BuildConfig
import com.muyeon.app.theme.customFontFamily
import com.muyeon.app.ui.common.MuyeonColors
import com.muyeon.app.ui.quote.QuoteDialog
import com.muyeon.app.ui.quote.QuoteEmptyState
import com.muyeon.app.ui.quote.QuoteNavBar
import com.muyeon.app.ui.quote.QuoteUi
import com.muyeon.app.ui.quote.intOrNull
import com.muyeon.app.ui.quote.map
import com.muyeon.app.ui.quote.stringList
import com.muyeon.app.ui.quote.stringOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 내 레슨 관리 — iOS `LessonManageView.swift` 1:1.
 *  개설 목록(수정·삭제·홈 노출권) + 보관함(복원).
 *
 * ⚠️ 삭제는 하드 삭제가 아니라 **보관(ARCHIVED) 소프트 삭제**다. 복원 경로가 반드시 함께 있어야 한다
 *   (레슨 슬롯 라이프사이클 규약).
 */
data class LessonProduct(
    val id: Int,
    val title: String?,
    val genre: String?,
    val isExperience: Boolean?,
    val promotedUntil: String?,   // ISO — 미래면 홈 노출중
    val images: List<String>?,
    val scheduleText: String?,
    val price: Int?,
    val region: String?,
    val status: String?,
) {
    /** 홈 노출중이면 종료 시각(millis), 아니면 null. */
    val boostUntilMillis: Long?
        get() = QuoteUi.parseDate(promotedUntil)?.takeIf { it > System.currentTimeMillis() }

    companion object {
        fun from(o: JSONObject) = LessonProduct(
            o.optInt("id"), o.stringOrNull("title"), o.stringOrNull("genre"),
            o.optBoolean("isExperience", false), o.stringOrNull("promotedUntil"),
            o.stringList("images"), o.stringOrNull("scheduleText"),
            o.intOrNull("price"), o.stringOrNull("region"), o.stringOrNull("status"),
        )
    }
}

data class LessonPlan(val code: String, val name: String?, val priceKrw: Int?) {
    companion object {
        fun from(o: JSONObject) = LessonPlan(o.optString("code"), o.stringOrNull("name"), o.intOrNull("priceKrw"))
    }
}

/** /lesson-products, /monetization — iOS LessonProductService. */
class LessonProductApi(private val token: String?) {
    private val client = OkHttpClient()
    private val apiBase = BuildConfig.API_BASE_URL + "/api"

    suspend fun myLessons(): Result<List<LessonProduct>> =
        call("/lesson-products/mine").map { JSONArray(it.ifBlank { "[]" }).map(LessonProduct::from) }

    suspend fun archivedLessons(): Result<List<LessonProduct>> =
        call("/lesson-products/archived").map { JSONArray(it.ifBlank { "[]" }).map(LessonProduct::from) }

    /** 보관(소프트 삭제) — 슬롯은 서버가 archiveProductSlots 로 정리. */
    suspend fun delete(id: Int): Result<Unit> = call("/lesson-products/$id", "DELETE").map { }

    suspend fun restore(id: Int): Result<Unit> = call("/lesson-products/$id/restore", "POST").map { }

    suspend fun plans(featureType: String): Result<List<LessonPlan>> =
        call("/monetization/plans?featureType=$featureType").map { JSONArray(it.ifBlank { "[]" }).map(LessonPlan::from) }

    suspend fun createOrder(planCode: String, scopeRefId: Int): Result<Unit> =
        call("/monetization/orders", "POST", JSONObject().put("planCode", planCode).put("scopeRefId", scopeRefId)).map { }

    private suspend fun call(path: String, method: String = "GET", body: JSONObject? = null): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                val payload = when {
                    body != null -> body.toString().toRequestBody(JSON)
                    method != "GET" && method != "DELETE" -> "".toRequestBody(JSON)
                    else -> null
                }
                val req = Request.Builder().url(apiBase + path).method(method, payload)
                    .addHeader("Content-Type", "application/json")
                    .apply { if (!token.isNullOrEmpty()) addHeader("Authorization", "Bearer $token") }
                    .build()
                client.newCall(req).execute().use { res ->
                    val text = res.body?.string().orEmpty()
                    if (!res.isSuccessful) {
                        val msg = runCatching { JSONObject(text).optString("message") }.getOrNull()
                        throw IllegalStateException(msg?.ifEmpty { null } ?: "요청에 실패했어요.")
                    }
                    text
                }
            }
        }

    private companion object { val JSON = "application/json".toMediaType() }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LessonManageScreen(
    api: LessonProductApi,
    onClose: () -> Unit,
    onCreate: () -> Unit,
    onEdit: (Int) -> Unit,
    onSlots: (Int) -> Unit,
) {
    var rows by remember { mutableStateOf<List<LessonProduct>?>(null) }   // null = 로딩 전
    var archived by remember { mutableStateOf<List<LessonProduct>>(emptyList()) }
    var showArchive by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<LessonProduct?>(null) }
    var boostTarget by remember { mutableStateOf<LessonProduct?>(null) }
    var plans by remember { mutableStateOf<List<LessonPlan>>(emptyList()) }
    var toast by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    suspend fun load() {
        rows = api.myLessons().getOrDefault(emptyList())
        archived = api.archivedLessons().getOrDefault(emptyList())
    }

    LaunchedEffect(Unit) { load() }

    Column(Modifier.fillMaxSize().background(MuyeonColors.surface)) {
        QuoteNavBar(title = "내 레슨 관리", onBack = onClose)

        val list = rows
        when {
            list == null -> Box(Modifier.weight(1f).fillMaxWidth(), Alignment.Center) {
                CircularProgressIndicator(color = MuyeonColors.primary)
            }
            list.isEmpty() -> Box(Modifier.weight(1f).fillMaxWidth(), Alignment.Center) {
                QuoteEmptyState(
                    Icons.Outlined.Inbox, "개설한 레슨이 없어요",
                    "레슨을 개설하면 예약 시간이 자동으로 만들어져요.",
                )
            }
            else -> LazyColumn(
                Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (archived.isNotEmpty()) {
                    item {
                        Text(
                            "보관함 ${archived.size}개 보기",
                            fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 13.sp,
                            lineHeight = 16.sp, color = MuyeonColors.textSub,
                            modifier = Modifier.clickable { showArchive = true },
                        )
                    }
                }
                items(list, key = { it.id }) { l ->
                    LessonCard(
                        l = l,
                        onEdit = { onEdit(l.id) },
                        onSlots = { onSlots(l.id) },
                        onBoost = {
                            boostTarget = l
                            scope.launch { plans = api.plans("HOME_PROMOTION").getOrDefault(emptyList()) }
                        },
                        onDelete = { deleteTarget = l },
                    )
                }
            }
        }

        Text(
            "새 레슨 개설",
            fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp,
            lineHeight = 19.sp, color = Color.White, textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(horizontal = 20.dp).padding(top = 8.dp, bottom = 12.dp)
                .fillMaxWidth().clip(RoundedCornerShape(12.dp))
                .background(MuyeonColors.primary).clickable(onClick = onCreate).padding(vertical = 16.dp),
        )
    }

    // 보관함 — 복원 경로(소프트 삭제라 반드시 필요)
    if (showArchive) {
        ModalBottomSheet(onDismissRequest = { showArchive = false }) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 24.dp)) {
                Text(
                    "보관함",
                    fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 17.sp,
                    lineHeight = 20.sp, color = MuyeonColors.textHead, modifier = Modifier.padding(bottom = 10.dp),
                )
                archived.forEach { a ->
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            a.title ?: "(제목 없음)",
                            fontFamily = customFontFamily, fontSize = 14.sp, lineHeight = 18.sp,
                            color = MuyeonColors.textHead, modifier = Modifier.weight(1f),
                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            "복원",
                            fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 13.sp,
                            lineHeight = 16.sp, color = MuyeonColors.primary,
                            modifier = Modifier.clickable {
                                scope.launch {
                                    api.restore(a.id).onSuccess { toast = "복원했어요." }.onFailure { toast = it.message }
                                    load(); showArchive = false
                                }
                            },
                        )
                    }
                }
            }
        }
    }

    // 홈 노출권 — 플랜 선택 후 주문 생성
    boostTarget?.let { target ->
        ModalBottomSheet(onDismissRequest = { boostTarget = null }) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 24.dp)) {
                Text(
                    "홈 노출권",
                    fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 17.sp,
                    lineHeight = 20.sp, color = MuyeonColors.textHead,
                )
                Text(
                    "홈 상단에 이 레슨을 노출해요.",
                    fontFamily = customFontFamily, fontSize = 13.sp, lineHeight = 18.sp,
                    color = MuyeonColors.textSub, modifier = Modifier.padding(bottom = 10.dp),
                )
                if (plans.isEmpty()) {
                    Text("이용 가능한 상품이 없어요.", fontFamily = customFontFamily, fontSize = 14.sp, color = MuyeonColors.textSub)
                }
                plans.forEach { p ->
                    Row(
                        Modifier.fillMaxWidth().clickable {
                            scope.launch {
                                api.createOrder(p.code, target.id)
                                    .onSuccess { toast = "신청했어요." }.onFailure { toast = it.message }
                                boostTarget = null; load()
                            }
                        }.padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            p.name ?: p.code,
                            fontFamily = customFontFamily, fontSize = 15.sp, lineHeight = 18.sp,
                            color = MuyeonColors.textHead, modifier = Modifier.weight(1f),
                        )
                        Text(
                            p.priceKrw?.let { LessonOptions.priceLabel(it) } ?: "",
                            fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
                            lineHeight = 17.sp, color = MuyeonColors.primary,
                        )
                    }
                }
            }
        }
    }

    deleteTarget?.let { t ->
        QuoteDialog(
            title = "이 레슨을 삭제할까요?",
            message = "보관함으로 이동해요. 예약 가능 시간도 함께 정리되며, 보관함에서 복원할 수 있어요.",
            confirmText = "삭제",
            onConfirm = {
                deleteTarget = null
                scope.launch {
                    api.delete(t.id).onSuccess { toast = "보관함으로 옮겼어요." }.onFailure { toast = it.message }
                    load()
                }
            },
            onDismiss = { deleteTarget = null },
        )
    }
    toast?.let { msg ->
        QuoteDialog("알림", msg, "확인", onConfirm = { toast = null }, onDismiss = { toast = null })
    }
}

@Composable
private fun LessonCard(
    l: LessonProduct,
    onEdit: () -> Unit,
    onSlots: () -> Unit,
    onBoost: () -> Unit,
    onDelete: () -> Unit,
) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
            .border(1.dp, MuyeonColors.border, RoundedCornerShape(14.dp)).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(72.dp).clip(RoundedCornerShape(10.dp)).background(Color(0xFFF2F2F7))) {
                l.images?.firstOrNull()?.let {
                    AsyncImage(QuoteUi.imageUrl(it), null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        l.title ?: "(제목 없음)",
                        fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp,
                        lineHeight = 18.sp, color = MuyeonColors.textHead,
                        maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false),
                    )
                    if (l.isExperience == true) CardPill("체험", filled = false)
                    l.boostUntilMillis?.let {
                        CardPill("홈 노출중 ~${SimpleDateFormat("M.d", Locale.KOREA).format(Date(it))}", filled = true)
                    }
                }
                Text(
                    listOfNotNull(l.genre, l.region, l.scheduleText).joinToString(" · "),
                    fontFamily = customFontFamily, fontSize = 12.sp, lineHeight = 16.sp,
                    color = MuyeonColors.textSub, maxLines = 2, overflow = TextOverflow.Ellipsis,
                )
                l.price?.let {
                    Text(
                        LessonOptions.priceLabel(it),
                        fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 13.sp,
                        lineHeight = 16.sp, color = MuyeonColors.textHead,
                    )
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            CardAction("수정", Modifier.weight(1f), onEdit)
            CardAction("예약 시간", Modifier.weight(1f), onSlots)
            CardAction("홈 노출", Modifier.weight(1f), onBoost)
            CardAction("삭제", Modifier.weight(1f), onDelete, danger = true)
        }
    }
}

@Composable
private fun CardPill(text: String, filled: Boolean) {
    Text(
        text,
        fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 10.sp, lineHeight = 12.sp,
        color = if (filled) Color.White else MuyeonColors.textSub,
        modifier = Modifier.clip(RoundedCornerShape(50))
            .background(if (filled) MuyeonColors.primary else Color(0xFFF2F2F7))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    )
}

@Composable
private fun CardAction(text: String, modifier: Modifier = Modifier, onClick: () -> Unit, danger: Boolean = false) {
    Text(
        text,
        fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, lineHeight = 15.sp,
        color = if (danger) MuyeonColors.danger else MuyeonColors.textSub, textAlign = TextAlign.Center,
        modifier = modifier.clip(RoundedCornerShape(8.dp)).background(Color(0xFFF7F7F7))
            .clickable(onClick = onClick).padding(vertical = 9.dp),
    )
}

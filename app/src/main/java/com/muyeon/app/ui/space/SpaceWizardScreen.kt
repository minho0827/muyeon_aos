package com.muyeon.app.ui.space

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muyeon.app.theme.customFontFamily
import com.muyeon.app.ui.castingstage.components.StageBottomButton
import com.muyeon.app.ui.common.FormChipWrap
import com.muyeon.app.ui.common.FormField
import com.muyeon.app.ui.common.FormTextField
import com.muyeon.app.ui.common.MarketRegion
import com.muyeon.app.ui.common.MuyeonColors
import com.muyeon.app.ui.common.MuyeonDialog
import com.muyeon.app.ui.common.MuyeonDialogAction
import com.muyeon.app.ui.common.MuyeonDialogKind
import com.muyeon.app.ui.common.MuyeonList
import com.muyeon.app.ui.common.UploadImagePreparer
import com.muyeon.app.ui.common.WizardProgress
import com.muyeon.app.ui.jobposting.DetailImages
import com.muyeon.app.ui.jobposting.SectionHead
import com.muyeon.app.ui.lesson.LocationSearchSheet
import com.muyeon.app.ui.quote.RegionPickRow
import com.muyeon.app.ui.transfer.AddressBlock
import com.muyeon.app.ui.transfer.NumberField
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

private val STEPS = listOf("사진", "위치", "기본", "가격", "운영", "시설·규정", "소개")
private const val MAX_IMAGES = 10
private val DAYS = listOf("0" to "일", "1" to "월", "2" to "화", "3" to "수", "4" to "목", "5" to "금", "6" to "토")
private val LEADS = listOf("0" to "바로", "30" to "30분 전", "60" to "1시간 전", "120" to "2시간 전", "180" to "3시간 전", "1440" to "하루 전")
private val MIN_HOURS = listOf("1" to "1시간", "2" to "2시간", "3" to "3시간", "4" to "4시간")

/** 세부공간 한 줄(이름·시간당 원). */
data class SpaceOptionInput(val label: String = "", val price: String = "")

data class SpaceForm(
    val images: List<String> = emptyList(),
    val roadAddress: String = "",
    val placeName: String = "",
    val addressDetail: String = "",
    val lat: Double? = null,
    val lng: Double? = null,
    val region: String = "",
    val regionCode: String? = null,
    val name: String = "",
    val categoryId: Int? = null,
    val capacityMax: String = "",
    val baseHeadcount: String = "",
    val extraPersonFee: String = "",
    val pricePerHour: String = "",
    val options: List<SpaceOptionInput> = emptyList(),
    val openHour: Int = 9,
    val closeHour: Int = 23,
    val openDays: Set<String> = DAYS.map { it.first }.toSet(),
    val minHours: String = "1",
    val leadMinutes: String = "60",
    val facilities: Set<String> = emptySet(),
    val facilityList: String = "",
    val cautions: String = "",
    val refund: List<SpaceRefundRule> = DEFAULT_REFUND,
    val intro: String = "",
    val phone: String = "",
) {
    fun toJson(): JSONObject {
        val o = JSONObject()
        fun lines(t: String) = JSONArray(t.lines().map { it.trim() }.filter { it.isNotEmpty() })
        fun num(t: String) = t.filter { it.isDigit() }.toIntOrNull()
        o.put("name", name.trim())
        o.put("categoryId", categoryId ?: JSONObject.NULL)
        o.put("region", region.trim())
        o.put("regionCode", regionCode ?: JSONObject.NULL)
        o.put("roadAddress", roadAddress.trim().ifEmpty { JSONObject.NULL })
        o.put("addressDetail", addressDetail.trim().ifEmpty { JSONObject.NULL })
        o.put("lat", lat ?: JSONObject.NULL); o.put("lng", lng ?: JSONObject.NULL)
        o.put("pricePerHour", num(pricePerHour) ?: 0)
        o.put("images", JSONArray(images))
        o.put("intro", intro.trim().ifEmpty { JSONObject.NULL })
        o.put("capacityMax", num(capacityMax)?.takeIf { it > 0 } ?: JSONObject.NULL)
        o.put("baseHeadcount", num(baseHeadcount)?.takeIf { it > 0 } ?: JSONObject.NULL)
        o.put("extraPersonFee", num(extraPersonFee) ?: 0)
        o.put("businessHours", String.format(java.util.Locale.KOREA, "%02d~%02d시", openHour, closeHour))
        o.put("openDays", JSONArray(openDays.mapNotNull { it.toIntOrNull() }.sorted()))
        o.put("hourlyMinHours", num(minHours) ?: 1)
        o.put("bookingLeadMinutes", num(leadMinutes) ?: 60)
        o.put("facilities", JSONObject().apply { SpaceFacility.ALL.forEach { put(it.key, it.key in facilities) } })
        o.put("facilityList", lines(facilityList))
        o.put("cautions", lines(cautions))
        o.put("refundPolicy", JSONArray(refund.filter { !it.label.isNullOrBlank() && !it.value.isNullOrBlank() }
            .map { JSONObject().put("label", it.label!!.trim()).put("value", it.value!!.trim()) }))
        o.put("phone", phone.trim().ifEmpty { JSONObject.NULL })
        o.put("priceOptions", JSONArray(options.mapNotNull { op ->
            val p = num(op.price) ?: return@mapNotNull null
            if (op.label.isBlank()) null else JSONObject().put("label", op.label.trim()).put("price", p)
        }))
        return o
    }

    companion object {
        fun from(d: SpaceDetail): SpaceForm {
            val hours = Regex("(\\d{1,2})(?::\\d{2})?\\s*시?\\s*[~\\-–]\\s*(\\d{1,2})").find(d.businessHours.orEmpty())
            return SpaceForm(
                images = d.images ?: emptyList(), roadAddress = d.roadAddress.orEmpty(), placeName = d.roadAddress.orEmpty(),
                addressDetail = d.addressDetail.orEmpty(), lat = d.lat, lng = d.lng, region = d.region.orEmpty(), regionCode = d.regionCode,
                name = d.name, categoryId = d.categoryId, capacityMax = d.capacityMax?.toString() ?: "",
                baseHeadcount = d.baseHeadcount?.toString() ?: "", extraPersonFee = d.extraPersonFee?.takeIf { it > 0 }?.toString() ?: "",
                pricePerHour = d.pricePerHour?.toString() ?: "",
                options = d.hourlyOptions.map { SpaceOptionInput(it.label.orEmpty(), it.price?.toString() ?: "") },
                openHour = hours?.groupValues?.get(1)?.toIntOrNull() ?: 9,
                closeHour = hours?.groupValues?.get(2)?.toIntOrNull()?.let { if (it == 0) 24 else it } ?: 23,
                openDays = if (d.openDays.isEmpty()) DAYS.map { it.first }.toSet() else d.openDays.map { it.toString() }.toSet(),
                minHours = (d.hourlyMinHours ?: 1).toString(), leadMinutes = (d.bookingLeadMinutes ?: 60).toString(),
                facilities = d.facilities?.filterValues { it }?.keys ?: emptySet(),
                facilityList = (d.facilityList ?: emptyList()).joinToString("\n"),
                cautions = (d.cautions ?: emptyList()).joinToString("\n"),
                refund = d.refundPolicy?.takeIf { it.isNotEmpty() } ?: DEFAULT_REFUND,
                intro = d.intro.orEmpty(), phone = d.phone.orEmpty(),
            )
        }
    }
}

/**
 * 공간 등록·수정 위저드(호스트) — 사진 → 위치(카카오 검색) → 기본(이름·카테고리·인원·추가 요금) → 가격(시간당 + 세부공간)
 *  → 운영(영업시간·요일·최소 시간·예약 마감) → 시설·주의사항·환불 규정(기본값 미리 채움) → 소개.
 */
@Composable
fun SpaceWizardScreen(
    api: SpaceApi,
    editId: Int?,
    onClose: () -> Unit,
    onSaved: (Int) -> Unit,
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val scroll = rememberScrollState()
    var form by remember { mutableStateOf(SpaceForm()) }
    var baseline by remember { mutableStateOf(SpaceForm()) }
    var step by remember { mutableIntStateOf(0) }
    var loading by remember { mutableStateOf(editId != null) }
    var saving by remember { mutableStateOf(false) }
    var uploading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var showExit by remember { mutableStateOf(false) }
    var showSearch by remember { mutableStateOf(false) }
    var categories by remember { mutableStateOf<List<SpaceCategory>>(emptyList()) }

    LaunchedEffect(editId) {
        api.categories().onSuccess { categories = it }
        if (editId != null) {
            api.detail(editId).onSuccess { form = SpaceForm.from(it); baseline = form }.onFailure { error = it.message }
            loading = false
        }
    }
    LaunchedEffect(step) { scroll.scrollTo(0) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(maxItems = MAX_IMAGES)) { uris ->
        if (uris.isEmpty()) return@rememberLauncherForActivityResult
        uploading = true
        scope.launch {
            var list = form.images
            uris.take((MAX_IMAGES - list.size).coerceAtLeast(0)).forEach { u ->
                UploadImagePreparer.prepare(ctx, u)?.let { bytes -> api.uploadImage(bytes).onSuccess { list = list + it }.onFailure { error = it.message } }
            }
            form = form.copy(images = list)
            uploading = false
        }
    }

    val dirty = form != baseline
    fun requestBack() { if (step > 0) step -= 1 else if (dirty) showExit = true else onClose() }
    BackHandler { requestBack() }

    val canNext = when (step) {
        0 -> !uploading
        1 -> form.region.isNotBlank()
        2 -> form.name.isNotBlank()
        3 -> (form.pricePerHour.toIntOrNull() ?: 0) > 0 || form.options.any { it.label.isNotBlank() && (it.price.toIntOrNull() ?: 0) > 0 }
        4 -> form.openDays.isNotEmpty() && form.closeHour > form.openHour
        else -> true
    }

    fun save() {
        if (form.name.isBlank()) { step = 2; error = "공간 이름을 입력해 주세요."; return }
        if (form.region.isBlank()) { step = 1; error = "지역을 선택해 주세요."; return }
        saving = true
        scope.launch {
            // 세부공간만 적고 기본 시간당을 비우면 가장 낮은 세부공간 가격을 기본값으로(서버 필수 칸).
            val f = if ((form.pricePerHour.toIntOrNull() ?: 0) <= 0) {
                form.copy(pricePerHour = form.options.mapNotNull { it.price.toIntOrNull() }.filter { it > 0 }.minOrNull()?.toString() ?: "0")
            } else form
            api.save(editId, f.toJson()).onSuccess { baseline = form; onSaved(it) }.onFailure { error = it.message ?: "저장에 실패했어요." }
            saving = false
        }
    }

    Column(Modifier.fillMaxSize().background(Color.White)) {
        Box(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp)) {
            Text(
                if (editId != null) "공간 수정" else "공간 등록",
                fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MuyeonColors.textHead,
                textAlign = TextAlign.Center, modifier = Modifier.align(Alignment.Center),
            )
            Icon(
                Icons.Filled.Close, "닫기", tint = MuyeonColors.body,
                modifier = Modifier.align(Alignment.CenterStart).size(18.dp).clickable { if (dirty) showExit = true else onClose() },
            )
        }
        WizardProgress(STEPS, step)
        Box(Modifier.fillMaxWidth().height(1.dp).background(MuyeonColors.border))

        if (loading) {
            Box(Modifier.weight(1f).fillMaxWidth(), Alignment.Center) { CircularProgressIndicator(color = MuyeonColors.primary) }
            return@Column
        }

        Column(
            Modifier.weight(1f).verticalScroll(scroll).padding(horizontal = 20.dp).padding(top = 18.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            when (step) {
                0 -> {
                    SectionHead("사진", "공간이 잘 보이는 사진을 올려 주세요. 첫 사진이 대표 사진이에요.")
                    DetailImages(
                        images = form.images, uploading = uploading,
                        onAdd = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                        onRemove = { i -> form = form.copy(images = form.images.filterIndexed { idx, _ -> idx != i }) },
                        title = "사진", hint = "최대 10장까지 올릴 수 있어요.",
                    )
                }
                1 -> {
                    SectionHead("위치", "주소를 검색하면 지도에 위치가 표시돼요.")
                    AddressBlock(form.placeName, form.roadAddress, form.lat, form.lng) { showSearch = true }
                    FormField("상세 주소") { FormTextField(form.addressDetail, { form = form.copy(addressDetail = it) }, "예) 지하 1층 B홀", 100) }
                    FormField("지역", required = true, hint = "(목록 지역 필터에 쓰여요)") {
                        RegionPickRow(token = api.http.token(), placeholder = "지역을 선택하세요", name = form.region, allowSidoAll = false) { name, code ->
                            form = form.copy(region = name, regionCode = code)
                        }
                    }
                }
                2 -> {
                    SectionHead("기본 정보", "공간 이름과 인원을 알려 주세요.")
                    FormField("공간 이름", required = true) { FormTextField(form.name, { form = form.copy(name = it) }, "예) 사당 미러 스튜디오 A", 60) }
                    if (categories.isNotEmpty()) {
                        FormField("카테고리") {
                            FormChipWrap(categories.map { it.id.toString() to it.name }, { form.categoryId?.toString() == it }) { v ->
                                form = form.copy(categoryId = v.toIntOrNull()?.takeIf { it != form.categoryId })
                            }
                        }
                    }
                    FormField("최대 인원", hint = "(명)") { NumberField(form.capacityMax, "예) 15", 4) { form = form.copy(capacityMax = it) } }
                    FormField("기본 인원", hint = "(명, 넘으면 추가 요금)") { NumberField(form.baseHeadcount, "예) 4", 4) { form = form.copy(baseHeadcount = it) } }
                    FormField("추가 인원 요금", hint = "(1인·1시간당 원)") { NumberField(form.extraPersonFee, "예) 2000", 7) { form = form.copy(extraPersonFee = it) } }
                    FormField("전화번호", hint = "(선택, 상세에 전화 버튼)") {
                        FormTextField(form.phone, { v -> form = form.copy(phone = v.filter { it.isDigit() || it == '-' }) }, "예) 02-123-4567", 20, keyboardType = KeyboardType.Phone)
                    }
                }
                3 -> {
                    SectionHead("가격", "기본 시간당 가격과 세부공간(홀·룸)별 가격을 정해 주세요.")
                    FormField("기본 시간당 가격", required = true, hint = "(원)") { NumberField(form.pricePerHour, "예) 7000", 8) { form = form.copy(pricePerHour = it) } }
                    FormField("세부공간", hint = "(선택)") {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            form.options.forEachIndexed { i, op ->
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Box(Modifier.weight(1.4f)) {
                                        FormTextField(op.label, { v -> form = form.copy(options = form.options.mapIndexed { j, o -> if (j == i) o.copy(label = v) else o }) }, "예) A홀 20평", 60)
                                    }
                                    Box(Modifier.weight(1f)) {
                                        NumberField(op.price, "시간당 원", 8) { v -> form = form.copy(options = form.options.mapIndexed { j, o -> if (j == i) o.copy(price = v) else o }) }
                                    }
                                    Icon(
                                        Icons.Filled.Close, "삭제", tint = MuyeonColors.hint,
                                        modifier = Modifier.size(20.dp).clickable { form = form.copy(options = form.options.filterIndexed { j, _ -> j != i }) },
                                    )
                                }
                            }
                            if (form.options.size < 10) AddRowLink("세부공간 추가") { form = form.copy(options = form.options + SpaceOptionInput()) }
                        }
                    }
                }
                4 -> {
                    SectionHead("운영", "영업시간과 예약 규칙을 정해 주세요.")
                    FormField("영업시간") {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            HourStepper(form.openHour, 0, 23) { form = form.copy(openHour = it) }
                            Text("~", fontFamily = customFontFamily, color = MuyeonColors.textSub)
                            HourStepper(form.closeHour, 1, 24) { form = form.copy(closeHour = it) }
                        }
                    }
                    FormField("운영 요일") {
                        FormChipWrap(DAYS, { it in form.openDays }) { v -> form = form.copy(openDays = if (v in form.openDays) form.openDays - v else form.openDays + v) }
                    }
                    FormField("최소 예약 시간") { FormChipWrap(MIN_HOURS, { form.minHours == it }) { form = form.copy(minHours = it) } }
                    FormField("예약 마감", hint = "(이용 시작 기준)") { FormChipWrap(LEADS, { form.leadMinutes == it }) { form = form.copy(leadMinutes = it) } }
                }
                5 -> {
                    SectionHead("시설·주의사항·환불 규정", "예약 전에 꼭 알아야 할 내용을 적어 주세요.")
                    FormField("시설") {
                        FormChipWrap(SpaceFacility.ALL.map { it.key to it.label }, { it in form.facilities }) { v ->
                            form = form.copy(facilities = if (v in form.facilities) form.facilities - v else form.facilities + v)
                        }
                    }
                    FormField("시설 상세", hint = "(한 줄에 하나)") { FormTextField(form.facilityList, { form = form.copy(facilityList = it) }, "예) 전면 거울 8m", 2000, minLines = 3) }
                    FormField("예약 시 주의사항", hint = "(한 줄에 하나)") { FormTextField(form.cautions, { form = form.copy(cautions = it) }, "예) 실내화를 꼭 챙겨 주세요", 2000, minLines = 3) }
                    FormField("환불 규정") {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            form.refund.forEachIndexed { i, r ->
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Box(Modifier.weight(1f)) {
                                        FormTextField(r.label.orEmpty(), { v -> form = form.copy(refund = form.refund.mapIndexed { j, x -> if (j == i) x.copy(label = v) else x }) }, "예) 이용 2일 전", 30)
                                    }
                                    Box(Modifier.weight(1.3f)) {
                                        FormTextField(r.value.orEmpty(), { v -> form = form.copy(refund = form.refund.mapIndexed { j, x -> if (j == i) x.copy(value = v) else x }) }, "예) 총 금액의 100% 환불", 60)
                                    }
                                    Icon(
                                        Icons.Filled.Close, "삭제", tint = MuyeonColors.hint,
                                        modifier = Modifier.size(20.dp).clickable { form = form.copy(refund = form.refund.filterIndexed { j, _ -> j != i }) },
                                    )
                                }
                            }
                            if (form.refund.size < 15) AddRowLink("규정 줄 추가") { form = form.copy(refund = form.refund + SpaceRefundRule("", "")) }
                        }
                    }
                }
                else -> {
                    SectionHead("소개", "공간의 분위기와 장점을 소개해 주세요.")
                    FormField("공간 소개") { FormTextField(form.intro, { form = form.copy(intro = it) }, "예) 탄성마루와 전면 거울을 갖춘 30평 연습실이에요.", 5000, minLines = 6) }
                }
            }
        }

        Row(
            Modifier.fillMaxWidth().background(Color.White).navigationBarsPadding().padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (step > 0) {
                Box(
                    Modifier.weight(1f).height(56.dp).clip(RoundedCornerShape(16.dp)).border(1.dp, MuyeonList.ink300, RoundedCornerShape(16.dp))
                        .clickable { step -= 1 },
                    Alignment.Center,
                ) { Text("이전", fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = MuyeonColors.textHead) }
            }
            Box(Modifier.weight(2f)) {
                val last = step == STEPS.lastIndex
                StageBottomButton(if (last) (if (editId != null) "수정 완료" else "등록") else "다음", enabled = canNext, busy = saving) {
                    if (last) save() else step += 1
                }
            }
        }
    }

    if (showSearch) {
        LocationSearchSheet(
            initialQuery = form.roadAddress, placeholder = "주소·건물명 검색",
            onSelect = { p ->
                val addr = p.address ?: p.name
                form = form.copy(roadAddress = addr, placeName = p.name, lat = p.lat, lng = p.lng, region = MarketRegion.regionName(addr), regionCode = null)
                scope.launch { MarketRegion.match(addr, api.http.token())?.let { r -> form = form.copy(region = r.name, regionCode = r.code) } }
            },
            onDismiss = { showSearch = false },
        )
    }
    if (showExit) {
        MuyeonDialog(
            title = "작성을 그만두고 나갈까요?",
            message = "작성 중인 내용은 저장되지 않아요.",
            actions = listOf(
                MuyeonDialogAction("나가기", MuyeonDialogKind.DESTRUCTIVE) { onClose() },
                MuyeonDialogAction("계속 작성", MuyeonDialogKind.CANCEL),
            ),
            onDismiss = { showExit = false },
        )
    }
    error?.let { msg -> MuyeonDialog(title = msg, actions = listOf(MuyeonDialogAction("확인")), onDismiss = { error = null }) }
}

@Composable
private fun AddRowLink(label: String, onClick: () -> Unit) {
    Text(
        "＋ $label", fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = MuyeonColors.primary,
        modifier = Modifier.clickable(onClick = onClick).padding(vertical = 6.dp),
    )
}

@Composable
private fun HourStepper(value: Int, min: Int, max: Int, onChange: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(Icons.Filled.Remove to (value > min), Icons.Filled.Add to (value < max)).forEachIndexed { i, (icon, en) ->
            if (i == 1) Text(String.format(java.util.Locale.KOREA, "%02d시", value), fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = MuyeonColors.textHead)
            Box(
                Modifier.size(32.dp).alpha(if (en) 1f else 0.35f).clip(CircleShape).border(1.dp, MuyeonList.ink300, CircleShape)
                    .clickable(enabled = en) { onChange(if (i == 0) value - 1 else value + 1) },
                Alignment.Center,
            ) { Icon(icon, null, tint = MuyeonColors.textHead, modifier = Modifier.size(16.dp)) }
        }
    }
}

package com.muyeon.app.ui.transfer

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.muyeon.app.ui.common.FormDateRow
import com.muyeon.app.ui.common.FormField
import com.muyeon.app.ui.common.FormTextField
import com.muyeon.app.ui.common.FormToggle
import com.muyeon.app.ui.common.MarketDatePickerDialog
import com.muyeon.app.ui.common.MarketInfoTable
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
import com.muyeon.app.ui.lesson.LessonPlace
import com.muyeon.app.ui.lesson.LocationPreviewCard
import com.muyeon.app.ui.lesson.LocationSearchSheet
import com.muyeon.app.ui.lesson.openLessonNaverMap
import com.muyeon.app.ui.quote.RegionPickRow
import kotlinx.coroutines.launch

private val STEPS = listOf("사진", "위치", "시설·면적", "금액", "옵션", "소개", "연락", "미리보기")
private const val MAX_IMAGES = 10

/**
 * 양도 글 등록·수정 위저드 — 채용 공고 위저드와 같은 단계형(상단 진행 + 하단 [이전][다음]).
 *  ① 사진 ② 위치 ③ 시설·면적 ④ 금액 ⑤ 옵션 ⑥ 소개 ⑦ 연락 → 미리보기 → [등록]. 작성 중 나가면 확인.
 */
@Composable
fun TransferWizardScreen(
    api: TransferApi,
    editId: Int?,
    onClose: () -> Unit,
    onSaved: (Int) -> Unit,
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val scroll = rememberScrollState()
    var form by remember { mutableStateOf(TransferForm()) }
    var baseline by remember { mutableStateOf(TransferForm()) }
    var step by remember { mutableIntStateOf(0) }
    var loading by remember { mutableStateOf(editId != null) }
    var saving by remember { mutableStateOf(false) }
    var uploading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var showExit by remember { mutableStateOf(false) }
    var showSearch by remember { mutableStateOf(false) }
    var showDate by remember { mutableStateOf(false) }

    LaunchedEffect(editId) {
        if (editId != null) {
            api.detail(editId).onSuccess { form = TransferForm.from(it); baseline = form }.onFailure { error = it.message }
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
                UploadImagePreparer.prepare(ctx, u)?.let { bytes ->
                    api.uploadImage(bytes).onSuccess { list = list + it }.onFailure { error = it.message }
                }
            }
            form = form.copy(images = list)
            uploading = false
        }
    }

    val dirty = form != baseline
    fun requestBack() {
        if (step > 0) { step -= 1; return }
        if (dirty) showExit = true else onClose()
    }
    BackHandler { requestBack() }

    val canNext = when (step) {
        2 -> form.title.isNotBlank()
        else -> !uploading
    }

    fun save() {
        if (form.title.isBlank()) { step = 2; error = "제목을 입력해 주세요."; return }
        saving = true
        scope.launch {
            api.save(editId, form).onSuccess { baseline = form; onSaved(it) }.onFailure { error = it.message ?: "저장에 실패했어요." }
            saving = false
        }
    }

    Column(Modifier.fillMaxSize().background(Color.White)) {
        Box(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp)) {
            Text(
                if (editId != null) "양도 글 수정" else "양도 글 등록",
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
                    SectionHead("위치", "주소를 검색해 주세요. 정확한 위치는 공개를 켠 경우에만 회원에게 보여요.")
                    AddressBlock(
                        name = form.placeName, address = form.roadAddress, lat = form.lat, lng = form.lng,
                        onSearch = { showSearch = true },
                    )
                    FormField("상세 주소") { FormTextField(form.addressDetail, { form = form.copy(addressDetail = it) }, "예) 3층 301호", 100) }
                    FormField("지역", hint = "(목록 지역 필터에 쓰여요)") {
                        RegionPickRow(token = api.http.token(), placeholder = "지역을 선택하세요", name = form.region, allowSidoAll = false) { name, code ->
                            form = form.copy(region = name, regionCode = code)
                        }
                    }
                    FormToggle("정확한 위치 공개", form.exactLocationVisible) { form = form.copy(exactLocationVisible = it) }
                }
                2 -> {
                    SectionHead("시설·면적", "어떤 공간인지 알려 주세요.")
                    FormField("제목", required = true) { FormTextField(form.title, { form = form.copy(title = it) }, "예) 강서구 45평 무용학원 양도", 60) }
                    FormField("시설 유형") { FormChipWrap(TransferLabels.facility, { form.facilityType == it }) { form = form.copy(facilityType = if (form.facilityType == it) null else it) } }
                    FormField("운영 상태") { FormChipWrap(TransferLabels.operating, { form.operatingStatus == it }) { form = form.copy(operatingStatus = if (form.operatingStatus == it) null else it) } }
                    FormField("평수", hint = "(평)") { FormTextField(form.areaPyeong, { v -> form = form.copy(areaPyeong = v.filter { it.isDigit() || it == '.' }) }, "예) 45", 7, keyboardType = KeyboardType.Decimal) }
                    FormField("층") { FormTextField(form.floor, { form = form.copy(floor = it) }, "예) 2 또는 지하1", 10) }
                    FormToggle("엘리베이터", form.hasElevator) { form = form.copy(hasElevator = it) }
                    FormField("주차", hint = "(대)") { NumberField(form.parkingCount, "예) 2", 4) { form = form.copy(parkingCount = it) } }
                }
                3 -> {
                    SectionHead("금액", "금액은 만원 단위예요. 비워 두면 '협의'로 보여요.")
                    FormField("보증금", hint = "(만원)") { NumberField(form.deposit, "예) 1800") { form = form.copy(deposit = it) } }
                    FormField("월세", hint = "(만원)") { NumberField(form.monthlyRent, "예) 140") { form = form.copy(monthlyRent = it) } }
                    FormField("관리비", hint = "(만원)") { NumberField(form.maintenanceFee, "예) 15") { form = form.copy(maintenanceFee = it) } }
                    FormField("권리금", hint = "(만원)") { NumberField(form.premium, "예) 1000") { form = form.copy(premium = it) } }
                    FormToggle("권리금 협의가능", form.premiumNegotiable) { form = form.copy(premiumNegotiable = it) }
                    FormToggle("부가세 별도", form.vatSeparate) { form = form.copy(vatSeparate = it) }
                    FormField("시간당 대관", hint = "(원, 선택)") { NumberField(form.hourlyPrice, "예) 15000") { form = form.copy(hourlyPrice = it) } }
                }
                4 -> {
                    SectionHead("옵션", "갖춘 시설을 모두 골라 주세요.")
                    FormChipWrap(TransferLabels.options, { it in form.options }) { v ->
                        form = form.copy(options = if (v in form.options) form.options - v else form.options + v)
                    }
                }
                5 -> {
                    SectionHead("소개", "공간의 장점과 양도 조건을 자세히 적어 주세요.")
                    FormField("상세 소개") { FormTextField(form.description, { form = form.copy(description = it) }, "예) 역에서 3분, 탄성마루 시공 2년…", 5000, minLines = 6) }
                    FormField("입주 가능일") {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            FormDateRow(form.availableFrom, "날짜를 선택해 주세요", Modifier.weight(1f)) { showDate = true }
                            if (form.availableFrom != null) {
                                Text("지우기", fontFamily = customFontFamily, fontSize = 13.sp, color = MuyeonColors.textSub,
                                    modifier = Modifier.clickable { form = form.copy(availableFrom = null) }.padding(10.dp))
                            }
                        }
                    }
                    FormField("원생 수", hint = "(명)") { NumberField(form.studentCount, "예) 60", 6) { form = form.copy(studentCount = it) } }
                    FormField("월매출", hint = "(만원)") { NumberField(form.monthlyRevenue, "예) 900") { form = form.copy(monthlyRevenue = it) } }
                    FormToggle("매출 공개", form.revenueVisible) { form = form.copy(revenueVisible = it) }
                }
                6 -> {
                    SectionHead("연락", "문의는 채팅으로 받아요. 전화번호는 공개를 켠 경우에만 회원에게 보여요.")
                    FormField("담당자명") { FormTextField(form.contactName, { form = form.copy(contactName = it) }, "예) 김원장", 30) }
                    FormField("전화번호") { FormTextField(form.contactPhone, { v -> form = form.copy(contactPhone = v.filter { it.isDigit() || it == '-' }) }, "예) 010-1234-5678", 20, keyboardType = KeyboardType.Phone) }
                    FormToggle("회원에게 전화번호 공개", form.phoneVisible) { form = form.copy(phoneVisible = it) }
                }
                else -> TransferPreview(form)
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
                StageBottomButton(
                    if (last) (if (editId != null) "수정 완료" else "등록") else if (step == STEPS.lastIndex - 1) "미리보기" else "다음",
                    enabled = canNext, busy = saving,
                ) { if (last) save() else step += 1 }
            }
        }
    }

    if (showSearch) {
        LocationSearchSheet(
            initialQuery = form.roadAddress,
            placeholder = "주소·건물명 검색",
            onSelect = { p -> applyPlace(p, form, api) { form = it } ; scope.launch {
                val addr = p.address ?: p.name
                MarketRegion.match(addr, api.http.token())?.let { r -> form = form.copy(region = r.name, regionCode = r.code) }
            } },
            onDismiss = { showSearch = false },
        )
    }
    if (showDate) {
        MarketDatePickerDialog(form.availableFrom, onPick = { form = form.copy(availableFrom = it) }, onDismiss = { showDate = false })
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
    error?.let { msg ->
        MuyeonDialog(title = msg, actions = listOf(MuyeonDialogAction("확인")), onDismiss = { error = null })
    }
}

/** 장소 선택 → 도로명·좌표·지역 이름(앞 두 단어). 지역 코드는 코드표 매칭으로 따로 채운다. */
private fun applyPlace(p: LessonPlace, form: TransferForm, @Suppress("UNUSED_PARAMETER") api: TransferApi, set: (TransferForm) -> Unit) {
    val addr = p.address ?: p.name
    set(form.copy(roadAddress = addr, placeName = p.name, lat = p.lat, lng = p.lng, region = MarketRegion.regionName(addr), regionCode = null))
}

/** 주소 검색 버튼 + 고른 주소 미리보기(레슨 장소 카드 재사용). */
@Composable
internal fun AddressBlock(name: String, address: String, lat: Double?, lng: Double?, onSearch: () -> Unit) {
    val ctx = LocalContext.current
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(MuyeonList.ink100).clickable(onClick = onSearch).padding(12.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(Icons.Filled.Search, null, tint = MuyeonColors.primary, modifier = Modifier.size(18.dp))
            Text(
                if (address.isBlank()) "주소 검색" else "다른 주소로 바꾸기", fontFamily = customFontFamily, fontSize = 15.sp,
                color = if (address.isBlank()) MuyeonColors.textSub else MuyeonColors.textHead,
            )
        }
        if (address.isNotBlank()) {
            LocationPreviewCard(LessonPlace(name = name.ifBlank { address }, address = address, lat = lat, lng = lng)) {
                openLessonNaverMap(ctx, name.ifBlank { address }, lat, lng)
            }
        }
    }
}

@Composable
internal fun NumberField(value: String, placeholder: String, max: Int = 9, onChange: (String) -> Unit) {
    FormTextField(value, { v -> onChange(v.filter { it.isDigit() }) }, placeholder, max, keyboardType = KeyboardType.Number)
}

@Composable
private fun TransferPreview(f: TransferForm) {
    val card = TransferCard(
        id = 0, authorId = 0, authorName = "", title = f.title.ifBlank { "제목" }, status = "OPEN",
        facilityType = f.facilityType, operatingStatus = f.operatingStatus, region = f.region.ifBlank { null }, regionCode = f.regionCode,
        areaPyeong = f.areaPyeong.toDoubleOrNull(), floor = f.floor.ifBlank { null }, deposit = f.deposit.toIntOrNull(),
        monthlyRent = f.monthlyRent.toIntOrNull(), maintenanceFee = f.maintenanceFee.toIntOrNull(), premium = f.premium.toIntOrNull(),
        premiumNegotiable = f.premiumNegotiable, hourlyPrice = f.hourlyPrice.toIntOrNull(), options = f.options.toList(),
        image = f.images.firstOrNull(), viewCount = 0, createdAt = null, scrapped = false, isExposed = true,
    )
    SectionHead("미리보기", "목록에는 이렇게 보여요. 확인 후 등록해 주세요.")
    Box(Modifier.fillMaxWidth().clip(MuyeonList.cardShape).background(MuyeonList.pageBg).padding(12.dp)) {
        TransferCardRow(card, mine = true, onClick = {}, onHeart = null, onChat = null)
    }
    val rows = buildList {
        add("사진" to "${f.images.size}장")
        if (f.roadAddress.isNotBlank()) add("주소" to (f.roadAddress + if (f.addressDetail.isNotBlank()) " ${f.addressDetail}" else ""))
        add("위치 공개" to if (f.exactLocationVisible) "정확한 위치" else "대략적인 위치")
        TransferLabels.facility(f.facilityType)?.let { add("시설 유형" to it) }
        TransferLabels.operating(f.operatingStatus)?.let { add("운영 상태" to it) }
        if (f.options.isNotEmpty()) add("옵션" to f.options.joinToString(", ") { TransferLabels.option(it) })
        f.availableFrom?.let { add("입주 가능일" to it) }
        if (f.monthlyRevenue.isNotBlank()) add("월매출" to "${f.monthlyRevenue}만원 · ${if (f.revenueVisible) "공개" else "비공개"}")
        if (f.contactName.isNotBlank()) add("담당자" to f.contactName)
        if (f.contactPhone.isNotBlank()) add("전화번호" to "${f.contactPhone} · ${if (f.phoneVisible) "회원 공개" else "비공개"}")
    }
    MarketInfoTable(rows)
    Spacer(Modifier.height(4.dp))
}

package com.muyeon.app.ui.space

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Checkroom
import androidx.compose.material.icons.outlined.CropPortrait
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.GridOn
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.LocalBar
import androidx.compose.material.icons.outlined.LocalParking
import androidx.compose.material.icons.outlined.MedicalServices
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Opacity
import androidx.compose.material.icons.outlined.Power
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.Shower
import androidx.compose.material.icons.outlined.SmokeFree
import androidx.compose.material.icons.outlined.TableRestaurant
import androidx.compose.material.icons.outlined.Tv
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material.icons.outlined.Waves
import androidx.compose.material.icons.outlined.Wc
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.ui.graphics.vector.ImageVector
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.roundToInt

/**
 * 공간(연습실 대관) 상세 모델 + REST — iOS `SpaceModels.swift` / `SpaceService.swift` 1:1.
 *  백엔드 GET /spaces/:id (Space + priceOptions). 웹 SpaceDetail.js 가 쓰는 필드와 같다.
 *  서버가 컬럼을 비워 두는 경우가 많아 대부분 nullable.
 */

/** 공간 가격 옵션 — bookingType(HOURLY|PACKAGE) 별 라벨/단가/단위. */
data class SpacePriceOption(
    val id: Int,
    val bookingType: String?,
    val label: String?,
    val price: Int?,
    val unit: String?,
    val sort: Int?,
) {
    val isHourly: Boolean get() = (bookingType ?: "HOURLY") == "HOURLY"

    /** "20,000원 / 시간" */
    val priceText: String get() = "${SpaceDesign.won(price ?: 0)}원 / ${unit ?: "시간"}"

    companion object {
        fun from(o: JSONObject) = SpacePriceOption(
            o.optInt("id"),
            o.optString("bookingType").ifEmpty { null },
            o.optString("label").ifEmpty { null },
            if (o.has("price") && !o.isNull("price")) o.optInt("price") else null,
            o.optString("unit").ifEmpty { null },
            if (o.has("sort") && !o.isNull("sort")) o.optInt("sort") else null,
        )
    }
}

/**
 * 환불 규칙 1줄(2026-11-03 무용연 정책, 백엔드 613f4b6) — "이용일 daysBefore 일 전 이상에 취소하면 percent% 환불".
 *  저장·전송은 이 숫자 형식만 쓴다(예전 {label,value} 글자 형식은 보내지 않는다).
 */
data class SpaceRefund(val daysBefore: Int, val percent: Int) {
    fun toJson(): JSONObject = JSONObject().put("daysBefore", daysBefore).put("percent", percent)

    companion object {
        /**
         * 서버 값 → 규칙(큰 daysBefore 순). 배포 전 서버가 주는 예전 글자 형식도 읽는다
         *  (서버 normalizeRefund 와 같은 해석). 못 읽으면 빈 목록.
         */
        fun listFrom(arr: JSONArray?): List<SpaceRefund> {
            if (arr == null) return emptyList()
            val out = (0 until arr.length()).mapNotNull { i ->
                val o = arr.optJSONObject(i) ?: return@mapNotNull null
                if (o.has("daysBefore") && o.has("percent")) {
                    SpaceRefund(o.optInt("daysBefore").coerceIn(0, 365), o.optInt("percent").coerceIn(0, 100))
                } else {
                    val label = o.optString("label"); val value = o.optString("value")
                    val n = when {
                        label.contains("당일") -> 0
                        label.contains("전날") -> 1
                        else -> Regex("(\\d+)\\s*일").find(label)?.groupValues?.get(1)?.toIntOrNull()
                    }
                    val pct = if (Regex("불가|없음").containsMatchIn(value)) 0 else Regex("(\\d+)\\s*%").find(value)?.groupValues?.get(1)?.toIntOrNull()
                    if (n != null && pct != null) SpaceRefund(n.coerceIn(0, 365), pct.coerceIn(0, 100)) else null
                }
            }
            return out.distinctBy { it.daysBefore }.sortedByDescending { it.daysBefore }
        }
    }
}

/** 환불 표 1줄 — 서버가 만든 글자 그대로("이용 3일 전까지" · "전액 환불"). */
data class SpaceRefundRow(val whenText: String, val refundText: String, val daysBefore: Int?, val percent: Int?)

/**
 * 공간 상세의 환불 표시 — GET /spaces/:id 의 refund{ preset, presetLabel, rows, freeCancelNote, policyUrl }.
 *  앱은 글자를 만들지 않고 받은 대로 그린다. 배포 전 서버(refund 없음)만 숫자 규칙으로 간단 표시한다.
 */
data class SpaceRefundView(
    val preset: String,
    val presetLabel: String?,
    val rows: List<SpaceRefundRow>,
    val freeCancelNote: String?,
    val policyUrl: String,
) {
    companion object {
        fun from(o: JSONObject) = SpaceRefundView(
            preset = o.optString("preset").ifEmpty { "CUSTOM" },
            presetLabel = o.optString("presetLabel").ifEmpty { null },
            rows = refundRows(o.optJSONArray("rows")),
            freeCancelNote = o.optString("freeCancelNote").ifEmpty { null },
            policyUrl = o.optString("policyUrl").ifEmpty { SPACE_REFUND_POLICY },
        )

        /** 배포 전 서버 대체 — 숫자 규칙을 간단한 줄로(서버 refundRuleLabels 와 같은 모양). */
        fun fallback(rules: List<SpaceRefund>) = SpaceRefundView(
            preset = "CUSTOM", presetLabel = null, rows = simpleRows(rules.ifEmpty { DEFAULT_REFUND_RULES }),
            freeCancelNote = null, policyUrl = SPACE_REFUND_POLICY,
        )

        fun refundRows(arr: JSONArray?): List<SpaceRefundRow> = arr?.let { a ->
            (0 until a.length()).mapNotNull { i ->
                a.optJSONObject(i)?.let {
                    SpaceRefundRow(
                        it.optString("when"), it.optString("refund"),
                        if (it.has("daysBefore")) it.optInt("daysBefore") else null,
                        if (it.has("percent")) it.optInt("percent") else null,
                    )
                }
            }
        } ?: emptyList()

        private fun simpleRows(rules: List<SpaceRefund>): List<SpaceRefundRow> = rules.mapIndexed { i, r ->
            val upper = if (i == 0) null else rules[i - 1].daysBefore - 1
            val whenText = when {
                upper == null -> if (r.daysBefore == 0) "이용 당일까지" else "이용 ${r.daysBefore}일 전까지"
                r.daysBefore == upper -> if (r.daysBefore == 0) "이용 당일" else "이용 ${r.daysBefore}일 전"
                else -> if (r.daysBefore == 0) "이용 ${upper}일 전~당일" else "이용 ${r.daysBefore}~${upper}일 전"
            }
            val refundText = when { r.percent >= 100 -> "전액 환불"; r.percent <= 0 -> "환불 없음"; else -> "결제 금액의 ${r.percent}% 환불" }
            SpaceRefundRow(whenText, refundText, r.daysBefore, r.percent)
        }
    }
}

/** 등록 화면 환불 단계 — GET /spaces/refund-presets 의 presets[]. */
data class SpaceRefundPreset(val key: String, val label: String, val rules: List<SpaceRefund>, val rows: List<SpaceRefundRow>)
data class SpaceRefundPresets(val defaultPreset: String, val presets: List<SpaceRefundPreset>, val policyUrl: String) {
    companion object {
        fun from(o: JSONObject) = SpaceRefundPresets(
            defaultPreset = o.optString("defaultPreset").ifEmpty { "STANDARD" },
            presets = o.optJSONArray("presets")?.let { a ->
                (0 until a.length()).mapNotNull { i ->
                    a.optJSONObject(i)?.let {
                        SpaceRefundPreset(it.optString("key"), it.optString("label"), SpaceRefund.listFrom(it.optJSONArray("rules")), SpaceRefundView.refundRows(it.optJSONArray("rows")))
                    }
                }
            } ?: emptyList(),
            policyUrl = o.optString("policyUrl").ifEmpty { SPACE_REFUND_POLICY },
        )
    }
}

data class SpaceDetail(
    val id: Int,
    val name: String,
    val ownerId: Int?,
    val genre: String?,
    val region: String?,
    val addressDetail: String?,
    val subwayInfo: String?,
    val tags: List<String>?,
    val pricePerHour: Int?,
    val images: List<String>?,
    val description: String?,     // 결제안내 등 짧은 설명
    val intro: String?,           // 공간소개(긴 텍스트)
    val capacityMin: Int?,
    val capacityMax: Int?,
    val areaM2: Int?,
    val spaceType: String?,
    val businessHours: String?,
    val holidays: String?,
    val facilities: Map<String, Boolean>?,
    val facilityList: List<String>?,
    val cautions: List<String>?,
    /** 숫자 규칙(서버가 늘 정리해서 준다). */
    val refundPolicy: List<SpaceRefund>,
    val hourlyEnabled: Boolean?,
    val hourlyMinHours: Int?,
    val packageEnabled: Boolean?,
    val phone: String?,
    val priceOptions: List<SpacePriceOption>?,
    // ★ 2026-11-03 결제 예약·호스트 — GET /spaces/:id 확장 필드
    val regionCode: String? = null,
    val categoryId: Int? = null,
    val roadAddress: String? = null,
    val lat: Double? = null,
    val lng: Double? = null,
    val hostName: String? = null,
    val isOwner: Boolean = false,
    val otherSpaces: List<SpaceListItem> = emptyList(),
    val baseHeadcount: Int? = null,
    val extraPersonFee: Int? = null,
    val openDays: List<Int> = emptyList(),
    val closedDates: List<String> = emptyList(),
    val bookingLeadMinutes: Int? = null,
    val status: String? = null,
    /** ★ 88f738b 예약 방식 — REQUEST(호스트 수락 후 확정, 기본) | INSTANT(바로 확정). */
    val approvalMode: String = "REQUEST",
    /** 호스트 예상 응답 시간 표시(서버 hostResponse.label, 예: "보통 2시간 안에 응답") — REQUEST 이고 기록이 있을 때만. */
    val hostResponseLabel: String? = null,
    /** 환불 표시(서버 refund). 배포 전 서버면 숫자 규칙으로 만든 대체. */
    val refund: SpaceRefundView = SpaceRefundView.fallback(emptyList()),
    /** 예약 동의 항목(서버 bookingConsents, 순서 그대로). 배포 전 서버면 기존 두 줄 대체. */
    val bookingConsents: List<SpaceConsentDoc> = FALLBACK_BOOKING_CONSENTS,
) {
    /** 그날 문을 여는지 — 운영 요일·휴무 날짜(서버 availability 와 같은 기준). */
    fun isOpenOn(ymd: String): Boolean =
        (openDays.isEmpty() || openDays.contains(com.muyeon.app.ui.common.market.MarketDates.dayOfWeek(ymd))) && !closedDates.contains(ymd)

    val hourlyOptions: List<SpacePriceOption> get() = (priceOptions ?: emptyList()).filter { it.isHourly }
    val packageOptions: List<SpacePriceOption> get() = (priceOptions ?: emptyList()).filter { !it.isHourly }

    /** 가격옵션·대표단가 중 최저가. 시안의 '최종 결제 금액' 자리. */
    val lowestPrice: Int?
        get() {
            val candidates = (priceOptions ?: emptyList()).mapNotNull { it.price }.filter { it > 0 }.toMutableList()
            pricePerHour?.takeIf { it > 0 }?.let { candidates.add(it) }
            return candidates.minOrNull()
        }

    val businessHoursText: String get() = businessHours?.takeIf { it.isNotEmpty() } ?: "문의"
    val holidaysText: String get() = holidays?.takeIf { it.isNotEmpty() } ?: "연중무휴"

    val capacityText: String
        get() = when {
            capacityMin != null && capacityMax != null -> "${capacityMin}~${capacityMax}명"
            capacityMin != null -> "${capacityMin}명~"
            capacityMax != null -> "~${capacityMax}명"
            else -> "문의"
        }

    /** 공간면적 "35㎡ (약 11평)" */
    val areaText: String?
        get() = areaM2?.takeIf { it > 0 }?.let { "${it}㎡ (약 ${(it / 3.3058).roundToInt()}평)" }

    /** 티켓 카드 중앙 필 배지 — 예약 방식 */
    val bookingBadge: String
        get() = if (hourlyEnabled == false && packageEnabled == true) "패키지" else "시간제"

    /** 시설 on 항목만 (시안 순서 = 웹 FACILITY_META 순서) */
    val enabledFacilities: List<SpaceFacility>
        get() = facilities?.let { flags -> SpaceFacility.ALL.filter { flags[it.key] == true } } ?: emptyList()

    val addressText: String?
        get() = listOfNotNull(roadAddress?.takeIf { it.isNotEmpty() }, addressDetail?.takeIf { it.isNotEmpty() })
            .joinToString(" ").ifEmpty { null } ?: region?.takeIf { it.isNotEmpty() }

    companion object {
        fun from(o: JSONObject): SpaceDetail {
            fun strings(key: String): List<String>? = o.optJSONArray(key)?.let { arr ->
                (0 until arr.length()).map { arr.optString(it) }.filter { it.isNotEmpty() }
            }
            fun intOrNull(key: String) = if (o.has(key) && !o.isNull(key)) o.optInt(key) else null
            fun boolOrNull(key: String) = if (o.has(key) && !o.isNull(key)) o.optBoolean(key) else null
            return SpaceDetail(
                id = o.optInt("id"),
                name = o.optString("name").ifEmpty { "공간" },
                ownerId = intOrNull("ownerId"),
                genre = o.optString("genre").ifEmpty { null },
                region = o.optString("region").ifEmpty { null },
                addressDetail = o.optString("addressDetail").ifEmpty { null },
                subwayInfo = o.optString("subwayInfo").ifEmpty { null },
                tags = strings("tags"),
                pricePerHour = intOrNull("pricePerHour"),
                images = strings("images"),
                description = o.optString("description").ifEmpty { null },
                intro = o.optString("intro").ifEmpty { null },
                capacityMin = intOrNull("capacityMin"),
                capacityMax = intOrNull("capacityMax"),
                areaM2 = intOrNull("areaM2"),
                spaceType = o.optString("spaceType").ifEmpty { null },
                businessHours = o.optString("businessHours").ifEmpty { null },
                holidays = o.optString("holidays").ifEmpty { null },
                facilities = o.optJSONObject("facilities")?.let { f ->
                    f.keys().asSequence().associateWith { f.optBoolean(it) }
                },
                facilityList = strings("facilityList"),
                cautions = strings("cautions"),
                refundPolicy = SpaceRefund.listFrom(o.optJSONArray("refundPolicy")),
                hourlyEnabled = boolOrNull("hourlyEnabled"),
                hourlyMinHours = intOrNull("hourlyMinHours"),
                packageEnabled = boolOrNull("packageEnabled"),
                phone = o.optString("phone").ifEmpty { null },
                priceOptions = o.optJSONArray("priceOptions")?.let { arr ->
                    (0 until arr.length()).mapNotNull { i -> arr.optJSONObject(i)?.let(SpacePriceOption::from) }
                },
                regionCode = o.optString("regionCode").ifEmpty { null },
                categoryId = intOrNull("categoryId"),
                roadAddress = o.optString("roadAddress").ifEmpty { null }?.takeIf { it != "null" },
                lat = o.optDouble("lat").takeIf { !it.isNaN() },
                lng = o.optDouble("lng").takeIf { !it.isNaN() },
                hostName = o.optString("hostName").ifEmpty { null },
                isOwner = o.optBoolean("isOwner"),
                otherSpaces = o.optJSONArray("otherSpaces")?.let { arr ->
                    (0 until arr.length()).mapNotNull { i -> arr.optJSONObject(i)?.let(SpaceListItem::from) }
                } ?: emptyList(),
                baseHeadcount = intOrNull("baseHeadcount"),
                extraPersonFee = intOrNull("extraPersonFee"),
                openDays = o.optJSONArray("openDays")?.let { a -> (0 until a.length()).map { a.optInt(it) } } ?: emptyList(),
                closedDates = strings("closedDates") ?: emptyList(),
                bookingLeadMinutes = intOrNull("bookingLeadMinutes"),
                status = o.optString("status").ifEmpty { null },
                approvalMode = o.optString("approvalMode").ifEmpty { "REQUEST" },
                hostResponseLabel = o.optJSONObject("hostResponse")?.optString("label")?.takeIf { it.isNotBlank() && it != "null" },
                refund = o.optJSONObject("refund")?.let(SpaceRefundView::from)
                    ?: SpaceRefundView.fallback(SpaceRefund.listFrom(o.optJSONArray("refundPolicy"))),
                bookingConsents = o.optJSONArray("bookingConsents")?.let(SpaceConsentDoc::listFrom)?.takeIf { it.isNotEmpty() }
                    ?: FALLBACK_BOOKING_CONSENTS,
            )
        }
    }
}

/**
 * 시설 아이콘 메타 — 웹 components/muyeon/SpaceFacilityIcons.js 의 FACILITY_META 와 키/순서 동일.
 *  iOS 는 SF Symbol, 여기서는 의미가 일치하는 Material 아이콘으로 대응한다.
 */
data class SpaceFacility(val key: String, val label: String, val icon: ImageVector) {
    companion object {
        val ALL: List<SpaceFacility> = listOf(
            SpaceFacility("woodFloor", "마루 바닥", Icons.Outlined.GridOn),
            SpaceFacility("springFloor", "탄성 마루", Icons.Outlined.Waves),
            SpaceFacility("danceFloor", "댄스 플로어", Icons.Outlined.MusicNote),
            SpaceFacility("parking", "주차 가능", Icons.Outlined.LocalParking),
            SpaceFacility("liquor", "주류반입 가능", Icons.Outlined.LocalBar),
            SpaceFacility("sound", "음향/마이크", Icons.Outlined.Mic),
            SpaceFacility("furniture", "의자/테이블", Icons.Outlined.TableRestaurant),
            SpaceFacility("mart", "마트/편의점", Icons.Outlined.ShoppingCart),
            SpaceFacility("electric", "전기", Icons.Outlined.Power),
            SpaceFacility("equipment", "장비대여", Icons.Outlined.Inventory2),
            SpaceFacility("changingRoom", "탈의실", Icons.Outlined.Checkroom),
            SpaceFacility("shower", "샤워실", Icons.Outlined.Shower),
            SpaceFacility("fullMirror", "전신거울", Icons.Outlined.CropPortrait),
            SpaceFacility("foodAllowed", "음식물 반입가능", Icons.Outlined.Restaurant),
            SpaceFacility("firstAid", "구급약품", Icons.Outlined.MedicalServices),
            SpaceFacility("hotWater", "온수", Icons.Outlined.WaterDrop),
            SpaceFacility("restroom", "내부 화장실", Icons.Outlined.Wc),
            SpaceFacility("nonSmoking", "금연", Icons.Outlined.SmokeFree),
            SpaceFacility("genderRestroom", "남/여 화장실 구분", Icons.Outlined.Wc),
            SpaceFacility("tvProjector", "TV/프로젝터", Icons.Outlined.Tv),
            SpaceFacility("whiteboard", "화이트보드", Icons.Outlined.Dashboard),
            SpaceFacility("wifi", "인터넷/WIFI", Icons.Outlined.Wifi),
            SpaceFacility("waterSupply", "급수시설", Icons.Outlined.Opacity),
        )
    }
}

/** 요금 카드 상세행 1건 — └ 리드 + (배지) 라벨 ↔ 값. */
data class SpaceFeeRow(val badge: String?, val title: String, val value: String)

/** 공간 목록 카드 — GET /spaces (+minPrice 시간당 최저가) · 상세의 otherSpaces. */
data class SpaceListItem(
    val id: Int,
    val name: String,
    val region: String?,
    val image: String?,
    val capacityMax: Int?,
    val minPrice: Int?,
    /** 갖춘 시설 키(SpaceFacility.key) — 필터가 서버 배포 전에도 걸러지게 화면에서도 본다. */
    val facilities: Set<String> = emptySet(),
) {
    /** "서울 동작구 · 최대 15명" */
    val metaLine: String get() = listOfNotNull(region?.takeIf { it.isNotBlank() }, capacityMax?.let { "최대 ${it}명" }).joinToString(" · ")
    /** "시간당 7,000원~" */
    val priceLine: String? get() = minPrice?.takeIf { it > 0 }?.let { "시간당 ${SpaceDesign.won(it)}원~" }

    companion object {
        fun from(o: JSONObject): SpaceListItem {
            val imgs = o.optJSONArray("images")
            fun intOrNull(k: String) = if (o.has(k) && !o.isNull(k)) o.optInt(k) else null
            return SpaceListItem(
                id = o.optInt("id"), name = o.optString("name").ifEmpty { "공간" },
                region = o.optString("region").ifEmpty { null },
                image = imgs?.optString(0)?.takeIf { it.isNotEmpty() },
                capacityMax = intOrNull("capacityMax"),
                minPrice = intOrNull("minPrice") ?: intOrNull("pricePerHour"),
                facilities = o.optJSONObject("facilities")?.let { f -> f.keys().asSequence().filter { f.optBoolean(it) }.toSet() } ?: emptySet(),
            )
        }
    }
}

data class SpaceCategory(val id: Int, val name: String)

/** 예약 가능 시간 — 분 단위로 바꿔 둔다. blocked = [시작, 끝) 목록. */
data class SpaceAvailability(val open: Boolean, val start: Int, val end: Int, val minHours: Double, val blocked: List<Pair<Int, Int>>) {
    /** start(분)부터 hours 시간이 막힌 구간과 겹치지 않고 영업시간 안인지. */
    fun fits(startMin: Int, hours: Double): Boolean {
        val endMin = startMin + (hours * 60).toInt()
        if (startMin < start || endMin > end) return false
        return blocked.none { (b0, b1) -> startMin < b1 && endMin > b0 }
    }

    companion object {
        fun minutes(hhmm: String?): Int? = hhmm?.split(":")?.takeIf { it.size == 2 }?.let { (h, m) -> (h.toIntOrNull() ?: return null) * 60 + (m.toIntOrNull() ?: 0) }
        fun hhmm(min: Int): String = String.format(java.util.Locale.KOREA, "%02d:%02d", min / 60, min % 60)
        fun from(o: JSONObject) = SpaceAvailability(
            open = o.optBoolean("open"),
            start = minutes(o.optString("start")) ?: 0,
            end = minutes(o.optString("end")) ?: 24 * 60,
            minHours = o.optDouble("minHours", 1.0).takeIf { !it.isNaN() } ?: 1.0,
            blocked = o.optJSONArray("blocked")?.let { a ->
                (0 until a.length()).mapNotNull { i ->
                    val b = a.optJSONObject(i) ?: return@mapNotNull null
                    val s0 = minutes(b.optString("start")) ?: return@mapNotNull null
                    val e0 = minutes(b.optString("end")) ?: return@mapNotNull null
                    s0 to e0
                }
            } ?: emptyList(),
        )
    }
}

/** 공간 예약 요청 본문 — POST /spaces/:id/reservation (결제 대기 10분 자리 잡기). */
data class SpaceReservationRequest(
    val optionId: Int?,
    val date: String,
    val startTime: String,
    val hours: Double,
    val headcount: Int,
    val message: String?,
    /** 예약 정보 호스트 제공 동의(필수, 2026-11-03 백엔드 BOOKING_CONSENT_REQUIRED). */
    val agreeThirdParty: Boolean,
) {
    fun toJson(): JSONObject {
        val o = JSONObject().put("date", date).put("startTime", startTime).put("hours", hours).put("headcount", headcount)
            .put("agreeThirdParty", agreeThirdParty)
        optionId?.let { o.put("optionId", it) }
        message?.trim()?.takeIf { it.isNotEmpty() }?.let { o.put("message", it) }
        return o
    }
}

/** 예약 행 — 내 예약(예약자) / 예약 관리(호스트, bookerName). */
data class SpaceReservationRow(
    val id: Int,
    val spaceId: Int,
    val spaceName: String,
    val spaceImage: String?,
    val spaceRegion: String?,
    val date: String,
    val startTime: String?,
    val hours: Double?,
    val headcount: Int?,
    val amount: Int?,
    val status: String,
    val refundAmount: Int?,
    val paidAt: String?,
    val canceledAt: String?,
    val bookerName: String?,
    val message: String?,
    // ★ 호스트 예약 상세용(GET /spaces/reservations/hosting) — optionLabel 은 백엔드 a8363f3 부터.
    val optionLabel: String? = null,
    val cancelReason: String? = null,
    val userId: Int? = null,
    val createdAt: String? = null,
    /** ★ 88f738b 수락 후 확정 — PENDING(호스트 응답 대기) | ACCEPTED | null. status 는 PAID 그대로. */
    val approvalStatus: String? = null,
    val respondBy: String? = null,
    val respondedAt: String? = null,
) {
    /** 결제는 됐고 호스트 응답을 기다리는 중. */
    val awaitingHost: Boolean get() = status == "PAID" && approvalStatus == "PENDING"

    /** 예약자 쪽 상태 글자 — 응답 대기면 "응답 대기". */
    val bookerStatusLabel: String get() = if (awaitingHost) "응답 대기" else statusLabel

    /** 호스트 쪽 상태 글자 — 응답 대기면 "수락 대기". */
    val hostStatusLabel: String get() = if (awaitingHost) "수락 대기" else statusLabel

    /** 응답 기한(한국 시간 "2026.11.04 18:00"). */
    val respondByText: String? get() = kstDateTime(respondBy)

    val statusLabel: String get() = SpaceReservationStatus.label(status)

    /** 끝 시각 "16:00" — 시작 + 이용 시간(자정 넘으면 24:00 이상 그대로). */
    val endTime: String?
        get() {
            val st = startTime?.split(":")?.takeIf { it.size == 2 } ?: return null
            val h = hours ?: return null
            val m = (st[0].toIntOrNull() ?: return null) * 60 + (st[1].toIntOrNull() ?: 0) + (h * 60).toInt()
            return String.format(java.util.Locale.KOREA, "%02d:%02d", m / 60, m % 60)
        }

    /** "2026-11-20 14:00 ~ 16:00, 2시간" */
    val rangeText: String
        get() {
            val h = hours?.let { v -> if (v % 1.0 == 0.0) "${v.toInt()}시간" else "${v.toInt()}시간 30분" }
            val time = listOfNotNull(startTime, endTime).joinToString(" ~ ")
            return listOf(listOf(date, time).filter { it.isNotEmpty() }.joinToString(" "), h).filterNotNull().joinToString(", ")
        }

    /** "2026-11-20 14:00 · 2시간" */
    val whenText: String
        get() {
            val h = hours?.let { v -> if (v % 1.0 == 0.0) "${v.toInt()}시간" else "${v.toInt()}시간 30분" }
            return listOfNotNull(listOfNotNull(date, startTime).joinToString(" "), h).joinToString(" · ")
        }

    /** 이용 시작 시각이 지났는지(한국 시간 문자열 비교). */
    fun isPast(nowYmdHm: String): Boolean = "$date ${startTime ?: "00:00"}" < nowYmdHm

    val isCanceled: Boolean get() = status.startsWith("CANCELED") || status == "EXPIRED"

    companion object {
        fun from(o: JSONObject): SpaceReservationRow {
            fun intOrNull(k: String) = if (o.has(k) && !o.isNull(k)) o.optInt(k) else null
            fun str(k: String) = if (o.has(k) && !o.isNull(k)) o.optString(k).ifEmpty { null } else null
            return SpaceReservationRow(
                id = o.optInt("id"), spaceId = o.optInt("spaceId"), spaceName = o.optString("spaceName").ifEmpty { "공간" },
                spaceImage = str("spaceImage"), spaceRegion = str("spaceRegion"), date = o.optString("date").take(10),
                startTime = str("startTime")?.take(5),
                hours = if (o.has("hours") && !o.isNull("hours")) o.optDouble("hours").takeIf { !it.isNaN() } else null,
                headcount = intOrNull("headcount"), amount = intOrNull("amount"), status = o.optString("status"),
                refundAmount = intOrNull("refundAmount"), paidAt = str("paidAt"), canceledAt = str("canceledAt"),
                bookerName = str("bookerName"), message = str("message"),
                optionLabel = str("optionLabel"), cancelReason = str("cancelReason"), userId = intOrNull("userId"), createdAt = str("createdAt"),
                approvalStatus = str("approvalStatus"), respondBy = str("respondBy"), respondedAt = str("respondedAt"),
            )
        }
    }
}

object SpaceReservationStatus {
    private val labels = mapOf(
        "PENDING_PAYMENT" to "결제 대기", "PAID" to "예약 확정", "CANCELED_BY_USER" to "취소(예약자)",
        "CANCELED_BY_HOST" to "취소(호스트)", "EXPIRED" to "결제 시간 초과", "REQUESTED" to "요청",
        "CONFIRMED" to "확정", "CANCELED" to "취소",
    )
    fun label(status: String): String = labels[status] ?: status
}

/** 동의 문서 — 제목·본문은 관리자 '약관·정책'이 정한다(앱은 받은 대로 "[필수] {title}" + 보기 ›). */
data class SpaceConsentDoc(val docKey: String, val title: String, val version: String?, val url: String, val required: Boolean = true) {
    companion object {
        fun listFrom(a: JSONArray): List<SpaceConsentDoc> = (0 until a.length()).mapNotNull { i ->
            a.optJSONObject(i)?.let {
                SpaceConsentDoc(
                    it.optString("docKey"), it.optString("title").ifEmpty { it.optString("docKey") }, it.optString("version").ifEmpty { null },
                    it.optString("url").ifEmpty { "/policy?doc=${it.optString("docKey")}&embed=1" },
                    if (it.has("required")) it.optBoolean("required") else true,
                )
            }
        }
    }
}

/** 호스트 이용 동의 — GET/POST /spaces/host-consent. docs = 서버 약관(제목·웹 경로). */
data class SpaceHostConsent(val agreed: Boolean, val docs: List<SpaceConsentDoc>) {
    companion object {
        fun from(o: JSONObject) = SpaceHostConsent(
            agreed = o.optBoolean("agreed"),
            docs = o.optJSONArray("docs")?.let(SpaceConsentDoc::listFrom) ?: emptyList(),
        )
    }
}

/** 무용연 취소·환불 정책 웹 경로(서버가 policyUrl 을 안 줄 때만). */
const val SPACE_REFUND_POLICY = "/policy?doc=spaceRefundPolicy&embed=1"

/** 예약 제3자 제공 동의 약관 웹 경로. */
const val SPACE_BOOKING_THIRD_PARTY_POLICY = "/policy?doc=spaceBookingThirdParty&embed=1"

/** POST /spaces 가 호스트 동의 전이면 주는 오류 코드(403). */
const val HOST_CONSENT_REQUIRED = "HOST_CONSENT_REQUIRED"

class SpaceApi(context: android.content.Context) {

    val http = com.muyeon.app.ui.common.NativeHttp(context)

    val isLoggedIn: Boolean get() = http.isLoggedIn

    private suspend fun call(path: String, method: String = "GET", body: JSONObject? = null): Result<String> =
        http.call(path, method, body)

    /** 공간 상세(가격옵션·호스트·다른 공간 포함) — GET /spaces/:id */
    suspend fun detail(id: Int): Result<SpaceDetail> =
        call("/spaces/$id").mapCatching { SpaceDetail.from(JSONObject(it.ifBlank { "{}" })) }

    suspend fun categories(): Result<List<SpaceCategory>> =
        call("/spaces/categories").mapCatching { body ->
            val arr = JSONArray(body.ifBlank { "[]" })
            (0 until arr.length()).mapNotNull { i -> arr.optJSONObject(i)?.let { SpaceCategory(it.optInt("id"), it.optString("name")) } }
        }

    /** 목록 — sido(시도 코드 2자리)·categoryId·date·headcount·sort(recent|priceAsc|priceDesc). */
    suspend fun list(sido: String?, categoryId: Int?, date: String?, headcount: Int?, sort: String, filter: SpaceFilter = SpaceFilter()): Result<List<SpaceListItem>> {
        val q = mutableListOf("sort=$sort")
        if (filter.facilities.isNotEmpty()) q += "facilities=" + filter.facilities.joinToString(",")
        filter.bookingType?.let { q += "bookingType=$it" }
        filter.minWon?.let { q += "minPrice=$it" }
        filter.maxWon?.let { q += "maxPrice=$it" }
        sido?.let { q += "sido=$it" }
        categoryId?.let { q += "categoryId=$it" }
        date?.let { q += "date=$it" }
        headcount?.let { q += "headcount=$it" }
        return call("/spaces?" + q.joinToString("&")).mapCatching { body ->
            val arr = JSONArray(body.ifBlank { "[]" })
            (0 until arr.length()).mapNotNull { i -> arr.optJSONObject(i)?.let(SpaceListItem::from) }.filter(filter::matches)
        }
    }

    /** 내 공간 찜 목록 — GET /me/scraps?targetType=SPACE. 비로그인/실패는 빈 집합(찜 버튼만 꺼짐). */
    suspend fun scrappedSpaceIds(): Set<Int> {
        if (!isLoggedIn) return emptySet()
        return call("/me/scraps?targetType=SPACE").map { body ->
            val arr = JSONArray(body.ifBlank { "[]" })
            (0 until arr.length()).mapNotNull { i ->
                arr.optJSONObject(i)?.let { if (it.has("targetId")) it.optInt("targetId") else null }
            }.toSet()
        }.getOrDefault(emptySet())
    }

    /** 찜 토글 — POST/DELETE /spaces/:id/scrap */
    suspend fun setScrap(id: Int, on: Boolean): Result<Unit> =
        call("/spaces/$id/scrap", if (on) "POST" else "DELETE").map { }

    suspend fun availability(id: Int, date: String, optionId: Int?): Result<SpaceAvailability> =
        call("/spaces/$id/availability?date=$date" + (optionId?.let { "&optionId=$it" } ?: ""))
            .mapCatching { SpaceAvailability.from(JSONObject(it)) }

    /** 예약 1단계 — 결제 대기로 자리 잡기 → (예약 id, 금액). 결제는 웹 /spacePay/{id}. */
    suspend fun reserve(id: Int, request: SpaceReservationRequest): Result<Pair<Int, Int>> =
        call("/spaces/$id/reservation", "POST", request.toJson()).mapCatching {
            val o = JSONObject(it)
            o.optInt("id").also { r -> if (r <= 0) error("예약을 만들지 못했어요.") } to o.optInt("amount")
        }

    suspend fun myReservations(): Result<List<SpaceReservationRow>> = rows("/spaces/reservations/mine")
    suspend fun hostReservations(): Result<List<SpaceReservationRow>> = rows("/spaces/reservations/hosting")

    private suspend fun rows(path: String): Result<List<SpaceReservationRow>> =
        call(path).mapCatching { body ->
            val arr = JSONArray(body.ifBlank { "[]" })
            (0 until arr.length()).mapNotNull { i -> arr.optJSONObject(i)?.let(SpaceReservationRow::from) }
        }

    /** 취소 미리보기 → (환불 비율 %, 환불 금액). */
    suspend fun cancelPreview(resId: Int): Result<Pair<Int, Int>> =
        call("/spaces/reservations/$resId/cancel-preview").mapCatching { val o = JSONObject(it); o.optInt("rate") to o.optInt("refund") }

    suspend fun cancel(resId: Int): Result<Int> =
        call("/spaces/reservations/$resId/cancel", "POST", JSONObject()).mapCatching { JSONObject(it.ifBlank { "{}" }).optInt("refund") }

    /** 호스트 수락 — 예약 확정. */
    suspend fun accept(resId: Int): Result<Unit> = call("/spaces/reservations/$resId/accept", "POST", JSONObject()).map { }

    /** 호스트 거절 — 예약자에게 전액 환불. */
    suspend fun decline(resId: Int, reason: String?): Result<Unit> =
        call("/spaces/reservations/$resId/decline", "POST", JSONObject().apply { reason?.trim()?.takeIf { it.isNotEmpty() }?.let { put("reason", it) } }).map { }

    suspend fun hostCancel(resId: Int): Result<Int> =
        call("/spaces/reservations/$resId/host-cancel", "POST", JSONObject()).mapCatching { JSONObject(it.ifBlank { "{}" }).optInt("refund") }

    // ── 호스트 ──
    suspend fun mySpaces(): Result<List<SpaceListItem>> =
        call("/spaces/mine").mapCatching { body ->
            val arr = JSONArray(body.ifBlank { "[]" })
            (0 until arr.length()).mapNotNull { i -> arr.optJSONObject(i)?.let(SpaceListItem::from) }
        }

    /** 등록(POST, X-Active-Type — NativeHttp 가 싣는다) / 수정(PATCH) → id */
    suspend fun save(id: Int?, body: JSONObject): Result<Int> =
        call(if (id != null) "/spaces/$id" else "/spaces", if (id != null) "PATCH" else "POST", body)
            .mapCatching { JSONObject(it.ifBlank { "{}" }).optInt("id", id ?: 0) }

    suspend fun remove(id: Int): Result<Unit> = call("/spaces/$id", "DELETE").map { }

    /** 등록 화면 환불 단계(유연·보통·엄격 + 표시 줄). */
    suspend fun refundPresets(): Result<SpaceRefundPresets> =
        call("/spaces/refund-presets").mapCatching { SpaceRefundPresets.from(JSONObject(it)) }

    /** 호스트 이용 동의 여부 + 약관 목록. */
    suspend fun hostConsent(): Result<SpaceHostConsent> =
        call("/spaces/host-consent").mapCatching { SpaceHostConsent.from(JSONObject(it)) }

    /** 호스트 이용 동의 저장(본문 없음) → agreed=true. */
    suspend fun agreeHostConsent(): Result<SpaceHostConsent> =
        call("/spaces/host-consent", "POST").mapCatching { SpaceHostConsent.from(JSONObject(it.ifBlank { "{}" })) }

    suspend fun uploadImage(bytes: ByteArray): Result<String> = http.uploadImage(bytes)

    /** 공간 소유자와 1:1 방 생성/조회 — POST /chat/rooms/direct */
    suspend fun createDirectRoom(ownerId: Int, spaceId: Int): Result<Int> =
        call(
            "/chat/rooms/direct", "POST",
            JSONObject().put("targetUserId", ownerId).put("spaceId", spaceId),
        ).map { JSONObject(it.ifBlank { "{}" }).optInt("roomId") }
}

/** 기본 환불 규칙 = 서버 기본 단계 '보통'의 숫자(3일 전 100% · 1일 전 50% · 당일 0%). 서버 값이 없을 때만 쓴다. */
internal val DEFAULT_REFUND_RULES = listOf(SpaceRefund(3, 100), SpaceRefund(1, 50), SpaceRefund(0, 0))

/**
 * 예약 동의 대체 — 배포 전 서버(bookingConsents 없음)일 때만. 예전 앱의 두 줄 문구를 그대로 쓴다.
 *  배포된 서버는 관리자 '약관·정책' 제목을 내려주므로 이 값은 쓰이지 않는다.
 */
internal val FALLBACK_BOOKING_CONSENTS = listOf(
    SpaceConsentDoc("spaceRefundPolicy", "유의사항과 환불 규정을 확인했고 동의해요.", null, SPACE_REFUND_POLICY),
    SpaceConsentDoc("spaceBookingThirdParty", "예약 정보(이름·일시·인원·요청사항)를 호스트에게 제공하는 데 동의해요", null, SPACE_BOOKING_THIRD_PARTY_POLICY),
)

/** 2.5 → "2시간 30분". */
internal fun hourLabel(value: Double): String {
    val whole = value.toInt()
    return if (value % 1.0 != 0.0) (if (whole > 0) "${whole}시간 30분" else "30분") else "${whole}시간"
}

/** 서버 시각(ISO, UTC) → 한국 시간 "2026.11.20 14:03". 못 읽으면 원문. */
internal fun kstDateTime(iso: String?): String? {
    if (iso.isNullOrBlank()) return null
    return runCatching {
        val inFmt = java.text.SimpleDateFormat(if (iso.contains('.')) "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'" else "yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US)
            .apply { timeZone = java.util.TimeZone.getTimeZone("UTC") }
        val d = inFmt.parse(iso) ?: return@runCatching iso
        java.text.SimpleDateFormat("yyyy.MM.dd HH:mm", java.util.Locale.KOREA).apply { timeZone = java.util.TimeZone.getTimeZone("Asia/Seoul") }.format(d)
    }.getOrDefault(iso)
}

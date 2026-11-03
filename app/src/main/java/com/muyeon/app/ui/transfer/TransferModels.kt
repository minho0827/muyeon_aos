package com.muyeon.app.ui.transfer

import android.content.Context
import com.muyeon.app.ui.common.MarketFormat
import com.muyeon.app.ui.common.NativeHttp
import com.muyeon.app.ui.common.jBool
import com.muyeon.app.ui.common.jDouble
import com.muyeon.app.ui.common.jInt
import com.muyeon.app.ui.common.jObjects
import com.muyeon.app.ui.common.jStr
import com.muyeon.app.ui.common.jStrings
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder

/**
 * 무용센터 양도양수 — 모델 + REST(백엔드 studio-transfers, 2026-11-03). iOS 와 같은 필드·라벨.
 *  금액(보증금·월세·관리비·권리금·월매출)은 **만원** 단위 정수, null = 협의. 시간당 대관만 원 단위.
 */
object TransferLabels {
    val facility = listOf("ACADEMY" to "학원", "STUDIO" to "연습실", "HALL" to "공연장", "ETC" to "기타")
    val operating = listOf("OPERATING" to "운영중", "VACANT" to "공실")
    val status = listOf("OPEN" to "판매중", "HOLD" to "예약중", "CLOSED" to "거래완료")
    val options = listOf(
        "AIRCON" to "에어컨", "HEATER" to "난방기", "DRESSING_ROOM" to "탈의실", "SHOWER" to "샤워실",
        "RESTROOM" to "내부 화장실", "MIRROR" to "전면거울", "BALLET_BAR" to "발레바", "SOUNDPROOF" to "방음",
        "SPRUNG_FLOOR" to "탄성마루", "AIR_PURIFIER" to "공기청정기", "WATER_PURIFIER" to "정수기", "WIFI" to "와이파이",
        "STORAGE" to "창고", "SOUND_SYSTEM" to "음향", "PIANO" to "피아노", "LOBBY" to "로비",
    )

    /**
     * 지역 칩 → 시도 코드(regionCode 앞 2자리). 앱 지역 코드표(/api/regions, 행정표준 구 코드)와 같은 체계 —
     *  강원 42 · 전북 45(특별자치도 신코드 51·52 가 아니라 코드표가 쓰는 값).
     */
    val regions: List<Pair<String, String?>> = listOf(
        "전체" to null, "서울" to "11", "경기" to "41", "인천" to "28", "부산" to "26", "대구" to "27", "대전" to "30",
        "광주" to "29", "울산" to "31", "세종" to "36", "강원" to "42", "충북" to "43", "충남" to "44", "전북" to "45",
        "전남" to "46", "경북" to "47", "경남" to "48", "제주" to "50",
    )

    fun facility(code: String?) = facility.firstOrNull { it.first == code }?.second
    fun operating(code: String?) = operating.firstOrNull { it.first == code }?.second
    fun status(code: String?) = status.firstOrNull { it.first == code }?.second ?: "판매중"
    fun option(code: String) = options.firstOrNull { it.first == code }?.second ?: code
}

data class TransferCard(
    val id: Int,
    val authorId: Int,
    val authorName: String,
    val title: String,
    val status: String,
    val facilityType: String?,
    val operatingStatus: String?,
    val region: String?,
    val regionCode: String?,
    val areaPyeong: Double?,
    val floor: String?,
    val deposit: Int?,
    val monthlyRent: Int?,
    val maintenanceFee: Int?,
    val premium: Int?,
    val premiumNegotiable: Boolean,
    val hourlyPrice: Int?,
    val options: List<String>,
    val image: String?,
    val viewCount: Int,
    val createdAt: String?,
    val scrapped: Boolean,
    val isExposed: Boolean,
) {
    /** "서울 강서구 · 45평 · 2층" */
    val metaLine: String
        get() = listOfNotNull(
            region?.takeIf { it.isNotBlank() },
            areaPyeong?.let { "${pyeongText(it)}평" },
            floor?.takeIf { it.isNotBlank() }?.let { if (it.endsWith("층")) it else "${it}층" },
        ).joinToString(" · ")

    /** "보증금 1,800 / 월세 140 · 권리금 1,000(만원)" — null 은 협의. */
    val priceLine: String
        get() = "보증금 ${MarketFormat.manwon(deposit)} / 월세 ${MarketFormat.manwon(monthlyRent)} · 권리금 ${MarketFormat.manwon(premium)}(만원)"

    companion object {
        fun from(o: JSONObject) = TransferCard(
            id = o.optInt("id"), authorId = o.optInt("authorId"), authorName = o.jStr("authorName") ?: "회원",
            title = o.optString("title"), status = o.jStr("status") ?: "OPEN",
            facilityType = o.jStr("facilityType"), operatingStatus = o.jStr("operatingStatus"),
            region = o.jStr("region"), regionCode = o.jStr("regionCode"), areaPyeong = o.jDouble("areaPyeong"),
            floor = o.jStr("floor"), deposit = o.jInt("deposit"), monthlyRent = o.jInt("monthlyRent"),
            maintenanceFee = o.jInt("maintenanceFee"), premium = o.jInt("premium"),
            premiumNegotiable = o.jBool("premiumNegotiable"), hourlyPrice = o.jInt("hourlyPrice"),
            options = o.jStrings("options"), image = o.jStr("image"), viewCount = o.optInt("viewCount"),
            createdAt = o.jStr("createdAt"), scrapped = o.jBool("scrapped"), isExposed = o.jBool("isExposed", true),
        )
    }
}

fun pyeongText(v: Double): String = if (v % 1.0 == 0.0) v.toInt().toString() else String.format(java.util.Locale.KOREA, "%.1f", v)

data class TransferDetail(
    val card: TransferCard,
    val roadAddress: String?,
    val addressDetail: String?,
    val lat: Double?,
    val lng: Double?,
    val locationApprox: Boolean,
    val exactLocationVisible: Boolean,
    val hasElevator: Boolean?,
    val vatSeparate: Boolean,
    val parkingCount: Int?,
    val images: List<String>,
    val description: String?,
    val availableFrom: String?,
    val studentCount: Int?,
    val monthlyRevenue: Int?,
    val revenueVisible: Boolean,
    val contactName: String?,
    val contactPhone: String?,
    val phoneVisible: Boolean,
    val isAuthor: Boolean,
) {
    companion object {
        fun from(o: JSONObject) = TransferDetail(
            card = TransferCard.from(o), roadAddress = o.jStr("roadAddress"), addressDetail = o.jStr("addressDetail"),
            lat = o.jDouble("lat"), lng = o.jDouble("lng"), locationApprox = o.jBool("locationApprox", true),
            exactLocationVisible = o.jBool("exactLocationVisible"),
            hasElevator = if (o.has("hasElevator") && !o.isNull("hasElevator")) o.optBoolean("hasElevator") else null,
            vatSeparate = o.jBool("vatSeparate"), parkingCount = o.jInt("parkingCount"), images = o.jStrings("images"),
            description = o.jStr("description"), availableFrom = o.jStr("availableFrom")?.take(10),
            studentCount = o.jInt("studentCount"), monthlyRevenue = o.jInt("monthlyRevenue"),
            revenueVisible = o.jBool("revenueVisible"), contactName = o.jStr("contactName"),
            contactPhone = o.jStr("contactPhone"), phoneVisible = o.jBool("phoneVisible"), isAuthor = o.jBool("isAuthor"),
        )
    }
}

/** 목록 필터(시트) — 금액 상한은 만원. */
data class TransferFilter(
    val facilityType: String? = null,
    val minArea: String = "",
    val maxArea: String = "",
    val maxDeposit: String = "",
    val maxRent: String = "",
    val maxPremium: String = "",
    val options: Set<String> = emptySet(),
) {
    val isActive: Boolean
        get() = facilityType != null || minArea.isNotBlank() || maxArea.isNotBlank() || maxDeposit.isNotBlank() ||
            maxRent.isNotBlank() || maxPremium.isNotBlank() || options.isNotEmpty()

    fun query(): String {
        val q = mutableListOf<String>()
        facilityType?.let { q += "facilityType=$it" }
        fun n(key: String, v: String) { v.trim().toIntOrNull()?.let { q += "$key=$it" } }
        n("minArea", minArea); n("maxArea", maxArea); n("maxDeposit", maxDeposit); n("maxRent", maxRent); n("maxPremium", maxPremium)
        if (options.isNotEmpty()) q += "options=" + URLEncoder.encode(options.joinToString(","), "UTF-8")
        return q.joinToString("&")
    }
}

/** 등록·수정 폼 — 숫자 칸은 입력 그대로(문자열) 들고 있다가 저장 때 바꾼다. */
data class TransferForm(
    val images: List<String> = emptyList(),
    val roadAddress: String = "",
    val placeName: String = "",
    val addressDetail: String = "",
    val lat: Double? = null,
    val lng: Double? = null,
    val region: String = "",
    val regionCode: String? = null,
    val exactLocationVisible: Boolean = false,
    val title: String = "",
    val facilityType: String? = null,
    val operatingStatus: String? = null,
    val areaPyeong: String = "",
    val floor: String = "",
    val hasElevator: Boolean = false,
    val parkingCount: String = "",
    val deposit: String = "",
    val monthlyRent: String = "",
    val maintenanceFee: String = "",
    val premium: String = "",
    val premiumNegotiable: Boolean = false,
    val vatSeparate: Boolean = false,
    val hourlyPrice: String = "",
    val options: Set<String> = emptySet(),
    val description: String = "",
    val availableFrom: String? = null,
    val studentCount: String = "",
    val monthlyRevenue: String = "",
    val revenueVisible: Boolean = false,
    val contactName: String = "",
    val contactPhone: String = "",
    val phoneVisible: Boolean = false,
) {
    fun toJson(): JSONObject {
        val o = JSONObject()
        fun s(k: String, v: String) { o.put(k, v.trim().ifEmpty { JSONObject.NULL }) }
        fun i(k: String, v: String) { o.put(k, v.filter { it.isDigit() }.toIntOrNull() ?: JSONObject.NULL) }
        o.put("title", title.trim())
        o.put("facilityType", facilityType ?: JSONObject.NULL)
        o.put("operatingStatus", operatingStatus ?: JSONObject.NULL)
        s("region", region)
        o.put("regionCode", regionCode ?: JSONObject.NULL)
        s("roadAddress", roadAddress); s("addressDetail", addressDetail)
        o.put("lat", lat ?: JSONObject.NULL); o.put("lng", lng ?: JSONObject.NULL)
        o.put("exactLocationVisible", exactLocationVisible)
        o.put("areaPyeong", areaPyeong.trim().toDoubleOrNull() ?: JSONObject.NULL)
        s("floor", floor)
        o.put("hasElevator", hasElevator)
        i("deposit", deposit); i("monthlyRent", monthlyRent); i("maintenanceFee", maintenanceFee); i("premium", premium)
        o.put("premiumNegotiable", premiumNegotiable); o.put("vatSeparate", vatSeparate)
        i("hourlyPrice", hourlyPrice); i("parkingCount", parkingCount)
        o.put("options", JSONArray(options.toList()))
        o.put("images", JSONArray(images))
        s("description", description)
        o.put("availableFrom", availableFrom ?: JSONObject.NULL)
        i("studentCount", studentCount); i("monthlyRevenue", monthlyRevenue)
        o.put("revenueVisible", revenueVisible)
        s("contactName", contactName); s("contactPhone", contactPhone)
        o.put("phoneVisible", phoneVisible)
        return o
    }

    companion object {
        /** 수정 — 글쓴이 본인 상세(정확 주소·연락처 포함)에서 채운다. */
        fun from(d: TransferDetail): TransferForm {
            val c = d.card
            fun n(v: Int?) = v?.toString() ?: ""
            return TransferForm(
                images = d.images, roadAddress = d.roadAddress.orEmpty(), placeName = d.roadAddress.orEmpty(),
                addressDetail = d.addressDetail.orEmpty(), lat = d.lat, lng = d.lng, region = c.region.orEmpty(),
                regionCode = c.regionCode, exactLocationVisible = d.exactLocationVisible, title = c.title,
                facilityType = c.facilityType, operatingStatus = c.operatingStatus,
                areaPyeong = c.areaPyeong?.let { pyeongText(it) } ?: "", floor = c.floor.orEmpty(),
                hasElevator = d.hasElevator == true, parkingCount = n(d.parkingCount), deposit = n(c.deposit),
                monthlyRent = n(c.monthlyRent), maintenanceFee = n(c.maintenanceFee), premium = n(c.premium),
                premiumNegotiable = c.premiumNegotiable, vatSeparate = d.vatSeparate, hourlyPrice = n(c.hourlyPrice),
                options = c.options.toSet(), description = d.description.orEmpty(), availableFrom = d.availableFrom,
                studentCount = n(d.studentCount), monthlyRevenue = n(d.monthlyRevenue), revenueVisible = d.revenueVisible,
                contactName = d.contactName.orEmpty(), contactPhone = d.contactPhone.orEmpty(), phoneVisible = d.phoneVisible,
            )
        }
    }
}

class TransferApi(context: Context) {
    val http = NativeHttp(context)
    val isLoggedIn: Boolean get() = http.isLoggedIn

    data class Page(val items: List<TransferCard>, val hasMore: Boolean, val nextCursor: String?)

    /** 목록(커서) — regionCode 는 시도 코드(앞자리 일치). */
    suspend fun page(regionCode: String?, filter: TransferFilter, cursor: String?): Result<Page> {
        val q = mutableListOf("limit=20")
        regionCode?.let { q += "regionCode=$it" }
        filter.query().takeIf { it.isNotEmpty() }?.let { q += it }
        cursor?.let { q += "cursor=" + URLEncoder.encode(it, "UTF-8") }
        return http.get("/studio-transfers/page?" + q.joinToString("&")).mapCatching {
            val o = JSONObject(it)
            Page(
                (o.optJSONArray("items") ?: JSONArray()).jObjects().map(TransferCard::from),
                o.optBoolean("hasMore"), o.jStr("nextCursor"),
            )
        }
    }

    suspend fun detail(id: Int): Result<TransferDetail> =
        http.get("/studio-transfers/$id").mapCatching { TransferDetail.from(JSONObject(it)) }

    suspend fun mine(): Result<List<TransferCard>> =
        http.get("/studio-transfers/mine").mapCatching { JSONArray(it.ifBlank { "[]" }).jObjects().map(TransferCard::from) }

    /** 등록(POST, X-Active-Type 필수 — NativeHttp 가 싣는다) / 수정(PATCH). → id */
    suspend fun save(id: Int?, form: TransferForm): Result<Int> =
        http.call(if (id != null) "/studio-transfers/$id" else "/studio-transfers", if (id != null) "PATCH" else "POST", form.toJson())
            .mapCatching { JSONObject(it.ifBlank { "{}" }).optInt("id", id ?: 0) }

    suspend fun setStatus(id: Int, status: String): Result<Unit> =
        http.call("/studio-transfers/$id/status", "PATCH", JSONObject().put("status", status)).map { }

    suspend fun remove(id: Int): Result<Unit> = http.call("/studio-transfers/$id", "DELETE").map { }

    suspend fun setScrap(id: Int, on: Boolean): Result<Unit> =
        http.call("/studio-transfers/$id/scrap", if (on) "POST" else "DELETE").map { }

    /** 허위 매물 신고 — 기존 신고 API(targetType STUDIO_TRANSFER). */
    suspend fun report(id: Int): Result<Unit> =
        http.call("/reports", "POST", JSONObject().put("targetType", "STUDIO_TRANSFER").put("targetId", id).put("reason", "FAKE")).map { }

    /** 글쓴이와 1:1 채팅방 — 글 id 를 남긴다(targetType TRANSFER). → roomId */
    suspend fun directRoom(authorId: Int, transferId: Int): Result<Int> =
        http.call(
            "/chat/rooms/direct", "POST",
            JSONObject().put("targetUserId", authorId).put("transferId", transferId).put("targetType", "TRANSFER"),
        ).mapCatching { JSONObject(it.ifBlank { "{}" }).optInt("roomId").also { r -> if (r <= 0) error("채팅방을 열지 못했어요.") } }

    suspend fun uploadImage(bytes: ByteArray): Result<String> = http.uploadImage(bytes)
}

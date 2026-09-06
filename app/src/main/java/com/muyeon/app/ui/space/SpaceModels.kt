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
import com.muyeon.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
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

/** 환불규정 1행 — { label: "이용 8일 전", value: "총 금액의 100% 환불" } */
data class SpaceRefundRule(val label: String?, val value: String?) {
    companion object {
        fun from(o: JSONObject) =
            SpaceRefundRule(o.optString("label").ifEmpty { null }, o.optString("value").ifEmpty { null })
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
    val refundPolicy: List<SpaceRefundRule>?,
    val hourlyEnabled: Boolean?,
    val hourlyMinHours: Int?,
    val packageEnabled: Boolean?,
    val phone: String?,
    val priceOptions: List<SpacePriceOption>?,
) {
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
        get() = addressDetail?.takeIf { it.isNotEmpty() } ?: region?.takeIf { it.isNotEmpty() }

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
                refundPolicy = o.optJSONArray("refundPolicy")?.let { arr ->
                    (0 until arr.length()).mapNotNull { i -> arr.optJSONObject(i)?.let(SpaceRefundRule::from) }
                },
                hourlyEnabled = boolOrNull("hourlyEnabled"),
                hourlyMinHours = intOrNull("hourlyMinHours"),
                packageEnabled = boolOrNull("packageEnabled"),
                phone = o.optString("phone").ifEmpty { null },
                priceOptions = o.optJSONArray("priceOptions")?.let { arr ->
                    (0 until arr.length()).mapNotNull { i -> arr.optJSONObject(i)?.let(SpacePriceOption::from) }
                },
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

/** 공간 예약 요청 본문 — POST /spaces/:id/reservation */
data class SpaceReservationRequest(
    val bookingType: String,
    val optionId: Int?,
    val date: String,
    val startTime: String?,
    val hours: Double?,
) {
    fun toJson(): JSONObject {
        val o = JSONObject().put("bookingType", bookingType).put("date", date)
        optionId?.let { o.put("optionId", it) }
        if (bookingType != "HOURLY") return o
        startTime?.takeIf { it.isNotEmpty() }?.let { o.put("startTime", it) }
        hours?.let { o.put("hours", it) }
        return o
    }
}

class SpaceApi(private val token: String?) {

    private val client = OkHttpClient()
    private val apiBase = BuildConfig.API_BASE_URL + "/api"
    private val json = "application/json; charset=utf-8".toMediaType()

    val isLoggedIn: Boolean get() = !token.isNullOrEmpty()

    /** 공간 상세(가격옵션 포함) — GET /spaces/:id */
    suspend fun detail(id: Int): Result<SpaceDetail> =
        call("/spaces/$id").map { SpaceDetail.from(JSONObject(it.ifBlank { "{}" })) }

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

    /** '바로 예약하기' — POST /spaces/:id/reservation */
    suspend fun reserve(id: Int, request: SpaceReservationRequest): Result<Unit> =
        call("/spaces/$id/reservation", "POST", request.toJson()).map { }

    /** 공간 소유자와 1:1 방 생성/조회 — POST /chat/rooms/direct */
    suspend fun createDirectRoom(ownerId: Int, spaceId: Int): Result<Int> =
        call(
            "/chat/rooms/direct", "POST",
            JSONObject().put("targetUserId", ownerId).put("spaceId", spaceId),
        ).map { JSONObject(it.ifBlank { "{}" }).optInt("roomId") }

    private suspend fun call(path: String, method: String = "GET", body: JSONObject? = null): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                val payload: RequestBody? = when {
                    body != null -> body.toString().toRequestBody(json)
                    method != "GET" && method != "DELETE" -> "".toRequestBody(json)
                    else -> null
                }
                val req = Request.Builder().url(apiBase + path).method(method, payload)
                    .apply { if (!token.isNullOrEmpty()) addHeader("Authorization", "Bearer $token") }
                    .build()
                client.newCall(req).execute().use { res ->
                    val text = res.body?.string().orEmpty()
                    // 본문 응답을 안 쓰는 요청도 비2xx 는 실패로 올린다(빈 성공으로 삼키지 않는다).
                    if (!res.isSuccessful) error("요청에 실패했어요.")
                    text
                }
            }
        }
}

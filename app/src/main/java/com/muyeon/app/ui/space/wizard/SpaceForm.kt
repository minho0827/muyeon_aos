package com.muyeon.app.ui.space.wizard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muyeon.app.theme.customFontFamily
import com.muyeon.app.ui.common.*
import com.muyeon.app.ui.common.market.*
import com.muyeon.app.ui.space.*
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

/** 공간 등록 폼 모델 + 단계·선택지 표. */
val SPACE_STEPS = listOf("사진", "위치", "기본", "가격", "운영", "시설·규정", "소개")
const val SPACE_MAX_IMAGES = 10
internal val DAYS = listOf("0" to "일", "1" to "월", "2" to "화", "3" to "수", "4" to "목", "5" to "금", "6" to "토")
internal val LEADS = listOf("0" to "바로", "30" to "30분 전", "60" to "1시간 전", "120" to "2시간 전", "180" to "3시간 전", "1440" to "하루 전")
internal val MIN_HOURS = listOf("1" to "1시간", "2" to "2시간", "3" to "3시간", "4" to "4시간")

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

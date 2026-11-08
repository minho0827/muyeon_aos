package com.muyeon.network.dto.write

import com.muyeon.domain.model.write.CommunityDraft
import com.muyeon.domain.model.write.SubEditForm
import com.muyeon.domain.model.write.SubUpdate
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody

/** ★ 2026-11-08 묶음 3 — GET /subs/:id 중 수정 화면이 읽는 칸(웹 SubCreate 수정 모드). */
@JsonClass(generateAdapter = true)
data class SubEditDto(
    val title: String? = null,
    val academy: String? = null,
    val region: String? = null,
    val regionCode: String? = null,
    val genre: String? = null,
    val target: String? = null,
    val fields: List<String>? = null,
    val scheduleType: String? = null,
    val classDate: String? = null,
    val classTime: String? = null,
    val classCount: Int? = null,
    val salary: String? = null,
    val career: String? = null,
    val description: String? = null,
    val imageUrl: String? = null,
    val image: String? = null,
    val dispatchEnabled: Boolean? = null,
    val confirmedApplicationId: Long? = null,
) {
    fun toDomain(): SubEditForm {
        fun String?.v() = this?.takeIf { it.isNotBlank() }
        return SubEditForm(
            title = title.orEmpty(), academy = academy.v(), region = region.v(), regionCode = regionCode.v(),
            genre = genre.v(), target = target.v(), fields = fields.orEmpty().filter { it.isNotBlank() },
            scheduleType = scheduleType.v(), classDate = classDate.v(), classTime = classTime.v(),
            classCount = classCount?.takeIf { it > 0 }, salary = salary.v(), career = career.v(), description = description.v(),
            image = (imageUrl ?: image).v(), dispatchEnabled = dispatchEnabled == true, payLocked = confirmedApplicationId != null,
        )
    }
}

/** 커뮤니티 글 본문 — images 가 null 이면 보내지 않는다(새 글에 사진이 없을 때 · 웹과 같다). */
@JsonClass(generateAdapter = true)
data class CommunityPostBody(val category: String, val title: String, val body: String, val images: List<String>? = null) {
    companion object {
        /** 새 글은 사진이 없으면 images 를 빼고, 수정은 빈 배열도 보낸다("첨부 전부 삭제" — 웹 CommunityWrite). */
        fun of(d: CommunityDraft, edit: Boolean) = CommunityPostBody(d.category, d.title, d.body, if (edit || d.images.isNotEmpty()) d.images else null)
    }
}

/**
 * 대타공고 수정 본문(PATCH) — null 칸은 빼고, 급여만 지울 때 null 을 그대로 보낸다(Moshi 기본은 null 을 빼므로 따로 만든다).
 */
object SubUpdateBody {
    fun of(moshi: Moshi, u: SubUpdate): RequestBody {
        val m = linkedMapOf<String, Any?>()
        fun put(k: String, v: Any?) { if (v != null) m[k] = v }
        put("title", u.title); put("academy", u.academy); put("region", u.region); put("regionCode", u.regionCode)
        put("image", u.image); put("genre", u.genre); put("target", u.target); put("fields", u.fields.takeIf { it.isNotEmpty() })
        put("classDate", u.classDate); put("classTime", u.classTime); put("scheduleType", u.scheduleType); put("classCount", u.classCount)
        if (u.sendSalary) m["salary"] = u.salary
        put("career", u.career); put("description", u.description)
        val type = Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java)
        val json = moshi.adapter<Map<String, Any?>>(type).serializeNulls().toJson(m)
        return json.toRequestBody("application/json; charset=utf-8".toMediaType())
    }
}

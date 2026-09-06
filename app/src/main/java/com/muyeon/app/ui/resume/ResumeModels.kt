package com.muyeon.app.ui.resume

import com.muyeon.app.ui.quote.boolOrNull
import com.muyeon.app.ui.quote.doubleOrNull
import com.muyeon.app.ui.quote.intOrNull
import com.muyeon.app.ui.quote.map
import com.muyeon.app.ui.quote.stringList
import com.muyeon.app.ui.quote.stringOrNull
import org.json.JSONArray
import org.json.JSONObject

/**
 * 이력서 모델 — iOS `ResumeModels.swift` 1:1.
 *  스키마 정본: muyeon-backend/src/modules/resumes/profile-mirror.ts
 *
 * ⚠️ data JSON 은 **레거시 키 + 신규 키가 공존**한다(availableDays/availableTimeSlots 등).
 *   서버는 저장 시 spread 병합이라 클라가 모르는 키를 빼고 보내면 그 값이 날아간다.
 *   그래서 원본 JSONObject(raw)를 들고 있다가 저장 시 아는 키만 덮어쓴다(미지 키 보존).
 */

data class EduItem(var school: String = "", var major: String = "", var period: String = "") {
    companion object {
        fun from(o: JSONObject) = EduItem(o.optString("school"), o.optString("major"), o.optString("period"))
    }
    fun toJson(): JSONObject = JSONObject().put("school", school).put("major", major).put("period", period)
}

data class CareerItem(
    var academy: String = "",   // 기관(웹 폼과 동일 키)
    var position: String = "",
    var classes: String = "",
    var period: String = "",
) {
    companion object {
        fun from(o: JSONObject) =
            CareerItem(o.optString("academy"), o.optString("position"), o.optString("classes"), o.optString("period"))
    }
    fun toJson(): JSONObject = JSONObject()
        .put("academy", academy).put("position", position).put("classes", classes).put("period", period)
}

data class PerfItem(var year: String = "", var title: String = "", var role: String = "") {
    companion object {
        fun from(o: JSONObject) = PerfItem(o.optString("year"), o.optString("title"), o.optString("role"))
    }
    fun toJson(): JSONObject = JSONObject().put("year", year).put("title", title).put("role", role)
}

data class ResumeBasic(
    var photo: String? = null,
    var name: String? = null,
    var birth: String? = null,   // YYYY.MM.DD
    var phone: String? = null,
    var email: String? = null,
) {
    companion object {
        fun from(o: JSONObject?) = o?.let {
            ResumeBasic(
                it.stringOrNull("photo"), it.stringOrNull("name"), it.stringOrNull("birth"),
                it.stringOrNull("phone"), it.stringOrNull("email"),
            )
        }
    }
    fun toJson(): JSONObject = JSONObject()
        .put("photo", photo).put("name", name).put("birth", birth).put("phone", phone).put("email", email)
}

data class DesiredCond(var job: String? = null, var region: String? = null, var salary: String? = null) {
    companion object {
        fun from(o: JSONObject?) = o?.let {
            DesiredCond(it.stringOrNull("job"), it.stringOrNull("region"), it.stringOrNull("salary"))
        }
    }
    fun toJson(): JSONObject = JSONObject().put("job", job).put("region", region).put("salary", salary)
}

/** 이력서 data — 레거시+신규 키 공존. raw 를 보존해 미지 키가 날아가지 않게 한다. */
data class ResumeData(
    var raw: JSONObject = JSONObject(),
    var basic: ResumeBasic? = null,
    var oneLiner: String? = null,
    var intro: String? = null,
    var image: String? = null,
    var images: List<String>? = null,
    var genres: List<String>? = null,
    var fields: List<String>? = null,
    var activeRegion: String? = null,
    var activeRegions: List<String>? = null,
    var activeRegionCode: String? = null,
    var desiredRegion: String? = null,
    var desiredRegionCode: String? = null,
    var availableDays: List<String>? = null,
    var availableTimeSlots: List<String>? = null,
    var educations: List<EduItem>? = null,
    var careers: List<CareerItem>? = null,
    var career: String? = null,          // 경력 버킷 NEW/Y1_3/…
    var certificates: String? = null,    // 여러 줄(줄당 1건) — 레거시 유지
    var performances: List<PerfItem>? = null,
    var awards: String? = null,
    var sns: List<String>? = null,
    var desired: DesiredCond? = null,
    // 레거시 학력(단일 문자열) — 신규 educations 비어있을 때 시드용
    var artMiddle: String? = null,
    var artHigh: String? = null,
    var university: String? = null,
    var gradSchool: String? = null,
    // 무용수(DANCER) 전용
    var gender: String? = null,
    var height: String? = null,
    var weight: String? = null,
    var companyCareer: String? = null,
    var videoUrl: String? = null,
    // 이력서 역할(TEACHER|DANCER) — 목록 필터/저장 태깅. 서버 정본 resumes.service.normalizeRole
    var roleIntent: String? = null,
) {
    /** 학력: 신규 배열 없으면 레거시 4필드에서 생성(iOS seededEducations). */
    fun seededEducations(): List<EduItem> {
        educations?.takeIf { it.isNotEmpty() }?.let { return it }
        return listOf(
            (artMiddle ?: "") to "예술중", (artHigh ?: "") to "예술고",
            (university ?: "") to "대학교", (gradSchool ?: "") to "대학원",
        ).filter { it.first.isNotEmpty() }.map { EduItem(school = it.first, major = it.second) }
    }

    /** 저장 payload — raw 위에 아는 키만 덮어쓴다(서버 spread 병합에서 미지 키 보존). */
    fun toJson(): JSONObject {
        val o = JSONObject(raw.toString())   // deep copy
        basic?.let { o.put("basic", it.toJson()) }
        o.putOpt("oneLiner", oneLiner); o.putOpt("intro", intro); o.putOpt("image", image)
        images?.let { o.put("images", JSONArray(it)) }
        genres?.let { o.put("genres", JSONArray(it)) }
        fields?.let { o.put("fields", JSONArray(it)) }
        o.putOpt("activeRegion", activeRegion)
        activeRegions?.let { o.put("activeRegions", JSONArray(it)) }
        o.putOpt("activeRegionCode", activeRegionCode)
        o.putOpt("desiredRegion", desiredRegion); o.putOpt("desiredRegionCode", desiredRegionCode)
        availableDays?.let { o.put("availableDays", JSONArray(it)) }
        availableTimeSlots?.let { o.put("availableTimeSlots", JSONArray(it)) }
        educations?.let { list -> o.put("educations", JSONArray().apply { list.forEach { put(it.toJson()) } }) }
        careers?.let { list -> o.put("careers", JSONArray().apply { list.forEach { put(it.toJson()) } }) }
        o.putOpt("career", career); o.putOpt("certificates", certificates)
        performances?.let { list -> o.put("performances", JSONArray().apply { list.forEach { put(it.toJson()) } }) }
        o.putOpt("awards", awards)
        sns?.let { o.put("sns", JSONArray(it)) }
        desired?.let { o.put("desired", it.toJson()) }
        o.putOpt("gender", gender); o.putOpt("height", height); o.putOpt("weight", weight)
        o.putOpt("companyCareer", companyCareer); o.putOpt("videoUrl", videoUrl)
        o.putOpt("roleIntent", roleIntent)
        return o
    }

    companion object {
        fun from(o: JSONObject?): ResumeData {
            if (o == null) return ResumeData()
            return ResumeData(
                raw = o,
                basic = ResumeBasic.from(o.optJSONObject("basic")),
                oneLiner = o.stringOrNull("oneLiner"),
                intro = o.stringOrNull("intro"),
                image = o.stringOrNull("image"),
                images = o.stringList("images"),
                genres = o.stringList("genres"),
                fields = o.stringList("fields"),
                activeRegion = o.stringOrNull("activeRegion"),
                activeRegions = o.stringList("activeRegions"),
                activeRegionCode = o.stringOrNull("activeRegionCode"),
                desiredRegion = o.stringOrNull("desiredRegion"),
                desiredRegionCode = o.stringOrNull("desiredRegionCode"),
                availableDays = o.stringList("availableDays"),
                availableTimeSlots = o.stringList("availableTimeSlots"),
                educations = o.optJSONArray("educations")?.map { EduItem.from(it) },
                careers = o.optJSONArray("careers")?.map { CareerItem.from(it) },
                career = o.stringOrNull("career"),
                certificates = o.stringOrNull("certificates"),
                performances = o.optJSONArray("performances")?.map { PerfItem.from(it) },
                awards = o.stringOrNull("awards"),
                sns = o.stringList("sns"),
                desired = DesiredCond.from(o.optJSONObject("desired")),
                artMiddle = o.stringOrNull("artMiddle"),
                artHigh = o.stringOrNull("artHigh"),
                university = o.stringOrNull("university"),
                gradSchool = o.stringOrNull("gradSchool"),
                gender = o.stringOrNull("gender"),
                height = o.stringOrNull("height"),
                weight = o.stringOrNull("weight"),
                companyCareer = o.stringOrNull("companyCareer"),
                videoUrl = o.stringOrNull("videoUrl"),
                roleIntent = o.stringOrNull("roleIntent"),
            )
        }
    }
}

data class ResumeListItem(val id: Int, val title: String, val isDefault: Boolean, val needsPeriodFix: Boolean?) {
    companion object {
        fun from(o: JSONObject) = ResumeListItem(
            o.optInt("id"), o.optString("title"),
            o.optBoolean("isDefault", false), o.boolOrNull("needsPeriodFix"),
        )
    }
}

data class ResumeDetail(val id: Int, var title: String, var data: ResumeData) {
    companion object {
        fun from(o: JSONObject) = ResumeDetail(
            o.optInt("id"), o.optString("title"), ResumeData.from(o.optJSONObject("data")),
        )
    }
}

/** 공개범위 플래그 — profileHidden(강사 목록 전체 숨김)은 별도. */
data class FieldVisibilityFlags(
    val flags: MutableMap<String, Boolean> = mutableMapOf(),
    var careerItems: List<Int>? = null,
    var performanceItems: List<Int>? = null,
) {
    /** iOS isOn — 값이 명시적으로 false 일 때만 꺼짐(미설정=켜짐). */
    fun isOn(key: String): Boolean = flags[key] != false
    fun set(key: String, on: Boolean) { flags[key] = on }

    fun toJson(): JSONObject = JSONObject().apply {
        flags.forEach { (k, v) -> put(k, v) }
        careerItems?.let { put("careerItems", JSONArray(it)) }
        performanceItems?.let { put("performanceItems", JSONArray(it)) }
    }

    companion object {
        /** 화면에 노출하는 토글 키 순서(iOS FieldVisibilityView 순서). */
        val KEYS = listOf(
            "photo" to "사진", "name" to "이름", "oneLiner" to "한 줄 소개", "fields" to "수업 분야",
            "region" to "활동 지역", "lessonTime" to "수업 가능 시간", "career" to "경력",
            "education" to "학력", "certificate" to "자격증", "performance" to "공연 경력",
            "award" to "수상", "sns" to "SNS", "contact" to "연락처", "body" to "신체 정보",
        )

        fun from(o: JSONObject?): FieldVisibilityFlags {
            val f = FieldVisibilityFlags()
            if (o == null) return f
            KEYS.forEach { (k, _) -> if (o.has(k) && !o.isNull(k)) f.flags[k] = o.optBoolean(k, true) }
            f.careerItems = o.optJSONArray("careerItems")?.let { arr -> (0 until arr.length()).map { arr.optInt(it) } }
            f.performanceItems = o.optJSONArray("performanceItems")?.let { arr -> (0 until arr.length()).map { arr.optInt(it) } }
            return f
        }
    }
}

/** 공개 프로필(/teachers/:id) — 미러된 profile 스프레드 응답. */
data class PublicProfile(
    val id: Int,
    val name: String?,
    val image: String?,
    val images: List<String>?,
    val oneLiner: String?,
    val intro: String?,
    val genres: List<String>?,
    val fields: List<String>?,
    val activeRegion: String?,
    val availableDays: List<String>?,
    val availableTimeSlots: List<String>?,
    val career: String?,
    val careers: List<CareerItem>?,
    val educations: List<EduItem>?,
    val certificates: String?,
    val performances: List<PerfItem>?,
    val awards: String?,
    val sns: List<String>?,
    val ratingAvg: Double?,
    val ratingCount: Int?,
    val contactVisible: Boolean?,
    val contactPhone: String?,  // 서버가 열람 권한을 판단해 내려준 번호(contactVisible 과 한 쌍)
    val scrapped: Boolean?,
    val chatRoomId: Int?,       // 기존 채팅방 — 있어야만 [문의하기] 노출
    val phone: String?,         // 원장+이용권(full) 열람 시에만 서버가 내려줌
    val email: String?,
    val full: Boolean?,         // 전체 이력서 열람됨(이용권/본인)
    val recruiterLocked: Boolean?,  // 채용 뷰어인데 이용권 없음 → 멤버십 유도
    val selfView: Boolean?,     // 본인 열람(서버 키는 "self")
    val myQuoteId: Int?,
    val myQuoteHasResponse: Boolean?,
) {
    companion object {
        /**
         * 지원자 응답 → 공개 프로필. iOS `PublicProfileDTO.init(applicant:)` 1:1.
         *  ★ 연락처/이메일은 항상 비운다 — 지원자 이력서에는 노출하지 않는 규약.
         */
        fun from(applicant: Applicant): PublicProfile {
            val d = applicant.mergedData
            return PublicProfile(
                id = applicant.applicantId,
                name = d.basic?.name ?: applicant.applicantName,
                image = d.image ?: d.basic?.photo,
                images = d.images,
                oneLiner = d.oneLiner, intro = d.intro,
                genres = d.genres, fields = d.fields,
                activeRegion = d.activeRegion,
                availableDays = d.availableDays, availableTimeSlots = d.availableTimeSlots,
                career = d.career, careers = d.careers,
                educations = d.seededEducations(),
                certificates = d.certificates, performances = d.performances,
                awards = d.awards, sns = d.sns,
                ratingAvg = null, ratingCount = null,
                contactVisible = false, contactPhone = null,
                scrapped = false, chatRoomId = null,
                phone = null, email = null,
                full = true, recruiterLocked = false, selfView = false,
                myQuoteId = null, myQuoteHasResponse = null,
            )
        }

        fun from(o: JSONObject) = PublicProfile(
            id = o.optInt("id"),
            name = o.stringOrNull("name"), image = o.stringOrNull("image"), images = o.stringList("images"),
            oneLiner = o.stringOrNull("oneLiner"), intro = o.stringOrNull("intro"),
            genres = o.stringList("genres"), fields = o.stringList("fields"),
            activeRegion = o.stringOrNull("activeRegion"),
            availableDays = o.stringList("availableDays"), availableTimeSlots = o.stringList("availableTimeSlots"),
            career = o.stringOrNull("career"),
            careers = o.optJSONArray("careers")?.map { CareerItem.from(it) },
            educations = o.optJSONArray("educations")?.map { EduItem.from(it) },
            certificates = o.stringOrNull("certificates"),
            performances = o.optJSONArray("performances")?.map { PerfItem.from(it) },
            awards = o.stringOrNull("awards"), sns = o.stringList("sns"),
            ratingAvg = o.doubleOrNull("ratingAvg"), ratingCount = o.intOrNull("ratingCount"),
            contactVisible = o.boolOrNull("contactVisible"),
            contactPhone = o.stringOrNull("contactPhone"),
            scrapped = o.boolOrNull("scrapped"),
            chatRoomId = o.intOrNull("chatRoomId"),
            phone = o.stringOrNull("phone"), email = o.stringOrNull("email"),
            full = o.boolOrNull("full"), recruiterLocked = o.boolOrNull("recruiterLocked"),
            selfView = o.boolOrNull("self"),
            myQuoteId = o.optJSONObject("myQuote")?.intOrNull("quoteId"),
            myQuoteHasResponse = o.optJSONObject("myQuote")?.boolOrNull("hasResponse"),
        )
    }
}

/** 원장이 여는 지원자 상세 — 지원 메타 + 이력서 원본. */
/** 지원 공고 종류 — iOS `ApplicantPostingKind` 1:1. */
enum class ApplicantPostingKind(val raw: String) {
    JOB("JOB"),   // 구인 공고
    SUB("SUB");   // 대타 공고

    val apiPath: String get() = if (this == SUB) "subs" else "jobs"
    val confirmTitle: String get() = if (this == SUB) "대타 확정" else "채용 확정"

    companion object {
        fun from(s: String?) = if (s == "SUB") SUB else JOB
    }
}

data class Applicant(
    val id: Int,
    val applicantId: Int,
    val applicantName: String?,
    val applicantType: String?,
    // 지원자가 프로필 노출 멤버십 보유 — true 면 공개 프로필 화면을 그대로 보여준다(iOS 와 동일)
    val profileMembershipActive: Boolean?,
    val postingHeadcount: Int?,
    val applicantPhone: String?,
    val phoneUnlocked: Boolean?,
    val status: String?,
    val appliedAt: String?,
    val applicantProfile: ResumeData?,
    val resumeTitle: String?,
    val resumeData: ResumeData?,
) {
    /**
     * 웹과 동일 규칙 — 공개 프로필 위에 첨부 이력서를 덮어 병합.
     *  첨부에 값이 있는 키만 덮는다(iOS mergedData take()).
     */
    val mergedData: ResumeData
        get() {
            val base = applicantProfile ?: ResumeData()
            val a = resumeData ?: return base
            return base.copy(
                basic = a.basic ?: base.basic,
                oneLiner = a.oneLiner ?: base.oneLiner,
                intro = a.intro ?: base.intro,
                image = a.image ?: base.image,
                images = a.images ?: base.images,
                genres = a.genres ?: base.genres,
                fields = a.fields ?: base.fields,
                activeRegion = a.activeRegion ?: base.activeRegion,
                activeRegions = a.activeRegions ?: base.activeRegions,
                activeRegionCode = a.activeRegionCode ?: base.activeRegionCode,
                desiredRegion = a.desiredRegion ?: base.desiredRegion,
                desiredRegionCode = a.desiredRegionCode ?: base.desiredRegionCode,
                availableDays = a.availableDays ?: base.availableDays,
                availableTimeSlots = a.availableTimeSlots ?: base.availableTimeSlots,
                educations = a.educations ?: base.educations,
                careers = a.careers ?: base.careers,
                career = a.career ?: base.career,
                certificates = a.certificates ?: base.certificates,
                performances = a.performances ?: base.performances,
                awards = a.awards ?: base.awards,
                sns = a.sns ?: base.sns,
                desired = a.desired ?: base.desired,
                artMiddle = a.artMiddle ?: base.artMiddle,
                artHigh = a.artHigh ?: base.artHigh,
                university = a.university ?: base.university,
                gradSchool = a.gradSchool ?: base.gradSchool,
                gender = a.gender ?: base.gender,
                height = a.height ?: base.height,
                weight = a.weight ?: base.weight,
                companyCareer = a.companyCareer ?: base.companyCareer,
                videoUrl = a.videoUrl ?: base.videoUrl,
            )
        }

    companion object {
        fun from(o: JSONObject): Applicant {
            val r = o.optJSONObject("resume")
            return Applicant(
                id = o.optInt("id"), applicantId = o.optInt("applicantId"),
                applicantName = o.stringOrNull("applicantName"),
                applicantType = o.stringOrNull("applicantType"),
                profileMembershipActive = o.boolOrNull("profileMembershipActive"),
                postingHeadcount = if (o.has("postingHeadcount")) o.optInt("postingHeadcount") else null,
                applicantPhone = o.stringOrNull("applicantPhone"),
                phoneUnlocked = o.boolOrNull("phoneUnlocked"),
                status = o.stringOrNull("status"), appliedAt = o.stringOrNull("appliedAt"),
                applicantProfile = o.optJSONObject("applicantProfile")?.let { ResumeData.from(it) },
                resumeTitle = r?.stringOrNull("title"),
                resumeData = r?.optJSONObject("data")?.let { ResumeData.from(it) },
            )
        }
    }
}

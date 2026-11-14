package com.muyeon.app.ui.my

import com.muyeon.app.R
import com.muyeon.domain.model.member.ActiveType
import com.muyeon.domain.model.menu.AppMenuItem
import com.muyeon.domain.model.my.MyPageInfo

/** MY 메뉴 한 칸. [icon] 은 웹 아이콘 이름(_icons.scss), [bridge] 가 있으면 앱 화면, 없으면 [path] 웹 창. */
data class MyMenuItem(
    val title: String,
    val icon: String,
    val path: String? = null,
    val bridge: String? = null,
    val desc: String? = null,
)

/**
 * 회원 MY 메뉴 묶음 만들기 — 웹 pages/muyeon/my/MyHome.js 의 규칙을 그대로 옮겼다(순수 함수, 화면 상태 없음).
 *   1) 서버 메뉴(GET /app-menus?surface=MY, groupNo 로 묶음)가 있으면 그것, 없으면 앱 기본 메뉴(웹 ROLE_MENUS 등).
 *   2) 강사·원장·무용수 보정(이름 바꾸기·막힌 메뉴 빼기·순서), 3) 경력·자격 확인 · 공간/양도 · 비공개 공고 · 상담 채팅 끼워 넣기.
 * @param str 문구(strings.xml) — 화면이 context::getString 을 넘긴다.
 */
object MyMenuBuilder {
    fun build(activeType: String, server: List<AppMenuItem>?, info: MyPageInfo?, str: (Int) -> String): List<List<MyMenuItem>> {
        val type = ActiveType.visible(activeType)
        val isGeneral = type == ActiveType.GENERAL
        val isDancer = type == ActiveType.DANCER
        val isLessonProvider = type == ActiveType.TEACHER || type == ActiveType.ACADEMY
        val m = Menus(str)

        // 1) 서버 또는 기본
        val groups: MutableList<MutableList<MyMenuItem>> = if (!server.isNullOrEmpty()) {
            val items = if (isDancer) server.map { it.dancerOrder() }.sortedWith(compareBy({ it.groupNo ?: 0 }, { it.order ?: 0 })) else server
            val out = mutableListOf<MutableList<MyMenuItem>>()
            var prev: Int? = Int.MIN_VALUE
            items.forEach { it ->
                if (out.isEmpty() || it.groupNo != prev) out.add(mutableListOf())
                prev = it.groupNo
                out.last().add(MyMenuItem(it.title.orEmpty(), it.icon ?: "myinfo", it.path, it.bridge, it.desc))
            }
            out
        } else {
            m.base(type).map { g -> g.flatMap { if (it.bridge == B_NOTIFICATIONS) listOf(it) + m.support else listOf(it) }.toMutableList() }.toMutableList()
        }

        // ★ 2026-11-14 '받은 견적'(서버 옛 이름) → 여는 화면 제목과 같은 '레슨 요청 내역'.
        groups.forEach { g -> g.replaceAll { if (it.bridge == B_MY_QUOTES) it.copy(title = str(R.string.my_menu_my_quotes)) else it } }

        // 2) 강사·원장·무용수 보정
        var final = groups.map { group ->
            if (!isLessonProvider && !isDancer) group
            else group.filter { item ->
                if ((isDancer || isLessonProvider) && item.path == "/receivedApplications") false
                else if (isDancer) !item.dancerBlocked() else item.bridge != "quoteBrowse"
            }.map { item ->
                when {
                    item.bridge == B_LESSON_HUB || item.path == "/lessonHub" ->
                        if (type == ActiveType.ACADEMY) item.copy(title = str(R.string.my_menu_academy_hub), bridge = B_LESSON_HUB, desc = str(R.string.my_desc_academy_hub))
                        else item.copy(title = str(R.string.my_menu_lesson_hub), bridge = B_LESSON_HUB)
                    item.path == "/myApplications" -> item.copy(title = str(R.string.my_menu_my_applications), icon = "filled-note")
                    isDancer && (item.bridge == B_JOB_POSTINGS || item.path == "/myPostings") ->
                        item.copy(title = str(R.string.my_menu_my_postings), bridge = B_JOB_POSTINGS, icon = "filled-note")
                    item.path == "/myReservations" -> item.copy(title = str(R.string.my_menu_my_reservations_booked))
                    else -> item
                }
            }.toMutableList()
        }.filter { !isDancer || it.isNotEmpty() }.toMutableList()

        if (isDancer) final.forEach { g ->
            val pi = g.indexOfFirst { it.bridge == B_JOB_POSTINGS || it.path == "/myPostings" }
            val ai = g.indexOfFirst { it.path == "/myApplications" }
            if (pi >= 0 && ai >= 0 && pi > ai) g.add(ai, g.removeAt(pi))
        }
        if (type == ActiveType.ACADEMY) {
            val hubG = final.indexOfFirst { g -> g.any { it.bridge == B_LESSON_HUB } }
            val jobG = final.indexOfFirst { g -> g.any { it.bridge == B_JOB_POSTINGS || it.path == "/myPostings" } }
            if (hubG < 0) {
                val hub = MyMenuItem(str(R.string.my_menu_academy_hub), "filled-note", "/lessonHub", B_LESSON_HUB, str(R.string.my_desc_academy_hub))
                if (jobG >= 0) final[jobG].add(0, hub) else final.add(1.coerceAtMost(final.size), mutableListOf(hub))
            } else if (hubG == jobG) {
                val g = final[hubG]
                val hi = g.indexOfFirst { it.bridge == B_LESSON_HUB }
                val ji = g.indexOfFirst { it.bridge == B_JOB_POSTINGS || it.path == "/myPostings" }
                if (ji >= 0 && hi > ji) g.add(ji, g.removeAt(hi))
            }
        }

        // 3) 끼워 넣기
        if ((type == ActiveType.TEACHER || type == ActiveType.DANCER) && final.isNotEmpty() && final.none { g -> g.any { it.path == "/my/credentials" } }) {
            final[0].add(MyMenuItem(str(R.string.my_menu_credentials), "myinfo", "/my/credentials", desc = str(R.string.my_desc_credentials)))
        }
        val market = listOfNotNull(
            MyMenuItem(str(R.string.my_menu_space_bookings), "filled-note", bridge = "mySpaceBookings", desc = str(R.string.my_desc_space_bookings)),
            if (type in setOf(ActiveType.ACADEMY, ActiveType.TEACHER, ActiveType.DANCER)) MyMenuItem(str(R.string.my_menu_my_spaces), "filled-building", bridge = "mySpaces", desc = str(R.string.my_desc_my_spaces)) else null,
            if (type in setOf(ActiveType.ACADEMY, ActiveType.TEACHER)) MyMenuItem(str(R.string.my_menu_my_transfers), "filled-building", bridge = "myStudioTransfers", desc = str(R.string.my_desc_my_transfers)) else null,
        ).filter { mi -> final.none { g -> g.any { it.bridge == mi.bridge } } }
        if (market.isNotEmpty() && final.isNotEmpty()) {
            val gi = final.indexOfFirst { g -> g.any { it.path == "/myReservations" } }
            if (gi >= 0) {
                val ii = final[gi].indexOfFirst { it.path == "/myReservations" }
                final[gi].addAll(ii + 1, market)
            } else final.add((final.size - 1).coerceAtLeast(0), market.toMutableList())
        }
        val pending = info?.pending
        if (isGeneral && pending != null && final.isNotEmpty()) {
            val role = pending.roles.firstOrNull()?.role
            final[0].add(0, MyMenuItem(
                String.format(str(R.string.my_menu_pending_postings), pending.total), "filled-note", "/myPostings",
                desc = if (pending.reviewing) str(R.string.my_desc_pending_reviewing) else String.format(str(R.string.my_desc_pending_verify), roleLabel(role, str)),
            ))
        }
        if (info?.supportChatEligible == true && final.isNotEmpty() && final.none { g -> g.any { it.path == "/support/chat" } }) {
            val chat = MyMenuItem(str(R.string.my_menu_support_chat), "ask", "/support/chat")
            val at = final.indexOfFirst { g -> g.any { it.path == "/support/inquiries" } }.let { gi ->
                if (gi >= 0) gi to final[gi].indexOfFirst { it.path == "/support/inquiries" }
                else final.indexOfFirst { g -> g.any { it.bridge == B_NOTIFICATIONS } }.let { g2 -> if (g2 >= 0) g2 to final[g2].indexOfFirst { it.bridge == B_NOTIFICATIONS } else null }
            }
            if (at != null) final[at.first].add(at.second + 1, chat) else final.last().add(chat)
        }
        return memberTypeOnTop(final, str(R.string.my_menu_member_type))
    }

    /** ★ 2026-11-14 개인정보 관리 경로 — 주황 머리(이름 줄)를 누르면 여기로 간다. */
    const val PROFILE_EDIT_PATH = "/profileEdit"

    /**
     * ★ 2026-11-14 '회원유형 변경 / 수정'을 맨 위(예전 '개인정보 관리' 자리)로 — 일반 회원이 유형 바꾸는 곳을 못 찾아
     *  탈퇴 후 재가입하던 문제. 개인정보 관리는 주황 머리를 누르면 열린다(목록엔 중복으로 두지 않는다).
     *  서버 메뉴 순서·이름과 상관없이 여기서 맞춘다(iOS memberTypeOnTop 1:1).
     */
    fun memberTypeOnTop(groups: List<List<MyMenuItem>>, title: String): List<List<MyMenuItem>> {
        val row = MyMenuItem(title, "myinfo", bridge = MyActions.MEMBER_TYPE)
        val out = groups.map { g -> g.filter { it.bridge != MyActions.MEMBER_TYPE }.toMutableList() }.toMutableList()
        val gi = out.indexOfFirst { g -> g.any { it.path == PROFILE_EDIT_PATH } }
        when {
            gi >= 0 -> out[gi][out[gi].indexOfFirst { it.path == PROFILE_EDIT_PATH }] = row
            out.isNotEmpty() -> out[0].add(0, row)
            else -> out.add(mutableListOf(row))
        }
        return out.filter { it.isNotEmpty() }
    }

    /** 비공개 공고 안내의 유형 이름(웹 PENDING_ROLE_LABEL). */
    fun roleLabel(role: String?, str: (Int) -> String): String = str(
        when (role) {
            ActiveType.ACADEMY -> R.string.pending_role_academy
            ActiveType.TEACHER -> R.string.pending_role_teacher
            ActiveType.TEAM -> R.string.pending_role_team
            else -> R.string.pending_role_member
        },
    )

    /** 설명 한 줄 — 멤버십은 이용 요약, 그 밖은 서버 desc → 기본 설명(웹 MENU_DESC). */
    fun descOf(item: MyMenuItem, membershipSummary: String?, str: (Int) -> String): String? =
        (if (item.bridge == "membership") membershipSummary else null) ?: item.desc ?: when {
            item.bridge == B_LESSON_HUB -> str(R.string.my_desc_lesson_hub)
            item.bridge == B_JOB_POSTINGS || item.path == "/myPostings" -> str(R.string.my_desc_my_postings)
            item.path == "/myApplications" -> str(R.string.my_desc_my_applications)
            item.path == "/subTrust" -> str(R.string.my_desc_sub_trust)
            item.path == "/myReservations" -> str(R.string.my_desc_my_reservations)
            else -> null
        }

    private fun AppMenuItem.dancerOrder(): AppMenuItem = when {
        bridge == B_JOB_POSTINGS || path == "/myPostings" -> copy(groupNo = 1, order = 0)
        path == "/myApplications" -> copy(groupNo = 1, order = 1)
        path == "/myReservations" -> copy(groupNo = 1, order = 2)
        else -> this
    }

    private val DANCER_BLOCKED_BRIDGES = setOf("quoteRequest", "myQuotes", "quoteBrowse", B_LESSON_HUB, "lessonSlotManage")
    private val DANCER_BLOCKED_PATHS = Regex("^/(quoteRequest|availableQuotes|quoteSettings|lessonHub|myLessons|lessonSettings|lessonGenres|lessonSlots)(/|\\?|$)")
    private fun MyMenuItem.dancerBlocked() = bridge in DANCER_BLOCKED_BRIDGES || DANCER_BLOCKED_PATHS.containsMatchIn(path.orEmpty())

    const val B_NOTIFICATIONS = "notifications"
    const val B_LESSON_HUB = "lessonQuoteHub"
    const val B_JOB_POSTINGS = "myJobPostings"
    const val B_MY_QUOTES = "myQuotes"

    /** 서버 메뉴가 없을 때의 기본(웹 ROLE_MENUS · GENERAL_MENU · SUPPORT_MENU 와 같은 순서). */
    private class Menus(val s: (Int) -> String) {
        fun i(t: Int, icon: String, path: String? = null, bridge: String? = null) = MyMenuItem(s(t), icon, path, bridge)
        val profile get() = i(R.string.my_menu_profile_edit, "myinfo", "/profileEdit")
        val common get() = listOf(
            i(R.string.my_menu_membership, "myinfo", "/membership/MEMBERSHIP", "membership"),
            i(R.string.my_menu_likes, "event", "/likes"),
            i(R.string.my_menu_member_type, "myinfo", bridge = "memberType"),
            i(R.string.my_menu_notice, "notice", "/notice"),
            i(R.string.my_menu_notifications, "notice", "/notifications", B_NOTIFICATIONS),
            i(R.string.my_menu_faq, "faq", "/faq"),
        )
        val support get() = listOf(i(R.string.my_menu_inquiries, "ask", "/support/inquiries"), i(R.string.my_menu_feedback, "edit", "/support/feedback"))

        fun base(type: String): List<List<MyMenuItem>> = when (type) {
            ActiveType.TEACHER -> listOf(
                listOf(profile, i(R.string.my_menu_resume, "myinfo", "/profileResume"), i(R.string.my_menu_academy_invites, "myinfo", "/my/academyInvites", "academyInvites")),
                listOf(
                    i(R.string.my_menu_lesson_hub, "filled-note", "/lessonHub", B_LESSON_HUB),
                    i(R.string.my_menu_my_postings, "filled-note", "/myPostings", B_JOB_POSTINGS),
                    i(R.string.my_menu_my_applications, "filled-note", "/myApplications"),
                    i(R.string.my_menu_my_reservations_booked, "filled-note", "/myReservations"),
                ),
                common,
            )
            ActiveType.ACADEMY -> listOf(
                listOf(profile, i(R.string.my_menu_academy_profile, "myinfo", "/academyProfile"), i(R.string.my_menu_job_create, "myinfo", "/jobs/create"),
                    i(R.string.my_menu_academy_teachers, "myinfo", "/my/academyTeachers", "academyTeachers")),
                listOf(
                    i(R.string.my_menu_academy_hub, "filled-note", "/lessonHub", B_LESSON_HUB),
                    i(R.string.my_menu_my_postings, "filled-note", "/myPostings", B_JOB_POSTINGS),
                    i(R.string.my_menu_my_reservations_booked, "filled-note", "/myReservations"),
                ),
                common,
            )
            ActiveType.TEAM -> listOf(
                listOf(profile, i(R.string.my_menu_team_postings, "myinfo", "/myPostings", B_JOB_POSTINGS)),
                listOf(i(R.string.my_menu_received_applications, "filled-paper", "/receivedApplications"), i(R.string.my_menu_dancers, "filled-paper", "/dancers")) + common,
            )
            ActiveType.DANCER -> listOf(
                listOf(profile, i(R.string.my_menu_dancer_resume, "myinfo", "/resume?as=dancer", "dancerProfile")),
                listOf(
                    i(R.string.my_menu_my_postings, "filled-note", "/myPostings", B_JOB_POSTINGS),
                    i(R.string.my_menu_my_applications, "filled-note", "/myApplications"),
                    i(R.string.my_menu_my_reservations_booked, "filled-note", "/myReservations"),
                ),
                common,
            )
            else -> listOf(
                listOf(profile),
                listOf(
                    i(R.string.my_menu_quote_request, "filled-note", "/myQuotes", "quoteRequest"),
                    i(R.string.my_menu_my_quotes, "filled-note", "/myQuotes", B_MY_QUOTES),
                    i(R.string.my_menu_reservations, "filled-note", "/myReservations"),
                    i(R.string.my_menu_likes, "event", "/likes"),
                    i(R.string.my_menu_recommend_blocks, "myinfo", "/recommendBlocks"),
                    i(R.string.my_menu_member_type, "myinfo", bridge = "memberType"),
                    i(R.string.my_menu_notice, "notice", "/notice"),
                    i(R.string.my_menu_notifications, "notice", "/notifications", B_NOTIFICATIONS),
                    i(R.string.my_menu_faq, "faq", "/faq"),
                ),
            )
        }
    }
}

/** 멤버십 이용 요약 — "스탠다드 이용 중 · 공고 1/3 · 레슨 무제한"(웹 MyHome membershipSummary + benefitUsageShortParts). */
fun membershipSummary(m: com.muyeon.domain.model.my.MyMembership?, str: (Int) -> String): String? {
    if (m == null || !m.active) return null
    val tier = when (m.tier) {
        "BASIC" -> str(R.string.tier_basic)
        "STANDARD" -> str(R.string.tier_standard)
        "PRO" -> str(R.string.tier_pro)
        else -> str(R.string.tier_default)
    }
    val parts = m.benefitUsage.filter { (it.kind == "COUNT" || it.kind == "MONTHLY") && it.limit != null }.take(2).map { b ->
        val name = when (b.key) {
            "POSTINGS" -> str(R.string.benefit_postings)
            "LESSONS" -> str(R.string.benefit_lessons)
            "AUTO_QUOTE" -> str(R.string.benefit_auto_quote)
            else -> b.label.orEmpty()
        }
        val limit = b.limit ?: 0
        val used = b.used ?: 0
        when {
            limit < 0 -> String.format(str(R.string.benefit_unlimited), name)
            b.kind == "COUNT" -> "$name $used/$limit"
            else -> String.format(str(R.string.benefit_left), name, (limit - used).coerceAtLeast(0))
        }
    }
    return (listOf(String.format(str(R.string.membership_using), tier)) + parts).joinToString(" · ")
}

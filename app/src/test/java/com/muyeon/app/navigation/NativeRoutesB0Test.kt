package com.muyeon.app.navigation

import com.muyeon.app.navigation.NativeRoutes.Target
import com.muyeon.app.ui.main.MainTab
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** 묶음 0 — 앱 화면이 이미 있는데 웹 창으로 가던 주소가 앱 화면으로 가는지(iOS NativeRoutes+B0 와 같은 표). */
class NativeRoutesB0Test {
    private fun a(name: String, vararg kv: Pair<String, String>) = Target.Action(name, kv.toMap())
    private fun r(path: String) = NativeRoutes.resolve(path)

    @Test fun profilesAndReviews() {
        assertEquals(a("openAcademyProfile", "academyId" to "7"), r("/academies/7"))
        assertEquals(a("openAcademyProfile", "academyId" to "7", "src" to "job"), r("/academies/7?src=job"))
        assertEquals(a("openReviewList", "teacherId" to "5"), r("/teachers/5/reviews"))
        assertEquals(a("openReviewWrite", "teacherId" to "5"), r("/teachers/5?review=1"))
        assertEquals(a("openPublicProfile", "userId" to "5", "src" to "quote"), r("/teachers/5?src=quote"))
        assertEquals(a("openPublicProfile", "userId" to "5"), r("/teachers/5"))
    }

    @Test fun resume() {
        assertEquals(a("openFieldVisibilityLink"), r("/fieldVisibility"))
        assertEquals(a("openFieldVisibilityLink", "as" to "dancer"), r("/fieldVisibility?as=dancer"))
        assertEquals(a("openResumeEditLink", "redirect" to "/resumeEdit"), r("/resumeEdit"))
        assertEquals(a("openResumeEditLink", "as" to "dancer", "redirect" to "/resumeEdit?as=dancer"), r("/resumeEdit?as=dancer"))
        assertEquals(a("openResumeEditLink", "id" to "12", "redirect" to "/resumeEdit?id=12"), r("/resumeEdit?id=12"))
        assertNull(r("/resumeEdit?id=abc"))
        assertEquals(a("openSeekProfile"), r("/profileManage"))
        assertEquals(a("openSeekProfile"), r("/profileManage?from=push"))
        assertEquals(a("openSeekProfile"), r("/profileManage?seek=1"))
    }

    @Test fun editors() {
        assertEquals(a("openJobPostingEdit", "jobId" to "3", "redirect" to "/jobs/3/edit"), r("/jobs/3/edit"))
        assertEquals(a("openLessonCreate", "lessonId" to "4"), r("/lessons/4/edit"))
        assertEquals(a("openLessonSettings"), r("/lessonSettings"))
        assertEquals(a("openSpaceEditor"), r("/spaces/create"))
        assertEquals(a("openSpaceEditor", "id" to "9"), r("/spaces/9/edit"))
        assertEquals(a("openStudioTransferEditor"), r("/studio-transfers/create"))
        assertEquals(a("openStudioTransferEditor", "id" to "2"), r("/studio-transfers/2/edit"))
        assertEquals(a("openStudioMemberDetail", "memberId" to "8", "redirect" to "/studio/members/8"), r("/studio/members/8"))
    }

    @Test fun castingStage() {
        assertEquals(a("openCastingStage", "screen" to "issues"), r("/casting/stage"))
        assertEquals(a("openCastingStage", "issueNo" to "3"), r("/casting/stage/3"))
        assertEquals(a("openCastingStage", "issueNo" to "3", "category" to "DOMESTIC"), r("/casting/stage/3/DOMESTIC"))
        // 11-10 공고 수정도 앱 화면(등록 흐름 수정 모드).
        assertEquals(a("openCastingStage", "castingId" to "5", "screen" to "edit"), r("/casting/5/edit"))
    }

    @Test fun accountAndMembership() {
        assertEquals(a("openMemberType", "redirect" to "/my/memberType"), r("/my/memberType"))
        assertEquals(a("openMemberType", "verify" to "ACADEMY", "redirect" to "/my/memberType?verify=ACADEMY"), r("/my/memberType?verify=ACADEMY"))
        assertEquals(a("openMemberType", "redirect" to "/memberSwitch"), r("/memberSwitch"))
        assertEquals(a("openAccountRecovery", "type" to "id"), r("/accountRecovery"))
        assertEquals(a("openAccountRecovery", "type" to "pw"), r("/accountRecovery?type=pw"))
        assertEquals(a("openMembership"), r("/pricing"))
        assertEquals(a("openQuoteBrowse"), r("/availableQuotes"))
        assertEquals(Target.Tab(MainTab.HOME), r("/home?roleApproved=TEACHER"))
        assertEquals(a("openMyQuotes"), r("/home?quotesPopup=1"))
    }

    @Test fun cleanup() {
        assertEquals(Target.Route(Screen.Likes.route, auth = true), r("/myScraps"))
        assertEquals(a("openCommunityCategory", "category" to "QUESTION"), r("/community/QUESTION"))
        assertEquals(Target.Route(Screen.CommunityList.route), r("/community"))
        // /community/write 는 묶음 3 이 앱 화면으로(NativeRoutesB3Test) — 카테고리로 걸리지 않는다.
        assertEquals(Target.Route("community/write", auth = true), r("/community/write"))
        // 취미 클래스는 없앴다 — 예전 링크는 홈 레슨 탭.
        assertEquals(Target.Tab(MainTab.LESSONS), r("/classes"))
        assertEquals(Target.Tab(MainTab.LESSONS), r("/classes/3"))
        assertEquals(Target.Tab(MainTab.LESSONS), r("/classes/3?src=push"))
    }
}

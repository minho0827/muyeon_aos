package com.muyeon.domain.di

import com.muyeon.domain.usecase.menu.GetMyMenusUseCase
import com.muyeon.domain.usecase.menu.GetTabMenusUseCase
import com.muyeon.domain.usecase.member.GetMyRolesUseCase
import com.muyeon.domain.usecase.member.GetRoleManageLaunchUseCase
import com.muyeon.domain.usecase.member.SwitchActiveTypeUseCase
import com.muyeon.domain.usecase.performance.GetPerformanceDetailUseCase
import com.muyeon.domain.usecase.performance.GetPerformancePageUseCase
import com.muyeon.domain.usecase.scrap.GetLikedIdsUseCase
import com.muyeon.domain.usecase.scrap.ToggleScrapUseCase
import com.muyeon.domain.usecase.community.*
import com.muyeon.domain.usecase.safety.BlockUserUseCase
import com.muyeon.domain.usecase.safety.ReportUseCase
import com.muyeon.domain.usecase.region.DetectRegionUseCase
import com.muyeon.domain.usecase.region.ObserveActiveRegionUseCase
import com.muyeon.domain.usecase.teacher.GetLessonTeachersPageUseCase
import com.muyeon.domain.usecase.uiflag.MarkGuideSeenUseCase
import com.muyeon.domain.usecase.uiflag.ShouldShowGuideUseCase
import org.koin.dsl.module

/**
 * :domain 유스케이스 등록. 저장소 구현은 :data 의 dataModule 이 바인딩한다.
 *   새 유스케이스는 여기 한 줄 — docs/ARCHITECTURE.md 9단계 체크리스트 6번.
 */
val domainModule = module {
    // 메뉴(탭·MY)
    factory { GetTabMenusUseCase(get()) }
    factory { GetMyMenusUseCase(get()) }
    // 회원 유형
    factory { GetMyRolesUseCase(get()) }
    factory { SwitchActiveTypeUseCase(get()) }
    factory { GetRoleManageLaunchUseCase(get()) }
    // 찜
    factory { GetLikedIdsUseCase(get(), get()) }
    factory { ToggleScrapUseCase(get(), get()) }
    factory { com.muyeon.domain.usecase.scrap.ObserveScrapChangesUseCase(get()) }
    // 공연
    factory { GetPerformancePageUseCase(get()) }
    factory { GetPerformanceDetailUseCase(get()) }
    // 커뮤니티
    factory { GetCommunityPostsUseCase(get()) }
    factory { GetCommunityPostUseCase(get()) }
    factory { GetCommunityCommentsUseCase(get()) }
    factory { EditCommunityCommentUseCase(get()) }
    factory { LikeCommunityUseCase(get()) }
    factory { DeleteCommunityPostUseCase(get()) }
    // 신고·차단
    factory { ReportUseCase(get()) }
    factory { BlockUserUseCase(get()) }
    // 강사·지역·안내
    factory { GetLessonTeachersPageUseCase(get()) }
    factory { ObserveActiveRegionUseCase(get()) }
    factory { DetectRegionUseCase(get()) }
    factory { ShouldShowGuideUseCase(get(), get()) }
    factory { MarkGuideSeenUseCase(get()) }
}

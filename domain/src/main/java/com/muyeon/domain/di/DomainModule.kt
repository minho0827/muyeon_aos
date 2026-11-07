package com.muyeon.domain.di

import com.muyeon.domain.usecase.menu.GetMyMenusUseCase
import com.muyeon.domain.usecase.menu.GetTabMenusUseCase
import com.muyeon.domain.usecase.member.GetMyRolesUseCase
import com.muyeon.domain.usecase.member.GetRoleManageLaunchUseCase
import com.muyeon.domain.usecase.member.SwitchActiveTypeUseCase
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
}

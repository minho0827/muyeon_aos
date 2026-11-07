package com.muyeon.data.di

import com.muyeon.data.local.MenuCache
import com.muyeon.data.repository.AppMenuRepositoryImpl
import com.muyeon.data.local.PrefsKeyValueStore
import com.muyeon.data.repository.MemberRepositoryImpl
import com.muyeon.data.repository.PerformanceRepositoryImpl
import com.muyeon.data.repository.ScrapRepositoryImpl
import com.muyeon.domain.common.KeyValueStore
import com.muyeon.domain.repository.PerformanceRepository
import com.muyeon.domain.repository.ScrapRepository
import com.muyeon.domain.repository.AppMenuRepository
import com.muyeon.domain.repository.MemberRepository
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

/** 저장소 구현 바인딩. 새 저장소는 여기 한 줄 — docs/ARCHITECTURE.md 9단계 체크리스트 5번. */
val dataModule = module {
    single { MenuCache(androidContext(), get()) }
    single<AppMenuRepository> { AppMenuRepositoryImpl(get(), get(), get()) }
    single<MemberRepository> { MemberRepositoryImpl(get(), get(), get()) }
    single<KeyValueStore> { PrefsKeyValueStore(androidContext()) }
    single<ScrapRepository> { ScrapRepositoryImpl(get(), get(), get()) }
    single<PerformanceRepository> { PerformanceRepositoryImpl(get(), get()) }
    single<com.muyeon.domain.repository.CommunityRepository> { com.muyeon.data.repository.CommunityRepositoryImpl(get(), get(), get()) }
    single<com.muyeon.domain.repository.TeacherRepository> { com.muyeon.data.repository.TeacherRepositoryImpl(get(), get()) }
    single<com.muyeon.domain.repository.RegionRepository> { com.muyeon.data.repository.RegionRepositoryImpl(androidContext(), get(), get()) }
    single<com.muyeon.domain.repository.UiFlagRepository> { com.muyeon.data.repository.UiFlagRepositoryImpl(androidContext(), get(), get(), get()) }
    single<com.muyeon.domain.repository.MyPageRepository> { com.muyeon.data.repository.MyPageRepositoryImpl(get(), get()) }
    single<com.muyeon.domain.repository.HomeRepository> { com.muyeon.data.repository.HomeRepositoryImpl(get(), get()) }
    single<com.muyeon.domain.repository.RecruitRepository> { com.muyeon.data.repository.RecruitRepositoryImpl(get(), get()) }
    single<com.muyeon.domain.repository.SafetyRepository> { com.muyeon.data.repository.SafetyRepositoryImpl(get(), get()) }
}

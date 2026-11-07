package com.muyeon.data.di

import com.muyeon.data.local.MenuCache
import com.muyeon.data.repository.AppMenuRepositoryImpl
import com.muyeon.data.repository.MemberRepositoryImpl
import com.muyeon.domain.repository.AppMenuRepository
import com.muyeon.domain.repository.MemberRepository
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

/** 저장소 구현 바인딩. 새 저장소는 여기 한 줄 — docs/ARCHITECTURE.md 9단계 체크리스트 5번. */
val dataModule = module {
    single { MenuCache(androidContext(), get()) }
    single<AppMenuRepository> { AppMenuRepositoryImpl(get(), get(), get()) }
    single<MemberRepository> { MemberRepositoryImpl(get(), get(), get()) }
}

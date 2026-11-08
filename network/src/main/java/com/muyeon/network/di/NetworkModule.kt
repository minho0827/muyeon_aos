package com.muyeon.network.di

import com.muyeon.network.BuildConfig
import com.muyeon.network.api.AppMenuApi
import com.muyeon.network.api.MemberApi
import com.muyeon.network.api.PerformanceApi
import com.muyeon.network.api.ScrapApi
import com.muyeon.network.interceptor.ForceLogoutInterceptor
import com.muyeon.network.interceptor.SessionHeadersInterceptor
import com.squareup.moshi.Moshi
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.core.module.Module
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

/**
 * :network Koin 모듈 — OkHttp/Retrofit/Moshi/Api 만. 저장소는 :data 의 dataModule.
 *   TokenStore·ActiveTypeStore·ForceLogoutTrigger 는 :app 의 appModule 이 바인딩한다.
 */
val networkModule = module {
    single<Moshi> { Moshi.Builder().build() }

    single<OkHttpClient> {
        val koin = getKoin()
        OkHttpClient.Builder()
            .addInterceptor(SessionHeadersInterceptor(get(), get()))
            // 늦게 꺼낸다 — 트리거 구현(:app)이 Activity 를 띄우므로 앱 시작 순서에 묶이지 않게.
            .addInterceptor(ForceLogoutInterceptor { koin.get() })
            // 연결 실패·타임아웃 → '다시 시도' 팝업(웹 GlobalNetworkOverlay). 구현(:app)은 늦게 꺼낸다.
            .addInterceptor(com.muyeon.network.interceptor.NetworkRetryInterceptor { koin.get() })
            .apply {
                if (BuildConfig.LOG_HTTP) addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC })
            }
            // 웹 axios timeout 30초와 같게.
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    single<Retrofit> {
        Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(get())
            .addConverterFactory(MoshiConverterFactory.create(get()))
            .build()
    }
}

/** 새 Api 는 여기 한 줄 — docs/ARCHITECTURE.md 9단계 체크리스트 3번. */
val apiModule = module {
    single<AppMenuApi> { get<Retrofit>().create(AppMenuApi::class.java) }
    single<MemberApi> { get<Retrofit>().create(MemberApi::class.java) }
    single<ScrapApi> { get<Retrofit>().create(ScrapApi::class.java) }
    single<PerformanceApi> { get<Retrofit>().create(PerformanceApi::class.java) }
    single<com.muyeon.network.api.CommunityApi> { get<Retrofit>().create(com.muyeon.network.api.CommunityApi::class.java) }
    single<com.muyeon.network.api.TeacherApi> { get<Retrofit>().create(com.muyeon.network.api.TeacherApi::class.java) }
    single<com.muyeon.network.api.RegionApi> { get<Retrofit>().create(com.muyeon.network.api.RegionApi::class.java) }
    single<com.muyeon.network.api.MyApi> { get<Retrofit>().create(com.muyeon.network.api.MyApi::class.java) }
    single<com.muyeon.network.api.HomeApi> { get<Retrofit>().create(com.muyeon.network.api.HomeApi::class.java) }
    single<com.muyeon.network.api.RecruitApi> { get<Retrofit>().create(com.muyeon.network.api.RecruitApi::class.java) }
    single<com.muyeon.network.api.SupportApi> { get<Retrofit>().create(com.muyeon.network.api.SupportApi::class.java) }
    single<com.muyeon.network.api.SafetyApi> { get<Retrofit>().create(com.muyeon.network.api.SafetyApi::class.java) }
}

fun networkModules(): List<Module> = listOf(networkModule, apiModule)

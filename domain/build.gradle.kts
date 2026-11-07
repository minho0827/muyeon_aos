plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

/**
 * ★ 2026-11-07 :domain — 모델·저장소 인터페이스·유스케이스·공통 결과 타입(PaceERA :domain 과 같은 자리).
 *   외부 라이브러리 최소: 순수 Kotlin + 코루틴 + Koin(등록용). Android·Retrofit·Moshi 를 모른다.
 */
android {
    namespace = "com.muyeon.domain"
    compileSdk = rootProject.extra["compileSdkVersion"] as Int
    defaultConfig {
        minSdk = rootProject.extra["minSdkVersion"] as Int
        consumerProguardFiles("consumer-rules.pro")
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin { jvmToolchain(17) }

dependencies {
    implementation(libs.kotlinx.coroutines.core)
    implementation(platform(libs.koin.bom))
    implementation(libs.koin.core)
    testImplementation(libs.junit)
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
}

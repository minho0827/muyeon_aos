plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
}

/**
 * ★ 2026-11-07 :network — 서버 경로(ApiEndpoints, 단일 출처)·Retrofit API·DTO(+toDomain)·인터셉터.
 *   PaceERA :network 와 같은 자리. 다만 DTO 어댑터는 Moshi **코드 생성(KSP)** 이다 —
 *   무용연 릴리스는 R8 을 켜므로(gradle.properties) 리플렉션 어댑터는 keep 규칙 누락 시 운영에서만 깨진다.
 */
android {
    namespace = "com.muyeon.network"
    compileSdk = rootProject.extra["compileSdkVersion"] as Int
    defaultConfig {
        minSdk = rootProject.extra["minSdkVersion"] as Int
        consumerProguardFiles("consumer-rules.pro")
        // dev·prod 모두 운영 서버(앱 build.gradle.kts API_BASE_URL 과 같은 값). Retrofit baseUrl 은 '/' 로 끝나야 한다.
        buildConfigField("String", "BASE_URL", "\"https://muyeon.co.kr/\"")
    }
    buildTypes {
        debug { buildConfigField("Boolean", "LOG_HTTP", "true") }
        release { buildConfigField("Boolean", "LOG_HTTP", "false") }
    }
    buildFeatures { buildConfig = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    testOptions { unitTests.isReturnDefaultValues = true }
}

kotlin { jvmToolchain(17) }

dependencies {
    implementation(project(":domain"))
    implementation(platform(libs.koin.bom))
    implementation(libs.koin.android)
    api(libs.retrofit)
    implementation(libs.retrofit.converter.moshi)
    api(libs.okhttp)
    implementation(libs.okhttp.logging)
    api(libs.moshi)
    ksp(libs.moshi.codegen)
    implementation(libs.kotlinx.coroutines.core)
    testImplementation(libs.junit)
}

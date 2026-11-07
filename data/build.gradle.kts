plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

/**
 * ★ 2026-11-07 :data — 저장소 구현(safeApiCall 로 Response → ApiResult)·로컬 캐시. PaceERA :data 와 같은 자리.
 *   의존: :data → :network, :domain. 화면(:app)은 :data 를 Koin 등록으로만 안다.
 */
android {
    namespace = "com.muyeon.data"
    compileSdk = rootProject.extra["compileSdkVersion"] as Int
    defaultConfig {
        minSdk = rootProject.extra["minSdkVersion"] as Int
        consumerProguardFiles("consumer-rules.pro")
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    testOptions { unitTests.isReturnDefaultValues = true }
}

kotlin { jvmToolchain(17) }

dependencies {
    implementation(project(":domain"))
    implementation(project(":network"))
    implementation(platform(libs.koin.bom))
    implementation(libs.koin.android)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
    testImplementation(libs.junit)
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
}

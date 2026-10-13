plugins {
    id("com.android.application") version "8.9.1" apply false
    id("com.android.library") version "8.9.1" apply false
    id("org.jetbrains.kotlin.android") version "2.1.20" apply false
    id("com.google.devtools.ksp") version "2.1.20-1.0.31" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.1.20" apply false
    id ( "com.google.firebase.appdistribution" ) version "4.0.1" apply false
    id ( "com.google.gms.google-services" ) version "4.4.0" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.0" apply false
}

val buildToolsVersion: String by extra("36.0.0")
val minSdkVersion: Int   by extra(24)
// Play 신규 제출은 targetSdk 36 이상 필수(2026-10 Play Console 출시 검토에서 오류로 차단됨).
val compileSdkVersion: Int by extra(36)
val targetSdkVersion: Int  by extra(36)
val ndkVersion: String     by extra("27.1.12297006")
val kotlinVersion: String  by extra("2.1.20")

buildscript {
    dependencies {
        classpath("com.android.tools.build:gradle:8.9.1")
    }
}

tasks.register("clean", org.gradle.api.tasks.Delete::class) {
    delete(rootProject.layout.buildDirectory)
}

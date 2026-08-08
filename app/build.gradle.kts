import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.gms.google-services")
}

val enableProguardInReleaseBuilds: Boolean =
    rootProject.findProperty("enableProguardInReleaseBuilds")
        ?.toString()?.toBoolean() ?: false

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localPropertiesFile.inputStream().use { localProperties.load(it) }
}

// -------------------------
// Read service-account.env at project root
// -------------------------
val serviceAccountFile = rootProject.file("service-account.env")
val saProps: MutableMap<String, String> = mutableMapOf()
if (serviceAccountFile.exists()) {
    serviceAccountFile.readLines().forEach { raw ->
        val line = raw.trim()
        if (line.isEmpty() || line.startsWith("#")) return@forEach
        val idx = line.indexOf('=')
        if (idx <= 0) return@forEach
        val key = line.substring(0, idx).trim()
        var value = line.substring(idx + 1).trim()
        // remove surrounding quotes if present
        if ((value.startsWith("\"") && value.endsWith("\"")) || (value.startsWith("'") && value.endsWith("'"))) {
            value = value.substring(1, value.length - 1)
        }
        saProps[key] = value
    }
} else {
    println("service-account.env not found at project root; BuildConfig SA_ fields will be empty")
}

android {
    ndkVersion = rootProject.extra["ndkVersion"] as String
    buildToolsVersion = rootProject.extra["buildToolsVersion"] as String
    compileSdk = rootProject.extra["compileSdkVersion"] as Int

    namespace = "com.muyeon.app"
    defaultConfig {
        applicationId = "com.muyeon.app"
        minSdk = rootProject.extra["minSdkVersion"] as Int
        targetSdk = rootProject.extra["targetSdkVersion"] as Int
        versionCode = 1
        versionName = "1.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Inject BuildConfig fields for service account (use empty defaults if not provided)
        val clientEmail = saProps["CLIENT_EMAIL"] ?: ""
        val projectId = saProps["PROJECT_ID"] ?: ""
        val tokenUri = saProps["TOKEN_URI"] ?: "https://oauth2.googleapis.com/token"
        val rawPrivateKey = saProps["PRIVATE_KEY"] ?: ""

        // Escape backslashes and double quotes for safe insertion into BuildConfig literal
        val escapedPrivateKey = rawPrivateKey.replace("\\", "\\\\").replace("\"", "\\\"")

        buildConfigField("String", "SA_CLIENT_EMAIL", "\"${clientEmail}\"")
        buildConfigField("String", "SA_PRIVATE_KEY", "\"${escapedPrivateKey}\"")
        buildConfigField("String", "SA_PROJECT_ID", "\"${projectId}\"")
        buildConfigField("String", "SA_TOKEN_URI", "\"${tokenUri}\"")
    }

    signingConfigs {
        create("release") {
            val storeFilePath = localProperties.getProperty("MY_RELEASE_STORE_FILE")
            if (storeFilePath != null) {
                storeFile = file(storeFilePath)
                storePassword = localProperties.getProperty("MY_RELEASE_STORE_PASSWORD")
                keyAlias = localProperties.getProperty("MY_RELEASE_KEY_ALIAS")
                keyPassword = localProperties.getProperty("MY_RELEASE_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("debug")
        }
        release {
            isMinifyEnabled = enableProguardInReleaseBuilds
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }

    flavorDimensions += "environment"

    productFlavors {
        create("dev") {
            dimension = "environment"
            buildConfigField("Boolean", "IS_DEV", "true")
            buildConfigField("String",  "API_BASE_URL", "\"https://muyeon.co.kr\"")
        }
        create("prod") {
            dimension = "environment"
            buildConfigField("Boolean", "IS_DEV", "false")
            buildConfigField("String",  "API_BASE_URL", "\"https://muyeon.co.kr\"")
        }
    }

    kotlin {
        jvmToolchain(17)
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "2.1.20"
    }

    packaging {
        resources {
            pickFirsts += "**/libc++_shared.so"
            pickFirsts += "**/x86/libc++_shared.so"
            pickFirsts += "**/x86_64/libc++_shared.so"
        }
    }
}

dependencies {
    // CameraX
    implementation("androidx.camera:camera-core:1.4.2")
    implementation("androidx.camera:camera-camera2:1.4.2")
    implementation("androidx.camera:camera-lifecycle:1.4.2")
    implementation("androidx.camera:camera-view:1.4.2")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")

    // Google ML Kit Barcode Scanning
    implementation("com.google.mlkit:barcode-scanning:17.3.0")

    implementation("com.google.code.gson:gson:2.10.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.2")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.6.2")
    implementation("androidx.activity:activity-ktx:1.8.0")
    // AndroidX / Compose / ...
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.0")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.constraintlayout:constraintlayout-compose:1.1.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose")
    implementation("androidx.compose.material3:material3:1.3.1")
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation(libs.androidx.ui.test.android)
    implementation(libs.play.services.location)
    implementation("io.coil-kt.coil3:coil-compose:3.2.0")
    implementation("io.insert-koin:koin-android:4.1.0")
    implementation("io.insert-koin:koin-androidx-compose:4.1.0")
    implementation("com.auth0:java-jwt:4.4.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.4")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.google.android.material:material:1.10.0")
    implementation("androidx.compose.runtime:runtime-livedata:1.6.8")
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("androidx.compose.foundation:foundation:1.5.4")
    implementation("com.github.bumptech.glide:glide:4.12.0")
    implementation("com.github.bumptech.glide:compose:1.0.0-beta01")
    annotationProcessor("com.github.bumptech.glide:compiler:4.12.0")

    // Firebase dependencies
    implementation(platform("com.google.firebase:firebase-bom:33.4.0"))
    implementation("com.google.firebase:firebase-messaging-ktx")
    implementation("com.google.firebase:firebase-analytics-ktx")

    implementation("com.android.volley:volley:1.2.1")
    implementation("org.json:json:20210307")

    // 채팅 — Socket.IO(/chat 네임스페이스). PaceERA 와 동일 버전.
    //  ⚠️ org.json 은 위에서 이미 들고 있으므로 socket.io 가 끌고 오는 중복 클래스를 제외한다.
    implementation("io.socket:socket.io-client:2.1.2") {
        exclude(group = "org.json", module = "json")
    }
    // 채팅 동영상 재생 — iOS AVKit VideoPlayer 대응.
    implementation("androidx.media3:media3-exoplayer:1.4.1")
    implementation("androidx.media3:media3-ui:1.4.1")
    // 백그라운드 진입 시 소켓 pause (ProcessLifecycleOwner)
    implementation("androidx.lifecycle:lifecycle-process:2.8.7")

    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}

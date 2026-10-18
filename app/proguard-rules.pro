# 무용연 R8 규칙
#  - Play "DEX 난독화 비율" 기준(25%) 때문에 라이브러리 통째 keep 은 하지 않는다.
#    AndroidX/Compose/Firebase/GMS/CameraX/ML Kit/Retrofit/OkHttp/Coil/Glide/Koin 은
#    각 AAR 의 consumer 규칙이 필요한 것만 보존한다.
#  - 여기엔 앱 코드에서 "이름"이 런타임 계약인 것만 둔다(웹 브릿지, Gson 필드, 리플렉션).

# 크래시 스택 추적용(mapping.txt 로 복원). 파일명은 숨김.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ==========================================
# WebView JS 브릿지 — 메서드명이 웹과의 계약 (window.AppBridge.xxx, AndroidToken.xxx …)
# ==========================================
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
# 브릿지 클래스와 웹으로 내보내는 응답 타입(Gson 직렬화 → 필드명 = JSON 키)
-keep class com.muyeon.app.webview.** { *; }

# ==========================================
# Gson / Retrofit 모델 — 필드명 = JSON 키
# ==========================================
-keep class com.muyeon.app.data.models.** { *; }
-keep class com.muyeon.app.domain.models.** { *; }

# Gson 2.10 은 자체 consumer 규칙이 없다 → 공식 권장 규칙
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes EnclosingMethod,InnerClasses
-dontwarn sun.misc.**
-keep class * extends com.google.gson.reflect.TypeToken
-keep,allowobfuscation,allowshrinking class com.google.gson.reflect.TypeToken
-keep class * implements com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Retrofit suspend 함수 반환 타입(Response<T>) 제네릭 보존
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation

# ==========================================
# java-jwt → Jackson databind (리플렉션 기반 직렬화)
# ==========================================
-keep class com.auth0.jwt.** { *; }
-keep class com.fasterxml.jackson.** { *; }
-dontwarn com.fasterxml.jackson.databind.**
-dontwarn java.beans.**
-dontwarn org.w3c.dom.bootstrap.**

# ==========================================
# 기타
# ==========================================
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# kotlinx.serialization (QrScannerViewModel 등)
-keepclassmembers class com.muyeon.app.** {
    *** Companion;
}
-keepclasseswithmembers class com.muyeon.app.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.muyeon.app.**$$serializer { *; }

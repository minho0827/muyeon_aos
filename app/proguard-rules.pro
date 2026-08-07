# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# ==========================================
# WebView JavaScript Interface - CRITICAL
# ==========================================
# Keep all WebView JavaScript interfaces
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# Keep EVERYTHING in webview package - methods, fields, constructors
-keep,allowobfuscation class com.muyeon.app.webview.** { *; }
-keepclassmembers class com.muyeon.app.webview.** {
    <init>(...);
    public *;
    private *;
    protected *;
}

# Keep all methods that might be called from JavaScript
-keepclassmembers class com.muyeon.app.webview.** {
    *** *(...);
}

# Specifically keep your JavaScript interfaces
-keep class com.muyeon.app.webview.LocationWebViewInterface { *; }
-keep class com.muyeon.app.webview.DownloadWebViewInterface { *; }
-keep class com.muyeon.app.webview.FileWebViewInterface { *; }
-keep class com.muyeon.app.webview.ScanQRWebViewInterface { *; }
-keep class com.muyeon.app.webview.DeviceInfoWebViewInterface { *; }
-keep class com.muyeon.app.webview.TokenWebViewInterface { *; }
-keep class com.muyeon.app.webview.NotificationWebViewInterface { *; }
-keep class com.muyeon.app.webview.PermissionWebViewInterface { *; }
-keep class com.muyeon.app.webview.PushNotificationInterface { *; }

# ==========================================
# Kotlin Coroutines
# ==========================================
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.** {
    volatile <fields>;
}

# ==========================================
# Retrofit & OkHttp
# ==========================================
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}

# ==========================================
# Gson
# ==========================================
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# Keep your data models (adjust package name as needed)
-keep class com.muyeon.app.data.** { *; }
-keep class com.muyeon.app.domain.** { *; }

# ==========================================
# Firebase
# ==========================================
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**

# ==========================================
# Jetpack Compose
# ==========================================
-keep class androidx.compose.** { *; }
-keep class androidx.lifecycle.** { *; }
-keepclassmembers class androidx.compose.** { *; }

# ==========================================
# CameraX & ML Kit
# ==========================================
-keep class androidx.camera.** { *; }
-keep class com.google.mlkit.** { *; }
-dontwarn androidx.camera.**

# ==========================================
# Koin (Dependency Injection)
# ==========================================
-keep class org.koin.** { *; }
-keep class org.koin.core.** { *; }
-keepnames class androidx.lifecycle.ViewModel

# ==========================================
# JWT
# ==========================================
-keep class com.auth0.jwt.** { *; }
-keepnames class com.auth0.jwt.** { *; }

# ==========================================
# General Android
# ==========================================
-keepclassmembers class * extends android.app.Activity {
   public void *(android.view.View);
}
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
-keep class * implements android.os.Parcelable {
  public static final android.os.Parcelable$Creator *;
}

# ==========================================
# Serialization
# ==========================================
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep Serializable classes
-keep,includedescriptorclasses class com.muyeon.app.**$$serializer { *; }
-keepclassmembers class com.muyeon.app.** {
    *** Companion;
}
-keepclasseswithmembers class com.muyeon.app.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# ==========================================
# Glide
# ==========================================
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep class * extends com.bumptech.glide.module.AppGlideModule {
 <init>(...);
}
-keep public enum com.bumptech.glide.load.ImageHeaderParser$** {
  **[] $VALUES;
  public *;
}
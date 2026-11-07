# ★ 2026-11-07 Retrofit 인터페이스·Moshi 생성 어댑터 — R8 이 지우지 않게.
-keep,allowobfuscation interface com.muyeon.network.api.** { *; }
-keepclassmembers class com.muyeon.network.dto.** { <init>(...); <fields>; }
-keep class com.muyeon.network.dto.**JsonAdapter { *; }
-keepattributes Signature, RuntimeVisibleAnnotations, AnnotationDefault, *Annotation*
-if interface * { @retrofit2.http.* public *** *(...); }
-keep,allowoptimization,allowshrinking,allowobfuscation class <3>
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation
-keep,allowobfuscation,allowshrinking class retrofit2.Response

# Keep ваши важные классы
-keep class com.example.airconditionerapp.** { *; }
-keep class com.example.airconditionerapp.models.** { *; }
-keep class com.example.airconditionerapp.utils.** { *; }

# Keep аннотации
-keepattributes *Annotation*

# Keep названия методов для reflection
-keepclassmembers class ** {
    @android.webkit.JavascriptInterface public *;
}

# Для Gson/JSON
-keepattributes Signature
-keepattributes *Annotation*

-keep class com.google.gson.stream.** { *; }
# Правила для библиотеки androidx.security.crypto (Tink)
-keep class javax.annotation.** { *; }
-dontwarn javax.annotation.**

# Общее правило для безопасности (опционально, но рекомендуется)
-keep class com.google.crypto.tink.** { *; }
-dontwarn com.google.crypto.tink.**
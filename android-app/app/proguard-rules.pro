# iText
-keep class com.itextpdf.** { *; }
-dontwarn com.itextpdf.**

# Apache POI
-keep class org.apache.poi.** { *; }
-dontwarn org.apache.poi.**
-dontwarn org.apache.xmlbeans.**
-dontwarn org.apache.commons.**

# Retrofit / OkHttp
-keepattributes Signature
-keepattributes *Annotation*
-keep class retrofit2.** { *; }
-dontwarn retrofit2.**
-keep class okhttp3.** { *; }
-dontwarn okhttp3.**

# Gson
-keep class com.google.gson.** { *; }
-keep class com.analista.fileorganizer.data.model.** { *; }

# ML Kit
-keep class com.google.mlkit.** { *; }

# Keep Firebase
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }

# Keep Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Keep Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }

# Keep Kotlin Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.familyos.**$$serializer { *; }
-keepclassmembers class com.familyos.** {
    *** Companion;
}
-keepclasseswithmembers class com.familyos.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Timber
-dontwarn org.jetbrains.annotations.**

# WorkManager / CameraX / ZXing
-keep class androidx.work.** { *; }
-dontwarn com.google.zxing.**

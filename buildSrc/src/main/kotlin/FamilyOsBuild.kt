// Shared Android library convention applied via copy-paste module scripts.
import org.gradle.api.JavaVersion

object FamilyOsBuild {
    const val APPLICATION_ID = "com.familyos.app"
    const val MIN_SDK = 29
    const val TARGET_SDK = 35
    const val COMPILE_SDK = 35
    const val VERSION_CODE = 1
    const val VERSION_NAME = "1.0.0"
    val JAVA_VERSION = JavaVersion.VERSION_17
}

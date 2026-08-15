plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.familyos.core.ui"
    compileSdk = FamilyOsBuild.COMPILE_SDK

    defaultConfig {
        minSdk = FamilyOsBuild.MIN_SDK
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = FamilyOsBuild.JAVA_VERSION
        targetCompatibility = FamilyOsBuild.JAVA_VERSION
    }

    kotlinOptions {
        jvmTarget = FamilyOsBuild.JAVA_VERSION.toString()
    }
}

dependencies {
    implementation(project(":core"))
    implementation(project(":core_domain"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(platform(libs.androidx.compose.bom))
    api("androidx.compose.ui:ui-android")
    api("androidx.compose.foundation:foundation-android")
    api("androidx.compose.material3:material3-android")
    api(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material.icons)
    implementation("androidx.compose.runtime:runtime-android")
    implementation(libs.coil.compose)
    implementation(libs.androidx.biometric)
    implementation(libs.androidx.activity.compose)
    implementation("androidx.fragment:fragment-ktx:1.8.6")

    debugImplementation(libs.androidx.compose.ui.tooling)
}

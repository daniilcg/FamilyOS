plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.familyos.core.domain"
    compileSdk = FamilyOsBuild.COMPILE_SDK

    defaultConfig {
        minSdk = FamilyOsBuild.MIN_SDK
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
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
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.paging.runtime)
    implementation("javax.inject:javax.inject:1")

    testImplementation(libs.junit)
    testImplementation(libs.truth)
}

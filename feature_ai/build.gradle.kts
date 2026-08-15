import java.util.Properties

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.familyos.feature.ai"
    compileSdk = FamilyOsBuild.COMPILE_SDK

    defaultConfig {
        minSdk = FamilyOsBuild.MIN_SDK
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")

        val localProperties = Properties()
        val localFile = rootProject.file("local.properties")
        if (localFile.exists()) {
            localFile.inputStream().use { localProperties.load(it) }
        }
        fun prop(key: String): String =
            (localProperties.getProperty(key) ?: "").replace("\"", "\\\"")

        buildConfigField("String", "AI_OPENAI_KEY", "\"${prop("AI_OPENAI_KEY")}\"")
        buildConfigField("String", "AI_GEMINI_KEY", "\"${prop("AI_GEMINI_KEY")}\"")
        buildConfigField("String", "AI_OPENROUTER_KEY", "\"${prop("AI_OPENROUTER_KEY")}\"")
    }

    compileOptions {
        sourceCompatibility = FamilyOsBuild.JAVA_VERSION
        targetCompatibility = FamilyOsBuild.JAVA_VERSION
    }

    kotlinOptions {
        jvmTarget = FamilyOsBuild.JAVA_VERSION.toString()
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(project(":core"))
    implementation(project(":core_domain"))
    implementation(project(":core_ui"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.activity.compose)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.androidx.datastore)

    debugImplementation(libs.androidx.compose.ui.tooling)
}

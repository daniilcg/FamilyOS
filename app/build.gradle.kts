plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.google.services)
}

import java.util.Properties

android {
    namespace = "com.familyos.app"
    compileSdk = FamilyOsBuild.COMPILE_SDK

    defaultConfig {
        applicationId = FamilyOsBuild.APPLICATION_ID
        minSdk = FamilyOsBuild.MIN_SDK
        targetSdk = FamilyOsBuild.TARGET_SDK
        versionCode = FamilyOsBuild.VERSION_CODE
        versionName = FamilyOsBuild.VERSION_NAME
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true

        val localProps = Properties().apply {
            val f = rootProject.file("local.properties")
            if (f.exists()) {
                f.inputStream().use { stream -> load(stream) }
            }
        }
        val googleServicesText = runCatching { file("google-services.json").readText() }.getOrDefault("")
        val webClientFromJson = Regex("\"client_id\"\\s*:\\s*\"([^\"]+\\.apps\\.googleusercontent\\.com)\"")
            .findAll(googleServicesText)
            .map { it.groupValues[1] }
            .firstOrNull { !it.contains("000000000000") && !it.contains("Placeholder", ignoreCase = true) }
            .orEmpty()
        val fromLocal = localProps.getProperty("GOOGLE_WEB_CLIENT_ID").orEmpty()
        val webClientId = when {
            fromLocal.isNotBlank() -> fromLocal
            webClientFromJson.isNotBlank() -> webClientFromJson
            else -> ""
        }
        // Do not resValue default_web_client_id — google-services plugin already generates it.
        buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"${webClientId.replace("\"", "\\\"")}\"")

        val firebaseConfigured = googleServicesText.isNotBlank() &&
            !googleServicesText.contains("AIzaSyPlaceholder") &&
            !googleServicesText.contains("\"project_number\": \"000000000000\"")
        buildConfigField("boolean", "FIREBASE_CONFIGURED", firebaseConfigured.toString())
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
        debug {
            isMinifyEnabled = false
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
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

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}

dependencies {
    implementation(project(":core"))
    implementation(project(":core_domain"))
    implementation(project(":core_data"))
    implementation(project(":core_ui"))
    implementation(project(":feature_auth"))
    implementation(project(":feature_home"))
    implementation(project(":feature_shopping"))
    implementation(project(":feature_tasks"))
    implementation(project(":feature_calendar"))
    implementation(project(":feature_budget"))
    implementation(project(":feature_documents"))
    implementation(project(":feature_notes"))
    implementation(project(":feature_notifications"))
    implementation(project(":feature_ai"))
    implementation(project(":feature_profile"))
    implementation(project(":feature_family"))
    implementation(project(":feature_settings"))
    implementation(project(":feature_chat"))
    implementation(project(":feature_billing"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.lifecycle.runtime)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.work)
    ksp(libs.hilt.work.compiler)

    implementation(libs.androidx.work.runtime)
    implementation(libs.androidx.biometric)
    implementation(libs.coil.compose)
    implementation(libs.timber)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.storage)
    implementation(libs.firebase.messaging)

    testImplementation(libs.junit)
    testImplementation(libs.truth)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
    testImplementation(libs.robolectric)

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.truth)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
}

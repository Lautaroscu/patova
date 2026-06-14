import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

val localProperties = Properties().apply {
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        load(localPropertiesFile.inputStream())
    }
}

val rawApiKey = localProperties.getProperty("PATOVA_API_KEY")
    ?: System.getenv("PATOVA_API_KEY")
    ?: "dev-dummy-key"

val patovaApiKey = if (rawApiKey.startsWith("\"") && rawApiKey.endsWith("\"")) {
    rawApiKey
} else {
    "\"$rawApiKey\""
}

val rawDebugBaseUrl = localProperties.getProperty("PATOVA_DEBUG_API_BASE_URL")
    ?: System.getenv("PATOVA_DEBUG_API_BASE_URL")
    ?: "https://699b-152-170-2-32.ngrok-free.app/"

val patovaDebugBaseUrl = if (rawDebugBaseUrl.startsWith("\"") && rawDebugBaseUrl.endsWith("\"")) {
    rawDebugBaseUrl
} else {
    "\"$rawDebugBaseUrl\""
}

val rawReleaseBaseUrl = localProperties.getProperty("PATOVA_RELEASE_API_BASE_URL")
    ?: System.getenv("PATOVA_RELEASE_API_BASE_URL")
    ?: "https://patova-api.serra.agency/"

val patovaReleaseBaseUrl = if (rawReleaseBaseUrl.startsWith("\"") && rawReleaseBaseUrl.endsWith("\"")) {
    rawReleaseBaseUrl
} else {
    "\"$rawReleaseBaseUrl\""
}

android {
    namespace = "ar.com.patova"
    compileSdk = 34

    defaultConfig {
        applicationId = "ar.com.patova"
        minSdk = 29
        targetSdk = 34
        versionCode = 1
        versionName = "0.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        buildConfigField("String", "API_BASE_URL", patovaDebugBaseUrl)
        buildConfigField("String", "API_KEY", patovaApiKey)
    }

    buildTypes {
        debug {
            buildConfigField("String", "API_BASE_URL", patovaDebugBaseUrl)
        }
        release {
            isMinifyEnabled = false
            buildConfigField("String", "API_BASE_URL", patovaReleaseBaseUrl)
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.compose.compiler.get()
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    ksp {
        arg("room.schemaLocation", "$projectDir/schemas")
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    debugImplementation(libs.compose.ui.tooling)

    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)
    implementation(libs.hilt.navigation.compose)

    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization.converter)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging.interceptor)

    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    implementation(libs.work.runtime.ktx)
    implementation(libs.navigation.compose)
    implementation(libs.lifecycle.viewmodel.compose)

    implementation(libs.hilt.work)
    ksp(libs.hilt.work.compiler)

    implementation(libs.androidx.security.crypto)
    implementation(libs.androidx.browser)
    implementation(libs.androidx.datastore.preferences)

    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.junit)
}

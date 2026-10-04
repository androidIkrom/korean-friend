plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

/** Gemini keys from the git-ignored .env, in file order, comma-separated; empty when the file is missing. */
val geminiKeys: String = rootProject.file(".env").takeIf { it.isFile }?.readLines()
    ?.map { it.trim() }
    ?.filter { it.startsWith("GEMINI_API_KEY") && "=" in it }
    ?.map { it.substringAfter("=").trim() }
    ?.filter { it.isNotEmpty() }
    ?.joinToString(",")
    .orEmpty()

android {
    namespace = "uz.hangulfriend"
    compileSdk = 37

    defaultConfig {
        applicationId = "uz.hangulfriend"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "0.1.0"
        buildConfigField("String", "GEMINI_KEYS", "\"$geminiKeys\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }

    // Exported Room schemas for MigrationTestHelper: Robolectric only sees the debug variant's merged assets,
    // so the schemas ship in debug builds (a few KB), never in release.
    sourceSets.getByName("debug").assets.directories.add("$projectDir/schemas")
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(project(":core:hangul"))
    implementation(project(":core:srs"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons)
    implementation(libs.haze)
    implementation(libs.haze.blur)
    debugImplementation(libs.compose.ui.tooling)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
    implementation(libs.datastore.preferences)
    implementation(libs.work.runtime.ktx)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.markdown.m3)
    implementation(libs.media3.exoplayer)

    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.test.ext.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.room.testing)
}

plugins {
    alias(libs.plugins.android.test)
    alias(libs.plugins.androidx.baselineprofile)
    alias(libs.plugins.detekt)
}

android {
    namespace = "dev.ronellsalunke.modellm.baselineprofile"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        // Baseline Profiles start at API 28. Older releases fully AOT compile the APK.
        minSdk = 28
        targetSdk = 37
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    targetProjectPath = ":app"

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    // Run the profile generator in its own process, apart from the app under test.
    experimentalProperties["android.experimental.self-instrumenting"] = true
}

detekt {
    buildUponDefaultConfig = true
    config.setFrom(rootProject.files("config/detekt/detekt.yml"))
    parallel = true
    basePath.set(projectDir)
}

dependencies {
    implementation(libs.androidx.benchmark.macro.junit4)
    implementation(libs.androidx.junit)
    implementation(libs.androidx.test.runner)
    implementation(libs.androidx.uiautomator)
}

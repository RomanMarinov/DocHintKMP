plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeCompiler)
}
android {
    namespace = "app.romanmarinov.dochintkmp.androidApp"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    defaultConfig {
        applicationId = "app.romanmarinov.dochintkmp.androidApp"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(projects.composeApp)
    // Tests reference Android+KMP types directly (ApiKeyStorage, FileType, MedicalData, ParseResult),
    // so we need these modules on the test compilation classpath.
    testImplementation(projects.data)
    testImplementation(projects.domain)
    testImplementation(libs.robolectric)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.testExt.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    implementation(libs.koin.android)
    implementation(libs.koin.androidx.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.compose.uiToolingPreview)
}
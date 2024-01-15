@Suppress("DSL_SCOPE_VIOLATION") // TODO: Remove once KTIJ-19369 is fixed
plugins {
    alias(libs.plugins.androidApplication)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.facedetection"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.facedetection"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.constraintlayout)
    implementation(libs.lifecycle.livedata.ktx)
    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)
    implementation(libs.camera.view)
    implementation(libs.play.services.vision)
    implementation(libs.firebase.auth)
    implementation(libs.firebase.database)
    implementation(libs.firebase.storage)
    implementation(libs.firebase.ml.vision)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation (libs.camera.core)
    implementation (libs.androidx.camera.camera2)
    implementation (libs.androidx.camera.lifecycle)
    implementation (libs.androidx.camera.video)
    implementation (libs.camera.view)
    implementation (libs.androidx.camera.extensions)
    implementation (libs.mlkit.face.detection)
    implementation (libs.text.recognition)
    implementation (libs.text.recognition.chinese)
    // If you want to use the base sdk
    implementation (libs.pose.detection)
    // If you want to use the accurate sdk
    implementation (libs.pose.detection.accurate)

    implementation ("com.google.mlkit:object-detection:17.0.0")
    implementation ("com.google.mlkit:image-labeling:17.0.7")

    implementation ("com.google.guava:guava:30.1-android")


}
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.example.airconditionerapp"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.airconditionerapp"
        minSdk = 24
        targetSdk = 34
        versionCode = 2
        versionName = "1.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true  // Включить обфускацию
            isShrinkResources = true  // Удалить неиспользуемые ресурсы
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    buildFeatures {
        viewBinding = true
    }


    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    // ... остальные зависимости
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    implementation("com.google.android.flexbox:flexbox:3.0.0")
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    // Для работы с JSON
    implementation("org.json:json:20230227")
// Для работы с корутинами
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
}

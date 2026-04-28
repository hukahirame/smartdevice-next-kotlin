plugins {
    id("com.android.application")
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.example.smartdevice"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.smartdevice"
        minSdk = 29
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            // エラー回避のために `minifyEnabled` を使用
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }

    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        viewBinding = true
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    implementation("androidx.core:core:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.1")
    implementation("androidx.navigation:navigation-fragment:2.8.9")
    implementation("androidx.navigation:navigation-ui-ktx:2.8.9")
    implementation ("androidx.work:work-runtime:2.8.0")
    implementation ("com.google.android.material:material:1.12.0")
    implementation ("com.google.android.gms:play-services-ads:22.0.0")
    implementation(libs.androidx.core.ktx)
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
}


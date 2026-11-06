plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "br.com.abadeus.paella"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "br.com.abadeus.paella"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.activity.ktx)
    implementation(libs.appcompat)
    implementation(libs.constraintlayout)
    implementation(libs.material)
    implementation(libs.recyclerview)
    implementation(libs.swiperefreshlayout)

    // Comunicacao com a API (servidor)
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)

    // Leitor de QR Code (retirada)
    implementation(libs.zxing.embedded)

    testImplementation(libs.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.ext.junit)
}

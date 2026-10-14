plugins {
    id("com.android.application")
}

android {
    namespace = "cn.nua.wenguan.schedule"
    compileSdk = 35

    defaultConfig {
        applicationId = "cn.nua.wenguan.schedule"
        minSdk = 26
        targetSdk = 35
        versionCode = 31
        versionName = "1.2.2"
        testInstrumentationRunner = "cn.nua.wenguan.schedule.test.SyncFailureInstrumentation"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    // Local JExcelAPI only; do not pull its obsolete optional Log4J dependency.
    implementation(files("libs/jxl-2.6.12.jar"))
}

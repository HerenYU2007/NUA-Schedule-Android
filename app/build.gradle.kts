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
        versionCode = 34
        versionName = "1.2.5"
        testInstrumentationRunner = "cn.nua.wenguan.schedule.test.SyncFailureInstrumentation"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    androidResources {
        additionalParameters += listOf("--stable-ids", file("stable-resource-ids.txt").absolutePath)
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

// Track the contents, not just the AAPT command-line path.
tasks.matching { it.name.startsWith("process") && it.name.endsWith("Resources") }.configureEach {
    inputs.file(file("stable-resource-ids.txt"))
}

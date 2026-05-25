plugins {
    alias(libs.plugins.android.library)
}

dependencies {
    compileOnly(libs.okhttp)
}

android {
    namespace = "com.flixclusive.provider.app.util"
    buildFeatures {
        buildConfig = true
    }
}


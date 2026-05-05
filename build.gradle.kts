import com.flixclusive.gradle.util.android
import com.flixclusive.gradle.util.flxProvider

plugins {
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.flixclusive.provider) apply false
}

subprojects {
    apply(plugin = "flx-provider")

    android {
        compileSdk = 36

        defaultConfig {
            minSdk = 23
            testOptions.targetSdk = 36
        }
    }

    flxProvider {
        setRepository("https://github.com/flixclusiveorg/flx-providers")
    }
}

tasks.register<Delete>("clean") {
    delete(rootProject.layout.buildDirectory)
}
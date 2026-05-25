import com.flixclusive.gradle.util.android
import com.flixclusive.gradle.util.flxProvider

plugins {
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.flixclusive.provider) apply false
}

subprojects {
    if (this.name != "util") {
        apply(plugin = "flx-provider")
    }

    plugins.withId("com.android.library") {
        android {
            compileSdk = 36

            defaultConfig {
                minSdk = 23
                testOptions.targetSdk = 36
            }

            buildTypes {
                release {
                    isMinifyEnabled = true
                    proguardFiles(
                        getDefaultProguardFile("proguard-android-optimize.txt"),
                        "../../core/proguard/proguard-rules.pro"
                    )
                }
            }
        }
    }

    plugins.withId("flx-provider") {
        flxProvider {
            setRepository("https://github.com/flixclusiveorg/flx-providers")
        }
    }
}

tasks.register<Delete>("clean") {
    delete(rootProject.layout.buildDirectory)
}
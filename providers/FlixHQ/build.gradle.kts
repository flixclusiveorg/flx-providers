import com.flixclusive.model.provider.Language
import com.flixclusive.model.provider.ProviderType
import com.flixclusive.model.provider.Status

dependencies {
    implementation("androidx.core:core:1.13.1")

    // Comment if not implementing own SettingsScreen
    val composeBom = platform("androidx.compose:compose-bom:2024.04.00")
    implementation(composeBom)
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.runtime:runtime")
    // ================= END: COMPOSE UI =================

}

android {
    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.3"
    }
}

flxProvider {
    description.set("""
        NOTICE: This provider uses WebView to scrape content. This might lag.
        
        Stream seamlessly in adjustable HD resolution (1080p) with blazing-fast loading speeds. Offers HLS media.
    """.trimIndent())

    changelog.set("""
        # v1.0.2 - patch
        Fix invalid user agents
    """.trimIndent())

    versionMajor = 1
    versionMinor = 0
    versionPatch = 3
    versionBuild = 1

    iconUrl.set("https://i.imgur.com/LNtqPTi.png")

    language.set(Language.Multiple)

    providerType.set(ProviderType.All)

    status.set(Status.Working)
}


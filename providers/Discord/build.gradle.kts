import com.flixclusive.model.provider.Language
import com.flixclusive.model.provider.ProviderStatus
import com.flixclusive.model.provider.ProviderType

plugins {
    alias(libs.plugins.kotlinx.serialization)
}

dependencies {
    coreLibraryDesugaring(libs.desugar.jdk.libs)
    compileOnly(libs.core.stubs.provider)

    compileOnly(platform(libs.compose.bom))
    compileOnly(libs.compose.material3)
    compileOnly(libs.compose.foundation)
    compileOnly(libs.compose.ui)
    compileOnly(libs.compose.runtime)
    compileOnly(libs.compose.activity)
    compileOnly(libs.lifecycle.runtime.compose)
    compileOnly(libs.coil.compose)

    compileOnly(libs.okhttp)
    compileOnly(libs.kotlinx.coroutines)
    compileOnly(libs.datastore)

    implementation(libs.kotlinx.serialization.json)

    testCompileOnly(platform(libs.compose.bom))
    testCompileOnly(libs.compose.runtime)
    testImplementation(libs.core.stubs.provider)
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.strikt)
    testImplementation(libs.kotlinx.coroutines)
    testRuntimeOnly(libs.junit.platform.launcher)
}

android {
    namespace = "com.flixclusive.provider.app.discord"

    testOptions {
        unitTests.all { it.useJUnitPlatform() }
    }
}

flxProvider {
    id = "flx-discord-dde6f7e8"

    adult = false
    providerName = "Discord"
    description = "Shows what you're watching on your Discord profile as a Rich Presence activity."

    changelog = """
        ## 0.0.1
        - Initial scaffold
    """.trimIndent()

    versionMajor = 0
    versionMinor = 0
    versionPatch = 2
    versionBuild = 0

    language = Language.Multiple
    providerType = ProviderType("Tracker")
    status = ProviderStatus.Beta

    iconUrl = "https://i.imgur.com/VkuQEQC.png"

    requiresResources = false
}

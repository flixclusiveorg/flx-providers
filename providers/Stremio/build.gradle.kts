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

    compileOnly(libs.datastore)

    compileOnly(libs.coil.compose)

    compileOnly(libs.okhttp)

    implementation(projects.util)

    implementation(libs.kotlinx.serialization.json)
    compileOnly(libs.kotlinx.coroutines)

    testCompileOnly(libs.okhttp)
    testCompileOnly(libs.junit.jupiter)
    testCompileOnly(libs.strikt)
}

android {
    namespace = "com.flixclusive.provider.app.stremio"
    buildFeatures {
        buildConfig = true
    }
}

flxProvider {
    id = "flx-stremio-1778084225"
    
    description = """
        A flixclusive adapter for Stremio addons. Torrent addons, such as Torrentio, don't work without debrid accounts.
    """.trimIndent()

    changelog = """
        - Updated to Core Stubs SDK v1.3.0
        - Brought this back just for testing - might remove again if I don't have time to maintain it
    """.trimIndent()

    versionMajor = 1
    versionMinor = 3
    versionPatch = 1
    versionBuild = 0

    // Extra authors for specific provider
    // author(
    //    name = "...",
    //    githubLink = "https://github.com/...",
    // )
    // ===

    iconUrl = "https://i.imgur.com/Hoq93zL.png" // OPTIONAL

    language = Language.Multiple

    providerType = ProviderType.All

    status = ProviderStatus.Working

    requiresResources = true
}
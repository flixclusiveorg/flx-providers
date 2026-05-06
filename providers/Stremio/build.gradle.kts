import com.flixclusive.model.provider.Language
import com.flixclusive.model.provider.ProviderType
import com.flixclusive.model.provider.Status

plugins {
    alias(libs.plugins.kotlinx.serialization)
}

dependencies {
    coreLibraryDesugaring(libs.desugar.jdk.libs)
    implementation(libs.core.stubs.provider)

    compileOnly(platform(libs.compose.bom))
    compileOnly(libs.compose.material3)
    compileOnly(libs.compose.foundation)
    compileOnly(libs.compose.ui)
    compileOnly(libs.compose.runtime)
    compileOnly(libs.compose.activity)

    compileOnly(libs.datastore)

    compileOnly(libs.coil.compose)

    compileOnly(libs.okhttp)
    compileOnly(libs.retrofit)
    compileOnly(libs.retrofit.kotlinx.serialization)

    compileOnly(libs.kotlinx.serialization.json)
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
    description = """
        A flixclusive adapter for Stremio addons. Torrent addons, such as Torrentio, don't work without debrid accounts.
    """.trimIndent()

    changelog = """
        ### Fixes:
        - Remove built-in opensubs-v3 addon
        - Allow subtitles addons
        - Fix default metadata loading (Cinemata)
    """.trimIndent()

    versionMajor = 1
    versionMinor = 2
    versionPatch = 6
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

    status = Status.Working

    requiresResources = true
}


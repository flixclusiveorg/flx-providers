import com.flixclusive.model.provider.Language
import com.flixclusive.model.provider.ProviderStatus
import com.flixclusive.model.provider.ProviderType
import org.jetbrains.kotlin.konan.properties.Properties

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
    compileOnly(libs.lifecycle.runtime.compose)
    compileOnly(libs.coil.compose)

    compileOnly(libs.okhttp)
    compileOnly(libs.kotlinx.coroutines)
    compileOnly(libs.datastore)

    implementation(projects.util)
    implementation(libs.kotlinx.serialization.json)

    testCompileOnly(platform(libs.compose.bom))
    testCompileOnly(libs.compose.runtime)
    testImplementation(libs.core.stubs.provider)
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.strikt)
    testImplementation(libs.kotlinx.coroutines)
    testImplementation(libs.kotlinx.coroutines.test)
    testRuntimeOnly(libs.junit.platform.launcher)
}

android {
    namespace = "com.flixclusive.provider.app.letterboxd"

    buildFeatures {
        buildConfig = true
    }

    // `local.properties` is git-ignored, so the PR workflow builds without it.
    defaultConfig {
        val properties = Properties()
        val localProperties = project.rootProject.file("local.properties")
        if (localProperties.exists()) {
            properties.load(localProperties.inputStream())
        }

        buildConfigField(
            "String",
            "LETTERBOXD_CLIENT_ID",
            "\"${properties["LETTERBOXD_CLIENT_ID"] ?: ""}\"",
        )
        buildConfigField(
            "String",
            "LETTERBOXD_CLIENT_SECRET",
            "\"${properties["LETTERBOXD_CLIENT_SECRET"] ?: ""}\"",
        )
    }

    testOptions {
        unitTests.all { it.useJUnitPlatform() }
    }
}

flxProvider {
    id = "flx-letterboxd-e53cef3c"

    adult = false
    providerName = "Letterboxd"
    description = """
        Browse Letterboxd film charts and lists, and sync your watchlist and diary.

        Letterboxd is films only and serves no streams — pair it with a source provider
        for playback.
    """.trimIndent()

    changelog = """
        ## 0.1.0
        - Initial release
        - Browse popular, highest rated and recently released films
        - Search films and open full details
        - Cross-match films from other providers via TMDB and IMDB ids
        - Sync your watchlist, and optionally log finished films to your diary
    """.trimIndent()

    versionMajor = 0
    versionMinor = 1
    versionPatch = 0
    versionBuild = 0

    iconUrl = "https://i.imgur.com/Gkc41xq.png"

    language = Language("en")
    providerType = ProviderType("Metadata, Tracker, Discovery")
    status = ProviderStatus.Beta

    requiresResources = false

    author(
        name = "rhenwinch",
        socialLink = "https://github.com/rhenwinch",
        image = "https://github.com/rhenwinch.png",
    )
}

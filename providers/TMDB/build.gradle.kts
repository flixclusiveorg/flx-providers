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
    compileOnly(libs.compose.activity)
    compileOnly(libs.coil.compose)
    compileOnly(libs.okhttp)
    compileOnly(libs.jsoup)
    compileOnly(libs.kotlinx.coroutines)
    compileOnly(libs.datastore)
    implementation(projects.util)
    implementation(libs.kotlinx.serialization.json)
}

android {
    namespace = "com.flixclusive.provider.app.tmdb"

    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        val properties = Properties()
        properties.load(project.rootProject.file("local.properties").inputStream())
        buildConfigField("String", "TMDB_API_KEY", "\"${properties["TMDB_API_KEY"]}\"")
    }
}

flxProvider {
    id = "flx-tmdb-8a3f2d1c"

    providerName = "TMDB"

    description = """
        A provider for The Movie Database (TMDB), offering catalog browsing, content
        discovery, metadata, search, and watch-provider links.
        
        Fixes:
        - [x] Resolve null TMDB API key issue
    """.trimIndent()

    versionMajor = 1
    versionMinor = 0
    versionPatch = 5
    versionBuild = 1

    iconUrl = "https://i.imgur.com/qd6zqII.png"
    language = Language.Multiple
    providerType = ProviderType("TMDB Content")
    status = ProviderStatus.Beta
    requiresResources = true

    author(
        name = "rhenwinch",
        socialLink = "https://github.com/rhenwinch",
        image = "https://github.com/rhenwinch.png",
    )
}

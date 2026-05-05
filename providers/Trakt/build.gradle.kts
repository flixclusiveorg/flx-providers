import com.flixclusive.model.provider.Language
import com.flixclusive.model.provider.ProviderType
import com.flixclusive.model.provider.Status
import org.jetbrains.kotlin.konan.properties.Properties

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
    namespace = "com.flixclusive.provider.app.trakt"

    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        val properties = Properties()
        properties.load(project.rootProject.file("local.properties").inputStream())

        buildConfigField("String", "TRAKT_TEST_AUTH_KEY", "\"${properties["TRAKT_TEST_AUTH_KEY"]}\"")
        buildConfigField("String", "TRAKT_CLIENT_SECRET", "\"${properties["TRAKT_CLIENT_SECRET"]}\"")
        buildConfigField("String", "TRAKT_CLIENT_ID", "\"${properties["TRAKT_CLIENT_ID"]}\"")
    }
}

flxProvider {
    id = "prov-trakt-53f7f73e-c4f8"

    adult = false
    providerName = "Trakt"
    description = "Trakt tracker and discovery provider."

    versionMajor = 0
    versionMinor = 0
    versionPatch = 1
    versionBuild = 0

    iconUrl = "https://i.imgur.com/cwmhW7c.png"

    language = Language.Multiple
    providerType = ProviderType("Metadata, Tracker, Discovery")
    status = Status.Beta

    requiresResources = true
}

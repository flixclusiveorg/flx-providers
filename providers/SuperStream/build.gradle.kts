import com.flixclusive.gradle.entities.Language
import com.flixclusive.gradle.entities.ProviderType
import com.flixclusive.gradle.entities.Status
import org.jetbrains.kotlin.konan.properties.Properties

android {
    defaultConfig {
        val properties = Properties()
        properties.load(project.rootProject.file("local.properties").inputStream())

        buildConfigField("String", "SUPERSTREAM_FIRST_API", "\"${properties.getProperty("SUPERSTREAM_FIRST_API")}\"")
        buildConfigField("String", "SUPERSTREAM_SECOND_API", "\"${properties.getProperty("SUPERSTREAM_SECOND_API")}\"")
        buildConfigField("String", "SUPERSTREAM_THIRD_API", "\"${properties.getProperty("SUPERSTREAM_THIRD_API")}\"")
        buildConfigField("String", "SUPERSTREAM_FOURTH_API", "\"${properties.getProperty("SUPERSTREAM_FOURTH_API")}\"")
    }
}

flxProvider {
    description.set("""
        NOTICE: This provider doesn't work sometimes, idk why.
        
        A classic streaming service with a large library of movies and TV shows, some even in 4K. Majority of the content included on this provider offers non-HLS streaming.
    """.trimIndent())

    changelog.set("""
        # v1.2.1-b3
        
        ### 🔧 Changes:
        - Fix search method not paginating
    """.trimIndent())

    versionMajor = 1
    versionMinor = 2
    versionPatch = 1
    versionBuild = 3

    iconUrl.set("https://i.imgur.com/KgMakl9.png") // OPTIONAL

    language.set(Language.Multiple)

    providerType.set(ProviderType.All)

    status.set(Status.Working)
}


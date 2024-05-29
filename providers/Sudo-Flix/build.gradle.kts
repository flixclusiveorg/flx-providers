import com.flixclusive.gradle.entities.Language
import com.flixclusive.gradle.entities.ProviderType
import com.flixclusive.gradle.entities.Status

flixclusive {
    description.set("""
        A forked clone of the old movie-web.
    """.trimIndent())

    versionMajor = 1
    versionMinor = 0
    versionPatch = 0
    versionBuild = 0

    // Extra authors for specific provider
     author(
        name = "sussy-code",
        githubLink = "https://github.com/sussy-code",
     )
    // ===

    iconUrl.set("https://i.imgur.com/dBgb2CR.png") // OPTIONAL

    language.set(Language.Multiple)

    providerType.set(ProviderType.All)

    status.set(Status.Working)
}


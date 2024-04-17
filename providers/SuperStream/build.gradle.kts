import com.flixclusive.gradle.entities.Language
import com.flixclusive.gradle.entities.ProviderType
import com.flixclusive.gradle.entities.Status

flixclusive {
    description.set("""
        NOTICE: This provider doesn't work sometimes, idk why.\n\n
        
        A classic streaming service with a large library of movies and TV shows, some even in 4K. Majority of the content included on this provider offers non-HLS streaming.
    """.trimIndent())

    versionMajor = 1
    versionMinor = 0
    versionPatch = 0
    versionBuild = 0

    // Extra authors for specific provider
    // author(
    //    name = "...",
    //    githubLink = "https://github.com/...",
    // )
    // ===

    iconUrl.set("https://i.imgur.com/KgMakl9.png") // OPTIONAL

    language.set(Language.Multiple)

    providerType.set(ProviderType.All)

    status.set(Status.Beta)
}


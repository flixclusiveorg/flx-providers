import com.flixclusive.gradle.entities.Language
import com.flixclusive.gradle.entities.ProviderType
import com.flixclusive.gradle.entities.Status

flixclusive {
    description.set("Stream seamlessly in adjustable HD resolution (1080p) with blazing-fast loading speeds. Offers HLS media.")

    versionMajor = 1
    versionMinor = 0
    versionPatch = 0
    versionBuild = 0

    // Extra authors
    author(
        name = "rhenwinch",
        githubLink = "https://github.com/rhenwinch",
    )
    // ===

    iconUrl.set("https://i.imgur.com/LNtqPTi.png")

    language.set(Language.Multiple)

    providerType.set(ProviderType.All)

    status.set(Status.Beta)
}


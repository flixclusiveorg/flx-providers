import com.flixclusive.gradle.entities.Language
import com.flixclusive.gradle.entities.ProviderType
import com.flixclusive.gradle.entities.Status

flxProvider {
    description.set("""
        A forked clone of the old movie-web.
        
        This is a set of providers. All source code references belong to sudo-flix.
    """.trimIndent())

    changelog.set("""
        # v1.3.2
        
        ### ✨ New:
        - Added VidBinge provider
        
        ### 🔧 Changes:
        - Fix NSBX (again)
        - Fix CloseLoad
    """.trimIndent())

    versionMajor = 1
    versionMinor = 3
    versionPatch = 2
    versionBuild = 2

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


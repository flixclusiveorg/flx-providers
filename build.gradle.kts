import com.flixclusive.gradle.FlixclusiveExtension
import com.android.build.gradle.BaseExtension


buildscript {
    configurations.getByName("classpath") {
        // Change this to `...(0, TimeUnit.SECONDS)`
        // to force update the providers-gradle dependency. Then, just return it
        // back to its default value `...(6, TimeUnit.HOURS)`
        resolutionStrategy.cacheChangingModulesFor(6, TimeUnit.HOURS)
    }

    repositories {
        google()
        mavenCentral()
        // Shitpack which still contains some Flixclusive dependencies for now.
        maven("https://jitpack.io")
        mavenLocal() // <- For testing
    }

    dependencies {
        classpath("com.android.tools.build:gradle:7.2.2")
        // Flixclusive gradle plugin which makes everything work and builds providers
        classpath("com.github.Flixclusive.providers-gradle:providers-gradle:main-SNAPSHOT") {
            isChanging = true
        }
        // Kotlin support. Remove if you want to use Java
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.10")
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
}

fun Project.flixclusive(configuration: FlixclusiveExtension.() -> Unit) = extensions.getByName<FlixclusiveExtension>("flixclusive").configuration()

fun Project.android(configuration: BaseExtension.() -> Unit) = extensions.getByName<BaseExtension>("android").configuration()

subprojects {
    apply(plugin = "com.android.library")
    apply(plugin = "com.flixclusive.gradle")
    // Remove if using Java
    apply(plugin = "kotlin-android")

    // Fill out with your info
    flixclusive {
        /**
         *
         * Add the author(s) of this repository.
         *
         * Optionally, you can add your
         * own github profile link
         * */
        author(
            name = "MyUsername",
            // githubLink = "http://github.com/myGithubUsername",
        )
        // author( ... )
        // author( ... )

        setRepository("https://github.com/your_username/myRepository")
    }

    android {
        compileSdkVersion(34)

        defaultConfig {
            minSdk = 21
            targetSdk = 34
        }

        compileOptions {
            isCoreLibraryDesugaringEnabled = true

            sourceCompatibility = JavaVersion.VERSION_17
            targetCompatibility = JavaVersion.VERSION_17
        }

        tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
            kotlinOptions {
                jvmTarget = JavaVersion.VERSION_17.toString() // Required
            }
        }
    }

    dependencies {
        val flixclusive by configurations
        val implementation by configurations
        val testImplementation by configurations
        val coreLibraryDesugaring by configurations

        coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")

        // Stubs for all Flixclusive classes
        flixclusive("com.flixclusive:flixclusive:pre-release") {
            isChanging = true
        }

        // ============= START: SCRAPING TOOLS =============
        val okHttpBom = platform("com.squareup.okhttp3:okhttp-bom:4.12.0")
        implementation(okHttpBom)
        // define any required OkHttp artifacts without version
        implementation("com.squareup.okhttp3:okhttp")
        implementation("com.squareup.okhttp3:okhttp-dnsoverhttps")
        implementation("com.squareup.okhttp3:logging-interceptor")


        implementation("org.jsoup:jsoup:1.16.1")
        implementation("com.google.code.gson:gson:2.10.1")
        // ============== END: SCRAPING TOOLS =============

        // ============= START: FOR TESTING ===============
        testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
        testImplementation("junit:junit:4.13.2")
        testImplementation("io.mockk:mockk:1.13.8")
        // ============== END: FOR TESTING ================
    }
}

task<Delete>("clean") {
    delete(rootProject.buildDir)
}

import com.android.build.api.dsl.LibraryExtension
import com.lagradost.cloudstream3.gradle.CloudstreamExtension
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.kotlin.dsl.register
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

buildscript {
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }

    dependencies {
        classpath("com.android.tools.build:gradle:9.1.1")
        classpath("com.github.recloudstream.gradle:gradle:81b1d424d")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.4.0")
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
}

fun Project.cloudstream(
    configuration: CloudstreamExtension.() -> Unit
) = extensions
    .getByName<CloudstreamExtension>("cloudstream")
    .configuration()

fun Project.android(
    configuration: LibraryExtension.() -> Unit
) {
    extensions.getByName<LibraryExtension>("android").apply {
        project.extensions
            .findByType(JavaPluginExtension::class.java)
            ?.apply {
                toolchain {
                    languageVersion.set(JavaLanguageVersion.of(17))
                }
            }

        configuration()
    }
}

subprojects {
    apply(plugin = "com.android.library")
    apply(plugin = "com.lagradost.cloudstream3.gradle")

    cloudstream {
        setRepo(
            System.getenv("GITHUB_REPOSITORY")
                ?: "https://github.com/michat88/free_repo"
        )

        authors = listOf("trinityzanetamanu")
    }

    android {
        namespace = "com.trinityzanetamanu"

        compileSdk = 36

        defaultConfig {
            minSdk = 21
        }

        lint {
            targetSdk = 36
        }

        /*
         * CloudStream pre-release terbaru menggunakan JVM target 11.
         *
         * Karena itu Java dan Kotlin provider juga harus menghasilkan
         * bytecode JVM 11 agar fungsi inline dari cloudstream.jar
         * dapat dikompilasi tanpa error:
         *
         * Cannot inline bytecode built with JVM target 11 into
         * bytecode that is being built with JVM target 1.8
         */
        compileOptions {
            sourceCompatibility = JavaVersion.VERSION_11
            targetCompatibility = JavaVersion.VERSION_11
        }

        //noinspection WrongGradleMethod
        tasks.withType<KotlinJvmCompile> {
            compilerOptions {
                jvmTarget.set(JvmTarget.JVM_11)

                freeCompilerArgs.addAll(
                    "-Xno-call-assertions",
                    "-Xno-param-assertions",
                    "-Xno-receiver-assertions",
                )
            }
        }
    }

    dependencies {
        val implementation by configurations
        val cloudstream by configurations

        cloudstream("com.lagradost:cloudstream3:pre-release")

        // Fix for Kotlin 2.4.0 strict type annotation checking
        implementation("org.jspecify:jspecify:1.0.0")

        // Kotlin
        implementation(kotlin("stdlib"))

        // Networking
        implementation("com.github.Blatzar:NiceHttp:0.4.18")

        // HTML parser
        implementation("org.jsoup:jsoup:1.22.2")

        // Android annotations
        implementation("androidx.annotation:annotation:1.10.0")

        // Do not bump above 2.13.1
        implementation(
            "com.fasterxml.jackson.module:jackson-module-kotlin:2.13.1"
        )
        implementation(
            "com.fasterxml.jackson.core:jackson-databind:2.13.1"
        )

        // Coroutines
        implementation(
            "org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2"
        )

        // Do not bump above 1.8.1
        implementation("org.mozilla:rhino:1.8.1")

        // Fuzzy matching
        implementation("me.xdrop:fuzzywuzzy:1.4.0")

        // JSON
        implementation("com.google.code.gson:gson:2.14.0")
        implementation(
            "org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0"
        )

        // Crypto
        implementation("org.bouncycastle:bcpkix-jdk18on:1.84")
    }
}

tasks.register<Delete>("clean") {
    delete(rootProject.layout.buildDirectory)
}

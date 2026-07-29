plugins {
    kotlin("jvm") version "2.2.20"
    `maven-publish`
}

group = "dev.bee"
version = "0.1.0"

// bee-fsrs is deliberately dependency-free apart from the Kotlin stdlib. It is pure
// memory mathematics with no clock, no storage, and no logging, so any consumer can
// pin it as the same tested artifact without inheriting a transitive dependency.
//
// The extraction rules that keep it that way are in PROVENANCE.md, and the reference
// fixture in testdata/ is the oracle that catches silent drift in the mathematics.

repositories {
    mavenCentral()
}

kotlin {
    jvmToolchain(17)
    compilerOptions {
        allWarningsAsErrors.set(true)
        // Default methods on interfaces, so a Java consumer sees the same API surface
        // a Kotlin one does.
        freeCompilerArgs.add("-Xjvm-default=all")
    }
}

dependencies {
    api(kotlin("stdlib"))
    testImplementation("junit:junit:4.13.2")
}

tasks.test {
    useJUnit()
    testLogging {
        events("passed", "failed")
    }
}

java {
    withSourcesJar()
}

publishing {
    // GitHub Packages, so an outside consumer can resolve `dev.bee:bee-fsrs` by
    // coordinate instead of vendoring the sources. That is the part of BeeCode's M0
    // gate the clean-consumer smoke build cannot demonstrate on its own: it
    // substitutes the sibling project, which proves the API is self-sufficient but
    // not that the artifact is actually fetchable.
    //
    // Credentials come from the environment so a fork can publish to its own package
    // registry without editing this file, and `publishToMavenLocal` keeps working with
    // no credentials at all.
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri(
                System.getenv("GITHUB_PACKAGES_URL")
                    ?: "https://maven.pkg.github.com/bee-san/bee-fsrs",
            )
            credentials {
                username = System.getenv("GITHUB_ACTOR")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }

    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            artifactId = "bee-fsrs"

            pom {
                name.set("bee-fsrs")
                description.set(
                    "FSRS-6.x spaced-repetition memory mathematics for the JVM. " +
                        "Dependency-free, clock-free, and deterministic.",
                )
                url.set("https://github.com/bee-san/bee-fsrs")
                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://opensource.org/licenses/MIT")
                    }
                }
                developers {
                    developer {
                        id.set("bee-san")
                        name.set("Autumn Skerritt")
                    }
                }
                scm {
                    url.set("https://github.com/bee-san/bee-fsrs")
                    connection.set("scm:git:https://github.com/bee-san/bee-fsrs.git")
                }
            }
        }
    }
}

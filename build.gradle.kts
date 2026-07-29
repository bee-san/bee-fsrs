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

/**
 * Prove the published artifact actually resolves, as part of `check`.
 *
 * The plan makes external consumption a gate, and a gate enforced only by a CI step is
 * weaker than it looks — nobody notices it was never added. This runs on every
 * `./gradlew check`, on a laptop as much as on a runner.
 *
 * `publishToMavenLocal` first, because the point of the build is to resolve a real
 * artifact from a repository rather than substitute a project. Making the ordering
 * explicit stops it failing confusingly on a fresh clone.
 *
 * `consumer-smoke` is deliberately *not* wired in the same way: it `includeBuild`s this
 * build, and Gradle does not support a composite build nested inside a `GradleBuild`
 * task ("Cannot include build ... This is not supported yet"). It stays a separate
 * invocation, which CI and the README both spell out.
 */
// Resolved out here: inside the configuration block below, `tasks` refers to
// GradleBuild's own list-of-task-names property, not the project's task container.
val publishLocally = tasks.named("publishToMavenLocal")

val artifactResolution by tasks.registering(GradleBuild::class) {
    group = "verification"
    description = "Resolves the published artifact by coordinate, with no substitution."
    dependsOn(publishLocally)
    dir = file("artifact-resolution")
    tasks = listOf("test")
}

tasks.named("check") {
    dependsOn(artifactResolution)
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

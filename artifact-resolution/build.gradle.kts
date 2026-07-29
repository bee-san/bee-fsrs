plugins {
    kotlin("jvm") version "2.2.20"
}

// Resolves the published artifact by coordinate, exactly as an unrelated project would.
//
// `mavenLocal()` first so this works offline immediately after
// `./gradlew publishToMavenLocal`, which is what CI does. Add the GitHub Packages
// repository here to verify a genuinely remote resolution; that needs credentials, so
// it is not the default path for a contributor running the suite.
repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    // A version, unlike consumer-smoke's substituted coordinate. Bump it with the
    // release: a stale version here means this gate is testing the previous artifact.
    implementation("dev.bee:bee-fsrs:0.2.0")
    testImplementation(kotlin("test"))
    testImplementation("junit:junit:4.13.2")
}

kotlin { jvmToolchain(17) }

tasks.test {
    useJUnit()
    testLogging { events("passed", "failed") }
}

plugins {
    kotlin("jvm") version "2.2.20"
}

// The clean-consumer smoke test.
//
// This is a **separate Gradle build** that resolves bee-fsrs the way an unrelated
// project would, rather than reaching into the parent's source sets. That distinction
// is the whole point: it proves the published artifact is self-sufficient, and it
// would fail if the engine ever grew a dependency it did not declare or leaked a type
// a consumer cannot see.
//
// The plan makes this a gate, not a nicety: the FSRS package is not considered
// reusable until an external consumer resolves the same pinned artifact and agrees on
// the same vectors.

repositories {
    mavenCentral()
}

dependencies {
    // Substituted by the composite build to the local project, and by a real consumer
    // to the published Maven coordinate. Identical either way from here.
    implementation("dev.bee:bee-fsrs")
    testImplementation(kotlin("test"))
    testImplementation("junit:junit:4.13.2")
}

kotlin {
    jvmToolchain(17)
}

tasks.test {
    useJUnit()
    testLogging { events("passed", "failed") }
}

rootProject.name = "artifact-resolution"

// Deliberately NOT a composite build.
//
// consumer-smoke/ includes the parent build and substitutes the coordinate, which
// proves the *API* is self-sufficient. This build does not: it resolves
// `dev.bee:bee-fsrs` from a real repository, so it also proves the *artifact*
// publishes correctly — that the POM is valid, the Kotlin stdlib dependency is
// declared, and the jar contains what a consumer needs.
//
// Those are different failures. A build that substitutes a project cannot notice a
// malformed POM or a missing transitive dependency, because Gradle never reads them.

rootProject.name = "consumer-smoke"

// Resolve `dev.bee:bee-fsrs` to the sibling build. A real consumer would resolve the
// same coordinate from a repository; substituting here means the smoke test exercises
// the published API surface without needing a publish step first.
includeBuild("..") {
    dependencySubstitution {
        substitute(module("dev.bee:bee-fsrs")).using(project(":"))
    }
}

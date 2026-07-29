rootProject.name = "bee-fsrs"

// The clean-consumer smoke test: a separate build that depends on the published
// artifact the way any external project would. Its existence is what proves the
// package is genuinely reusable rather than merely extracted.
includeBuild("consumer-smoke")

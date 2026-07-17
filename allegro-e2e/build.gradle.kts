// Live E2E layer: Java tests that drive BOTH the SDK and (via Playwright-Java,
// in-process) the buyer-side web UI for flows the REST API cannot reach
// (buy-now, disputes, device-flow consent). Manual/live-sandbox only, NOT
// published, and deliberately NOT wired into the aggregate `check` — the e2e
// tests need a real browser under Xvfb, the live sandbox, and credentials.
plugins {
    application
}

description = "Live E2E tests + buyer-side web automation (Playwright-Java); not published"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
    // Classpath, NOT the module path: Playwright is an automatic module and the
    // e2e layer is test tooling, not part of the published JPMS surface.
}

dependencies {
    implementation(project(":allegro-client"))
    implementation(libs.playwright)
    runtimeOnly(libs.slf4j.simple)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}

application {
    // One-time buyer-session bootstrap: log in once, persist storageState.
    mainClass.set("io.github.mgrtomaszzurawski.allegro.e2e.BuyerSessionBootstrap")
}

// E2E tests are opt-in: they need a browser + live sandbox, so they never run in
// the normal `check`/CI. Every test here is `@Tag("e2e")`; by default that tag is
// excluded (the task runs but matches nothing), so a plain `check` launches no
// browser. Opt in with `-Pe2e`. (A tag filter is config-cache safe; an `onlyIf`
// lambda in a .kts script is not — it captures the script object.)
val e2eEnabled = providers.gradleProperty("e2e").isPresent
tasks.named<Test>("test") {
    useJUnitPlatform {
        if (!e2eEnabled) {
            excludeTags("e2e")
        }
    }
    // Playwright E2E MUST run as a serial series with a rate limit: they share
    // one buyer session/IP, and concurrent or rapid browser traffic trips
    // DataDome's hard IP block. One JVM fork, no parallel test execution, and
    // one class per fork so sessions never overlap.
    maxParallelForks = 1
    forkEvery = 1
    systemProperty("junit.jupiter.execution.parallel.enabled", "false")
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(17)
    options.encoding = "UTF-8"
}

// Live-sandbox probe runner (manual execution against the Allegro sandbox).
// Placeholder in the bootstrap scaffold: a DemoApp driving real HTTP traffic
// lands with the transport/auth core PR. Not published.
plugins {
    application
}

description = "Live sandbox demo runner for the Allegro SDK (not published)"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
    modularity.inferModulePath.set(true)
}

dependencies {
    implementation(project(":allegro-client"))
    // Console logging backend so the SDK's named debug channels are visible
    // during live probes (enable with -Dorg.slf4j.simpleLogger.log.io.github
    // .mgrtomaszzurawski.allegro=debug).
    runtimeOnly(libs.slf4j.simple)
}

// Pass -Pdemo.scenario=<name> (and optional -Pdemo.account=seller|buyer)
// through to the runner.
tasks.named<JavaExec>("run") {
    providers.gradleProperty("demo.scenario").orNull?.let { args = listOf(it) }
    providers.gradleProperty("demo.account").orNull?.let { systemProperty("demo.account", it) }
    standardInput = System.`in`
}

application {
    // Placeholder main class — added with the first demo scenario.
    mainClass.set("io.github.mgrtomaszzurawski.allegro.demo.DemoApp")
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(17)
    options.encoding = "UTF-8"
}

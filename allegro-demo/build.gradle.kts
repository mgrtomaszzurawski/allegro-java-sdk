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

    // The shared token store is agent infrastructure that has caused real data
    // loss; it carries a regression test (no live traffic — pure filesystem).
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}

// Pass -Pdemo.scenario=<name> as the runner argument, and forward EVERY other
// -Pdemo.* gradle property as a system property so scenarios can read their
// parameters via System.getProperty (offerId, createName, publishIds, ...).
// gradlePropertiesPrefixedBy is config-cache safe.
val demoProperties = providers.gradlePropertiesPrefixedBy("demo.")
tasks.named<JavaExec>("run") {
    providers.gradleProperty("demo.scenario").orNull?.let { args = listOf(it) }
    demoProperties.get().forEach { (key, value) -> systemProperty(key, value) }
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

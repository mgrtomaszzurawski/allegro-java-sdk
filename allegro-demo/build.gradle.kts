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
}

application {
    // Placeholder main class — added with the first demo scenario.
    mainClass.set("io.github.mgrtomaszzurawski.allegro.demo.DemoApp")
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(17)
    options.encoding = "UTF-8"
}

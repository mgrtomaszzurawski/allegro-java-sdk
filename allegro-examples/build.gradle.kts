// Compile-only check of the consumer examples under the top-level examples/
// directory. Placeholder in the bootstrap scaffold; snippets are added as
// domains land so README code stays compiled. Not published.
plugins {
    `java-library`
}

description = "Compile-only consumer examples for the Allegro SDK (not published)"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
    modularity.inferModulePath.set(true)
}

dependencies {
    implementation(project(":allegro-client"))
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(17)
    options.encoding = "UTF-8"
}

plugins {
    `java-library`
}

description = """
    Tiny named module that imports the Allegro SDK as a real JPMS consumer.
    Its module-info requires io.github.mgrtomaszzurawski.allegro. If a public
    type returned by the SDK ever lives in a non-exported package, this module
    fails to compile — catching export-surface regressions a reflection test
    cannot. Not published to Maven Central.
""".trimIndent()

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

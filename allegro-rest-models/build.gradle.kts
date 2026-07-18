// OpenAPI-generated REST models (`*Raw` DTOs) for the Allegro REST API.
//
// Lives in its own module so the generation pass is cached as a JAR consumed
// by allegro-client; allegro-client only rebuilds these when the vendored
// openapi/allegro-openapi.yaml spec changes.
//
// Generation is MODELS-ONLY (globalProperties "models"): Layer 1 is pure
// `*Raw` POJOs. The HTTP transport, auth, and endpoint wiring are hand-written
// in allegro-client, so the generator's api-client/supporting files are not
// produced — nothing here but data-transfer types.

import com.vanniktech.maven.publish.JavaLibrary
import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.SonatypeHost

plugins {
    `java-library`
    id("org.openapi.generator") version "7.12.0"
    id("com.vanniktech.maven.publish")
}

description = "OpenAPI-generated REST models — companion to the unofficial allegro-client (preview)"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
    modularity.inferModulePath.set(true)
}

dependencies {
    api(libs.jackson.databind)
    api(libs.jackson.core)
    api(libs.jackson.annotations)
    api(libs.jackson.datatype.jsr310)
    api(libs.jackson.databind.nullable)
    api(libs.jakarta.annotation.api)
}

val specFile = layout.projectDirectory.file("openapi/allegro-openapi.yaml")

openApiGenerate {
    generatorName.set("java")
    // The `native` library serializes with Jackson (the okhttp-gson default
    // pulls in Gson type-adapters + a JSON support class). Models-only
    // generation keeps just the Jackson-annotated DTOs.
    library.set("native")
    inputSpec.set(specFile.asFile.absolutePath)
    outputDir.set(layout.buildDirectory.dir("generated-sources/openapi").map { it.asFile.absolutePath })
    modelPackage.set("io.github.mgrtomaszzurawski.allegro.client.model")
    invokerPackage.set("io.github.mgrtomaszzurawski.allegro.client")
    modelNameSuffix.set("Raw")
    // Models + supporting files, but NO api-client classes: Layer 1 is the
    // `*Raw` DTOs. Composed (oneOf/anyOf) DTOs extend AbstractOpenApiSchema and
    // use the JSON support class, so the supporting files must come along; the
    // per-endpoint `*Api` classes are hand-written in allegro-client and are
    // not generated here.
    globalProperties.set(mapOf("models" to "", "supportingFiles" to ""))
    // Allegro's public spec carries vendor media types and extensions that a
    // strict validation pass rejects; we want the DTOs regardless. Generation
    // still fails loudly on a structurally broken spec.
    validateSpec.set(false)
    generateApiTests.set(false)
    generateModelTests.set(false)
    generateApiDocumentation.set(false)
    generateModelDocumentation.set(false)
    skipOverwrite.set(false)
    configOptions.set(mapOf(
        "dateLibrary" to "java8",
        "useJakartaEe" to "true",
        "openApiNullable" to "true",
        "hideGenerationTimestamp" to "true",
        // The `java` generator defaults to the okhttp-gson library, which
        // emits Gson type-adapters and a JSON support class into every model.
        // Layer 1 is Jackson-serialized DTOs — force Jackson so no Gson symbol
        // is referenced (models-only generation omits the support classes).
        "serializationLibrary" to "jackson",
        // Forward compatibility: a generated enum's fromValue() returns the
        // UNKNOWN_DEFAULT_OPEN_API sentinel for an unrecognized wire value
        // instead of throwing, so a value Allegro adds later does not fail the
        // whole response deserialization. Domain enums map that sentinel (and
        // any unmodelled value) to their own UNKNOWN via a switch default.
        "enumUnknownDefaultCase" to "true",
    ))
}

// Cache the generator output across `clean build` runs — keyed on the spec file
// + generator config. doFirst clears stale generated files before each run so a
// removed schema definition does not leave orphan *Raw classes behind.
tasks.named("openApiGenerate") {
    inputs.file(specFile).withPathSensitivity(PathSensitivity.RELATIVE)
    val outDir = layout.buildDirectory.dir("generated-sources/openapi")
    outputs.dir(outDir)
    outputs.cacheIf { true }
    doFirst {
        outDir.get().asFile.also {
            it.deleteRecursively()
            it.mkdirs()
        }
    }
}

sourceSets.main {
    java.srcDirs(layout.buildDirectory.dir("generated-sources/openapi/src/main/java"))
}

tasks.named("compileJava") {
    dependsOn("openApiGenerate")
}

tasks.matching { it.name == "sourcesJar" }.configureEach {
    dependsOn("openApiGenerate")
}
tasks.named("javadoc") {
    dependsOn("openApiGenerate")
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(17)
    options.encoding = "UTF-8"
    options.compilerArgs.addAll(listOf("-nowarn", "-Xlint:none"))
}

// Generated *Raw sources may contain non-ASCII (Polish field-name comments).
// Force UTF-8 + silence the lint warnings on generated code.
tasks.withType<Javadoc>().configureEach {
    options.encoding = "UTF-8"
    (options as StandardJavadocDocletOptions).addStringOption("Xdoclint:none", "-quiet")
}

// ---------- Maven Central publication (companion to allegro-client) ----------

if (providers.gradleProperty("signingEnabled").orNull == "true") {
    apply(plugin = "signing")
    extensions.configure<SigningExtension> { useGpgCmd() }
}

mavenPublishing {
    configure(JavaLibrary(javadocJar = JavadocJar.Javadoc(), sourcesJar = true))
    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL, automaticRelease = false)
    if (providers.gradleProperty("signingEnabled").orNull == "true") {
        signAllPublications()
    }

    coordinates(
        groupId = "io.github.mgrtomaszzurawski",
        artifactId = "allegro-rest-models",
        version = project.version.toString()
    )

    pom {
        name.set("Allegro REST Models")
        description.set(project.description)
        url.set("https://github.com/mgrtomaszzurawski/allegro-java-sdk")
        licenses {
            license {
                name.set("GNU Affero General Public License v3.0")
                url.set("https://www.gnu.org/licenses/agpl-3.0.html")
            }
        }
        developers {
            developer {
                id.set("mgrtomaszzurawski")
                name.set("Tomasz Zurawski")
                email.set("mgrtomaszzurawski@gmail.com")
            }
        }
        scm {
            connection.set("scm:git:https://github.com/mgrtomaszzurawski/allegro-java-sdk.git")
            developerConnection.set("scm:git:ssh://github.com/mgrtomaszzurawski/allegro-java-sdk.git")
            url.set("https://github.com/mgrtomaszzurawski/allegro-java-sdk")
        }
    }
}

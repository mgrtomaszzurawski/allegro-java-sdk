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

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator
import com.vanniktech.maven.publish.JavaLibrary
import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.SonatypeHost

buildscript {
    dependencies {
        // YAML tree access for the spec-preprocessing task below. The vendored
        // OpenAPI spec is never edited; the patch runs on a build/ copy.
        classpath("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.18.2")
    }
}

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

// ---------- Generation-time spec normalization (vendored spec untouched) ----------
//
// Some Allegro schemas declare a discriminator whose mapping targets are NOT
// linked back to the parent with `allOf` (the spec relies on the discriminator
// mapping alone). The OpenAPI generator only sets up Java inheritance when a
// subtype `allOf`-references its parent, so for those schemas the mapped
// subtypes generate as STANDALONE classes and the parent field cannot carry a
// concrete subtype instance — the value-bearing fields become unreachable.
//
// `OfferBulkModification.stock` is such a schema: its `FIXED`/`GAIN` subtypes
// (`StockModification{Fixed,Gain}`, each carrying `value`) are standalone, so
// the generated `stock` field (typed to the changeType-only base) can never
// hold a stock value. The price side of the same request has the identical
// discriminator but its subtypes DO `allOf`-reference the parent, so it works.
//
// The vendored spec must never appear in a diff, so this runs on a build/ copy:
// promote the inline `stock` schema to a named component and rewrite each mapped
// subtype into `allOf: [ {$ref parent}, {original} ]`, mirroring the price side.
val patchedSpecFile = layout.buildDirectory.file("patched-openapi/allegro-openapi.yaml")

// One discriminator-only schema to normalize: the inline object under
// `owner.property` is promoted to `parentName`, and each mapped subtype is
// re-parented under it via allOf.
data class StockLikeFix(
    val owner: String,
    val property: String,
    val parentName: String,
    val subtypes: List<String>,
)

val patchOpenApiSpec = tasks.register("patchOpenApiSpec") {
    val source = specFile
    val target = patchedSpecFile
    inputs.file(source)
    outputs.file(target)
    doLast {
        val schemaRefPrefix = "#/components/schemas/"
        val mapper = ObjectMapper(
            YAMLFactory().enable(YAMLGenerator.Feature.MINIMIZE_QUOTES)
        )
        val root = mapper.readTree(source.asFile) as ObjectNode
        val schemas = root.path("components").path("schemas") as ObjectNode

        // Data-driven: (owning schema, inline object property, promoted component,
        // discriminator-mapped subtypes) — one entry per discriminator-only schema.
        val fixes = listOf(
            StockLikeFix(
                owner = "OfferBulkModification",
                property = "stock",
                parentName = "OfferBulkModificationStock",
                subtypes = listOf("StockModificationFixed", "StockModificationGain"),
            ),
        )
        for (fixEntry in fixes) {
            if (schemas.has(fixEntry.parentName)) {
                continue // already normalized (idempotent across incremental runs)
            }
            val parentRef = schemaRefPrefix + fixEntry.parentName
            val owner = schemas.get(fixEntry.owner) as ObjectNode
            val ownerProps = owner.get("properties") as ObjectNode
            val inline = ownerProps.get(fixEntry.property) as ObjectNode

            // 1. Promote the inline object (keeps its discriminator + mapping) to a named component.
            schemas.set<JsonNode>(fixEntry.parentName, inline.deepCopy())
            // 2. Replace the inline with a $ref to that component.
            ownerProps.set<JsonNode>(fixEntry.property, mapper.createObjectNode().put("\$ref", parentRef))
            // 3. Wrap each mapped subtype so it allOf-references the parent (Java inheritance).
            for (subName in fixEntry.subtypes) {
                val original = schemas.get(subName) as ObjectNode
                val wrapped = mapper.createObjectNode()
                val allOf = wrapped.putArray("allOf")
                allOf.add(mapper.createObjectNode().put("\$ref", parentRef))
                allOf.add(original.deepCopy())
                schemas.set<JsonNode>(subName, wrapped)
            }
            logger.lifecycle("patchOpenApiSpec: linked ${fixEntry.subtypes} under ${fixEntry.parentName}")
        }

        target.get().asFile.also { it.parentFile.mkdirs() }
        mapper.writeValue(target.get().asFile, root)
    }
}

openApiGenerate {
    generatorName.set("java")
    // The `native` library serializes with Jackson (the okhttp-gson default
    // pulls in Gson type-adapters + a JSON support class). Models-only
    // generation keeps just the Jackson-annotated DTOs.
    library.set("native")
    // Consume the normalized copy (see patchOpenApiSpec); the vendored spec stays untouched.
    inputSpec.set(patchedSpecFile.map { it.asFile.absolutePath })
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
    dependsOn(patchOpenApiSpec)
    inputs.file(patchedSpecFile).withPathSensitivity(PathSensitivity.RELATIVE)
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

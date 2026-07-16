import com.vanniktech.maven.publish.JavaLibrary
import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.SonatypeHost

plugins {
    `java-library`
    jacoco
    checkstyle
    pmd
    id("com.github.spotbugs") version "6.0.26"
    id("com.diffplug.spotless") version "6.25.0"
    id("com.vanniktech.maven.publish")
}

description = "Unofficial typed Java SDK for the Allegro REST API (preview)"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
    modularity.inferModulePath.set(true)
}

dependencies {
    // Generated REST models — *Raw DTOs are internal (Layer 1); a public-API
    // surface test will enforce zero leakage into the exported surface.
    implementation(project(":allegro-rest-models"))

    // Jackson — used internally to map *Raw DTOs to immutable domain records.
    // implementation scope keeps it off consumers' compile classpath, matching
    // the plain (non-transitive) `requires` in module-info.java.
    implementation(libs.jackson.databind)
    implementation(libs.jackson.datatype.jsr310)

    // Logging — internal facade only; no SLF4J type appears on the public
    // surface, so implementation scope. Consumers bind their own backend.
    implementation(libs.slf4j.api)

    // Null-safety annotations (JSpecify) — compile-time only.
    compileOnly(libs.jspecify)
    testCompileOnly(libs.jspecify)

    // apiguardian @API EXPERIMENTAL marker is RUNTIME-retained on the exported
    // AllegroClient — api scope + `requires static transitive` in module-info
    // so consumers can read the annotation without declaring it themselves.
    api(libs.apiguardian.api)

    // Test
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.wiremock.standalone)
    testImplementation(libs.mockito.core)
    testRuntimeOnly(libs.junit.platform.launcher)
}

// Bundle license + third-party notices into the published JAR (AGPL 5(a)).
tasks.named<ProcessResources>("processResources") {
    from(layout.projectDirectory.dir("..")) {
        into("META-INF")
        include("LICENSE.txt", "THIRD-PARTY-NOTICES.md")
    }
}

// Implementation-Version drives AllegroClient.sdkVersion() — the version is
// maintained ONLY in gradle.properties and flows here via project.version.
tasks.jar {
    manifest {
        attributes(
            "Implementation-Title" to "allegro-client",
            "Implementation-Version" to project.version.toString(),
        )
    }
}

// ---------- Compile + test ----------

tasks.withType<JavaCompile>().configureEach {
    options.release.set(17)
    options.encoding = "UTF-8"
}

// Stamp the module descriptor with the project version so module-path consumers
// (the flagship JPMS mode) can read it — Package.getImplementationVersion()
// returns null for named modules, so the manifest alone is not enough.
tasks.compileJava {
    options.javaModuleVersion.set(provider { project.version.toString() })
}

tasks.test {
    useJUnitPlatform()
    finalizedBy(tasks.jacocoTestReport)
}

// ---------- License headers ----------

spotless {
    java {
        target("src/main/java/**/*.java", "src/test/java/**/*.java")
        licenseHeader(
            """
            /*
             * Copyright (c) 2026 Tomasz Zurawski
             * SPDX-License-Identifier: AGPL-3.0-only
             */
            """.trimIndent()
        )
    }
}

// ---------- Static analysis ----------

// Static gates analyse main source only — test files use a different style.
checkstyle {
    toolVersion = "10.20.1"
    configFile = rootProject.file("checkstyle.xml")
    sourceSets = listOf(project.sourceSets.main.get())
}

tasks.withType<Checkstyle>().configureEach {
    exclude("**/module-info.java")
}

// PMD 7.7 cannot parse some JLS-valid module directives (`requires static
// transitive`); module descriptors carry no logic worth analysing anyway.
tasks.withType<Pmd>().configureEach {
    exclude("**/module-info.java")
}

pmd {
    toolVersion = "7.7.0"
    ruleSetFiles = files(rootProject.file("pmd-ruleset.xml"))
    ruleSets = emptyList()
    isIgnoreFailures = false
}

tasks.named("pmdTest") { enabled = false }
tasks.named("spotbugsTest") { enabled = false }

spotbugs {
    toolVersion.set("4.8.6")
    excludeFilter.set(rootProject.file("spotbugs-exclude.xml"))
    onlyAnalyze.set(listOf("io.github.mgrtomaszzurawski.allegro.sdk.-"))
}

// ---------- JaCoCo coverage ----------
//
// The coverage GATE (bundle INSTRUCTION >= 0.75 / METHOD >= 0.80 floor +
// per-class METHOD = 1.00 on every sdk.domain.*.builder.*Builder and
// sdk.domain.*.*Client) is enabled with the first domain PR — an empty
// bootstrap skeleton has no methods to cover, so wiring the ratchet now would
// fail the reactor on 0%. The report task stays wired so Sonar has input.
// The verification rules are staged in the block below (currently not part of
// `check`) and get attached to `check` when domains land — see BACKLOG.md.

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}

tasks.jacocoTestCoverageVerification {
    dependsOn(tasks.test)
    violationRules {
        rule {
            element = "BUNDLE"
            limit {
                counter = "INSTRUCTION"
                value = "COVEREDRATIO"
                minimum = "0.75".toBigDecimal()
            }
            limit {
                counter = "METHOD"
                value = "COVEREDRATIO"
                minimum = "0.80".toBigDecimal()
            }
        }
        rule {
            element = "CLASS"
            includes = listOf(
                "io.github.mgrtomaszzurawski.allegro.sdk.domain.*.builder.*Builder",
                "io.github.mgrtomaszzurawski.allegro.sdk.domain.*.builder.*Builder\$*",
                "io.github.mgrtomaszzurawski.allegro.sdk.domain.*.*Client",
            )
            limit {
                counter = "METHOD"
                value = "COVEREDRATIO"
                minimum = "1.00".toBigDecimal()
            }
        }
    }
}

// ---------- Javadoc ----------

tasks.javadoc {
    options.encoding = "UTF-8"
    (options as StandardJavadocDocletOptions).apply {
        encoding = "UTF-8"
        charSet = "UTF-8"
        docEncoding = "UTF-8"
        group("Entry point", "io.github.mgrtomaszzurawski.allegro.sdk")
        group("Configuration", "io.github.mgrtomaszzurawski.allegro.sdk.config*")
        group(
            "Common types",
            "io.github.mgrtomaszzurawski.allegro.sdk.core:io.github.mgrtomaszzurawski.allegro.sdk.exception"
        )
        group("Operational domain APIs", "io.github.mgrtomaszzurawski.allegro.sdk.domain*")
        tags(
            "apiNote:a:API Note:",
            "implSpec:a:Implementation Requirements:",
            "implNote:a:Implementation Note:",
        )
        // Full doclint except `missing` — syntax/reference/html errors in the
        // hand-written published surface must fail the build (broken docs would
        // otherwise ship silently); missing @param/@return tags stay non-fatal
        // because names often carry the meaning (see CLAUDE.md javadoc rule).
        addStringOption("Xdoclint:all,-missing", "-quiet")
    }
    isFailOnError = true
}

// ---------- Maven Central publication ----------

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
        artifactId = "allegro-client",
        version = project.version.toString()
    )

    pom {
        name.set("Allegro Client")
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

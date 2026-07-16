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

val slf4jVersion = "2.0.16"
val jspecifyVersion = "1.0.0"
val apiguardianVersion = "1.1.2"
val jacksonVersion = "2.18.2"
val junitVersion = "5.11.4"
val wiremockVersion = "3.12.1"
val mockitoVersion = "5.14.2"

dependencies {
    // Generated REST models — *Raw DTOs are internal (Layer 1); a public-API
    // surface test will enforce zero leakage into the exported surface.
    implementation(project(":allegro-rest-models"))

    // Jackson — the SDK maps *Raw DTOs to immutable domain records and needs
    // the mapper directly. api so the transitive DTO deps resolve for tests.
    api("com.fasterxml.jackson.core:jackson-databind:$jacksonVersion")
    api("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:$jacksonVersion")

    // Logging — api so consumers configure their own SLF4J backend without
    // re-declaring the API dependency.
    api("org.slf4j:slf4j-api:$slf4jVersion")

    // Null-safety annotations (JSpecify) — compile-time only.
    compileOnly("org.jspecify:jspecify:$jspecifyVersion")
    testCompileOnly("org.jspecify:jspecify:$jspecifyVersion")

    // apiguardian @API EXPERIMENTAL marker on AllegroClient — preview signal.
    api("org.apiguardian:apiguardian-api:$apiguardianVersion")

    // Test
    testImplementation(platform("org.junit:junit-bom:$junitVersion"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.wiremock:wiremock-standalone:$wiremockVersion")
    testImplementation("org.mockito:mockito-core:$mockitoVersion")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

// Bundle license + third-party notices into the published JAR (AGPL 5(a)).
tasks.named<ProcessResources>("processResources") {
    from(layout.projectDirectory.dir("..")) {
        into("META-INF")
        include("LICENSE.txt", "THIRD-PARTY-NOTICES.md")
    }
}

// ---------- Compile + test ----------

tasks.withType<JavaCompile>().configureEach {
    options.release.set(17)
    options.encoding = "UTF-8"
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

checkstyle {
    toolVersion = "10.20.1"
    configFile = rootProject.file("checkstyle.xml")
    sourceSets = listOf(
        project.sourceSets.main.get(),
        project.sourceSets.test.get(),
    )
}

tasks.withType<Checkstyle>().configureEach {
    exclude("**/module-info.java")
}

pmd {
    toolVersion = "7.7.0"
    ruleSetFiles = files(rootProject.file("pmd-ruleset.xml"))
    ruleSets = emptyList()
    isIgnoreFailures = false
}

// Static gates analyse main source only — test files use a different style.
tasks.named("pmdTest") { enabled = false }
tasks.named("spotbugsTest") { enabled = false }
tasks.named("checkstyleTest") { enabled = false }

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
        addStringOption("Xdoclint:none", "-quiet")
    }
    isFailOnError = false
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

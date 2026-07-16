// Aggregate root build for the Allegro Java SDK.
//
// Each subproject (allegro-rest-models, allegro-client, allegro-demo,
// allegro-examples, allegro-jpms-consumer) owns its own build.gradle.kts.
// The root holds only project metadata that applies uniformly to every
// module, plus the manual-run OWASP dependency-check task.

plugins {
    base
    id("org.owasp.dependencycheck") version "11.1.1"
    id("org.sonarqube") version "5.1.0.4882"
    // Declared at root with `apply false` so the plugin class is loaded by a
    // single Gradle ClassLoader. Subprojects apply it without a version.
    // Without this the SonatypeRepositoryBuildService gets instantiated per
    // subproject ClassLoader and the build-service registry rejects the
    // cross-loader handoff with a type-mismatch.
    id("com.vanniktech.maven.publish") version "0.30.0" apply false
}

sonar {
    properties {
        property("sonar.projectKey", "allegro-java-sdk")
        property("sonar.projectName", "Allegro Java SDK")
        property(
            "sonar.coverage.jacoco.xmlReportPaths",
            "${rootProject.projectDir}/allegro-client/build/reports/jacoco/test/jacocoTestReport.xml"
        )
        // allegro-demo is a live-execution probe runner driven manually against
        // the sandbox, not a unit-tested library module. Exclude the demo,
        // examples, and JPMS gate from coverage + duplication so quality gates
        // measure the library module (allegro-client) the SDK actually ships.
        property("sonar.coverage.exclusions", "allegro-demo/**, allegro-examples/**, allegro-jpms-consumer/**")
        property("sonar.cpd.exclusions", "allegro-demo/**")
        // Generated source trees never appear in analysis input.
        property("sonar.exclusions", "**/build/generated-sources/**, **/generated/**")
    }
}

// The sonarqube plugin auto-applies to every subproject; without this each one
// tries to analyse its own tree and fails when the auto-detected binaries path
// is missing. Only allegro-client is hand-written library code worth scanning.
gradle.projectsEvaluated {
    subprojects.filter {
        it.name in setOf(
            "allegro-rest-models", "allegro-demo",
            "allegro-examples", "allegro-jpms-consumer"
        )
    }.forEach { sp ->
        sp.extensions.findByType(org.sonarqube.gradle.SonarExtension::class.java)?.isSkipProject = true
    }
}

allprojects {
    repositories {
        mavenCentral()
    }
}

subprojects {
    group = rootProject.group
    version = rootProject.version
}

// Manual-run OWASP scan, NOT bound to the default verify cycle to keep the dev
// loop fast. Release engineer invokes `./gradlew dependencyCheckAggregate`
// before each tag. Fails the build on CVSS >= 7 (HIGH/CRITICAL).
dependencyCheck {
    failBuildOnCVSS = 7.0f
    skipConfigurations = listOf("testRuntimeClasspath", "testCompileClasspath")
    suppressionFile = rootProject.file("owasp-suppressions.xml").absolutePath
    formats = listOf("HTML", "JSON")
}

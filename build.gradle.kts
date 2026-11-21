import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    id("java-library")
    id("java-gradle-plugin")
    `kotlin-dsl`
    id("signing")
    id("com.gradle.plugin-publish") version "2.2.1"
    id("net.researchgate.release") version "3.2.0"
    id("com.github.ben-manes.versions") version "0.67.0"
    id("io.codearte.nexus-staging") version "0.30.0"
    id("com.adarshr.test-logger") version "4.0.0"
}

// *****************************************************************************
//
// *****************************************************************************

group = "com.github.lburgazzoli"
description = "Automates the process of creating Apache Karaf feature, repo, and kar files"

val isReleaseVersion = !version.toString().endsWith("SNAPSHOT")
val isSnapshotVersion = version.toString().endsWith("SNAPSHOT")
val gitRoot = "https://github.com/lburgazzoli"
val gitProject = "https://github.com/lburgazzoli/gradle-karaf-plugin"
val gitURL = "git@github.com:lburgazzoli/gradle-karaf-plugin"
val gradlePluginId = "com.github.lburgazzoli.karaf"
val gradlePluginTags = listOf("karaf")
val gradleCiTasks = listOf("check")

repositories {
    mavenCentral()
}

object versions {
    // The oldest Gradle the plugin supports. Main compiles against its API, so a call to an API
    // added later fails the build rather than a user's build on Gradle 8.0.
    val gradleApi = "8.0"
    val assertj = "3.26.3"
    val junit = "5.10.3"
    val junitPlatform = "1.10.3"
    val paxUrl = "2.5.4"
}

// java-gradle-plugin adds gradleApi() to compileOnlyApi, and kotlin-dsl adds gradleKotlinDsl() to
// compileOnly, both for the Gradle that runs this build. This removes the file dependencies the two
// configurations hold when the plugins have been applied; verifyGradleApiFloor fails if a Gradle jar
// reaches the main compile classpath another way.
listOf("compileOnlyApi", "compileOnly").forEach { name ->
    configurations.named(name) {
        dependencies.removeIf { it is FileCollectionDependency }
    }
}

val verifyGradleApiFloor by tasks.registering {
    description = "Checks that main compiles against no Gradle API newer than ${versions.gradleApi}."
    val classpath = files(configurations.compileClasspath)
    val allowed = "gradle-api-${versions.gradleApi}.jar"
    doLast {
        val newer = classpath.files.map { it.name }.filter { it.startsWith("gradle-") && it != allowed }
        if (newer.isNotEmpty()) {
            throw GradleException("The main compile classpath holds $newer; only $allowed may provide the Gradle API.")
        }
    }
}

tasks.check {
    dependsOn(verifyGradleApiFloor)
}

dependencies {
    compileOnly("dev.gradleplugins:gradle-api:${versions.gradleApi}")

    implementation("org.ops4j.pax.url:pax-url-aether:${versions.paxUrl}")

    testImplementation("org.junit.jupiter:junit-jupiter:${versions.junit}")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:${versions.junitPlatform}")
    testImplementation("org.assertj:assertj-core:${versions.assertj}")
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
    withSourcesJar()
    withJavadocJar()
}

tasks.jar {
    archiveBaseName.set(project.name)

    manifest {
        attributes["Implementation-Title"   ] = "${group}.${project.name}-${project.version}"
        attributes["Implementation-Version" ] = project.version
        attributes["Implementation-Vendor"  ] = "Luca Burgazzoli"
    }
}

// Gradle 8.0 runs on Java 8 and ships Kotlin 1.8.10, the compiler and standard library a build
// script or a buildSrc plugin uses with this plugin. The Kotlin compiler of the Gradle that builds the
// plugin accepts language and API version 2.0 at the oldest, so it does not reject a call to a function
// the 1.9 or 2.0 standard library added; EndToEndTest runs the plugin on Gradle 8.0.2 to catch one on
// the paths the tests exercise.
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        apiVersion.set(KotlinVersion.KOTLIN_2_0)
        languageVersion.set(KotlinVersion.KOTLIN_2_0)
        jvmTarget.set(JvmTarget.JVM_1_8)
    }
}

// Kotlin 1.8.10 rejects a class whose metadata version is 2.0.0, which is what language version 2.0
// writes by default. -Xjdk-release limits the JDK API to Java 8, as options.release does for javac.
tasks.compileKotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xmetadata-version=1.8.0", "-Xjdk-release=1.8")
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(8)
}

tasks.test {
    useJUnitPlatform()
    // TestKit starts each Gradle version on this JVM: Gradle 9 needs Java 17, and Gradle 8.0 runs on Java 19 at most.
    javaLauncher.set(javaToolchains.launcherFor {
        languageVersion.set(JavaLanguageVersion.of(17))
    })
}

testlogger {
    showStandardStreams = false
    slowThreshold = 30000
    theme = com.adarshr.gradle.testlogger.theme.ThemeType.MOCHA
}

// *****************************************************************************
// ARTIFACTS
// *****************************************************************************

// com.gradle.plugin-publish signs every publication once the signing plugin is applied. Signing is
// required only for publishPlugins, so publishToMavenLocal works without a key.
signing {
    setRequired({ gradle.taskGraph.hasTask(":publishPlugins") })
}

release {
    git {
        requireBranch.set("master")
    }
}

gradlePlugin {
    website.set(gitProject)
    vcsUrl.set(gitProject)
    plugins {
        create("karafPlugin") {
            id = gradlePluginId
            displayName = project.description
            description = project.description
            tags.set(gradlePluginTags)
            implementationClass = "com.github.lburgazzoli.gradle.plugin.karaf.KarafPlugin"
        }
    }
}

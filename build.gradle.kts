plugins {
    id("java-library")
    id("java-gradle-plugin")
    `kotlin-dsl`
    id("signing")
    id("com.gradle.plugin-publish") version "0.21.0"
    id("net.researchgate.release") version "2.8.1"
    id("com.github.ben-manes.versions") version "0.29.0"
    id("io.codearte.nexus-staging") version "0.21.2"
    id("com.adarshr.test-logger") version "2.1.0"
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
    val assertj = "3.26.3"
    val junit = "5.10.3"
    val paxUrl = "2.5.4"
}

dependencies {
    implementation(gradleApi())

    implementation("org.ops4j.pax.url:pax-url-aether:${versions.paxUrl}")

    testImplementation("org.junit.jupiter:junit-jupiter:${versions.junit}")
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

tasks.test {
    useJUnitPlatform()
}

testlogger {
    showStandardStreams = false
    slowThreshold = 30000
    theme = com.adarshr.gradle.testlogger.theme.ThemeType.MOCHA
}

// *****************************************************************************
// ARTIFACTS
// *****************************************************************************

pluginBundle {
    website = gitProject
    vcsUrl = gitProject
    description = project.description
    tags = gradlePluginTags
}

gradlePlugin {
    plugins {
        create("karafPlugin") {
            id = gradlePluginId
            displayName = project.description
            description = project.description
            implementationClass = "com.github.lburgazzoli.gradle.plugin.karaf.KarafPlugin"
        }
    }
}

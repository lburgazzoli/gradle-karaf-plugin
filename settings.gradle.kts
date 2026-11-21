plugins {
    // Provisions the Java 17 the tests run on when no local JDK 17 is installed.
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "gradle-karaf-plugin"

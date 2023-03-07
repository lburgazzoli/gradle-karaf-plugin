package com.github.lburgazzoli.gradle.plugin.karaf

import org.gradle.testkit.runner.GradleRunner
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class EndToEndTest {
    protected val gradleRunner = GradleRunner.create().withPluginClasspath()

    @get:Rule
    val projectDir = TemporaryFolder()

    protected fun prepare(gradleVersion: String, vararg arguments: String) =
        gradleRunner
            .withGradleVersion(gradleVersion)
            .withProjectDir(projectDir.root)
            .withArguments(*arguments)
            .forwardOutput()

    @Test
    fun `kotlin dsl`() {
        projectDir.root.resolve("settings.gradle").writeText(
            """
                rootProject.name = 'sample'
            """
        )
        projectDir.root.resolve("build.gradle.kts").writeText(/* language=kotlin */
            """
                plugins {
                  `java-library`
                  id("com.github.lburgazzoli.karaf")
                }

                group = "com.example"
                version = "1.0"

                repositories {
                    mavenCentral()
                }

                dependencies {
                    implementation("org.postgresql:postgresql:42.4.0")
                }

                val karafFeatures by configurations.creating {
                     extendsFrom(configurations.implementation.get())
                }

                karaf {
                    features {
                        xsdVersion.set("1.5.0")
                        feature {
                            name = "postgresql"
                            description = "PostgreSQL JDBC driver karaf feature"
                            version = project.version.toString()
                            details = "Java JDBC 4.2 (JRE 8+) driver for PostgreSQL database"
                            feature("transaction-api")
                            includeProject = true
                            bundle(project.group.toString()) {
                                wrap = false
                            }
                            configuration("karafFeatures")
                        }
                    }
                }
           """
        )

        prepare("7.4.2", "generateFeatures", "-i").build()
    }

    @Test
    fun `groovy dsl`() {
        projectDir.root.resolve("settings.gradle").writeText(
            """
                rootProject.name = 'sample'
            """
        )
        projectDir.root.resolve("build.gradle").writeText(/* language=groovy */
            """
                plugins {
                  id 'java-library'
                  id 'com.github.lburgazzoli.karaf'
                }

                group = 'com.example'
                version = '1.0'

                repositories {
                    mavenCentral()
                }

                dependencies {
                    implementation 'org.postgresql:postgresql:42.4.0'
                }

                configurations {
                    karafFeatures
                }
                configurations.karafFeatures {
                     extendsFrom(configurations.implementation)
                }

                karaf {
                    features {
                        xsdVersion = '1.5.0'
                        feature {
                            name = 'postgresql'
                            description = 'PostgreSQL JDBC driver karaf feature'
                            version = project.version.toString()
                            details = 'Java JDBC 4.2 (JRE 8+) driver for PostgreSQL database'
                            feature('transaction-api')
                            includeProject = true
                            bundle(project.group.toString()) {
                                wrap = false
                            }
                            configuration('karafFeatures')
                        }
                    }
                }
           """
        )

        prepare("7.4.2", "generateFeatures", "-i").build()
    }
}

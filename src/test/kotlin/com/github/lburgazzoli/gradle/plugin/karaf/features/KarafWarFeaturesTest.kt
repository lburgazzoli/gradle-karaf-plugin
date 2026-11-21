package com.github.lburgazzoli.gradle.plugin.karaf.features

import com.github.lburgazzoli.gradle.plugin.karaf.generateFeatures
import com.github.lburgazzoli.gradle.plugin.karaf.karaf
import com.github.lburgazzoli.gradle.plugin.karaf.karafProject
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.SoftAssertions.assertSoftly
import org.gradle.api.artifacts.ExternalModuleDependency
import org.gradle.api.tasks.bundling.War
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class KarafWarFeaturesTest {
    @TempDir
    lateinit var projectDir: File

    @Test
    fun `a war dependency becomes a war bundle with its instructions as a query`() {
        val project = karafProject(projectDir) {
            (dependencies.add("implementation", "org.apache.activemq:activemq-web-console:5.13.2@war")
                as ExternalModuleDependency).isTransitive = false
        }
        project.karaf.features.apply {
            xsdVersion = "1.3.0"
            feature {
                name = "karaf-features-simple-project"
                description = "feature-description"
                details = "my detailed description"
                bundle("org.apache.activemq:activemq-web-console:5.13.2") {
                    attribute("startLevel", "50")
                    instruction("Webapp-Context", "activemq-web-console")
                }
            }
        }

        val xml = project.generateFeatures()

        assertSoftly {
            it.assertThat(xml.elements("feature").map { e -> e.attributes })
                .containsExactly(
                    mapOf(
                        "name" to "karaf-features-simple-project",
                        "description" to "feature-description",
                        "version" to "1.2.3"
                    )
                )
            it.assertThat(xml.bundles())
                .containsOnlyOnce("war:mvn:org.apache.activemq/activemq-web-console/5.13.2/war?Webapp-Context=activemq-web-console")
        }
    }

    @Test
    fun `a project with the war plugin is included as a war bundle`() {
        val project = karafProject(projectDir) {
            pluginManager.apply("war")
            tasks.withType(War::class.java).configureEach {
                manifest.attributes(
                    mapOf(
                        "Web-ContextPath" to "/context-path",
                        "Webapp-Context" to "/context-path",
                        "Bundle-ClassPath" to ".;/WEB-INF/classes"
                    )
                )
            }
        }
        project.karaf.features.feature {
            includeProject = true
        }

        val bundles = project.generateFeatures().bundles()

        assertSoftly {
            it.assertThat(bundles).containsOnlyOnce("war:mvn:com.lburgazzoli.github/gradle-karaf/1.2.3/war")
            it.assertThat(bundles).doesNotContain("mvn:com.lburgazzoli.github/gradle-karaf/1.2.3")
        }
    }
}

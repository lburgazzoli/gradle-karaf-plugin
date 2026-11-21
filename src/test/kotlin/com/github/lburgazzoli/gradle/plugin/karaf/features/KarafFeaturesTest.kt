package com.github.lburgazzoli.gradle.plugin.karaf.features

import com.github.lburgazzoli.gradle.plugin.karaf.FeaturesXml
import com.github.lburgazzoli.gradle.plugin.karaf.XmlElement
import com.github.lburgazzoli.gradle.plugin.karaf.generateFeatures
import com.github.lburgazzoli.gradle.plugin.karaf.karaf
import com.github.lburgazzoli.gradle.plugin.karaf.karafProject
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.SoftAssertions.assertSoftly
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.io.File

class KarafFeaturesTest {
    @TempDir
    lateinit var projectDir: File

    private fun FeaturesXml.bundlesMatching(regex: String): List<String> =
        bundles().filter { Regex(regex).containsMatchIn(it) }

    @ParameterizedTest(name = "{0} is written as {1}")
    @CsvSource(
        "1.3, 1.3.0",
        "1.2, 1.2.0",
        "1.3.0, 1.3.0",
        "1.3.0.0, 1.3.0",
        "01.3, 1.3.0",
        "1.3.0.1, 1.3.0.1",
        "1, 1.0.0",
        "1.3-SNAPSHOT, 1.3-SNAPSHOT",
        "'', ''"
    )
    fun `xsdVersion is written in the major-minor-micro form of Karaf's namespaces`(xsdVersion: String, expected: String) {
        assertThat(KarafFeaturesTask.normalizeXsdVersion(xsdVersion))
            .`as`("normalizeXsdVersion(%s)", xsdVersion)
            .isEqualTo(expected)
    }

    @Test
    fun `a feature reads runtimeClasspath by default`() {
        val project = karafProject(projectDir)

        project.karaf.features.feature {
            name = "feature-1"
            description = "my feature n1"
        }

        assertThat(project.karaf.features.featureDescriptors.map { it.configurations.toList() })
            .containsExactly(listOf(project.configurations.getByName("runtimeClasspath")))
    }

    @Test
    fun `one GAV as a jar with a classifier and as a war yields two bundles`() {
        val project = karafProject(projectDir) {
            dependencies.add("implementation", "ca.uhn.hapi.fhir:hapi-fhir-testpage-overlay:2.1:classes")
            dependencies.add("implementation", "ca.uhn.hapi.fhir:hapi-fhir-testpage-overlay:2.1@war")
        }
        project.karaf.features.feature {
            name = "feature-1"
            description = "my feature n1"
        }

        val xml = project.generateFeatures()

        assertSoftly {
            it.assertThat(xml.bundlesMatching("wrap:mvn:ca.uhn.hapi.fhir/hapi-fhir-testpage-overlay/2.1/jar/classes"))
                .hasSize(1)
            it.assertThat(xml.bundlesMatching("war:mvn:ca.uhn.hapi.fhir/hapi-fhir-testpage-overlay/2.1/war"))
                .hasSize(1)
        }
    }

    @Test
    fun `remap clears the type of a bundle with a classifier`() {
        val project = karafProject(projectDir) {
            dependencies.add("implementation", "ca.uhn.hapi.fhir:hapi-fhir-testpage-overlay:2.1:classes")
            dependencies.add("implementation", "ca.uhn.hapi.fhir:hapi-fhir-testpage-overlay:2.1@war")
        }
        project.karaf.features.feature {
            name = "feature-1"
            description = "my feature n1"
            bundle("ca.uhn.hapi.fhir:hapi-fhir-testpage-overlay:2.1:classes") {
                wrap = true
                remap {
                    type = null
                }
            }
        }

        val xml = project.generateFeatures()

        assertSoftly {
            it.assertThat(xml.bundlesMatching("wrap:mvn:ca.uhn.hapi.fhir/hapi-fhir-testpage-overlay/2.1//classes"))
                .hasSize(1)
            it.assertThat(xml.bundlesMatching("war:mvn:ca.uhn.hapi.fhir/hapi-fhir-testpage-overlay/2.1/war"))
                .hasSize(1)
        }
    }

    @Test
    fun `only the resolved version of a dependency becomes a bundle`() {
        val project = karafProject(projectDir) {
            dependencies.add("implementation", "com.graphql-java:graphql-java-servlet:0.9.0")
            dependencies.add("implementation", "com.google.guava:guava:20.0")
        }
        project.karaf.features.feature {
            name = "feature-1"
            description = "my feature n1"
        }

        val xml = project.generateFeatures()

        assertSoftly {
            it.assertThat(xml.bundlesMatching("mvn:com.google.guava/guava/20.0")).hasSize(1)
            it.assertThat(xml.bundlesMatching("mvn:com.google.guava/guava/19.0")).isEmpty()
        }
    }

    @Test
    fun `a feature lists its dependencies, feature dependencies, and bundle attributes`() {
        val project = karafProject(projectDir) {
            dependencies.add("runtimeOnly", "com.google.guava:guava:19.0")
            dependencies.add("runtimeOnly", "com.squareup.retrofit:retrofit:1.9.0")
            dependencies.add("runtimeOnly", "com.squareup.retrofit:converter-jackson:1.9.0")
            dependencies.add("implementation", "org.apache.activemq:activemq-web:5.13.2")
            dependencies.add("implementation", "org.apache.activemq:activemq-web-console:5.13.2@war")
            dependencies.add("implementation", "commons-codec:commons-codec:1.10")
            dependencies.add("implementation", "commons-collections:commons-collections:3.2.2")
            dependencies.add("implementation", "commons-fileupload:commons-fileupload:1.3.2")
            dependencies.add("implementation", "commons-io:commons-io:2.5")
            dependencies.add("implementation", "commons-lang:commons-lang:2.6")
        }
        project.karaf.features.apply {
            xsdVersion = "1.3.0"
            repository("mvn:org.apache.karaf.cellar/apache-karaf-cellar/4.0.0/xml/features")
            repository("mvn:org.apache.karaf.features/standard/4.0.0/xml/features")
            feature {
                name = "karaf-features-simple-project"
                description = "feature-description"
                details = "my detailed description"
                includeProject = true
                configFile {
                    filename = "/etc/org.apache.karaf.cellar.groups.cfg"
                    uri = "mvn:org.apache.karaf.cellar/apache-karaf-cellar/${project.version}/cfg/groups"
                }
                feature("dependencyFeatureName1")
                feature("dependencyFeatureName2") {
                    version = "5.6.7"
                    dependency = true
                }
                feature("dependencyFeatureName3") {
                    prerequisite = true
                }
                bundle("com.squareup.retrofit:converter-jackson") {
                    include = false
                }
                bundle("commons-*:*-c*") {
                    attribute("dependency", "true")
                }
            }
        }

        val xml = project.generateFeatures()

        assertSoftly {
            it.assertThat(xml.elements(".").map { e -> e.attributes["xmlns"] })
                .containsExactly("http://karaf.apache.org/xmlns/features/v1.3.0")
            it.assertThat(xml.elements("feature").map { e -> e.attributes })
                .containsExactly(
                    mapOf(
                        "name" to "karaf-features-simple-project",
                        "description" to "feature-description",
                        "version" to "1.2.3"
                    )
                )
            it.assertThat(xml.elements("feature/feature").take(2))
                .containsExactly(
                    XmlElement("dependencyFeatureName1", emptyMap()),
                    XmlElement("dependencyFeatureName2", mapOf("version" to "5.6.7", "dependency" to "true"))
                )
            it.assertThat(xml.bundlesMatching("wrap:mvn:com.lburgazzoli.github/gradle-karaf/1.2.3")).hasSize(1)
            it.assertThat(xml.bundlesMatching("mvn:com.google.guava/guava/19.0")).hasSize(1)
            it.assertThat(xml.bundlesMatching("mvn:com.google.code.gson/gson/2.3.1")).hasSize(1)
            it.assertThat(xml.bundlesMatching("^*.mvn:com.squareup.retrofit/retrofit/1.9.0$")).hasSize(1)
            it.assertThat(xml.bundlesMatching("^*.mvn:com.squareup.retrofit/converter-jackson/1.9.0$")).isEmpty()
            it.assertThat(
                xml.elements("feature/bundle")
                    .filter { b -> b.text.contains("mvn:commons-") && b.attributes["dependency"] == "true" }
            ).hasSize(2)
        }
    }

    @Test
    fun `a config file without override has no override attribute`() {
        val project = karafProject(projectDir) {
            configurations.create("hazelcast")
            dependencies.add("hazelcast", "org.apache.geronimo.specs:geronimo-jta_1.1_spec:1.1.1")
            dependencies.add("hazelcast", "com.eclipsesource.minimal-json:minimal-json:0.9.2")
            dependencies.add("hazelcast", "com.hazelcast:hazelcast-all:3.6.1")
        }
        project.karaf.features.apply {
            xsdVersion = "1.3.0"
            feature {
                name = "hazelcast"
                description = "In memory data grid"
                configurations {
                    add("hazelcast")
                }
                configFile {
                    filename = "/etc/hazelcast.xml"
                    uri = "mvn:org.apache.karaf.cellar/apache-karaf-cellar/${project.version}/xml/hazelcast"
                }
            }
        }

        val xml = project.generateFeatures()

        assertThat(xml.elements("feature/configfile"))
            .containsExactly(
                XmlElement(
                    "mvn:org.apache.karaf.cellar/apache-karaf-cellar/1.2.3/xml/hazelcast",
                    mapOf("finalname" to "/etc/hazelcast.xml")
                )
            )
    }

    @Test
    fun `a config file carries the override flag it was given`() {
        val project = karafProject(projectDir) {
            configurations.create("hazelcast")
            dependencies.add("hazelcast", "org.apache.geronimo.specs:geronimo-jta_1.1_spec:1.1.1")
            dependencies.add("hazelcast", "com.eclipsesource.minimal-json:minimal-json:0.9.2")
            dependencies.add("hazelcast", "com.hazelcast:hazelcast-all:3.6.1")
        }
        project.karaf.features.apply {
            xsdVersion = "1.3.0"
            feature {
                name = "hazelcast"
                description = "In memory data grid"
                configurations {
                    add("hazelcast")
                }
                configFile {
                    filename = "/etc/hazelcast-1.xml"
                    uri = "mvn:org.apache.karaf.cellar/apache-karaf-cellar/${project.version}/xml/hazelcast"
                    override = true
                }
                configFile {
                    filename = "/etc/hazelcast-2.xml"
                    uri = "mvn:org.apache.karaf.cellar/apache-karaf-cellar/${project.version}/xml/hazelcast"
                    override = false
                }
            }
        }

        val xml = project.generateFeatures()

        assertThat(xml.elements("feature/configfile").map { it.attributes })
            .containsExactly(
                mapOf("finalname" to "/etc/hazelcast-1.xml", "override" to "true"),
                mapOf("finalname" to "/etc/hazelcast-2.xml", "override" to "false")
            )
    }

    @Test
    fun `a feature with a conditional section is written`() {
        val project = karafProject(projectDir) {
            dependencies.add("implementation", "com.google.guava:guava:19.0")
            dependencies.add("implementation", "com.squareup.retrofit:retrofit:1.9.0")
            dependencies.add("implementation", "com.fasterxml.jackson.core:jackson-core:2.7.0")
            dependencies.add("implementation", "com.fasterxml.jackson.core:jackson-databind:2.7.0")
            dependencies.add("implementation", "com.fasterxml.jackson.core:jackson-annotations:2.7.0")
        }
        project.karaf.features.apply {
            xsdVersion = "1.3.0"
            feature {
                name = "karaf-features-simple-project"
                description = "feature-description"
                details = "my detailed description"
                includeProject = false
                conditional("json-p") {
                    feature("myfeature-1")
                    feature("myfeature-2")
                    bundle("com.fasterxml.jackson.core")
                }
            }
        }

        val xml = project.generateFeatures()

        assertThat(xml.elements("feature").map { it.attributes["name"] })
            .containsExactly("karaf-features-simple-project")
    }

    @Test
    fun `capabilities are formatted with their filter, effective, resolution, and extra parts`() {
        val project = karafProject(projectDir) {
            dependencies.add("implementation", "com.google.guava:guava:19.0")
            dependencies.add("implementation", "com.squareup.retrofit:retrofit:1.9.0")
            dependencies.add("implementation", "com.fasterxml.jackson.core:jackson-core:2.7.0")
            dependencies.add("implementation", "com.fasterxml.jackson.core:jackson-databind:2.7.0")
            dependencies.add("implementation", "com.fasterxml.jackson.core:jackson-annotations:2.7.0")
        }
        project.karaf.features.apply {
            xsdVersion = "1.3.0"
            feature {
                name = "karaf-features-simple-project"
                description = "feature-description"
                details = "my detailed description"
                includeProject = false
                capability("osgi.extender") {
                    effective = "active"
                    filter = "(&(osgi.extender=osgi.enroute.configurer)\${frange;1.2.3})"
                    resolution = "effective"
                    extra = "someExtraStuffs"
                }
                capability("osgi.service") {
                    effective = "active"
                    filter = "(&(osgi.extender=osgi.enroute.configurer)\${frange;1.2.3})"
                }
                capability("osgi.service") {
                    effective = "active"
                    extra = "objectClass=javax.jms.ConnectionFactory"
                }
            }
        }

        val capabilities = project.generateFeatures().elements("feature/capability").map { it.text }

        assertSoftly {
            it.assertThat(capabilities.filter { c -> c.contains("osgi.extender;filter") }).hasSize(1)
            it.assertThat(capabilities.filter { c -> c.contains("osgi.service;filter") }).hasSize(1)
            it.assertThat(capabilities)
                .containsOnlyOnce(
                    "osgi.service;effective:='active';resolution:='mandatory';objectClass=javax.jms.ConnectionFactory"
                )
        }
    }

    @Test
    fun `wrap instructions are appended to the bundle URL`() {
        val project = karafProject(projectDir) {
            dependencies.add("implementation", "org.apache.geronimo.specs:geronimo-jta_1.1_spec:1.1.1")
            dependencies.add("implementation", "com.eclipsesource.minimal-json:minimal-json:0.9.2")
            dependencies.add("implementation", "com.hazelcast:hazelcast-all:3.6.1")
        }
        project.karaf.features.apply {
            xsdVersion = "1.3.0"
            feature {
                name = "hazelcast"
                description = "In memory data grid"
                bundle("com.eclipsesource.minimal-json:minimal-json:0.9.2") {
                    wrap {
                        instruction("Bundle-SymbolicName", "my-bundle-name")
                    }
                }
            }
        }

        val xml = project.generateFeatures()

        assertSoftly {
            it.assertThat(
                xml.bundlesMatching(
                    "wrap:mvn:com.eclipsesource.minimal-json/minimal-json/0.9.2\\\$Bundle-SymbolicName=my-bundle-name"
                )
            ).hasSize(1)
            it.assertThat(xml.bundlesMatching("mvn:org.apache.geronimo.specs/geronimo-jta_1.1_spec/1.1.1")).hasSize(1)
            it.assertThat(xml.bundlesMatching("mvn:com.hazelcast/hazelcast-all/3.6.1")).hasSize(1)
        }
    }

    @Test
    fun `includeProject adds the project bundle only to the feature that sets it`() {
        val project = karafProject(projectDir) {
            configurations.create("hazelcast")
            configurations.create("squareup")
            dependencies.add("hazelcast", "org.apache.geronimo.specs:geronimo-jta_1.1_spec:1.1.1")
            dependencies.add("hazelcast", "com.eclipsesource.minimal-json:minimal-json:0.9.2")
            dependencies.add("hazelcast", "com.hazelcast:hazelcast-all:3.6.1")
            dependencies.add("squareup", "com.squareup.retrofit:retrofit:1.9.0")
            dependencies.add("squareup", "com.squareup.retrofit:converter-jackson:1.9.0")
        }
        project.karaf.features.apply {
            xsdVersion = "1.3.0"
            feature {
                name = "hazelcast"
                description = "In memory data grid"
                includeProject = false
                configurations("hazelcast")
            }
            feature {
                name = "squareup"
                description = "Squareup"
                includeProject = true
                configurations("squareup")
            }
        }

        val xml = project.generateFeatures()
        val projectBundle = "wrap:mvn:com.lburgazzoli.github/gradle-karaf/1.2.3"

        assertSoftly {
            it.assertThat(xml.elements("feature[@name='hazelcast']/bundle").map { b -> b.text })
                .doesNotContain(projectBundle)
            it.assertThat(xml.elements("feature[@name='squareup']/bundle").map { b -> b.text })
                .containsOnlyOnce(projectBundle)
        }
    }

    @Test
    fun `two dependencies remapped to the same coordinates give one bundle`() {
        val project = karafProject(projectDir) {
            dependencies.add("implementation", "commons-lang:commons-lang:2.6")
            dependencies.add("implementation", "com.google.guava:guava:19.0")
        }
        project.karaf.features.feature {
            name = "feature-1"
            bundle("com.google.guava") {
                remap(mapOf("group" to "commons-lang", "name" to "commons-lang", "version" to "2.6"))
            }
        }

        val xml = project.generateFeatures()

        assertThat(xml.bundles()).containsExactly("mvn:commons-lang/commons-lang/2.6")
    }
}

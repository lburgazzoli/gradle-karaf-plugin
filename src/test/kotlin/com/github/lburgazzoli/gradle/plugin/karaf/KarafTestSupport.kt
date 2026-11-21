package com.github.lburgazzoli.gradle.plugin.karaf

import com.github.lburgazzoli.gradle.plugin.karaf.features.KarafFeaturesTask
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.w3c.dom.Element
import org.w3c.dom.NodeList
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.xpath.XPathConstants
import javax.xml.xpath.XPathFactory

/**
 * Gradle user home shared by every [ProjectBuilder] project, so the dependency cache is filled once per build
 * rather than once per test.
 */
private val sharedGradleUserHome = File("build/test-gradle-user-home").absoluteFile

/**
 * Creates `com.lburgazzoli.github:gradle-karaf:1.2.3` in [projectDir] with the `java` plugin,
 * Maven Central, and the Karaf plugin applied, then runs [configure] on it.
 */
fun karafProject(projectDir: File, configure: Project.() -> Unit = {}): Project {
    val project = ProjectBuilder.builder()
        .withName("gradle-karaf")
        .withProjectDir(projectDir)
        .withGradleUserHomeDir(sharedGradleUserHome)
        .build()
    project.group = "com.lburgazzoli.github"
    project.version = "1.2.3"
    project.pluginManager.apply("java")
    project.repositories.mavenCentral()
    project.pluginManager.apply(KarafPlugin::class.java)
    project.configure()
    return project
}

val Project.karaf: KarafPluginExtension
    get() = extensions.getByType(KarafPluginExtension::class.java)

/**
 * Runs the `generateFeatures` action and parses the file it writes.
 */
fun Project.generateFeatures(): FeaturesXml {
    (tasks.getByName(KarafFeaturesTask.NAME) as KarafFeaturesTask).run()
    return FeaturesXml.parse(karaf.features.outputFile)
}

data class XmlElement(val text: String, val attributes: Map<String, String>)

class FeaturesXml(private val root: Element, val text: String) {
    companion object {
        fun parse(file: File): FeaturesXml {
            val document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
            return FeaturesXml(document.documentElement, file.readText())
        }
    }

    /**
     * Returns the elements [xpath] selects, relative to the `features` root element.
     */
    fun elements(xpath: String): List<XmlElement> {
        val nodes = XPathFactory.newInstance().newXPath()
            .evaluate(xpath, root, XPathConstants.NODESET) as NodeList
        return (0 until nodes.length).map { index ->
            val element = nodes.item(index) as Element
            val attributes = element.attributes
            XmlElement(
                element.textContent,
                (0 until attributes.length).associate { attributes.item(it).nodeName to attributes.item(it).nodeValue }
            )
        }
    }

    /**
     * Returns the URLs of the `bundle` elements directly under each `feature`.
     */
    fun bundles(): List<String> = elements("feature/bundle").map { it.text }
}

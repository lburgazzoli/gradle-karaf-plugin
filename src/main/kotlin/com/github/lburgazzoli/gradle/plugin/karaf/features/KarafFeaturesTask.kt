/**
 * Copyright 2016, Luca Burgazzoli and contributors as indicated by the @author tags
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.lburgazzoli.gradle.plugin.karaf.features

import com.github.lburgazzoli.gradle.plugin.karaf.KarafPluginExtension
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.the
import org.gradle.kotlin.dsl.withGroovyBuilder
import org.gradle.util.VersionNumber

/**
 * @author lburgazzoli
 */
open class KarafFeaturesTask : DefaultTask() {
    companion object {
        const val GROUP = "karaf"
        const val NAME = "generateFeatures"
        const val DESCRIPTION = "Generates Karaf features file"

        const val FEATURES_XMLNS_PREFIX = "http://karaf.apache.org/xmlns/features/v"
        val XMLNS_V13 = VersionNumber(1, 3, 0, null)

    }

    init {
        // TODO: to be improved
        outputs.upToDateWhen {
            false
        }
    }

    private val karaf = project.the<KarafPluginExtension>()

    @TaskAction
    fun run() {
        if (karaf.hasFeatures()) {
            val outputFile = karaf.features.outputFile.get().asFile

            // write out a features repository xml.
            if (!outputFile.parentFile.exists()) {
                outputFile.parentFile.mkdirs()
            }

            outputFile.writeText(generateFeatures(karaf.features))
        }
    }

    fun generateFeatures(karaf: KarafFeaturesExtension): String {
        val builder = KarafFeaturesBuilder()
        val xsdVer13 = VersionNumber.parse(karaf.xsdVersion.get()) == XMLNS_V13
        val resolver = karaf.resolver

        builder.mkp.xmlDeclaration(mapOf("version" to "1.0", "encoding" to "UTF-8", "standalone" to "yes"))
        builder.withGroovyBuilder {
            "features"(
                "xmlns" to FEATURES_XMLNS_PREFIX + karaf.xsdVersion.get(),
                "name" to karaf.name.get() + "-" + karaf.version.get()
            ) {
                karaf.repositories.get().forEach {
                    "repository"(it)
                }
                karaf.featureDescriptors.forEach { feature ->
                    "feature"(
                        "name" to feature.name,
                        "version" to feature.version,
                        "description" to feature.description
                    ) {
                        feature.details?.let { "details"(feature.details) }
                        feature.configs.forEach {
                            "config"("name" to it.name, it.content)
                        }

                        feature.configFiles.forEach {
                            if (it.filename != null && it.uri != null) {
                                "configfile"(mapOf("finalname" to it.filename, "override" to it.override), it.uri)
                            }
                        }
                        feature.featureDependencies.forEach {
                            "feature"(
                                mapOf(
                                    "version" to it.version,
                                    "dependency" to (it.dependency != null && xsdVer13).takeIf { it },
                                    "prerequisite" to it.prerequisite
                                ),
                                it.name
                            )
                        }
                        val dependencies = resolver.resolve(feature)
                        dependencies.forEach {
                            if (!feature.isConditional(it)) {
                                if (it.bundle != null) {
                                    "bundle"(it.bundle.attributes, it.dependency.url)
                                } else {
                                    "bundle"(it.dependency.url)
                                }
                            }
                        }
                        feature.conditions.get().forEach { condition ->
                            "conditional" {
                                condition.condition?.let {
                                    "condition"(it)
                                }
                            }
                            condition.configs.forEach {
                                "config"("name" to it.name, it.content)
                            }
                            feature.configFiles.forEach {
                                if (it.filename != null && it.uri != null) {
                                    "configfile"(mapOf("finalname" to it.filename, "override" to it.override), it.uri)
                                }
                            }
                            condition.featureDependencies.forEach {
                                "feature"(
                                    mapOf(
                                        "version" to it.version,
                                        "dependency" to (it.dependency != null && xsdVer13).takeIf { it },
                                        "prerequisite" to it.prerequisite
                                    ),
                                    it.name
                                )
                            }
                            dependencies.forEach { dependency ->
                                condition.bundleDescriptors.forEach {
                                    if (it.matcher.matches(dependency.dependency)) {
                                        if (dependency.bundle != null) {
                                            "bundle"(dependency.bundle.attributes, dependency.dependency.url)
                                        } else {
                                            "bundle"(dependency.dependency.url)
                                        }
                                    }
                                }
                            }
                        }
                        feature.capabilities.forEach {
                            "capability"(it.format())
                        }
                    }
                }
            }
        }
        return builder.toString()
    }
}

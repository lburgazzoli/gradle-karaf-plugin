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

import org.gradle.api.tasks.Internal
import org.gradle.api.Action
import com.github.lburgazzoli.gradle.plugin.karaf.KarafPluginExtension
import com.github.lburgazzoli.gradle.plugin.karaf.KarafTaskExtension
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import com.github.lburgazzoli.gradle.plugin.karaf.features.model.DependencyDescriptor
import com.github.lburgazzoli.gradle.plugin.karaf.features.model.FeatureDependencyDescriptor

/**
 * @author lburgazzoli
 */
open class KarafFeaturesTask : DefaultTask() {
    companion object {
        const val GROUP = "karaf"
        const val NAME = "generateFeatures"
        const val DESCRIPTION = "Generates Karaf features file"

        const val FEATURES_XMLNS_PREFIX = "http://karaf.apache.org/xmlns/features/v"
        const val XMLNS_V13 = "1.3.0"

        /**
         * Returns `true` when [version] is 1.3, with any number of trailing zero components,
         * so `1.3` and `1.3.0` both match.
         */
        @JvmStatic
        fun isXsdVersion13(version: String): Boolean = normalizeXsdVersion(version) == XMLNS_V13

        /**
         * Returns [version] in the `major.minor.micro` form of Karaf's features namespaces: `1` becomes `1.0.0`, and
         * `1.3` and `1.3.0.0` become `1.3.0`. A version with a non-numeric component, or with a non-zero component
         * past the third, is returned unchanged.
         */
        @JvmStatic
        fun normalizeXsdVersion(version: String): String {
            val parts = version.split('.').map { it.toIntOrNull() ?: return version }
            if (parts.drop(3).any { it != 0 }) {
                return version
            }
            return (parts.take(3) + List(3 - minOf(parts.size, 3)) { 0 }).joinToString(".")
        }
    }

    init {
        // TODO: to be improved
        outputs.upToDateWhen {
            false
        }
    }

    private val karafExtension = KarafTaskExtension(this)

    /**
     * The settings this task reads: the extension set with [karaf], or else the project's `karaf` extension.
     */
    @get:Internal
    val karaf: KarafPluginExtension
        get() = karafExtension.extension

    /**
     * Makes this task read [extension] instead of the project's `karaf` extension.
     */
    fun karaf(extension: KarafPluginExtension) {
        karafExtension.set(extension)
    }

    /**
     * Makes this task read a new extension, configured by [action], instead of the project's `karaf` extension.
     */
    fun karaf(action: Action<in KarafPluginExtension>) {
        karafExtension.set(action)
    }

    @TaskAction
    fun run() {
        if (karaf.hasFeatures()) {
            val outputFile = karaf.features.outputFile

            // write out a features repository xml.
            if (!outputFile.parentFile.exists()) {
                outputFile.parentFile.mkdirs()
            }

            outputFile.writeText(generateFeatures(karaf.features))
        }
    }

    fun generateFeatures(karaf: KarafFeaturesExtension): String {
        val builder = KarafFeaturesBuilder()
        val xsdVer13 = isXsdVersion13(karaf.xsdVersion)
        val resolver = karaf.resolver

        builder.element(
            "features",
            mapOf(
                "xmlns" to FEATURES_XMLNS_PREFIX + normalizeXsdVersion(karaf.xsdVersion),
                "name" to karaf.name + "-" + karaf.version
            )
        ) {
            karaf.repositories.forEach {
                builder.element("repository", text = it)
            }
            karaf.featureDescriptors.forEach { feature ->
                builder.element(
                    "feature",
                    mapOf(
                        "name" to feature.name,
                        "version" to feature.version,
                        "description" to feature.description
                    )
                ) {
                    feature.details?.takeIf { it.isNotEmpty() }?.let { builder.element("details", text = it) }
                    feature.configs.forEach {
                        builder.element("config", mapOf("name" to it.name), it.content)
                    }
                    feature.configFiles.forEach {
                        if (!it.filename.isNullOrEmpty() && !it.uri.isNullOrEmpty()) {
                            builder.element("configfile", mapOf("finalname" to it.filename, "override" to it.override), it.uri)
                        }
                    }
                    feature.featureDependencies.forEach {
                        builder.featureDependency(it, xsdVer13)
                    }
                    val dependencies = resolver.resolve(feature)
                    dependencies.forEach {
                        if (!feature.isConditional(it)) {
                            builder.bundle(it)
                        }
                    }
                    feature.conditions.forEach { condition ->
                        builder.element("conditional") {
                            condition.condition?.takeIf { it.isNotEmpty() }?.let {
                                builder.element("condition", text = it)
                            }
                            condition.configs.forEach {
                                builder.element("config", mapOf("name" to it.name), it.content)
                            }
                            feature.configFiles.forEach {
                                if (!it.filename.isNullOrEmpty() && !it.uri.isNullOrEmpty()) {
                                    builder.element("configfile", mapOf("finalname" to it.filename), it.uri)
                                }
                            }
                            condition.featureDependencies.forEach {
                                builder.featureDependency(it, xsdVer13)
                            }
                            dependencies.forEach { dependency ->
                                condition.bundleDescriptors.forEach {
                                    if (it.matcher.matches(dependency.dependency)) {
                                        builder.bundle(dependency)
                                    }
                                }
                            }
                        }
                    }
                    feature.capabilities.forEach {
                        builder.element("capability", text = it.format())
                    }
                }
            }
        }
        return builder.toString()
    }

    private fun KarafFeaturesBuilder.featureDependency(dependency: FeatureDependencyDescriptor, xsdVer13: Boolean) =
        element(
            "feature",
            mapOf(
                "version" to dependency.version,
                "dependency" to (if (dependency.dependency == true && xsdVer13) true else null),
                "prerequisite" to dependency.prerequisite
            ),
            dependency.name
        )

    private fun KarafFeaturesBuilder.bundle(dependency: DependencyDescriptor) =
        element("bundle", dependency.bundle?.attributes ?: emptyMap<String, String>(), dependency.dependency.url)
}

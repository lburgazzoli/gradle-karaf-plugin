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
package com.github.lburgazzoli.gradle.plugin.karaf.features.model

import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString
import org.gradle.api.Action
import org.gradle.api.artifacts.ModuleVersionIdentifier
import org.gradle.api.artifacts.ResolvedArtifact
import java.io.File

/**
 * @author lburgazzoli
 */
abstract class FeatureDefinition {
    val bundleDescriptors: MutableList<BundleDescriptor> = mutableListOf()
    val featureDependencies: MutableList<FeatureDependencyDescriptor> = mutableListOf()
    val configs: MutableSet<Config> = mutableSetOf()
    val configFiles: MutableSet<ConfigFile> = mutableSetOf()
    val capabilities: MutableList<Capability> = mutableListOf()

    var includeProject: Boolean = false

    // *************************************************************************
    // Bundles
    // *************************************************************************

    @JvmOverloads
    fun bundle(pattern: String, action: Action<in BundleDescriptor>? = null) {
        val descriptor = BundleDescriptor.fromPattern(pattern)
        action?.execute(descriptor)

        bundleDescriptors += descriptor
    }

    fun bundle(action: Action<in BundleDescriptor>) {
        bundleDescriptors += BundleExtendedDescriptor().also { action.execute(it) }
    }

    fun findBundleDescriptors(artifact: ResolvedArtifact): BundleDescriptor? {
        return this.bundleDescriptors.firstOrNull {
            it.matcher.matches(
                artifact.moduleVersion.id.group,
                artifact.moduleVersion.id.name,
                artifact.moduleVersion.id.version,
                artifact.type,
                artifact.classifier
            )
        }
    }

    fun findBundleDescriptors(dependency: DependencyDescriptor): BundleDescriptor? {
        return this.bundleDescriptors.firstOrNull {
            it.matcher.matches(dependency.dependency)
        }
    }

    fun findBundleDescriptors(identifier: ModuleVersionIdentifier): BundleDescriptor? {
        return this.bundleDescriptors.firstOrNull {
            it.matcher.matches(
                identifier.group,
                identifier.name,
                identifier.version,
                null,
                null
            )
        }
    }

    // *************************************************************************
    // Features
    // *************************************************************************

    fun features(featureNames: Collection<String>) {
        this.featureDependencies.clear()

        featureNames.forEach {
            feature(it, null)
        }
    }

    @JvmOverloads
    fun feature(feature: FeatureDescriptor, action: Action<in FeatureDependencyDescriptor>? = null) {
        feature(feature.name, action)
    }

    @JvmOverloads
    fun feature(featureName: String, action: Action<in FeatureDependencyDescriptor>? = null) {
        val featureDependencyDescriptor = FeatureDependencyDescriptor(featureName)
        action?.execute(featureDependencyDescriptor)

        featureDependencies += featureDependencyDescriptor
    }

    // *************************************************************************
    // Config/ConfigFile
    // *************************************************************************

    fun config(name: String, action: Action<in Config>) {
        configs += Config(name).also { action.execute(it) }
    }

    fun configFile(uri: String, fileName: String, action: Action<in ConfigFile>? = null) {
        val configFile = ConfigFile(uri, fileName)
        action?.execute(configFile)
        configFiles += configFile
    }

    fun configFile(uri: String, fileName: String, override: Boolean, action: Action<in ConfigFile>? = null) {
        val configFile = ConfigFile(uri, fileName, override)
        action?.execute(configFile)
        configFiles += configFile
    }

    fun configFile(action: Action<in ConfigFile>) {
        configFiles += ConfigFile().also { action.execute(it) }
    }

    // *************************************************************************
    // Capabilities
    // *************************************************************************

    fun capability(ns: String, action: Action<in Capability>) {
        capabilities += Capability(ns).also { action.execute(it) }
    }

    // *************************************************************************
    //
    // *************************************************************************

    @ToString(includeNames = true)
    @EqualsAndHashCode(includes = ["name"])
    data class Config(val name: String) {
        var append: Boolean = false
        var content: String? = null
    }

    @ToString(includeNames = true)
    @EqualsAndHashCode(includes = ["uri", "filename", "override"])
    data class ConfigFile(var uri: String? = null, var filename: String? = null, var override: Boolean? = null) {
        var file: File? = null
    }

    @ToString(includeNames = true)
    @EqualsAndHashCode(includes = ["ns"])
    data class Capability(val ns: String) {
        var effective: String? = "resolve"
        var filter: String? = null
        var resolution: String? = "mandatory"
        var extra: String? = null

        fun format(): String {
            val text = StringBuilder(ns)
            if (filter != null) {
                text.append(";filter:='").append(filter).append("'")
            }
            if (effective != null) {
                text.append(";effective:='").append(effective).append("'")
            }
            if (resolution != null) {
                text.append(";resolution:='").append(resolution).append("'")
            }
            if (extra != null) {
                text.append(";").append(extra)
            }

            return text.toString();
        }
    }

    class BundleExtendedDescriptor : BundleDescriptor(DependencyMatcher()) {
        fun match(action: Action<in DependencyMatcher>) {
            action.execute(matcher)
        }
    }
}

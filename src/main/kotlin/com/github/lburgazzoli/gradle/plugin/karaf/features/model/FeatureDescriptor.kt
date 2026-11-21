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

import org.gradle.api.Action
import com.github.lburgazzoli.gradle.plugin.karaf.features.KarafFeaturesExtension
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import javax.inject.Inject

/**
 * @author lburgazzoli
 */
abstract class FeatureDescriptor @Inject constructor(
    val project: Project,
    /**
     * The features settings this feature belongs to. The project bundle is included when either their
     * `includeProject` or the feature's own is `true`.
     */
    val owner: KarafFeaturesExtension
) : FeatureDefinition() {
    var name: String? = null
    var version: String? = project.version.toString()
    var description: String? = null
    var details: String? = null

    val configurations: MutableList<Configuration> = mutableListOf()
    val conditions: MutableList<FeatureCondition> = mutableListOf()

    init {
        project.configurations.findByName("runtimeClasspath")?.let { configurations.add(it) }
    }

    inner class ConfigurationsScope {
        fun add(configurationName: String) {
            configuration(configurationName)
        }

        fun add(configuration: Configuration) {
            configuration(configuration)
        }

        fun del(configurationName: String) {
            del(project.configurations.getByName(configurationName))
        }

        fun del(configuration: Configuration) {
            configurations.remove(configuration)
        }
    }

    fun configurations(action: Action<in ConfigurationsScope>) {
        configurations.clear()
        action.execute(ConfigurationsScope())
    }

    fun configuration(configurationName: String) {
        configuration(project.configurations.getByName(configurationName))
    }

    fun configuration(configuration: Configuration) {
        configurations.add(configuration)
    }

    fun configurations(vararg configurationNames: String) {
        configurations(configurationNames.map { project.configurations.getByName(it) })
    }

    fun configurations(vararg configurations: Configuration) {
        this.configurations(configurations.toList())
    }

    fun configurations(configurations: Collection<Configuration>) {
        this.configurations.clear()
        this.configurations.addAll(configurations)
    }

    // *************************************************************************
    // Conditions
    // *************************************************************************

    fun conditional(action: Action<in FeatureCondition>) {
        conditions.add(objects.newInstance(FeatureCondition::class.java).also { action.execute(it) })
    }

    fun conditional(condition: String, action: Action<in FeatureCondition>) {
        conditions.add(objects.newInstance(FeatureCondition::class.java).also {
            it.condition = condition
            action.execute(it)
        })
    }

    fun isConditional(dependency: DependencyDescriptor): Boolean =
        this.conditions.any { condition ->
            condition.bundleDescriptors.any { instruction ->
                instruction.matcher.matches(dependency.dependency)
            }
        }

    override fun toString(): String = "FeatureDescriptor(name=$name, version=$version)"
}

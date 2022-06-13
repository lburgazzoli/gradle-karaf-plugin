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

import groovy.transform.ToString
import org.gradle.api.Action
import org.gradle.api.Named
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.provider.ListProperty
import javax.inject.Inject

/**
 * @author lburgazzoli
 */
@ToString(includeNames = true)
abstract class FeatureDescriptor @Inject constructor(
    private var name: String
) : FeatureDefinition(), Named {
    var version: Any? = null
    var description: String? = null
    var details: String? = null
    internal lateinit var _project: Project
    internal val projectIsInitialized: Boolean get() = this::_project.isInitialized

    abstract val configurations: ListProperty<Configuration>
    abstract val conditions: ListProperty<FeatureCondition>

    class ConfigurationsScope(
        private val feature: FeatureDescriptor
    ) {
        fun add(configurationName: String) {
            feature.configuration(configurationName)
        }

        fun add(configuration: Configuration) {
            feature.configuration(configuration)
        }
    }

    fun configurations(action: Action<in ConfigurationsScope>) {
        configurations.empty()
        action.execute(ConfigurationsScope(this))
    }

    fun configuration(configurationName: String) {
        configuration(_project.configurations.getByName(configurationName))
    }

    fun configuration(configuration: Configuration) {
        configurations.add(configuration)
    }

    fun configurations(vararg configurationNames: String) {
        configurations(configurationNames.map { _project.configurations.getByName(it) })
    }

    fun configurations(vararg configurations: Configuration) {
        this.configurations(configurations.toList())
    }

    fun configurations(configurations: Collection<Configuration>) {
        this.configurations.empty()
        this.configurations.addAll(configurations)
    }

    // *************************************************************************
    // Conditions
    // *************************************************************************

    fun conditional(action: Action<in FeatureCondition>) {
        conditions.add(FeatureCondition().also { action.execute(it) })
    }

    fun conditional(condition: String, action: Action<in FeatureCondition>) {
        conditions.add(FeatureCondition(condition).also { action.execute(it) })
    }

    override fun getName(): String {
        return name
    }

    fun setName(name: String) {
        this.name = name
    }

    fun isConditional(dependency: DependencyDescriptor): Boolean =
        this.conditions.get().any { condition ->
            condition.bundleDescriptors.any { instruction ->
                instruction.matcher.matches(dependency.dependency)
            }
        }
}

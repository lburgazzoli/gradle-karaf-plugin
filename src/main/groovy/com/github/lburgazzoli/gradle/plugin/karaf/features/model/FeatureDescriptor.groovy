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
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.util.ConfigureUtil

/**
 * @author lburgazzoli
 */
@ToString(includeNames = true)
class FeatureDescriptor extends FeatureDefinition {
    final Project project

    String name
    String version
    String description
    String details

    final List<Configuration> configurations
    final List<FeatureCondition> conditions

    FeatureDescriptor(Project project) {
        this.project = project
        this.name = null
        this.version = project.version
        this.description = null
        this.details = null
        this.configurations = []
        this.conditions = []

        // some known configurationa re added by default
        Configuration implementation = project.configurations.findByName("implementation")
        if(implementation) {
            this.configurations << implementation
        }
        Configuration runtimeOnly = project.configurations.findByName("runtimeOnly")
        if(runtimeOnly) {
            this.configurations << runtimeOnly
        }
    }

    // *************************************************************************
    // Configurations
    // *************************************************************************

    void configurations(Closure closure) {
        this.configurations.clear()

        ConfigureUtil.configure(
            closure,
            new ConfigurationsHelper()
        );
    }

    void configurations(String... configurationNames) {
        configurations(configurationNames.collect {
            this.project.configurations.getByName(it)
        })
    }

    void configuration(Configuration configuration) {
        if (configuration) {
            this.configurations << configuration
        }
    }

    void configurations(Configuration... configurations) {
        this.configurations(configurations.collect())
    }

    void configurations(Collection<Configuration> configurations) {
        this.configurations.clear()
        this.configurations.addAll(configurations)
    }

    void configuration(String configurationName) {
        configuration(this.project.configurations.getByName(configurationName))
    }

    // *************************************************************************
    // Conditions
    // *************************************************************************

    void conditional(Closure closure) {
        this.conditions << ConfigureUtil.configure(
            closure,
            new FeatureCondition()
        )
    }

    void conditional(String condition, Closure closure) {
        this.conditions << ConfigureUtil.configure(
            closure,
            new FeatureCondition(condition)
        )
    }

    boolean isConditional(DependencyDescriptor dependency) {
        return this.conditions.find {
            condition -> condition.bundleDescriptors.find {
                instruction -> instruction.matcher.matches(dependency)
            }
        } != null
    }

    // *************************************************************************
    //
    // *************************************************************************

    class ConfigurationsHelper {

        def add(String configurationName) {
            add(project.configurations.getByName(configurationName))
        }

        def add(Configuration configuration) {
            FeatureDescriptor.this.configuration(configuration)
        }

        def del(String configurationName) {
            del(project.configurations.getByName(configurationName))
        }

        def del(Configuration configuration) {
            configurations.remove(configuration)
        }
    }
}

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

    List<Configuration> configurations
    List<FeatureCondition> conditions

    public FeatureDescriptor(Project project) {
        this.project = project
        this.name = null
        this.version = project.version
        this.description = null
        this.details = null
        this.configurations = []
        this.conditions = []

        // If "runtime" configuration exists add it as default
        Configuration runtime = project.configurations.findByName("runtime")
        if(runtime) {
            this.configurations << runtime
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

    void configuration(Configuration configuration) {
        if (configuration) {
            this.configurations << configuration
        }
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

    boolean isConditional(DependencyDescriptor dependency) {
        return this.conditions.find {
            condition -> condition.bundleInstructions.find {
                instruction -> instruction.matches(dependency)
            }
        } != null
    }

    // *************************************************************************
    //
    // *************************************************************************

    class ConfigurationsHelper {

        public add(String configurationName) {
            configuration(project.configurations.getByName(configurationName))
        }

        public add(Configuration configuration) {
            configuration(configuration)
        }
    }
}

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

import org.gradle.api.Project
import org.gradle.api.tasks.OutputFile
import org.gradle.util.ConfigureUtil

import com.github.lburgazzoli.gradle.plugin.karaf.features.model.DependencyResolver
import com.github.lburgazzoli.gradle.plugin.karaf.features.model.DependencyResolverMvn
import com.github.lburgazzoli.gradle.plugin.karaf.features.model.FeatureDescriptor

/**
 * @author lburgazzoli
 */
class KarafFeaturesExtension {
    public static final String DEFAULT_XSD_VERSION = "1.2.0"

    private final Project project
    private final DependencyResolver resolver
    private final def Set<String> repositories
    private final List<FeatureDescriptor> featureDescriptors;

    String name
    String xsdVersion

    @OutputFile
    private File outputFile

    KarafFeaturesExtension(Project project) {
        this.project = project
        this.name = project.name
        this.resolver = new DependencyResolverMvn()
        this.xsdVersion = DEFAULT_XSD_VERSION
        this.repositories = []
        this.outputFile = null
        this.featureDescriptors = []
    }

    def feature(Closure closure) {
        featureDescriptors << ConfigureUtil.configure(
            closure,
            new FeatureDescriptor(project)
        )
    }

    def getFeatureDescriptors() {
        return this.featureDescriptors
    }

    def DependencyResolver getResolver() {
        return this.resolver
    }

    void setOutputFile(String file) {
        this.outputFile = new File(file)
    }

    void setOutputFile(File file) {
        this.outputFile = file
    }

    File getOutputFile() {
        if(outputFile == null) {
            def path = "${project.buildDir}/karaf-feautures"
            def name = "${name}-${project.version}.xml"

            outputFile = new File(path, name)
        }

        return outputFile
    }
}

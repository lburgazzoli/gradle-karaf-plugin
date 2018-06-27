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

import java.nio.file.Path
import org.gradle.api.Project
import org.gradle.api.tasks.OutputFile
import org.gradle.util.ConfigureUtil

import com.github.lburgazzoli.gradle.plugin.karaf.features.model.DependencyResolver
import com.github.lburgazzoli.gradle.plugin.karaf.mvn.MvnDependencyResolver
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
    String group
    String version
    String xsdVersion
    Boolean includeProject

    private File outputFile

    KarafFeaturesExtension(Project project) {
        this.project = project
        this.name = project.name
        this.group = project.group
        this.version = project.version
        this.resolver = new MvnDependencyResolver()
        this.xsdVersion = DEFAULT_XSD_VERSION
        this.repositories = []
        this.featureDescriptors = []

        this.outputFile = new File(
            "${project.buildDir}/karaf/features",
            "${name}-${version}.xml"
        )
    }

    def feature(Closure closure) {
        featureDescriptors << ConfigureUtil.configure(
            closure,
            new FeatureDescriptor(project)
        )
    }

    def repository(String repository) {
        repositories << repository
    }

    def getRepositories() {
        return this.repositories
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

    @OutputFile
    File getOutputFile() {
        return outputFile
    }

    Path getOutputPath() {
        return outputFile.toPath()
    }
}

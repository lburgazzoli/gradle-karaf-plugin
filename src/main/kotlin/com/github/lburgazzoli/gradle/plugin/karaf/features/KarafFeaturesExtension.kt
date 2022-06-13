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

import com.github.lburgazzoli.gradle.plugin.karaf.features.model.DependencyResolver
import com.github.lburgazzoli.gradle.plugin.karaf.features.model.FeatureDescriptor
import com.github.lburgazzoli.gradle.plugin.karaf.mvn.MvnDependencyResolver
import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.newInstance
import javax.inject.Inject

/**
 * @author lburgazzoli
 */
abstract class KarafFeaturesExtension(private val project: Project) {
    companion object {
        const val DEFAULT_XSD_VERSION = "1.2.0"
        const val NAME = "features"
    }

    val resolver: DependencyResolver = MvnDependencyResolver()

    @get:Inject
    protected abstract val objects: ObjectFactory

    abstract val repositories: SetProperty<String>
    abstract val outputFile: RegularFileProperty

    //    val featureDescriptors = mutableListOf<FeatureDescriptor>()
    abstract val featureDescriptors: NamedDomainObjectContainer<FeatureDescriptor>

    abstract val name: Property<String>
    abstract val group: Property<Any>
    abstract val version: Property<Any>
    abstract val xsdVersion: Property<String>
    abstract val includeProject: Property<Boolean>

    init {
        xsdVersion.convention(DEFAULT_XSD_VERSION)
        name.convention(project.name)
        group.convention(project.group)
        version.convention(project.version)
        val layout = project.layout
        outputFile.convention(
            name.flatMap { layout.buildDirectory.file("karaf/features/${it}-${version.get()}-features.xml") }
        )
        val prj = project
        featureDescriptors.configureEach {
            if (!projectIsInitialized) {
                this._project = prj
                version = prj.version
                _project.configurations.findByName("runtimeClasspath")?.let { configurations.add(it) }
            }
        }
    }

    fun repository(name: String) {
        repositories.add(name)
    }

    fun feature(action: Action<in FeatureDescriptor>) {
        val feature = objects.newInstance(FeatureDescriptor::class, "").apply {
            _project = project
            version = project.version
            _project.configurations.findByName("runtimeClasspath")?.let { configurations.add(it) }
        }
        action.execute(feature)
        featureDescriptors.add(feature)
    }
}

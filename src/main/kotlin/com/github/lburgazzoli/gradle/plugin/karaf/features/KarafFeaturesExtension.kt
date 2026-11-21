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
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import java.io.File
import java.nio.file.Path
import javax.inject.Inject

/**
 * Settings of the features file.
 *
 * [name], [group], [version], and [outputFile] fall back to the project's values when they are read, so a build
 * script that sets `version` after applying the plugin still gets that version.
 *
 * @author lburgazzoli
 */
abstract class KarafFeaturesExtension @Inject constructor(private val project: Project) {
    companion object {
        const val DEFAULT_XSD_VERSION = "1.2.0"
        const val NAME = "features"
    }

    val resolver: DependencyResolver = MvnDependencyResolver()

    @get:Inject
    protected abstract val objects: ObjectFactory

    val repositories: MutableSet<String> = linkedSetOf()

    val featureDescriptors: List<FeatureDescriptor>
        get() = _featureDescriptors

    private val _featureDescriptors = mutableListOf<FeatureDescriptor>()

    private var explicitName: String? = null
    private var explicitGroup: String? = null
    private var explicitVersion: String? = null
    private var explicitOutputFile: File? = null

    var name: String
        get() = explicitName ?: project.name
        set(value) {
            explicitName = value
        }

    var group: String
        get() = explicitGroup ?: project.group.toString()
        set(value) {
            explicitGroup = value
        }

    var version: String
        get() = explicitVersion ?: project.version.toString()
        set(value) {
            explicitVersion = value
        }

    var xsdVersion: String = DEFAULT_XSD_VERSION

    var includeProject: Boolean? = null

    /**
     * The features file to write; `build/karaf/features/<name>-<version>-features.xml` unless set.
     */
    var outputFile: File
        get() = explicitOutputFile
            ?: project.layout.buildDirectory.file("karaf/features/$name-$version-features.xml").get().asFile
        set(value) {
            explicitOutputFile = value
        }

    /**
     * Sets [outputFile] to [path], resolved against the project directory.
     */
    fun setOutputFile(path: String) {
        outputFile = project.file(path)
    }

    val outputPath: Path
        get() = outputFile.toPath()

    fun repository(name: String) {
        repositories.add(name)
    }

    fun feature(action: Action<in FeatureDescriptor>) {
        val feature = objects.newInstance(FeatureDescriptor::class.java, project, this)
        action.execute(feature)
        _featureDescriptors += feature
    }
}

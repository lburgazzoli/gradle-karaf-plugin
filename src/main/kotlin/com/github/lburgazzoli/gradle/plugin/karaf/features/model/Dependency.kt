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

import com.github.lburgazzoli.gradle.plugin.karaf.features.KarafFeaturesUtils
import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.artifacts.result.ResolvedComponentResult
import java.io.File

/**
 * @author lburgazzoli
 */
@ToString(includeNames = true)
@EqualsAndHashCode(includes = ["group", "name", "version", "type", "classifier"])
data class Dependency(
    var group: String?,
    var name: String?,
    var version: String?,
    var type: String?,
    var classifier: String?,
    var file: File?,
    var kind: Kind,
    var url: String?
) {
    @ToString(includeNames = true)
    enum class Kind {
        UNKNOWN,
        MODULE,
        PROJECT
    }

    constructor(kind: Kind = Kind.UNKNOWN) : this(
        group = null,
        name = null,
        version = null,
        type = null,
        classifier = null,
        file = null,
        kind = kind,
        url = null
    )

    constructor(component: ResolvedComponentResult, type: String, file: File) :
            this(component, null, type, file)

    constructor(component: ResolvedComponentResult, classifier: String?, type: String?, file: File) : this(
        group = component.moduleVersion?.group,
        name = component.moduleVersion?.name,
        version = component.moduleVersion?.version,
        classifier = classifier,
        type = type,
        file = file,
        kind = when (component.id) {
            is ProjectComponentIdentifier -> Kind.PROJECT
            is ModuleComponentIdentifier -> Kind.MODULE
            else -> Kind.UNKNOWN
        },
        url = null
    )

    val isProject: Boolean get() = kind == Kind.PROJECT

    val isModule: Boolean get() = kind == Kind.MODULE

    val isJar: Boolean get() = type == "jar"

    val isWar: Boolean get() = type == "war"

    val isOSGi: Boolean get() = KarafFeaturesUtils.hasOsgiManifestHeaders(file)

    val isResolved: Boolean get() = url != null

    val hasClassifier: Boolean get() = !classifier.isNullOrEmpty()
}

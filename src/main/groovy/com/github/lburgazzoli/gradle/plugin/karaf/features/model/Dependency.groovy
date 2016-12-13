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
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.artifacts.result.ResolvedComponentResult

import com.github.lburgazzoli.gradle.plugin.karaf.features.KarafFeaturesUtils
/**
 * @author lburgazzoli
 */
@ToString(includeNames = true)
@EqualsAndHashCode(includes = [ "group", "name", "version", "type", "classifier"])
class Dependency {
    @ToString(includeNames = true)
    public enum Kind {
        UNKNOWN,
        MODULE,
        PROJECT
    }

    String group
    String name
    String version
    String type
    String classifier
    File file
    Kind kind
    String url

    Dependency() {
        this.group = null
        this.name = null
        this.version = null
        this.type = null
        this.classifier = null
        this.file = null
        this.kind = Kind.UNKNOWN
        this.url = null
    }

    Dependency(ResolvedComponentResult component, String type, File file) {
        this(component, null, type, file)
    }

    Dependency(ResolvedComponentResult component, String classifier, String type, File file) {
        this.group = component.moduleVersion.group
        this.name = component.moduleVersion.name
        this.version = component.moduleVersion.version
        this.classifier = classifier
        this.type = type
        this.file = file

        if(component.id instanceof ProjectComponentIdentifier) {
            this.kind = Kind.PROJECT
        } else if(component.id instanceof ModuleComponentIdentifier) {
            this.kind = Kind.MODULE
        } else {
            this.kind = Kind.UNKNOWN
        }
    }

    boolean isProject() {
        return this.kind == Kind.PROJECT
    }

    boolean isModule() {
        return this.kind == Kind.MODULE
    }

    boolean isJar() {
        return this.type && this.type.equals('jar')
    }

    boolean isWar() {
        return this.type && this.type.equals('war')
    }

    boolean isOSGi() {
        return KarafFeaturesUtils.hasOsgiManifestHeaders(file)
    }

    boolean isResolved() {
        return this.url != null
    }

    boolean hasClassifier() {
        return this.classifier != null
    }
}
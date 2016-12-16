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
import org.gradle.api.IllegalDependencyNotation

/**
 * @author Steve Ebersole
 * @author Luca Burgazzoli
 */
@ToString(includeNames = true)
class DependencyMatcher {
    String group
    String name
    String version
    String type
    String classifier

    DependencyMatcher(String group, String name, String version, String type, String classifier) {
        this.group = group
        this.name = name
        this.version = version
        this.type = type
        this.classifier = classifier
    }

    boolean matches(Dependency dependency) {
        return this.matches(
            dependency.group,
            dependency.name,
            dependency.version,
            dependency.type,
            dependency.classifier
        )
    }

    boolean matches(String group, String name, String version, String type, String classifier) {
        if (this.group && this.group != group) {
            return false
        }
        if (this.name && this.name != name) {
            return false
        }
        if (this.version && this.version != version) {
            return false
        }
        if (this.type && this.type != type) {
            return false
        }
        if (this.classifier && this.classifier != classifier) {
            return false
        }

        return true
    }

    static DependencyMatcher from(String notation) {
        String type = null
        if (notation.contains('@')) {
            String[] fields = notation.split('@')
            notation = fields[0]
            type = fields[1]
        }

        String[] notationParts = notation.split(":")
        if (notationParts.length < 1 || notationParts.length > 4) {
            throw new IllegalDependencyNotation(
                "Supplied String module notation '${notation}' is invalid.")
        }

        return new DependencyMatcher(
            notationParts.length >= 1 ? notationParts[0] : null,
            notationParts.length >= 2 ? notationParts[1] : null,
            notationParts.length >= 3 ? notationParts[2] : null,
            type,
            notationParts.length == 4 ? notationParts[3] : null)
    }
}

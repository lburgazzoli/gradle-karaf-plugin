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
import org.gradle.api.artifacts.ModuleVersionIdentifier
import org.gradle.api.artifacts.result.ResolvedComponentResult

/**
 * @author Steve Ebersole
 * @author Luca Burgazzoli
 */
@ToString(includeNames = true)
public class DependencyMatcher {
	String group
    String name
    String version

	public DependencyMatcher(String group, String name, String version) {
        this.group = group
        this.name = name
        this.version = version
	}

	public boolean matches(ResolvedComponentResult resolvedComponent) {
		return matches( resolvedComponent.moduleVersion )
	}

	public boolean matches(ModuleVersionIdentifier check) {
		return ( check.group.equals( group )
            && ( name == null || check.name.equals( name ) )
            && ( version == null || check.version.equals( version ) )
        )
	}

	public boolean matches(DependencyDescriptor check) {
		return ( check.group.equals( group )
			&& ( name == null || check.name.equals( name ) )
			&& ( version == null || check.version.equals( version ) )
		)
	}

	public static DependencyMatcher from(String notation) {
		final String[] notationParts = notation.split(":");
		if (notationParts.length < 1 || notationParts.length > 3) {
			throw new IllegalDependencyNotation(
                "Supplied String module notation '${notation}' is invalid.");
		}

        return new DependencyMatcher(
            notationParts.length >= 1 ? notationParts[0] : null,
            notationParts.length >= 2 ? notationParts[1] : null,
            notationParts.length == 3 ? notationParts[2] : null)
	}
}

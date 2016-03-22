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

import org.gradle.api.artifacts.ModuleVersionIdentifier
import org.gradle.util.ConfigureUtil

/**
 * DSL extension allowing instruction on how to produce a {@code <bundle/>} entry
 * in a Karaf features repository file
 *
 * @author Steve Ebersole
 * @author Luca Burgazzoli
 */
class BundleDescriptor {
	private DependencyMatcher matcher
    private Dependency remap

	boolean include
    boolean wrap

    Map<String, String> attributes
    Map<String, String> instructions

    protected BundleDescriptor(DependencyMatcher matcher) {
        this.matcher = matcher
        this.remap = null
        this.include = true
        this.wrap = false
        this.attributes = new HashMap<>()
        this.instructions = new HashMap<>()
    }

	DependencyMatcher getMatcher() {
		return matcher
	}

    boolean matches(Dependency dependency) {
        return matcher.matches(dependency)
    }

    boolean matches(ModuleVersionIdentifier identifier) {
        return matcher.matches(identifier)
    }

    // *************************************************************************
    // Shortcuts for attributes/instructions
    // *************************************************************************

    void attribute(String key, String value) {
        this.attributes[ key ] = value
    }

    void attributes(Map<String, String> instructions) {
        this.attributes.clear()
        this.attributes.putAll(instructions)
    }

    void instruction(String key, String value) {
        this.instructions[ key ] = value
    }

    void instructions(Map<String, String> instructions) {
        this.instructions.clear()
        this.instructions.putAll(instructions)
    }

    // *************************************************************************
    // Remap
    // *************************************************************************

	def remap(Closure closure) {
		remap = ConfigureUtil.configure(
            closure,
            new Dependency()
        )
	}

    def remap(Map properties) {
		remap = ConfigureUtil.configureByMap(
            properties,
            new Dependency()
        )
	}

    boolean hasRemap() {
        return this.remap != null;
    }

    DependencyDescriptor getRemap() {
		return remap
	}

    // *************************************************************************
    // Wrap
    // *************************************************************************

	def wrap(Closure closure) {
        this.wrap = true

		ConfigureUtil.configure(
            closure,
            new WrapInstructionsHelper()
        )
	}

    private class WrapInstructionsHelper {
        public void attribute(String key, String value) {
            instruction(key, value);
        }

        public void attribute(Map<String, String> instructions) {
            instruction(instructions)
        }
    }

    // *************************************************************************
    // Helpers
    // *************************************************************************

    static BundleDescriptor fromPattern(String pattern) {
        return new BundleDescriptor(DependencyMatcher.from(pattern))
    }
}

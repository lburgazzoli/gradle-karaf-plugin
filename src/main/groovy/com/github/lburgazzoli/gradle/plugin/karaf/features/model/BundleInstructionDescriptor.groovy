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
class BundleInstructionDescriptor {
	private DependencyMatcher matcher
    private DependencyDescriptor remap

	boolean include
	String startLevel
	boolean dependency
    boolean wrap

    final Map<String, String> wrapAttributes

    protected BundleInstructionDescriptor(DependencyMatcher matcher) {
        this.matcher = matcher
        this.remap = null
        this.include = true
        this.startLevel = null
        this.dependency = false
        this.wrap = false
    }

	DependencyMatcher getMatcher() {
		return matcher
	}

    boolean matches(DependencyDescriptor dependency) {
        return matcher.matches(dependency)
    }

    boolean matches(ModuleVersionIdentifier identifier) {
        return matcher.matches(identifier)
    }

    // *************************************************************************
    // Shortcuts for attributes
    // *************************************************************************

    void attribute(String key, String value) {
        getOrCreateRemap().attribute(key, value)
    }

    void attributes(Map<String, String> attributes) {
        getOrCreateRemap().attributes(attributes)
    }

    // *************************************************************************
    // Remap
    // *************************************************************************

	def remap(Closure closure) {
		ConfigureUtil.configure(closure, getOrCreateRemap())
	}

    def remap(Map properties) {
		ConfigureUtil.configureByMap(properties, getOrCreateRemap())
	}

    boolean hasRemap() {
        return this.remap != null;
    }

    DependencyDescriptor getRemap() {
		return remap
	}

    DependencyDescriptor getOrCreateRemap() {
        if (remap == null) {
            remap = new DependencyDescriptor();
        }

        return remap;
    }

    // *************************************************************************
    // Wrap
    // *************************************************************************

	def wrap(Closure closure) {
        this.wrap = true

        getOrCreateRemap();

		ConfigureUtil.configure(
            closure,
            new WrapInstructionsHelper()
        )
	}

    private class WrapInstructionsHelper {
        public void attribute(String key, String value) {
            getOrCreateRemap().attribute(key, value);
        }

        public void attribute(Map<String, String> instructions) {
            getOrCreateRemap().attributes(instructions)
        }
    }

    // *************************************************************************
    // Helpers
    // *************************************************************************

    static  BundleInstructionDescriptor fromPattern(String pattern) {
        return new BundleInstructionDescriptor(DependencyMatcher.from(pattern))
    }
}

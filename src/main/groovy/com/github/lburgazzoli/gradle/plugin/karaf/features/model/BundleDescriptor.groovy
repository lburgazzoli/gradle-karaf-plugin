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
    private Closure remap

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

	def remap(final Closure closure) {
		remap = {
            dependency -> ConfigureUtil.configure(closure, dependency)
        }
	}

    def remap(final Map properties) {
		remap = {
            dependency -> ConfigureUtil.configureByMap(properties, dependency)
        }
	}

    boolean hasRemap() {
        return this.remap != null
    }

    Closure getRemap() {
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
        void attribute(String key, String value) {
            instruction(key, value)
        }

        void attribute(Map<String, String> instructions) {
            instruction(instructions)
        }

        void instruction(String key, String value) {
            BundleDescriptor.this.instruction(key, value)
        }

        void instruction(Map<String, String> instructions) {
            BundleDescriptor.this.instruction(instructions)
        }
    }

    // *************************************************************************
    // Helpers
    // *************************************************************************

    static BundleDescriptor fromPattern(String pattern) {
        return new BundleDescriptor(DependencyMatcher.from(pattern))
    }
}

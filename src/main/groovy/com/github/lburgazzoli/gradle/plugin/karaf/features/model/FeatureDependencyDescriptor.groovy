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

/**
 *
 * @author Steve Ebersole
 * @author Sergey Nekhviadovich
 */
class FeatureDependencyDescriptor {
	/**
	 * Dependency feature name
	 */
	def String name

	/**
	 * Dependency feature version, will be skipped if unset
	 */
	def String version

	/**
	 * Dependency feature dependency flag. Available only for karaf 4+ and feature xsd version 1.3.0+
	 */
	def boolean dependency

	FeatureDependencyDescriptor() {
		this(null)
	}

	FeatureDependencyDescriptor(String name) {
	    this.name = name
        this.version = null
        this.dependency = false
	}
}

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
package com.github.lburgazzoli.gradle.plugin.karaf

import com.github.lburgazzoli.gradle.plugin.karaf.features.model.FeaturesDescriptor
import org.gradle.api.Project
import org.gradle.util.ConfigureUtil
/**
 * @author lburgazzoli
 */
class KarafPluginExtension {
    public static final String NAME = 'karaf'

    private final Project project
    private FeaturesDescriptor features

    public KarafPluginExtension(Project project) {
        this.project = project
    }

    def features(Closure closure) {
        features = ConfigureUtil.configure(
            closure,
            new FeaturesDescriptor(this.project)
        )
    }

    def FeaturesDescriptor getFeatures() {
        return features
    }

    // *************************************************************************
    // Helpers
    // *************************************************************************

    public static KarafPluginExtension lookup(Project project) {
        return project.extensions.findByName(NAME) as KarafPluginExtension
    }

    public static KarafPluginExtension create(Project project) {
        return project.extensions.create( NAME, KarafPluginExtension, project )
    }
}

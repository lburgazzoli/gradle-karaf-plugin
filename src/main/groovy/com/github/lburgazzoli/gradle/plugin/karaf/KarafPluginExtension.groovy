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

import com.github.lburgazzoli.gradle.plugin.karaf.distribution.KarafDistributionExtension
import org.gradle.api.Project
import org.gradle.util.ConfigureUtil

import com.github.lburgazzoli.gradle.plugin.karaf.features.KarafFeaturesExtension
import com.github.lburgazzoli.gradle.plugin.karaf.kar.KarafKarExtension

/**
 * @author lburgazzoli
 */
class KarafPluginExtension {
    public static final String NAME = 'karaf'

    private final Project project
    private KarafFeaturesExtension features
    private KarafKarExtension kar
    private KarafDistributionExtension distribution

    KarafPluginExtension(Project project) {
        this.project = project
        this.features = null
        this.kar = null
        this.distribution = null
    }

    // *************************************************************************
    // Features
    // *************************************************************************

    def features(Closure closure) {
        ConfigureUtil.configure(closure, getFeatures())
    }

    KarafFeaturesExtension getFeatures() {
        if (!this.features) {
            this.features = new KarafFeaturesExtension(project)
        }

        return this.features
    }

    boolean hasFeatures() {
        return !getFeatures().featureDescriptors.empty
    }

    // *************************************************************************
    // Kar
    // *************************************************************************

    def kar(Closure closure) {
        getKar()

        kar.enabled = true
        kar = ConfigureUtil.configure(closure, kar)
    }

    KarafKarExtension getKar() {
        if (!this.kar) {
            this.kar = new KarafKarExtension(project)
        }

        return this.kar
    }

    boolean hasKar() {
        return getKar().enabled
    }

    // *************************************************************************
    // Distribution
    // *************************************************************************

    def distribution(Closure closure) {
        getKar()

        kar.enabled = true
        kar = ConfigureUtil.configure(closure, kar)
    }

    KarafDistributionExtension getDistribution() {
        if (!this.distribution) {
            this.distribution = new KarafDistributionExtension(project)
        }

        return this.distribution
    }

    boolean hasDistribution() {
        return getDistribution().enabled
    }

    // *************************************************************************
    // Helpers
    // *************************************************************************

    static KarafPluginExtension lookup(Project project) {
        return project.extensions.findByName(NAME) as KarafPluginExtension
    }

    static KarafPluginExtension create(Project project) {
        return project.extensions.create(NAME, KarafPluginExtension, project)
    }
}

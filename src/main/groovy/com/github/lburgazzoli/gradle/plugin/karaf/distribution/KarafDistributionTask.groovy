/**
 * Copyright 2017, Luca Burgazzoli and contributors as indicated by the @author tags
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
package com.github.lburgazzoli.gradle.plugin.karaf.distribution

import com.github.lburgazzoli.gradle.plugin.karaf.KarafTaskTrait
import org.apache.karaf.profile.assembly.Builder
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

/**
 * @author lburgazzoli
 */
class KarafDistributionTask extends DefaultTask implements KarafTaskTrait {
    public static final String GROUP = "karaf"
    public static final String NAME = "generateDistribution"
    public static final String DESCRIPTION = "Generates Karaf distribution"

    KarafDistributionTask() {
        // TODO: to be improved
        outputs.upToDateWhen {
            false
        }
    }

    @TaskAction
    void run() {
        def builder = Builder.newInstance()
        builder.localRepository = localRepository()
        builder.mavenRepositories = remoteRepositories()
    }

    // ************************************************************************
    //
    // ************************************************************************

    def remoteRepositories() {
    }

    def localRepository() {
    }
}
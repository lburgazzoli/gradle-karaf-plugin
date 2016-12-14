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

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.result.ResolvedComponentResult
import org.gradle.api.artifacts.result.ResolvedDependencyResult

/**
 * @author lburgazzoli
 */
class KarafUtils {

    static void forEachTask(Project project, List<String> tasks, Closure closure) {
        project.tasks.findAll {
            it.name in tasks
        }.each {
            closure(it)
        }
    }

    static void walkDeps(List<Configuration> configurations, Closure closure) {
        configurations.each {
            Set<ResolvedDependencyResult> alreadyKnownDependencies = new HashSet<>();
            walkDeps(it, it.incoming.resolutionResult.root, alreadyKnownDependencies, closure)
        }
    }

    static void walkDeps(Configuration configuration,
                         ResolvedComponentResult root,
                         Set<ResolvedDependencyResult> alreadyKnownDependencies,
                         Closure closure) {
        root.dependencies.findAll {
            it instanceof ResolvedDependencyResult
        }.collect {
            (ResolvedDependencyResult) it
        }.each {
            def result = (ResolvedDependencyResult) it
            if (!alreadyKnownDependencies.contains(result)) {
                alreadyKnownDependencies.add(result)
                walkDeps(configuration, result.selected, alreadyKnownDependencies, closure)
            }
        }

        closure(configuration, root)
    }
}

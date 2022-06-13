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

import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.result.ResolvedComponentResult
import org.gradle.api.artifacts.result.ResolvedDependencyResult

/**
 * @author lburgazzoli
 */
object KarafUtils {
    fun walkDeps(
        configurations: List<Configuration>,
        action: (configuration: Configuration, root: ResolvedComponentResult) -> Unit
    ) {
        configurations.forEach {
            val alreadyKnownDependencies = mutableSetOf<ResolvedDependencyResult>();
            walkDeps(it, it.incoming.resolutionResult.root, alreadyKnownDependencies, action)
        }
    }

    fun walkDeps(
        configuration: Configuration,
        root: ResolvedComponentResult,
        alreadyKnownDependencies: MutableSet<ResolvedDependencyResult>,
        action: (configuration: Configuration, root: ResolvedComponentResult) -> Unit
    ) {
        root.dependencies.filterIsInstance<ResolvedDependencyResult>().forEach {
            if (!alreadyKnownDependencies.contains(it)) {
                alreadyKnownDependencies.add(it)
                walkDeps(configuration, it.selected, alreadyKnownDependencies, action)
            }
        }

        action(configuration, root)
    }
}

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
package com.github.lburgazzoli.gradle.plugin.karaf.mvn

import org.ops4j.pax.url.mvn.internal.Parser
import com.github.lburgazzoli.gradle.plugin.karaf.features.model.Dependency

/**
 * @author lburgazzoli
 */
class MvnProtocolParser {

    static Dependency parse(String path) {
        def parser = new Parser(path)

        def dependency = new Dependency()
        dependency.group = parser.group
        dependency.name = parser.artifact
        dependency.version = parser.version
        dependency.type = parser.type ?: dependency.type
        dependency.classifier = parser.classifier ?: dependency.classifier

        return dependency
    }
}

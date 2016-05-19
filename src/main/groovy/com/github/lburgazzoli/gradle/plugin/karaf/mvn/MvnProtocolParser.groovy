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

import com.github.lburgazzoli.gradle.plugin.karaf.features.model.Dependency

/**
 * @author lburgazzoli
 */
class MvnProtocolParser {
    private static final String PROTOCOL_PREFIX = 'mvn:'
    private static final String REPOSITORY_SEPARATOR = '!'
    private static final String ARTIFACT_SEPARATOR = '/'

    static Dependency parse(String path) {
        int pos = path.lastIndexOf(REPOSITORY_SEPARATOR);
        if (pos >= 0) {
            path = path.substring(pos + 1)
        } else if (path.startsWith(PROTOCOL_PREFIX)){
            path = path.substring(PROTOCOL_PREFIX.length())
        }

        String[] segments = path.split(ARTIFACT_SEPARATOR)
        if (segments.length < 3) {
            throw new MalformedURLException("Invalid path (${path})")
        }

        def dependency = new Dependency()
        dependency.group = segments[0].trim()
        dependency.name = segments[1].trim()
        dependency.version = segments[2].trim()

        if (segments.length >= 4) {
            dependency.type = segments[3].trim()
        }
        if (segments.length >= 5) {
            dependency.classifier = segments[4].trim()
        }

        return dependency
    }
}

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
 * @author lburgazzoli
 */
class DependencyResolverMvn extends DependencyResolver {
    @Override
    protected void renderUrl(
        DependencyDescriptor dependencyDescriptor,
        BundleInstructionDescriptor bundleInstructionDescriptor) {

        dependencyDescriptor.url = baseMvnUrl( dependencyDescriptor )

        if (bundleInstructionDescriptor) {
            if(bundleInstructionDescriptor.hasWrapInstructions() ) {
                dependencyDescriptor.url = "wrap:${dependencyDescriptor.url}"

                def sep = '?'
                bundleInstructionDescriptor.wrapInstructions?.instructions.each { key , val ->
                    // do these need to be encoded?
                    dependencyDescriptor.url = "${dependencyDescriptor.url}${sep}${key}=${val}"
                    sep = '&'
                }
            }
        } else if (!dependencyDescriptor.isOSGi() ) {
            // if the resolved file does not have "proper" OSGi headers we
            // implicitly do the wrap as a courtesy...
            dependencyDescriptor.url = "wrap:${dependencyDescriptor.url}"
        }
    }

    /**
     *
     * @param bundleCoordinates
     * @return
     */
    public static String baseMvnUrl(DependencyDescriptor dependencyDescriptor) {
        def gnv = "${dependencyDescriptor.group}/${dependencyDescriptor.name}/${dependencyDescriptor.version}";
        return dependencyDescriptor.isWar() ? "mvn:${gnv}/war" : "mvn:${gnv}"
    }
}

package com.github.lburgazzoli.gradle.plugin.karaf.mvn

import com.github.lburgazzoli.gradle.plugin.karaf.features.model.DependencyDescriptor
import com.github.lburgazzoli.gradle.plugin.karaf.features.model.DependencyResolver

/**
 * @author lburgazzoli
 */
class MvnDependencyResolver : DependencyResolver() {
    companion object {
        /**
         * @param bundleCoordinates
         * @return
         */
        fun baseMvnUrl(dependencyDescriptor: DependencyDescriptor): String {
            val dep = dependencyDescriptor.dependency
            var gnv = "${dep.group}/${dep.name}/${dep.version}"
            if (!dep.classifier.isNullOrEmpty()) {
                gnv = "$gnv/${dep.type ?: ""}/${dep.classifier}"
            }
            return if (dep.isWar) "war:mvn:$gnv/war" else "mvn:$gnv"
        }
    }

    override fun renderUrl(dependency: DependencyDescriptor): String {
        var url = baseMvnUrl(dependency)
        if (dependency.bundle == null) {
            if (!dependency.dependency.isOSGi && !dependency.dependency.isWar) {
                // if the resolved file does not have "proper" OSGi headers we
                // implicitly do the wrap as a courtesy...
                url = "wrap:$url"
            }
        } else {
            if (dependency.bundle.wrap) {
                url = "wrap:$url"
            }
            if (dependency.bundle.instructions.isNotEmpty()) {
                val res = dependency.bundle.instructions
                    .map { "${it.key}=${it.value}" }
                    .joinToString("&")
                url = "$url${if (dependency.dependency.isWar) "?" else "$"}$res"
            }
        }
        return url
    }
}

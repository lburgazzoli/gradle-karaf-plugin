package com.github.lburgazzoli.gradle.plugin.karaf.mvn

import com.github.lburgazzoli.gradle.plugin.karaf.features.model.Dependency
import org.ops4j.pax.url.mvn.internal.Parser

/**
 * @author lburgazzoli
 */
object MvnProtocolParser {
    fun parse(path: String?): Dependency {
        val parser = Parser(path)
        val dependency = Dependency().apply {
            // Parser.getGroup() returns a group prefixed with 'mvn:' which should be removed
            group = parser.group.removePrefix("mvn:")
            name = parser.artifact
            version = parser.version
            type = parser.type?.takeIf { it.isNotEmpty() } ?: type
            classifier = parser.classifier?.takeIf { it.isNotEmpty() } ?: classifier
        }
        return dependency
    }
}

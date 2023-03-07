package com.github.lburgazzoli.gradle.plugin.karaf.features.model

import com.github.lburgazzoli.gradle.plugin.karaf.util.AntPathMatcher
import org.gradle.api.IllegalDependencyNotation
import java.util.regex.Pattern

/**
 * @author Steve Ebersole
 * @author Luca Burgazzoli
 */
class DependencyMatcher(
    var group: String? = null,
    var name: String? = null,
    var version: String? = null,
    var type: String? = null,
    var classifier: String? = null
) {
    companion object {
        fun from(notation: String): DependencyMatcher {
            var s = notation
            var type: String? = null
            if (s.contains("@")) {
                val fields = s.split("@".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                s = fields[0]
                type = fields[1]
            }
            val notationParts = s.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            if (notationParts.size < 1 || notationParts.size > 4) {
                throw IllegalDependencyNotation("Supplied String module notation \'$s\' is invalid.")
            }
            return DependencyMatcher(
                if (notationParts.size >= 1) notationParts[0] else null,
                if (notationParts.size >= 2) notationParts[1] else null,
                if (notationParts.size >= 3) notationParts[2] else null,
                type,
                if (notationParts.size == 4) notationParts[3] else null
            )
        }
    }

    fun matches(dependency: Dependency): Boolean {
        return this.matches(
            dependency.group,
            dependency.name,
            dependency.version,
            dependency.type,
            dependency.classifier
        )
    }

    fun matches(group: String?, name: String?, version: String?, type: String?, classifier: String?): Boolean {
        if (this.group != null && this.group != group && !patternMatch(this.group, group)) {
            return false
        }
        if (this.name != null && this.name != name && !patternMatch(this.name, name)) {
            return false
        }
        if (this.version != null && this.version != version && !patternMatch(this.version, version)) {
            return false
        }
        if (this.type != null && this.type != type && !patternMatch(this.type, type)) {
            return false
        }
        if (this.classifier != null && this.classifier != classifier && !patternMatch(this.classifier, classifier)) {
            return false
        }
        return true
    }

    protected fun patternMatch(pattern: String?, target: String?): Boolean {
        return if (pattern != null && target != null) {
            var rex = false
            var ant = false
            try {
                rex = Pattern.compile(pattern).matcher(target).matches()
            } catch (e: Exception) {
            }
            try {
                ant = AntPathMatcher().match(pattern, target)
            } catch (e: Exception) {
            }
            rex || ant
        } else {
            false
        }
    }
}

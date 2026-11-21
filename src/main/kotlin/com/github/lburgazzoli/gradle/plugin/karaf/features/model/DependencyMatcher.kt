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
        @JvmStatic
        fun from(notation: String): DependencyMatcher {
            var s = notation
            var type: String? = null
            if (s.contains("@")) {
                val fields = javaSplit(s, '@')
                s = fields[0]
                type = fields[1]
            }
            val notationParts = javaSplit(s, ':')
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
        if (!this.group.isNullOrEmpty() && this.group != group && !patternMatch(this.group, group)) {
            return false
        }
        if (!this.name.isNullOrEmpty() && this.name != name && !patternMatch(this.name, name)) {
            return false
        }
        if (!this.version.isNullOrEmpty() && this.version != version && !patternMatch(this.version, version)) {
            return false
        }
        if (!this.type.isNullOrEmpty() && this.type != type && !patternMatch(this.type, type)) {
            return false
        }
        if (!this.classifier.isNullOrEmpty() && this.classifier != classifier && !patternMatch(this.classifier, classifier)) {
            return false
        }
        return true
    }

    protected fun patternMatch(pattern: String?, target: String?): Boolean {
        return if (!pattern.isNullOrEmpty() && !target.isNullOrEmpty()) {
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

/**
 * Splits [text] around [separator] as [java.lang.String.split] does: trailing empty strings are dropped, and an empty
 * [text] gives one empty string.
 */
private fun javaSplit(text: String, separator: Char): Array<String> =
    Pattern.compile(Pattern.quote(separator.toString())).split(text)

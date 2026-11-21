package com.github.lburgazzoli.gradle.plugin.karaf.features.model

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class DependencyMatcherTest {
    /**
     * An empty part of a pattern, such as the name in `org.foo::1.0`, matches any value, as it did when the plugin
     * was written in Groovy.
     */
    @ParameterizedTest(name = "{0} matches {1}:{2}:{3}@{4} = {5}")
    @CsvSource(
        "org.foo:bar:1.0,  org.foo, bar,   1.0, jar, true",
        "org.foo:bar:1.0,  org.foo, bar,   2.0, jar, false",
        "org.foo::1.0,     org.foo, other, 1.0, jar, true",
        "org.foo::1.0,     org.foo, other, 2.0, jar, false",
        ":bar,             any,     bar,   1.0, jar, true",
        ":bar,             any,     baz,   1.0, jar, false",
        "@war,             org.foo, bar,   1.0, war, true",
        "@war,             org.foo, bar,   1.0, jar, false",
        "org.foo,          org.foo, bar,   1.0, jar, true",
        "org.*,            org.foo, bar,   1.0, jar, true",
        "org.*,            com.foo, bar,   1.0, jar, false",
        "org.foo:bar:1.0:tests, org.foo, bar, 1.0, jar, false"
    )
    fun `a pattern matches dependency coordinates`(
        pattern: String, group: String, name: String, version: String, type: String, expected: Boolean
    ) {
        assertThat(DependencyMatcher.from(pattern).matches(group, name, version, type, null))
            .`as`("DependencyMatcher.from(%s).matches(%s:%s:%s@%s)", pattern, group, name, version, type)
            .isEqualTo(expected)
    }
}

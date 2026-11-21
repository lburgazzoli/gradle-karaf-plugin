package com.github.lburgazzoli.gradle.plugin.karaf

import org.gradle.api.Action

class ClosureUtil {
    static <T> T configure(Action<? super T> action, T target) {
        action.execute(target)
        return target
    }

    static <T> T configure(Closure closure, T target) {
        closure.resolveStrategy = Closure.DELEGATE_FIRST
        closure.delegate = target
        closure.call(target)
        return target
    }

    static <T> T configureByMap(Map<String, ?> properties, T target) {
        properties.each { key, value ->
            target."${key}" = value
        }
        return target
    }
}
